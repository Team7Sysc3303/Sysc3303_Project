SYSC 3303 - Iteration 1
Team #7
-----------------------
-----------------------


Team Members
------------

Abdulrahman Alnaaim - 100909383
Denis Chupin - 100909755
Franklin Chibueze Ndudirim - 100977934
Hussain Aljabri - 100935515
Omar Ibrahim - 100943448
Suhib Habush - 100898373


Responsibilities
----------------

- Abdulrahman: Helped in implenting the client class + responsibe fro the UML-class and UCM
- Denis: Implemented the Server class (indicate your part exaxtly)
- Franklin: Implemented the Server class (indicate your part exaxtly)
- Hussain: Implemented client class
- Omar: Helped in implenting the client class + responsibe fro the UML-class and UCM
- Suhib: Implemented the Server class (Listener)


Set up and Test Instructions
----------------------------
1. Extract sysc3303Project-i1.
2. luanch Eclipse, click on open project and select the extracted file.
3. Open 3 console windows in Eclipse to see all three outputs of the classes.
4. Run the Server.java class first and Intermediate.java second and then run the Client.java class.

5. Folders "/TFTP-Server-Storage-Folder" and "/TFTP-Client-Storage-Folder"
will be created automatically under your home. This is the location where
the resulting files will be saved under. 
Server folder has files uploaded from the client to the server.
Client folder has files downloaded from the server to the client

//Check this part after testing the code...
...................................................
If you're on windows it is:
C:\Users\<user name>\TFTP-Server-Storage-Folder
C:\Users\<user name>\TFTP-Client-Storage-Folder
If you're on linux it is
/Users/username/TFTP-Server-Storage-Folder
/Users/username/TFTP-Client-Storage-Folder
...................................................


Usage Instructions
------------------

You'll be primarily interfacing with Client.java. 


Client Select Option:
----------------------
1. Read File
2. Write File
3. Exit File

Select option : 

1. Type "Read", "Write" or "Q"  to select one of the three options stated above.

Select option:
Client: Enter the type of request (Read/ Write) [Press Q to shutdown]:

Read

Client: Enter File name, with extension (e.g .txt) [Press Q to shutdown]:

[..........]

Names of Test Files.
-.....
-.....
-.....

// chenge 4, 5 and 6
......................................................................
4. Try write/read
5. Read request: Only enter the file name into the prompt.
6. Write request: Enter the full absolute path to your file
......................................................................


/**
We assume the transferring goes sequentially, i.e bloch#1 , Block#2 ... etc
*/
