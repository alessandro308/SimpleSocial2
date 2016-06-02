package SocialClient.RemoteMessage;

import SimpleSocial.Message.Post;

import java.rmi.RemoteException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * È l'oggetto che gestisce le componenti RMI del client
 */
public class ClientFollowerUpdateImpl implements ClientFollowerUpdate {
    private CopyOnWriteArrayList<Post> unreadMessage = new CopyOnWriteArrayList<>();

    public ClientFollowerUpdateImpl(){
    }

    /**
     * Aggiunge un messaggio spedito dal server per un utente che si sta seguendo
     * @param from Nome dell'utente che ha scritto il messaggio
     * @param message Messaggio pubblicato
     * @throws RemoteException Se avvie un errore remoto
     */
    @Override
    public void addMessage(String from, String message) throws RemoteException {
        unreadMessage.add(new Post(from, message));
    }

    /**
     * Restituisce la coda dei messaggi ricevuti
     * @return La lista dei messaggi non ricevuti
     */
    public CopyOnWriteArrayList<Post> getUnreadMessage(){
        /*Il client, alla riconnessione, chiama questa funzione per avere la lista dei messaggi mandati in sua assenza.
         * Forse era logicamente più corretto che glieli mandava il server e non fosse lui a doverla richiedere ma così
         * si sono scritte meno righe di codice.
         */
        return this.unreadMessage;
    }

    /**
     * Restituisce il numero dei messaggi non ancora letti
     * @return il numero dei messaggi non ancora letti
     */
    public int getUnreadMessageCount(){
        return this.unreadMessage.size();
    }

    /**
     * Rimuove tutti gli elementi dalla coda dei messaggi ricevuti.
     */
    public void removeAllMessage(){
        this.unreadMessage.clear();
    }
}
