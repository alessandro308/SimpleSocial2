package SocialServer.RemoteMessage;

import SimpleSocial.Message.PacketMessage;
import SocialServer.SocialServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by alessandro on 10/05/16.
 */
public interface FollowerManager extends Remote {
    public static final String OBJECT_NAME="FOLLOWER_MANAGER";

    void follow(PacketMessage pkt) throws RemoteException;
}
