package SocialServer;

import SimpleSocial.Exception.LoginFailException;
import SimpleSocial.Message.Post;
import SimpleSocial.RandomString;
import SocialClient.RemoteMessage.ClientFollowerUpdate;

import java.io.Serializable;
import java.util.Vector;

public class User implements Serializable{
    private static final long serialVersionUID = 1L;
    transient private String host;
    transient private int port = 0;
    private String user;
    private String password;
    private String nick = "";
    transient private String token;
    transient private long loginTime;
    private static RandomString rand = new RandomString(10);
    private Vector<String> friends = new Vector<>();
    private Vector<String> followers = new Vector<>();
    transient private ClientFollowerUpdate stub;

    private Vector<Post> unsentMessage = new Vector<>();

    public User(String user, String password){
        this.user = user;
        this.password = password;
    }

    public void addFollower(String u){
        this.followers.add(u);
        SocialServer.database.writeJSON();
    }

    public Vector<String> getFollowers(){
        return this.followers;
    }

    /**
     * Sovrascrive l'hostname. Usare per salvare l'ultimo hostname,
     * per ricordarsi a chi mandare i messaggi.
     * @param hostname Hostname dell'utente
     */
    public void setHost(String hostname, int port){
        this.host = hostname;
        this.port = port;
    }

    /**
     *
     * @return L'hostname dell'utente
     * @throws NullPointerException se l'hostname non è mai stato settato.
     */
    public String getHost() throws NullPointerException{
        if (this.host == null)
            throw new NullPointerException("Hostname utente "+this.user+" non valido");
        return this.host;
    }
    public int getPort() throws NullPointerException{
        if(this.port == 0){
            throw new NullPointerException("Port del client "+this.user+"non settata");
        }
        return this.port;
    }

    /**
     * @return il nome dell'utente
     */
    public String getUsername(){
        return this.user;
    }

    /**
     * Controlla se la password è corretta
     * @param password - La password da controllare
     * @return oAuth Token se il login è andato a buon fine. Null altrimenti.
     */
    public String checkLogin(String password) throws LoginFailException{
        if(this.password.equals(password)){
            String text = rand.nextString();
            this.loginTime = System.currentTimeMillis();
            this.token = text;
            return text;
        }
        throw new LoginFailException();
    }

    /**
     * Rinnova il token per altri 24 ore
     * @param token Vecchio token
     * @return il nuovo token
     * @throws LoginFailException se il token dichiarato non è valido
     */
    public String extendSession(String token) throws LoginFailException{
        if(!this.checkToken(token)){
            throw new LoginFailException();
        }
        String text = rand.nextString();
        this.loginTime = System.currentTimeMillis();
        this.token = text;
        return text;
    }

    /**
     * Controlla che il token comunicato dal cliente sia corretto.
     * @param token Token da validare.
     * @return true se il token risulta valido. False se errato o scaduto.
     */
    public boolean checkToken(String token) {
        return this.token != null && System.currentTimeMillis() - loginTime < 86400000 /*24 ore*/ && this.token.equals(token);
    }

    /**
     * @return il System.currentMills time di quando è stato registrato il login sul server
     */
    public long getLoginTime(){ return this.loginTime;}

    /**
     * Resetta i campi token e loginTime
     */
    public void logout(){
        this.token = null;
        this.loginTime = 0;
    }

    /**
     * Aggiorna la lista degli amici dell'utente. Tale lista rappresenta le amicizie confermate, non quelle in sospeso.
     * @param friendName Nome dell'amico da aggiungere
     */
    public void addFriend(String friendName){
        if(!friends.contains(friendName)) {
            friends.add(friendName);
            SocialServer.database.writeJSON();
        }
    }

    /**
     * Serve per conoscere la lista di amici dell'utente
     * @return La lista dei nomi degli utenti amici di this
     */
    public Vector<String> getFriends(){
        return friends;
    }

    /**
     * Aggiorna lo stub che permette al server di mandare messaggi di update al client
     * @param stub Oggetto remoto del client
     */
    public void setStub(ClientFollowerUpdate stub){
        this.stub = stub;
    }

    /**
     * @return l'oggetto ClientFollowerUpdate dell'utente. Null se non è impostato.
     */
    public ClientFollowerUpdate getStub(){
        return this.stub;
    }

    public void addUnsentMessage(Post msg){
        this.unsentMessage.add(msg);
    }

    public void goOffline(){
        this.token = null;
        this.loginTime = 0;
        this.stub = null;
        this.host = null;
        this.port = 0;
    }

}


