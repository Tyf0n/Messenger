
import java.util.*;

class client {

	private ChatClient cc;

	public static void main(String[] args) {
	
		client cmdlineObj;
		if (args.length == 2) {
			cmdlineObj = new client (args[0], args[1], "");	
		}
		else {
			cmdlineObj = new client();
		}
		cmdlineObj.connect();
		cmdlineObj.run();
	}
	
	public client(String serverName,String username,String password) {
		cc = new ChatClient(serverName,username,"");
	}

	public void connect() {
		cc.connect();
	}
	
	public client() {
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

