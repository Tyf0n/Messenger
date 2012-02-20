
import java.io.*;
import java.net.*;

// The object of the below class will maintain the connection to the server. The contructor
// for this object will create a new thread of execution when it has established a valid
// connection with the server.
class ServerConnectionManager implements Runnable {

    // The below 4 objects are the main ones that will be used throughout the 
    // lifetime of this object.
    ChatClient cc;
    Socket connection;
    ObjectOutputStream oos;
    ObjectInputStream ois;
    
    public ServerConnectionManager(ChatClient cc, String serverName) throws Exception {
    
        // Initialize the internal vairables
        this.cc = cc;
        this.connection = new Socket (serverName, 3000);
        
        // Initialize the streams
		System.out.println( "Connection established to " + serverName );
		oos = new ObjectOutputStream(connection.getOutputStream());
		ois = new ObjectInputStream(connection.getInputStream());
		
        // Register the client to the server.
		System.out.println( "Sending REGISTER command to server." );
		sendServerCommand(ServerCommand.REGISTER,oos);
		
        // Wait for acknowledgement from the server.
		System.out.println( "Waiting for acknowledgement from server." );
		recieveAcknowledgement(ois);
		
        // Now, having got the acknowledgement, send my info to the server.
		System.out.println( "Sending info to server." );
		oos.writeObject(cc.whoAmI().getInfo());
		
        // Wait for the acknowledgement from the server.
		System.out.println( "Waiting for acknowledgement from server." );
		recieveAcknowledgement(ois);

        // Everything done, now fork a thread to manage things in the background.
        Thread runner = new Thread (this, "server_connection_manager");        
        runner.start();
    }
    
    // The below routine will update the members visible to this chat client.
    public boolean updateMembers () throws Exception {
    
		System.out.println( "Sending GET_MEMBERS command to server." );
		sendServerCommand(ServerCommand.GET_MEMBERS,oos);
			
		System.out.println( "Waiting for acknowledgement from server." );
		recieveAcknowledgement(ois);

		System.out.println( "Recieving chatters information from server." );
        ChatterInfo[] chatters = (ChatterInfo[]) ois.readObject();
        cc.updateChatters(chatters);
		return true;
    }

    // Routine to wait for acknowledgement continously.
	public boolean recieveAcknowledgement(ObjectInputStream ois) throws Exception {
	
		ClientCommand cc = (ClientCommand) ois.readObject();
		if (cc == ClientCommand.ACKNOWLEDGEMENT) {
			return true;
		}
		else {
			return false;
		}
	}
	
    // Send an acknowledgement to the server.
	public void sendAcknowledgement(ObjectOutputStream oos) throws Exception {
	
		oos.writeObject(ClientCommand.ACKNOWLEDGEMENT);
	}
	
    // Send a particular server command
	public void sendServerCommand(ServerCommand sc,ObjectOutputStream oos) throws Exception {
		oos.writeObject(sc);
	}
    
    public void run() {
        
        try {
        
            while (true) {
        
                ClientCommandInterface cci = (ClientCommandInterface) ois.readObject();
                cci.process(this);
            }
        }
		catch (EOFException e) {
		
			// Lost connection while trying to read from the stream.
			System.out.println( "Server connection lost.." );
			System.out.println( "Thread exiting." );
			return;
		}
		
		catch (Exception e) {
				
			// Encountered an exception. Probably I should close the connection and
			// remove the client.
			System.out.println( "Encountered exception" + e.getMessage());
			e.printStackTrace();
			return;
		}
    }
}
