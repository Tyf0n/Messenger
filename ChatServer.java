

// Imported for sockets.
import java.net.*;

// Imported for ArrayList utilities.
import java.util.*;

// Imported for object stream classes.
import java.io.*;


// This is the interface for ChatServer. This enforces particular methods to be available 
// for each type of ChatServers
interface ChatServerInterface {

	// The master thread looking for new connections from chat clients
	public void listen();
	
	// Just a debug method to print all the chatters available
	public void printChatters();
	
	// The method to check whether a chatter is available or not.
	public int getChatterByName(String username);
	public int getChatterByAddress(String address);
	
	//  Methods to add or remove a chatter
	public boolean removeChatter(int index);
	public boolean addChatter(ChatterInfo c,ChatServerConnectionManager ch);
	
	// Method to get the list of all the chatters
	public ChatterInfo[] getChatters();
}

// The default implementation of ChatServer using sockets and direct connection
class ChatServer implements ChatServerInterface {

	// This implementation opens a server socket to listen to a particular
	// port for incoming connections from the client.
	ServerSocket ss;
	
	// The below list will have the list of chatters.
	ArrayList<Registration> list;

	// The default constructor.
	// For now, this will just initialize the array list.
	public ChatServer() {
	
		this.list = new ArrayList<Registration>();
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
				
				// Create a new client handler for this. The ChatServerConnectionManager should take care of necessary
				// things for registering this new chatter.
				ChatServerConnectionManager ch = new ChatServerConnectionManager(this, client);
			}
			catch (Exception e) { 
		
				System.out.println ("Error:" + e.getMessage());
			}
		}
	}

	// A helper routine to print information about all the chatters.
	public void printChatters() {
	
		for (int i=0; i<list.size(); i++) {
		
			System.out.println( list.get(i) );
		}
	}
	
	// Returns the ID of the chatter based on the username. This routine searches through
	// all the registrations to find out the correct one by the username.
	public int getChatterByName(String username) {
		for(int i=0; i<list.size(); i++) {
			if (list.get(i).getChatterInfo().getName().equals(username)) {
				return i;
			}
		}
		return -1;
	}
	
	// Returns the ID of the chatter based on the IP address of the connection. This routine
	// searches through all the registrations to find out the correct one by the ip address.
	public int getChatterByAddress(String address) {
		for(int i=0; i<list.size(); i++) {
			if (list.get(i).getChatterInfo().getAddress().equals(address)) {
				return i;
			}
		}
		return -1;	
	}
	
	public ChatServerConnectionManager getConnectionManager(int i) {
		return list.get(i).getChatServerConnectionManager();
	}
	
	// This function removes the chatter, given the index of the registration.
	public boolean removeChatter(int index) {
		if (index == -1) {
			return false;
		}
		else {
			list.remove(index);
		}
		return true;
	}
	
	// This function creates a new registration object and adds the chatter.
	public boolean addChatter(ChatterInfo newChatter,ChatServerConnectionManager ch) {
		System.out.println( "Adding new chatter: " + newChatter );
		list.add(new Registration(ch,newChatter));
		return true;
	}
	
	// This function sends a message to all the client handlers to send a message
	// to their clients to update the members.
	public void updateAllHandlers() {
		for(int i=0; i<list.size(); i++) {
			System.out.println( "Asking to update.." );
			list.get(i).getChatServerConnectionManager().queueCommand(ServerCommand.ASK_CLIENT_TO_UPDATE);
		}		
	}
	
	// This function returns an array of chatters.
	public ChatterInfo[] getChatters() {
	
		ChatterInfo[] chatters = new ChatterInfo[list.size()];
		for(int i=0; i<list.size(); i++) {
			chatters[i] = list.get(i).getChatterInfo();
		}
		return chatters;
	}

	// The main routine for the chat server. Just listen for connections.
	public static void main(String[] args) {

		ChatServerInterface c = ChatServer.getChatServerObject();
		c.listen();
	}
}

// A object which composes the pair of information of client and the object
// which handles the client. For most of the operations, the information
// of client is used, and for lesser operations, the registration object is
// used.
class Registration {

	ChatServerConnectionManager ch;
	ChatterInfo ci;
	
	public Registration(ChatServerConnectionManager ch,ChatterInfo ci) {
		this.ch = ch;
		this.ci = ci;
	}
	
	public ChatServerConnectionManager getChatServerConnectionManager() {
		return ch;
	}
	
	public ChatterInfo getChatterInfo() {
		return ci;
	}
	
	public String toString() {
		String res = ci.toString();
		return res;
	}
}
