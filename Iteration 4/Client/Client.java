package Client;


// TFTPClient.java
// This class is the client side for a very simple assignment based on TFTP on
// UDP/IP. The client uses one port and sends a read or write request and gets 
// the appropriate response from the server.  No actual file transfer takes place.   
import java.io.*;
import java.net.*;
import java.security.AccessControlException;
import java.util.*;

public class Client {

   private DatagramPacket sendPacket, receivePacket;
   private static DatagramSocket sendReceiveSocket;
   private static boolean request;
   private static String filename;
   private File writeFile;
   BufferedInputStream in;
   BufferedOutputStream out;
   private static int sendPort;
   private static boolean verbose = true;
   private static String path;
   
   // we can run in normal (send directly to server) or test
   // (send to simulator) mode
   public static enum Mode { NORMAL, TEST};

   public Client()
   {
      try {
         // Construct a datagram socket and bind it to any available
         // port on the local host machine. This socket will be used to
         // send and receive UDP Datagram packets.
         sendReceiveSocket = new DatagramSocket();
      } catch (SocketException se) {   // Can't create the socket.
         se.printStackTrace();
         System.exit(1);
      }
   }
   
   
   
   
   

   //////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   ////////////////////////////////////////////  Methods ////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////
	public static String packetInfo(DatagramPacket packet)
	{
		String opCode = "unknown";
		switch(packet.getData()[1]) {
		case (1): opCode = "RRQ"; break;
		case (2): opCode = "WRQ"; break;
		case (3): opCode = "DATA"; break;
		case (4): opCode = "ACK"; break;
		case (5): opCode = "ERROR"; break;
		}
		return opCode;
	}
	public static void analyzePacket(DatagramPacket packet) {
		System.out.println("--------------------------------------------");
		byte[] received = packet.getData();
		System.out.println("Packet Opcode: " + received[1]);
		System.out.println("Packet Type: " + packetInfo(packet));
		System.out.print("Packet Contents: ");
		for (int i = 0; i < packet.getLength(); i++)  {
			System.out.print(received[i] + " ");
		}
		System.out.println();
		System.out.println("--------------------------------------------");
	}
	
   public boolean checkValidOpcode(DatagramPacket e){
	   boolean error = false;
	   if(e.getData()[0] == 0){
		   switch(e.getData()[1]){
		   case (byte) 3: case (byte) 4: case (byte) 5:
			   return true;
		   default:
			   error = true;
			   
		   }
	   }
	   if(error){
		   if(verbose){
			System.out.println("Error, illegal TFTP operation!");
		   }
			error((byte) 4, e.getAddress(), e.getPort());
			return false;
	   }
	   return false;
   }
   
