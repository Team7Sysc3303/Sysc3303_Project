package Client;
import java.awt.HeadlessException;
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

import javax.swing.JOptionPane;

import ErrorSimulator.ErrorSimulator;

public class Client {

	/**
	 * The path that the client will read/write from
	 */
	private String path;

	/**
	 * Predefined listener port for the server
	 */
	private static final int SERVERPORT = 69;

	/**
	 * Predefined listener port for the error simulator
	 */
	private static final int SIMULATORPORT = 23;

	/**
	 * Type of mode the user has entered ("Read" or "Write")
	 */
	private String mode;

	/**
	 *  Name of the file the user has entered
	 */
	private String file;

	/**
	 *  The DatagramSocket that the client will use to send and receive
	 */
	private DatagramSocket sendReceive;

	/**
	 * True if the client is in test mode (sending to the error simulator instead of the server).
	 * **MUST BE MANUALLY SET TO ENABLE/DISABLE TEST MODE**
	 */
	private boolean testMode = true;

	/**
	 * Port the client will send to (varies depending on operation mode).
	 */
	private int sendPort = testMode ? SIMULATORPORT : SERVERPORT;

	public Client() {
		try {
			sendReceive = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	
	   public boolean verifyBlock(DatagramPacket p, byte[] expected){
		   if(p.getData()[0] == expected[0] && p.getData()[1] == expected[1]){
		   if(p.getData()[2] == expected[2] && p.getData()[3] == expected[3]){
			   
			   // expected received, returning true.
			   return true; // same as expected.

		   }else if(p.getData()[2] < expected[2] ||( p.getData()[2] == expected[2] && p.getData()[3] < expected[3])){
			   
			   return false; // duplicated.
		   }
		   }
		   
		   // the received ACK is bigger than the expected one.
		   System.out.println("Packet received cannot be explained by delayed/duplicate errors.");
		   return false;
	   }
	
	
	
	
	/**
	 * This method will print the given byte array's contents both as bytes and as a string
	 *
	 * @param array the byte array that needs to be printed
	 * @param length the length of the printed byte array
	 */
	public static void printByteArray(byte[] array, int length) {
		for (int j = 0; j < length; j++) {
			System.out.println("Byte " + j + ": " + array[j]);
		}
		System.out.println(new String(array, 0, length));
	}
	
	public static void analyzePacket(DatagramPacket packet) {
		System.out.println("--------------------------------------------");
		byte[] received = packet.getData();
		System.out.println("Packet Opcode: " + received[1]);
		System.out.println("Packet Type: " + ErrorSimulator.packetInfo(packet));
		System.out.print("Packet Contents: ");
		for (int i = 0; i < packet.getLength(); i++)  {
			System.out.print(received[i] + " ");
		}
		System.out.println();
		System.out.println("--------------------------------------------");
	}

	/**
	 * This method creates the request that the server sends.
	 *
	 * @param requestType
	 *            The type of request being set (Write, Read, Invalid). Default
	 *            is Read
	 * @param fileName
	 *            the name of the file
	 * @param mode
	 *            the mode being used
	 * @return the byte array that of the request
	 */
	public byte[] createRequest(String requestType, String fileName, String mode) {
		byte[] b = new byte[fileName.length() + mode.length() + 4];
		if (requestType.equals("Invalid")) {
			String invalid = "Invalid Request";
			return invalid.getBytes();
		}
		b[0] = (byte) 0;
		b[1] = (byte) 1;
		if (requestType.equals("Write"))
			b[1] = (byte) 2;
		System.arraycopy(fileName.getBytes(), 0, b, 2, fileName.length());
		b[fileName.length() + 2] = 0;
		System.arraycopy(mode.getBytes(), 0, b, fileName.length() + 3, mode.length());
		b[fileName.length() + mode.length() + 3] = 0;
		return b;
	}

	/**
	 * Launches a dialog box for user input
	 * Options:
	 * 	- accepts "read" or "write" as text (not case sensitive).
	 *  - once valid "read" or "write" is detected will prompt
	 *    for file name.
	 *  - once valid filename is given, it will allow runClient() to
	 *    continue
	 *  - to close, simply press cancel button
	 */
	public void launchUserInterface() {
		boolean validCommand = false;
		boolean validFileName = false;
		while (!validCommand) {
			try {
				mode = JOptionPane.showInputDialog(null, "Enter Command (\"Read\" or \"Write\")", "Enter Mode", JOptionPane.INFORMATION_MESSAGE).toLowerCase();
				if (mode == null) {
					System.exit(0);
				} else if (mode.equals("read") || mode.equals("write")) {
					validCommand = true; // break while loop and continue to next check
				}
			} catch (NullPointerException e) { // If null, cancel has been selected so close program
				System.out.println("Closing client");
				System.exit(0);
			}
		}
		while (!validFileName) {
			try {
				file = JOptionPane.showInputDialog(null, "Enter File Name (including file extension ie. *.txt)", "Enter File Name", JOptionPane.INFORMATION_MESSAGE);
				validFileName = true;
			} catch (NullPointerException e) {
				System.out.println("Closing client");
				sendReceive.close();
				System.exit(0);
			}

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
	private DatagramPacket receivePacket(DatagramSocket receiveSocket, DatagramPacket receivePacket, int port, int timeout) throws SocketTimeoutException
	{
		boolean error = true;	//Indicates whether or not an invalid packet has been received and the server
		//must continue waiting
		try {
			while(error) {
				error = false;
				receiveSocket.setSoTimeout(timeout);
				receiveSocket.receive(receivePacket);
				analyzePacket(receivePacket);
				System.out.println("Received a " + ErrorSimulator.packetInfo(receivePacket) + " packet");
				//Ensure received packet is a valid TFTP operation
				if (!isValid(receivePacket.getData(), receivePacket.getData()[3], receivePacket.getPort())) {
					System.out.println("Error, illegal TFTP operation!");
					error((byte) 4, receivePacket.getPort());
					error = true;
				}
				//Ensure received packet came from the intended sender
				if (receivePacket.getPort() != port) {
					System.out.println("Error, unknown transfer ID");
					System.out.println("Expected transfer ID: " + port);
					System.out.println("Received packet's transfer ID: " + receivePacket.getPort());
					error((byte) 5, receivePacket.getPort());
					error = true;
				}
			}
		} catch (IOException e) {
			throw new SocketTimeoutException();
		}
		return receivePacket;
	}
	
	// regular updateblocknum used to update the expected block number either for write/read requests expected packets.
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
			   data[2] = 0; data[3] = 1;
		   }

	   }
	
	

	/**
	 * This method will check to ensure that the given packet data is a valid TFTP packet.
	 * @param b the data of the received packet in the form of a byte array
	 * @param block the block number specified by a DATA or ACK packet. Value negligible for WRQ or RRQ
	 * @param port the port number of the intended sender. Used to determine unknown transfer ID errors
	 * @return true if the request is valid
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
				switch (b[1]) {
				//If the packet is a DATA or ACK
				case 3: case 4:
					return true;
					//If the packet is an ERROR
				case 5:
					System.out.println("ERROR packet acknowledged.");
					return true;
				default:
					System.out.println("Invalid opcode!");
					return false;
				}
	}

	/**
	 * Sends an ERROR packet.
	 * @param ErrorCode TFTP error code of the ERROR packet
	 * @param port port to send the packet through
	 */
	public void error(byte ErrorCode, int port){
		try {
			byte[] connection = new byte[516];
			connection[0] = (byte) 0;
			connection[1] = (byte) 5;
			connection[2] = (byte) 0;
			connection[3] = (byte) ErrorCode;
			DatagramPacket ErrorMessage = new DatagramPacket(connection, 516, InetAddress.getLocalHost(), port);
			System.out.println("Sending ERROR packet.");
			sendReceive.send(ErrorMessage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to run the Client
	 * - Calls UI to prompt user for mode type and file name
	 * - Checks what user requested
	 * - Constructs packet and either calls read or write
	 * - Sends to ErrorSimulator, which is forwarded to server
	 * - Waits for ACK
	 * - Re-prompts user until cancel button is clicked
	 */
	public void runClient() {
		DatagramPacket fileTransfer=null;
		System.out.println("--------------------------------------");
		while(true) {
			launchUserInterface();
			byte[] request;
			if (mode.equals("read")) { // check request type
				System.out.println("Read Request");
				if (new File(path + "\\" + file).exists()) {
					System.out.println("File Already Exists"); 
					continue;
				}
				request = createRequest("Read", file, "mode");
			} else if (mode.equals("write")) {
				System.out.println("Write Request");
				if (!new File(path + "\\" + file).exists()) {
					System.out.println("File Does Not Exist");
					continue;
				}
				request = createRequest("Write", file, "mode");
			} else {
				continue;
			}

			//Send read or write request
			DatagramPacket p;
			// Wait for response
			byte[] receive = new byte[516];
			DatagramPacket received = new DatagramPacket(receive, receive.length);
			DatagramPacket lastPacket = received;
			try {
				p = new DatagramPacket(request, request.length, InetAddress.getLocalHost(), sendPort);
				System.out.println("Sending Request");
				analyzePacket(p);
				while(true){
					try{
						sendReceive.send(p);
						sendReceive.setSoTimeout(1000);
						System.out.println("Waiting for a response...");
						sendReceive.receive(received);
					}catch(SocketTimeoutException e){
						System.out.println("Resending Request.");
					}
					if((received.getData()[1] == 4 && received.getData()[3] == 0)
							|| (received.getData()[1] == 3 && received.getData()[3] == 1)
							|| (received.getData()[1] == 5)){
						sendReceive.setSoTimeout(0);
						break;
					}
				}
				analyzePacket(received);
				
				if (receive[1] == 5 && receive[3] == 1) {
					System.out.println("File wasn't found");
					//continue; This is what broke everything in the demo btw DB
				} else if (receive[1] == 5 && receive[3] == 6) {
					System.out.println("File already exists");
					//continue;
				}
				System.out.println("Received a response packet.");
				byte rw = (receive[1] == (byte) 4) ? (byte) 0 : (byte) 1;
				int block =0;
				int x;
				if (isValid(received.getData(), rw, sendPort)) {
		switch (receive[1])
					{
					//Write operation----------------------------------------------------------------------------------------
		case (byte) 4:
			
						System.out.println("Forming DATA packet to send.");
					BufferedInputStream input = new BufferedInputStream(new FileInputStream(path + "\\" + file));
					byte[] sendingData = new byte[512];
					block = (byte) 1; //The current block of data being transferred
					byte[] sendingMessage = new byte[516];
					byte[] rack = new byte[4];
					byte[] expAck = {0, 4 , 0 , 1};
					sendingMessage[0] = (byte) 0;
					sendingMessage[1] = (byte) 3;
					sendingMessage[2] = (byte) 0;
					sendingMessage[3] = (byte) 1;
					while ((x = input.read(sendingData)) != -1) {								
						

						System.arraycopy(sendingData, 0, sendingMessage, 4, sendingData.length);
						

						do {
							DatagramPacket sendPacket = fileTransfer;
							while(true){
							sendingMessage[1] = 3;
							fileTransfer = new DatagramPacket(sendingMessage, x + 4, InetAddress.getLocalHost(), received.getPort());
							System.out.println("Sending following Data to Server ");
							analyzePacket(fileTransfer);
							sendReceive.send(fileTransfer);
							try {
								fileTransfer = new DatagramPacket(rack, 4);
								fileTransfer = receivePacket(sendReceive, fileTransfer, received.getPort(),1000);
								if(fileTransfer.getData() != null && verifyBlock(fileTransfer, expAck)){
										updateBlockNum(sendingMessage);
										lastPacket = fileTransfer;
										updateBlockNum(expAck);
										break;
								}else if(fileTransfer.getData()[1] == 5){
									break;
								}
							} catch (SocketTimeoutException error) {
								System.out.println("Socket Timeout Resending");
								sendReceive.send(sendPacket);
							}
							}
							//Check for any IO errors
							if (fileTransfer.getData()[1] == (byte) 5 &&
									(fileTransfer.getData()[3] == (byte) 1 || fileTransfer.getData()[3] == (byte) 2 ||
									fileTransfer.getData()[3] == (byte) 3 || fileTransfer.getData()[3] == (byte) 6))
							{
								System.out.println("IO error detected. Ending file transfer.");
								return;
							}
							
						} while (fileTransfer.getData()[1] == (byte) 5);	//Re-send if an ERROR is received
						//Send an ERROR if a received packet is invalid
						if (!isValid(fileTransfer.getData(), block, fileTransfer.getPort())) {
							error((byte) 4, received.getPort());
						}
						block++;
						sendingMessage[1] = 3;
					}
					input.close();
					break;
					//Read operation
					
		case (byte) 3:
					byte[] receiveFile= new byte[516];
					fileTransfer=null;
					
					System.out.println("Forming ACK packet to send.");
					byte[] ack = {0, 4 , 0 , 1};
					byte[] expData = {0, 3, 0, 2};
					block = 1; //The current block of data being transferred
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(path + "\\"+ file));
					out.write(receive, 4, received.getLength() - 4);
					DatagramPacket acknowledge =null;

					x = received.getLength();
					while (x == 516) {
						
						while(true){
							
							acknowledge = new DatagramPacket(ack, 4, InetAddress.getLocalHost(), received.getPort());
							System.out.println("Sending ACK packet to server");
							analyzePacket(acknowledge);
							sendReceive.send(acknowledge); // sends the ack.
							receiveFile = new byte[516];
							fileTransfer = new DatagramPacket(receiveFile, receiveFile.length);
							System.out.println("Waiting for DATA from server...");
							fileTransfer = receivePacket(sendReceive, fileTransfer, received.getPort(), 0);
							System.out.println("Received packet from server!");
							// checks for duplicate
							if (fileTransfer.getData()[3] == lastPacket.getData()[3] &&
									fileTransfer.getData()[2] == lastPacket.getData()[2]) { //If same block (Written already)
								System.out.println("Discarding Packet since it has already been written");
							}else if(verifyBlock(fileTransfer, expData)){
								updateBlockNum(expData);
								updateBlockNum(ack);
								break;
							}else if(fileTransfer.getData()[1] == 5){
								break;
							}
						}
							
							if(fileTransfer.getData()[1] == 5){
								break;
							}
							try {
								out.write(receiveFile, 4, fileTransfer.getLength() - 4);

							} catch (AccessControlException e) {
								System.out.println("Server does not have permission to access file.");
								error((byte)2, acknowledge.getPort());
							} catch (IOException e) {
								//It's possible this may be able to catch multiple IO errors along with error 3, in
								//which case we might be able to just add a switch that identifies which error occurred
								System.out.println("IOException: " + e.getMessage());
								//Send an ERROR packet with error code 3 (disk full)
								error((byte)3, acknowledge.getPort());
							}
							lastPacket = fileTransfer;
						x = fileTransfer.getLength();
					}
					System.out.println("Sending ACK packet to server");
					analyzePacket(acknowledge);
					sendReceive.send(acknowledge); // sends the ack.
					out.close();
					break;			
		case (byte) 5:
			
					System.out.println("Error in transmission is acknowlaged");
					break;
					}
				}
				else {
					System.out.println("Request response not valid!");
					error((byte)4, SERVERPORT);
				}
			} catch (IOException e1) {
				System.out.println("Found Exception " + e1);
			}
		
    }
	
	}

	public static void main(String args[]) {
		Client c = new Client();
		String test = JOptionPane.showInputDialog(null, "Would you like to run in test mode (y or n) ", "Test Mode?", JOptionPane.INFORMATION_MESSAGE);
		while (!test.equals("y") && !test.equals("n")) {
			test = JOptionPane.showInputDialog(null, "Would you like to run in test mode (y or n) ", "Test Mode?", JOptionPane.INFORMATION_MESSAGE);
		}
		if (test.equals("y")) c.sendPort = 23;
		else c.sendPort = 69;

		c.path = JOptionPane.showInputDialog(null, "Enter path where Client will Read/Write", "Enter Path", JOptionPane.INFORMATION_MESSAGE);
		
		if(c.path == null){
			System.out.println("Closing client");
			System.exit(0);
		}
		else {
			
			c.runClient();
			
			
		}
	}

}
