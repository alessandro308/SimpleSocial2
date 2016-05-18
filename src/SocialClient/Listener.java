package SocialClient;

import SimpleSocial.Message.PacketMessage;
import SimpleSocial.ObjectSocket;

import java.io.IOException;
import java.net.*;
import java.util.Vector;

/**
 * Created by alessandro on 09/05/16.
 */
public class Listener implements Runnable {
    private int IP;
    private String hostname;
    private Vector<String> friendRequest=new Vector<>();

    /**
     * Avvia una socket in ascolto per ricevere le richieste di amicizia.
     * Se i pacchetti non sono di tipo FRIENDREQUEST_CONFIRM i pacchetti sono scartati.
     */

    public Listener(){
        try {
            this.hostname = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {}
    }

    @Override
    public void run() {
        try{
            ServerSocket skt = new ServerSocket(0);
            this.IP = skt.getLocalPort();
            while(true){
                Socket server = skt.accept();
                PacketMessage msg = (PacketMessage) ObjectSocket.readObject(server);
                switch (msg.getType()){
                    case FRIENDREQUEST_CONFIRM:
                        friendRequest.add((String) msg.getMessage().getData());
                        break;
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
    public int getIP(){
        return IP;
    }
    public String getHostname(){
        return hostname;
    }
}
