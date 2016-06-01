package Server;



import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.AccessControlException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Stack;


public class Server {

	/**
	 * The path where the server will Read/Write
	 */
	private String path;
	
	private HashSet<File> fileSet;

	/**
	 *  The name of the mode received from the request from the intermediate host
	 */
	private String receivedMode;

	/**
	 * The socket for the server which will be set to use port 69
	 */
	private DatagramSocket transferSocket;

	private Stack<Integer> activeThreads = new Stack<Integer>();

	/**
	 *  Shutdown flag to show if shutdown has been requested
	 */
	private boolean shutdown;

	
	private boolean verbose = true;

	public Server() {
		try {
			transferSocket = new DatagramSocket(69);
			fileSet = new HashSet<File>();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
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
	   
	   
	   
	   public boolean verifyBlock(byte[] p, byte[] expected){
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
	   
	   
	   public boolean areEqual(DatagramPacket A, DatagramPacket B){
		   if((A.getLength() == B.getLength()) && (A.getData()[2] == B.getData()[2]) && (A.getData()[3] == B.getData()[3])){
			   return true;
		   }
		   return false;
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
			   data[3] = 1; data[2] = 0;
		   }

	   }
	   
	   public void writeData(DatagramPacket p, BufferedOutputStream out , String filename, DatagramSocket t){
		   if(new File(path + "\\" + filename).canWrite()){
		   try {
			   out.write(p.getData(), 4, p.getLength()-4);
			} catch (IOException e) {
				//It's possible this may be able to catch multiple IO errors along with error 3, in
				//which case we might be able to just add a switch that identifies which error occurred
				System.out.println("IOException: " + e.getMessage());
				//Send an ERROR packet with error code 3 (disk full)
				error((byte)3, p.getAddress(), p.getPort(), t);
			}
		   }else{
			   
				System.out.println("Client: Cannot write to file, file is ReadOnly.");
				error((byte)2, p.getAddress(), p.getPort(), t);
				//System.exit(1);
		   }
	   }
	
	   public boolean verify(DatagramPacket p, byte[] expected , DatagramSocket t){
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
		   // sends error 4
		   error((byte)4, p.getAddress(), p.getPort(), t);
		   return false;
	   }
	   
	   
	   public boolean checkAddPort(DatagramPacket p , InetAddress expAdd, int expPort , DatagramSocket t){
		   	if(p.getAddress() == expAdd || p.getPort() == expPort){
		   		return true;
		   	}else{
		   		try{
		   			//if(verbose){
						System.out.println("Error, unknown transfer ID");
						System.out.println("Expected transfer ID: " + expPort);
						System.out.println("Received packet's transfer ID: " + p.getPort());
						System.out.println("Expected Address: " + expAdd);
						System.out.println("Received packet's Address: " + p.getAddress());
		   			//}
				error((byte) 5, p.getAddress(), p.getPort(), t);
		   		}catch(Exception e){
		   			e.printStackTrace();
		   		}
		   		return false;
		   	}
	   }
	   
	   
	   
	   
	/**
	 * This method will check to ensure that the given packet data is a valid TFTP packet. This method will
	 * also extract the filename and the mode of a read or write request and place them in the global
	 * variables of the server.
	 * @param b the data of the received packet in the form of a byte array
	 * @param block the block number specified by a DATA or ACK packet. Value negligible for WRQ or RRQ
	 * @param port the port number of the intended sender. Used to determine unknown transfer ID errors
	 * @return true if the packet is valid
	 */
	
	
	
	public boolean isValid(byte[] b, int block, int port) {
		//Initial checks to see if it is a valid packet
		if (b == null || b.length == 0) {
			System.out.println("No packet data!");
			return false;
		} else if (b[0] != 0) {
			System.out.println("Invalid opcode, does not start with 0!");
			return false;
		}
		System.out.println("Packet opcode: " + b[1]);
		switch (b[1]) {
		//If the packet is a WRQ or RRQ
		case (byte) 1: case (byte) 2:
			//Get the filename from the byte array
			StringBuilder builder = new StringBuilder();
		int index;
		String receivedFileName = null;
		for (index = 2; index < b.length; index++) {
			if(b[index] != 0) {
				builder.append((char) b[index]);
			} else {
				receivedFileName = builder.toString();
				break;
			}
		}
		//Get the mode from the byte array
		builder = new StringBuilder();
		for (int i = index+1; i < b.length; i++) {
			if(b[i] != 0) {
				builder.append((char) b[i]);
			} else {
				receivedMode = builder.toString();
				break;
			}
		}
		if (receivedMode != null && receivedFileName != null) {
			return true;
		} else {
			System.out.println("Null file name or mode!");
			return false;
		}
		//If the packet is a DATA or ACK
		case (byte) 3: case (byte) 4:
			return true;
		//If the packet is an ERROR
		case (byte) 5:
			System.out.println("ERROR packet acknowledged.");
		return true;
		default: 
			System.out.println("Invalid opcode!");
			return false;
		}
	}

	/**
	 * Handles write operations.
	 * @param receivedPacket data held in the write request
	 * @param port port number from which the write request came
	 */
	public boolean write(byte[] receivedPacket, int port, InetAddress address, String receivedFileName) {
		DatagramSocket transfer = null;
		BufferedOutputStream out;
		byte[] data = new byte[516];
		DatagramPacket receivedData = new DatagramPacket(data, data.length);
		byte[] ack = {0, 4, 0, 0};
		byte[] expData = {0, 3, 0 , 1};
		DatagramPacket lastPacket = null;
		
		File f = new File(path + "\\" + receivedFileName);
		if (fileSet.contains(f)) {
			System.out.println("File in use");
			error((byte)2, address, port, transferSocket);
			return false;
		} 
		fileSet.add(f);
		try {
			out = new BufferedOutputStream(new FileOutputStream(f));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			error((byte)1, address, port, transferSocket);
			return false;
		}
		
		// create socket
		try {
			transfer = new DatagramSocket();
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while(true){
			// send ack
			try {
				System.out.println("Server: Sending ACK:");
				analyzePacket(new DatagramPacket(ack, ack.length, address, port));
				transfer.send(new DatagramPacket(ack, ack.length, address, port));
				// receive data
				transfer.receive(receivedData);
				if(receivedData.getData()[1] == 5){
					if(checkTypeError(receivedData)){
						fileSet.remove(f);
						out.close();
						return false;
					}
				}else if(verify(receivedData, expData, transfer) && checkAddPort(receivedData, address, port, transfer)){
					writeData(receivedData, out, receivedFileName, transfer); // writes to file.
					updateBlockNum(ack);
					updateBlockNum(expData);
					System.out.println("Server: Data packet received:");
					analyzePacket(receivedData);
					if(receivedData.getLength() < 516){
						// sends last ack and halt.
						System.out.println("Server: Sending last Ack.");
						analyzePacket(new DatagramPacket(ack, ack.length, address, port));
						transfer.send(new DatagramPacket(ack, ack.length, address, port));
						out.close();
						fileSet.remove(f);
						return true;
					}
					
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}

	}

	/**
	 * Handles read operations
	 * @param receivedPacket data held in the read request
	 * @param portport number from which the read request came
	 */
	public boolean read(byte[] receivedPacket, int port, InetAddress address, String receivedFileName) {
		byte[] data = new byte[516];
		byte[] expACK = {0, 4, 0, 1};
		byte[] ack = new byte[4];
		data[0] = 0; data[1] = 3;
		data[2] = 0; data[3] = 1;
		DatagramSocket transfer= null;
		BufferedInputStream input = null;
		DatagramPacket lastPacket = new DatagramPacket(receivedPacket, receivedPacket.length, address, port);
		DatagramPacket receivedACK = new DatagramPacket(ack, ack.length);
		boolean receivedIt;
		
		int n;
		try {
			transfer = new DatagramSocket();
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		File f = new File(path + "\\" + receivedFileName);
		if (fileSet.contains(f)) {
			System.out.println("File in use");
			error((byte)2, address, port, transfer);
			return false;
		}
		try {
			input = new BufferedInputStream(new FileInputStream(f));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			error((byte)1, address, port, transfer);
			return false;
		}
		////// Starting to read ///////
		while(true){
			receivedIt = false;
			//send data
			try{
				if((n = input.read(data, 4, data.length-4))!= -1){
					DatagramPacket sendData = new DatagramPacket(data, n+4, address, port);
					for(int i = 1; i<6 ; i++){
						try{
						System.out.println("Server: Sending Data:");
						analyzePacket(sendData);
						transfer.send(sendData);
						transfer.setSoTimeout(1000);
						// waits for ack
						transfer.receive(receivedACK);
						transfer.setSoTimeout(0);
						if(receivedACK.getData()[1] == 5){
							if(checkTypeError(receivedACK)){
								input.close();
								fileSet.remove(f);
								return false; // terminates the connection.
							}
						}else if(verify(receivedACK, expACK, transfer) && checkAddPort(receivedACK, lastPacket.getAddress(), lastPacket.getPort(), transfer)){
							lastPacket = receivedACK;
							updateBlockNum(expACK);
							updateBlockNum(data);
							receivedIt = true;
							System.out.println("Server: received ACK:");
							analyzePacket(receivedACK);
							break;
						}
						
						}catch(SocketTimeoutException e){
							//if verbose
							System.out.println("Server: Waiting for Ack timedout for the " + i + " time.");
						}
					
					}
					if(!receivedIt){
						System.out.println("Server: Did not receive a Packet, Server is terminating the connection.");
						input.close();
						return false;
					}
					
				}else if(lastPacket.getLength() == 516){
					// send empty data packet.
					System.out.println("Server: Sending empty data packet.");
					analyzePacket(new DatagramPacket(data, 4));
					transfer.send(new DatagramPacket(data, 4, address, port));
					input.close();
					fileSet.remove(f);
					return true;
				}else{
					fileSet.remove(f);
					input.close();
					return true;
				}

			}catch(IOException e){
				//
			}
			
		}
		
	}

	/**
	 * Sends an ERROR packet.
	 * @param ErrorCode TFTP error code of the ERROR packet
	 * @param port port to send the packet through
	 */
	public void error(byte ErrorCode, InetAddress address, int port, DatagramSocket transferSocket){
		//DatagramSocket transferSocket;	//Socket to be sent through
		try {
			//transferSocket = new DatagramSocket();
			byte[] connection = new byte[516];
			connection[0] = (byte) 0;
			connection[1] = (byte) 5;
			connection[2] = (byte) 0;
			connection[3] = (byte) ErrorCode;
			DatagramPacket ErrorMessage = new DatagramPacket(connection, 516, address, port);
			System.out.println("Sending ERROR(From Server) packet.");
			transferSocket.send(ErrorMessage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Receives a packet from an intended sender specified by port. If the received packet is invalid (ie it is not
	 * from the intended sender, or it has an invalid opcode), the program will respond by sending an appropriate
	 * ERROR packet. It then continues waiting for a packet.
	 * 
	 * @param transferSocket socket to receive from
	 * @param receivePacket packet received
	 * @param port port number of the intended sender
	 * @return the received DatagramPacket
	 * @throws IOException
	 */
	private DatagramPacket receivePacket(DatagramSocket transferSocket, DatagramPacket receivePacket, int port, int timeout) throws SocketTimeoutException
	{
		boolean error = true;	//Indicates whether or not an invalid packet has been received and the server
		//must continue waiting
		try {
			while(error) {
				error = false;
				transferSocket.setSoTimeout(timeout);
				transferSocket.receive(receivePacket);
				analyzePacket(receivePacket);
				//Ensure received packet is a valid TFTP operation
				if (!isValid(receivePacket.getData(), receivePacket.getData()[3], port)) {
					System.out.println("Error, illegal TFTP operation!");
					error((byte) 4, receivePacket.getAddress() , receivePacket.getPort(), transferSocket);
					error = true;
				}
				//Ensure received packet came from the intended sender
				if (receivePacket.getPort() != port) {
					System.out.println("Error, unknown transfer ID");
					error((byte) 5, receivePacket.getAddress() , receivePacket.getPort(), transferSocket);
					error = true;
				}
			}
		} catch (IOException e) {
			
		}
		return receivePacket;
	}
	
	public String extractName(byte[] b) {
		StringBuilder builder = new StringBuilder();
		for (int index = 2; index < b.length; index++) {
			if(b[index] != 0) {
				builder.append((char) b[index]);
			} else {
				break;
			}
		}
		return builder.toString();
	}

	/**
	 * This method is used to run the Server
	 */
	public void runServer()
	{
		byte[] b = new byte[100];
		DatagramPacket receival = new DatagramPacket(b, b.length);

		/**
		 * Thread created to launch prompt for server shutdown.
		 * Thread is required as server could become blocked
		 * with the transferSocket.receive(receival) call as it will
		 * wait to receive rather than quit. The thread ensures
		 * that as long as no requests come in or no requests
		 * are currently running, it will override and quit.
		 */
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) { // run till shutdown requested
					if(!shutdown) {
						System.out.print("Enter quit to shutdown the Server: ");
						Scanner in = new Scanner(System.in);
						String input = in.nextLine().toLowerCase();
						if (input.equals("quit")) {
							in.close();
							shutdown = true;
						}

					} else if (shutdown && activeThreads.isEmpty()) { // wait till all active threads finish & shutdown requested
						System.out.println("Server has shut down");
						System.exit(0);
					}
				}
			}
		}).start();
		try {
			while (true) {
				System.out.println("---------------------------------");
				System.out.println("Waiting to Receive from Host");

				//Server receives a read or write request
				transferSocket.receive(receival);
				analyzePacket(receival);
				System.out.println("Received a packet.");
				int port = receival.getPort();
				InetAddress address = receival.getAddress();
				String receivedFileName = extractName(b);
				if (isValid(b, (byte) 0, port)) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							System.out.println("New thread created.");
							activeThreads.push(0);
							if (b[1] == 1) {
								System.out.println("Read request recieved");
								if (!new File(path + "\\" + receivedFileName).exists()) {
									System.out.println("File Does Not Exist");
									try {
										error((byte) 1, address, port , new DatagramSocket());
									} catch (SocketException e) {
										e.printStackTrace();
									}
									return;
								}
								if(read(b, port, address, receivedFileName)){
									System.out.println("A thread has completed execution.");
								}else{
									System.out.println("A thread has not completed execution.");
								}
							} else if (b[1] == 2) {
								System.out.println("Write request recieved");
								if (new File(path + "\\" + receivedFileName).exists()) {
									System.out.println("File Already Exists");
									try {
										error((byte) 6, address, port , new DatagramSocket());
									} catch (SocketException e) {
										e.printStackTrace();
									}
									return;
								}
								if(write(b, port, address, receivedFileName)){
									System.out.println("A thread has completed execution.");
								}else{
									System.out.println("A thread has not completed execution.");
								}
							} else {
								System.out.println("ERR");
							}
							activeThreads.pop();
						}
					}).start();
				} else {
					System.out.println("Not valid");
					//do nothing;
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main( String args[] ) {
		Server s = new Server();
		try {
			System.out.println("Server's InetAddress: " + InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		System.out.println("Please provide path for where the Server will Read/Write: ");
		Scanner scanner = new Scanner(System.in);
		s.path = scanner.nextLine();
		System.out.println(s.path);
		try {
			s.runServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
		scanner.close();
	}
}
