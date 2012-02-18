
// Imported for sockets.
import java.net.*;

// Imported for ArrayList utilities.
import java.util.*;

// Imported for object stream classes.
import java.io.*;

// Define interface and the enum to process server commands
// This uses interfaces for enums, which makes it possible to declare
// separate methods for each of the enum values.
// These are the commands that are sent from the clients.
interface ServerCommandInterface {

	void process(ChatServer cs, ObjectInputStream ois, ObjectOutputStream oos) throws Exception;
}

enum ServerCommand implements ServerCommandInterface {

    // The command to register a new chatter.
	REGISTER {
	
		public void process(ChatServer cs, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
			
			// Send the acknowledgement that I will process the request.
			cs.sendAcknowledgement(oos);
			
			// Having sent the acknowledgement, the client should now be sending
			// the chatter info object.
			ChatterInfo newChatter = (ChatterInfo)ois.readObject();
			
			// Add the chatter info object to the list of chatters.
			cs.addChatter(newChatter);
			
			// Send the acknowledgement that I have registered.
			cs.sendAcknowledgement(oos);
		}
	},

	// The command to unregister a chatter.
	UNREGISTER {
	
		public void process(ChatServer cs, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
			
			// Send the acknowledgement that I will process the request.
			cs.sendAcknowledgement(oos);
			
			// Now read the chatter info object of the client who wants to unregister.
			ChatterInfo newChatter = (ChatterInfo)ois.readObject();
			
			// Remove the chatter from the room.
			cs.removeChatter(newChatter.getName());
			
			// Send the acknowledgement that I have unregistered.
			cs.sendAcknowledgement(oos);
		}	
	},
	
	// The command to get all the chatters information.
	GET_MEMBERS {
	
		public void process(ChatServer cs, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
			
			// Send the acknowledgement that I will process the request.
			cs.sendAcknowledgement(oos);
			
			// Get the array of all the chatters.
			ChatterInfo[] ciArray = cs.getChatters();
			
			// Write it into the connection's output stream.
			oos.writeObject(ciArray);
		}		
	},
}

// This is the interface for ChatServer. This enforces particular methods to be available 
// for each type of ChatServers
interface ChatServerInterface {

	// The master thread looking for new connections from chat clients
	public void listen();
	
	// Just a debug method to print all the chatters available
	public void printChatters();
	
	// The method to check whether a chatter is available or not.
	public boolean isChatterAvailable(String username);
	
	//  Methods to add or remove a chatter
	public boolean removeChatter(String username);
	public boolean addChatter(ChatterInfo c);
	
	// The method to send acknowledgements to the clients
	public void sendAcknowledgement (ObjectOutputStream oos);
	
	// Method to get the list of all the chatters
	public ChatterInfo[] getChatters();
}

// The default implementation of ChatServer using sockets and direct connection
class ChatServer implements ChatServerInterface {

	// This implementation opens a server socket to listen to a particular
	// port for incoming connections from the client.
	ServerSocket ss;
	
	// The below list will have the list of chatters.
	ArrayList<ChatterInfo> list;

	// This routine will send acknowledgement to the clients
	public void sendAcknowledgement (ObjectOutputStream oos) throws Exception {

		oos.writeObject(ClientCommand.ACKNOWLEDGEMENT);
	}

	// The default constructor.
	// For now, this will just initialize the array list.
	public ChatServer() {
	
		this.list = new ArrayList<ChatterInfo>();
	}
	
	// A factory method to return a interface object for the chat server.
	public static ChatServerInterface getChatServerObject() {
	
		ChatServer chatServerObject = new ChatServer();
		ChatServerInterface castedObject = (ChatServerInterface) chatServerObject;
		return castedObject;	
	}
	
	// The main thread of the chat server program which will just listen to the 
	// incoming connections from the client.
	public void listen() {
	
		// Initialize the server socket.
		try {
			ss = new ServerSocket( 3000 );
		}
		catch (Exception e) { }
		
		ObjectOutputStream oos;
		ObjectInputStream ois;
		
		// Now, get onto a infinite loop waiting for the connections.
		while (true) {

			try {
			
				// Wait for some client connections.
				System.out.println( "Waiting for connections.." );
				Socket client = ss.accept();
				System.out.println( "Accepted connection from " + client.getInetAddress() );
				
				// Initialize the output and input streams for socket communication.
				oos = new ObjectOutputStream(client.getOutputStream());
				ois = new ObjectInputStream(client.getInputStream());
				
				// Process the command that was sent by the client.
				ServerCommandInterface sci = (ServerCommandInterface)ois.readObject();
				sci.process(this,ois,oos);
			}
			catch (Exception e) { 
		
				System.out.println ("Error:" + e.getMessage());
			}
		}
	}

	public void printChatters() {
	
		for (int i=0; i<list.size(); i++) {
		
			System.out.println( list.get(i) );
		}
	}
	

	public boolean isChatterAvailable(String username) {
		for(int i=0; i<list.size(); i++) {
			if (list.get(i).getName().equals(username)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean removeChatter(String username) {
	
		for(int i=0; i<list.size(); i++) {
			if (list.get(i).getName().equals(username)) {
				//perform some additional checks whether i can remove this chatter
				list.remove(i);
				return true;
			}
		}
		return false;
	}
	
	public boolean addChatter(ChatterInfo newChatter) {
		System.out.println( "Adding new chatter: " + newChatter );
		list.add(newChatter);
		return true;
	}
	
	public ChatterInfo[] getChatters() {
	
		ChatterInfo[] chatters = (ChatterInfo[]) list.toArray();
		return chatters;
	}

	public static void main(String[] args) {

		ChatServerInterface c = ChatServer.getChatServerObject();
		c.listen();
	}
}
