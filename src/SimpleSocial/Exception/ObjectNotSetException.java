package SimpleSocial.Exception;

/**
 * Eccezione lanciata se un oggetto che doveva essere settato è invece null
 */
public class ObjectNotSetException extends RuntimeException {
    public ObjectNotSetException(String message){
        super(message);
    }
    public ObjectNotSetException(){super();}
}
