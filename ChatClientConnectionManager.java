
import java.io.*;
import java.net.*;
import java.util.*;

// The object of the below class will maintain the connection to the server. The contructor
// for this object will create a new thread of execution when it has established a valid
// connection with the server.
// Belongs to ChatClient
class ChatClientConnectionManager implements Runnable {

    // The below 4 objects are the main ones that will be used throughout the 
    // lifetime of this object.
    ChatClient cc;
    Socket connection;
    ObjectOutputStream oos;
    ObjectInputStream ois;
	
	AbstractQueue<ClientCommandInterface> commandsQueue;
	AbstractQueue<ChatRequest> chatRequestQueue;
    
    public ChatClientConnectionManager(ChatClient cc, String serverName) throws Exception {
    
        // Initialize the internal vairables
        this.cc = cc;
        this.connection = new Socket (serverName, 3000);
        
        // Initialize the streams
		System.out.println( "Connection established to " + serverName );
		oos = new ObjectOutputStream(connection.getOutputStream());
		ois = new ObjectInputStream(connection.getInputStream());
		
        // Register the client to the server.
		System.out.println( "Sending REGISTER command to server." );
		sendServerCommand(ServerCommand.REGISTER);
		
        // Wait for acknowledgement from the server.
		System.out.println( "Waiting for acknowledgement from server." );
		recieveAcknowledgement();
		
        // Now, having got the acknowledgement, send my info to the server.
		System.out.println( "Sending info to server." );
		oos.writeObject(cc.whoAmI());
		
        // Wait for the acknowledgement from the server.
		System.out.println( "Waiting for acknowledgement from server." );
		recieveAcknowledgement();
		
		commandsQueue = new java.util.concurrent.ConcurrentLinkedQueue<ClientCommandInterface>();
		chatRequestQueue = new java.util.concurrent.ConcurrentLinkedQueue<ChatRequest>();

        // Everything done, now fork a thread to manage things in the background.
        Thread runner = new Thread (this, "server_connection_manager");        
        runner.start();
    }
    
    // The below routine will update the members visible to this chat client.
    public boolean updateMembers () throws Exception {
    
		System.out.println( "Sending GET_MEMBERS command to server." );
		sendServerCommand(ServerCommand.GET_MEMBERS);
			
		System.out.println( "Waiting for acknowledgement from server." );
		recieveAcknowledgement();

		System.out.println( "Recieving chatters information from server." );
        ChatterInfo[] chatters = (ChatterInfo[]) ois.readObject();
        cc.updateChatters(chatters);
		return true;
    }
	
	public void processIncomingChat() throws Exception {
	
		System.out.println("processIncomingChat: Got an incoming chat");
		
		System.out.println("processIncomingChat: Waiting for chat request object");
		ChatRequest cr = (ChatRequest) ois.readObject();
		
		int port = cr.getPortAddress();
		System.out.println("processIncomingChat: Port is " + port);
		
		System.out.println("processIncomingChat: Creating new chatter object");
		Chatter newChatter = new Chatter( cr, cc.whoAmI(), port );
		
		System.out.println("processIncomingChat: Adding the new chatter to the chat client");
		getChatClient().addChat(newChatter);
	}

	public void queueCommand(ClientCommandInterface cci) {
		commandsQueue.add(cci);
	}

	public void queueChatRequest(ChatRequest cr) {
		chatRequestQueue.add(cr);
	}
	
	public ChatRequest getFirstChatRequest() {
		return chatRequestQueue.remove();
	}
	
	// A set of getXX routines to get the various variables.
	public ChatClient getChatClient() {
		return cc;
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

		
    // Routine to wait for acknowledgement continously.
	public boolean recieveAcknowledgement() throws Exception {
	
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
	
	public void closeConnection() throws Exception {
	
		sendServerCommand( ServerCommand.UNREGISTER );
	}
	
    // Send a particular server command
	public void sendServerCommand(ServerCommand sc) throws Exception {
		oos.writeObject(sc);
	}
    
    public void run() {
        
		
        try {
        
			BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());
            while (true) {
        
                ClientCommandInterface cci = null;
				
				if (bis.available() > 0) {
					cci = (ClientCommandInterface) ois.readObject();
				}
				else if (!commandsQueue.isEmpty()) {
					cci = (ClientCommandInterface) commandsQueue.remove();
				}
				
				if (cci != null) {
					cci.process(this);
				}
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

// Below is interface for the client command set. Client-to-Server connection manager executes the 
// process routine for the command, whenever it recieves a command.
interface ClientCommandInterface {

	// The below routine is the inteface to the outside world for each of the client commands.
    void process (ChatClientConnectionManager cccm) throws Exception;
}

enum ClientCommand implements ClientCommandInterface {

    // Default reply of acknowledgement whenever server has agreed to something.
	ACKNOWLEDGEMENT {
    
        public void process (ChatClientConnectionManager cccm) throws Exception {
        
        }
	},
    
    // A command from the chat server to update the members because of a change.
    UPDATE_MEMBERS {
    
        public void process (ChatClientConnectionManager cccm) throws Exception {
            cccm.updateMembers();
        }
    },
	
	INITIATE_CHAT {

        public void process (ChatClientConnectionManager cccm) throws Exception {
			
			System.out.println("INITIATE_CHAT: In INITIATE_CHAT");
			System.out.println("INITIATE_CHAT: Sending forward chat request command");
            cccm.sendServerCommand(ServerCommand.FORWARD_CHAT_REQUEST);
			System.out.println("INITIATE_CHAT: Waiting for acknowledgement");
			cccm.recieveAcknowledgement();
			System.out.println("INITIATE_CHAT: Sending the chat request to the server");
			ObjectOutputStream oos = cccm.getOutputStream();
			ChatRequest cr = cccm.getFirstChatRequest(); 
			oos.writeObject(cr);
			System.out.println("INITIATE_CHAT: Waiting for acknowledgement");
			cccm.recieveAcknowledgement();
			
			Chatter newChatter = new Chatter(cccm.getChatClient().whoAmI(),cr.getPortAddress());
			System.out.println("INITIATE_CHAT: Creating a new chatter thread");
			cccm.getChatClient().addChat(newChatter);
        }
	},
	
	CHAT_REQUEST {
	
		public void process (ChatClientConnectionManager cccm) throws Exception {
		
			System.out.println("CHAT_REQUEST: Got an incoming chat");
			cccm.processIncomingChat();
		}
	},
	
	EXIT {

        public void process (ChatClientConnectionManager cccm) throws Exception {
            cccm.closeConnection();
        }	
	}
}
