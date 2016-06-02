package SocialServer;

import SimpleSocial.Config;
import SimpleSocial.Exception.UnregisteredConfigNameException;
import SimpleSocial.Exception.UserNotFoundException;

import java.io.IOException;
import java.net.*;
import java.util.Vector;

/**
 * Classe che gestisce i messaggi di keepAlive per il server.
 */
class KeepAliveServerService implements Runnable{
    private Config config;
    private UserDB database;
    private DatagramSocket skt;
    private Long sendTime;

    public KeepAliveServerService(Config config, UserDB database){
        this.config = config;
        this.database = database;
        try{
           this.skt = new DatagramSocket((Integer) config.getValue("PORTA_SERVER_KA"));
        } catch (SocketException | UnregisteredConfigNameException e) {
            e.printStackTrace();
        }

        (new Thread(){ //Mi scuso con chiunque per questa dichiarazione inline del Thread ma fare l'ennesimo file mi sembrava peggio
            public void run(){
                try {
                    DatagramPacket pkt = new DatagramPacket(new byte[512], 512);
                    while(!Thread.currentThread().isInterrupted()){
                        skt.receive(pkt);
                        if(System.currentTimeMillis() - sendTime < 10000 ) {
                            //Scarta i pacchetti arrivati dopo i 10 secondi dall'ultimo invio di KeepAlive
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

            int port = (Integer) config.getValue("MULTICAST_RECV_PORT");
            DatagramPacket pkt = new DatagramPacket(msg.getBytes(), msg.getBytes().length, multicastGroup, port);

            int delay = (Integer) config.getValue("KEEP_ALIVE_DELAY");

            while(!Thread.currentThread().isInterrupted()) {
                try{
                    multicastSocket.send(pkt);
                    sendTime = System.currentTimeMillis();
                }catch ( IOException e){
                    System.out.println("Errore di comunicazione con la rete multicast. "+e.getMessage());
                }
                Thread.sleep(10000); //Dormi almeno 10 secondi, tempo minimo di KEEPALiVE.
                try {
                    Vector<User> online = database.updateOnline();
                    System.out.println("-Utenti Online-");
                    if (online.isEmpty())
                        System.out.println("_nessuno_");
                    else
                        online.forEach(e -> System.out.println(e.getUsername()));
                    try {
                        Thread.sleep(delay - 10000);//Aspetta fino alla prossima iterazione
                    } catch (IllegalArgumentException ignored) {
                        /*
                         * Se è stata lanciata l'eccezione vuol dire che si vogliono mandare keepAlive troppo frequenti.
                         * Procedi quindi a rimandare nuovamente un KA.
                         */
                    }
                }catch (IllegalArgumentException | InterruptedException e){
                    return; //TODO: Testare questa nuova gestione del KEEPALIVE
                }
            }
        }catch (UnregisteredConfigNameException e){
            System.err.println("MULTICAST_PORT o MULTICAST_TTL non settati correttamente nel file di configurazione");
        }catch (IOException e){
            System.err.println("Errore nella creazione della MultiSocket");
            e.printStackTrace();
        }catch (InterruptedException ignored){}
    }

}
