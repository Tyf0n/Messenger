
import java.io.*;
import java.util.*;

interface ChatObjectInterface {

    public void send(ObjectOutputStream oos) throws Exception;
    public void recieve(ObjectInputStream oos) throws Exception;
    public void processChatObject();
    public void doPreProcess();
}

class TextChatObject implements ChatObjectInterface {

    //final ChatterCommand sendCmd = ChatterCommand.SEND_TEXT;
    //final ChatterCommand recieveCmd = ChatterCommand.RECIEVE_TEXT;
    String text;
    
    public TextChatObject(String text) {
        this.text = text;
    }
    
    public void send(ObjectOutputStream oos) throws Exception {   
        oos.writeObject(text);
    }
    
    public void recieve(ObjectInputStream ois) throws Exception {
        text = (String) ois.readObject();
    }
    
    public void doPreProcess() {
    
        char[] chars = new char[text.length()];
        Arrays.fill(chars,'\b');
        String backSpaces = new String( chars );
        System.out.print('\r');
        System.out.print(backSpaces);
    }
    
    public void processChatObject() {
        System.out.println(text);
    }
    
    public String getString() {
        return text;
    }
}
