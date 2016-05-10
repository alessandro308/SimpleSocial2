package SimpleSocial.Exception;

/**
 * Created by alessandro on 09/05/16.
 */
public class ObjectNotSetException extends RuntimeException {
    public ObjectNotSetException(String message){
        super(message);
    }
    public ObjectNotSetException(){super();}
}
