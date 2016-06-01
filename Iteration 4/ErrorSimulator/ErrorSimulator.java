package ErrorSimulator;



import java.io.IOException;
import java.net.*;

public class ErrorSimulator extends Thread{
	public static final int DATA_SIZE = 516;
	private boolean verbose;
	private boolean done = false; // Variable to keep track of whether the file transfer is done
	// Mode of operation entered by the user. 0 for Normal mode, 1 for lost mode, 2 for delayed mode, 3 for duplicate mode, 4 to shutdown the error simulator
	private int Selection;
	private int serverPort = 69; // the server port will be initiated to 69 and will change according to the thread needed 
	private DatagramSocket serverSocket, clientSocket; // socket deceleration for all three required sockets 
	private DatagramPacket sendClientPacket, receiveClientPacket, sendServerPacket , receiveServerPacket; // packet deceleration for all packets being sent and received for both client and server
	private byte clientData[]; // Stores the data received from the client
	private int clientPort; //Stores the port number used to receive the client's request
	private int clientLength; //Stores the length of the dat received from the client
	private int serverLength; // stores the length of the data received from the server
	private int delay; // Stores the delay specified by the user
	private boolean Read = false; // This variable is true if and only if data is being read from the server
	private boolean Write = false; // This variable is true if and only if data is being written to the server
	private boolean lastPacketWrite = false; // This variable is true if and only if the error simulator receives the last data packet to be written to the server
	private boolean lastPacketRead = false; // This variable is true if and only if the error simulator receives the last packet data to be read from the server
	private boolean firstPacket = true;
	private boolean end = false; // This variable stores the return values of the various modes of operation and is true if and only if the full file transfer process between the client and the server has been completed
	private boolean errorReceived = false;
	private int packetType = 0; // Stores the type of packet to be lost, duplicated or delayed, as specified by the user
	private int packetNumber = 0; // Stores the packet number to be lost, duplicated or delayed, as specified by the user
	byte clientReply[] = new byte[DATA_SIZE]; // Store the client's reply
	byte serverReply[] = new byte[DATA_SIZE]; // // Store the server's reply
	byte serverData[] = new byte[DATA_SIZE]; // Stores the data received from the server
	byte trueLastPacket[] = new byte[2]; // Stores the correct last block number of a data transfer to make sure that the file transfer process has been fully successful
	
	public ErrorSimulator(boolean verbose, int Selection, int delay, int packetType, int packetNumber, byte[] clientData, int clientPort, int clientLength, boolean Read, boolean Write, boolean errorReceived) {
		this.verbose = verbose;
		this.clientData = clientData;
		this.clientPort = clientPort;
		this.clientLength = clientLength;
		this.Selection = Selection;
		this.packetType = packetType;
		this.delay = delay;
		this.packetType = packetType;
		this.packetNumber = packetNumber;
		this.Read = Read;
		this.Write = Write;
		this.errorReceived = errorReceived;
		try {
			serverSocket = new DatagramSocket(); // Create a new Datagram socket for receiving data from the server.
		} // end try 
		catch (SocketException se) {
			System.err.println("SocketException: " + se.getMessage());
		} // end catch
		
		try {
			clientSocket = new DatagramSocket();
		} // end try 
		catch (SocketException se) {
			System.err.println("SocketException: " + se.getMessage());
		} // end catch
		
		if(verbose){
		System.out.println("Starting Simulation");
		}
		try {
			receiveClientPacket = new DatagramPacket(clientData, clientLength, InetAddress.getLocalHost(), clientPort);
		} catch (UnknownHostException e) {
			System.err.println("UnknownHostException: " + e.getMessage());
		}
		if(verbose){
		System.out.println("Exiting Constructor");
		}
	} // end constructor


	public void run() {
		if(verbose){
			System.out.println(" " + Selection);
		}
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
		if(verbose){
			System.out.println("File transfer done. I am closing my client and server sockets");
		}
		serverSocket.close();
		clientSocket.close();
	} // end method




