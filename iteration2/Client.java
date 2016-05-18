package iteration1;
// TFTPClient.java
// This class is the client side for a very simple assignment based on TFTP on
// UDP/IP. The client uses one port and sends a read or write request and gets 
// the appropriate response from the server.  No actual file transfer takes place.   
import java.io.*;
import java.net.*;
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
   
   public void updateBlockNum(byte[] data){
	   //first and second bytes of ACK + 1 and stored into data block#;
	   
	   if(Byte.toUnsignedInt(new Byte(data[3])) != 255){
		   int newSecond = Byte.toUnsignedInt(new Byte(data[3])) + 1;
		   data[3] = (byte) newSecond;
		   data[2] = 0;
		   
	   }else if(Byte.toUnsignedInt(new Byte(data[2])) == 0 && Byte.toUnsignedInt(new Byte(data[3])) == 255){
		   int newFirst = Byte.toUnsignedInt(new Byte(data[2])) + 1;
		   data[2] = (byte) newFirst ; data[3] = 0;
	   }else{
		   System.out.println("Transfer is out of block numbers boundry");
	   }

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
	   System.out.println("ACK received cannot be explained by delayed/duplicate errors.");
	   System.out.println("Client: Shutting Down...");
	   System.exit(1);
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
   public void CreateOutStream() throws FileNotFoundException, IOException
   {

	   writeFile = new File("Directory\\test2.txt");
	   if(!writeFile.exists()){ // checks if the file doesn't exist.
	   if (writeFile.getParentFile().mkdir()) { // creates the Directory.
	       writeFile.createNewFile(); // creates the file.
	   } else {
	       throw new IOException("Failed to create directory " + writeFile.getParent()); // failed to create directory.
	   } 
	   }
	   
	   out = new BufferedOutputStream(new FileOutputStream(writeFile)); // then uses the file to write in whatever we receive from server.
	   
   }
   
   
   /**
    * Initializes BufferedInputStream to the given filename/path given by user.
    */
   public void CreateInStream() throws FileNotFoundException, IOException
   {
	   // to be able to read from filename/path 
	   in = new BufferedInputStream(new FileInputStream(filename));

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
	   byte[] ack = {0,4,0,0};
	   boolean firstTime = true;
	   boolean finished = false;
	   byte[] expData = {0, 3, 0, 1};
	   boolean receivedIt;
			   
	   try {
		CreateOutStream();
	} catch (FileNotFoundException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	}
	   
	   
	   
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
					System.out.println("Waiting for Data for the "+ i + " time.");
					sendReceiveSocket.setSoTimeout(1000);
					sendReceiveSocket.receive(receivePacket);
					// we received it in time, validate it.
					if(verify(receivePacket, expData)){
						receivedIt = true;
						System.arraycopy(receivePacket.getData(), 0, expData, 0, expData.length); // saving the data block number received.
						updateBlockNum(expData);
						System.arraycopy(receivePacket.getData(), 0, data, 0, receivePacket.getLength()); // ensures that the data received is in data[].
						break;
					}
					
					
				}catch(SocketTimeoutException e){
					System.out.println("Waiting for Data" + i + "  Timedout ( " + (5-i) +" ) tries left");
				}

				
			} // end waiting for ack
			if(!receivedIt){
				// we finished looping and we didnt receive a valid response therefore we give up.
				System.out.println("Client: Did not receive Ack Packet.. Client Shutting Down..");
				System.exit(1);
			}
			firstTime = false;
			// we write the data received
			out.write(receivePacket.getData(), 4, receivePacket.getLength()-4);
		   /* printing what we received */
			System.out.println("Client: Data received:");
			System.out.println("To host: " + receivePacket.getAddress());
			System.out.println("Destination host port: " + receivePacket.getPort());
			int len = receivePacket.getLength();
			System.out.println("Length: " + len);
			System.out.println("Containing: ");
			for (int j=0;j<receivePacket.getLength();j++) {
				 System.out.print(receivePacket.getData()[j] + " ");
			}
			System.out.println();
			/*                          */
			
		}else{
				try{
				System.arraycopy(data, 2, ack, 2, 2);
				sendReceiveSocket.send(new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(),
												sendPort));
				}catch(IOException e){
					e.printStackTrace();
					System.exit(1);
				}
				System.out.println("Client: ACK sent:");
				System.out.println("Client: Containing:");
				for (int j=0;j<receivePacket.getLength();j++) {
				   System.out.print(receivePacket.getData()[j] + " ");
				}
				System.out.println();
				// after sending the ACK, we're expecting DATA.
				try{
					
					 sendReceiveSocket.receive(receivePacket);
				}catch(IOException e){
					e.printStackTrace();
					System.exit(1);
				}
				// after receiving the new data, we should update the write array. with the new one.
			   /* printing what we received */
				System.out.println("Client: Data received:");
				System.out.println("To host: " + receivePacket.getAddress());
				System.out.println("Destination host port: " + receivePacket.getPort());
				int len = receivePacket.getLength();
				System.out.println("Length: " + len);
				System.out.println("Containing: ");
				for (int j=0;j<receivePacket.getLength();j++) {
					 System.out.print(receivePacket.getData()[j] + " ");
				}
				System.out.println();
				/*                          */
		}
		if(receivePacket.getLength() < 516){
			// prepare to stop writing. and send the last ACK.
			out.close();
			finished = true;
			try{
				System.arraycopy(data, 2, ack, 2, 2);
				sendReceiveSocket.send(new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(),
												sendPort));
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
	   byte[] ack = {0,4,0,0};
	   data[0] = 0; data[1] = 3;
	   byte[] expACK = ack;
	   boolean receivedIt = false;

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
				System.out.println("Waiting for ACK for the "+ i + " time.");
				sendReceiveSocket.setSoTimeout(1000);
				sendReceiveSocket.receive(receivePacket);
				// we received it in time, validate it.
				if(verify(receivePacket, expACK)){
					receivedIt = true;
					expACK = receivePacket.getData(); // saving the ack received.
					updateBlockNum( expACK);
					break;
				}
				
				
			}catch(SocketTimeoutException e){
				System.out.println("Waiting for ACK" + i + "  Timedout ( " + (5-i) +" ) tries left");
			}

			
		} // end waiting for ack
		if(!receivedIt){
			// we finished looping and we didnt receive a valid response therefore we give up.
			System.out.println("Client: Did not receive Ack Packet.. Client Shutting Down..");
			System.exit(1);
		}
	   //---------------------------------------------------------//
	   
	   
	   
	   
	   

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

				// we need to update the data block # corresponding to the ACK block #;
				updateACK(receivePacket.getData()[2], receivePacket.getData()[3], data);
				// now read has the data, data has the first 4 bytes ready. therefore;
				System.arraycopy(read, 0, data, 4, read.length);
				// data is read to be sent.
				
				// sending the data packet block#n//
				for(int i = 1; i<=5; i++){
					try{
						/* Printing what we've sent */
						System.out.println("Client: Sending Data Packet, for the "+ i + " time.");
						System.out.println("Containing: ");
						for (int j=0;j<4+n;j++) {
							 System.out.print(data[j] + " ");
						}
						System.out.println();
						sendReceiveSocket.send(new DatagramPacket(data, 4+n , InetAddress.getLocalHost(), sendPort)); // 4(opcode and block#) + n (how many bytes we read from file)
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
						if(verify(receivePacket, expACK)){
							receivedIt = true;
							expACK = receivePacket.getData(); // saving the ack received.
							updateBlockNum(expACK);
							break;
						}
						
						
						}catch(SocketTimeoutException e){
						System.out.println("Waiting for ACK" + i + "  Timedout. ( " + (5-i) +" ) tries left");
						}

					
				} // end waiting for ack
				if(!receivedIt){
					// we finished looping and we didnt receive a valid response therefore we give up.
					System.out.println("Client: Did not receive Ack Packet, Client Shutting Down..");
					System.exit(1);
				}
			   //---------------------------------------------------------//

				
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
       // loop for ever till user types Q to shutdown.
	   for(;;){
		   
	    for(;;){ // inner loop for inputs.
		   System.out.println("Client: Enter the type of request (Read/ Write) [Press Q to shutdown]: ");
			   requestType = input.next();
			   System.out.println("You entered:" + requestType + ".");
		   // checks if the user wants to shutdown.
		   if(requestType.equalsIgnoreCase("Q")){
			   System.out.println("Client: Shutting Down..");
		   		System.exit(1);
	       }
		   
		   // Adjusting the boolean values so we use that to determine whether its read or write requests.
		   if(requestType.equalsIgnoreCase("Read")){
			   request = true;
			   break;
		   }else if(requestType.equalsIgnoreCase("Write")){
			   request = false;
			   break;
		   }else{
			   System.out.println("Entered request type is invalid. try again. [Press Q to shutdown]");
			   
		   }
	    }// inner loop for type inputs.
		   System.out.println("Client: Enter the file directory and file name (with extension) [Press Q to shutdown]: ");
		   filename = input.next();
		   System.out.println("You entered:" + filename + ".");
		   // checks if the user wants to shutdown.
		   if(filename.equalsIgnoreCase("Q")){
			   System.out.println("Client: Shutting Down..");
		   		System.exit(1);
	       }
		   
	    
		   System.out.println("Client: Processing your request...");
		    Mode run = Mode.TEST; // change to NORMAL to send directly to server  // change it to TEST.
		      
		    if (run==Mode.NORMAL) {
		        sendPort = 69;
		         System.out.println("Client: mode is Normal");
		    }else{
		    	System.out.println("Client: mode is Test");
		    	sendPort = 23;
		   	}
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

		    
           // We're finished, so close the socket.
           sendReceiveSocket.close();
 
	}
	   
   }
}
