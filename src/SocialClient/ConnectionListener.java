package SocialClient;

import SimpleSocial.Message.PacketMessage;
import SimpleSocial.ObjectSocket;

import java.io.IOException;
import java.net.*;

/**
 * Gestisce le connessioni in entrata per il SocialClient
 */
public class ConnectionListener {
    ServerSocket socket;

    public ConnectionListener(int port){
        try{
            socket = new ServerSocket(port);
            Socket server = socket.accept();
            PacketMessage pkt = (PacketMessage) ObjectSocket.readObject(server);
            if(pkt.getType() == PacketMessage.MessageType.KEEPALIVE){

            }

        } catch (IOException e){
            System.err.println("Errore gestione Socket");
        }

    }

}
