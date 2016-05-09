// Server.java 
// This class is the server side of a simple TFTP server based on
// UDP/IP. The server receives a read or write packet from a client and
// sends back the appropriate response without any actual file transfer.
// One socket (69) is used to receive (it stays open) and another for each response. 

import java.io.*;
import java.net.*;
//import java.lang.Object.*;
import java.lang.*;
import java.util.*;


public class Server implements Runnable {

   // types of requests we can receive
   public static enum Request { READ, WRITE, ERROR};
   // responses for valid requests
   public static final byte[] readResp = {0, 3, 0, 1};
   public static final byte[] writeResp = {0, 4, 0, 0};
   //public static final byte[] errorResp 
   
   // UDP datagram packets and sockets used to send / receive
   private DatagramPacket sendPacket, receivePacket;
   private DatagramSocket receiveSocket, sendSocket;
   private byte data[];
   private volatile boolean StopTransfer = false;

   public Server()
   {
	   try {
		   // Construct a datagram socket and bind it to port 69
		   // on the local host machine. This socket will be used to
		   // receive UDP Datagram packets.
		   receiveSocket = new DatagramSocket(69);
	   } catch (SocketException se) {
		   se.printStackTrace();
		   System.exit(1);
	   }
   }
   
 public void listener()
   
   {
	   byte [] data;
	   int i, s=0, counter=0;
	   String filename, mode;
	   Request req; // READ, WRITE or ERROR
	   
	   for(;;) { // loop forever
	         // Construct a DatagramPacket for receiving packets up
	         // to 516 bytes long (the length of the byte array).
	         
	         data = new byte[516];
	         receivePacket = new DatagramPacket(data, data.length);
	         
	         System.out.println("Server: Waiting for packet.");
	         // Block until a datagram packet is received from receiveSocket.
	         try {
	            receiveSocket.receive(receivePacket);
	         } catch (IOException e) {
	            e.printStackTrace();
	            System.exit(1);
	         }
	         
	         // Process the received datagram.
	         System.out.println("Server: Packet received:");
	         System.out.println("From host: " + receivePacket.getAddress());
	         System.out.println("Host port: " + receivePacket.getPort());
	         int len = receivePacket.getLength();
	         System.out.println("Length: " + len);
	         System.out.println("Containing: " );
	         
	         // print the bytes
	         for ( i=0;i<len;i++) {
	            System.out.println("byte " + i + " " + data[i]);
	         }

	         // Form a String from the byte array.
	         String received = new String(data,0,len);
	         System.out.println(received);
	         
	      // If it's a read, send back DATA (03) block 1
	         // If it's a write, send back ACK (04) block 0
	         // Otherwise, ignore it
	         if (data[0]!=0) req = Request.ERROR; // bad
	         else if (data[1]==1) req = Request.READ; // could be read
	         else if (data[1]==4) req = Request.READ; // could be acknoledgment
	         else if (data[1]==2) req = Request.WRITE; // could be write
	         else req = Request.ERROR; // bad

	         if (req!=Request.ERROR) { // check for filename
	             // search for next all 0 byte
	             for(i=2;i<len;i++) {
	                 if (data[i] == 0) break;
	            }
	            if (i==len) req=Request.ERROR; // didn't find a 0 byte
	            if (i==2) req=Request.ERROR; // filename is 0 bytes long
	            // otherwise, extract filename
	            filename = new String(data,2,i-2);
	         }
	 
	         if(req!=Request.ERROR) { // check for mode
	             // search for next all 0 byte
	             for(s=i+1;s<len;s++) { 
	                 if (data[s] == 0) break;
	            }
	            if (s==len) req=Request.ERROR; // didn't find a 0 byte
	            if (s==i+1) req=Request.ERROR; // mode is 0 bytes long
	            mode = new String(data,i,s-i-1);
	         }
	         
	         if(s!=len-1) req=Request.ERROR; // other stuff at end of packet   
	         	
	         Thread responseThread = new Thread (new ResponseHandler(req, receivePacket), "ThreadSend" + counter);
	         counter ++;
	         responseThread.start();
	   
	   }
   }
   public void run(){
	   System.out.println("Server is operational");
	   
	   //receivePacket =  new DatagramPacket(data, data.length);
	  // while(!StopTransfer)
		   this.listener();
   }
   public void stopTransfer(){
	   StopTransfer = true;
	   receiveSocket.close();
   }

   public static void main( String args[] ) throws Exception
   {
	   Server c = new Server();
	   Thread ServerThread = new Thread ( c, "Server");
	   ServerThread.start();
   }
}
