
/*-------------------------------------------------------------------------------------------

This file contains the common things used inbetween the client and the server code.

-------------------------------------------------------------------------------------------*/

import java.io.*;
import java.net.*;

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

// Define interface and the enum to process server commands
// This uses interfaces for enums, which makes it possible to declare
// separate methods for each of the enum values.
// These are the commands that are sent from the clients.
interface ServerCommandInterface {

	void process(ClientHandler ch) throws Exception;
}

enum ServerCommand implements ServerCommandInterface {

    // The command to register a new chatter.
	REGISTER {
	
		public void process(ClientHandler ch) throws Exception {
			
			// Get all the required variables
			ChatServer cs          = ch.getChatServer();
			ObjectInputStream ois  = ch.getInputStream();
			ObjectOutputStream oos = ch.getOutputStream();
			Socket connection      = ch.getConnection();
			
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
			cs.addChatter(newChatter,ch);
			
			// Send the acknowledgement that I have registered.
			System.out.println( "Sending acknowledgement." );
			cs.sendAcknowledgement(oos);
			
			cs.updateAllHandlers();
		}
	},

	// The command to unregister a chatter.
	UNREGISTER {
	
		public void process(ClientHandler ch) throws Exception {

			// Get all the required variables
			ChatServer cs          = ch.getChatServer();
			ObjectInputStream ois  = ch.getInputStream();
			ObjectOutputStream oos = ch.getOutputStream();
		
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
	
		public void process(ClientHandler ch) throws Exception {

			// Get all the required variables
			ChatServer cs          = ch.getChatServer();
			ObjectOutputStream oos = ch.getOutputStream();
		
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
