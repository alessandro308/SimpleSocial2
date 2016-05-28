package SimpleSocial;

import SimpleSocial.Exception.ObjectNotSetException;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * La morte.
 */
public class ObjectSocketChannel {
    private SocketChannel socket;
    private ByteBuffer object;
    private Object receivedObj;
    public ByteBuffer objectToSend;
    private ByteBuffer dimension = ByteBuffer.allocate(4);

    public ObjectSocketChannel(SocketChannel socket){
        this.socket = socket;
    }

    public SocketChannel getSocket(){
        return socket;
    }

    public boolean readObject(){
        try {
            if(object == null) {
                if (dimension.remaining() != 0) { //Leggi 32 bit (un intero)
                    socket.read(dimension);
                }
                if (!dimension.hasRemaining()){
                    dimension.flip();
                    object = ByteBuffer.allocate(ByteBuffer.wrap(dimension.array()).getInt());
                }
            }
            if(object != null) {
                socket.read(object);
                if(!object.hasRemaining()){
                    object.flip();
                    InputStream in = new ByteArrayInputStream(object.array(), 0, object.limit());
                    ObjectInputStream ois = new ObjectInputStream(in);
                    Object res = ois.readUnshared();
                    ois.close();
                    receivedObj = res;
                    return true;
                }
            }
            return false;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    public Object getReceivedObject(){
        return receivedObj;
    }

    public void setObjectToSend(Object obj) throws ObjectNotSetException{
        if(obj == null)
            throw new ObjectNotSetException("l'oggetto da settare non può essere null");

        try{
            ByteArrayOutputStream serializedObj = new ByteArrayOutputStream();
            ObjectOutputStream writer = new ObjectOutputStream(serializedObj);
            writer.writeUnshared(obj);

            objectToSend = ByteBuffer.wrap(serializedObj.toByteArray());

            dimension = ByteBuffer.allocate(4).putInt(objectToSend.array().length);
            dimension.flip();
        }catch (IOException ignored){}
    }

    /**
     * Invia l'oggetto inserito tramite la setObjectToSend()
     * @return true
     * @throws IOException
     */
    public boolean writeObject() throws IOException, ObjectNotSetException{
        if(objectToSend == null){
            throw new ObjectNotSetException();
        }

        while(dimension.hasRemaining()) {
            this.getSocket().write(dimension);
        }

        if(!dimension.hasRemaining()) { //è stata scritta tutta la dimensione
            this.getSocket().write(objectToSend);
            if (objectToSend.hasRemaining()) //L'oggetto non è terminato
                return false;
        }
        this.getSocket().close();
        return true;
    }
}
