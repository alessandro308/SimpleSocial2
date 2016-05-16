package SimpleSocial.Message;

/**
 * Classe che gestisce l'update da parte degli utenti.
 */
public class Post {
    public String user;
    public String message;

    public Post(String user, String message){
        this.user = user;
        this.message = message;
    }
}
