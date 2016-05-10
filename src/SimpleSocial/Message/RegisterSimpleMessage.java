package SimpleSocial.Message;



/**
 * Created by alessandro on 06/05/16.
 */
public class RegisterSimpleMessage extends SimpleMessage {
    String psw;

    public RegisterSimpleMessage(String user, String psw){
        this.user = user;
        this.psw = psw;
    }

    public String getPassword(){
        return psw;
    }
}
