package SocialClient;

import SimpleSocial.Config;
import SimpleSocial.Exception.UnregisteredConfigNameException;

import java.io.IOException;
import java.net.*;


/**
 * Gestisce le connessioni di tipo keepAlive che arrivano dal server.
 *
 * Il KeepAliveUserService riceve i pacchetti che gli arrivano e se contiene il dato "KA" risponde
 * con un messaggio su TCP utilizzando la serializzazione dei PacketMessage implementata in questo progetto.
 */
class KeepAliveUserService implements Runnable {
    private MulticastSocket multicastSocket;
    private Config config;
    private DatagramSocket skt;
    KeepAliveUserService(Config config){
        this.config = config;
        try {
            this.multicastSocket = new MulticastSocket((Integer) config.getValue("MULTICAST_PORT"));
            InetAddress multicastGroup = InetAddress.getByName((String) config.getValue("MULTICAST_IP"));
            this.multicastSocket.joinGroup(multicastGroup);
        } catch (IOException e) {
            System.err.println("Errore nella gestione del multicasting. Riprovare.");
        } catch (UnregisteredConfigNameException e){
            System.err.println("Non sono stati correttamente impostati MULTICAST_PORT e/o MULTICAST_IP");
        }

        try {
            //Usata per mandare la risposte del KeepAlive al server
            skt = new DatagramSocket();
        } catch (SocketException e) {
            System.err.println("Errore avvio DatagramSocket nel KeepAliveUserService. Riavviare");
        }
    }

    @Override
    public void run() {
        DatagramPacket pkt = new DatagramPacket(new byte[512], 512);
        while(!Thread.currentThread().isInterrupted()){
            try{
                multicastSocket.receive(pkt);
                byte[] b = pkt.getData();
                String msg = new String(b, 0, pkt.getLength());
                String reply;
                try{
                    reply = ((String) config.getValue("OAUTH"))+config.getValue("USER");
                } catch (UnregisteredConfigNameException e){
                    /*L'utente non è loggato*/
                    return;
                }

                DatagramPacket pacchetto = new DatagramPacket(reply.getBytes(), reply.getBytes().length,
                                                                InetAddress.getByName((String) config.getValue("SERVER_HOSTNAME")),
                                                                (Integer) config.getValue("PORTA_SERVER_KA"));
                if(msg.equals("KA")){ //K_eep A_live: Tutto bene
                    skt.send(pacchetto);
                }
            } catch (IOException e){
                System.err.println("Errore ricezione keepAlive. Verificare la connessione con il server");
                e.printStackTrace();
            } catch (UnregisteredConfigNameException e) {
                if(!Thread.currentThread().isInterrupted()) {
                    System.err.println("Errore nel caricamento delle impostazioni. " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        try {
            multicastSocket.leaveGroup(InetAddress.getByName((String) config.getValue("MULTICAST_IP")));
        } catch (IOException | UnregisteredConfigNameException e) {
            System.err.println("Qualcosa è andato storto nell'uscita dal MulticastGroup. "+e.getMessage());
        }
    }
}
