package SocialServer;

import SimpleSocial.Config;
import SimpleSocial.Exception.UnregisteredConfigNameException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Classe che gestisce i messaggi di keepAlive per il server.
 */
public class KeepAliveServerService implements Runnable{
    Config config;
    UserDB database;
    Lock l = new ReentrantLock();

    public KeepAliveServerService(Config config, UserDB database){
        this.config = config;
        this.database = database;
    }

    @Override
    public void run() {
        try{
            MulticastSocket users = new MulticastSocket();
            users.setTimeToLive((Integer) config.getValue("MULTICAST_TTL"));
            users.setLoopbackMode(false);
            users.setReuseAddress(true);

            InetAddress multicastGroup = InetAddress.getByName((String) config.getValue("MULTICAST_IP"));

            String msg = "KA";
            int port = (Integer) config.getValue("MULTICAST_RECV_PORT");

            DatagramPacket pkt = new DatagramPacket(msg.getBytes(), msg.getBytes().length, multicastGroup, port);
            int delay = (Integer) config.getValue("KEEP_ALIVE_DELAY");
            while(true) {
                try{
                    users.send(pkt);
                }catch ( IOException e){
                    System.out.println("Errore di comunicazione con la rete. "+e.getMessage());
                }
                Thread.sleep(delay);
                Vector<User> online = database.updateOnline();
                System.out.println("-Utenti Online-");
                online.forEach(e -> System.out.println(e.getUsername()));
            }
        }catch (UnregisteredConfigNameException e){
            System.err.println("MULTICAST_PORT o MULTICAST_TTL non settati correttamente nel file di configurazione");
        }catch (IOException e){
            System.err.println("Errore nella creazione della MultiSocket");
            e.printStackTrace();
        }catch (InterruptedException ignored){}
    }

}
