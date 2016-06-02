package SocialServer;

import SimpleSocial.Config;
import SimpleSocial.Exception.UnregisteredConfigNameException;
import SimpleSocial.Exception.UserNotFoundException;
import SimpleSocial.Message.FriendRequestSimpleMessage;
import SimpleSocial.Message.PacketMessage;
import SimpleSocial.Message.SimpleMessage;
import SimpleSocial.ObjectSocketChannel;
import SocialServer.RemoteMessage.FollowerManager;
import SocialServer.RemoteMessage.FollowerManagerImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main class used to run server.
 */
public class SocialServer {
    static Config config = new Config("config.txt");
    static UserDB database;
    KeepAliveServerService keepAliveService;
    static Selector selector;
    Registry registry;

    /**
     * Funzione principale del server. Tramite selettore gestisce le richieste passando poi il canale alla funzione di
     * handler che si preoccupa di gestirle.
     */
    public SocialServer() {
        File dbFile;
        try {
            dbFile = new File((String) config.getValue("DBNAME"));

            if (dbFile.exists()) {
                /*Qui viene letto il DB serializzato contenuto nel file USERDB.dat, deserializzato e sistemato per l'uso*/
                ObjectInputStream objectinputstream = null;
                try {
                    FileInputStream streamIn = new FileInputStream(dbFile);
                    objectinputstream = new ObjectInputStream(streamIn);
                    database = (UserDB) objectinputstream.readObject();
                } catch (ClassNotFoundException | IOException e) {
                    System.err.println("Errore nel caricamento del file. Potrebbe essere corrotto.");
                } finally {
                    if(objectinputstream != null){
                        try{ objectinputstream.close();} catch (IOException ignored){}
                    }
                }
                database.resumeFromSerialization();
            }
            //Se ha fallito la lettura del file o semplicemente il file non esiste, inizializzo nuovo DB
            if (database == null) {
                database = new UserDB();
            }
        } catch (UnregisteredConfigNameException e) {
            System.err.println("Errore config: DBUSER_NAME non correttamente configurato");
        }

        keepAliveService = new KeepAliveServerService(config, database);
        Thread keepAliveThread = new Thread(keepAliveService);
        keepAliveThread.start();
        ExecutorService pool = Executors.newFixedThreadPool(6);

        FollowerManager manager;
        try {
            boolean cond;
            try {
                cond = config.getValue("REGISTRY_IS_LOCAL").equals("true");
            } catch (UnregisteredConfigNameException e) {
                cond = false;
            }
            if (cond)
                registry = LocateRegistry.createRegistry(1099);
            else
                registry = LocateRegistry.getRegistry((String) config.getValue("REGISTRY_HOST"));

            manager = (FollowerManager) UnicastRemoteObject.exportObject(new FollowerManagerImpl(database), 0);
            registry.rebind(FollowerManager.OBJECT_NAME, manager);
        } catch (RemoteException e) {
            System.err.println("Errore creazione RMI Object: " + e.getMessage());
        } catch (UnregisteredConfigNameException e) {
            System.err.println("REGISTRY_HOST non correttamente configurato");
        }

        try {
            selector = Selector.open();
            ServerSocketChannel socket = ServerSocketChannel.open();
            try {
                socket.bind(new InetSocketAddress(InetAddress.getByName((String) config.getValue("SERVER_HOST")),
                        (Integer) config.getValue("SERVER_PORT")));
            } catch (UnregisteredConfigNameException e) {
                System.err.println("Impostazioni non valide. Imposta SERVER_HOST e SERVER_PORT");
                return;
            }

            socket.configureBlocking(false);
            socket.register(selector, SelectionKey.OP_ACCEPT);

            while (!Thread.currentThread().isInterrupted()) {
                selector.selectedKeys().clear();
                selector.select();
                for (SelectionKey key : selector.selectedKeys()) {
                    if (key.isAcceptable()) {
                        try {
                            SocketChannel client = ((ServerSocketChannel) key.channel()).accept();
                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ);
                        } catch (IOException e) {
                            System.err.println("Errore connessione nuovo client - " + e.getClass() + " " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    if (key.isReadable()) {
                        PacketMessage pktMsg;
                        ObjectSocketChannel att;
                        if (key.attachment() == null) {
                            att = new ObjectSocketChannel((SocketChannel) key.channel());
                            key.attach(att);
                        } else {
                            att = (ObjectSocketChannel) key.attachment();
                        }

                        if (att.readObject()) { //Se read return true ha finito di leggere
                            key.interestOps(0);
                            pktMsg = (PacketMessage) att.getReceivedObject();
                            boolean logged = false;
                            try{
                                if(!pktMsg.getType().equals(PacketMessage.MessageType.REGISTER)) {
                                    if (pktMsg.getType().equals(PacketMessage.MessageType.LOGIN) ||
                                            database.getUserByName(pktMsg.getMessage().getUsername()).checkToken(pktMsg.getMessage().getoAuth())) {
                                        database.setOnline(database.getUserByName(pktMsg.getMessage().getUsername()));
                                        logged = true;
                                    } else {
                                        PacketMessageHandler.sendPkt(key, PacketMessage.MessageType.NOTLOGGED, new SimpleMessage());
                                    }
                                }
                            }catch (UserNotFoundException closeAll){
                                PacketMessageHandler.sendError(key, "Utente non riconosciuto.");
                            }

                            /*
                            Tutte i messaggi vengono processati dal ThreadPool, eccezione di quelli di FRIENDREQUEST
                            che causa la registrazione di una nuova socket al selector deve essere fatto in questo stesso
                            thread.
                             */
                            if (logged && pktMsg.getType().equals(PacketMessage.MessageType.FRIENDREQUEST)) {
                                try {
                                    User u = database.getUserByName(pktMsg.getMessage().getUsername());
                                    FriendRequestSimpleMessage msg = (FriendRequestSimpleMessage) pktMsg.getMessage();

                                    User friend = database.getUserByName(msg.getFriend());

                                    try {
                                        SocketChannel friendSkt = SocketChannel.open(new InetSocketAddress(InetAddress.getByName(friend.getHost()), friend.getPort()));
                                        friendSkt.configureBlocking(false);
                                        database.addFriendRequest(msg.getUsername(), msg.getFriend());
                                        ObjectSocketChannel obj = new ObjectSocketChannel(friendSkt);

                                        PacketMessage p = new PacketMessage(new SimpleMessage(u.getUsername()),
                                                PacketMessage.MessageType.FRIENDREQUEST_CONFIRM);
                                        obj.setObjectToSend(p);

                                        friendSkt.register(selector, SelectionKey.OP_WRITE, obj);
                                        PacketMessageHandler.sendPkt(key, PacketMessage.MessageType.FRIENDREQUEST_SENT,
                                                new SimpleMessage("Richiesta di amicizia inoltrata correttamente."));
                                    } catch (NullPointerException | IOException e) {
                                        PacketMessageHandler.sendError(key, "Errore, l'utente non è attualmente online");
                                    }
                                } catch (UserNotFoundException e) {
                                    PacketMessageHandler.sendError(key, "Errore: Utente non valido");
                                }
                            } else if(logged && pktMsg.getType().equals(PacketMessage.MessageType.FRIENDREQUEST_CONFIRM)){
                                try {
                                    String u1 = pktMsg.getMessage().getUsername();
                                    String u2 = (String) pktMsg.getMessage().getData();
                                    if(database.friendRequest.get(u1).equals(u2)){ //Confermo che c'era una richiesta pendente
                                        database.getUserByName(u1).addFriend(u2);
                                        database.getUserByName(u2).addFriend(u1);
                                        PacketMessageHandler.sendPkt(key, PacketMessage.MessageType.SUCCESS, new SimpleMessage());

                                        //Avviso il mittente che la richiesta è stata accettata
                                        User friend = database.getUserByName(u2);
                                        SocketChannel friendSkt = SocketChannel.open(new InetSocketAddress(InetAddress.getByName(friend.getHost()), friend.getPort()));
                                        friendSkt.configureBlocking(false);
                                        ObjectSocketChannel obj = new ObjectSocketChannel(friendSkt);

                                        PacketMessage p = new PacketMessage(new SimpleMessage(u1), PacketMessage.MessageType.FRIENDREQUEST_ACCEPTED);
                                        obj.setObjectToSend(p);

                                        friendSkt.register(selector, SelectionKey.OP_WRITE, obj);
                                    }
                                }catch (UserNotFoundException ignored){
                                }catch (NullPointerException e){
                                    PacketMessageHandler.sendError(key, "Errore interno al server. La richiesta di amicizia non esiste.");
                                }
                            } else if (logged || pktMsg.getType().equals(PacketMessage.MessageType.REGISTER) ){
                                pool.submit(new PacketMessageHandler(key, pktMsg));
                            }
                        }

                    }
                    if (key.isWritable()) {
                        if (((ObjectSocketChannel) key.attachment()).writeObject()) { //se torna true ha finito di scrivere
                            key.cancel();
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Errore creazione selector - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        new SocialServer();

    }

}
