
import java.io.*;

class Chatter {

	private ChatterInfo info;
	
	ChatServerClientInterface c;
	
	public Chatter (String userName) {
	
		info = new ChatterInfo(userName, "");
	}
	
	public ChatterInfo getInfo() {
		return info;
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
