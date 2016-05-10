package SimpleSocial.Message;

import java.util.Vector;

/**
 * Created by alessandro on 08/05/16.
 */
public class SearchUserSimpleMessage extends SimpleMessage {
    private Vector<String> usersFound;
    private String query;
    public SearchUserSimpleMessage(String username, String oAuth, String query){
        this.user = username;
        this.oAuth = oAuth;
        this.query = query;
    }
    public String getQuery(){
        return this.query;
    }

    public SearchUserSimpleMessage(Vector<String> users){
        this.usersFound = users;
    }
    public Vector<String> response(){
        return this.usersFound;
    }
}
