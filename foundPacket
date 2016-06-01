private boolean foundPacket(DatagramPacket p) {
		int type;
		int temp = 0;
		byte blk[] =new byte[2];
		// set type, to the type of packet it is based on its second byte
		type = (int)(p.getData()[1]);
		if(verbose){
		System.out.println("ConnectionManagerESim: packet type is " + type);
		}
		// Create a new byte that stores the block number in the right 2 byte format
		blk[0] = (byte)((packetNumber - (packetNumber % 256)) /256);
		// the low byte
		blk[1] = (byte)(packetNumber % 256);

		// the packet's block number that we are checking
		byte blockReceived[] = {p.getData()[2], p.getData()[3]};
		if(verbose){
		System.out.println("Block number received: " + Integer.toHexString(blockReceived[0]) + "" + Integer.toHexString(blockReceived[1]));
		}
		// check if it is the right packet type
		if (type ==  packetType) {
			if (blk[0] == blockReceived[0] && blk[1] == blockReceived[1]){
				if(verbose){
				System.out.println("Error simulator has the right block number.");
				}
				return true;
			} // end if
		} // end if
		return false;
	}// end method
