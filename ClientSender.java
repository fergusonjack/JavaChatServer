import java.io.*;
import java.util.Arrays;
import java.util.List;


// Repeatedly reads recipient's nickname and text from the user in two
// separate lines, sending them to the server (read by ServerReceiver
// thread).

public class ClientSender extends Thread {

  private PrintStream server;
  List<String> twoLine = Arrays.asList("create","join","leave", "history");
  List<String> ThreeLine = Arrays.asList("remove user","message group","add user","add admin", "set group visabilty");

  ClientSender(PrintStream server) {
    this.server = server;
  }

  public void run() {
    // So that we can use the method readLine:
    BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
    try {
      // Then loop forever sending messages to recipients via the server:
      while (Client.runLoop) {
		String function = user.readLine();
		if (Client.request){
    		if (function.equals("yes")){
    			server.println("add user");
    			server.println(Client.groupName);
    			server.println(Client.userName); 
    			Client.request = false;
    			Client.groupName = "";
    			Client.userName = "";
    		} else {
    			Client.request = false;
    			server.println("message");
    			server.println(Client.userName);
    			server.println("you have not been added");
    			System.out.println(Client.userName + " has not been added");
    		}
    	} else if (function.toLowerCase().equals("logout")){
			server.println(function);
			server.println("null");
			server.println("null");
		} else if ((function.toLowerCase()).equals("quit")){
			Client.runLoop = false;
			server.println("quit");
			server.println("quit");
			server.println("Null"); 
		} else if ((function.toLowerCase()).equals("message")){
			String recipient = user.readLine();
			String text = user.readLine();
			server.println(function);
			server.println(recipient); // Matches CCCCC in ServerReceiver.java
			server.println(text); 
		} else if (twoLine.contains(function.toLowerCase())) {
			String userInput = user.readLine();
			server.println(function.toLowerCase());
			server.println(userInput.toLowerCase());
      	} else if(ThreeLine.contains(function.toLowerCase())) {
      		String inputOne = user.readLine();
      		String inputTwo = user.readLine();
			server.println(function.toLowerCase());
			server.println(inputOne.toLowerCase());
			server.println(inputTwo.toLowerCase());
      	} else if (function.toLowerCase().equals("create html")){
      		String userInputOne = user.readLine();
      		server.println(function.toLowerCase());
      		server.println(userInputOne.toLowerCase());
        } else {
			String recipient = user.readLine();
			String text = user.readLine();
			server.println(function);
			server.println(recipient); // Matches CCCCC in ServerReceiver.java
			server.println(text);      // Matches DDDDD in ServerReceiver.java
		}
	  }
    }
    catch (IOException e) {
      Report.errorAndGiveUp("Communication broke in ClientSender" 
                        + e.getMessage());
    }
  }
}
