package SocialServer.RemoteMessage;

import SimpleSocial.Exception.UserNotFoundException;
import SimpleSocial.Message.PacketMessage;
import SocialClient.RemoteMessage.ClientFollowerUpdate;
import SocialServer.User;

import java.rmi.RemoteException;

/**
 * Created by alessandro on 10/05/16.
 */
public class FollowerManagerImpl implements FollowerManager {

    private SocialServer.UserDB database;

    public FollowerManagerImpl(SocialServer.UserDB database){
        this.database = database;
    }

    /**
     * Richiamare la funzione con un PacketMessage contentente come dati il nome dell'utente che si vuole seguire
     * @param pkt Pacchetto spedito
     */
    public void follow(PacketMessage pkt) throws RemoteException{
        SocialServer.User u;
        SocialServer.User followed;
        try {
            u = database.getUserByName(pkt.getMessage().getUsername());
            followed = database.getUserByName((String) pkt.getMessage().getData());
        } catch (UserNotFoundException ignored) {
            return;
        }

        if(pkt.getType().equals(PacketMessage.MessageType.FOLLOWREQUEST) && u.checkToken(pkt.getMessage().getoAuth())) {
            database.setOnline(u);
            followed.addFollower(u.getUsername());
        }
    }

    @Override
    public void addCallback(String user, String oAuth, ClientFollowerUpdate callback) throws RemoteException {
        try {
            User u = database.getUserByName(user);
            if(u.checkToken(oAuth)){
                u.setStub(callback);
                database.setOnline(u);
            }
        } catch (UserNotFoundException ignored) {
            /* L'utente non esiste. Ciaone! */
        }
    }

}
