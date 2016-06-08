package SocialServer.RemoteMessage;

import SocialClient.RemoteMessage.ClientFollowerUpdate;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia che permette ai client di registrare la callback e di seguire un amico
 */
public interface FollowerManager extends Remote {
    String OBJECT_NAME="FOLLOWER_MANAGER";

    public void follow(String username, String oAuth, String toFollow) throws RemoteException;
    void addCallback(String user, String oAuth, ClientFollowerUpdate callback) throws RemoteException;
}
