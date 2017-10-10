import java.io.*;
import java.util.concurrent.CopyOnWriteArrayList;

// Gets messages from client and puts them in a queue, for another
// thread to forward to the appropriate client.

public class ServerReceiver extends Thread {
	private String myClientsName;
	private BufferedReader myClient;
	private ClientTable clientTable;
	private boolean runLoop = true;
	private boolean loggedIn = false;
	private PrintStream toClient;
	private int count = 0;

	public ServerReceiver(BufferedReader c, ClientTable t, PrintStream x) {
		toClient = x;
		myClient = c;
		clientTable = t;
	}

	public void run() {
		try {
			// this section is where the names from the file are loaded in
			Server.getFromFile();

			while (runLoop) {
				String functionInput = myClient.readLine();
				if (loggedIn) {
					loggedInCommands(functionInput);
				} else {
					loggedOutFunctions(functionInput);
				}
			}
		} catch (IOException e) {
			Report.error("Something went wrong with the client " + myClientsName + " " + e.getMessage());
			// No point in trying to close sockets. Just give up.
			// We end this thread (we don't do System.exit(1)).
		}
	}
	

	public void loggedInCommands(String function) throws IOException {
		if ((function.equals("quit") || function.equals("message") || function.equals("logout"))) {
			String recipient = myClient.readLine();
			String text = myClient.readLine();
			if (function.toLowerCase().equals("quit")) {
				// Closes corresponding thread
				loggedIn = false;
				Server.loggedInUsers.remove(myClientsName);
				sendMessage(myClientsName, myClientsName, null);
				System.out.println(myClientsName + " has disconnected");
				clientTable.remove(myClientsName);
				runLoop = false; // ends this loop
			} else if (function.equals("logout")) {
				// Closes corresponding thread
				System.out.println(myClientsName + " has logged out");
				loggedIn = false;
				Server.loggedInUsers.remove(myClientsName);
				sendMessage(myClientsName, myClientsName, null);
				clientTable.remove(myClientsName);
			} else if (recipient != null && text != null) {
				Message msg = new Message(myClientsName, text);
				MessageQueue recipientsQueue = clientTable.getQueue(recipient);
				if (recipientsQueue != null){
					recipientsQueue.offer(msg);
					sendMessage("Server", myClientsName, "The message has been sent");
				} else {
					Report.error("Message for unexistent client " + recipient + ": " + text);
					sendMessage("Server", myClientsName, "This person does not exist");
				}
			} else
				// No point in closing socket. Just give up.
				return;
		} else if (function.equals("create")) {
			String groupName = myClient.readLine();
			if (Server.createGroup(groupName, myClientsName)) {
				System.out.println("group " + groupName + " created");
				sendMessage("Server", myClientsName, "group has been created");
			} else {
				sendMessage("Server", myClientsName, "invalid group name");
			}
		} else if (function.equals("join")) {
			String groupName = myClient.readLine();
			if (Server.checkGroup(groupName)) {
				//checks that the group should be visable
				if (Server.getGroupVisablity(groupName) == 1){
					String admin = Server.returnAdmin(groupName);
					if (!(admin == null)) {
						sendMessage("Server", admin, "do you want to add " + myClientsName + " to " + groupName);
						sendMessage("Server", admin, myClientsName + " " + groupName);
					} else {
						sendMessage("Server", myClientsName, "There are no admins for this group online");
					}
				} else {
					sendMessage("Server", myClientsName, "This group does not exist");
				}
			} else {
				sendMessage("Server", myClientsName, "This group does not exist");
			}
		} else if (function.equals("leave")) {
			String groupName = myClient.readLine();
			if (Server.checkAdmin(groupName, myClientsName)) {
				Server.removeAdmin(groupName, myClientsName);
				Server.removeUser(groupName, myClientsName);
				sendMessage("Server", myClientsName, "removed from group");
			} else {
				if (Server.userInGroup(groupName, myClientsName)){
					Server.removeUser(groupName, myClientsName);
					sendMessage("Server", myClientsName, "you have left " + groupName);
				} else {
					sendMessage("Server", myClientsName, "group does not exist or your not in that group");
				}
			}
		} else if (function.equals("remove user")) {
			String groupName = myClient.readLine();
			String userNameToRemove = myClient.readLine();
			if (Server.checkAdmin(groupName, myClientsName)) {
				Server.removeUser(groupName, userNameToRemove);
				Server.removeAdmin(groupName, userNameToRemove);
				sendMessage("Server", myClientsName, "user removed");
				sendMessage("Server", userNameToRemove, "you have been remove from " + groupName);
			} else {
				if (Server.userInGroup(groupName, userNameToRemove)){
					Server.removeUser(groupName, userNameToRemove);
					sendMessage("Server", myClientsName, "user removed");
				} else {
					sendMessage("Server", myClientsName, "user or the group does not exist");
				}
			}
		} else if (function.equals("message group")) {
			String groupName = myClient.readLine();
			String message = myClient.readLine();
			if (Server.checkGroup(groupName)) {
				if (Server.userInGroup(groupName, myClientsName)) {
					CopyOnWriteArrayList<String> users = new CopyOnWriteArrayList<String>();
					users = Server.groupMemeber.get(groupName);
					for (String user : users) {
						if (!user.equals(myClientsName))
							sendMessage(myClientsName + " in " + groupName + ":  ", user, message);
					}
					Server.addString(groupName, (myClientsName + " sent " + message));
					sendMessage("Server", myClientsName, "message sent");
				} else {
					sendMessage("Server", myClientsName, "You are not part of this group");
				}
			} else {
				sendMessage("Server", myClientsName, "Group does not exist");
			}
		} else if (function.equals("add user")) {
			String groupName = myClient.readLine();
			String userToAdd = myClient.readLine();
			if (Server.checkAdmin(groupName, myClientsName)) {
				Server.addUser(groupName, userToAdd);
				sendMessage("Server", userToAdd, "You have been added");
				sendMessage("Server", myClientsName, userToAdd + " has been added");
			} else {
				sendMessage("Server", myClientsName, "You are not an admin or that group does not exist");
			}
		} else if (function.equals("add admin")) {
			String groupName = myClient.readLine();
			String adminToAdd = myClient.readLine();
			if (Server.checkAdmin(groupName, myClientsName)) {
				if (Server.userInGroup(groupName, adminToAdd)) {
					Server.addAdmin(groupName, adminToAdd);
					sendMessage("Server", myClientsName, "admin added");
					sendMessage("Server", adminToAdd, "you are now admin of " + groupName);
				} else {
					Server.addAdmin(groupName, adminToAdd);
					Server.addUser(groupName, adminToAdd);
					sendMessage("Server", myClientsName, "admin added");
					sendMessage("Server", adminToAdd, "you are now admin of " + groupName);
				}
			} else {
				sendMessage("Server", myClientsName, "you are not an admin or that group does not exist");
			}
		} else if(function.equals("set group visabilty")) {
			String groupName = myClient.readLine();
			String visablity = myClient.readLine();
			if (Server.checkAdmin(groupName, myClientsName)){
				if (visablity.equals("secret")){
					Server.setGroupVisablity(groupName, false);
					sendMessage("Server",myClientsName , groupName + " has been set to secret");
				} else if (visablity.equals("visable")){
					Server.setGroupVisablity(groupName, true);
					sendMessage("Server",myClientsName , groupName + " has been set to visable");
				} else {
					sendMessage("Server",myClientsName , groupName + " Please type secret or visable");
				}
			} else {
				sendMessage("Server", myClientsName, "you are not admin or the group does not exist");
			}
		} else if(function.equals("history")) {
			String groupName = myClient.readLine();
			if (Server.checkAdmin(groupName, myClientsName)){
				sendMessage("History Archive", myClientsName, Server.returnGroupString(groupName));
			} else {
				sendMessage("Server", myClientsName, "You are not admin");
			}
		} else if (function.equals("create html")){
			String groupName = myClient.readLine();
			if (Server.checkAdmin(groupName, myClientsName)){
				sendMessage("Server", myClientsName, "html file has been created in the users messLog folder");
				sendMessage("Server", myClientsName, groupName);
				sendMessage("Server", myClientsName, Server.returnHtml(groupName));
			} else {
				sendMessage("Server", myClientsName, "You are not admin or the group does not exist");
			}
		} else {
			//this helps to print an error message
			count++;
			if (count==3){
				sendMessage("Server", myClientsName, function + " is not a valid command!!");
				count=0;
				return;
			}
		}

	}

