

import java.net.*;
import java.io.*;
import java.util.*;

// Interface of the ChatClient.
interface ChatClientInterface {

	public void connect();
}


// ChatClient which actually manages the chat functionalities at the client side.
class ChatClient implements ChatClientInterface, CommandlineInterface {

	Socket serverConnection;
	String serverName;
	String userName;
	String password;
	
	ChatterInfo myself;
	ChatterInfo[] availableChatters;
	
	ChatClientConnectionManager cccm;
	ArrayList<Chatter> chatterArrayList;
    Chatter activeChatter = null;
    client clientObj;
	
	int freePort;
    
    boolean inFocus;
    
    public void setToFocus() {
        inFocus = true;
    }
    
    public void setOutOfFocus() {
        inFocus = false;
    }
        
    public void processInput(String lineOfText, client clientObj) {
    
        this.clientObj = clientObj;
        if (!lineOfText.isEmpty()) {
            
            String[] args = lineOfText.split("\\s+");
            String command = args[0];
            String[] remArgs = new String[args.length-1];
            
            if (args.length>1) {
                System.arraycopy(args,1,remArgs,0,args.length-1);
            }
        
            if(command.equals("list")) {
                printChatters();
            }
            else if(command.equals("connect")) {
                connectChat(args);
            }
            else if(command.equals("exit")) {
                System.exit(0);
            }
        }
        System.out.print( "Enter command: " );
    }
	
	public ChatClient() {
	
		availableChatters = new ChatterInfo[0];
	}		
	
	public ChatClient(String serverName,String userName,String password) {
	
		this.serverName 	= serverName;
		this.userName   	= userName;
		this.password		= password;
		this.myself       	= new ChatterInfo( userName, password );
		
		Random ro           = new Random();
		freePort            = ro.nextInt(5000);
		
		availableChatters 	= new ChatterInfo[0];
		chatterArrayList  	= new ArrayList<Chatter>();
	}

	public void addChat(Chatter newChatter) {
		chatterArrayList.add(newChatter);
        clientObj.changeFocus((CommandlineInterface)newChatter);
	}
    
    public void sendChatObject(ChatObjectInterface coi) {
        
        if (activeChatter != null) {
            activeChatter.sendChatObject(coi);
        }
    }
	
	public static ChatClientInterface getClientObject(String serverName,String userName,String password) {

		ChatClient ChatClientObject = new ChatClient(serverName,userName,password);
		ChatClientInterface castedObject = (ChatClientInterface) ChatClientObject;
		return castedObject;
	}

	public void updateChatters(ChatterInfo[] availableChatters) {
		this.availableChatters = availableChatters;
	}
	
	public void connectChat(String[] args) {
	
		System.out.println("connectChat: in this routine now");
		
		ArrayList<ChatterInfo> chatterList = new ArrayList<ChatterInfo>();
		for (String chatter:args) {
			for (ChatterInfo thisChatter:availableChatters) {
				if (chatter.equals(thisChatter.getName()) && 
				    (!chatter.equals(myself.getName()))) {
					System.out.println("Adding chatter " + thisChatter.getName() + " for conversation.");
					chatterList.add(thisChatter);
				}
			}
		}
		
		ChatterInfo[] chatterArray = new ChatterInfo[chatterList.size()];
		chatterList.toArray(chatterArray);
		
		ChatRequest cr = new ChatRequest(myself,chatterArray,freePort++);
		System.out.println("Created chat request " + cr);
		
		System.out.println("Queueing chat request ");		
		cccm.queueChatRequest(cr);
		
		System.out.println("Queueing command to initiate chat");		
		cccm.queueCommand(ClientCommand.INITIATE_CHAT);
	}
    
	public void printChatters() {
	
		for(ChatterInfo c:availableChatters) {
			System.out.println( " * " + c.getName() + " ( " + c.getAddress() + " ) " );
		}
	}
	
	public void closeConnection() {
		
		System.out.println( "Queueing command" );
		cccm.queueCommand( ClientCommand.EXIT );
	}
	
	public void connect() {
	
		try {
			
			cccm = new ChatClientConnectionManager(this, serverName);
		}
		catch (Exception e) {
		
			System.out.println ("Error:" + e.getMessage());
		} 
	}
		
	public ChatterInfo whoAmI() {
	
		return myself;
	}
	
}

