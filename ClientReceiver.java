import java.io.*;
import java.net.*;

// Gets messages from other clients via the server (by the
// ServerSender thread).

public class ClientReceiver extends Thread {

  private BufferedReader server;

  ClientReceiver(BufferedReader server) {
    this.server = server;
  }

  public void run() {
    // Print to the user whatever we get from the server:
    try {
      while (Client.runLoop) {
		String s = server.readLine(); // Matches FFFFF in ServerSender.java
        if (s != null) {
        	System.out.println(s);
        	if (s.length() > 26){
        		//allows the admin of the group to be able to accept to add a person to the group
        		//the username Server is not valid so this cannot be recreated by just sending a message
        		if (s.substring(0, 25).equals("From Server: do you want ")){
        			String[] userDetails = server.readLine().split("\\s+");
        			if (userDetails.length > 3){
	        			Client.request = true;
	        			Client.userName = userDetails[2];
	        			Client.groupName = userDetails[3]; 
        			}
        		//this is how the user is able to accept the html data from the server
        		} else if (s.substring(0, 25).equals("From Server: html file ha")) {
        			String groupName = server.readLine();
        			String htmlData = server.readLine().substring(13);
        			saveHtml(groupName.substring(13), htmlData);
        		}
        	}
        } else
		  Report.errorAndGiveUp("Server seems to have died");
      }
    }
	catch (SocketException e){
		Report.errorAndGiveUp("Socket has been closed");
	}
    catch (IOException e) {
	  //this seems to be running even when readLine should be returning null after a closed socket
      Report.errorAndGiveUp("Server seems to have died " + e.getMessage());
    }
  }
  
  //file saving for the html a file is created if it does not already exist
  public void saveHtml(String groupName, String stringToSave){
	  try {
		  	System.out.println("this has been run");
			PrintWriter writer = new PrintWriter("messLog/" + groupName + ".html", "UTF-8");
			writer.print(stringToSave);
			writer.close();
		} catch (IOException e) {
			System.out.println("group saving has broken");
		}
  }
}
