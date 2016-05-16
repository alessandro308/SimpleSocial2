package SocialClient;

import SimpleSocial.Config;
import SimpleSocial.Exception.UnregisteredConfigNameException;
import SimpleSocial.Exception.UserExistsException;
import SimpleSocial.Message.*;
import SocialClient.RemoteMessage.ClientFollowerUpdate;
import SocialClient.RemoteMessage.ClientFollowerUpdateImpl;
import SocialServer.RemoteMessage.FollowerManager;
import SocialServer.RemoteMessage.FollowerManagerImpl;
import SimpleSocial.ObjectSocket;
import SocialServer.UserDB;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Vector;

public class SocialClient {
    public Config config = new Config("client_config.txt");
    MulticastSocket multiServer;
    Thread keepAliveService;
    Listener listener = new Listener();
    Thread listenerThr;
    Registry registry;
    ClientFollowerUpdateImpl updateHandler = new ClientFollowerUpdateImpl();
    ClientFollowerUpdate stub;

    public SocialClient(){
        if(!config.isSet("SERVER_HOSTNAME")){
            config.setConfig("SERVER_HOSTNAME", "localhost");
        }
        if(!config.isSet("SERVER_PORT")){
            config.setConfig("SERVER_PORT", 2000);
        }
        try {
            registry = LocateRegistry.getRegistry((String) config.getValue("REGISTRY_HOST"));
            FollowerManager manager = (FollowerManager) UnicastRemoteObject.exportObject(new FollowerManagerImpl(new UserDB()), 0);
            stub = (ClientFollowerUpdate) UnicastRemoteObject.exportObject(updateHandler, 0);
            //registry.bind(FollowerManager.OBJECT_NAME, manager); //Testo se il server ha inserito l'emento e se l'RMIRegistry è lo stesso
        } catch (UnregisteredConfigNameException e) {
            System.err.println("Errore: REGISTRY_HOST non impostato.");
        } catch (RemoteException e){
            System.err.println("Errore avvio RMI Registry e co. - Il server ha avviato rmiregistry?" + e.getMessage());
        }
    }

