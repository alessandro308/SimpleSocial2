package SocialServer.RemoteMessage;

import SimpleSocial.Exception.UserNotFoundException;
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

    public void follow(String username, String oAuth, String toFollow) throws RemoteException{
        SocialServer.User u;
        SocialServer.User followed;
        try {
            u = database.getUserByName(username);
            followed = database.getUserByName(toFollow);
        } catch (UserNotFoundException ignored) {
            return;
        }

        if(u.checkToken(oAuth)) {
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
