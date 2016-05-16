package SocialClient.RemoteMessage;

import SimpleSocial.Message.Post;

import java.rmi.RemoteException;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by alessandro on 16/05/16.
 */
public class ClientFollowerUpdateImpl implements ClientFollowerUpdate {
    CopyOnWriteArrayList<Post> unreadMessage = new CopyOnWriteArrayList<>();

    public ClientFollowerUpdateImpl(){
    }

    @Override
    public void addMessage(String from, String message) throws RemoteException {
        unreadMessage.add(new Post(from, message));
    }

    public CopyOnWriteArrayList<Post> getUnreadMessage(){
        return this.unreadMessage;
    }

    public int getUnreadMessageCount(){
        return this.unreadMessage.size();
    }

    public void removeAllMessage(){
        this.unreadMessage.clear();
    }
}
