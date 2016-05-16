package SocialClient.UI;

import SimpleSocial.Exception.UnregisteredConfigNameException;
import SimpleSocial.Exception.UserExistsException;
import SocialClient.SocialClient;

import java.io.IOException;
import java.util.Vector;

/**
 * Presenta le stesse funzioni di SocialClient, solo pubbliche per richiamarle esternamente all'oggetto.
 *
 */
public class SocialClientPublic extends SocialClient {

    public void sendFollowRequest(String toFollow) {
        super.sendFollowRequest(toFollow);
    }

    public void shareMessage(String msg) throws UnregisteredConfigNameException, IOException {
        super.shareMessage(msg);
    }

    public String acceptsFriendshipRequest(String friend) throws UnregisteredConfigNameException, IOException {
        return super.acceptsFriendshipRequest(friend);
    }

    public String requestFriendship(String friendName) throws UnregisteredConfigNameException, IOException {
        return super.requestFriendship(friendName);
    }

    public Vector<String> getFriendList() throws UnregisteredConfigNameException, IOException {
        return super.getFriendList();
    }

    public String newToken() {
        return super.newToken();
    }

    public boolean login(String user, String psw) {
        return super.login(user, psw);
    }

    public boolean logout() {
        return super.logout();
    }

    public boolean register(String user, String psw) throws UserExistsException {
        return super.register(user, psw);
    }

    public Vector<String> searchUser(String user) {
        return super.searchUser(user);
    }

    public boolean isValidSession() {
        return super.isValidSession();
    }
}
