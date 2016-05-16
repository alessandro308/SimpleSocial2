package SocialClient.RemoteMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Riceve gli aggiornamenti dei contenuti pubblicati dagli utenti che si seguono
 */
public interface ClientFollowerUpdate extends Remote {
    void addMessage(String from, String message) throws RemoteException;
}
