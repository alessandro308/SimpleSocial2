package SocialClient;

import SimpleSocial.Config;
import SimpleSocial.Exception.UnregisteredConfigNameException;
import SimpleSocial.Message.PacketMessage;
import SimpleSocial.Message.SimpleMessage;
import SimpleSocial.ObjectSocket;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;


/**
 * Gestisce le connessioni di tipo keepAlive che arrivano dal server.
 *
 * Il KeepAliveUserService riceve i pacchetti che gli arrivano e se contiene il dato "KA" risponde
 * con un messaggio su TCP utilizzando la serializzazione dei PacketMessage implementata in questo progetto.
 * /TODO: Capire se posso usare TCP qui o se le risposte devono viaggiare su UDP
 */
public class KeepAliveUserService implements Runnable {
    private MulticastSocket server;
    Config config;
    public KeepAliveUserService(MulticastSocket server, Config config){
        this.server = server;
        this.config = config;
    }

    @Override
    public void run() {
        DatagramPacket pkt = new DatagramPacket(new byte[512], 512);
        while(true){
            try{
                server.receive(pkt);

                byte[] b = pkt.getData();
                String msg = new String(b, 0, pkt.getLength());

                if(msg.equals("KA")){ //KA: Tutto bene
                    ObjectSocket skt = new ObjectSocket(InetAddress.getByName((String) config.getValue("SERVER_HOSTNAME")),
                                                (Integer) config.getValue("SERVER_PORT"));
                    try{
                        skt.writeObject(new PacketMessage(
                                            new SimpleMessage(  (String) config.getValue("USER"),
                                                                (String) config.getValue("OAUTH")),
                                                                 PacketMessage.MessageType.KEEPALIVE));
                    } catch (UnregisteredConfigNameException e){
                        return;
                    }
                }
            } catch (IOException e){
                System.err.println("Errore ricezione keepAlive. Verificare la connessione con il server");
                e.printStackTrace();
            } catch (UnregisteredConfigNameException e){
                System.err.println("Errore nel caricamento delle impostazioni. Verificare SERVER_HOSTNAME");
            }
        }
    }
}
