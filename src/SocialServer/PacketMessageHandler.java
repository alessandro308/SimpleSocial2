package SocialServer;

import SimpleSocial.Message.PacketMessage;

import java.nio.channels.SelectionKey;

/**
 * Created by alessandro on 18/05/16.
 */
public class PacketMessageHandler implements Runnable {
    SelectionKey sender;
    PacketMessage pkt;
    public PacketMessageHandler(SelectionKey sender, PacketMessage pkt){
        this.sender = sender;
        this.pkt = pkt;
    }
    @Override
    public void run() {

    }
}
