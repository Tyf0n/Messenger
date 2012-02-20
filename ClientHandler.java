
import java.io.*;
import java.net.*;

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

	// Initialize the output and input streams for socket communication.
	ObjectOutputStream oos;
	ObjectInputStream ois;
	
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
	
	public void sendUpdateMsg() {
	
		try {
			oos.writeObject(ClientCommand.UPDATE_MEMBERS);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
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

			while (true) {

				// Process the command that was sent by the client.
				ServerCommandInterface sci = (ServerCommandInterface)ois.readObject();
				System.out.println( "Thread processing command: " + sci);
				sci.process(this);
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
			cs.updateAllHandlers();
			System.out.println( "Thread exiting." );
			return;
		}
		catch (SocketException e) {
		
			System.out.println( "Client " + address + " disconnected " );
			System.out.println( "Removing chatter at " + address );
			int index = cs.getChatterByAddress(address.toString());
			cs.removeChatter(index);
			cs.updateAllHandlers();
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
