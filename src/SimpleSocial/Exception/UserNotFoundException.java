package SimpleSocial.Exception;

/**
 * Created by alessandro on 05/05/16.
 */
public class UserNotFoundException extends Throwable {

    public UserNotFoundException(){
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}
