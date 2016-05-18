package SimpleSocial.Message;

import SocialClient.RemoteMessage.ClientFollowerUpdate;

/**
 * Created by alessandro on 06/05/16.
 */
public class LoginSimpleMessage extends SimpleMessage {
    private String psw;
    private long oAuthTime;
    private String userIP;
    private int userPORT;
    private ClientFollowerUpdate stub;

    private String multicastIP;


    public LoginSimpleMessage(String user, String psw){
        this.user = user;
        this.psw = psw;
    }

    public LoginSimpleMessage(String user, String psw, String IP, int port){
        this.user = user;
        this.psw = psw;
        this.userPORT = port;
        this.userIP = IP;
    }

    public LoginSimpleMessage(String oAuth, Long oAuthTime, String multicastIP){
        this.oAuth = oAuth;
        this.oAuthTime = oAuthTime;
        this.multicastIP = multicastIP;
    }

    public String getPassword(){
        return this.psw;
    }
    public long getoAuthTime(){
        return oAuthTime;
    }
    public String getMulticastIP(){ return multicastIP; }
    public String getUserHostname(){ return this.userIP;}
    public int getUserPORT(){ return this.userPORT;}
    public ClientFollowerUpdate getStub(){ return stub;}

}