    /**
     * Funzione principale del SocialClient.
     * Effettua un ciclo continuo chiedendo quale operazione si vuole effettuare invocandola.
     */
    public void start(){

        while(!config.isSet("REGISTERED")){ // Fase di Registrazione
            String psw;
            String user;
            System.out.println("REGISTRAZIONE ** PRIMO AVVIO");
            try{
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Enter username: ");
                user = br.readLine().replaceAll("\\s+","");
                System.out.println("Enter password: ");
                psw = br.readLine().replaceAll("\\s+","");
            } catch (IOException e){
                System.err.println("Errore. Provare a riavviare l'applicazione.");
                return;
            }
            if(!user.equals("") && !psw.equals(""))
                try{
                    this.register(user, psw);
                }catch (UserExistsException e){
                    System.out.println("Il nome utente scelto esiste già");
                }
            else
                System.out.println("Lo user e la psw non possono essere vuoti");
        }

        int opt = -1;
        while(opt != 0){
            while(!this.isValidSession()){ // Fase di login
                try {
                    System.out.println("**EFFETTUA IL LOGIN**");
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    System.out.println("Inserisci username: ");
                    String user = br.readLine().replaceAll("\\s+", "");
                    System.out.println("Inserisci password: ");
                    String psw = br.readLine().replaceAll("\\s+", "");
                    if(user.length() > 0 && psw.length() > 0 && !login(user, psw))
                        System.out.println("User o psw errati. Ritenta, sarai più fortunato!");
                }catch (IOException e){
                    e.printStackTrace();
                    System.err.println(e.getMessage());
                }
            }
            if(config.isSet("OAUTH")){
                printMan();
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                try{
                    opt = Integer.parseInt(br.readLine());
                } catch (IOException | NumberFormatException e){
                    System.out.println("Inserita scelta non valida");
                    opt = -1;
                }
                switch (opt) {
                    case 0:
                        config.saveOnFile();
                        System.exit(0);
                        break;
                    case 1:
                        logout();
                        System.out.println("Logout avvenuto con successo");
                        break;
                    case 2:
                        try {
                            System.out.print("Inserisci nome utente da ricercare: ");
                            String user = br.readLine();
                            Vector<String> found = searchUser(user);
                            System.out.println("Trovati " + found.size() + " utenti:");
                            found.forEach(System.out::println);
                        } catch (IOException e) {
                            System.err.println("Errore lettura System In");
                        }

                        break;
                    case 3: //Rinnovo sessione
                        System.out.println("Rinnovo token avvenuto con successo. Token di autorizzazione: " + newToken());
                        break;
                    case 4: //Richiesta lista amici
                        try {
                            Vector<String> list = getFriendList();
                            System.out.println("\n\r**LISTA AMICI:**");
                            for (String n : list)
                                System.out.println(n);
                        } catch (UnregisteredConfigNameException e) {
                            System.err.println("Qualcosa è andato storto. Riprovare. // " + e.getMessage());
                        } catch (IOException e) {
                            System.err.println("Errore " + e.getMessage() + " nella comunicazione con il server. Riprovare");
                        }
                        break;
                    case 5: //Richiesta amicizia
                        try {
                            System.out.print("Inserisci nome dell'amico: ");
                            String user = br.readLine();
                            if (user.length() < 0) {
                                System.out.println("Nome non valido");
                                break;
                            }
                            try {
                                System.out.println(requestFriendship(user));
                            } catch (UnregisteredConfigNameException ignored) {
                            } catch (IOException e) {
                                System.out.println("Problemi di comunicazione. Riprovare.");
                            }
                        } catch (IOException e) {
                            System.err.println("Errore lettura System In");
                        }
                        break;
                    case 6: //Accetta richieste di amicizia in sospeso
                        Vector<String> friends = listener.getFriendRequest();
                        for (Iterator<String> iterator = friends.iterator(); iterator.hasNext(); ) {
                            String f = iterator.next();
                            System.out.println("Nuova richiesta di amicizia da " + f + ". Vuoi accettare? (Y/ignore)");
                            try {
                                if (br.readLine().toLowerCase().equals("y")) {
                                    if (acceptsFriendshipRequest(f).equals("OK")) {
                                        System.out.println("Amico aggiunto con successo.");
                                        iterator.remove();
                                    } else {
                                        System.out.println("Errore nell'accettazione dell'amicizia. Boh!");
                                    }
                                }
                            } catch (IOException e) {
                                System.out.println("Errori di comunicazione. Riprovare.");
                            } catch (UnregisteredConfigNameException ignored) {
                            }
                        }
                        break;
                    case 7: // Pubblica contenuto
                        try {
                            System.out.print(config.getValue("USER") + " a cosa stai pensando?");
                            String msg = br.readLine();
                            shareMessage(msg);
                            System.out.println("Contenuto pubblicato con successo.");
                        } catch (UnregisteredConfigNameException e) {
                            logout();
                        } catch (IOException e) {
                            System.err.println("Errore di comunicazione: " + e.getMessage() + " Riprovare");
                        }
                        break;
                    case 8: //Inizia a seguire un nuovo amico - RMI
                        try {
                            Vector<String> friendsList = getFriendList();
                            System.out.println("Quale amico vuoi seguire? (lascia bianco per annullare)\n Amici:");
                            friendsList.forEach(System.out::println);
                            System.out.print(":");
                            String toFollow = null;
                            while (toFollow == null) {
                                String tmp = br.readLine();
                                if (tmp.equals(""))
                                    toFollow = "";
                                if (friendsList.contains(tmp)) {
                                    toFollow = tmp;
                                } else {
                                    System.out.println("Nome dell'amico non valido. Riprova.");
                                }
                            }
                            if (toFollow.equals(""))
                                break;
                            sendFollowRequest(toFollow);
                        } catch (UnregisteredConfigNameException e) {
                            logout();
                        } catch (IOException e) {
                            System.err.println("Errore di comunicazione: " + e.getMessage());
                        }
                        break;
                    case 9: //Ricevi aggiornamenti da coloro che segui
                        for(Post p : updateHandler.getUnreadMessage()){
                            System.out.println("Da "+p.user+"\n"+p.message);
                        }
                        updateHandler.removeAllMessage();
                }
            }
        }
    }

    /**
     * Inizia a seguire un utente. Funzione che utilizza RMI
     * @param toFollow nome utente da seguire
     */
    protected void sendFollowRequest(String toFollow){
        try {
            FollowerManager manager = (FollowerManager) registry.lookup(FollowerManager.OBJECT_NAME);
            String user = (String) config.getValue("USER");
            String oAuth = (String) config.getValue("OAUTH");
            manager.follow(new PacketMessage(new SimpleMessage(user, oAuth, toFollow), PacketMessage.MessageType.FOLLOWREQUEST));
        } catch (RemoteException e) {
            System.err.println("Errore RMI. "+e.getMessage());
        } catch (NotBoundException e) {
            System.err.println("Il server avrà inserito il Manager nel registro? Secondo me no! "+e.getMessage());
        } catch (UnregisteredConfigNameException e){
            logout();
        }
    }

