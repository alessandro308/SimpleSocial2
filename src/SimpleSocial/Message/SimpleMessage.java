package SimpleSocial.Message;

import java.io.Serializable;

/**
 * Interfaccia per i messaggi. Include il nome utente del mittente se proviene dal client.
 */
public class SimpleMessage implements Serializable {
    protected String user;
    protected String oAuth;
    protected int senderPort;
    protected Object genericData;

    public SimpleMessage(){}
    public SimpleMessage(String username, String oAuth){
        this.user = username;
        this.oAuth = oAuth;
    }
    public SimpleMessage(String username, String oAuth, Object data){
        this.user = username;
        this.oAuth = oAuth;
        this.genericData = data;
    }
    public SimpleMessage(Object data){
        this.genericData = data;
    }

    public String getUsername(){
        return this.user;
    }
    public String getoAuth(){
        return oAuth;
    }
    public int getSenderPort(){
        return senderPort;
    }
    public Object getData() { return genericData; }
}
