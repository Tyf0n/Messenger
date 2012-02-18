
import java.net.*;
import java.io.*;
import java.util.*;

interface ChatServerClientInterface {

	public void connect(Chatter c);
}

enum ClientCommand {

	ACKNOWLEDGEMENT {
	
	}
}

class ChatServerClient implements ChatServerClientInterface {

	Socket serverConnection;
	String serverName;
	String userName;
	String password;
	
	public ChatServerClient(String serverName,String userName,String password) {
	
		this.serverName = serverName;
		this.userName   = userName;
		this.password   = password;
	}
	
	public static ChatServerClientInterface getClientObject(String serverName,String userName,String password) {

		ChatServerClient chatServerClientObject = new ChatServerClient(serverName,userName,password);
		ChatServerClientInterface castedObject = (ChatServerClientInterface) chatServerClientObject;
		return castedObject;
	}

	public boolean recieveAcknowledgement(ObjectInputStream ois) throws Exception {
	
		ClientCommand cc = (ClientCommand) ois.readObject();
		if (cc == ClientCommand.ACKNOWLEDGEMENT) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public void sendAcknowledgement(ObjectOutputStream oos) throws Exception {
	
		oos.writeObject(ClientCommand.ACKNOWLEDGEMENT);
	}
	
	public void sendServerCommand(ServerCommand sc,ObjectOutputStream oos) throws Exception {
		oos.writeObject(sc);
	}
	
	public void connect(Chatter c) {
	
		try {
		
			serverConnection = new Socket (serverName, 3000);
			System.out.println( "Connection established to " + serverName );
			ObjectOutputStream oos = new ObjectOutputStream(serverConnection.getOutputStream());
			ObjectInputStream ois = new ObjectInputStream(serverConnection.getInputStream());
			
			sendServerCommand(ServerCommand.REGISTER,oos);
			recieveAcknowledgement(ois);
			oos.writeObject(c.getInfo());
			recieveAcknowledgement(ois);
			
			/*
			sendServerCommand(ServerCommand.GET_MEMBERS,oos);
			ChatterInfo[] chatters = (ChatterInfo[]) ois.readObject();
			for (ChatterInfo ci:chatters) {
				System.out.println(ci);
			}
			*/
		}
		catch (Exception e) {
		
			System.out.println ("Error:" + e.getMessage());
		} 
	}	
}
