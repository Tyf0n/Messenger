
class chattest {

	public static void main(String[] args) {
	
		String name = "ramesh";
		if (args.length == 1) {
		    name = args[0];
		}
		Chatter c = new Chatter(name);
	}
}
