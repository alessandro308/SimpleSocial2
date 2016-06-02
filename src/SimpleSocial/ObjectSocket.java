package SimpleSocial;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by alessandro on 06/05/16.
 */
public class ObjectSocket extends Socket {

    public ObjectSocket(InetAddress addr, int port) throws IOException{
        super(addr, port);
    }

    /**
     * Serializza un oggetto su un ByteArrayOutputStream. Scrive sulla socket la lunghezza dell'array e l'array.
     * @param obj - Oggetto da serializzare
     * @throws IOException - Se le operazioni di IO falliscono
     */
    public void writeObject(Object obj) throws IOException{
        ByteArrayOutputStream serializedObj = new ByteArrayOutputStream();
        ObjectOutputStream writer = new ObjectOutputStream(serializedObj);

        writer.writeUnshared(obj);
        byte[] bytes = serializedObj.toByteArray();
        this.getOutputStream().write(ByteBuffer.allocate(4).putInt(bytes.length).array());
        this.getOutputStream().write(bytes);
    }

    /**
     * Legge un oggetto spedito tramite la writeObject. Legge la dimensione, alloca un buffer per contenerlo e lo deserializza.
     * @return L'oggetto letto.
     */
    public Object readObject(){
        try {
            //Leggi dimensione totale pacchetto
            byte[] dimension = new byte[4];

            int byteRead = 0;

            while(byteRead < 4) {
                byteRead += this.getInputStream().read(dimension, byteRead, 4 - byteRead);
            }

            int size = ByteBuffer.wrap(dimension).getInt();
            byte[] object = new byte[size];

            while(size > 0){
                size -= this.getInputStream().read(object);
            }

            InputStream in = new ByteArrayInputStream(object, 0, object.length);
            ObjectInputStream ois = new ObjectInputStream(in);
            Object res = ois.readUnshared();
            ois.close();

            return res;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Funzione statica che effettua le stesse operazioni di readObject sull'oggetto passato per parametro.
     * @param socket Socket sulla quale è stato mandato l'oggetto
     * @return l'oggetto letto
     */
    static public Object readObject(Socket socket){
        try {
            //Leggi dimensione totale pacchetto
            byte[] dimension = new byte[4];

            int byteRead = 0;

            while(byteRead < 4) {
                InputStream x = socket.getInputStream();
                byteRead= x.read(dimension, byteRead, 4 - byteRead);
            }

            int size = ByteBuffer.wrap(dimension).getInt();
            byte[] object = new byte[size];

            while(size > 0){
                size -= socket.getInputStream().read(object);
            }

            InputStream in = new ByteArrayInputStream(object, 0, object.length);
            ObjectInputStream ois = new ObjectInputStream(in);
            Object res = ois.readUnshared();
            ois.close();

            return res;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}
