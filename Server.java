
// Usage:
//        java Server
//
// There is no provision for ending the server gracefully.  It will
// end if (and only if) something exceptional happens.

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map.Entry;
import java.util.Properties;

public class Server {

	protected static ConcurrentHashMap<String, String> UsersWithPasswords = new ConcurrentHashMap<String, String>();
	protected static boolean loggedIn = false;
	protected static CopyOnWriteArrayList<String> loggedInUsers = new CopyOnWriteArrayList<String>();
	protected static ConcurrentHashMap<String, CopyOnWriteArrayList<String>> groupMemeber = new ConcurrentHashMap<String, CopyOnWriteArrayList<String>>();
	protected static ConcurrentHashMap<String, CopyOnWriteArrayList<String>> groupAdmins = new ConcurrentHashMap<String, CopyOnWriteArrayList<String>>();
	protected static ConcurrentHashMap<String, Boolean> groupVisablity = new ConcurrentHashMap<String, Boolean>();
	protected static ConcurrentHashMap<String, String> messageArchive = new ConcurrentHashMap<String, String>();
	protected static CopyOnWriteArrayList<String> invalidUsernames = new CopyOnWriteArrayList<String>();

	public static void main(String[] args) {

		getGroupFromFile();
		getTextLog();
		invalidUsernames.add(" ");
		invalidUsernames.add("Server");
		invalidUsernames.add("server");
		invalidUsernames.add("admin");
		invalidUsernames.add("Admin");
		invalidUsernames.add("");

		// This table will be shared by the server threads:
		ClientTable clientTable = new ClientTable();

		ServerSocket serverSocket = null;

		try {
			serverSocket = new ServerSocket(Port.number);
		} catch (IOException e) {
			Report.errorAndGiveUp("Couldn't listen on port " + Port.number);
		}

		try {
			// We loop for ever, as servers usually do.
			while (true) {
				// Listen to the socket, accepting connections from new clients:
				Socket socket = serverSocket.accept(); // Matches AAAAA in
														// Client.java

				// This is so that we can use readLine():
				BufferedReader fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintStream toClient = new PrintStream(socket.getOutputStream());

				// client name is now added to the client table in
				// ServerReceiver

				Report.behaviour("user has connected");

				// We create and start a new thread to read from the client:
				(new ServerReceiver(fromClient, clientTable, toClient)).start();

				// server sender now gets opened in the ServerReceiver
			}
		} catch (IOException e) {
			// Lazy approach:
			Report.error("IO error " + e.getMessage());
			// A more sophisticated approach could try to establish a new
			// connection. But this is beyond this simple exercise.
		}
	}

	// this checks to see if the password for that user is correct and checks
	// the users existence and
	// checks that the user is not already logged in
	public static boolean checkPassword(String username, String password) {
		if (!loggedIn(username)) {
			if (checkDup(username)) {
				if (UsersWithPasswords.get(username).equals(password)) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
		return false;
	}

	// this checks if the user exists in the table
	public static boolean checkDup(String name) {
		if (UsersWithPasswords.containsKey(name)) {
			return true;
		} else {
			return false;
		}
	}

	// checks to see if that user is logged in
	public static boolean loggedIn(String name) {
		for (String user : loggedInUsers) {
			if (user.equals(name)) {
				return true;
			}
		}
		return false;
	}

	// saves all the data in the hashmap to a file
	public static void saveToFile() {
		Properties properties = new Properties();
		for (Entry<String, String> entry : UsersWithPasswords.entrySet()) {
			properties.put(entry.getKey(), entry.getValue());
		}

		try {
			properties.store(new FileOutputStream("userName.properties"), null);
		} catch (FileNotFoundException e) {
			System.out.println("file has not been found");
		} catch (IOException e) {
			System.out.println("there has been a i/o exception");
		}
	}

	// gets all the data from the file and fills the HashMap
	public static void getFromFile() {
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream("userName.properties"));
		} catch (IOException e) {
			System.out.println("I/O exception");
		}

		for (String key : properties.stringPropertyNames()) {
			UsersWithPasswords.put(key, properties.get(key).toString());
		}
	}

