
import java.io.*;
import java.util.*;
import java.net.*;

class Chatter implements Runnable, CommandlineInterface {

	    
	private ChatterInfo myself;
	int port;
	
    ArrayList<Socket> connections = new ArrayList<Socket>();
    ArrayList<ObjectInputStream> oiss = new ArrayList<ObjectInputStream>();
    ArrayList<ObjectOutputStream> ooss = new ArrayList<ObjectOutputStream>();
    ArrayList<BufferedInputStream> biss = new ArrayList<BufferedInputStream>();
    
	boolean isInitiator;
	boolean exitNow;
	
	ArrayList<ChatterRegistration> chatters = new ArrayList<ChatterRegistration>();
	
	ChatClientInterface c;
	
	AbstractQueue<ChatterCommandInterface> commandsQueue;
    AbstractQueue<ChatObjectInterface> coiQueue = new java.util.concurrent.ConcurrentLinkedQueue<ChatObjectInterface>();
    
    ChatRequest cr;

    public void setToFocus() {
    
    }
    
    public void setOutOfFocus() {
    
    }
    
    public ChatterInfo getChatterByConnection(Socket connection) {
    
        for(int i=0; i<chatters.size(); i++) {
		
            if (chatters.get(i).connection.equals(connection)) {
				return chatters.get(i).getChatterInfo();
			}
		}
        return null;
    }
    
    public void processInput(String line,client clientObj) {
        
        TextChatObject tco = new TextChatObject(line);
        sendChatObject((ChatObjectInterface)tco);
    }
        
	public boolean recieveAcknowledgement(ObjectInputStream ois) throws Exception {
	
		ChatterCommand cc = (ChatterCommand) ois.readObject();
		if (cc == ChatterCommand.ACKNOWLEDGEMENT) {
			return true;
		}
		else {
			return false;
		}
	}

   	public void queueCommand(ChatterCommandInterface cci) {
		commandsQueue.add(cci);
	}

    public void sendChatObject(ChatObjectInterface coi) {
        coiQueue.add(coi);
        queueCommand(ChatterCommand.SEND_TEXT);
    }
    
	public Chatter (ChatterInfo myself, int port) {

        connections = new ArrayList<Socket>();

		System.out.println("Chatter: In constructor, initiator");
		isInitiator = true;
		this.myself = myself;
        commandsQueue = new java.util.concurrent.ConcurrentLinkedQueue<ChatterCommandInterface>();

		try {
            ServerSocket ss = new ServerSocket(port);
            ChatIncomingListener cil = new ChatIncomingListener (ss,this);

        }
		catch (Exception e) {
			e.printStackTrace();
            return;
		}

		Thread t = new Thread( this, "chatter" );
		t.start();
	}
    
    public ChatObjectInterface getFirstChatObject() {
        return coiQueue.remove();
    }

	public void addRegistration(ChatterRegistration cr) {
		chatters.add(cr);
	}
	
	public void addIncomingChatter(Socket connection) throws Exception {
        System.out.println( "Added a new connection" );
		ooss.add(new ObjectOutputStream(connection.getOutputStream()));
        oiss.add(new ObjectInputStream(connection.getInputStream()));
        biss.add(new BufferedInputStream(connection.getInputStream()));
        connections.add(connection);
        System.out.println( "Done adding streams and connection" );
	}

	public Chatter (ChatRequest cr, ChatterInfo myself, int port) {
			
		Socket connection;
        System.out.println( "port = " + port);
        commandsQueue = new java.util.concurrent.ConcurrentLinkedQueue<ChatterCommandInterface>();
        connections = new ArrayList<Socket>();
        this.cr = cr;

		try {
			connection = new Socket(cr.getSourceChatter().getAddress(), port);
			addIncomingChatter(connection);

        }
		catch (Exception e) {
			e.printStackTrace();
            return;
		}

        this.myself = myself;
        queueCommand(ChatterCommand.DO_REGISTER);
		
		Thread t = new Thread( this, "chatter" );
		t.start();
	}
    
    public ChatRequest getChatRequest() {
        return cr;
    }
	
    public ChatterInfo whoAmI() {
        return myself;
    }
    
	public void sendAcknowledgement(ObjectOutputStream oos) throws Exception {
		oos.writeObject(ChatterCommand.ACKNOWLEDGEMENT);
	}
	
	public void demandExit() {
		exitNow = true;
	}
	
	public void run() {
	
		System.out.println("Chatter: Started a new thread");
        
		while(true) {
		
        
			if (exitNow) {
				return;
			}
            
            try {

                for(int i=0; i<connections.size(); i++) {
                
                    Socket connection = connections.get(i);
                    ObjectOutputStream oos = ooss.get(i);
                    ObjectInputStream ois = oiss.get(i);
                    BufferedInputStream bis = biss.get(i);
					
					ChatterCommandInterface cci = null;
					//while (true) {
        
						if (bis.available() > 0) {
                            //System.out.println( "Something is available to read");
                            cci = (ChatterCommandInterface) ois.readObject();
						}
						else if (!commandsQueue.isEmpty()) {
                            //System.out.println( "Something is available to process");
							cci = (ChatterCommandInterface) commandsQueue.remove();
						}
				
						if (cci != null) {
							cci.process(this,connection,ois,oos);
						}
					//}
                }
                
			}
			catch (Exception e) {
				e.printStackTrace();
                return;
			}
        }		
	}
	
