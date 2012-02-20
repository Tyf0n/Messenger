

import java.net.*;
import java.io.*;
import java.util.*;


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

