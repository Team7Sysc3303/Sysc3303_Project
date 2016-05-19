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
			mode = 0;// Reset mode
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
					mode = 0;
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
					mode = 0;
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
					mode = 0;
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
					mode = 0;
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
