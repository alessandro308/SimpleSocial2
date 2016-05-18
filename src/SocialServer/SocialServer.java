package SocialServer;

import SimpleSocial.Config;
import SimpleSocial.Exception.UnregisteredConfigNameException;
import SimpleSocial.Message.PacketMessage;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main class used to run server.
 */
public class SocialServer {
    static UserDB database = new UserDB();
    static Config config = new Config("config.txt");
    KeepAliveServerService keepAliveService = new KeepAliveServerService(config, database);
    static Selector selector;
    Registry registry;

    /**
     * Funzione principale del server. Tramite selettore gestisce le richieste passando poi il canale alla funzione di
     * handler che si preoccupa di gestirle.
     */
    public SocialServer(){
        Thread keepAliveThread = new Thread(keepAliveService);
        keepAliveThread.start();
        ExecutorService pool = Executors.newFixedThreadPool(6);

        FollowerManager manager;
        try{
            boolean cond;
            try{
                cond = config.getValue("REGISTRY_IS_LOCAL").equals("true");
            } catch (UnregisteredConfigNameException e){
                cond = false;
            }
            if(cond)
                registry = LocateRegistry.createRegistry(1099);
            else
                registry = LocateRegistry.getRegistry((String) config.getValue("REGISTRY_HOST"));

        }catch (RemoteException e){
            System.err.println("Errore creazione RMI Object: "+e.getMessage());
        }catch (UnregisteredConfigNameException e){
            System.err.println("REGISTRY_HOST non correttamente configurato");
        }

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
                socket.bind(new InetSocketAddress(InetAddress.getByName((String) config.getValue("SERVER_HOST")), (Integer) config.getValue("SERVER_PORT")));
            } catch (UnregisteredConfigNameException e){
                System.err.println("Impostazioni non valide. Imposta SERVER_HOST e SERVER_PORT");
                return;
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
                            key.interestOps(0);
                            pktMsg = (PacketMessage) att.getReceivedObject();
                            pool.submit(new PacketMessageHandler(key, pktMsg));
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

    public static void main(String[] args) {

        new SocialServer();

    }

}
