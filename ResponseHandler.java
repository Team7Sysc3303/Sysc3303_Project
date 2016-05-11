package iteration1;

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
	private boolean finished=false;
	private File writeFile;
	private int n;
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
				out= new BufferedOutputStream(new FileOutputStream("textc.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}//constructor
	
	private byte[] formResponse(BufferedInputStream in,BufferedOutputStream out) throws FileNotFoundException, IOException
	{
		byte[] responseR =new byte[516];//byte array for read response (with 512 byte data)
		byte[] responseW =new byte[4];//byte array for ACK for write response;
		byte[] data=new byte[512];
		int offset=0;//for length of write forming
		//read request
		n = 0;
		//System.out.println("Inside FormMsg"+" "+request+"1"+filename+"1");
		if (request==Server.Request.READ){
			System.out.println("read if");
			System.arraycopy(Server.readResp,0,responseR,0,2); //{0,3} at the beginning
			//if ((n = in.read(data)) != -1){			
				if (receivePacket.getData()[0]==0 && receivePacket.getData()[1]==1){//RRQ case
					System.arraycopy(Server.readResp,2,responseR,2,2);//{0,3,0,1} - data first block					
				} else if (receivePacket.getData()[0]==0 && receivePacket.getData()[1]==4){//ACK SST
					//System.arraycopy(receivePacket.getData(),2,responseR,2,2);//{0,3,n-1} - n=# block
					//responseR[]
					offset=receivePacket.getData()[2]*10+receivePacket.getData()[3]+1;
					responseR[2]=(byte) (offset/10);
					responseR[3]=(byte) (offset%10);
					//System.out.println(responseR.toString());
				}
				if(data==null) System.out.println("byte - null");
				if(in==null) System.out.println("buffer - null");
				if (data!=null&&in!=null)
					n = in.read(data);//,0,data.length);//get n bytes in data (512 or less)
				    System.out.println("Value of N is: "+ n);
				if(n!=-1)
					System.arraycopy(data,0,responseR,4,n);//append data part to message
				else {
					in.close();
					finished=true;
				}
				
				return responseR;
			//}				
		} else if (request==Server.Request.WRITE){
			System.out.println("write if");
			/*writeFile = new File("Textc.txt");
			   if(!writeFile.exists()){
			   if (writeFile.getParentFile().mkdir()) {
			       writeFile.createNewFile();
			   } else {
			       throw new IOException("Failed to create directory " + writeFile.getParent());
			   } 
			   }*/
			System.arraycopy(Server.writeResp,0,responseW,0,2);//{0,4} at the start of array
			if (receivePacket.getData()[0]==0 && receivePacket.getData()[1]==2){//WRQ case
				System.arraycopy(Server.writeResp,2,responseR,2,2);//{0,4,0,0} - data first block					
			} else if (receivePacket.getData()[0]==0 && receivePacket.getData()[1]==3){
				System.arraycopy(receivePacket.getData(),2,responseR,2,2);//{0,4,n} - n=# block
				//message is formed, now need to write in server
				System.arraycopy(receivePacket.getData(),4, data,0,
						receivePacket.getLength()-4);//copy block of data from packet
				if(data==null) System.out.println("byte - null");
				if(out==null) System.out.println("buffer - null");
				if (data!=null&&out!=null)
					out.write(data);				
			}
			return responseW;
		}
		System.out.println("NULL");
		return null;
	}//end of formResponse
	
	public void run(){
		responseH();
		byte[] data=new byte[516];
		while(!finished){
	         receivePacket = new DatagramPacket(data, data.length);
	         
	         System.out.println("Server: Waiting for packet.");
	         // Block until a datagram packet is received from receiveSocket.
	         try {
	            sendSocket.receive(receivePacket);
	         } catch (IOException e) {
	            e.printStackTrace();
	            System.exit(1);
	         }
	         
	         // Process the received datagram.
	         System.out.println("Server: Packet received:");
	         System.out.println("From host: " + receivePacket.getAddress());
	         System.out.println("Host port: " + receivePacket.getPort());
	         int len = receivePacket.getLength();
	         System.out.println("Length: " + len);
	         System.out.println("Containing: " );
	         
	         if(data[1]==3)
	         {
	         if(receivePacket.getLength()<516){
	        	 
	         	responseH();
	         	try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	         	finished = true;
	         }
	         } else if (data[1]==4){
	        	 responseH();
	         }
	    }//end while
		

		// We're finished with this socket, so close it.
		sendSocket.close();		
	}//end of run
	
	private void responseH(){
		byte[] response=new byte[516];
		try{
			response=formResponse(in,out);
		}catch (IOException ie){
			//System.out.println("I/O Error: "+ie);
			ie.printStackTrace();
		}
		sendPacket = new DatagramPacket(response, n+4 /*response.length*/,
                receivePacket.getAddress(), receivePacket.getPort());

		System.out.println("Server: Sending packet:");
		System.out.println("To host: " + sendPacket.getAddress());
		System.out.println("Destination host port: " + sendPacket.getPort());
		int len = sendPacket.getLength();
		System.out.println("Length: " + len);
		System.out.println("Containing: ");
		for (int j=0;j<len;j++) {
			 System.out.print(response[j] + " ");
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
	}
	
}
