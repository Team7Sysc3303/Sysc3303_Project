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

- Abdulrahman: Helped in implenting the client class + responsibe for the UML class diagram and UCM
- Denis: Implemented the Server class (indicate your part exaxtly)
- Franklin: Implemented the Server class (indicate your part exaxtly)
- Hussain: Implemented client class
- Omar: Helped in implenting the client class + responsibe for the UML class diagram and UCM
- Suhib: Implemented the Server class (Listener)


Set up and Test Instructions
----------------------------
1. Extract the zipped project folder.
2. luanch Eclipse, click on open project and select the extracted file.
3. Open 3 console windows in Eclipse to see all three outputs of the classes.
4. Run the Server.java class first and Intermediate.java second and then run the Client.java class.


Usage Instructions
------------------

You'll be primarily interfacing with Client.java. 


Client Select Options:
----------------------
1. Read File
2. Write File
3. Quit File
----------------------

1. Type "Read", "Write" or "Q"  to select one of the three options stated above. The following instructions will then be shown on the console depending on which option was selected. In this case the read option was selected:


Client: Enter the type of request (Read/ Write) [Press Q to shutdown]:

Read

Client: Enter the file directory and file name (with extension) [Press Q to shutdown]: 

2. Follow the instructions and then test it for both the read and write requests using the test files available in the project folder.

Names of test files to be used:
- lessthan512.txt
- 512.txt
- morethan512.txt
- 1024.txt
- 99*512.txt


Notes: 
- For both read and write requests, the full absolute file path needs to be entered in order for the program to work.
- We assume the transferring goes sequentially, i.e bloch#1 , Block#2 ... etc
