
import java.io.*;
import java.net.*;
import java.util.*;

// The class for handling client connections. Each object runs on a separate thread
// for managing client connections.
// Belongs to ChatServer

class ChatServerConnectionManager implements Runnable {

	// The chat server object for this client handler.
	ChatServer cs;
	
	// The connection to handle.
	Socket connection;
	
	// InetAddress can be used to unregister the client if the connection gets
	// closed.
	InetAddress address;
	
	// The thread object that will be initialized later.
	Thread runner;

	// Initialize the output and input streams for socket communication.
	ObjectOutputStream oos;
	ObjectInputStream ois;
	
	AbstractQueue<ServerCommandInterface> commandsQueue;
	AbstractQueue<ChatRequest> chatRequestQueue;
	
	boolean exitThread = false;
	
	ChatServerConnectionManager( ChatServer cs, Socket connection ) {
	
		// Create a new thread for this connection.
		runner = new Thread(this, connection.getInetAddress().toString());
		
		// Initialize the private variables.
		this.cs = cs;
		this.connection = connection;
		
		address = connection.getInetAddress();
		
		commandsQueue = new java.util.concurrent.ConcurrentLinkedQueue<ServerCommandInterface>();
		chatRequestQueue = new java.util.concurrent.ConcurrentLinkedQueue<ChatRequest>();
		
		// Start the thread. The main thread should return back here.
		runner.start();
	}
	
	public void queueChatRequest(ChatRequest cr) {
		chatRequestQueue.add(cr);
	}
	
	public ChatRequest getFirstChatRequest() {
		return chatRequestQueue.remove();
	}

	public void queueCommand(ServerCommandInterface sci) {
		commandsQueue.add(sci);
	}
	
	// A set of getXX routines to get the various variables.
	public ChatServer getChatServer() {
		return cs;
	}
	
	public Socket getConnection() {
		return connection;
	}
	
	public ObjectOutputStream getOutputStream() {
		return oos;
	}
	
	public ObjectInputStream getInputStream() {
		return ois;
	}
	
	public void sendUpdateMsg() throws Exception {
	
		oos.writeObject(ClientCommand.UPDATE_MEMBERS);
	}

	// This routine will send acknowledgement to the clients
	public void sendAcknowledgement () throws Exception {

		oos.writeObject(ClientCommand.ACKNOWLEDGEMENT);
	}

	public void sendClientCommand (ClientCommand cc) throws Exception {

		oos.writeObject(cc);
	}

	public void cleanup() {
	
		System.out.println( "Client " + address + " disconnected " );
		System.out.println( "Removing chatter at " + address );
		int index = cs.getChatterByAddress(address.toString());
		cs.removeChatter(index);
		cs.updateAllHandlers();
		System.out.println( "Thread exiting." );
	}
	
	// The execution routine for the thread handling the client connection.
	public void run() {
	
		try {
			oos = new ObjectOutputStream(connection.getOutputStream());
			ois = new ObjectInputStream(connection.getInputStream());
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		try {

			BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());
			while (true) {

				// Process the command that was sent by the client.
				// First preference to requests from client
				ServerCommandInterface sci = null;
				
				if (exitThread == true) {
					cleanup();
					return;
				}
				
				if (bis.available() > 0) {
					sci = (ServerCommandInterface)ois.readObject();
				}
				else if (!commandsQueue.isEmpty()) {
					sci = (ServerCommandInterface)commandsQueue.remove();
				}

				if (sci != null) {
					System.out.println( "Thread processing command: " + sci);
					sci.process(this);
					System.out.println( "Done processing command: " + sci );
				}
			}
		}
		
		// EOFException is encountered when this thread was expecting a command from the
		// client and suddenly the stream got closed. In that case, it can be assumed that
		// the client got disconnected.
		catch (EOFException e) {
		
			cleanup();
			return;
		}
		catch (SocketException e) {
		
			cleanup();
			return;
		}
		
		catch (Exception e) {
				
			// Encountered an exception. Probably I should close the connection and
			// remove the client.
			cleanup();
			return;
		}
	}
	
