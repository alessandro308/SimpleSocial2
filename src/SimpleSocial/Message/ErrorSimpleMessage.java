package SimpleSocial.Message;

/**
 * Created by alessandro on 06/05/16.
 */
public class ErrorSimpleMessage extends SimpleMessage {
    private String cause;

    public ErrorSimpleMessage(String cause){
        this.cause = cause;
    }
    public String getCause(){
        return cause;
    }
}
