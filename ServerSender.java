import java.io.*;

// Continuously reads from message queue for a particular client,
// forwarding to the client.

public class ServerSender extends Thread {
  private MessageQueue clientQueue;
  private PrintStream client;
  private boolean runLoop = true;

  public ServerSender(MessageQueue q, PrintStream c) {
    clientQueue = q;   
    client = c;
  }

  public void run() {
    while (runLoop) {
      Message msg = clientQueue.take(); // Matches EEEEE in ServerReceiver
	  if (msg.getText() == null){   //this checks to see if the Receiver thread has been closed as null will added to the list if the user exits
		  runLoop = false;
	  } else {
		client.println(msg); // Matches FFFFF in ClientReceiver
	  }
    }
  }
}