	public void closeConnection() {

		System.out.println( "Client " + address + " disconnected " );
		System.out.println( "Removing chatter at " + address );
		int index = cs.getChatterByAddress(address.toString());
		cs.removeChatter(index);
		cs.updateAllHandlers();
		System.out.println( "Thread exiting." );
	}
}

// Define interface and the enum to process server commands
// This uses interfaces for enums, which makes it possible to declare
// separate methods for each of the enum values.
// These are the commands that are sent from the clients.
interface ServerCommandInterface {

	void process(ChatServerConnectionManager ch) throws Exception;
}

enum ServerCommand implements ServerCommandInterface {

    // The command to register a new chatter.
	REGISTER {
	
		public void process(ChatServerConnectionManager ch) throws Exception {
			
			// Get all the required variables
			ChatServer cs          = ch.getChatServer();
			ObjectInputStream ois  = ch.getInputStream();
			ObjectOutputStream oos = ch.getOutputStream();
			Socket connection      = ch.getConnection();
			
			// Send the acknowledgement that I will process the request.
			System.out.println( "Sending acknowledgement for command." );
			ch.sendAcknowledgement();
			
			// Having sent the acknowledgement, the client should now be sending
			// the chatter info object.
			System.out.println( "Waiting for client to send info." );
			ChatterInfo newChatter = (ChatterInfo)ois.readObject();
			newChatter.setAddress(connection.getInetAddress().getHostAddress());
						
			// Add the chatter info object to the list of chatters.
			System.out.println( "Adding new chatter." );
			cs.addChatter(newChatter,ch);
			
			// Send the acknowledgement that I have registered.
			System.out.println( "Sending acknowledgement." );
			ch.sendAcknowledgement();
			
			cs.updateAllHandlers();
		}
	},

	// The command to unregister a chatter.
	UNREGISTER {
	
		public void process(ChatServerConnectionManager ch) throws Exception {

			// Get all the required variables
			ChatServer cs          = ch.getChatServer();
			ObjectInputStream ois  = ch.getInputStream();
			ObjectOutputStream oos = ch.getOutputStream();

			ch.closeConnection();
						
			// Send the acknowledgement that I have unregistered.
			System.out.println( "Sending acknowledgement." );
			ch.sendAcknowledgement();
		}	
	},
	
	// The command to get all the chatters information.
	GET_MEMBERS {
	
		public void process(ChatServerConnectionManager ch) throws Exception {

			// Get all the required variables
			ChatServer cs          = ch.getChatServer();
			ObjectOutputStream oos = ch.getOutputStream();
		
			// Send the acknowledgement that I will process the request.
			System.out.println( "Sending acknowledgement for command." );
			ch.sendAcknowledgement();
			
			// Get the array of all the chatters.
			System.out.println( "Sending info to client." );
			ChatterInfo[] ciArray = cs.getChatters();
			
			// Write it into the connection's output stream.
			oos.writeObject(ciArray);
		}		
	},
	
	ASK_CLIENT_TO_UPDATE {
		
		public void process(ChatServerConnectionManager ch) throws Exception {
			ch.sendUpdateMsg();
		}
	},
	
	FORWARD_CHAT_REQUEST {
	
		public void process(ChatServerConnectionManager ch) throws Exception {
		
			ch.sendAcknowledgement();
			ObjectInputStream ois = ch.getInputStream();
			ChatRequest cr = (ChatRequest) ois.readObject();
			ChatterInfo[] chatterArray = cr.getTargetChatters();
			for (ChatterInfo ci:chatterArray) {
				int index = ch.getChatServer().getChatterByName(ci.getName());
				ChatServerConnectionManager cscm = ch.getChatServer().getConnectionManager(index);
				cscm.queueChatRequest(cr);
				cscm.queueCommand(ServerCommand.INCOMING_CHAT_REQUEST);
			}
			ch.sendAcknowledgement();
		}	
	},
	
	INCOMING_CHAT_REQUEST {
	
		public void process(ChatServerConnectionManager ch) throws Exception {
	
			ch.sendClientCommand(ClientCommand.CHAT_REQUEST);
			
			ObjectOutputStream oos = ch.getOutputStream();
			oos.writeObject(ch.getFirstChatRequest());
		}
	}
}
