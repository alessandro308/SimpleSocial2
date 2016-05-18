package SocialServer;

import SimpleSocial.Config;
import SimpleSocial.Exception.LoginFailException;
import SimpleSocial.Exception.UnregisteredConfigNameException;
import SimpleSocial.Exception.UserExistsException;
import SimpleSocial.Exception.UserNotFoundException;
import SimpleSocial.Message.*;
import SimpleSocial.ObjectSocketChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.util.Vector;

/**
 * Created by alessandro on 18/05/16.
 */
public class PacketMessageHandler implements Runnable {
    SelectionKey sender;
    PacketMessage p;

    /**
     * Funzione di lettura e gestione dei pacchetti arrivati
     * @param pkt - Pacchetto ricevuto
     * @param sender - Mittente. Necessario per le risposte.
     */
    public PacketMessageHandler(SelectionKey sender, PacketMessage pkt){
        this.sender = sender;
        this.p = pkt;
    }


    public void run() {
        UserDB database = SocialServer.database;
        Config config = SocialServer.config;
        try{
            if(!p.getType().equals(PacketMessage.MessageType.REGISTER)) {
                if (p.getType().equals(PacketMessage.MessageType.LOGIN) ||
                        database.getUserByName(p.getMessage().getUsername()).checkToken(p.getMessage().getoAuth())) {
                    database.setOnline(database.getUserByName(p.getMessage().getUsername()));
                } else {
                    sendError(sender, "Utente non autenticato correttamente.");
                }
            }
        }catch (UserNotFoundException closeAll){
            sendError(sender, "Utente non riconosciuto.");
        }

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

        if(p.getType().equals(PacketMessage.MessageType.FRIENDREQUEST)){
            try{
                User u = database.getUserByName(p.getMessage().getUsername());
                FriendRequestSimpleMessage msg = (FriendRequestSimpleMessage) p.getMessage();
                try{
                    if(database.isOnline(msg.getFriend())){
                        User friend = database.getUserByName(msg.getFriend());
                        SocketChannel friendSkt = SocketChannel.open();
                        try{
                            friendSkt.connect(new InetSocketAddress(friend.getHost(), friend.getPort()));
                            friendSkt.configureBlocking(false);
                            database.addFriendRequest(msg.getUsername(), msg.getFriend());
                            sendPkt(sender, PacketMessage.MessageType.FRIENDREQUEST_SENT,
                                    new SimpleMessage("Richiesta di amicizia inoltrata correttamente."));
                            ObjectSocketChannel obj = new ObjectSocketChannel(friendSkt);
                            obj.setObjectToSend(new PacketMessage(new SimpleMessage(u.getUsername()), PacketMessage.MessageType.FRIENDREQUEST_CONFIRM));
                            friendSkt.register(SocialServer.selector, SelectionKey.OP_WRITE, obj);
                        }catch (NullPointerException e){
                            sendError(sender, "Errore: amico non raggiunbile. Non si conosce l'hostname e l'ip dell'amico.");
                        }

                    }else{
                        sendError(sender, "Errore, l'utente non è attualmente online");
                    }
                }catch (IOException e){
                    sendError(sender, "Errore: "+e.getMessage());
                }
            }catch (UserNotFoundException e){
                sendError(sender, "Errore: Utente non valido");
            }
            return;
        }

        if(p.getType().equals(PacketMessage.MessageType.FRIENDREQUEST_CONFIRM)){
            try {
                User u1 = database.getUserByName(p.getMessage().getUsername());
                User u2 = database.getUserByName((String) p.getMessage().getData());
                if(database.friendRequest.get(u1).getUsername().equals(u2.getUsername())) { //Confermo che c'era una cosa pendente
                    u1.addFriend(u2.getUsername());
                    u2.addFriend(u1.getUsername());
                    sendPkt(sender, PacketMessage.MessageType.SUCCESS, new SimpleMessage());
                }
            } catch (UserNotFoundException ignored){
            }catch (NullPointerException e){
                sendError(sender, "Errore interno al server. La richiesta di amicizia non esiste.");
            }
            return;
        }
        if(p.getType().equals(PacketMessage.MessageType.SHARETHIS)){
            try{
                User u = database.getUserByName(p.getMessage().getUsername());
                for(String follower : u.getFollowers()){
                    if(!database.isOnline(follower)){
                        database.getUserByName(follower).addUnsentMessage(new Post(u.getUsername(), (String) p.getMessage().getData()));
                    }
                    else{
                        database.getUserByName(follower).getStub().addMessage(u.getUsername(), (String) p.getMessage().getData());
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