	private boolean normalMode(){
		if(verbose){
		System.out.println("Now operating in Normal data transfer mode.");
		}

		// If this is not the first iteration, i.e if you are just returning from a previous mode of operation, retrieve either a request, ACK or DATA from the client, depending on what was last sent to the client. 
		if (!firstPacket) {
			clientReceive();

		}//end if
		//if this is the initial iteration, send the initial request received from the client, to the Server
		serverSend();
		doneTransfer();
		if (done){ 
			return true;
		}
		// Check if this is not the first iteration and the request type is WRQ
		if (Write == true && !firstPacket) {
			if(verbose){
				System.out.println("Checking if this packet size...");
				printInformation(sendServerPacket);
			}
			// Check if we have written the last packet to the server.
			if(sendServerPacket.getLength() < DATA_SIZE)
				lastPacketWrite = true;	
		} // end if

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
		if(verbose){
			System.out.println("Starting Lost Mode...");
		}
        if (Read == true && packetType == 1){ // this is a read request and the packet to be lost is an RRQ
        	if(verbose){
        		System.out.println("I'm sorry but I think I just lost an RRQ... Serves you right :p");
        		printInformation(receiveClientPacket);
        	}
			return true; 
		}// end 
        
        if (Write == true && packetType == 2){ // this is a read request and the packet to be lost is an RRQ
        	if(verbose){
        		System.out.println("I'm sorry but I think I just lost a WRQ... Serves you right :p");
        		printInformation(receiveClientPacket);
        	}
			return true; 
		}// end 
        
        else if (Read == true && packetType == 4){ // This is a read request and the packet to be lost is an ACK
            if (!lastPacketRead) {
                if (!firstPacket) {
                    // If this is not the first packet, receive ACK from the client client
                    clientReceive();
                }// end if
                firstPacket = false; // this is no longer the first packet
                // check if this is the ACK packet to be lost
                if (foundPacket(receiveClientPacket)) { // if this is the packet to be ACK to be lost, do not send it to the server. instead, receive the previous data from the server and send to the client again
                	if(verbose){
                		System.out.println("I just got the ACK I want and I am not sending it to the server.. ***Evil Laugh***");
                		printInformation(receiveClientPacket);
                	}
                    //Reset to normal mode of operation
                    Selection = 0;
                }//end if
                else { // If this is not the ACK packet to be lost, send it to the server
                    serverSend();
                }//end else
                //receive next data packet from the server
                serverReceive();
                // Send the server's data packet to the client
                clientSend();
                // Check if that was the last data to be transfered
                if(sendClientPacket.getLength() < DATA_SIZE){
                    lastPacketRead = true;
                }
                return false;					
            } // end if
            else if (lastPacketRead) { // If the last data packet had just been read from the server
                // receive from client
                clientReceive();
                // check if this is the ACK packet to be lost
                if (foundPacket(receiveClientPacket)) { // if this is the last ACK and it is the packet to be ACK to be lost, do not send it to the server. instead, return true and exit
                	if(verbose){
                		System.out.println("I just got the ACK I want and I am not sending it to the server.. ***Evil Laugh***");
                		printInformation(receiveClientPacket);
                	}
                    return true;
                }//end if
                else {
                    serverSend(); // if this is the last ACK and it is not the packet to be ACK to be lost, send it to the server and return true and exit
                    return true; 
                }
            }//end else if
        }// end else if
        
        else if (Read == true && packetType == 3){ // This is a read request and the packet to be lost is DATA
            if (!firstPacket) {
			    clientReceive(); //If theis is not the first packet, receive ACK from the client
			}// end if
			firstPacket = false; // this is no longer the first packet so set this variable to false
			serverSend(); // Send the client's response to the server
			serverReceive(); // Receive new data from the server
			// check if this is the DATA packet to be lost
			if (foundPacket(receiveServerPacket)) { // If this is the DATA packet to be lost, do not send it to the server. Instead, wait for server to re-send data
				if(verbose){
				System.out.println("I just received the DATA you want and...I AM NOT SENDING IT! :p");
			    printInformation(receiveServerPacket);
				}
			    // Reset to normal mode of operation
			    Selection = 0;
			    serverReceive(); // wait on server to resend DATA
			}//end if
               
			//send the data to the client
			clientSend();
			// Check if that was the last data to be transfered
			if(sendClientPacket.getLength() < DATA_SIZE){
				lastPacketRead = true;
            }
            return false;					
		} // end else if
        else if (Write == true && packetType == 4){ // This is a write request and the packet to be lost is an ACK
            if (firstPacket) { // if this is the first packet
                serverSend();
				serverReceive();
                // Check if this is the ACK packet to be lost
				if(foundPacket(receiveServerPacket)) { // If the ACK to be lost is the first, which gives the client permission to write to the server, do not send it and exit because the client will timeout if it does not have permission to write to the server.
					if(verbose){
					System.out.println("You do not have permission to write to the server because I just lost your first ACK...Sorry");
					printInformation(receiveServerPacket);
					}
				    return true;
			    }// end if
			    else { // If the ACK to be lost is not the first, send it to the client and keep searching
				    firstPacket = false; // this is no longer the first packet so set this variable to false
				    clientSend();
				    return false;
			    }//end else	
		    }// end if        
            clientReceive(); // Receive DATA from the client
		    // Check if that was the last data to be transfered
		    if (receiveClientPacket.getLength() < DATA_SIZE) {
			    lastPacketWrite = true;
		    }
		    serverSend(); // Send the client's data to the server
		    serverReceive(); // Recieve an ACK from the server
		    // Check if this is the ACK packet to be lost
		    if (foundPacket(receiveServerPacket)) { // this is the packet we want to lose
		    	if(verbose){
			    System.out.println("I just lost the 'TOP SECRET' ACK you wanted me to lose... you happy?");
			    printInformation(receiveServerPacket);
		    	}
		        // Check if that was the last ACK
		        if (lastPacketWrite) { // this is the last ACK packet that we are sending back to the client
			        return true;
		        }
		        else {
		            //Reset to normal mode of operation
		            Selection = 0;
			        return false;
		        }
	        }//end if
		    //send server response to the client
		    clientSend();
		    // Check if that was the last ACK
		    if (lastPacketWrite){
			    return true;
            }
		    else{
			    return false;
		    }// end else if
        }
         else if (Write == true && packetType == 3){ // This is a write request and the packet to be lost is an DATA
            if (!firstPacket) {
				clientReceive(); // If this is not the first packet, receive new data from the client
			}// end if
			firstPacket = false; // this is no longer the first packet so set this variable to false
			// Check if this is the ACK packet to be lost
			if (foundPacket(receiveClientPacket)) { // this is the packet we want to lose
				if(verbose){
				System.out.println("Em... Well... I dont know what just happened but I just lost your DATA packet.");
				printInformation(receiveClientPacket);
				}
				// Reset to normal mode of operation
				Selection = 0;
                return false;
			}//end if
            // Send the DATA packet to the server
			serverSend();
            if (sendServerPacket.getLength() < DATA_SIZE){
					lastPacketWrite = true; // Check if that was the last DATA packet to be written to the server
            }
			serverReceive(); // Receive response from the server
			clientSend(); // Send server response to the client
            return false;					
		} // end else if
		return false;
	}
            
