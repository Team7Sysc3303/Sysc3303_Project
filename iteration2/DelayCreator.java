import java.io.*;
import java.net.*;
import java.util.*;

//Class for delayed (in seconds) send of packet through Simulator normal Socket
public class DelayCreator implements Runnable{
	private DatagramSocket sendSocket;
	private double factor;
	private DatagramPacket sendPacket;
	
	public DelayCreator(double seconds, DatagramPacket sendPack, DatagramSocket send ) {
		factor=seconds;
		sendPacket=sendPack;
		sendSocket=send;
	}
	
	private void delaySim(double factor){
		int delay=(int)(1000*factor)/1;
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			e= new InterruptedException("Delay interrupted");
			e.printStackTrace();
		}
	}
	public void run(){
		delaySim(factor);
		
		System.out.println("Simulator: sending packet.");
	      System.out.println("To host: " + sendPacket.getAddress());
	      System.out.println("Destination host port: " + sendPacket.getPort());
	      int len = sendPacket.getLength();
	      System.out.println("Length: " + len);
	      System.out.println("Containing: ");
	      for (int j=0;j<len;j++) {
	    	  System.out.print(sendPacket.getData()[j] + " ");
	      }

	      // Send the datagram packet to the server via the send/receive socket.

	      try {
	         sendSocket.send(sendPacket);
	      } catch (IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	      }
	}
}
