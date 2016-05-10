package SimpleSocial.Message;

/**
 * Created by alessandro on 07/05/16.
 */
public class LogoutSimpleMessage extends SimpleMessage {

    public LogoutSimpleMessage(String user, String oAuth){
        this.oAuth = oAuth;
        this.user = user;
    }

    public String getoAuth(){
        return this.oAuth;
    }

    public String getUsername(){
        return this.user;
    }
}
