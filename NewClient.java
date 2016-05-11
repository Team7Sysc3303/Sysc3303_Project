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
   
   
   public DatagramPacket constructWRQ(byte[] data){
	   data[0] = 0; data[1] = 2;
	   System.arraycopy(filename.getBytes(), 0, data, 2, filename.length());
	   data[filename.length()+2] = 0;
	   System.arraycopy("octet".getBytes(), 0, data, filename.length()+3, "octet".length());
	   int len = filename.length() + "octet".length() + 4;
	   data[len-1] = 0;
	   DatagramPacket p = null;
	   try{
		   p = new DatagramPacket(data, len, InetAddress.getLocalHost(), sendPort);
	   }catch(IOException e){
		   e.printStackTrace();
		   System.exit(1);
	   }
	   return p;
   }
   
   public void updateACK(byte first, byte second, byte[] data){
	   //first and second bytes of ACK + 1 and stored into data block#;
	   
	   if(Byte.toUnsignedInt(new Byte(second)) != 255){
		   int newSecond = Byte.toUnsignedInt(new Byte(second)) + 1;
		   data[3] = (byte) newSecond;
		   
	   }else if(Byte.toUnsignedInt(new Byte(first)) == 0 && Byte.toUnsignedInt(new Byte(second)) == 255){
		   int newFirst = Byte.toUnsignedInt(new Byte(first)) + 1;
		   data[2] = (byte) newFirst ; data[3] = 0;
	   }else{
		   System.out.println("Transfer is out of block numbers boundry");
	   }

   }
   public void CreateOutStream() throws FileNotFoundException, IOException
   {

	  /* writeFile = new File("Directory\\test2.txt");
	   if(!writeFile.exists()){
	   if (writeFile.getParentFile().mkdir()) {
	       writeFile.createNewFile();
	   } else {
	       throw new IOException("Failed to create directory " + writeFile.getParent());
	   } 
	   }*/
	   
	   out = new BufferedOutputStream(new FileOutputStream("test2.txt"));
	   
   }
   
   
   
   public void CreateInStream() throws FileNotFoundException, IOException
   {
	   
	   in = new BufferedInputStream(new FileInputStream(filename));

   }
   
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////// Read Request Handler ////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////
   
   public boolean readRequestHandler(){
	   byte[] data = new byte[516];
	   byte[] ack = {0,4,0,0};
	   
	   // sending a read Request // 
	   try{
		   sendReceiveSocket.send(constructRRQ(data));
	   }catch(IOException e){
		   e.printStackTrace();
		   System.exit(1);
	   }
	   // Waiting for Data //
	   receivePacket = new DatagramPacket(data, data.length); 
	   try{
		   sendReceiveSocket.receive(receivePacket);
	   }catch(IOException e){
		   e.printStackTrace();
		   System.exit(1);
	   }
	   // checking if its Data.
	   if(data[1] != 3 && data[0] != 0){
		   System.out.println("We received invalid Data packet (OP-error!).");
		   System.exit(1);
	   }
	   // writing what we got into file given in filename //
	   data = receivePacket.getData(); // ensures that the data received is in data[].

	   try {
		CreateOutStream();
	} catch (FileNotFoundException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	}
	   try {
		boolean finished = false;
		byte[] write = new byte[512];
		System.arraycopy(data, 4, write, 0, data.length-4);
		while(!finished){
			   // do transfer. and also send and receive till we finish.
			out.write(write);
			
			// check if we have to loop again. or we reached our end of transfer.
			if(receivePacket.getLength() < 512){//TODO 516?
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
			}else{ // else we are still have to do more transfers.
				
				try{
					System.arraycopy(data, 2, ack, 2, 2);
					sendReceiveSocket.send(new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(),
					   								sendPort));
				}catch(IOException e){
					e.printStackTrace();
					System.exit(1);
				}
				// after sending the ACK, we're expecting DATA.
				try{
					
					 sendReceiveSocket.receive(receivePacket);
				}catch(IOException e){
					e.printStackTrace();
					System.exit(1);
				}
				// after receiving the new data, we should update the write array. with the new one.
				System.arraycopy(receivePacket.getData(), 4, write, 0, receivePacket.getLength()-4);

			}

	    }// end while
		return true; // we finished our RRQ transfer.
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
   
   public boolean writeRequestHandler(){
	   byte[] data = new byte[516];
	   byte[] ack = new byte[4];//{0,4,0,0};
	   data[0] = 0; data[1] = 3;
	   // sending a write Request // 
	   try{
		   sendReceiveSocket.send(constructWRQ(data));
	   }catch(IOException e){
		   e.printStackTrace();
		   System.exit(1);
	   }
	   receivePacket = new DatagramPacket(ack, ack.length);
	   try {
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

			// condition to check if we aren't done yet from reading.
			if((n = in.read(read,0,read.length))!= -1 ){
				
				// we're expecting a ACK of block# 0.
				try{
					 sendReceiveSocket.receive(receivePacket);
				}catch(IOException e){
					e.printStackTrace();
					System.exit(1);
				}
				// checking if its not Ack.
				if(ack[1] != 4 && ack[0] != 0){
					System.out.println("We received invalid ACK packet (OP-error!).");
					System.exit(1);
				}
				// we need to update the data block # corresponding to the ACK block #;
				updateACK(receivePacket.getData()[2], receivePacket.getData()[3], data);
				// now read has the data, data has the first 4 bytes ready. therefore;
				System.arraycopy(read, 0, data, 4, read.length);
				// data is read to be sent.
				// sending the data packet block# 0//
				try{
					sendReceiveSocket.send(new DatagramPacket(data, data.length, InetAddress.getLocalHost(), receivePacket.getPort()));//new (was sendPort)
				}catch(IOException e){
					e.printStackTrace();
					System.exit(1);
				}
				
			}else{ 
				// prepare to stop reading. and wait the last ACK.
				in.close();
				finished = true;
				try{
					sendReceiveSocket.receive(receivePacket);
				}catch(IOException e){
					e.printStackTrace();
					System.exit(1);
				}
				// checking if its not ACK.
				if(ack[1] != 4 && ack[0] != 0){
					System.out.println("We received invalid ACK packet (OP-error!).");
					System.exit(1);
				}
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
   
   
   public static void main(String args[])
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
		    if(request){
		    	c.readRequestHandler();
		    }else{
		    	c.writeRequestHandler();
		    }
            
           
           
           // We're finished, so close the socket.
           sendReceiveSocket.close();
 
	}
	   
   }
}
