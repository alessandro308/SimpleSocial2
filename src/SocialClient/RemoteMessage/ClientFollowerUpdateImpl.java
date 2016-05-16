package SocialClient.RemoteMessage;

import java.rmi.RemoteException;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by alessandro on 16/05/16.
 */
public class ClientFollowerUpdateImpl implements ClientFollowerUpdate {
    CopyOnWriteArrayList<Post> unreadMessage;

    ClientFollowerUpdateImpl(CopyOnWriteArrayList<Post> unreadMessage){
        this.unreadMessage = unreadMessage;
    }

    @Override
    public void addMessage(String from, String message) throws RemoteException {
        unreadMessage.add(new Post(from, message));
    }
}
