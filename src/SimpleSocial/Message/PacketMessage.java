package SimpleSocial.Message;

import java.io.Serializable;

/**
 * Classe dei pacchetti inviati.
 */
public class PacketMessage implements Serializable{
    public enum MessageType{
        KEEPALIVE,
        FRIENDREQUEST, FRIENDREQUEST_SENT, FRIENDREQUEST_CONFIRM,
        FRIENDLIST, FRIENDLISTRESPONSE,
        REGISTER, LOGIN, LOGOUT, LOGINRESPONSE, NOTLOGGED,
        SEARCHUSER, SEARCHUSERRESPONSE,
        ERROR, SUCCESS,
        TOKENUPDATE,
        SHARETHIS, FOLLOWREQUEST
    }

    private SimpleMessage message;
    private MessageType type;

    public PacketMessage(SimpleMessage msg, MessageType type){
        if(msg != null)
            this.message = msg;
        else
            this.message = new SimpleMessage();
        this.type = type;
    }

    public PacketMessage(){
    }

    public SimpleMessage getMessage(){
        return this.message;
    }

    public MessageType getType(){
        return this.type;
    }

}
