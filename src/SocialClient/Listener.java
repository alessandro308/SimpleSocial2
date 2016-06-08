package SocialClient;

import SimpleSocial.Message.PacketMessage;
import SimpleSocial.ObjectSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

/**
 * Created by alessandro on 09/05/16.
 */
public class Listener implements Runnable {
    private int port;
    private String hostname;
    private Vector<String> friendRequest=new Vector<>();

    /**
     * Avvia una socket in ascolto per ricevere le richieste di amicizia.
     * Se i pacchetti non sono di tipo FRIENDREQUEST_CONFIRM i pacchetti sono scartati.
     */

    @Override
    public void run() {
        try{
            ServerSocket skt = new ServerSocket(0);
            this.hostname = skt.getInetAddress().getHostAddress();
            this.port = skt.getLocalPort();

            while(!Thread.currentThread().isInterrupted()){
                Socket server = skt.accept();
                /* Sarebbe bene fare questa cosa multithread ma anche così l'efficienza, visto che l'evento di
                 * conferma amicizia non è così frequente, può andare bene
                 */
                PacketMessage msg = (PacketMessage) ObjectSocket.readObject(server);
                if(msg != null)
                    switch (msg.getType()){
                        case FRIENDREQUEST_CONFIRM:
                            friendRequest.add((String) msg.getMessage().getData());
                            break;
                        case FRIENDREQUEST_ACCEPTED:
                            System.out.println("Ei! "+msg.getMessage().getData()+" ha accettato la tua richiesta di amicizia");
                        default:
                            break;
                    }
            }

        }catch (IOException e){
            System.err.println("Errore di comunicazione. "+e.getMessage());
        }

    }

    public Vector<String> getFriendRequest(){
        return friendRequest;
    }
    public int getPort(){
        return port;
    }
    public String getHostname(){
        return hostname;
    }
}
