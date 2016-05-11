package SimpleSocial.Message.RemoteMessage;

import SimpleSocial.Exception.UserNotFoundException;
import SimpleSocial.Message.PacketMessage;
import SocialServer.User;
import SocialServer.UserDB;

import java.rmi.RemoteException;

/**
 * Created by alessandro on 10/05/16.
 */
public class FollowerManagerImpl implements FollowerManager {

    private UserDB database;

    public FollowerManagerImpl(UserDB database){
        this.database = database;
    }

    /**
     * Richiamare la funzione con un PacketMessage contentente come dati il nome dell'utente che si vuole seguire
     * @param pkt Pacchetto spedito
     */
    public void follow(PacketMessage pkt) throws RemoteException{
        User u;
        User followed;
        try {
            u = database.getUserByName(pkt.getMessage().getUsername());
            followed = database.getUserByName((String) pkt.getMessage().getData());
        } catch (UserNotFoundException ignored) {
            return;
        }

        System.out.println("CHIAMATA FOLLOW");
        if(pkt.getType().equals(PacketMessage.MessageType.FOLLOWREQUEST) && u.checkToken(pkt.getMessage().getoAuth()))
            database.setOnline(u);
        followed.addFollower(u.getUsername());
    }

}
