https://git.cs.bham.ac.uk/jxf636/Assessed-ex1-SWW

# Part1
Part 1 - The approach that I took was to allow the user to type in quit then press enter and this would exit 
the client closing both the threads as there is a shared variable in client. The clientSenders sends 
quit and null to the server and the serverReceiver ends the thread and adds null to the message queue, 
when the serverSender receives null in its message queue it ends its thread.
Commit hash - 151e620a6b2ee0dc17376d39facb629017fc6352

# Part2
Part 2 – for this task I added in two new data structures to the Server they are a hashmap for the usernames linked to the passwords and an array list of all the logged in users. The ServerSender Thread is only started when the users name is known and any messages that need to be sent to user before there name is used are sent directly to the user. The users and their passwords are stored in the HashMap called UsersWithPasswords. Passwords have been implemented in this solution. There is a .properties file that contains the usernames and the passwords. On the Client side I have added in “Type operation: ” this is to help tell the user that they need to type an operation to perform such as login, register, message, etc. The user .properties file will be updated each time a new user is added to it. Only one user is able to login with the same username at one time, messages can be sent to any logged in users if the user is not currently logged in then an error message will be sent to the user.

Possable ambiguities - The user has to type 3 things inorder for it to be sent to the server all in one go, appart from typing logout or quit this only need to be typed followed by enter as a single command. Using control-c to exit could cause problems if the user tries to login again with the same username as using control-c will not set that user to be logged out only using "logout" or "quit" will allow for the user to be correctly logged out from the system.

Passwords have been implemented with the username as the key for the password for the value

The password is a .properties file that can be read in plain text with the username and password assosiated with each other the usernames are saved when a new one is added

-----------------------------------------------------
e.g

register

jack

123


this would register jack with the password 123

----------------------------------------------------- 
Commit hash - e94b92d737698ecd4c43cfbcb7d52ce3cb3101a6


# Part 3

## Commands
Anything with with "" marks round it should be replaced by your own text  
Set visablity to "secret" or to "visable"  

Create  
"Groupname"  

join  
"Groupname"  

leave  
"Groupname"  

remove user  
"Groupname"  
"Username"  

message group  
"Groupname"  
"message"  

add user  
"Groupname"  
"Username"  

add admin  
"Groupname"  
"Username"  

set group visabilty  
"Groupname"  
"visabilty"  

history  
"Groupname"  

create html  
"Groupname"     



## Class description
### Client.java
Still as normal not much has been changed
### ClientReceiver.java
Added a check so that the user is able to reecive the html data from the server when they request it
### ClientSender.java
This checks the function the user wants to perform and then makes sure the correct number of lines is sent to the server

### Server.java
This has had all the functions added to it so that the user is able to perfom all the actions needed to be part of a groups 
### ServerSender.java
This has stayed the same
### ServerReceiver.java
This has the function groupCommands that will take the type of function provided the user is logged in then it will perform the necessary function that the user needs
### New folder srcs and messLog
srcs is where the groups are saved and messLog is where the message log is saved and the html logs are saved to. There are a couple of txt files with some examples of the files that will be created when you make the program the txt files can be deleted if nessasary but the program will not work if there folders are deleted.

## Description
The methods that the ServerReceiver uses to be able to make changes to any of the HashMaps are stored in the Server class, ServerReciver has two big methods one for when the user is logged in and one for when the user is logged out. There are 4 main HashMaps ((groupMembers - this stores the name of the group as key then the value is a CopyOnWriteArrayList the array list stores all the users), (groupAdmins - this has the same setup to groupMembers but the arrayList has all the admins for that group), (groupVisablity - this stores the name of the group as the key and the value as a boolean variable groups start of as visable whem first created) and (messageArchive - this is a HashMap with the key and value as string the messages for the groups are appended to the end of there corresponding strings)) these are all the main data types in Server. This does not inclide UsersWithPasswords as this is described above. invalidUsernames is just a CopyOnWriteArrayList that has any usernames that are not allowed to be used by the user.   
The Client side has not changed too much there has been some changes in ClientReceiver to allow for the user to be able to accept or deny a user to join there group and ClientReceiver checks through different arrays to see how many lines of text it should be sending to the server.

## Extras 
I have added the ability for the admin of a group to add more admins and add more users but only if that person is the admin of that group. Aswell as this I have added it so that all data about groups is saved to files so when the Server is exited it will take the data from the files so that all the groups that had previously been made are still there this includes the group users, admins, group visabilty and the chat logs. I have made it so what a groups messages are logged and can be viewed by the admin of that group this is done using the history command followed by the group that they want to view the history of. The user can also request a html vertion of the history provided that they are admin, this will be saved in the messLog folder.

Commit hash - 4e387133a5e67c66b26beec7c5060888e67da08c