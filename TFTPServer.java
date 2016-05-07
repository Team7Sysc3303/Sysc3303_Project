// TFTPServer.java 
// This class is the server side of a simple TFTP server based on
// UDP/IP. The server receives a read or write packet from a client and
// sends back the appropriate response without any actual file transfer.
// One socket (69) is used to receive (it stays open) and another for each response. 

import java.io.*;
import java.net.*;
import java.lang.Object.*;


public class TFTPServer implements Runnable {

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

   public TFTPServer()
   {
	   try {
		   // Construct a datagram socket and bind it to port 69
		   // on the local host machine. This socket will be used to
		   // receive UDP Datagram packets.
		   receiveSocket = new DatagramSocket(1069);
	   } catch (SocketException se) {
		   se.printStackTrace();
		   System.exit(1);
	   }
   }
      
   public void run(){
	   System.out.println("Server is operational");
	   receivePacket =  new DatagramPacket(data, data.length);
	   while(!StopTransfer)
		   this.listener();
   }
   public void stopTransfer(){
	   StopTransfer = true;
	   receiveSocket.close();
   }

   public static void main( String args[] ) throws Exception
   {
	   TFTPServer c = new TFTPServer();
	   c.run();
   }
}
