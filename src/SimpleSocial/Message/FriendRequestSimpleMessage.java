package SimpleSocial.Message;

/**
 * Created by alessandro on 09/05/16.
 */
public class FriendRequestSimpleMessage extends SimpleMessage {
    private String friend;

    public FriendRequestSimpleMessage(String user, String oAuth, String friend){
        this.oAuth = oAuth;
        this.user = user;
        this.friend = friend;
    }

    public String getFriend(){
        return friend;
    }

}
