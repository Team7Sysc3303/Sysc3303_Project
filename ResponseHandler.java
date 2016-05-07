import java.io.*;
import java.net.*;
import java.util.*;


public class ResponseHandler extends TFTPServer implements Runnable {

	private Request request;
	private DatagramPacket receivePacket, sendPacket;
	private DatagramSocket sendSocket;
	
	public ResponseHandler(Request req, DatagramPacket receive){
		request=req;
		receivePacket= receive;
	}//constructor
	
	public void run(){
		byte[] response =new byte[4];
		//Choosing right response, error case not implemented
		if (request==Request.READ) { // for Read it's 0301
            response = readResp;
        } else if (request==Request.WRITE) { // for Write it's 0400
            response = writeResp;
        }
		sendPacket = new DatagramPacket(response, response.length,
                receivePacket.getAddress(), receivePacket.getPort());

		System.out.println("Server: Sending packet:");
		System.out.println("To host: " + sendPacket.getAddress());
		System.out.println("Destination host port: " + sendPacket.getPort());
		int len = sendPacket.getLength();
		System.out.println("Length: " + len);
		System.out.println("Containing: ");
		for (int j=0;j<len;j++) {
			System.out.println("byte " + j + " " + response[j]);
		}

		// Send the datagram packet to the client via a new socket.

		try {
			// Construct a new datagram socket
			sendSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}

		try {
			sendSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("Server: packet sent using port " + sendSocket.getLocalPort());
		System.out.println();

		// We're finished with this socket, so close it.
		sendSocket.close();		
	}//end of run
}
