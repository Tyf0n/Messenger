
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
			ObjectInputStream ois  = new ObjectInputStream(serverConnection.getInputStream());
			
			System.out.println( "Sending REGISTER command to server." );
			sendServerCommand(ServerCommand.REGISTER,oos);
			
			System.out.println( "Waiting for acknowledgement from server." );
			recieveAcknowledgement(ois);
			
			System.out.println( "Sending info to server." );
			oos.writeObject(c.getInfo());
			
			System.out.println( "Waiting for acknowledgement from server." );
			recieveAcknowledgement(ois);
			
			System.out.println( "Sending GET_MEMBERS command to server." );
			sendServerCommand(ServerCommand.GET_MEMBERS,oos);
			
			System.out.println( "Waiting for acknowledgement from server." );
			recieveAcknowledgement(ois);
			
			System.out.println( "Recieving chatters information from server." );
			ChatterInfo[] chatters = (ChatterInfo[]) ois.readObject();
			
			for (ChatterInfo ci:chatters) {
				System.out.println(ci);
			}

			System.out.println( "Sleeping for 10 seconds before exit." );
			Thread.sleep(10000);
			oos.close();
			ois.close();
			serverConnection.close();
		}
		catch (Exception e) {
		
			System.out.println ("Error:" + e.getMessage());
		} 
	}	
}
