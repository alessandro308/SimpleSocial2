package SocialServer;

import SimpleSocial.Config;
import SimpleSocial.Exception.LoginFailException;
import SimpleSocial.Exception.UnregisteredConfigNameException;
import SimpleSocial.Exception.UserExistsException;
import SimpleSocial.Exception.UserNotFoundException;
import SimpleSocial.Message.*;
import SimpleSocial.Message.PacketMessage.MessageType;
import SimpleSocial.ObjectSocketChannel;
import SocialServer.RemoteMessage.FollowerManager;
import SocialServer.RemoteMessage.FollowerManagerImpl;

import java.io.IOException;
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
import java.util.Vector;

/**
 * Main class used to run server.
 */
public class SocialServer {
    UserDB database = new UserDB();
    Config config = new Config("config.txt");
    KeepAliveServerService keepAliveService = new KeepAliveServerService(config, database);
    Selector selector;
    Registry registry;
    /**
     * Funzione principale del server. Tramite selettore gestisce le richieste passando poi il canale alla funzione di
     * handler che si preoccupa di gestirle.
     */
    public SocialServer(){
        Thread keepAliveThread = new Thread(keepAliveService);
        keepAliveThread.start();

        FollowerManager manager;
        try{
            //registry = LocateRegistry.getRegistry((String) config.getValue("REGISTRY_HOST"));
            registry = LocateRegistry.createRegistry(1099);
        }catch (RemoteException e){
            System.err.println("Errore creazione RMI Object: "+e.getMessage());
        }/*catch (UnregisteredConfigNameException e){
            System.err.println("REGISTRY_HOST non correttamente configurato");
        }*/

        try {
            manager = (FollowerManager) UnicastRemoteObject.exportObject(new FollowerManagerImpl(database), 0);
            registry.rebind(FollowerManager.OBJECT_NAME, manager);
        } catch (RemoteException e) {
            System.err.println("Errore bind RMI: "+e.getMessage());
        }

        try {
            selector = Selector.open();
            ServerSocketChannel socket = ServerSocketChannel.open();
            try{
                socket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), (Integer) config.getValue("SERVER_PORT")));
            } catch (UnregisteredConfigNameException e){
                config.setConfig("SERVER_PORT", 2000);
                socket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), 2000));
            }
            socket.configureBlocking(false);
            socket.register(selector, SelectionKey.OP_ACCEPT);

            while(true){
                selector.selectedKeys().clear();
                selector.select();
                for(SelectionKey key : selector.selectedKeys()){
                    if(key.isAcceptable()){
                        try{
                            SocketChannel client = ((ServerSocketChannel) key.channel()).accept();
                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ);
                        } catch (IOException e){
                            System.err.println("Errore connessione nuovo client - "+e.getClass()+" "+e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    if(key.isReadable()){
                        PacketMessage pktMsg;
                        ObjectSocketChannel att;
                        if(key.attachment() == null ) {
                            att = new ObjectSocketChannel((SocketChannel) key.channel());
                            key.attach(att);
                        }
                        else {
                            att = (ObjectSocketChannel) key.attachment();
                        }
                        if(att.readObject()){ //Se read return true ha finito di leggere
                            pktMsg = (PacketMessage) att.getReceivedObject();
                            connectionHandler(pktMsg, key);
                        }
                    }
                    if(key.isWritable()){
                        if( ((ObjectSocketChannel) key.attachment()).writeObject() ){ //se torna true ha finito di scrivere
                            key.cancel();
                        }
                    }
                }
            }
        } catch (IOException e){
            System.err.println("Errore creazione selector - "+e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Funzione di lettura e gestione dei pacchetti arrivati
     * @param p - Pacchetto ricevuto
     * @param sender - Mittente. Necessario per le risposte.
     */
    private void connectionHandler(PacketMessage p, SelectionKey sender){
        try{
            if(!p.getType().equals(MessageType.REGISTER)) {
                if (p.getType().equals(MessageType.LOGIN) ||
                        database.getUserByName(p.getMessage().getUsername()).checkToken(p.getMessage().getoAuth())) {
                    database.setOnline(database.getUserByName(p.getMessage().getUsername()));
                } else {
                    sendError(sender, "Utente non autenticato correttamente.");
                }
            }
        }catch (UserNotFoundException closeAll){
            sendError(sender, "Utente non riconosciuto.");
        }

        if(p.getType().equals(MessageType.REGISTER)){
            RegisterSimpleMessage msg = (RegisterSimpleMessage) p.getMessage();
            try{
                if(msg.getUsername().equals("") || msg.getUsername().length() < 3){
                    sendError(sender, "Nome utente troppo corto");
                    return;
                }
                database.addUser(new User(msg.getUsername(), msg.getPassword()));
                sendPkt(sender, MessageType.SUCCESS, new SimpleMessage());
            } catch (UserExistsException e){
                sendError(sender, "L'utente esiste già");
            }
            return;
        }

        if(p.getType().equals(MessageType.LOGIN)){
            LoginSimpleMessage msg = (LoginSimpleMessage) p.getMessage();
            try{
                User u = database.getUserByName(msg.getUsername());
                String oAuth = u.checkLogin(msg.getPassword());
                u.setHost(msg.getUserHostname(), msg.getUserPORT());
                database.setOnline(u.getUsername());
                sendPkt(sender, MessageType.LOGINRESPONSE, new LoginSimpleMessage(oAuth, u.getLoginTime(), (String) config.getValue("MULTICAST_IP")));
            }catch (UserNotFoundException e){
                sendError(sender, "L'utente non esiste nel database. Registrarsi.");
            } catch (LoginFailException e){
                sendError(sender, "Login fallito. Riprovare.");
            } catch (UnregisteredConfigNameException e){
                System.err.println("Non è possibile comunicare il multicast IP, non è configurato correttamente");
                e.printStackTrace();
            }
        }
        if(p.getType().equals(MessageType.TOKENUPDATE)){
            SimpleMessage msg = p.getMessage();
            try{
                User u = database.getUserByName(msg.getUsername());
                sendPkt(sender, MessageType.LOGINRESPONSE,
                        new LoginSimpleMessage(u.extendSession(msg.getoAuth()),
                                                u.getLoginTime(),
                                                (String) config.getValue("MULTICAST_IP")));
            } catch (UserNotFoundException e){
                sendError(sender, "L'utente non esiste nel database. Registrarsi.");
            } catch (LoginFailException e){
               sendError(sender, "oAuth token non valido.");
            } catch (UnregisteredConfigNameException ignored){}
        }

        if(p.getType().equals(MessageType.LOGOUT)){
            LogoutSimpleMessage msg = (LogoutSimpleMessage) p.getMessage();
            try{
                User u = database.getUserByName(msg.getUsername());
                database.setOffline(u);
                u.logout();
            } catch (UserNotFoundException ignored){}
        }

        if(p.getType().equals(MessageType.KEEPALIVE)){
            try{
                User u = database.getUserByName(p.getMessage().getUsername());
                database.setOnline(u);
            } catch (UserNotFoundException ignored){}
        }

        if(p.getType().equals(MessageType.SEARCHUSER)){
            Vector<String> found = database.searchUser(((SearchUserSimpleMessage) p.getMessage()).getQuery());
            sendPkt(sender, MessageType.SEARCHUSERRESPONSE, new SearchUserSimpleMessage(found));
        }

        if(p.getType().equals(MessageType.FRIENDLIST)){
            try{
                User u = database.getUserByName(p.getMessage().getUsername());
                sendPkt(sender, MessageType.FRIENDLISTRESPONSE, new SimpleMessage(u.getFriends()));
            }catch (UserNotFoundException ignored){}
        }

        if(p.getType().equals(MessageType.FRIENDREQUEST)){
            try{
                User u = database.getUserByName(p.getMessage().getUsername());
                FriendRequestSimpleMessage msg = (FriendRequestSimpleMessage) p.getMessage();
                try{
                    if(database.isOnline(msg.getFriend())){
                        User friend = database.getUserByName(msg.getFriend());
                        SocketChannel friendSkt = SocketChannel.open();
                        try{
                            friendSkt.connect(new InetSocketAddress(friend.getHost(), friend.getPort()));
                            friendSkt.configureBlocking(false);
                            database.addFriendRequest(msg.getUsername(), msg.getFriend());
                            sendPkt(sender, MessageType.FRIENDREQUEST_SENT,
                                            new SimpleMessage("Richiesta di amicizia inoltrata correttamente."));
                            ObjectSocketChannel obj = new ObjectSocketChannel(friendSkt);
                            obj.setObjectToSend(new PacketMessage(new SimpleMessage(u.getUsername()), MessageType.FRIENDREQUEST_CONFIRM));
                            SelectionKey friendKey = friendSkt.register(selector, SelectionKey.OP_WRITE, obj);
                        }catch (NullPointerException e){
                            sendError(sender, "Errore: amico non raggiunbile. Non si conosce l'hostname e l'ip dell'amico.");
                        }

                    }else{
                        throw new IOException("L'utente non è online");
                    }
                }catch (IOException e){
                    sendError(sender, "Errore: "+e.getMessage());
                }
            }catch (UserNotFoundException ignored){}
        }
        if(p.getType().equals(MessageType.FRIENDREQUEST_CONFIRM)){
            try {
                User u1 = database.getUserByName(p.getMessage().getUsername());
                User u2 = database.getUserByName((String) p.getMessage().getData());
                if(database.friendRequest.get(u1).getUsername().equals(u2.getUsername())) { //Confermo che c'era una cosa pendente
                    u1.addFriend(u2.getUsername());
                    u2.addFriend(u1.getUsername());
                    sendPkt(sender, MessageType.SUCCESS, new SimpleMessage());
                }
            } catch (UserNotFoundException ignored){
            }catch (NullPointerException e){
                sendError(sender, "Errore interno al server. La richiesta di amicizia non esiste.");
            }
        }
        if(p.getType().equals(MessageType.SHARETHIS)){
            try{
                User u = database.getUserByName(p.getMessage().getUsername());
                for(String friendName : u.getFollowers()){

                }
            } catch (UserNotFoundException ignored){}
        }
    }

    /**
     * Spedisce un pacchetto di errore.
     * @param to - destinatario del pacchetto
     * @param cause - Causa dell'errore
     */
    public static void sendError(SelectionKey to, String cause){
        sendPkt(to, MessageType.ERROR, new ErrorSimpleMessage(cause));
    }

    /**
     * Spedisce un pacchetto generico.
     * @param to - Destinatario del pacchetto
     * @param type - Tipo del pacchetto spedito
     * @param msg - Messaggio contenuto nel pacchetto
     */
    public static void sendPkt(SelectionKey to, MessageType type, SimpleMessage msg){
        ObjectSocketChannel client;
        if((client = (ObjectSocketChannel) to.attachment()) == null)
            client = new ObjectSocketChannel((SocketChannel) to.channel());

        client.setObjectToSend(new PacketMessage(msg, type));

        if(to.attachment() == null)
            to.attach(client);

        to.interestOps(SelectionKey.OP_WRITE);
    }

    public static void main(String[] args) {
        try{
            new SocialServer();
        }catch (Throwable e){
            e.printStackTrace();
        }
    }

}
