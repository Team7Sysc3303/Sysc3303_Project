package Server;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

import Client.Client;

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

	public Server() {
		try {
			transferSocket = new DatagramSocket(69);
			fileSet = new HashSet<File>();
		} catch (SocketException e) {
			e.printStackTrace();
		}
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
			   data[3] = 1; data[2] = 0;
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
	public void write(byte[] receivedPacket, int port, InetAddress address, String receivedFileName) {
		DatagramSocket transferSocket;	//Socket through which the transfer is done
		DatagramPacket lastPacket = new DatagramPacket(new byte[516], 516);
		byte block;	//The current block of data being transferred
		byte[] expData = {0, 3, 0, 1};
		//Create data for an ACK packet
		byte[] connection = new byte[4];
		connection[0] = (byte) 0;
		connection[1] = (byte) 4;
		connection[2] = (byte) 0;
		connection[3] = (byte) 0;
		block = (byte) 1;
		try {

			transferSocket = new DatagramSocket();


			File f = new File(path + "\\" + receivedFileName);
			if (fileSet.contains(f)) {
				System.out.println("File in use");
				error((byte)2, port, transferSocket, address);
				return;
			} 
			fileSet.add(f);
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
			while(true) {
				//Send ACK packet
				DatagramPacket establishPacket = new DatagramPacket(connection, connection.length, 
						address, port);
				System.out.println("Sending ACK");
				Client.analyzePacket(establishPacket);
				transferSocket.send(establishPacket);
				
				//Receive
				byte[] receiveFile = new byte[516];
				establishPacket = new DatagramPacket(receiveFile, receiveFile.length);
				transferSocket.receive(establishPacket);
				Client.analyzePacket(establishPacket);
				System.out.println("Received a packet.");
				if (!isValid(establishPacket.getData(), block, port)) {
					System.out.println("Packet recieved from host has an invalid Opcode, reporting back to Host");
					error((byte)4, port, transferSocket, address);
				}
				else {
					if (lastPacket.getData()[3] == establishPacket.getData()[3]) {
						System.out.println("Discarding Data pack as it is a duplicate");
					} else if(verifyBlock(establishPacket.getData(), expData)) {
						updateBlockNum(expData);
						try {
							out.write(receiveFile, 4, establishPacket.getLength() - 4);
						} 
						catch (AccessControlException e) {
							System.out.println("Server does not have permission to access file.");
							error((byte)2, port, transferSocket, address);
							fileSet.remove(f);
							return;
						} catch (IOException e) {
							//It's possible this may be able to catch multiple IO errors along with error 3, in
							//which case we might be able to just add a switch that identifies which error occurred
							System.out.println("IOException: " + e.getMessage());
							//Send an ERROR packet with error code 3 (disk full)
							error((byte)3, port, transferSocket, address);
							fileSet.remove(f);
							return;
						}
						lastPacket = establishPacket;
						//A packet of less than max size indicates the end of the transfer
						if (establishPacket.getLength() < 516) break;
						block++;
						updateBlockNum(connection);
					}

				}
			}
			//Send ACK packet
			updateBlockNum(connection);
			DatagramPacket establishPacket = new DatagramPacket(connection, connection.length, 
					address, port);
			System.out.println("Sending ACK");
			Client.analyzePacket(establishPacket);
			transferSocket.send(establishPacket);
			fileSet.remove(f);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handles read operations
	 * @param receivedPacket data held in the read request
	 * @param portport number from which the read request came
	 */
	public void read(byte[] receivedPacket, int port, InetAddress address, String receivedFileName) {
		DatagramSocket transferSocket;	//Socket through which the transfer is done
		byte block;	//The current block of data being transferred
		byte[] receive = new byte[4];	//Buffer for incoming packets
		byte[] expAck = {0, 4, 0, 1};
		byte[] connection = new byte[516];	//Buffer for outgoing packets
		connection[0] = 0; connection[1] = 3;
		connection[2] = 0; connection[3] = 1;
		DatagramPacket received = new DatagramPacket(receive, receive.length);	//Holds incoming packets
		try { 
			transferSocket = new DatagramSocket();
			byte[] sendingData = new byte[512];
			File f = new File(path + "\\" + receivedFileName);
			if (fileSet.contains(f)) {
				System.out.println("File in use");
				error((byte)2, port, transferSocket, address);
				return;
			}
			BufferedInputStream input = new BufferedInputStream(new FileInputStream(f));
			int x;	//End of stream indicator
			while ((x = input.read(sendingData)) != -1) {
				System.arraycopy(sendingData, 0, connection, 4, sendingData.length);
				DatagramPacket fileTransfer = new DatagramPacket(connection, x + 4, address, port);
				do {
					System.out.println("Sending DATA packet.");
					Client.analyzePacket(fileTransfer);
					
					while(true){
						transferSocket.send(fileTransfer);
						try {
							System.out.println("Attempting to Receive Ack");
							received = receivePacket(transferSocket, received, port, 2000);
							if(received.getData() != null && verifyBlock(received.getData(), expAck)){
								updateBlockNum(expAck);
								break;
							}
						} catch (SocketTimeoutException e) {
							System.out.println("Ack Receive Timed out, Resending Data");
							transferSocket.send(fileTransfer);
						}
					}
					//Check for any IO errors
					if (received.getData()[1] == (byte) 5 &&
						(received.getData()[3] == (byte) 1 || received.getData()[3] == (byte) 2 ||
							received.getData()[3] == (byte) 3 || received.getData()[3] == (byte) 6))
					{
						System.out.println("IO error detected. Ending file transfer.");
					}
				} while (received.getData()[1] == (byte) 5);	//Re-send if an ERROR is received
				updateBlockNum(connection);
			}
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends an ERROR packet.
	 * @param ErrorCode TFTP error code of the ERROR packet
	 * @param port port to send the packet through
	 */
	public void error(byte ErrorCode, int port, DatagramSocket transferSocket, InetAddress address){
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
				Client.analyzePacket(receivePacket);
				//Ensure received packet is a valid TFTP operation
				if (!isValid(receivePacket.getData(), receivePacket.getData()[3], port)) {
					System.out.println("Error, illegal TFTP operation!");
					error((byte) 4, receivePacket.getPort(), transferSocket, receivePacket.getAddress());
					error = true;
				}
				//Ensure received packet came from the intended sender
				if (receivePacket.getPort() != port) {
					System.out.println("Error, unknown transfer ID");
					error((byte) 5, receivePacket.getPort(), transferSocket, receivePacket.getAddress());
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
						//						close = JOptionPane.showConfirmDialog(null, modeLabel, "Warning", JOptionPane.CLOSED_OPTION);
						//						if (close == 0) { // ok has been selected, set shutdown to true
						//							shutdown = true;
						//						}
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
				Client.analyzePacket(receival);
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
										error((byte) 1, port, new DatagramSocket(), address);
									} catch (SocketException e) {
										e.printStackTrace();
									}
									return;
								}
								read(b, port, address, receivedFileName);
							} else if (b[1] == 2) {
								System.out.println("Write request recieved");
								if (new File(path + "\\" + receivedFileName).exists()) {
									System.out.println("File Already Exists");
									try {
										error((byte) 6, port, new DatagramSocket(), address);
									} catch (SocketException e) {
										e.printStackTrace();
									}
									return;
								}
								write(b, port, address, receivedFileName);
							} else {
								System.out.println("ERR");
							}
							activeThreads.pop();
							System.out.println("A thread has completed execution.");
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
