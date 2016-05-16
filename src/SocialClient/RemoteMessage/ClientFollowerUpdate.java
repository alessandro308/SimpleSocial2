package SocialClient.RemoteMessage;

import SimpleSocial.Message.Post;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Riceve gli aggiornamenti dei contenuti pubblicati dagli utenti che si seguono
 */
public interface ClientFollowerUpdate extends Remote {
    void addMessage(String from, String message) throws RemoteException;
}