    /**
     * Pubblica un messaggio sul SocialServer
     * @param msg Messaggio da pubblicare
     * @throws UnregisteredConfigNameException Se il SERVER_HOSTNAME non è correttamente configurato
     * @throws IOException Se ci sono stati problemi di recezione.
     */
    protected void shareMessage(String msg) throws UnregisteredConfigNameException, IOException {
        ObjectSocket skt = new ObjectSocket(InetAddress.getByName((String) config.getValue("SERVER_HOSTNAME")), (Integer) config.getValue("SERVER_PORT"));
        SimpleMessage message = new SimpleMessage((String) config.getValue("USER"),
                (String) config.getValue("OAUTH"), msg);
        skt.writeObject(new PacketMessage(message, PacketMessage.MessageType.SHARETHIS));
    }

    /**
     * Accetta una richiesta di amicizia. Sii amici di tutti! ;)
     * @param friend Nome dell'amico
     * @return "OK" se è andato tutto bene. Altrimenti una stringa con l'errore.
     * @throws UnregisteredConfigNameException se non è stato settato bene il server ip o port
     * @throws IOException Se è occorso un errore di comunicazione
     */
    protected String acceptsFriendshipRequest(String friend) throws UnregisteredConfigNameException, IOException{
        ObjectSocket skt = new ObjectSocket(InetAddress.getByName((String) config.getValue("SERVER_HOSTNAME")), (Integer) config.getValue("SERVER_PORT"));
        SimpleMessage msg = new SimpleMessage((String) config.getValue("USER"),
                                    (String) config.getValue("OAUTH"), friend);
        skt.writeObject(new PacketMessage(msg, PacketMessage.MessageType.FRIENDREQUEST_CONFIRM));
        PacketMessage reply = (PacketMessage) skt.readObject();
        if(reply.getType().equals(PacketMessage.MessageType.SUCCESS)){
            return "OK";
        }

        return ((ErrorSimpleMessage) reply.getMessage()).getCause();
    }

    /**
     * Inoltra una richiesta di amicizia verso l'utente friendName
     * @param friendName Nome dell'utente a cui richiedere l'amicizia
     * @return Un messaggio di risposta del server.
     * @throws UnregisteredConfigNameException Se non è correttamente configurato il server config params
     * @throws IOException Se ci sono stati errori di comunicazione
     */
    protected String requestFriendship(String friendName) throws UnregisteredConfigNameException, IOException {
        if(friendName.equals(config.getValue("USER")))
            return "Non puoi aggiungere te stesso";
        ObjectSocket skt = new ObjectSocket(InetAddress.getByName((String) config.getValue("SERVER_HOSTNAME")), (Integer) config.getValue("SERVER_PORT"));
        FriendRequestSimpleMessage msg = new FriendRequestSimpleMessage((String) config.getValue("USER"),
                                                                        (String) config.getValue("OAUTH"), friendName);
        skt.writeObject(new PacketMessage(msg, PacketMessage.MessageType.FRIENDREQUEST));
        PacketMessage reply = (PacketMessage) skt.readObject();
        if(reply.getType().equals(PacketMessage.MessageType.FRIENDREQUEST_SENT)){
            return (String) reply.getMessage().getData();
        }
        return ((ErrorSimpleMessage) reply.getMessage()).getCause();
    }

    /**
     * Ottiene la lista degli amici
     * @return la lista degli amici
     * @throws UnregisteredConfigNameException se non è configurato l'hostname o l'ip del server
     * @throws IOException se c'è stato un problema di comunicazione
     */
    protected Vector<String> getFriendList() throws UnregisteredConfigNameException, IOException {
        ObjectSocket skt = new ObjectSocket(InetAddress.getByName((String) config.getValue("SERVER_HOSTNAME")), (Integer) config.getValue("SERVER_PORT"));
        SimpleMessage msg = new SimpleMessage((String) config.getValue("USER"), (String) config.getValue("OAUTH"));
        skt.writeObject(new PacketMessage(msg, PacketMessage.MessageType.FRIENDLIST));
        PacketMessage reply = (PacketMessage) skt.readObject();
        if(reply.getType().equals(PacketMessage.MessageType.FRIENDLISTRESPONSE)){
            Vector<String> list = (Vector<String>) reply.getMessage().getData();
            return list;
        }
        return null;
    }

