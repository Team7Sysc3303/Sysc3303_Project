import java.io.*;
import java.net.*;
import java.util.*;



public class ResponseHandler implements Runnable {

	private Server.Request request;
	private DatagramPacket receivePacket, sendPacket;
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
			if (receivePacket.getData()[0]==0 && receivePacket.getData()[1]==1)//RRQ case
				in = new BufferedInputStream(new FileInputStream(filename));
			if (receivePacket.getData()[0]==0 && receivePacket.getData()[1]==2)//WRQ case
				out= new BufferedOutputStream(new FileOutputStream(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}//constructor
	
	private byte[] formResponse(BufferedInputStream in,BufferedOutputStream out) throws FileNotFoundException, IOException
	{
		byte[] responseR =new byte[516];//byte array for read response (with 512 byte data)
		byte[] responseW =new byte[4];//byte array for ACK for write response;
		byte[] data=new byte[512];
		int n;//for length of write forming
		//read request
		if (request==Server.Request.READ){
			System.arraycopy(Server.readResp,0,responseR,0,2); //{0,3} at the beginning
			//if ((n = in.read(data)) != -1){			
				if (receivePacket.getData()[0]==0 && receivePacket.getData()[1]==1){//RRQ case
					System.arraycopy(Server.readResp,2,responseR,2,2);//{0,3,0,1} - data first block					
				} else if (receivePacket.getData()[0]==0 && receivePacket.getData()[1]==4){
					System.arraycopy(receivePacket.getData(),2,responseR,2,2);//{0,3,n} - n=# block
				}
				n = in.read(data);//get n bytes in data (512 or less)
				System.arraycopy(data,0,responseR,4,n);//append data part to message
				return responseR;
			//}				
		} else if (request==Server.Request.WRITE){
			System.arraycopy(Server.writeResp,0,responseW,0,2);//{0,4} at the start of array
			if (receivePacket.getData()[0]==0 && receivePacket.getData()[1]==2){//WRQ case
				System.arraycopy(Server.writeResp,2,responseR,2,2);//{0,4,0,0} - data first block					
			} else if (receivePacket.getData()[0]==0 && receivePacket.getData()[1]==3){
				System.arraycopy(receivePacket.getData(),2,responseR,2,2);//{0,4,n} - n=# block
				//message is formed, now need to write in server
				System.arraycopy(receivePacket.getData(),4, data,0,
						receivePacket.getLength()-4);//copy block of data from packet
				out.write(data);
				return responseW;
			}			
		}
		
		return null;
	}//end of formResponse
	
	public void run(){
		byte[] response=new byte[516];
		try{
			response=formResponse(in,out);
		}catch (IOException ie){
			//System.out.println("I/O Error: "+ie);
			ie.printStackTrace();
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
