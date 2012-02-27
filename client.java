

import java.util.*;

interface CommandlineInterface {

    void processInput(String lineInput,client cmdlineObject);
    void setToFocus();
    void setOutOfFocus();
}

class client {

	private ChatClient cc;
    private CommandlineInterface focussedCli;

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
    
    public void changeFocus(CommandlineInterface cli) {
        focussedCli = cli;
    }
	
	void run() {
	
        changeFocus((CommandlineInterface)cc);
        focussedCli.processInput("list",this);
		Scanner sc = new Scanner(System.in);
		while(true) {
            
            String lineOfText = sc.nextLine();
            focussedCli.processInput(lineOfText,this);
		}
	}
}

