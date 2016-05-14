package iteration1;

import java.io.*;
import java.net.*;
import java.util.*;



public class ResponseHandler implements Runnable {

	private Server.Request request;
	private DatagramPacket receivePacket;
	private DatagramSocket sendSocket;
	private String filename, mode;
	private BufferedInputStream in;
	private BufferedOutputStream out;



	public ResponseHandler(Server.Request req, DatagramPacket receive,
												String name, String mod){
		request=req;
		receivePacket= receive;
		filename=name;
		mode=mod;
		try {
			if (receivePacket.getData()[0]==0 && receivePacket.getData()[1]==1){//RRQ case
				in = new BufferedInputStream(new FileInputStream(filename));
				System.out.println("constructor");
			}	
			if (receivePacket.getData()[0]==0 && receivePacket.getData()[1]==2)//WRQ case
				out= new BufferedOutputStream(new FileOutputStream("textServer.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		try{
			sendSocket = new DatagramSocket();	
		}catch(SocketException e){
			e.printStackTrace();
			System.exit(1);
		}
		
		
	}//constructor
	
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
	
	
	   //////////////////////////////////////////////////////////////////////////////////////////////////////
	   //////////////////////////////////////////////////////////////////////////////////////////////////////
	   //////////////////////////////////// Read Request Handler ////////////////////////////////////////////
	   //////////////////////////////////////////////////////////////////////////////////////////////////////
	   //////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public boolean readRequestHandler() throws IOException{
		byte[] data = new byte[516];
		byte[] ack = new byte[4];
		data[0] = 0; data[1] = 3;
		data[2] = 0; data[3] = 1;
		boolean finished = false;
		byte[] read = new byte[512];
		int n;
		// after receiving the request in receivePacket, and identify "RRQ" 
		// receivePacket going to act here only to receive "Ack's" therefore prepare it before loop.
		receivePacket.setData(ack);
		receivePacket.setLength(ack.length);
		
		while(!finished){
			
			if((n=in.read(read)) != -1 ){
				System.out.println("Value of n: " + n);

				/**
				 * SENDING & PRINTING DATA!
				 */
				try{
					
					System.arraycopy(read, 0, data, 4, read.length);
					//--------printing what we got---------//
					System.out.println("Server: Sending Data packet:");
					System.out.println("To host: " + receivePacket.getAddress());
					System.out.println("Destination host port: " + receivePacket.getPort());
					int len = n+4;
					System.out.println("Length: " + len);
					System.out.println("Containing: ");
					for (int j=0;j<len;j++) {
						 System.out.print(data[j] + " ");
					}
					//------------------------------------//
					sendSocket.send(new DatagramPacket(data, n+4, receivePacket.getAddress(), receivePacket.getPort()));
				}catch(IOException e){
					e.printStackTrace();
					System.exit(1);
				}
				
				/**
				 * Wait & Print ACK's 
				 */
				try{
					
					System.out.println("Server: Waiting ACK Packet..");
					 sendSocket.receive(receivePacket);
				}catch(IOException e){
					e.printStackTrace();
					System.exit(1);
				}
				//--------------------------//
				
				/**
				 * Checking and Printing Ack's
				 */
				// checking if its not Ack.
				if(ack[1] != 4 && ack[0] != 0){
					System.out.println("We received invalid ACK packet (OP-error!).");
					System.exit(1);
				}else{
					/* prints the ack received */
					System.out.println("Server: Acknowledge packet received.");
					System.out.println("To host: " + receivePacket.getAddress());
					System.out.println("Destination host port: " + receivePacket.getPort());
					int len = receivePacket.getLength();
					System.out.println("Length: " + len);
					System.out.println("Containing: ");
					for (int j=0;j<receivePacket.getLength();j++) {
						 System.out.print(receivePacket.getData()[j] + " ");
					}
				}
				System.out.println();
				//------------------------------//

				// we need to update the data block #
				updateBlockNum(data);

				
			}else{
				
				return true; // we finished.
			}
			
		}// end while
		
		return false; // something weird happened.
		
	}
	
	
	
	
	   //////////////////////////////////////////////////////////////////////////////////////////////////////
	   //////////////////////////////////////////////////////////////////////////////////////////////////////
	   //////////////////////////////////// Write Request Handler////////////////////////////////////////////
	   //////////////////////////////////////////////////////////////////////////////////////////////////////
	   //////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public boolean writeRequestHandler(){
		byte[] ack = {0,4,0,0};
		byte[] data = new byte[516];
		boolean finished = false;
		int len;
		while(!finished){
			/**
			 * Forming and Sending ACK# 0 --- #n
			 */
			try{
				// receivePacket going to send "Ack's"
				receivePacket.setData(ack);
				receivePacket.setLength(ack.length);
				sendSocket.send(receivePacket); 
			}catch(IOException e){
				e.printStackTrace();
				System.exit(1);
			}
			/*---------------------------------*/
			/*          Printing what we sent  */
			System.out.println("Server: Sending ACK packet:");
			System.out.println("To host: " + receivePacket.getAddress());
			System.out.println("Destination host port: " + receivePacket.getPort());
			len = receivePacket.getLength();
			System.out.println("Length: " + len);
			System.out.println("Containing: ");
			for (int j=0;j<len;j++) {
				 System.out.print(receivePacket.getData()[j] + " ");
			}
			//----------------------------------//
			// we should update the ack block number for next transfer.
			
			updateBlockNum(ack);
			
			/*---------------------------------*/
			
			/**
			 * Waiting for Data Packet.
			 */
			try{
				// receivePacket going to receive data.
				receivePacket.setData(data);
				receivePacket.setLength(data.length);
				sendSocket.receive(receivePacket);
			}catch(IOException e){
				e.printStackTrace();
				System.exit(1);
			}
			/*--------------------------------*/
			/*      Print what we received      */
			System.out.println("Server: Received Data packet:");
			System.out.println("To host: " + receivePacket.getAddress());
			System.out.println("Destination host port: " + receivePacket.getPort());
			len = receivePacket.getLength();
			System.out.println("Length: " + len);
			System.out.println("Containing: ");
			for (int j=0;j<len;j++) {
				 System.out.print(receivePacket.getData()[j] + " ");
			}
			/*----------------------------------*/
			// Writing it to file now.
			
			try {
				out.write(receivePacket.getData(), 4, receivePacket.getLength()-4);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			//-----------------------
			/**
			 * Check if we need to end this transfer connection
			 */
			if(receivePacket.getLength() < 516){
				finished = true;
				// and send the LAST ACK of the transfer.----//
				
				try{
					// receivePacket going to send "Ack's"
					receivePacket.setData(ack);
					receivePacket.setLength(ack.length);
					sendSocket.send(receivePacket); 
				}catch(IOException e){
					e.printStackTrace();
					System.exit(1);
				}
				/*---------------------------------*/
				/*          Printing what we sent  */
				System.out.println("Server: Sending ACK packet:");
				System.out.println("To host: " + receivePacket.getAddress());
				System.out.println("Destination host port: " + receivePacket.getPort());
				len = receivePacket.getLength();
				System.out.println("Length: " + len);
				System.out.println("Containing: ");
				for (int j=0;j<len;j++) {
					 System.out.print(receivePacket.getData()[j] + " ");
				}
				//------------Last ACK sent------------------//
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return true; // we finished.
			}
			
		}
		return false;
	}
	
	
	
	
	
	
	
	public void run(){
		// we check if the request is read, we inspect the data to check if its a read request opcode.
		if(request==Server.Request.READ && receivePacket.getData()[0]==0 && receivePacket.getData()[1]==1){
			try {
				if(readRequestHandler()){
					System.out.println("Server: Read Request Transfer file complete.");
				}else{
					System.out.println("Server: Read Request Transfer file incomplete.");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}else if(request == Server.Request.WRITE && receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 2){
			
			if(writeRequestHandler()){
				System.out.println("Server: Write Request Transfer file complete.");
			}else{
				System.out.println("Server: Write Request Transfer file incomplete.");
			}
		}
			
			
			
		
		

		// We're finished with this socket, so close it.
		sendSocket.close();		
	}//end of run
	
}