	//returns true if the user exists within that groups admins
	public static boolean checkAdmin(String group, String username) {
		for (String key : groupAdmins.keySet()){
			if (key.equals(group)) {
				CopyOnWriteArrayList<String> admins = groupAdmins.get(key);
				for (String adminUser : admins) {
					if (adminUser.equals(username)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// checks if there are any admins online and then returns one of them
	public static String returnAdmin(String group) {
		if (checkGroup(group)) {
			String admin = sameElementArrayList(loggedInUsers, groupAdmins.get(group));
			return admin;
		} else {
			return null;
		}
	}

	//this checks two arraylists to see if they have the same element in them
	public static String sameElementArrayList(CopyOnWriteArrayList<String> arrayList1,
			CopyOnWriteArrayList<String> arrayList2) {
		for (String user1 : arrayList1) {
			for (String user2 : arrayList2) {
				if (user1.equals(user2)) {
					return user1;
				}
			}
		}
		return null;
	}

	// This checks for the existence of a group
	public static boolean checkGroup(String groupName) {
		for (Entry<String, CopyOnWriteArrayList<String>> singleEntry : groupMemeber.entrySet()) {
			String key = singleEntry.getKey();
			if (key.equals(groupName)) {
				return true;
			}
		}
		return false;
	}

	// adds a user and makes that person an admin
	//this creates adds the necessary lists to the HashMap and saves this to files
	public static synchronized boolean createGroup(String groupName, String currentUser) {
		if (groupName.length() > 0) {
			if (!checkGroup(groupName)) {
				CopyOnWriteArrayList<String> tempUsers = new CopyOnWriteArrayList<String>();
				CopyOnWriteArrayList<String> tempAdmins = new CopyOnWriteArrayList<String>();
				tempUsers.add(currentUser);
				tempAdmins.add(currentUser);
				groupMemeber.put(groupName, tempUsers);
				groupAdmins.put(groupName, tempAdmins);
				groupVisablity.put(groupName, true);
				messageArchive.put(groupName, "");
				saveGroupToFile(groupName);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	// checks if this users is in the group creates an array list from the
	// original HashMap
	public static boolean userInGroup(String groupName, String user) {
		CopyOnWriteArrayList<String> userGroup = groupMemeber.get(groupName);
		if (userGroup == null) {
			return false;
		} else {
			if (userGroup.contains(user)) {
				return true;
			}
			return false;
		}
	}

	// returns false if the group does not exist
	public static synchronized boolean addUser(String group, String user) {
		if (!userInGroup(group, user)) {
			groupMemeber.get(group).add(user);
			saveGroupToFile(group);
			return true;
		} else {
			return false;
		}
	}

	// returns true if the user is able to be removed and has been removed
	public static synchronized boolean removeUser(String group, String user) {
		if (userInGroup(group, user)) {
			groupMemeber.get(group).remove(user);
			saveGroupToFile(group);
			return true;
		} else {
			return false;
		}
	}

	// can add any user as admin they dont need to be part of the group already
	public static synchronized boolean addAdmin(String group, String user) {
		if (!checkAdmin(group, user)) {
			groupAdmins.get(group).add(user);
			saveGroupToFile(group);
			return true;
		} else {
			return false;
		}
	}

	// Not Possible to have a group with no admin remove admin will always remove that user as well
	public static synchronized boolean removeAdmin(String group, String user) {
		if (checkAdmin(group, user)) {
			groupMemeber.get(group).remove(user);
			groupAdmins.get(group).remove(user);
			saveGroupToFile(group);
			return true;
		} else {
			return false;
		}
	}

	//returns a specific set if users given a groupName
	public static CopyOnWriteArrayList<String> returnUsers(String groupName) {
		CopyOnWriteArrayList<String> users = new CopyOnWriteArrayList<String>();
		for (Entry<String, CopyOnWriteArrayList<String>> singleEntry : groupMemeber.entrySet()) {
			String key = singleEntry.getKey();
			if (key.equals(groupName)) {
				users = singleEntry.getValue();
			}
		}
		return users;
	}

	//group visibility is set to true or false this is also stored in a file
	public static boolean setGroupVisablity(String groupName, boolean val) {
		if (checkGroup(groupName)) {
			groupVisablity.replace(groupName, groupVisablity.get(groupName), val);
			saveGroupToFile(groupName);
			return true;
		} else {
			return false;
		}
	}

	// key
	// 1 == true
	// 0 == false
	// -1 == failed
	public static int getGroupVisablity(String groupName) {
		if (checkGroup(groupName)) {
			if (groupVisablity.get(groupName)) {
				return 1;
			} else {
				return 0;
			}
		} else {
			return -1;
		}
	}

	//this adds a string into message archive for that specific group
	public static void addString(String groupName, String toAdd) {
		if (checkGroup(groupName)) {
			messageArchive.put(groupName, messageArchive.get(groupName) + "\n" + toAdd);
			saveTextLog(groupName);
		}
	}

	public static String returnGroupString(String group) {
		return messageArchive.get(group);
	}

	//saves the groups into a file these are stored in the srcs files
	public static void saveGroupToFile(String groupName) {
		try {
			PrintWriter writer = new PrintWriter("srcs/" + groupName + ".txt", "UTF-8");
			writer.println(getGroupVisablity(groupName));
			for (String eachUser : groupAdmins.get(groupName)) {
				writer.println("admin " + eachUser);
			}
			for (String eachUser : groupMemeber.get(groupName)) {
				writer.println(eachUser);
			}
			writer.close();
		} catch (IOException e) {
			System.out.println("group saving has broken");
		}
	}

	// takes all the files from inside the srcs file and creates the groups from that so the 
	//server is in the same state as it was when the server was exited
	public synchronized static void getGroupFromFile() {
		File dir = new File("srcs");
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			BufferedReader br = null;
			FileReader fr = null;
			for (File file : directoryListing) {
				try {

					String groupName = file.getName().substring(0, file.getName().length() - 4);
					fr = new FileReader(file);
					br = new BufferedReader(fr);

					String sCurrentLine;

					br = new BufferedReader(new FileReader(file));

					boolean adminSet = false;

					while ((sCurrentLine = br.readLine()) != null) {
						if (sCurrentLine.length() > 5 && sCurrentLine.substring(0, 5).equals("admin")) {
							if (!adminSet) {
								createGroup(groupName, sCurrentLine.substring(6));
								adminSet = true;
							} else {
								addAdmin(groupName, sCurrentLine.substring(6));
							}
						} else if (sCurrentLine.substring(0, 1).equals("0")
								|| sCurrentLine.substring(0, 1).equals("1")) {
							if (sCurrentLine.substring(0, 1).equals("0")) {
								setGroupVisablity(groupName, false);
							} else if (sCurrentLine.substring(0, 1).equals("1")) {
								setGroupVisablity(groupName, true);
							}
						} else {
							addUser(groupName, sCurrentLine);
						}
					}
				} catch (IOException e) {

					e.printStackTrace();

				} finally {
					try {
						if (br != null) {
							br.close();
						}
						if (fr != null) {
							fr.close();
						}

					} catch (IOException e) {
						System.out.println("error closing the buffered reader and the file reader");
					}
				}
			}
		} else {
			System.out.println("directory has not been found");
		}
	}

	//this saves the text log for all the groups 
	public static synchronized void saveTextLog(String groupName) {
		try {
			PrintWriter writer = new PrintWriter("messLog/" + groupName + ".txt", "UTF-8");
			writer.print(messageArchive.get(groupName));
			writer.close();
		} catch (IOException e) {
			System.out.println("group saving has broken");
		}
	}

	//this retrieves the text log and saves it into the HashMap
	public static synchronized void getTextLog() {
		File dir = new File("messLog");
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			BufferedReader br = null;
			FileReader fr = null;
			for (File file : directoryListing) {
				try {

					String groupName = file.getName().substring(0, file.getName().length() - 4);
					fr = new FileReader(file);
					br = new BufferedReader(fr);

					String sCurrentLine;

					br = new BufferedReader(new FileReader(file));

					if (checkGroup(groupName)) {
						while ((sCurrentLine = br.readLine()) != null) {
							if (!sCurrentLine.equals("")){
								messageArchive.put(groupName, messageArchive.get(groupName) + "\n" + sCurrentLine);
							}
						}
					}

				} catch (IOException e) {

					e.printStackTrace();

				} finally {
					try {
						if (br != null) {
							br.close();
						}
						if (fr != null) {
							fr.close();
						}

					} catch (IOException e) {
						System.out.println("error closing the buffered reader and the file reader");
					}
				}
			}
		} else {
			System.out.println("directory has not been found");
		}
	}
	
	//this returns a html version of the group string by taking a template that can be found in the main file 
	//add replacing the specific tags with formatted html so that it can be easily displayed 
	public static String returnHtml(String groupName){
		String templateString = null;
		if (checkGroup(groupName)){
			try {
				templateString = new String(Files.readAllBytes(Paths.get("template.html")), StandardCharsets.UTF_8);
			} catch (IOException e) {
				System.out.println("problem with reading the template html file");
			}
			
			String body = "<h1>" + returnGroupString(groupName).replaceAll("\n", "</h1><h1>") + "</h1>";
			
			String title = (groupName + " message log");
			templateString = templateString.replace("$title", title);
			templateString = templateString.replace("$body", body);
			return templateString;
		}
		return templateString;
	}
}