    /**
     * Funzione per richiedere l'aggiornamento del token
     */
    protected String newToken(){
        try{
            ObjectSocket skt = new ObjectSocket(InetAddress.getByName((String) config.getValue("SERVER_HOSTNAME")), (Integer) config.getValue("SERVER_PORT"));
            SimpleMessage msg = new SimpleMessage((String) config.getValue("USER"), (String) config.getValue("OAUTH"));
            skt.writeObject(new PacketMessage(msg, PacketMessage.MessageType.TOKENUPDATE));
            PacketMessage reply = (PacketMessage) skt.readObject();
            if(reply.getType().equals(PacketMessage.MessageType.LOGINRESPONSE)){
                config.updateConfig("OAUTH", (reply.getMessage()).getoAuth());
                config.updateConfig("OAUTH_TIME", ((LoginSimpleMessage) reply.getMessage()).getoAuthTime());
                return (String) config.getValue("OAUTH");
            }
        }catch (UnregisteredConfigNameException e){
            System.err.println("Qualcosa è andato storto. Riprovare. // "+e.getMessage());
            logout();
        }catch (IOException e){
            System.err.println("Errore "+e.getMessage()+" nella comunicazione con il server. Riprovare");
        }
        return "";
    }

    /**
     * Funzione che gestite il login e la comunicazione con il server.
     * @return true se il login avvie con successo. False altrimenti.
     */
    protected boolean login(String user, String psw){
        try{
            config.updateConfig("USER", user);
        }catch (UnregisteredConfigNameException e){
            config.setConfig("USER", user);
        }
        listenerThr = new Thread(listener);
        listenerThr.start();
        try{
            ObjectSocket skt = new ObjectSocket(InetAddress.getByName((String) config.getValue("SERVER_HOSTNAME")), (Integer) config.getValue("SERVER_PORT"));
            SimpleMessage msg = new LoginSimpleMessage(user, psw, listener.getHostname(), listener.getIP(), stub);

            skt.writeObject(new PacketMessage(msg, PacketMessage.MessageType.LOGIN));

            PacketMessage reply = (PacketMessage) skt.readObject();
            if(reply.getType() == PacketMessage.MessageType.LOGINRESPONSE){
                config.reConfig("OAUTH", (reply.getMessage()).getoAuth());
                System.out.println("Login avvenuto con successo. Token di autorizzazione: "+config.getValue("OAUTH"));
                Long time = ((LoginSimpleMessage) reply.getMessage()).getoAuthTime();
                config.reConfig("OAUTH_TIME", time);
                config.reConfig("MULTICAST_IP", ((LoginSimpleMessage) reply.getMessage()).getMulticastIP());
                try{
                    multiServer = new MulticastSocket((Integer) config.getValue("MULTICAST_PORT"));

                    InetAddress multicastGroup = InetAddress.getByName((String) config.getValue("MULTICAST_IP"));
                    multiServer.joinGroup(multicastGroup);
                    keepAliveService = new Thread(new KeepAliveUserService(multiServer, config));
                    keepAliveService.start();
                }catch (UnregisteredConfigNameException ignored){}
                return true;
            }else if(reply.getType() == PacketMessage.MessageType.ERROR){
                return false;
            }
        } catch (IOException e){
            e.printStackTrace();
            System.err.println("Errore comunicazione con il server. Riprovare. "+e.getMessage());
        } catch (UnregisteredConfigNameException e){
            System.err.println("Non tutte le configurazioni sono settate opportunamente");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Funzione di logout. Comunica al server di voler effettuare il logout e azzera i campi di config.
     * @return true se il logout ha successo. False altrimenti.
     */
    protected boolean logout(){
        try{
            ObjectSocket skt = new ObjectSocket(InetAddress.getByName((String) config.getValue("SERVER_HOSTNAME")), (int) config.getValue("SERVER_PORT"));
            SimpleMessage msg = new LogoutSimpleMessage((String) config.getValue("USER"), (String) config.getValue("OAUTH"));

            PacketMessage pkt = new PacketMessage(msg, PacketMessage.MessageType.LOGOUT);
            skt.writeObject(pkt);
            config.removeKey("USER");
            config.removeKey("OAUTH");
            config.removeKey("OAUTH_TIME");

            multiServer.leaveGroup(InetAddress.getByName((String) config.getValue("MULTICAST_IP")));
            keepAliveService.interrupt();
            config.removeKey("MULTICAST_IP");

            return true;
        } catch (IOException | UnregisteredConfigNameException e){
            System.err.println("Errore durante la fase di logout "+e.getMessage());
        }
        return false;
    }

    /**
     * Funzione di registrazione del nuovo utente. Eseguita all'avvio dell'app se non è settato il campo USER in config.
     * @return true se la registrazione avviene con successo. False altrimenti
     */
    protected boolean register(String user, String psw) throws UserExistsException{
        try {
            if(user.length() == 0 || psw.length() == 0)
                throw new UserExistsException("User o psw vuoti.");
            ObjectSocket server = new ObjectSocket(InetAddress.getByName((String) config.getValue("SERVER_HOSTNAME")), (int) config.getValue("SERVER_PORT"));
            RegisterSimpleMessage msg = new RegisterSimpleMessage(user, psw);
            PacketMessage pkt = new PacketMessage(msg, PacketMessage.MessageType.REGISTER);
            server.writeObject(pkt);

            pkt = (PacketMessage) server.readObject();

            if(pkt.getType()== PacketMessage.MessageType.ERROR){
                server.close();
                throw new UserExistsException( ((ErrorSimpleMessage) pkt.getMessage()).getCause() );
            }
            if(pkt.getType() == PacketMessage.MessageType.SUCCESS){
                config.setConfig("REGISTERED", "true");
                System.out.println("Registrazione avvenuta con successo\n\r");
                server.close();
                return true;
            }
        } catch (IOException e) {
            System.err.println("Errore connessione al server. Controllare le impostazioni di connessione al server. " + e.getMessage());
        } catch (UnregisteredConfigNameException e){
            System.err.println("Vedere le configurazioni. Mancano server ip e/o hostname");
        }
        return false;
    }


    /**
     * Ricerca un utente per nome, contattando il server e stampando al risposta.
     */
    protected Vector<String> searchUser(String user){
        try{
            ObjectSocket server = new ObjectSocket(InetAddress.getByName((String) config.getValue("SERVER_HOSTNAME")), (Integer) config.getValue("SERVER_PORT"));

            SimpleMessage msg = new SearchUserSimpleMessage((String) config.getValue("USER"),(String) config.getValue("OAUTH"), user);
            server.writeObject(new PacketMessage(msg, PacketMessage.MessageType.SEARCHUSER));
            PacketMessage reply = (PacketMessage) server.readObject();
            if(reply.getType() == PacketMessage.MessageType.SEARCHUSERRESPONSE){
                return ((SearchUserSimpleMessage) reply.getMessage()).response();
            }else {
                System.out.println("Qualcosa è andato storto. Riprova.");
            }
        } catch (IOException ignored){
        } catch (UnregisteredConfigNameException e){
            System.err.println("Controllare configurazione per il server");
        }
        return new Vector<>();

    }
    /**
     * Funzione di verifica del token oAuth.
     * @return Ritorna true se il token è ancora valido. False altrimenti.
     */
    protected boolean isValidSession(){
        try {
            return config.isSet("OAUTH") && (Long.parseLong(config.getValue("OAUTH_TIME").toString())-System.currentTimeMillis()) < 86400000;
        } catch (UnregisteredConfigNameException e) {
            return false;
        }
    }

    /**
     * Stampa le opzioni per gestire il client.
     */
    protected void printMan(){
        System.out.println();
        System.out.println("*****************************");
        System.out.println("SCEGLIERE COSA SI VUOLE FARE");
        System.out.println("0 - Exit");
        System.out.println("1 - Logout");
        System.out.println("2 - Search User");
        System.out.println("3 - Rinnovo del token di sessione");
        System.out.println("4 - Stampa lista dei miei amici");
        System.out.println("5 - Richiesti amico");
        if(listener.getFriendRequest().size() > 0)
            System.out.println("6 - Vedi richieste di amicizia in sospeso");
        else
            System.out.println("_ - Nessuna nuova richiesta di amicizia");
        System.out.println("7 - Pubblica contenuto");
        System.out.println("8 - Inizia a seguire un amico");
        if(updateHandler.getUnreadMessageCount() > 0)
            System.out.println("9 - Nuovi aggiornamenti da coloro che segui");
        System.out.println("*****************************");
    }

    public static void main(String[] args){
        new SocialClient().start();
    }


}