	private boolean duplicateMode()
	{
		if(verbose){
		System.out.println("Starting Duplication...");
		}
		// If RRQ or WRQ is to be duplicated, the error simulator only receives the Server's acknowledgement when the duplicate has been sent after a delay. This is then sent to the client only once and then the mode returns to normal. This way, the client only receives one ACK despite the duplicate data being sent.
		if (packetType == 1 || packetType == 2) { // If we are duplicating a RRQ or WRQ
			if(verbose){
			System.out.println("Sending first packet");
			}
			serverSend();
			if(verbose){
			System.out.println("Error Simulator thread entering sleep mode");
			}
			try {
				Thread.sleep(delay);	//delays the packet for the specified amount
			}// end  try
			catch (InterruptedException e) { } // end catch
			if(verbose){
			System.out.println("Sending duplicate packet after a " + delay + "ms delay");
			}
			serverSend(); //send duplicate packet 

			serverReceive(); // receive response from server

			clientSend(); // send server response to the client

			// Reset to Normal mode of operation
			Selection = 0;

			firstPacket = false; // first packet has been transfered to the client so set this variable to false and return
			return false;
		}// end if

		// If the client is trying to read data from the Server and the Server sends duplicate data to the client, the error simulator, after sending the client's request to the server, receives the data from the Server and sends it to the client twice, after a delay
		else if (Read == true) { // this is a read request
			if(verbose){
			System.out.println("Read Data Duplication...");
			}
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
					if(verbose){
					System.out.println("Error simulator recieved the data to be duplicated. Sending initial data to client...");
					}
					clientSend();
					if(verbose){
					System.out.println("Putting thread to sleep for the specified delay time...");
					}
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					// Revert to normal mode
					Selection = 0;
					if(verbose){
					System.out.println("After a delay of : " + delay + "ms, Sending duplicate data");
					}
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
					if(verbose){
					System.out.println("Error simulator recieved the ACK to be duplicated. Sending initial ACK...");
					}
					serverSend();
					if(verbose){
					System.out.println("Putting thread to sleep for the specified delay...");
					}
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					if(verbose){
					System.out.println("After a delay of " + delay + "ms, sending duplicate data...");
					}
					// switch back to normal operation
					Selection = 0;
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
			if(verbose){
			System.out.println("Write Data Duplication...");
			}
			if (packetType == 3) { // Check if the user specified a data duplication mode
				if (!firstPacket){
					clientReceive();
				}
				firstPacket = false;
				// Verify that this is the right DATA packet to be duplicated
				if (foundPacket(receiveClientPacket)){
					if(verbose){
					System.out.println("Error simulator found the data being written to the server, for duplication. Sending initial data...");
					}
					serverSend();
					if(verbose){
					System.out.println("Putting thread to sleep for the specified delay");
					}
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					// switch back to normal operation
					Selection = 0;
					if(verbose){
					System.out.println("After a delay of : " + delay + "ms, sending duplicate data...");
					}
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
					if(verbose){
					System.out.println("Error Simulator recieved ACK packet to be duplicated to the client. Sending first ACK to the client...");
					}
					clientSend(); // Send first ACK to the client
					if(verbose){
					System.out.println("Putting thread to sleep for the specified delay...");
					}
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					// switch back to normal operation
					Selection = 0;
					if(verbose){
					System.out.println("After a delay of " + delay + "ms, sending duplicate ACK to the server...");
					}
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
			if(verbose){
			System.out.println("Delaying the request");
			System.out.println("Error Simulator thread entering sleep mode");
			}
			try {
				Thread.sleep(delay);	//delays the packet for the specified amount
			}// end  try
			catch (InterruptedException e) { } // end catch
			if(verbose){
			System.out.println("Sending delayed request after a " + delay + " ms delay");
			}
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
					if(verbose){
					System.out.println("Error simulator recieved the data to delay...");
					System.out.println("Putting thread to sleep for the specified delay time...");
					}
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					// Revert to normal mode
					Selection = 0;
					if(verbose){
					System.out.println("After a delay of : " + delay + " ms, Sending delayed data");
					}
					
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
					if(verbose){
					System.out.println("Error simulator recieved the ACK to be delayed...");
					}
					serverSend();
					if(verbose){
					System.out.println("Putting thread to sleep for the specified delay...");
					}
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					if(verbose){
					System.out.println("After a delay of " + delay + "ms, sending delayed data...");
					}
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
					if(verbose){
					System.out.println("Error simulator found the data being written to the server to be delayed...");
					System.out.println("Putting thread to sleep for the specified delay");
					}
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					// switch back to normal operation
					Selection = 0;
					if(verbose){
					System.out.println("After a delay of : " + delay + "ms, sending delayed data...");
					}
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
					if(verbose){
					System.out.println("Error Simulator recieved ACK packet to be delayed...");
					}
					clientSend(); // Send first ACK to the client
					if(verbose){
					System.out.println("Putting thread to sleep for the specified delay...");
					}
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					// switch back to normal operation
					Selection = 0;
					if(verbose){
					System.out.println("After a delay of " + delay + "ms, sending delayed ACK to Client...");
					}
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
			if(verbose){
			System.out.println("Checking if the packet is less than 512 bytes:");
			}
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
		if(verbose){
		System.out.println("Waiting to receive packet from client");
		}

		receiveClientPacket = new DatagramPacket(clientReply, clientReply.length);
		try { // wait to receive client packet
			clientSocket.receive(receiveClientPacket);
		}//end try 
		catch (IOException ie) {
			System.err.println("IOException error: " + ie.getMessage());
		}//end catch
		if (receiveClientPacket.getData()[1] == (byte)5){
			errorReceived = true;
		}
		if(verbose){
		System.out.println("ConnectionManagerESim: Received packet from client");
		printInformation(receiveClientPacket);
		}
		// updating the data and length in the packet being sent to the server
		clientData = receiveClientPacket.getData();
		clientLength = receiveClientPacket.getLength();
	}

	private void clientSend(){
		if(verbose){
		System.out.println("Sending packet to the Client");
		}

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
		
		if(verbose){
		// print confirmation message that the packet has been sent to the client
		System.out.println("ConnectionManagerESim: response packet sent to client");
		// print out information about the packet being sent to the client
		printInformation(sendClientPacket);
		}
	}

	private void serverReceive(){
		if(verbose){
		System.out.println("Receiving Packet from Server");
		}

		receiveServerPacket = new DatagramPacket(serverReply, serverReply.length);

		// block until you receive a packet from the server
		try {
			serverSocket.receive(receiveServerPacket);
		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch
		
		if(verbose){
			System.out.println("Received Server Packet");
			// print out information about the packet received from the server if verbose
			printInformation(receiveServerPacket);
		}
		if (receiveServerPacket.getData()[1] == (byte)5)
			errorReceived = true;
		serverData = receiveServerPacket.getData();
		serverLength = receiveServerPacket.getLength();
		// set the serverPort to the port we have just received it from (meaning to the Server Thread that will deal with this request
		serverPort = receiveServerPacket.getPort();

	}

	private void serverSend() {
		if(verbose){
		System.out.println("Sending Packet to the Server");
		System.out.print(" " + clientData);
		}
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
		
		if(verbose){
		System.out.println("Packet sent to server");
		printInformation(sendServerPacket);
		}
	}

	private boolean foundPacket(DatagramPacket p) {
		int type;
		int temp = 0;
		byte blk[] =new byte[2];
		// set type, to the type of packet it is based on its second byte
		type = (int)(p.getData()[1]);
		if(verbose){
		System.out.println("ConnectionManagerESim: packet type is " + type);
		}
		// Create a new byte that stores the block number in the right 2 byte format
		blk[0] = (byte)((packetNumber - (packetNumber % 256)) /256);
		// the low byte
		blk[1] = (byte)(packetNumber % 256);

		// the packet's block number that we are checking
		byte blockReceived[] = {p.getData()[2], p.getData()[3]};
		if(verbose){
		System.out.println("Block number received: " + Integer.toHexString(blockReceived[0]) + "" + Integer.toHexString(blockReceived[1]));
		}
		// check if it is the right packet type
		if (type ==  packetType) {
			if (blk[0] == blockReceived[0] && blk[1] == blockReceived[1]){
				if(verbose){
				System.out.println("Error simulator has the right block number.");
				}
				return true;
			} // end if
		} // end if
		return false;
	}// end method


	
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
		}
		System.out.println("\n******************************************************");
		System.out.println("\n\n");
	} // end method 
}