   public void error(byte ErrorCode, InetAddress Add, int port){
		try {
			byte[] connection = new byte[516];
			connection[0] = (byte) 0;
			connection[1] = (byte) 5;
			connection[2] = (byte) 0;
			connection[3] = (byte) ErrorCode;
			DatagramPacket ErrorMessage = new DatagramPacket(connection, 516, Add, port);
			if(verbose){
			System.out.println("Sending ERROR packet.");
			}
			sendReceiveSocket.send(ErrorMessage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
   
   
   public void updateBlockNum(byte[] data){
	   //first and second bytes of ACK + 1 and stored into data block#;
	   
	   if(Byte.toUnsignedInt(new Byte(data[3])) != 255){
		   int newSecond = Byte.toUnsignedInt(new Byte(data[3])) + 1;
		   data[3] = (byte) newSecond;

		   
	   }else if(Byte.toUnsignedInt(new Byte(data[2])) == 0 && Byte.toUnsignedInt(new Byte(data[3])) == 255){
		   int newFirst = Byte.toUnsignedInt(new Byte(data[2])) + 1;
		   data[2] = (byte) newFirst ; data[3] = 0;
	   }else{
		   data[2] = 0; data[3] = 1;
	   }

   }
   public boolean checkAddPort(DatagramPacket p , InetAddress expAdd, int expPort){
	   	if(p.getAddress() == expAdd && p.getPort() == expPort){
	   		return true;
	   	}else{
	   		try{
	   			if(verbose){
					System.out.println("Error, unknown transfer ID");
					System.out.println("Expected transfer ID: " + expPort);
					System.out.println("Received packet's transfer ID: " + p.getPort());
	   			}
			error((byte) 5, p.getAddress(), p.getPort());
	   		}catch(Exception e){
	   			e.printStackTrace();
	   		}
	   		return false;
	   	}
   }
   
   
   public boolean areEqual(DatagramPacket A, DatagramPacket B){
	   if((A.getLength() == B.getLength()) && (A.getData()[2] == B.getData()[2]) && (A.getData()[3] == B.getData()[3])){
		   return true;
	   }
	   return false;
   }
   
   
   
   /**
    * verifyACK checks if the ack passed in parameter is 
    * duplicated or not
    * delayed or not
    * if both are not, then the received ack is bigger than what we expected.
    */
   public boolean verify(DatagramPacket p, byte[] expected){
	   if(p.getData()[0] == expected[0] && p.getData()[1] == expected[1]){
	   if(p.getData()[2] == expected[2] && p.getData()[3] == expected[3]){
		   
		   // expected received, returning true.
		   return true; // same as expected.

	   }else if(p.getData()[2] < expected[2] ||( p.getData()[2] == expected[2] && p.getData()[3] < expected[3])){
		   
		   return false; // duplicated.
	   }
	   }

	   // the received ACK is bigger than the expected one.
	   System.out.println("Client: Sending ERROR packet (code 4).");
	   System.out.println("Packet is: ");
	   analyzePacket(p);
	   System.out.println("Expected was: " + expected[0] + expected[1] + expected[2] + expected[3]);
	   // sends error 4
	   error((byte)4, p.getAddress(), p.getPort());
	   return false;
   }
   
   
   public void writeData(DatagramPacket p){
	  
	   if(writeFile.canWrite()){
	   try {
		   out.write(p.getData(), 4, p.getLength()-4);
		} catch (IOException e) {
			//It's possible this may be able to catch multiple IO errors along with error 3, in
			//which case we might be able to just add a switch that identifies which error occurred
			System.out.println("IOException: " + e.getMessage());
			//Send an ERROR packet with error code 3 (disk full)
			error((byte)3, p.getAddress(), p.getPort());
		}
	   }else{
		   
			System.out.println("Client: Cannot write to file, file is ReadOnly.");
			error((byte)2, p.getAddress(), p.getPort());
			//System.exit(1);
	   }
   }
   
   
   public boolean checkTypeError(DatagramPacket p){
	   switch(p.getData()[3]){
	   case (byte) 1: case (byte) 2: case (byte) 3: case (byte) 6:
		   if(verbose){
		    System.out.println("Client: ERROR packet received (IO error)");
	   		System.out.println("Client: Terminating the connection.");
		   }
	   		return true;
	   case (byte) 4:
		   if(verbose){
		    System.out.println("Client: ERROR packet received (Code 4: Invalid packet)");
	   		System.out.println("Client: Terminating the connection.");
		   }
	   		return true;
	   case (byte) 5:
		   if(verbose){
		    System.out.println("Client: ERROR packet received (Code 5: Unknown Transfer ID)");
	   		System.out.println("Client: Terminating the connection.");
		   }
	   		return true;
		   
	   }
	   if(verbose){
	   System.out.println("Client: ERROR packet received (Code UNKNOWN)");
	   System.out.println("Client: ERROR packet ignored");
	   }
	   return false;
   }
   
   
   
   /**
    * Constructs a READ REQUEST packet to be sent to the server.
    */
   public DatagramPacket constructRRQ(byte[] data){
	   data[0] = 0; data[1] = 1;
	   System.arraycopy(filename.getBytes(), 0, data, 2, filename.length());
	   data[filename.length()+2] = 0;
	   System.arraycopy("octet".getBytes(), 0, data, filename.length()+3, "octet".length());
	   int len = filename.length() + "octet".length() + 4;
	   data[len-1] = 0;
	   DatagramPacket p = null;
	   try{
		   p = new DatagramPacket(data, len, InetAddress.getLocalHost(), sendPort);  //changed data.length to len
	   }catch(IOException e){
		   e.printStackTrace();
		   System.exit(1);
	   }
	   return p;
   }
   
   /**
    * Constructs a WRITE REQUEST packet to be sent to the server.
    */
   public DatagramPacket constructWRQ(byte[] data){
	   data[0] = 0; data[1] = 2;
	   System.arraycopy(filename.getBytes(), 0, data, 2, filename.length());
	   data[filename.length()+2] = 0;
	   System.arraycopy("octet".getBytes(), 0, data, filename.length()+3, "octet".length());
	   int len = filename.length() + "octet".length() + 4;
	   data[len-1] = 0;
	   DatagramPacket p = null; // a way to avoid error message ("variable might not be initialized")
	   try{
		   p = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), sendPort);
	   }catch(IOException e){
		   e.printStackTrace();
		   System.exit(1);
	   }
	   return p;
   }
   /**
    * Updates the data block# to the received ACK block# used for WRITE REQUESTS's data transfers.
    * Where the server send the ACK with block # 0, and we respond with a data of block# 1
    * Also, a check if we reached the maximum number of blocks which is, (2^16)-1, in two bytes (255, 255)
    */
   public void updateACK(byte first, byte second, byte[] data){
	   //first and second bytes of ACK + 1 and stored into data block#;
	   
	   if(Byte.toUnsignedInt(new Byte(second)) != 255){
		   int newSecond = Byte.toUnsignedInt(new Byte(second)) + 1;
		   data[3] = (byte) newSecond;
		   data[2] = 0;
		   
	   }else if(Byte.toUnsignedInt(new Byte(first)) == 0 && Byte.toUnsignedInt(new Byte(second)) == 255){
		   int newFirst = Byte.toUnsignedInt(new Byte(first)) + 1;
		   data[2] = (byte) newFirst ; data[3] = 0;
	   }else{
		   System.out.println("Transfer is out of block numbers boundry");
	   }

   }
   /**
    * Creates a folder named Directory and creates a text inside of it to be used then for writing data 
    * whenever we read from the server to it.
    * Also, never overwrite the file. If the file already there it uses it and dont create a new one.
    * Also, it initializes BufferedOutputStream
    */
   public boolean CreateOutStream(DatagramPacket p) throws FileNotFoundException, IOException
   {
		if (new File(path + "\\" + filename).exists()) {
			System.out.println("File Already Exists");
			error((byte) 6, p.getAddress(), p.getPort());
			return false;
		}
	   writeFile = new File(path + "\\" + filename);
	   out = new BufferedOutputStream(new FileOutputStream(writeFile)); // then uses the file to write in whatever we receive from server.
	   return true;
   }
   
   
   /**
    * Initializes BufferedInputStream to the given filename/path given by user.
    */
   public void CreateInStream() throws FileNotFoundException, IOException
   {
	   // to be able to read from filename/path 
	   in = new BufferedInputStream(new FileInputStream(path + "\\" + filename));

   }
   
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////// Read Request Handler ////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   /**
    * First it establishes the connection between the client and the host
    * Then it transfers the file from/to host.
    * returns true if the read file transfer is complete.
    * returns false if something weird happened.
    */
   public boolean readRequestHandler(){
	   byte[] data = new byte[516];
	   byte[] ack = {0,4,0,1};
	   boolean firstTime = true;
	   boolean finished = false;
	   byte[] expData = {0, 3, 0, 1};
	   boolean receivedIt;
	   DatagramPacket lastPacket = new DatagramPacket(new byte[516], 516);

	   // that fixed the problem.
	   	receivePacket = new DatagramPacket(data, data.length);
		// --------------- //

	   
   try {
	   while(!finished){
		   receivedIt = false;
		if(firstTime){
			for(int i = 1; i<=5; i++){
			   try{
				   sendReceiveSocket.send(constructRRQ(new byte[516]));
			   }catch(IOException e){
				   e.printStackTrace();
				   System.exit(1);
			   }
				   
				try{
					/* waiting for first Data */
					if(verbose){
					System.out.println("Waiting for Data for the "+ i + " time.");
					}
					sendReceiveSocket.setSoTimeout(1000);
					sendReceiveSocket.receive(receivePacket);
					// we received it in time, validate it.
					if(receivePacket.getData()[1] == 5){
						if(checkTypeError(receivePacket)){
							return false; // terminates the connection.
						}
					}else if(verify(receivePacket, expData)){
						
						lastPacket = receivePacket;
						receivedIt = true;
						System.arraycopy(receivePacket.getData(), 0, expData, 0, expData.length); // saving the data block number received.
						updateBlockNum(expData);
						System.arraycopy(receivePacket.getData(), 0, data, 0, receivePacket.getLength()); // ensures that the data received is in data[].
						break;
					}
					
					
				}catch(SocketTimeoutException e){
					if(verbose)
					System.out.println("Waiting for Data" + i + "  Timedout ( " + (5-i) +" ) tries left");
				}

				
			} // end waiting for ack
			if(!receivedIt){
				// we finished looping and we didnt receive a valid response therefore we give up.
				System.out.println("Client: Did not receive Ack Packet.. Terminating Connection..");
				return false;
			}
			firstTime = false;

			   try {
					if(!CreateOutStream(receivePacket)){
						return false;
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			// we write the data received
			
			writeData(receivePacket);
			if(verbose){
				analyzePacket(receivePacket);
			}
			/*                          */
			sendReceiveSocket.setSoTimeout(0);
		}else{
				while(true){
					try{
					
					if(verbose){
					System.out.println("Client: Sending ACK..");
					}
					sendReceiveSocket.send(new DatagramPacket(ack, ack.length, lastPacket.getAddress(),
													lastPacket.getPort()));
					}catch(IOException e){
						e.printStackTrace();
						System.exit(1);
					}
					if(verbose){
						analyzePacket(new DatagramPacket(ack, ack.length));
					}
					// after sending the ACK, we're expecting DATA.
					try{
						 sendReceiveSocket.receive(receivePacket);
							if(verbose){
								System.out.println("Client: Packet Received:");
								analyzePacket(receivePacket);
							}
						 if(receivePacket.getData()[1] == 5){
							if(checkTypeError(receivePacket)){
								out.close();
								return false; // terminates the connection.
							}
						 }else if(verify(receivePacket, expData) && checkAddPort(receivePacket, lastPacket.getAddress(), lastPacket.getPort())){
							System.arraycopy(receivePacket.getData(), 0, expData, 0, expData.length); // saving the data block number received.
							lastPacket = receivePacket;
							updateBlockNum(expData);
							
							System.arraycopy(receivePacket.getData(), 0, data, 0, receivePacket.getLength()); // ensures that the data received is in data[].
							break;
						}
					}catch(IOException e){
						e.printStackTrace();
						System.exit(1);
					}
				}
				// after receiving the new data, we should update the write array. with the new one.
				
				// we write the data received
				writeData(receivePacket);
				
				updateBlockNum(ack);
		}
		if(receivePacket.getLength() < 516){
			// prepare to stop writing. and send the last ACK.
			out.close();
			finished = true;
			try{
				if(verbose){
					analyzePacket(receivePacket);
				}
				System.arraycopy(data, 2, ack, 2, 2);
				sendReceiveSocket.send(new DatagramPacket(ack, ack.length, lastPacket.getAddress(),
												lastPacket.getPort()));
			}catch(IOException e){
				e.printStackTrace();
				System.exit(1);
			}
			return true;
		
		}
	}
	} catch (IOException e) {
		e.printStackTrace();
	}

	   return false; // Something weird happened, RRQ transfer connection failed.
   }
   

   //////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////// Write Request Handler////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   /**
    * First it establishes the connection between the client and the host
    * Then it transfers the file from/to host.
    * returns true if the write transfer file is complete.
    * returns false if something weird happened.
 * @throws IOException 
    */
   public boolean writeRequestHandler() throws IOException{
	   byte[] data = new byte[516];
	   byte[] ack = new byte[4];
	   data[0] = 0; data[1] = 3;
	   data[2] = 0; data[3] = 1;
	   byte[] expACK = {0,4,0,0};
	   boolean receivedIt = false;
	   DatagramPacket lastPacket = null;

	   // prepareing the receive packet to receive the acknowledge.
	   receivePacket = new DatagramPacket(ack, ack.length);

	   //-----------------sending a write Request-----------------// 
		for(int i = 1; i<=5; i++){
			   try{
				   sendReceiveSocket.send(constructWRQ(new byte[516]));
			   }catch(IOException e){
				   e.printStackTrace();
				   System.exit(1);
			   }
			   
			try{
				/* waiting for first ack */
				if(verbose){
				System.out.println("Waiting for ACK for the "+ i + " time.");
				}
				sendReceiveSocket.setSoTimeout(1000);
				sendReceiveSocket.receive(receivePacket);
				// we received it in time, validate it.
				if(receivePacket.getData()[1] == 5){
					if(checkTypeError(receivePacket)){
						return false;
					}
				}else if(verify(receivePacket, expACK)){
					lastPacket = receivePacket;
					receivedIt = true;
					updateBlockNum(expACK);
					break;
				}
				
				
			}catch(SocketTimeoutException e){
				if(verbose){
				System.out.println("Waiting for ACK " + i + " Timedout ( " + (5-i) +" ) tries left");
				}
			}

			
		} // end waiting for ack
		if(!receivedIt){
			// we finished looping and we didnt receive a valid response therefore we give up.
			System.out.println("Client: Did not receive Ack Packet.. Terminating Connection..");
			return false;
		}
	   //---------------------------------------------------------//
		if(verbose){
			//analyzePacket(receivePacket);
		   /* printing what we received */
			System.out.println("Client: ACK received:");
			System.out.println("To host: " + receivePacket.getAddress());
			System.out.println("Destination host port: " + receivePacket.getPort());
			int len = receivePacket.getLength();
			System.out.println("Length: " + len);
			System.out.println("Containing: ");
			for (int j=0;j<receivePacket.getLength();j++) {
				 System.out.print(receivePacket.getData()[j] + " ");
			}
			System.out.println();
		}
		/*                          */
	   
	   

	   try {
	    // preparing to read from file
		CreateInStream();
	} catch (FileNotFoundException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	}
	   try {
		boolean finished = false;
		byte[] read = new byte[512];
		
		int n;
		while(!finished){
			receivedIt = false;
			// condition to check if we aren't done yet from reading.
			// read() returns -1 if we have no more data to be read from the file.
			if((n=in.read(read)) != -1 ){
				// now read has the data, data has the first 4 bytes ready. therefore;
				System.arraycopy(read, 0, data, 4, read.length);
				// data is read to be sent.
				System.out.println("----------- N = " + n);
				// sending the data packet block#n//
				for(int i = 1; i<=10; i++){
					try{
						/* Printing what we've sent */
						System.out.println("Client: Sending Data Packet, for the "+ i + " time.");
						System.out.println("Containing: ");
						for (int j=0;j<4+n;j++) {
							 System.out.print(data[j] + " ");
						}
						System.out.println();
						sendReceiveSocket.send(new DatagramPacket(data, 4+n , lastPacket.getAddress(), lastPacket.getPort())); // 4(opcode and block#) + n (how many bytes we read from file)
					}catch(IOException e){
						e.printStackTrace();
						System.exit(1);
					}
					   
					try{
						/* waiting for first ack */
						System.out.println("Waiting for ACK for the "+ i + " time.");
						sendReceiveSocket.setSoTimeout(1000);
						sendReceiveSocket.receive(receivePacket);
						// we received it in time, validate it.
						if(receivePacket.getData()[1] == 5){
							if(checkTypeError(receivePacket)){
								in.close();
								return false;
							}
						}else if(verify(receivePacket, expACK) && checkAddPort(receivePacket, lastPacket.getAddress(), lastPacket.getPort())){
							receivedIt = true;
							// we need to update the data block # corresponding to the ACK block #;
							updateBlockNum(expACK);
							break;
						}
						}catch(SocketTimeoutException e){
						System.out.println("Waiting for ACK" + i + "  Timedout. ( " + (5-i) +" ) tries left");
						}

					
				} // end waiting for ack
				if(!receivedIt){
					// we finished looping and we didnt receive a valid response therefore we give up.
					System.out.println("Client: Did not receive Ack Packet, Terminating Connection..");
					in.close();
					return false;
				}
			   //---------------------------------------------------------//
				/* Printing what we've sent */
				if(verbose){
					System.out.println("Client: ACK Packet received");
					System.out.println("Containing: ");
					for (int j=0;j<4;j++) {
						 System.out.print(receivePacket.getData()[j] + " ");
					}
				}
				updateACK(receivePacket.getData()[2], receivePacket.getData()[3], data);
				
			}else{ 
				
				in.close();
				finished = true;
			}
	    }// end while
		
		return true; // we finished our WRQ transfer.
	} catch (IOException e) {
		e.printStackTrace();
	}

	   return false; // Something weird happened, WRQ transfer connection failed.

   }

   
   
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////     Main    ////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   
   
   public static void main(String args[]) throws IOException
   {
	   
	   String requestType;
	   Scanner input = new Scanner(System.in);
	   
   	System.out.println("Client: Enter a directory path where client read/write from/to: [type Q to shutdown]: ");
   	path = input.next();
   	if(path.equalsIgnoreCase("Q")){
   		System.out.println("Client: Shutting Down..");
   		System.exit(1);
   	}
       // loop for ever till user types Q to shutdown.
	   for(;;){
		   
	    for(;;){ // inner loop for inputs.
		   System.out.println("Client: Enter the type of request (R/W) [type Q to shutdown]: ");
			   requestType = input.next();
			   System.out.println("You entered:" + requestType + ".");
		   // checks if the user wants to shutdown.
		   if(requestType.equalsIgnoreCase("Q")){
			   System.out.println("Client: Shutting Down..");
		   		System.exit(1);
	       }
		   
		   // Adjusting the boolean values so we use that to determine whether its read or write requests.
		   if(requestType.equalsIgnoreCase("R")){
			   request = true;
			   break;
		   }else if(requestType.equalsIgnoreCase("W")){
			   request = false;
			   break;
		   }else{
			   System.out.println("Entered request type is invalid. try again. [type Q to shutdown]");
			   
		   }
	    }// inner loop for type inputs.
	    
	    
	    /*
		if (new File(path + "\\" + receivedFileName).exists()) {
			System.out.println("File Already Exists");
			try {
				error((byte) 6, address, port , new DatagramSocket());
			} catch (SocketException e) {
				e.printStackTrace();
			}
			return;
		}
		*/

	    
		   System.out.println("Client: Enter the file name (with extension) [type Q to shutdown]: ");
		   filename = input.next();
		   System.out.println("You entered:" + filename + ".");
		   // checks if the user wants to shutdown.
		   if(filename.equalsIgnoreCase("Q")){
			   System.out.println("Client: Shutting Down..");
		   		System.exit(1);
	       }
		   
		   for(;;){
		   	System.out.println("Client: Do you want test mode (y/n):");
		   	String md = input.next();
		    if (md.equalsIgnoreCase("n")) {
		    	System.out.println("Client: mode is Normal");
		        sendPort = 69;
		         break;
		    }else if(md.equalsIgnoreCase("y")){
		    	System.out.println("Client: mode is Test");
		    	sendPort = 23;
		    	break;
		   	}
		   }
		   /*
		   for(;;){
			   	System.out.println("Client: Do you want verbose mode (y/n):");
			   	String md = input.next();
			    if (md.equalsIgnoreCase("y")) {
			    	verbose = true;
			         break;
			    }else if(md.equalsIgnoreCase("n")){
			    	verbose = false;
			    	break;
			   	}
			}
		   */
			System.out.println("Client: Processing your request...");
		    Client c = new Client();
		    boolean check = false;
		    if(request){
		    	check = c.readRequestHandler();
		    	if(check)
		    	    System.out.println("File Read complete.");
		    }else{
		    	check = c.writeRequestHandler();
		    	if(check)
		    	System.out.println("File Write complete.");
		    }

		   if(verbose){
			   System.out.println("Client: Closing the socket..");
		   }
           // We're finished, so close the socket.
           sendReceiveSocket.close();
 
	}
	   
   }
}
