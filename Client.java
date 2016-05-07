 // TFTPClient.java
// This class is the client side for a very simple assignment based on TFTP on
// UDP/IP. The client uses one port and sends a read or write request and gets 
// the appropriate response from the server.  No actual file transfer takes place.   

import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

   private DatagramPacket sendPacket, receivePacket;
   private DatagramSocket sendReceiveSocket;
   private static boolean request;
   private static String filename;
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

   public void sendAndReceive()
   {
      byte[] msg = new byte[100], // message we send
             fn, // filename as an array of bytes
             md, // mode as an array of bytes
             data; // reply as array of bytes
      String mode; // filename and mode as Strings
      int j, len, sendPort;
      
      // In the assignment, students are told to send to 23, so just:
      // sendPort = 23; 
      // is needed.
      // However, in the project, the following will be useful, except
      // that test vs. normal will be entered by the user.
      Mode run = Mode.TEST; // change to NORMAL to send directly to server  // change it to TEST.
      
      	if (run==Mode.NORMAL) {
         sendPort = 69;
         System.out.println("Client: mode is Normal");
      	}else{
    	 System.out.println("Client: mode is Test");
         sendPort = 23;
   		}
      // sends 10 packets -- 5 reads, 5 writes, 1 invalid


         System.out.println("Client: creating packet.");
         
         // Prepare a DatagramPacket and send it via sendReceiveSocket
         // to sendPort on the destination host (also on this machine).

         // if i even (2,4,6,8,10), it's a read; otherwise a write
         // (1,3,5,7,9) opcode for read is 01, and for write 02
         // And #11 is invalid (opcode 07 here -- could be anything)

        msg[0] = 0;
        if(request) // checking for read
           msg[1]=1;
        else // if not, its write.
           msg[1]=2;


        // convert to bytes
        fn = filename.getBytes();
        
        // and copy into the msg
        System.arraycopy(fn,0,msg,2,fn.length);
        // format is: source array, source index, dest array,
        // dest index, # array elements to copy
        // i.e. copy fn from 0 to fn.length to msg, starting at
        // index 2
        
        // now add a 0 byte
        msg[fn.length+2] = 0;

        // now add "octet" (or "netascii")
        mode = "octet";
        // convert to bytes
        md = mode.getBytes();
        
        // and copy into the msg
        System.arraycopy(md,0,msg,fn.length+3,md.length);
        
        len = fn.length+md.length+4; // length of the message
        // length of filename + length of mode + opcode (2) + two 0s (2)
        // second 0 to be added next:

        // end with another 0 byte 
        msg[len-1] = 0;

        // Construct a datagram packet that is to be sent to a specified port
        // on a specified host.
        // The arguments are:
        //  msg - the message contained in the packet (the byte array)
        //  the length we care about - k+1
        //  InetAddress.getLocalHost() - the Internet address of the
        //     destination host.
        //     In this example, we want the destination to be the same as
        //     the source (i.e., we want to run the client and server on the
        //     same computer). InetAddress.getLocalHost() returns the Internet
        //     address of the local host.
        //  69 - the destination port number on the destination host.
        try {
           sendPacket = new DatagramPacket(msg, len,
                               InetAddress.getLocalHost(), sendPort);
        } catch (UnknownHostException e) {
           e.printStackTrace();
           System.exit(1);
        }

        System.out.println("Client: sending packet.");
        System.out.println("To host: " + sendPacket.getAddress());
        System.out.println("Destination host port: " + sendPacket.getPort());
        len = sendPacket.getLength();
        System.out.println("Length: " + len);
        System.out.println("Containing: ");
        for (j=0;j<len;j++) {
            System.out.println("byte " + j + " " + msg[j]);
        }
        
        // Form a String from the byte array, and print the string.
        String sending = new String(msg,0,len);
        System.out.println(sending);

        // Send the datagram packet to the server via the send/receive socket.

        try {
           sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
           e.printStackTrace();
           System.exit(1);
        }

        System.out.println("Client: Packet sent.");

        // Construct a DatagramPacket for receiving packets up
        // to 100 bytes long (the length of the byte array).

        data = new byte[100];
        receivePacket = new DatagramPacket(data, data.length);

        System.out.println("Client: Waiting for packet.");
        try {
           // Block until a datagram is received via sendReceiveSocket.
           sendReceiveSocket.receive(receivePacket);
        } catch(IOException e) {
           e.printStackTrace();
           System.exit(1);
        }

        // Process the received datagram.
        System.out.println("Client: Packet received:");
        System.out.println("From host: " + receivePacket.getAddress());
        System.out.println("Host port: " + receivePacket.getPort());
        len = receivePacket.getLength();
        System.out.println("Length: " + len);
        System.out.println("Containing: ");
        for (j=0;j<len;j++) {
            System.out.println("byte " + j + " " + data[j]);
        }
        
        System.out.println();

      

      // We're finished, so close the socket.
      sendReceiveSocket.close();
   }

   public static void main(String args[])
   {
	   String requestType;
	   Scanner in = new Scanner(System.in);
       // loop for ever till user types Q to shutdown.
	   for(;;){
		   
		   
		   System.out.println("Client: Enter the type of request (Read/ Write) [Press Q to shutdown]: ");
			   requestType = in.next();
			   System.out.println("You entered:" + requestType + ".");
		   // checks if the user wants to shutdown.
		   if(requestType.equalsIgnoreCase("Q")){
			   System.out.println("Client: Shutting Down..");
		   		System.exit(1);
	       }
		   // Adjusting the boolean values so we use that to determine whether its read or write requests.
		   if(requestType.equalsIgnoreCase("Read")){
			   request = true;
		   }else{
			   request = false;
		   }
		   
		   System.out.println("Client: Enter File name, with extension (e.g .txt) [Press Q to shutdown]: ");
		   filename = in.next();
		   // checks if the user wants to shutdown.
		   if(filename.equalsIgnoreCase("Q")){
			   System.out.println("Client: Shutting Down..");
		   		System.exit(1);
	       }
		   
		   System.out.println("Client: Processing your request...");
           Client c = new Client();
           c.sendAndReceive();
           
	   }
   }
}
