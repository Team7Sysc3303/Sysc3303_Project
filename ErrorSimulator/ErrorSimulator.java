package ErrorSimulator;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;
import java.util.Scanner;

public class ErrorSimulator extends Thread {
	/**
	 *  This socket will be used to send and receive to and from the client
	 */	
	private DatagramSocket clientSocket;

	/**
	 * This is used to communicate with the server
	 */
	private DatagramSocket serverSocket;

	/**
	 * This is the error type that the user has chosen. Default value is NORMAL_MODE
	 */
	private Errors errorType = Errors.NORMAL_MODE;

	/**
	 * This is the network error type that the user has chosen. Default value is NORMAL
	 */
	private NetworkErrors networkErrorType = NetworkErrors.NORMAL;

	/**
	 * This is the type of packet (ACK or DATA) that the error must be performed on
	 */
	private String packetToPerform;

	/**
	 * This is the block number the error must be performed on
	 */
	private int blockToPerform;

	/**
	 * This is the client port
	 */
	private int client;

	/**
	 * This is the server port
	 */
	private int server;

	public ErrorSimulator() {
		try {
			clientSocket = new DatagramSocket(23);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return the port the client socket uses
	 */	
	public int getClientPort() {
		return clientSocket.getLocalPort();
	}

	/**
	 * Creates and returns a randomized array of bytes. Used to generate invalid packets.
	 * @param packet the initial, unaltered packet
	 * @return a byte array to be fed into the receiver with random data;
	 */	
	private byte[] createFalsePacket(DatagramPacket packet){
		byte[] randomData = new byte[packet.getLength()];
		for(int i = 4; i < packet.getLength(); i++){
			Random RNG = new Random();
			randomData[i] = (byte) RNG.nextInt(9);
		}
		packet.setData(randomData);
		return packet.getData();
	}

	/**
	 * Print out the type of a given packet.
	 * @param packet the packet to print the info of
	 */
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

	/**
	 * This checks the current packet to see if an error needs to be performed
	 * If yes then this function will perform the error
	 * @param p the current packet
	 * @param sendPort the port the packet should be sent to
	 */
	public void checkPacket(DatagramPacket p, int sendPort, DatagramSocket newClientSocket, InetAddress clientAddress) {
		if (errorType != Errors.NORMAL_MODE || networkErrorType != NetworkErrors.NORMAL) {
			String currentPacket = packetInfo(p);
			if ((currentPacket.equals(packetToPerform) && p.getData()[3] == (byte) blockToPerform) ||
					((packetToPerform.equals("RRQ") || packetToPerform.equals("WRQ")) && currentPacket.equals(packetToPerform))) {
				if (networkErrorType.equals(NetworkErrors.DELAY)) {
					System.out.println("-----------------------------");
					System.out.println("Simulating Delay of packet");
					try {
						System.out.println("Sleeping for half a second");
						Thread.sleep(500);
						System.out.println("Slept for half a second, now continuing");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println("-----------------------------");
				} else if (networkErrorType.equals(NetworkErrors.LOSE)) {
					System.out.println("-----------------------------");
					System.out.println("Simulating loss of Packet");
					System.out.println("Lost Packet");
					try{
						if (packetToPerform.equals("DATA")) {
							if (sendPort == server) {
								newClientSocket.receive(p);
								System.out.println("Received " + packetInfo(p) + " from Client again");
							} else {
								serverSocket.receive(p);
								System.out.println("Received " + packetInfo(p) + " from Server again");
							}
						} else {
							if (sendPort == client) {
								newClientSocket.receive(p);
								System.out.println("Received " + packetInfo(p) + " from Client again");
								System.out.println("Resending " + packetInfo(p) + " to Server");
								p.setPort(server);
								serverSocket.send(p);
								serverSocket.receive(p);
								System.out.println("Received " + packetInfo(p) + " from Server again");
							} else {
								serverSocket.receive(p);
								System.out.println("Received " + packetInfo(p) + " from Server again");
								System.out.println("Resending " + packetInfo(p) + " to Client");
								DatagramPacket sendPacket = new DatagramPacket(p.getData(), p.getLength(), clientAddress, client);
								newClientSocket.send(sendPacket);
								newClientSocket.receive(p);
								System.out.println("Received " + packetInfo(p) + " from Client again");
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("-----------------------------");
				} else if (networkErrorType.equals(NetworkErrors.DUPLICATE)) {
					System.out.println("-----------------------------");
					System.out.println("Simulating Duplication");
					try {
						if (sendPort == server) {
							DatagramPacket dup = new DatagramPacket(p.getData(), p.getLength(), p.getAddress(), server);
							System.out.println("Sending " + packetInfo(p) + " to Server");
							p.setPort(server);
							serverSocket.send(p);
							p = new DatagramPacket(new byte[516], 516);
							serverSocket.receive(p);
							System.out.println("Received " + packetInfo(p) + " from Server");
							p.setPort(client);
							p.setAddress(clientAddress);
							System.out.println("Sending " + packetInfo(p) + " to Client");
							newClientSocket.send(p);
							newClientSocket.receive(p);
							System.out.println("Received " + packetInfo(p) + " from Client");
							p.setPort(server);
							p.setAddress(InetAddress.getLocalHost());
							System.out.println("Sending Duplicate " + packetInfo(dup) + " to Server");
							serverSocket.send(dup);
						} else {
							DatagramPacket dup = new DatagramPacket(p.getData(), p.getLength(), clientAddress, client);
							System.out.println("Sending " + packetInfo(p) + " to Client");
							p.setPort(client);
							p.setAddress(clientAddress);
							newClientSocket.send(p);
							p = new DatagramPacket(new byte[516], 516);
							newClientSocket.receive(p);
							System.out.println("Received " + packetInfo(p) + " from Client");
							p.setPort(server);
							p.setAddress(InetAddress.getLocalHost());
							System.out.println("Sending " + packetInfo(p) + " to Server");
							serverSocket.send(p);
							serverSocket.receive(p);
							System.out.println("Received " + packetInfo(p) + " from Server");
							p.setPort(client);
							System.out.println("Sending Duplicate " + packetInfo(dup) + " to Client");
							newClientSocket.send(dup);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("-----------------------------");
				}
				if (errorType.equals(Errors.INVALID_TID)) {
					simulateError5(p, sendPort, clientAddress);
				} else if (errorType.equals(Errors.ILLEGAL_TFTP_OP)) {
					simulateError4(p, sendPort, newClientSocket, clientAddress);
				} 
			}
		}
	}

	/**
	 * Function to simulate error code 4 (Invalid TFTP Operation)
	 * @param p the current packet
	 * @param sendPort the port the packet should be sent to
	 */
	private void simulateError4(DatagramPacket p, int sendPort, DatagramSocket newClientSocket, InetAddress clientAddress) {
		System.out.println("------------------------------------------------");
		System.out.println("Invalid TFTP Operation Simulation (Error Code 4)");
		String first, second;
		InetAddress firstAdd, secondAdd;
		DatagramSocket firstSocket, secondSocket;
		int firstPort, secondPort;
		try {
			if (sendPort == server) {
				first = "Server"; 
				firstSocket = serverSocket;
				firstPort = server;
				firstAdd = InetAddress.getLocalHost();
				second = "Client";
				secondSocket = newClientSocket;
				secondPort = client;
				secondAdd = clientAddress;
			} else {
				first = "Client";
				firstSocket = newClientSocket;
				firstPort = client;
				firstAdd = clientAddress;
				second = "Server";
				secondSocket = serverSocket;
				secondPort = server;
				secondAdd = InetAddress.getLocalHost();
			}
			byte[] b = createFalsePacket(p);

			System.out.println("Sending Invalid packet to " + first);
			DatagramPacket packet = new DatagramPacket(b, b.length, firstAdd, firstPort);
			firstSocket.send(packet);
			firstSocket.receive(packet);
			System.out.println("Received " + packetInfo(packet) + " from " + first);
			System.out.println("Sending " + packetInfo(packet) + " to " + second);
			p.setPort(secondPort);
			p.setAddress(secondAdd);
			secondSocket.send(packet);
			secondSocket.receive(packet);
			System.out.println("Received " + packetInfo(packet) + "from " + second);
			System.out.println("---------------------------------------------------");
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	/**
	 * Function to simulate error 5 (Invalid TID)
	 * @param p the current packet
	 * @param sendPort the port the current packet should be sent to
	 */
	private void simulateError5(DatagramPacket p, int sendPort, InetAddress clientAddress) {
		System.out.println("-------------------------------------");
		System.out.println("Invalid TID Simulation (Error Code 5)");

		try {
			String first, second;
			if (sendPort == server) {
				first = "Server"; 
				second = "Client";
			} else {
				first = "Client";
				second = "Server";
			}
			DatagramSocket TIDErrorSocket = new DatagramSocket();

			DatagramPacket newPacket;
			if (sendPort == server) {
				newPacket = new DatagramPacket(p.getData(), p.getLength(), InetAddress.getLocalHost(), sendPort);
			} else {
				newPacket = new DatagramPacket(p.getData(), p.getLength(), clientAddress, sendPort);
			}

			System.out.println("Sending to " + first + " using new Socket");
			TIDErrorSocket.send(newPacket);
			byte[] b = new byte[516];
			DatagramPacket receive = new DatagramPacket(b, 516);
			TIDErrorSocket.receive(receive);
			if (packetInfo(receive).equals("ERROR")) {
				System.out.println("Received a " + packetInfo(receive) + "packet from " + first);
			}
			if (sendPort == server) {
				receive.setPort(client);
				receive.setAddress(clientAddress);
			} else {
				receive.setPort(server);
				receive.setAddress(InetAddress.getLocalHost());
			}
			TIDErrorSocket.send(receive);
			System.out.println("Sending Error Packet to " + second);						
			b = new byte[516];
			receive = new DatagramPacket(b, 516);
			TIDErrorSocket.receive(receive);
			System.out.println("Received a " + packetToPerform + "from " + second);
			System.out.println("-------------------------------------");
			TIDErrorSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This function runs the Error Simulator
	 */
	public void run() {
		byte[] b = new byte[516];
		DatagramPacket receival = new DatagramPacket(b, b.length);
		System.out.println("ErrorSimulator running for Client: " + getClientPort());
		try {
			while (true) {
				serverSocket = new DatagramSocket();
				clientSocket.receive(receival);
				System.out.println("Received " + packetInfo(receival) + " packet from Client");
				client = receival.getPort();
				DatagramPacket send = new DatagramPacket(b, receival.getLength(), InetAddress.getLocalHost(), 69);

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							int port = receival.getPort();
							InetAddress clientAddress = receival.getAddress();
							DatagramSocket newClientSocket = new DatagramSocket();
							//							checkPacket(receival, server, newClientSocket, clientAddress);
							//							System.out.println("Sending " + packetInfo(receival) + " packet to Server");
							serverSocket.send(send);
							while (true) {
								byte[] serverReceival = new byte[516];
								DatagramPacket receiveFromServer = new DatagramPacket(serverReceival, serverReceival.length);
								serverSocket.receive(receiveFromServer);
								System.out.println("Received " + packetInfo(receiveFromServer) + " packet from Server");
								server = receiveFromServer.getPort();
								checkPacket(receiveFromServer, client, newClientSocket, clientAddress);
								DatagramPacket sendClientPacket = new DatagramPacket(serverReceival, receiveFromServer.getLength(), clientAddress, port);
								System.out.println("Sending " + packetInfo(sendClientPacket) + " packet to Client");
								newClientSocket.send(sendClientPacket);

								byte[] clientReceival = new byte[516];
								DatagramPacket receiveClientPacket = new DatagramPacket(clientReceival, clientReceival.length);
								newClientSocket.receive(receiveClientPacket); 
								System.out.println("Received " + packetInfo(receiveClientPacket) + " packet from Client");
								checkPacket(receiveClientPacket, server, newClientSocket, clientAddress);
								DatagramPacket sendServer = new DatagramPacket(clientReceival, receiveClientPacket.getLength(), InetAddress.getLocalHost(), receiveFromServer.getPort());
								System.out.println("Sending " + packetInfo(sendServer) + " packet to Server");
								serverSocket.send(sendServer);
							}
						} catch (IOException e ) {
							e.printStackTrace();
						}
					}
				}).start();
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Displays all the commands possible when choosing an error
	 */
	public void displayPossibleCommands() {
		System.out.println("Enter: ");
		for (Errors s : Errors.values()) System.out.println(s.toString());
	}

	/**
	 * Displays all possible commands to choose a network error
	 */
	public void askNetworkErrors() {
		System.out.println("Enter: ");
		for (NetworkErrors s : NetworkErrors.values()) System.out.println(s.toString());

	}

	public static void main(String args[])
	{
		ErrorSimulator es = new ErrorSimulator();
		es.start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				@SuppressWarnings("resource")
				Scanner userInput = new Scanner(System.in);
				while (true) {
					es.askNetworkErrors();
					String networkInput = userInput.nextLine().toLowerCase();
					if (networkInput.equals("1")) {
						es.networkErrorType = NetworkErrors.DELAY;
					} else if (networkInput.equals("2")) {
						es.networkErrorType = NetworkErrors.DUPLICATE;
					} else if (networkInput.equals("3")) {
						es.networkErrorType = NetworkErrors.LOSE;
					} else {
						es.networkErrorType = NetworkErrors.NORMAL;
					}
					boolean errorSet = false;
					while(!errorSet) {
						es.displayPossibleCommands();
						String input = userInput.nextLine().toLowerCase();
						if (input.equals("0")) {
							es.errorType = Errors.NORMAL_MODE;
						} else if (input.equals("2")) {
							es.errorType = Errors.INVALID_TID;
						} else if (input.equals("1")) {
							es.errorType = Errors.ILLEGAL_TFTP_OP;
						} else {
							System.out.println("The command that you have entered is not valid. Enter \"commands\" to see all valid commands");
							continue;
						}
						errorSet = true;
					}
					if (es.errorType != Errors.NORMAL_MODE || es.networkErrorType != NetworkErrors.NORMAL) {
						boolean packet = false;
						while (!packet) {
							System.out.println("Enter Packet to Perform Error On: (DATA,ACK,RRQ,WRQ)");
							es.packetToPerform = userInput.nextLine().toUpperCase();
							if (es.packetToPerform.equals("ACK") || es.packetToPerform.equals("DATA") ||
									es.packetToPerform.equals("RRQ") || es.packetToPerform.equals("WRQ")) packet = true;
						}
						System.out.println("Enter Block Number to Perform Error On: ");
						if (!es.packetToPerform.equals("RRQ") && !es.packetToPerform.equals("WRQ"))
							es.blockToPerform = new Integer(userInput.nextLine().toLowerCase());
					}
					System.out.println("------------------------------------------------");
					System.out.println("System is set to perform " + es.networkErrorType.toString() + " and error type: " +es.errorType.toString());
					System.out.println("Your setup has been saved!");
					System.out.println("** Run Client operation now to see desired error simulation.**");
					System.out.println("------------------------------------------------");
					System.out.println("Showing you initial menu again so you may change the setup.");
				}
			}
		}).start();
	}
}
