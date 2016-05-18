package SocialServer;

import SimpleSocial.Config;
import SimpleSocial.Exception.UnregisteredConfigNameException;
import SimpleSocial.Exception.UserNotFoundException;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
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
    DatagramSocket skt;
    Long sendTime;

    public KeepAliveServerService(Config config, UserDB database){
        this.config = config;
        this.database = database;
        try{
           this.skt = new DatagramSocket((Integer) config.getValue("PORTA_SERVER_KA"));
        } catch (SocketException | UnregisteredConfigNameException e) {
            e.printStackTrace();
        }

        (new Thread(){
            public void run(){
                try {
                    DatagramPacket pkt = new DatagramPacket(new byte[512], 512);
                    while(true){
                        skt.receive(pkt);
                        if(System.currentTimeMillis() - sendTime < 10000 ) {
                            //Controlla al più pacchetti arrivati dopo 10 secondi dall'invio del keepAlive
                            String user = new String(pkt.getData(), 10, pkt.getLength() - 10);
                            String oAuth = new String(pkt.getData(), 0, 10);
                            if (database.getUserByName(user).checkToken(oAuth))
                                database.setOnline(user);
                        }
                    }
                } catch (IOException | UserNotFoundException e) {
                    e.printStackTrace();
                }

            }
        }).start();

    }

    @Override
    public void run() {
        try{
            MulticastSocket multicastSocket = new MulticastSocket();
            multicastSocket.setTimeToLive((Integer) config.getValue("MULTICAST_TTL"));

            multicastSocket.setLoopbackMode(false);
            multicastSocket.setReuseAddress(true);

            String msg = "KA";
            InetAddress multicastGroup = InetAddress.getByName((String) config.getValue("MULTICAST_IP"));
            multicastSocket.joinGroup(multicastGroup); //todo: capire il senso di questa riga di codice!!

            int port = (Integer) config.getValue("MULTICAST_RECV_PORT");
            DatagramPacket pkt = new DatagramPacket(msg.getBytes(), msg.getBytes().length, multicastGroup, port);

            int delay = (Integer) config.getValue("KEEP_ALIVE_DELAY");

            while(true) {
                try{
                    multicastSocket.send(pkt);
                    sendTime = System.currentTimeMillis();
                }catch ( IOException e){
                    System.out.println("Errore di comunicazione con la rete multicast. "+e.getMessage());
                }

                Thread.sleep(delay);
                Vector<User> online = database.updateOnline();
                System.out.println("-Utenti Online-");
                if(online.isEmpty())
                    System.out.println("_nessuno_");
                else
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
