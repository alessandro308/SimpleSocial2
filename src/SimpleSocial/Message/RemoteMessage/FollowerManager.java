package SimpleSocial.Message.RemoteMessage;

import SimpleSocial.Message.PacketMessage;
import SocialServer.UserDB;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

/**
 * Created by alessandro on 10/05/16.
 */
public interface FollowerManager extends Remote {
    public static final String OBJECT_NAME="FOLLOWER_MANAGER";

    void follow(PacketMessage pkt) throws RemoteException;
}
