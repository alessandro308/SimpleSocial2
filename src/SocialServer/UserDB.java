package SocialServer;

import SimpleSocial.Exception.UnregisteredConfigNameException;
import SimpleSocial.Exception.UserExistsException;
import SimpleSocial.Exception.UserNotFoundException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import static SocialServer.SocialServer.config;

/**
 * Database degli utenti:User
 */
public class UserDB implements Serializable{
    private Vector<User> users = new Vector<>();
    transient private Vector<User> online = new Vector<>();
    transient public Vector<User> userOffline = new Vector<>();
    public Map<String, String> friendRequest = new HashMap<>();

    /**
     * Aggiunge un utente alla lista di utenti
     * @param user - Utente da aggiungere
     */
    public synchronized void addUser(User user) throws UserExistsException{
        for(User u:users){
            if(u.getUsername().equals(user.getUsername()))
                throw new UserExistsException();
        }
        this.users.add(user);
        writeJSON();
    }

    /**
     * Rimuove l'utente userName dal DB se esiste. Se l'utente non esiste il DB rimane invariato.
     * @param userName - Name utente da rimuovere
     */
    public synchronized void removeUser(String userName) {
        try {
            users.remove(this.getUserByName(userName));
        } catch (UserNotFoundException ignored) {}
    }

    /**
     * Prende un nome utente o un hostname ricercato ne ritorna l'oggetto.
     * Se ci sono più utente con lo stesso hostname restituisce il primo trovato.
     * @param query - Name dell'utente o hostname
     * @return la lista degli utenti che soddisfano la ricerca
     */
    public Vector<String> searchUser(String query) {
        Vector<String> res = new Vector<>();

        for(User u : users){
            if(query.equals(""))
                res.add(u.getUsername());
            else if(u.getUsername().toLowerCase().contains(query.toLowerCase()))
                res.add(u.getUsername());
        }
        return res;
    }

    /**
     * Cerca un utente per nome e ne ritorna l'oggetto se esiste
     * @param username Nome dell'utente
     * @return L'utente con nome username
     * @throws UserNotFoundException se l'utente non esiste.
     */
    public User getUserByName(String username) throws UserNotFoundException {
        for(User u : users){
            if(u.getUsername().equals(username))
                return u;
        }
        throw new UserNotFoundException("La ricerca per "+username+" non ha restituito nessun utente");
    }

    /**
     * Imposta l'utente userName nello stato di online
     * @param userName Il nome dell'utente da impostare online
     * @throws UserNotFoundException Se l'utente userName non esiste
     */
    public  synchronized void setOnline(String userName) throws UserNotFoundException {
        if(!online.contains(this.getUserByName(userName)))
            online.add(this.getUserByName(userName));
        userOffline.remove(this.getUserByName(userName));
    }
    public synchronized void setOnline(User user){
        if(!online.contains(user))
            online.add(user);
        userOffline.remove(user);
    }

    /**
     * Imposta l'utente userName nello stato di offline
     * @param userName - Il nome dell'utente da impostare online
     * @throws UserNotFoundException Se l'utente userName non esiste
     */
    public synchronized void setOffline(String userName) throws UserNotFoundException{
        this.setOffline(this.getUserByName(userName));
    }

    public synchronized void setOffline(User u){
        u.goOffline();
        online.remove(u);
    }

    /**
     * Restituisce la lista degli utenti online.
     * @return la lista degli utenti online.
     */
    public synchronized Vector<User> getUserOnline(){
        return online;
    }

    /**
     * Verifica se un utente è nello stato di online connettendosi all'utente
     * @param username Nome dell'utente
     * @return true se l'utente è online, false altrimenti.
     * @throws UserNotFoundException se l'utente username non esiste nella base di dati
     */
    public synchronized boolean isOnline(String username) throws UserNotFoundException {
        return this.online.contains(this.getUserByName(username));
    }

    public synchronized boolean isOnline(User user) throws UserNotFoundException {
        return this.online.contains(user);
    }

    /**
     * Funzione chiamata nel KeepAliveServerService per gestire gli utenti online.
     */
    public synchronized Vector<User> updateOnline(){
        online.removeAll(userOffline);
        userOffline.addAll(online);
        return online;
    }

    public synchronized void addFriendRequest(String from, String to) throws UserNotFoundException{
        friendRequest.put(to, from);
        writeJSON();
    }


    public void writeJSON(){
        FileOutputStream fout;
        try {
            File f = new File((String) config.getValue("DBNAME"));
            if(!f.exists()){
                f.createNewFile();
            }
            fout = new FileOutputStream(f);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(this);
        } catch (UnregisteredConfigNameException | IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return;
    }

    public void resumeFromSerialization(){
        if(this.online == null){
            this.online = new Vector<>();
        }
        if(this.userOffline == null){
            this.userOffline = new Vector<>();
        }
    }

}