	//simplifies the message sending into one function
	public void sendMessage(String sender, String recipient, String message) {
		Message msg = new Message(sender, message);
		MessageQueue recipientsQueue = clientTable.getQueue(recipient);
		recipientsQueue.offer(msg);
	}

	// these are the functions that check to see what the user has typed (only
	// run when the user is not logged in)
	public void loggedOutFunctions(String function) {
		try {
			// this is the function that will register a new user it first reads
			// in the username and password
			// this is checked for duplication, if it is not a duplicate they
			// will be added to the HashMap, clientTable,
			// and the logged in users, they will be logged in automatically
			// from this and a ServerSender thread is started
			if (function.equals("register")) {
				String userName = myClient.readLine();
				String password = myClient.readLine();
				if (!Server.invalidUsernames.contains(userName)) {
					if (!Server.checkDup(userName)) {
						Server.UsersWithPasswords.put(userName, password);
						myClientsName = userName;
						clientTable.add(myClientsName);
						toClient.println(myClientsName + " has been registered");
						loggedIn = true;
						Server.loggedInUsers.add(myClientsName);
						Server.saveToFile();
						(new ServerSender(clientTable.getQueue(myClientsName), toClient)).start();
					} else {
						toClient.println("That username already exists");
					}
				} else {
					toClient.println("That is an invalid username");
				}

				// this allows a returning user to log back in, the user will
				// just have there username and password checked
				// then they will be added to the logged in users and will have
				// a serverSender thread started for them to be
				// able to receive messages
			} else if (function.equals("login")) {
				String userName = myClient.readLine();
				String password = myClient.readLine();
				if (Server.checkPassword(userName, password)) {
					myClientsName = userName;
					loggedIn = true;
					clientTable.add(myClientsName);
					toClient.println(userName + " has been logged in");
					Server.loggedInUsers.add(myClientsName);
					System.out.println("the user " + myClientsName + " has just logged in");
					(new ServerSender(clientTable.getQueue(myClientsName), toClient)).start();
				} else {
					toClient.println("log in has failed");
				}
			} else {
				// this runs i the users tries some other command even though
				// they are not logged in
				count++;
				if (count == 3) {
					toClient.println("You are not logged in");
					count = 0;
				}
				return;
			}
		} catch (IOException e) {
			Report.error("Something went wrong with the client " + myClientsName + " " + e.getMessage());
			// No point in trying to close sockets. Just give up.
			// We end this thread (we don't do System.exit(1)).
		}
	}
}
