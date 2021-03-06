package SimpleSocial;

import SimpleSocial.Exception.ConfigValueAlreadyExistsException;
import SimpleSocial.Exception.UnregisteredConfigNameException;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Oggetto che carica la configurazione dal file specificato. Vengono ignorate le righe vuote.
 * La formattazione del file deve essere del tipo
 * #COMMENTO
 * NOME VALORE
 *
 * Se valore è un intero viene parsato e considerato come tale
 */
public class Config {
    private HashMap<String, Object> config = new HashMap<>();
    private String fileName;

    public Config(String configFile){
        fileName = configFile;
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(configFile));
            String line;
            while((line = br.readLine()) != null){
                if(!line.startsWith("#") && line.length() != 0){
                    String[] tmp = line.split(" ");
                    try{
                        int value = Integer.parseInt(tmp[1]);
                        config.put(tmp[0], value);
                    } catch (NumberFormatException e){
                        config.put(tmp[0], tmp[1]);
                    }
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Ritorna il valore della configurazione.
     * @param configName - Il nome della configurazione di cui si vuole conoscere il valore
     * @return l'oggetto contentente il valore della configurazione. Tipicamente stringa o intero.
     * @throws UnregisteredConfigNameException se la registrazione non è presente nel sistema.
     */
    public Object getValue(String configName) throws UnregisteredConfigNameException {
        if(!config.containsKey(configName))
            throw new UnregisteredConfigNameException("Valore "+configName+" non configurato");
        return config.get(configName);
    }


    /**
     * Imposta una nuova configurazione.
     * @param key - Il nome della nuova configurazione
     * @param value - Il valore della configurazione
     * @throws ConfigValueAlreadyExistsException se la configurazione è già presente nel database
     */
    public void setConfig(String key, Object value) throws ConfigValueAlreadyExistsException {
        if(config.containsKey(key)){
            throw new ConfigValueAlreadyExistsException("Il valore "+key+" esiste già");
        }
        config.put(key, value);
    }

    /**
     * Imposta una nuova configurazione. Aggiorna il nome configurazione se questa già esiste.
     * @param key - Il nome della nuova configurazione
     * @param value - Il valore della configurazione
     */
    public void reConfig(String key, Object value) throws ConfigValueAlreadyExistsException {
        config.put(key, value);
    }

    /**
     * Imposta una nuova configurazione. Aggiorna il nome configurazione.
     * @param key - Il nome della nuova configurazione
     * @param value - Il valore della configurazione
     * @param description - Descrizione della configurazione
     * @throws ConfigValueAlreadyExistsException se la configurazione è già presente nel database
     */
    public void setConfig(String key, Object value, String description) throws ConfigValueAlreadyExistsException {
        if(config.containsKey(key)){
            throw new ConfigValueAlreadyExistsException();
        }
        config.put(key, value);

    }


    public void saveOnFile(){
        try{
        FileOutputStream out = new FileOutputStream(fileName);
            for (Object o : config.entrySet()) {
                Map.Entry pair = (Map.Entry) o;
                out.write((pair.getKey() + " " + pair.getValue() + "\n").getBytes());
            }
            out.close();
        }
        catch(IOException e){
            System.err.println("Non è stato possibile trovare il file di configurazione.");
        }
    }

    /**
     * Aggiorna il valore di configurazione key con un nuovo valore. Non aggiorna il file di configurazione.
     * @param key - Nome del valore
     * @param value - Nuovo valore
     * @throws UnregisteredConfigNameException se non esiste una configurazione chiamata key
     */
    public void updateConfig(String key, Object value) throws UnregisteredConfigNameException {
        if(!config.containsKey(key)){
            throw new UnregisteredConfigNameException("Valore "+key+" non presente tra le configurazioni");
        }
        if(value == null){
            throw new NullPointerException();
        }
        config.put(key, value);
    }

    /**
     * Controlla se la chiave key è associata ad un valore.
     * @param key - Nome della configurazione da controllare
     * @return true se key esiste tra le configurazione. False altrimenti.
     */
    public boolean isSet(String key){
        return config.containsKey(key);
    }

    /**
     * Rimuove la chiave key dall'insieme di configurazioni attive
     * @param key - Nome della configurazione da rimuovere
     */
    public void removeKey(String key){
        config.remove(key);
    }

}
