package iteration1;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class ErrorSim{
	public static final int DATA_SIZE = 516;

	private boolean done = false;
	/**
	 * Mode the user wants to enter
	 * 0 Normal Mode
	 * 1 Lost Mode
	 * 2 Delayed Mode
	 * 3 Duplicate Mode
	 */
	private static int Selection;
	public static final long TIMEOUT = 3000;	//int value in miliseconds 

	private int serverPort = 69; // the server port will be initiated to 69 and will change according to the thread needed 
	private DatagramSocket serverSocket, clientSocket; // socket deceleration for all three required sockets 
	private DatagramPacket sendClientPacket, receiveClientPacket, receiveServerPacket, sendServerPacket; // packet deceleration for all packets being sent and received for both client and server
	private byte clientData[];
	private int clientPort;
	private int clientLength;
	private int serverLength;
	//private int mode;	// will have the value of the current error simulation mode 
	private int delay; // will store the amount of delay if we are running in delayed mode
	private boolean Read = false;
	private boolean Write = false;
	private boolean lastPacketWrite = false;
	private boolean lastPacketRead = false;
	private boolean firstPacket = true;
	private boolean end = false;
	private boolean errorReceived = false;
	private boolean errorOnServer = false;
	// stores which packet type we are altering -- default is 0 if we are running normally
	private int packetType = 0;
	// stores which packet number we are altering -- default is 0 if we are running normally
	private int packetNumber = 0;
	byte clientReply[] = new byte[DATA_SIZE]; // this will store the reply from the client
	byte serverReply[] = new byte[DATA_SIZE]; // this will store the reply from the server
	byte serverData[] = new byte[DATA_SIZE]; // this will store the response from the server
	byte trueLastPacket[] = new byte[2]; // will store the block number of the truly last packet to verify if we have received it or not
	
	public ErrorSim() {
		
		if (Selection == 0) {
			System.out.println("ErrorSim will be running in Normal mode");
		}

		// LOST MODE 
		else if (Selection == 1) {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println("User specified Lost Packet Mode");
			System.out.println("Enter packet type to be lost: 1 for RRQ; 2 for WRQ; 3 for DATA; 4 for ACK");
			packetType = input.nextInt();

			// If packet type to be lost is DATA or ACK, get the speific packet number
			if (packetType == 3 || packetType == 4){
				System.out.println("Which packet do you want to lose: ");
				packetNumber = input.nextInt();
			}

			// If the packet to be lost is a request, we will just lose the first packet
			else {
				packetNumber = 1;
			}

		}
		// DELAYED MODE
		else if (Selection == 2) {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println("User specified Delayed Packet Mode");
			System.out.println("Enter packet type to be delayed: 1 for RRQ; 2 for WRQ; 3 for DATA; 4 for ACK");
			packetType = input.nextInt();
			// If packet type to be delayed is DATA or ACK, get the speific packet number
			if (packetType == 3 || packetType == 4){
				System.out.println("Which packet do you want to delay: ");
				packetNumber = input.nextInt();
			}

			// If the packet to be delayed is a request, we will just delay the first packet
			else {
				packetNumber = 1;
			}
			System.out.println("Specify the delay in milliseconds: ");
			delay = input.nextInt();

		}
		// DUPLICATE MODE
		else if (Selection == 3) {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println("User specified Duplicate Packet Mode");
			System.out.println("Enter packet type to be delayed: 1 for RRQ; 2 for WRQ; 3 for DATA; 4 for ACK");
			packetType = input.nextInt();

			// If packet type to be duplicated is DATA or ACK, get the speific packet number
			if (packetType == 3 || packetType == 4){
				System.out.println("Which packet do you want to duplicate: ");
				packetNumber = input.nextInt();
			}

			// If the packet to be duplicated is a request, we will just duplicated the first packet
			else {
				packetNumber = 1;
			}

			System.out.println("Enter the delay between duplication in milliseconds: ");
			delay = input.nextInt();
		}

		
		clientData = new byte[DATA_SIZE];
		try{
			clientSocket = new DatagramSocket(23);
		} catch (SocketException e){
			System.err.println("SocketException: " + e.getMessage());
		}
		receiveClientPacket = new DatagramPacket(clientData, clientData.length);
		
		System.out.println("Waiting for client request...");

		try { // recieve client request
			clientSocket.receive(receiveClientPacket);
		} // end try 
		catch (IOException ie) {
			System.err.println("IOException error: " + ie.getMessage());
		} // end catch
		System.out.println("Recieved client's request");
		printInformation(receiveClientPacket);
		verifyReadWrite(receiveClientPacket);
		clientPort = receiveClientPacket.getPort();
		clientLength = receiveClientPacket.getLength();
		
		try {
			serverSocket = new DatagramSocket();
		} // end try 
		catch (SocketException se) {
			System.err.println("SocketException: " + se.getMessage());
		} // end catch

		System.out.println("ConnectionManagerESim: Thread started to service request!");
		try {
			receiveClientPacket = new DatagramPacket(clientData, clientLength, InetAddress.getLocalHost(), clientPort);
		} catch (UnknownHostException e) {
			System.err.println("UnknownHostException: " + e.getMessage());
		}
		System.out.println("Exiting Constructor");
	} // end constructor


	public void startOperation() {
		System.out.println(" " + Selection);
		// Keep looping until we are done with the data transfer.
		while (!end) {
			if (Selection == 0)
			{
				end = normalMode();
			}
			else if (Selection == 1)
			{
				end = lostMode();
			}
			else if (Selection == 2)
			{
				end = delayedMode();
			}
			else if (Selection == 3) {
				end = duplicateMode();
			}
		}//end while

		// If we are done with the data transfer, close sockets and return
		System.out.println("ConnectionManagerESim: closing its sockets and shutting down the thread");
		serverSocket.close();
		clientSocket.close();
	} // end method




	private boolean normalMode(){
		System.out.println("Now operating in Normal data transfer mode. \n");

		// If this is not the first iteration, i.e if you are just returning from a previous mode of operation, retrieve either a request, ACK or DATA from the client, depending on what was last sent to the client. 
		if (!firstPacket) {
			clientReceive();

		}//end if
		//if this is the initial iteration, send the initial request received from the client, to the Server
		serverSend();
		//doneTransfer();
		if (errorReceived || (lastPacketRead == true && trueLastPacket[0] == receiveServerPacket.getData()[2] && trueLastPacket[1] == receiveServerPacket.getData()[3]))//change
			return true;
		//if (done){ 
			//return true;
		//}
		// Check if this is not the first iteration and the request type is WRQ
		if (Write == true && !firstPacket) {
			System.out.println("Checking if this packet size...");
			printInformation(sendServerPacket);
			// Check if we have written the last packet to the server.
			if(sendServerPacket.getLength() < DATA_SIZE)
				lastPacketWrite = true;	
		} // end if

		//*********************************************************************************

		// Receive ACK or data from the server and send to the client. 
		serverReceive();
		clientSend();
		firstPacket = false;		// At this point, we are no longer in the first iteration of the data transfer. So, set this variable to false.
		// If an error was received from the server or we have completely and successfully written the client's desired data to the server, return true and exit.
		if (errorReceived || lastPacketWrite)
			return true;
		/* Check if this is not the first iteration and the request type is RRQ. If so, check if we have completely and successfully read the client's desired data from the server. If we have done this, we have to return 
		false, which will enable the program return to the begining of this method, so that the final ACK can be sent back to the server, after which we exit.*/
		if (Read == true) {
			verifyPacketSize();
			return false;
		} // end if
		return false;

	}






	private boolean lostMode()
	{
		if (packetType == 1 || packetType == 2) 
		{ // If RRQ or WRQ, we just dont send anything
			System.out.println("First Packet (RRQ or WRQ) wass lost");
			clientReceive();
			firstPacket = true;
			Selection = 0;
			return true;
			
		}
		else if (packetType == 3 || packetType == 4)
		{
			if (errorOnServer == true)
			{
				if (packetType == 3 || packetType == 4)
				{
					System.out.println("Data or Ack didn't reach Server");
					System.out.println("receive data from Client again");
					clientReceive();
				}
			}
			else 
			{
				System.out.println("Data Or Ack didn't reach Client ");
				System.out.println("receive data from Server again");
				serverReceive();
			}
		} return false;
	}
	private boolean duplicateMode()
	{
		System.out.println("Starting Duplication...");
		// If RRQ or WRQ is to be duplicated, the error simulator only recieves the Server's acknowledgement when the duplicate has been sent after a delay. This is then sent to the client only once and then the mode returns to normal. This way, the client only receives one ACK despite the duplicate data being sent.
		if (packetType == 1 || packetType == 2) { // If we are duplicating a RRQ or WRQ
			System.out.println("Sending first packet");
			serverSend();
			System.out.println("Error Simulator thread entering sleep mode");
			try {
				Thread.sleep(delay);	//delays the packet for the specified amount
			}// end  try
			catch (InterruptedException e) { } // end catch
			System.out.println("Sending duplicate packet after a " + delay + "ms delay");
			serverSend(); //send duplicate packet 

			serverReceive(); // recieve response from server

			clientSend(); // send server response to the client

			// Reset mode
			Selection = 0;

			firstPacket = false;
			return false;
		}// end if

		// If the client is trying to read data from the Server and the Server sends duplicate data to the client, the error simulator, after sending the client's request to the server, recieves the data from the Server and sends it to the client twice, after a delay
		else if (Read == true) { // this is a read request
			System.out.println("Read Data Duplication...");
			if (packetType == 3) { // DATA packet being duplicated from the server 

				if (!firstPacket){ // Recieve request packet from client
					clientReceive();
				}
				firstPacket = false;
				serverSend(); // Send request to the server
				doneTransfer();
				if (done) return true;
				serverReceive(); // Recieve desired packet from the server 

				// Verify that this is the right data packet to be duplicated
				if (foundPacket(receiveServerPacket)) {
					System.out.println("Error simulator recieved the data to be duplicated. Sending initial data to client...");
					clientSend();
					System.out.println("Putting thread to sleep for the specified delay time...");
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					// Revert to normal mode
					Selection = 0;
					System.out.println("After a delay of : " + delay + "ms, Sending duplicate data");
				}
				if (!(foundPacket(receiveServerPacket))){
					System.out.println("Data retrieved from the Server is not the desired data to be duplicated. Sending single data to client...");
				}
				clientSend();
				verifyPacketSize();
				firstPacket = false;
				return false; // Return false and continue the rest of the data transfer in normal mode
			}// end if

			else if (packetType == 4) { // Check if the user wants the client to send a duplicate ACK to the server.

				if (!firstPacket) {
					clientReceive();
				}
				// Verify that this is the right ACK packet to be duplicated
				if (foundPacket(receiveClientPacket)) {
					System.out.println("Error simulator recieved the ACK to be duplicated. Sending initial ACK...");
					serverSend();
					System.out.println("Putting thread to sleep for the specified delay...");
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					System.out.println("After a delay of " + delay + "ms, sending duplicate data...");
					// switch back to normal operation
					Selection = 0;
				}
				if (!(foundPacket(receiveServerPacket))){
					System.out.println("ACK retrieved from the Client is not the desired ACK to be duplicated. Sending single data to client...");
				}
				serverSend();
				// Check if the last acknowledge was just sent to the server
				doneTransfer();
				if (done){
					return true;
				}
				// If the last ACK has not been sent to the server, retrieve next 512 bytes of data from the server and send to the client.
				serverReceive();
				clientSend();
				// Check if we are done reading the client's desired data from the client, return false. This will take the program back to normal mode, where the last or next ACK will be sent to the server.
				verifyPacketSize();
				firstPacket = false;
				return false;
			}// end else if
		} // end read duplicate mode

		// This checks if the user wants to duplicate the data being written to the server
		else if (Write == true) { // Check if the user specified a write request
			System.out.println("Write Data Duplication...");
			if (packetType == 3) { // Check if the user specified a data duplication mode
				if (!firstPacket){
					clientReceive();
				}
				firstPacket = false;
				// Verify that this is the right DATA packet to be duplicated
				if (foundPacket(receiveClientPacket)){
					System.out.println("Error simulator found the data being written to the server, for duplication. Sending initial data...");
					serverSend();
					System.out.println("Putting thread to sleep for the specified delay");
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					// switch back to normal operation
					Selection = 0;
					System.out.println("After a delay of : " + delay + "ms, sending duplicate data...");
				}
				if (!(foundPacket(receiveServerPacket))){
					System.out.println("Data retrieved from the client is not the desired data to be duplicated. Sending single data to server...");
				}
				serverSend(); // If the duplicate was found, send the duplicate data to the server. Otherwise, this still sends the data it receives to the server, without duplicating it.
				verifyPacketSize(); // Check if the sent data was the last data to be transfered.
				serverReceive(); // receive ACK from the server
				clientSend(); // Send ACK to the client
				doneTransfer(); // check if we are done with the data transfer
				if (done){ 
					return true;
				} // If we are done with the data transfer, return true.
				else {
					firstPacket = false;
					return false;
				}// This will take the program back to normal mode, if not done, where the last or next DATA will be sent to the server normally, without duplication.
			}// end if

			else if (packetType == 4) { // Check if the user specified a data duplication mode

				if (!firstPacket) { // if this is not the first packet to be sent, recieve next data or ACK from the client. If this is the first data transfer, skip this loop and sent request to the server.
					clientReceive();
				} // end if

				serverSend(); // Send the client's packet to the server
				verifyPacketSize(); // Check if we have sent the last packet to the client, if this is not the first data transfer stage
				serverReceive(); // Retrieve an ACK from the server
				// Verify that this is the right ACK packet to be duplicated
				if (foundPacket(receiveServerPacket)) {
					System.out.println("Error Simulator recieved ACK packet to be duplicated to the client. Sending first ACK to the client...");
					clientSend(); // Send first ACK to the client
					System.out.println("Putting thread to sleep for the specified delay...");
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					// switch back to normal operation
					Selection = 0;
					System.out.println("After a delay of " + delay + "ms, sending duplicate ACK to the server...");
				}
				clientSend();
				doneTransfer();
				if (done){
					return true;
				}
				else{
					firstPacket = false;
					return false;
				}
			}// end else if
		}// End write ACK duplication mode
		return false;
	} // End duplication mode
	
	public boolean delayedMode(){
		if (packetType==1||packetType==2){//WRQ or RRQ
			System.out.println("Delaying the request");
			System.out.println("Error Simulator thread entering sleep mode");
			try {
				Thread.sleep(delay);	//delays the packet for the specified amount
			}// end  try
			catch (InterruptedException e) { } // end catch
			System.out.println("Sending delayed request after a " + delay + " ms delay");
			serverSend(); //send delayed packet
			serverReceive(); // receive response from server
			clientSend(); // send server response to the client			
			Selection = 0;// Reset mode
			firstPacket = false;
			return false;
		}//end if for requests
		else if (Read == true) { // this is a read request
			if (packetType == 3) { // DATA packet being delayed from the server 

				if (!firstPacket){ // Receive request packet from client
					clientReceive();
				}
				firstPacket = false;
				serverSend(); // Send request to the server
				doneTransfer();
				if (done) return true;
				serverReceive(); // Receive desired packet from the server 
			
				// Verify that this is the right data packet to be duplicated
				if (foundPacket(receiveServerPacket)) {
					System.out.println("Error simulator recieved the data to delay...");
					System.out.println("Putting thread to sleep for the specified delay time...");
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					// Revert to normal mode
					Selection = 0;
					System.out.println("After a delay of : " + delay + " ms, Sending delayed data");
					
				}//end if for found packet
				/*if (!(foundPacket(receiveServerPacket))){
					System.out.println("ACK retrieved from the Client is not the desired ACK to be duplicated. Sending single data to client...");
				}*/
				clientSend();
				verifyPacketSize();
				firstPacket = false;
				return false; // Return false and continue the rest of the data transfer in normal mode
			}//end of if for DATA delaying
			else if (packetType == 4) { // Check if the user wants the client to send a delayed ACK to the server.

				if (!firstPacket) {
					clientReceive();
				}
				// Verify that this is the right ACK packet to be delayed
				if (foundPacket(receiveClientPacket)) {
					System.out.println("Error simulator recieved the ACK to be delayed...");
					serverSend();
					System.out.println("Putting thread to sleep for the specified delay...");
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					System.out.println("After a delay of " + delay + "ms, sending delayed data...");
					// switch back to normal operation
					Selection = 0;
				}
				/*if (!(foundPacket(receiveClientPacket))){
					System.out.println("ACK retrieved from the Client is not the desired ACK to be duplicated. Sending single data to client...");
				}*/
				serverSend();
				// Check if the last acknowledge was just sent to the server
				doneTransfer();
				if (done){
					return true;
				}
				// If the last ACK has not been sent to the server, retrieve next 512 bytes of data from the server and send to the client.
				serverReceive();
				clientSend();
				// Check if we are done reading the client's desired data from the client, return false. This will take the program back to normal mode, where the last or next ACK will be sent to the server.
				verifyPacketSize();
				firstPacket = false;
				return false;
			}// end else if for ACK
		}//end of read
		// This checks if the user wants to delay the data being written to the server
		else if (Write == true) { // Check if the user specified a write request
			if (packetType == 3) { // Check if the user specified a data duplication mode
				if (!firstPacket){
					clientReceive();
				}
				firstPacket = false;
				// Verify that this is the right DATA packet to be delayed
				if (foundPacket(receiveClientPacket)){
					System.out.println("Error simulator found the data being written to the server to be delayed...");
					System.out.println("Putting thread to sleep for the specified delay");
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					// switch back to normal operation
					Selection = 0;
					System.out.println("After a delay of : " + delay + "ms, sending delayed data...");
				}
				/*if (!(foundPacket(receiveClientPacket))){
					System.out.println("Data retrieved from the client is not the desired data to be duplicated. Sending single data to server...");
				}*/
				serverSend(); // If the packet to delay was found, send  data to the server. Otherwise, this still sends the data it receives to the server, without delay.
				verifyPacketSize(); // Check if the sent data was the last data to be transfered.
				serverReceive(); // receive ACK from the server
				clientSend(); // Send ACK to the client
				doneTransfer(); // check if we are done with the data transfer
				if (done){ 
					return true;
				} // If we are done with the data transfer, return true.
				else {
					firstPacket = false;
					return false;
				}// This will take the program back to normal mode, if not done, where the last or next DATA will be sent to the server normally, without duplication.
			}// end if for delaying Data from client
			else if (packetType == 4) { // Check if the user specified an ACK delay
				if (!firstPacket) { 
					clientReceive();
				} // end if

				serverSend(); // Send the client's packet to the server
				verifyPacketSize(); // Check if we have sent the last packet to the client, if this is not the first data transfer stage
				serverReceive(); // Retrieve an ACK from the server
				// Verify that this is the right ACK packet to be duplicated
				if (foundPacket(receiveServerPacket)) {
					System.out.println("Error Simulator recieved ACK packet to be delayed...");
					clientSend(); // Send first ACK to the client
					System.out.println("Putting thread to sleep for the specified delay...");
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					// switch back to normal operation
					Selection = 0;
					System.out.println("After a delay of " + delay + "ms, sending delayed ACK to Client...");
				}
				clientSend();
				doneTransfer();
				if (done){
					return true;
				}
				else{
					firstPacket = false;
					return false;
				}
			}// end else if			
		}
		return false;
	}
	public void verifyPacketSize(){
		if (!firstPacket) {
			System.out.println("Checking if the packet is less than 512 bytes:");
			if (Read == true){
				printInformation(sendClientPacket);
				if(sendClientPacket.getLength() < DATA_SIZE) {
					lastPacketRead = true;
					trueLastPacket[0] = sendClientPacket.getData()[2];
					trueLastPacket[1] = sendClientPacket.getData()[3];
				}// end if
			}
			else if (Write == true){
				printInformation(sendServerPacket);
				if(sendServerPacket.getLength() < DATA_SIZE) {
					lastPacketWrite = true;	
					trueLastPacket[0] = sendServerPacket.getData()[2];
					trueLastPacket[1] = sendServerPacket.getData()[3];
				}//end if
			} // 
		}
	}

	public void doneTransfer(){
		if (Read == true){
			if (errorReceived || (lastPacketRead == true && trueLastPacket[0] == sendServerPacket.getData()[2] && trueLastPacket[1] == sendServerPacket.getData()[3])){
				done = true;
			}// end if
		}
		else if (Write == true){
			if (errorReceived || (lastPacketWrite == true && trueLastPacket[0] == sendClientPacket.getData()[2] && trueLastPacket[1] == sendClientPacket.getData()[3])){	
				done = true;	// Last packet is now sent. The thread will close
			}// end if
		}
		else{
			done = false;
		}
	}

	private void clientReceive() {
		System.out.println("ConnectionManagerESim: Waiting to receive packet from client");

		receiveClientPacket = new DatagramPacket(clientReply, clientReply.length);
		try { // wait to receive client packet
			clientSocket.receive(receiveClientPacket);
		}//end try 
		catch (IOException ie) {
			System.err.println("IOException error: " + ie.getMessage());
		}//end catch
		if (receiveClientPacket.getData()[1] == (byte)5)
			errorReceived = true;
		System.out.println("ConnectionManagerESim: Received packet from client");
		printInformation(receiveClientPacket);
		// updating the data and length in the packet being sent to the server
		clientData = receiveClientPacket.getData();
		clientLength = receiveClientPacket.getLength();
	}

	private void clientSend(){

		System.out.println("ConnectionManagerESim: Preparing packet to send to Client");

		// prepare the new send packet to the client
		try {
			sendClientPacket = new DatagramPacket(serverData, serverLength, InetAddress.getLocalHost(), clientPort);
		} // end try
		catch (UnknownHostException uhe) {
			uhe.printStackTrace();
			System.exit(1);
		} // end catch

		// send the packet to the client via the send socket 
		try {
			clientSocket.send(sendClientPacket);

		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		// print confirmation message that the packet has been sent to the client
		System.out.println("ConnectionManagerESim: response packet sent to client");
		// print out information about the packet being sent to the client
		printInformation(sendClientPacket);
	}

	private void serverReceive(){

		System.out.println("ConnectionManagerESim: Waiting to receive a packet from server...\n");

		receiveServerPacket = new DatagramPacket(serverReply, serverReply.length);

		// block until you receive a packet from the server
		try {
			serverSocket.receive(receiveServerPacket);
		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		// print out information about the packet received from the server if verbose
		printInformation(receiveServerPacket);
		if (receiveServerPacket.getData()[1] == (byte)5)
			errorReceived = true;
		serverData = receiveServerPacket.getData();
		serverLength = receiveServerPacket.getLength();
		// set the serverPort to the port we have just received it from (meaning to the Server Thread that will deal with this request
		serverPort = receiveServerPacket.getPort();

	}

	private void serverSend() {
		System.out.println("ConnectionManageESim: Preparing packet to send to Server");
		System.out.print(" " + clientData);
		// prepare the new send packet to the server
		try {
			sendServerPacket = new DatagramPacket(clientData, clientLength, InetAddress.getLocalHost(), serverPort);
		} // end try 
		catch (UnknownHostException uhe) {
			System.err.println("Unknown host exception error: " + uhe.getMessage());
		} // end catch

		// send the packet to the server via the send/receive socket to server port
		try {
			serverSocket.send(sendServerPacket);
		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		// print confirmation message that the packet has been sent to the server
		System.out.println("Packet sent to server");
		printInformation(sendServerPacket);
	}

	private boolean foundPacket(DatagramPacket p) {
		int type;
		int temp = 0;
		byte blk[] =new byte[2];
		// set type, to the type of packet it is based on its second byte
		type = (int)(p.getData()[1]);
		System.out.println("ConnectionManagerESim: packet type is " + type);
		// Create a new byte that stores the block number in the right 2 byte format
		if (packetNumber > 10){
			temp = packetNumber/10;
		}
		blk[0] = (byte) temp;
		// the low byte
		blk[1] = (byte) (packetNumber % 10);

		// the packet's block number that we are checking
		byte blockReceived[] = {p.getData()[2], p.getData()[3]};
		System.out.println("Block number received: " + Integer.toHexString(blockReceived[0]) + "" + Integer.toHexString(blockReceived[1]));
		// check if it is the right packet type
		if (type ==  packetType) {
			if (blk[0] == blockReceived[0] && blk[1] == blockReceived[1]){
				System.out.println("Error simulator has the right block number.");
				return true;
			} // end if
		} // end if
		return false;
	}// end method


	/*public void getUserInput() {
		if (Selection == 0) {
			System.out.println("ErrorSim will be running in Normal mode");
		}

		// LOST MODE 
		else if (Selection == 1) {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println("User specified Lost Packet Mode");
			System.out.println("Enter packet type to be lost: 1 for RRQ; 2 for WRQ; 3 for DATA; 4 for ACK");
			packetType = input.nextInt();

			// If packet type to be lost is DATA or ACK, get the speific packet number
			if (packetType == 3 || packetType == 4){
				System.out.println("Which packet do you want to lose: ");
				packetNumber = input.nextInt();
			}

			// If the packet to be lost is a request, we will just lose the first packet
			else {
				packetNumber = 1;
			}

		}
		// DELAYED MODE
		else if (Selection == 2) {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println("User specified Delayed Packet Mode");
			System.out.println("Enter packet type to be delayed: 1 for RRQ; 2 for WRQ; 3 for DATA; 4 for ACK");
			packetType = input.nextInt();
			// If packet type to be delayed is DATA or ACK, get the speific packet number
			if (packetType == 3 || packetType == 4){
				System.out.println("Which packet do you want to delay: ");
				packetNumber = input.nextInt();
			}

			// If the packet to be delayed is a request, we will just delay the first packet
			else {
				packetNumber = 1;
			}
			System.out.println("Specify the delay in milliseconds: ");
			delay = input.nextInt();

		}
		// DUPLICATE MODE
		else if (Selection == 3) {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println("User specified Duplicate Packet Mode");
			System.out.println("Enter packet type to be delayed: 1 for RRQ; 2 for WRQ; 3 for DATA; 4 for ACK");
			packetType = input.nextInt();

			// If packet type to be duplicated is DATA or ACK, get the speific packet number
			if (packetType == 3 || packetType == 4){
				System.out.println("Which packet do you want to duplicate: ");
				packetNumber = input.nextInt();
			}

			// If the packet to be duplicated is a request, we will just duplicated the first packet
			else {
				packetNumber = 1;
			}

			System.out.println("Enter the delay between duplication in milliseconds: ");
			delay = input.nextInt();
		}

		startOperation();
	}// end while*/
	
private void printInformation(DatagramPacket p) {
		
		// print out the information on the packet
		System.out.println("PACKET INFORMATION");
		System.out.println("Host: " + p.getAddress());
		System.out.println("Host port: " + p.getPort());
		System.out.println("Containing the following \nString: " + new String(p.getData()));
		System.out.println("Length of packet: " + p.getLength());
		System.out.println("Bytes: ");
		for (int i = 0; i < p.getLength(); i++) {
			System.out.print(Integer.toHexString(p.getData()[i]));
		} // end forloop
		System.out.println("\n******************************************************");
		System.out.println("\n\n");
	} // end method 


	public void verifyReadWrite (DatagramPacket p){
		System.out.println ("Verifying transfer type");
		if(p.getData()[0]!=0){
			System.exit(1);
			if(p.getData()[0]== (byte) 0 && p.getData()[1]==(byte) 1){
				System.out.println ("Transfer type is a RRQ");
				Read = true; 
			}
			else if(p.getData()[0]== (byte) 0 && p.getData()[1]==(byte) 2){
				System.out.println ("Transfer type is a WRQ"); 
				Write = true; 
			}
		}
	}


		public static void main(String[] args) {
			// the scanner to receive a user input
			Scanner in = new Scanner(System.in);

			// will hold the value if a valid choice has been entered
			boolean validEntry = false;
			boolean shutdown = false;

			while (!shutdown) { 
				while (!validEntry) {

					// print out information for the user depending on the mode of run they want to use
					System.out.println("0 - Normal\n1 - Lose a packet\n2 - Delay a packet\n3 - Duplicate\n9\n");
					System.out.println("Please enter a mode for the Error Simulator to start in:");

					Selection = in.nextInt();
					// check if a valid choice has been entered
					if (Selection == 0 || Selection == 1 || Selection == 2 || Selection == 3) {
						validEntry = true;
					}
					else {
						System.out.println("Invalid choice entered. Please try again!");
						validEntry = false;
					}
				} // end while

				
				ErrorSim esim = new ErrorSim();

				esim.startOperation();
				validEntry = false;
			}// end while	

			// close the Scanner
			in.close();
			System.out.println ("ErrorSim: shutting down");
			System.exit(0);
		} // end method
	} // end class
