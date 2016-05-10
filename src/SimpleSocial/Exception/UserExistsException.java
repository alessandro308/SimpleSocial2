package SimpleSocial.Exception;

/**
 * Created by alessandro on 05/05/16.
 */
public class UserExistsException extends Exception {
    public UserExistsException(){
        super();
    }
    public UserExistsException(String message){
        super(message);
    }
}
