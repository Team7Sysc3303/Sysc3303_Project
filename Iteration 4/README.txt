SYSC 3303 - Iteration 5

Team #7

-----------------------

-----------------------

Team Members
------------

Hussain Aljabri - 100935515
Suhib Habush - 100898373
Abdulrahman Alnaaim - 100909383
Denis Chupin - 100909755
Franklin Chibueze Ndudirim - 100977934
Omar Ibrahim - 100943448


Responsibilities:
------------------

Hussain Aljabri - Implemented  both client and the server+ file testing + Fixed the errors from old iterations.
Suhib Habush -  file  testing
Abdulrahman Alnaaim - Timing Diagrams for all iterations (Fixed old mistakes) + file testing
Denis Chupin - Debugging
Franklin Chibueze Ndudirim - Implemented Error simulator + file testing
Omar Ibrahim - UML Class diagrams







Set up and Test Instructions

----------------------------

1. Extract the zipped project folder.
2. Luanch Eclipse, click on open project and select the extracted file.
3. Load the extracted files to Eclipse.
4. Run the server first, enter path location where you want to save tranfered files, Run the intermidate host second then the client.
5. Enter the port address for the other device on the network.
6.
7. Ready to request files transfer.


NOTE 1: In the machine to machine transfer, the intermidate host is set to be with the client side.

NOTE 2: Files bigger than 65kb, transfer will roll over.

Usage Instructions
------------------

You'll be primarily interfacing with ResponseHandler.java. 






Client Select Options:
----------------------

1. Read File

2. Write File

3. Quit File

----------------------




1. Type "Read", "Write" or "Q"  to select one of the three options stated above. The following instructions will then be shown on the console depending on which option was selected. In this case the read option was selected:





/**

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




*/


Notes: 

- For both read and write requests, the full absolute file path needs to be entered in order for the program to work.

- We assume the transferring goes sequentially, i.e bloch#1 , Block#2 ... etc