	public ChatterInfo getInfo() {
		return myself;
	}
	    
}

class ChatIncomingListener implements Runnable {

	ServerSocket ss;
	Chatter c;
	boolean exitNow;
	
	    
	public ChatIncomingListener(ServerSocket ss,Chatter c) {
		this.ss = ss;
		this.c  = c;
		
		Thread t = new Thread( this, "listener" );
		t.start();
	}

	public void run() {
		
		System.out.println("ChatIncomingListener: Created port, now waiting");
		try {
		
			while(true) {
				Socket newConnection = ss.accept();
				c.addIncomingChatter(newConnection);
			}
		}
		
		catch (Exception e) {
			
			System.out.println( "Chat listener quitting" );
			return;
		}
	}
    	
}

class ChatterRegistration {

	ChatterInfo ci;
	Socket connection;

		    
	public ChatterRegistration (ChatterInfo ci, Socket connection) {
		this.ci = ci;
		this.connection  = connection;
	}
    
    public ChatterInfo getChatterInfo() {
        return ci;
    }
	
}


class ChatterInfo implements Serializable {

	    
	private String username;
	private String password;
	private String address;
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public String getAddress() {
		return this.address;
	}
	
	public ChatterInfo (String username, String password) {
		this.username = username;
		this.password = password;
	}

	public String getName() {
		return username;
	}
	
	public String toString() {
	
		String retVal = "Username: " + username + ", Password: " + password;
		return retVal;
	}
	    
}

class ChatRequest implements Serializable {

	int portAddress;
	ChatterInfo sourceChatter;
	ChatterInfo[] targetChatter;
	
	public ChatRequest(ChatterInfo sourceChatter,ChatterInfo[] targetChatter,int portAddress) {
	
		System.out.println("ChatRequest: In the constructor");
		this.sourceChatter = sourceChatter;
		this.targetChatter = targetChatter;
		this.portAddress   = portAddress;
	}

	public ChatterInfo getSourceChatter() {
		return sourceChatter;
	}

	public ChatterInfo[] getTargetChatters() {
		return targetChatter;
	}
	
	public int getPortAddress() {
		return portAddress;
	}
	
	public String toString() {
		String res = "Source chatter=" + sourceChatter.getName() +
					 "Target chatters = ";
		for (ChatterInfo c:targetChatter) {
			res = res + c.getName() + ",";
		}
		res = res + "port=" + portAddress;
		return res;
	}
}

// Define interface and the enum to process server commands
// This uses interfaces for enums, which makes it possible to declare
// separate methods for each of the enum values.
// These are the commands that are sent from the clients.
interface ChatterCommandInterface {

	void process(Chatter c, Socket connection, ObjectInputStream ois, ObjectOutputStream oos) throws Exception;
}

enum ChatterCommand implements ChatterCommandInterface {

	ACKNOWLEDGEMENT {

        public void process(Chatter c, Socket connection, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
			
		}	
	},
	
	REGISTER_INCOMING {
	
		public void process(Chatter c, Socket connection, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
			
            
            c.sendAcknowledgement(oos);
			ChatterInfo newChatterInfo = (ChatterInfo) ois.readObject();
			c.sendAcknowledgement(oos);
			c.addRegistration(new ChatterRegistration( newChatterInfo, connection ));
            c.sendAcknowledgement(oos);
            System.out.println( "<connected to the new chatter " + newChatterInfo.getName() + ">" );
		}
	},
    
    DO_REGISTER {
    
		public void process(Chatter c, Socket connection, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
			
            Thread.sleep(1000);
            System.out.println( "Sending incoming register command to server" );
			oos.writeObject(ChatterCommand.REGISTER_INCOMING);
            System.out.println( "Waiting for acknowledgement" );
			c.recieveAcknowledgement(ois);
            System.out.println( "Writing my info" );
			oos.writeObject(c.whoAmI());
            System.out.println( "Waiting for acknowledgement" );
            c.recieveAcknowledgement(ois);
            c.addRegistration(new ChatterRegistration( c.getChatRequest().getSourceChatter(), connection ));
            System.out.println( "<connected to the new chatter " + c.getChatRequest().getSourceChatter().getName() + ">" );
		}    
    },
    
    SEND_TEXT {
    
		public void process(Chatter c, Socket connection, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
			
            oos.writeObject(ChatterCommand.RECIEVE_TEXT);
            c.recieveAcknowledgement(ois);
            ChatObjectInterface coi = c.getFirstChatObject();
            
            coi.doPreProcess();
            System.out.print( c.whoAmI().getName() + "> " );
            coi.processChatObject();
            coi.send(oos);
		}        
    },

    RECIEVE_TEXT {
    
		public void process(Chatter c, Socket connection, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
			
            c.sendAcknowledgement(oos);
            ChatObjectInterface coi = new TextChatObject("");
            coi.recieve(ois);
            System.out.print( c.getChatterByConnection(connection).getName() + "> " );
            coi.processChatObject();
		}        
    },

}
