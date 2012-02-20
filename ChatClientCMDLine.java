
import java.net.*;
import java.io.*;
import java.util.*;

// Below is interface for the client command set. Client-to-Server connection manager executes the 
// process routine for the command, whenever it recieves a command.
interface ClientCommandInterface {

	// The below routine is the inteface to the outside world for each of the client commands.
    void process (ServerConnectionManager scm) throws Exception;
}

enum ClientCommand implements ClientCommandInterface {

    // Default reply of acknowledgement whenever server has agreed to something.
	ACKNOWLEDGEMENT {
    
        public void process (ServerConnectionManager scm) throws Exception {
        
        }
	},
    
    // A command from the chat server to update the members because of a change.
    UPDATE_MEMBERS {
    
        public void process (ServerConnectionManager scm) throws Exception {
            scm.updateMembers();
        }
    }
}

// Interface of the ChatClient.
interface ChatClientInterface {

	public void connect();
}

// ChatClient which actually manages the chat functionalities at the client side.
class ChatClient implements ChatClientInterface {

	Socket serverConnection;
	String serverName;
	String userName;
	String password;
	
	Chatter myself;
	ChatterInfo[] availableChatters;
	
	public ChatClient() {
	
		availableChatters = new ChatterInfo[0];
	}		
	
	public ChatClient(String serverName,String userName,String password) {
	
		this.serverName = serverName;
		this.userName   = userName;
		this.password   = password;
		this.myself     = new Chatter( userName );
		availableChatters = new ChatterInfo[0];
	}
	
	public static ChatClientInterface getClientObject(String serverName,String userName,String password) {

		ChatClient ChatClientObject = new ChatClient(serverName,userName,password);
		ChatClientInterface castedObject = (ChatClientInterface) ChatClientObject;
		return castedObject;
	}

	public void updateChatters(ChatterInfo[] availableChatters) {
		this.availableChatters = availableChatters;
	}
	
	public void printChatters() {
	
		for(ChatterInfo c:availableChatters) {
			System.out.println( " * " + c.getName() + " ( " + c.getAddress() + " ) " );
		}
	}
	
	public void connect() {
	
		try {
			
			ServerConnectionManager scm = new ServerConnectionManager(this, serverName);
		}
		catch (Exception e) {
		
			System.out.println ("Error:" + e.getMessage());
		} 
	}
		
	public Chatter whoAmI() {
	
		return myself;
	}
	
}

class ChatClientCMDLine {

	private ChatClient cc;

	public static void main(String[] args) {
	
		ChatClientCMDLine cmdlineObj;
		if (args.length == 2) {
			cmdlineObj = new ChatClientCMDLine (args[0], args[1], "");	
		}
		else {
			cmdlineObj = new ChatClientCMDLine();
		}
		cmdlineObj.connect();
		cmdlineObj.run();
	}
	
	public ChatClientCMDLine(String serverName,String username,String password) {
		cc = new ChatClient(serverName,username,"");
	}

	public void connect() {
		cc.connect();
	}
	
	public ChatClientCMDLine() {
		System.out.print( "Enter the chat server name: " );
		Scanner sc = new Scanner(System.in);
		String serverName = sc.nextLine();
		
		System.out.print( "Enter your name: " );
		String userName = sc.nextLine();
		
		cc = new ChatClient(serverName,userName,"");
	}
	
	void run() {
	
		Scanner sc = new Scanner(System.in);
		while(true) {
		
			System.out.print ("Enter action: ");
			String action = sc.nextLine();
			
			if (action.equals("list")) {
				cc.printChatters();			
			}
			else if (action.equals("exit")) {
				System.exit(0);			
			}
		}
	}
}

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