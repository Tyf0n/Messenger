
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

	void process(ChatServer cs, Socket connection, ObjectInputStream ois, ObjectOutputStream oos) throws Exception;
}

enum ServerCommand implements ServerCommandInterface {

    // The command to register a new chatter.
	REGISTER {
	
		public void process(ChatServer cs, Socket connection, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
			
			// Send the acknowledgement that I will process the request.
			System.out.println( "Sending acknowledgement for command." );
			cs.sendAcknowledgement(oos);
			
			// Having sent the acknowledgement, the client should now be sending
			// the chatter info object.
			System.out.println( "Waiting for client to send info." );
			ChatterInfo newChatter = (ChatterInfo)ois.readObject();
			newChatter.setAddress(connection.getInetAddress().toString());
						
			// Add the chatter info object to the list of chatters.
			System.out.println( "Adding new chatter." );
			cs.addChatter(newChatter);
			
			// Send the acknowledgement that I have registered.
			System.out.println( "Sending acknowledgement." );
			cs.sendAcknowledgement(oos);
		}
	},

	// The command to unregister a chatter.
	UNREGISTER {
	
		public void process(ChatServer cs, Socket connection, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
			
			// Send the acknowledgement that I will process the request.
			System.out.println( "Sending acknowledgement for command." );
			cs.sendAcknowledgement(oos);
			
			// Now read the chatter info object of the client who wants to unregister.
			System.out.println( "Waiting for client to send info." );
			ChatterInfo newChatter = (ChatterInfo)ois.readObject();
			
			// Remove the chatter from the room.
			System.out.println( "Removing chatter." );
			int index = cs.getChatterByName(newChatter.getName());
			cs.removeChatter(index);
			
			// Send the acknowledgement that I have unregistered.
			System.out.println( "Sending acknowledgement." );
			cs.sendAcknowledgement(oos);
		}	
	},
	
	// The command to get all the chatters information.
	GET_MEMBERS {
	
		public void process(ChatServer cs, Socket connection, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
			
			// Send the acknowledgement that I will process the request.
			System.out.println( "Sending acknowledgement for command." );
			cs.sendAcknowledgement(oos);
			
			// Get the array of all the chatters.
			System.out.println( "Sending info to client." );
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
	public int getChatterByName(String username);
	public int getChatterByAddress(String address);
	
	//  Methods to add or remove a chatter
	public boolean removeChatter(int index);
	public boolean addChatter(ChatterInfo c);
	
	// The method to send acknowledgements to the clients
	public void sendAcknowledgement (ObjectOutputStream oos) throws Exception;
	
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
				
				ClientHandler ch = new ClientHandler(this, client);
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
	

	public int getChatterByName(String username) {
		for(int i=0; i<list.size(); i++) {
			if (list.get(i).getName().equals(username)) {
				return i;
			}
		}
		return -1;
	}
	
	public int getChatterByAddress(String address) {
		for(int i=0; i<list.size(); i++) {
			if (list.get(i).getAddress().equals(address)) {
				return i;
			}
		}
		return -1;	
	}
		
	public boolean removeChatter(int index) {
		if (index == -1) {
			return false;
		}
		else {
			list.remove(index);
		}
		return true;
	}
	
	public boolean addChatter(ChatterInfo newChatter) {
		System.out.println( "Adding new chatter: " + newChatter );
		list.add(newChatter);
		return true;
	}
	
	public ChatterInfo[] getChatters() {
	
		ChatterInfo[] chatters = new ChatterInfo[list.size()];
		list.toArray(chatters);
		return chatters;
	}

	public static void main(String[] args) {

		ChatServerInterface c = ChatServer.getChatServerObject();
		c.listen();
	}
}

// The class for handling client connections. Each object runs on a separate thread
// for managing client connections.
class ClientHandler implements Runnable {

	// The chat server object for this client handler.
	ChatServer cs;
	
	// The connection to handle.
	Socket connection;
	
	// InetAddress can be used to unregister the client if the connection gets
	// closed.
	InetAddress address;
	
	// The thread object that will be initialized later.
	Thread runner;
	
	ClientHandler( ChatServer cs, Socket connection ) {
	
		// Create a new thread for this connection.
		runner = new Thread(this, connection.getInetAddress().toString());
		
		// Initialize the private variables.
		this.cs = cs;
		this.connection = connection;
		
		address = connection.getInetAddress();
		
		// Start the thread. The main thread should return back here.
		runner.start();
	}
	
	// The execution routine for the thread handling the client connection.
	public void run() {
	
		// Initialize the output and input streams for socket communication.
		ObjectOutputStream oos;
		ObjectInputStream ois;
		try {
			oos = new ObjectOutputStream(connection.getOutputStream());
			ois = new ObjectInputStream(connection.getInputStream());
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		try {

			while (true) {

				// Process the command that was sent by the client.
				ServerCommandInterface sci = (ServerCommandInterface)ois.readObject();
				System.out.println( "Thread processing command: " + sci);
				sci.process(this.cs,connection,ois,oos);
				System.out.println( "Done processing command: " + sci );
			}
		}
		
		// EOFException is encountered when this thread was expecting a command from the
		// client and suddenly the stream got closed. In that case, it can be assumed that
		// the client got disconnected.
		catch (EOFException e) {
		
			System.out.println( "Client " + address + " disconnected " );
			System.out.println( "Removing chatter at " + address );
			int index = cs.getChatterByAddress(address.toString());
			cs.removeChatter(index);
			System.out.println( "Thread exiting." );
			return;
		}
		
		catch (Exception e) {
				
			// Encountered an exception. Probably I should close the connection and
			// remove the client.
			System.out.println( "ClientHandler encountered exception" + e.getMessage());
			e.printStackTrace();
			return;
		}
	}
}
