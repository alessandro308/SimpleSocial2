package SocialServer;

import SimpleSocial.Config;
import SimpleSocial.Exception.LoginFailException;
import SimpleSocial.Exception.UnregisteredConfigNameException;
import SimpleSocial.Exception.UserExistsException;
import SimpleSocial.Exception.UserNotFoundException;
import SimpleSocial.Message.*;
import SimpleSocial.ObjectSocketChannel;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.util.Vector;

/**
 * Thread che viene avviato e che processa il messaggio ricevuto dal client. Se il messaggio è di tipo FRIENDREQUEST
 * viene gestito direttamente nel ServerMain in quanto la registrazione di una socketchannel sul selector, in caso in cui
 * il selector è bloccato sulla .select(), porta in deadlock il sistema.
 */
class PacketMessageHandler implements Runnable {
    SelectionKey sender;
    PacketMessage p;

    /**
     * Thread di lettura e gestione dei pacchetti arrivati
     * @param pkt - Pacchetto ricevuto
     * @param sender - Mittente. Necessario per le risposte.
     */
    public PacketMessageHandler(SelectionKey sender, PacketMessage pkt){
        this.sender = sender;
        this.p = pkt;
    }


    public void run() {
        /* La verifica che l'utente sia loggato è fatta dal main che avvia questo thread*/
        UserDB database = SocialServer.database;
        Config config = SocialServer.config;

        if(p.getType().equals(PacketMessage.MessageType.REGISTER)){
            RegisterSimpleMessage msg = (RegisterSimpleMessage) p.getMessage();
            try{
                if(msg.getUsername().equals("") || msg.getUsername().length() < 3){
                    sendError(sender, "Nome utente troppo corto");
                    return;
                }

                database.addUser(new User(msg.getUsername(), msg.getPassword()));
                sendPkt(sender, PacketMessage.MessageType.SUCCESS, new SimpleMessage());
            } catch (UserExistsException e){
                sendError(sender, "L'utente esiste già");
            }
            return;
        }

        if(p.getType().equals(PacketMessage.MessageType.LOGIN)){
            LoginSimpleMessage msg = (LoginSimpleMessage) p.getMessage();
            try{
                User u = database.getUserByName(msg.getUsername());
                String oAuth = u.checkLogin(msg.getPassword());
                u.setHost(msg.getUserHostname(), msg.getUserPORT());
                database.setOnline(u.getUsername());
                sendPkt(sender, PacketMessage.MessageType.LOGINRESPONSE, new LoginSimpleMessage(oAuth, u.getLoginTime(), (String) config.getValue("MULTICAST_IP")));

            }catch (UserNotFoundException e){
                sendError(sender, "L'utente non esiste nel database. Registrarsi.");
            } catch (LoginFailException e){
                sendError(sender, "Login fallito. Riprovare.");
            } catch (UnregisteredConfigNameException e){
                System.err.println("Non è possibile comunicare il multicast IP, non è configurato correttamente");
                e.printStackTrace();
            }
            return;
        }

        if(p.getType().equals(PacketMessage.MessageType.TOKENUPDATE)){
            SimpleMessage msg = p.getMessage();
            try{
                User u = database.getUserByName(msg.getUsername());
                sendPkt(sender, PacketMessage.MessageType.LOGINRESPONSE,
                        new LoginSimpleMessage(u.extendSession(msg.getoAuth()),
                                u.getLoginTime(),
                                (String) config.getValue("MULTICAST_IP")));
            } catch (UserNotFoundException e){
                sendError(sender, "L'utente non esiste nel database. Registrarsi.");
            } catch (LoginFailException e){
                sendError(sender, "oAuth token non valido.");
            } catch (UnregisteredConfigNameException ignored){}
            return;
        }

        if(p.getType().equals(PacketMessage.MessageType.LOGOUT)){
            LogoutSimpleMessage msg = (LogoutSimpleMessage) p.getMessage();
            try{
                User u = database.getUserByName(msg.getUsername());
                database.setOffline(u);
                u.logout();
            } catch (UserNotFoundException ignored){}
            return;
        }

        if(p.getType().equals(PacketMessage.MessageType.KEEPALIVE)){
            try{
                User u = database.getUserByName(p.getMessage().getUsername());
                database.setOnline(u);
            } catch (UserNotFoundException ignored){}
            return;
        }

        if(p.getType().equals(PacketMessage.MessageType.SEARCHUSER)){
            System.out.println("RICERCA UTENTE");
            Vector<String> found = database.searchUser(((SearchUserSimpleMessage) p.getMessage()).getQuery());
            sendPkt(sender, PacketMessage.MessageType.SEARCHUSERRESPONSE, new SearchUserSimpleMessage(found));
            return;
        }

        if(p.getType().equals(PacketMessage.MessageType.FRIENDLIST)){
            try{
                User u = database.getUserByName(p.getMessage().getUsername());
                sendPkt(sender, PacketMessage.MessageType.FRIENDLISTRESPONSE, new SimpleMessage(u.getFriends()));
            }catch (UserNotFoundException ignored){}
            return;
        }

        if(p.getType().equals(PacketMessage.MessageType.SHARETHIS)){
            try{
                User u = database.getUserByName(p.getMessage().getUsername());
                for(String follower : u.getFollowers()){
                    try{
                        database.getUserByName(follower).getStub().addMessage(u.getUsername(), (String) p.getMessage().getData());
                    }catch (NullPointerException e){
                        /* L'utente non è online, infatti lo Stub non è settato e restituisce nullPointerException*/
                        database.getUserByName(follower).addUnsentMessage(new Post(u.getUsername(), (String) p.getMessage().getData()));
                    }
                }
            }
            catch (UserNotFoundException ignored){}
            catch (RemoteException e) {
                System.err.println("Errore remoto. "+e.getMessage());
            }
            return;
        }
    }

    /**
     * Spedisce un pacchetto di errore.
     * @param to - destinatario del pacchetto
     * @param cause - Causa dell'errore
     */
    public static void sendError(SelectionKey to, String cause){
        sendPkt(to, PacketMessage.MessageType.ERROR, new ErrorSimpleMessage(cause));
    }

    /**
     * Spedisce un pacchetto generico.
     * @param to - Destinatario del pacchetto
     * @param type - Tipo del pacchetto spedito
     * @param msg - Messaggio contenuto nel pacchetto
     */
    public static void sendPkt(SelectionKey to, PacketMessage.MessageType type, SimpleMessage msg){
        ObjectSocketChannel client;
        if((client = (ObjectSocketChannel) to.attachment()) == null)
            client = new ObjectSocketChannel((SocketChannel) to.channel());

        client.setObjectToSend(new PacketMessage(msg, type));

        if(to.attachment() == null)
            to.attach(client);

        to.interestOps(SelectionKey.OP_WRITE);
        SocialServer.selector.wakeup();
    }
}
