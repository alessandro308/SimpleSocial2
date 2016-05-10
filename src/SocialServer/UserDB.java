package SocialServer;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import SimpleSocial.Exception.*;
import com.sun.xml.internal.xsom.impl.scd.Iterators;

/**
 * Created by alessandro on 05/05/16.
 */
public class UserDB {
    private Vector<User> users = new Vector<>();
    private Vector<User> online = new Vector<>();
    public Vector<User> userOffline = new Vector<>();
    public Map<User, User> friendRequest = new HashMap<>();

    /**
     * Aggiunge un utente alla lista di utenti
     * @param user - Utente da aggiungere
     */
    public void addUser(User user) throws UserExistsException{
        for(User u:users){
            if(u.getUsername().equals(user.getUsername()))
                throw new UserExistsException();
        }
        this.users.add(user);
    }

    /**
     * Rimuove l'utente userName dal DB se esiste. Se l'utente non esiste il DB rimane invariato.
     * @param userName - Name utente da rimuovere
     */
    public void removeUser(String userName) {
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
        online.remove(this.getUserByName(userName));
    }
    public synchronized void setOffline(User u){
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
     * Verifica se un utente è nello stato di online
     * @param username Nome dell'utente
     * @return true se l'utente è online, false altrimenti.
     * @throws UserNotFoundException se l'utente username non esiste nella base di dati
     */
    public synchronized boolean isOnline(String username) throws UserNotFoundException {
        return this.online.contains(this.getUserByName(username));
    }

    /**
     * Funzione chiamata nel KeepAliveServerService per gestire gli utenti online.
     */
    public synchronized void updateOnline(){
        online.removeAll(userOffline);
        userOffline.addAll(online);
        System.out.println("-Utenti Online-");
        for(User u : online)
            System.out.println(u.getUsername());
    }

    public void addFriendRequest(String from, String to) throws UserNotFoundException{
        friendRequest.put(this.getUserByName(to), this.getUserByName(from));
    }

}
