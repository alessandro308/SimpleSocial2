package SimpleSocial.Exception;

/**
 * Created by alessandro on 06/05/16.
 */
public class ConfigValueAlreadyExistsException extends RuntimeException {
    public ConfigValueAlreadyExistsException(String message){
        super(message);
    }
    public ConfigValueAlreadyExistsException(){
        super();
    }
}
