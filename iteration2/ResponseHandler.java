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
	
	   //////////////////////////////////////////////////////////////////////////////////////////////////////
	   //////////////////////////////////////////////////////////////////////////////////////////////////////
	   ////////////////////////////////////////////  Methods ////////////////////////////////////////////////
	   //////////////////////////////////////////////////////////////////////////////////////////////////////
	   //////////////////////////////////////////////////////////////////////////////////////////////////////
	   /**
	    * duplicate checks if the data/ack passed in parameter is 
	    * duplicated or not
	    * delayed or not
	    * if both are not, then the received ack is bigger than what we expected.
	    */
	   public boolean verify(byte[] p, byte[] expected){
	   if(p[0] == expected[0] && p[1] == expected[1]){
		   if(p[2] == expected[2] && p[3] == expected[3]){
			   
			   // expected received, returning true.
			   return true; // same as expected.
	
		   }else if(p[2] < expected[2] ||( p[2] == expected[2] && p[3] < expected[3])){
			   
			   return false; // duplicated.
		   }
		   }else if(p[1] == 1 || p[1] == 2){
			   return false;
		   }
		   
		   // the received ACK is bigger than the expected one.
	   		System.out.println();
		   System.out.println("ACK received cannot be explained by delayed/duplicate errors.");
		   System.out.println("Server: Shutting Down...");
		   System.exit(1);
		   return false;
	   }
	
	
		
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
		byte[] expACK = {0, 4, 0, 1};
		data[0] = 0; data[1] = 3;
		data[2] = 0; data[3] = 1;
		boolean finished = false;
		byte[] read = new byte[512];
		int n;
		// after receiving the request in receivePacket, and identify "RRQ" 
		// receivePacket going to act here only to receive "Ack's" therefore prepare it before loop.
		receivePacket.setData(ack);
		receivePacket.setLength(ack.length);
		boolean receivedIt;
		while(!finished){
			receivedIt = false;
			if((n=in.read(read)) != -1 ){
				for(int i = 1; i<=5; i++){
					/**
					 * SENDING & PRINTING DATA!
					 */
					try{
						System.arraycopy(read, 0, data, 4, read.length);
						//--------printing what we got---------//
						System.out.println("Server: Sending Data packet ["+ i +"]");
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
						sendSocket.setSoTimeout(1000);
						System.out.println("Server: Waiting ACK Packet[" + i +"]");
						sendSocket.receive(receivePacket);
						// verify if we received the expected block number or not.
						if(verify(receivePacket.getData(), expACK)){
							receivedIt = true;
							// getting the expected block number to wait for the new ACK to be compared to.
							updateBlockNum(expACK);
							break;
						}
					}catch(SocketTimeoutException e){
						System.out.println("Waiting for ACK" + i + "  Timedout. ( " + (5-i) +" ) tries left");
					}
					//--------------------------//
				}
				// check if we break out the loop and received the intended packet or not.
				// if we dont receive any packet and our tries expired. Server will giveup and quit.
					if(!receivedIt){
						System.out.println("Server: Did not receive Ack Packet.. Server Shutting Down..");
						System.exit(1);
					}
				
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
					
					
					
				}else{
					return true;
				}
			updateBlockNum(data);
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
		// since we're expecting data block 01 for first data transfer, we might need to initialize oldData for first time
		byte[] expData = {0,3,0,1}; // it'll carry the block num of old data.
		
		
		boolean finished = false;
		int len;
		while(!finished){
			/**
			 * Forming and Sending ACK# 0 --- #n
			 */
			while(true){
			try{
				// receivePacket going to send "Ack's"
				receivePacket.setData(ack);
				receivePacket.setLength(ack.length);
				sendSocket.send(receivePacket); 
			}catch(IOException e){
				e.printStackTrace();
				System.exit(1);
			}
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
			System.out.println();
			//----------------------------------//
			
			
			/**
			 * Waiting for Data Packet.
			 */
			try{
				// receivePacket going to receive data.
				receivePacket.setData(data);
				receivePacket.setLength(data.length);
				sendSocket.receive(receivePacket);
				if(verify(receivePacket.getData(), expData)){
					break;
				}
			}catch(IOException e){
				e.printStackTrace();
				System.exit(1);
			}
		}
			
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
			System.out.println();
			/*----------------------------------*/
			
			if(receivePacket.getData()[1] == 3){
			// get the data block number and transfer it to ack
				ack[2] = receivePacket.getData()[2];
				ack[3] = receivePacket.getData()[3];
			//-------------------------------------------------
			
			
				try {
					out.write(receivePacket.getData(), 4, receivePacket.getLength()-4);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				// get the block number.
				System.arraycopy(receivePacket.getData(), 0, expData, 0, expData.length);
				updateBlockNum(expData);
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
					}// end try catch
					return true;
				}
			
				
			}
			
		}// end while.
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
