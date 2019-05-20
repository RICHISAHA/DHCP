package DHCPClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;

import DHCPHelper.Utility;
import DHCPHelper.DHCPMessage;
import DHCPHelper.DHCPoption;

/**
 * This DHCPClient will send and receive messages with the target of receiving and allocating an IP-address
 * The client will not broadcast a DHCP discover message, therefore an IP-address of the server must be assigned on top
 * 
 */
public class DHCPClient 
{
	// Basic values & attributes
	public final int 	portServer = 1234;
	public String IPServerString;
	public final String IPServerLocalhost = "localhost";
	public final byte MACLENGTH = 6;
	public final int leaseDuration = 60; 
	public InetAddress IPServer; 
	private DatagramSocket clientSocket;
	private byte[] IPAllocated = new byte[4];
	
	// Choose the IP-address in use
	public String IPInUse;

	// Mac-address with padding
	public int macAddress[];
	
	/**
	 * This method will be called by MainClient to initialize the client
	 */
	public  DHCPClient(String serverIP)
	{
		System.out.print("Initialization of the client started\n");
		try 
		{
			IPServerString = serverIP;
			IPInUse = IPServerString;
			macAddress = getMacAddress();
			clientSocket = new DatagramSocket();
			System.out.println("Initialization of the client completed\n");
		} 
		catch (SocketException e) 
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * This method will be called by MainClient after initialization of the client to start running the client
	 * 1: Send a DHCP-discover
	 * 2: Listen for a DHCP-offer
	 * 3: Send a DHCP-Request
	 * 4: Listen for a DHCP-ack or DHCP-nack
	 * 	  4.1: If ACK: set Client IP to the given IP
	 * 	 	 4.1.1: Ask if the client want to release the IP-adress
	 * 	  		4.1.1.1: If so, send release message
	 * 	  4.2: If NACK: No IP set, terminated
	 */
	public void run()
	{
		// Send DHCPDIscover (1)
		DHCPDiscover();
		
		// Listen for DHCPOffer (2)
		byte[] buffer = new byte[576];
		int length = buffer.length;
		DatagramPacket receivePacketOffer = new DatagramPacket(buffer,length);
		try 
		{
			clientSocket.receive(receivePacketOffer);
			System.out.print("Client receives packet with a size of " + receivePacketOffer.getLength() + "\n");
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		// Receive a message
		Utility.printDataBytes(receivePacketOffer.getData());
		DHCPMessage message = new DHCPMessage(receivePacketOffer.getData());
		System.out.println(message);
		
		// Send DHCPDiscover (3)
		DHCPRequest(receivePacketOffer);
		
		// Listen for DHCPAck OR DHCPNak (4)
		DatagramPacket receivePacketAck = new DatagramPacket(buffer,length);
		try 
		{
			clientSocket.receive(receivePacketAck);
			System.out.print("Client receives packet with a size of " + receivePacketAck.getLength() + "\n");
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		
		Utility.printDataBytes(receivePacketAck.getData());
		DHCPMessage message2 = new DHCPMessage(receivePacketAck.getData());
		System.out.println(message2);
		
		//Determine DHCPAck OR DHCPNak
		switch(message2.getType())
		{
			case DHCPMessage.DHCPACK :
				// Allocate client-IP (4.1)
				IPAllocated = message2.yourIP;
				System.out.println("IP Allocated to this client: ");
				Utility.printDataBytes(IPAllocated);
				
					// Ask if the client wants to release his IP (4.1.1)
					Scanner scanner = new Scanner(System.in);
					System.out.print("Do you want to release your ip ?\nIndicate 0 or 1: ");
					boolean input = false;
					if(scanner.hasNextInt()) 
						input = scanner.nextInt() == 1 ? true :false ;
					if(input)
						// Send DHCPRelease (4.1.1.1)
						DHCPRelease();
					scanner.close();
					break;
			case DHCPMessage.DHCPNAK : 
				//No IP allocated (4.2)
				System.out.println("No IP allocated to this client");	
				return;
				
		}
		
		// Close the socket
		clientSocket.close();	
	}	
	
	/**
	 * Build a DHCPDiscover message out of nothing and sent it (no broadcast) to the server
	 */
	public void DHCPDiscover() 
	{
		System.out.print("Building DHCPDiscover\n");
		try 
		{
			DHCPMessage message = new DHCPMessage();
			
			message.opCode = DHCPMessage.BOOTREQUEST; 
			message.hardWareType = DHCPMessage.ETHERNET;
			message.hardWareAddressLength = MACLENGTH;
			
			// Hopcount set to zero by client
			message.hopCount = 0;
			
			// Hashcode 32 bit = 4 byte fills transactionID with exactly 4 byte
			ByteBuffer a = ByteBuffer.allocate(4);
			a.putInt(message.hashCode());
			message.transactionID = a.array();
			
			// Secs to zero
			int[] b = {0,0};
			message.secs = Utility.toBytes(b);
			
			// Flags to zero
			int[] c = {0,0};
			message.flags = Utility.toBytes(c);
			
			// Not yet an IP adress -> zero
			int[] d = {0,0,0,0};
			message.clientIP = Utility.toBytes(d);
			
			// Not yet an IP adress -> zero
			int[] e = {0,0,0,0};
			message.yourIP =  Utility.toBytes(e);
			
			// Set server IP to zero
			int[] e2 = {0,0,0,0};
			message.serverIP =  Utility.toBytes(e2);
			
			// Gateway ip set to zero
			int[] f = {0,0,0,0};
			message.gateWayIP = Utility.toBytes(f);
			
			// Client hardware adress			
			message.clientHardWareAddress = Utility.toBytes(macAddress);
			
			// Optional: set or not set Server Host Name
			int[] g = new int[64];
			for(int i = 0; i < g.length; i++)
				g[i] = 0;
			message.serverHostName = Utility.toBytes(g);
			
			// Optional: set or not set Boot File Name
			int[] h = new int[128];
			for(int i = 0; i < h.length; i++)
				h[i] = 0;
			message.bootFileName = Utility.toBytes(h);
			
			// Set the magic cookie
			message.magicCookie = DHCPMessage.COOKIE;
			
			// Set option 53 to value 1
			byte[] i = {DHCPMessage.DHCPDISCOVER};
			message.addOption((byte)53, (byte)1, i);
			
			message.addOption((byte)51, (byte)4, Utility.toByteArray(leaseDuration));
			
			// Set option 255 to indicate the end of the message
			int[] j = {0};
			message.addOption((byte) 255, (byte)0, Utility.toBytes(j));
			
			// Send the message in bytes in no-broadcast to the server allocated above
			IPServer = InetAddress.getByName(IPInUse);
			DatagramPacket sendingPacket = new DatagramPacket(message.retrieveBytes(), message.getLength(), IPServer, portServer);
			clientSocket.send(sendingPacket);	
			
			System.out.println("DHCPDiscover sent\n");
		} 
		catch (Exception e) {e.printStackTrace();}
	}
	
	/**
	 * Build and send a DHCPRequest based on the given DHCPOffer message
	 * @param receivePacket		The received DHCPOffer
	 */
	public  void DHCPRequest(DatagramPacket receivePacket){
		try
		{
			System.out.print("Building DHCP request \n");
			DHCPMessage message = new DHCPMessage(receivePacket.getData());
			
			message.opCode = DHCPMessage.BOOTREQUEST;

			// clear the options
			message.resetoptions();
			
			// Set option 50 to the offered IP adress
			message.addOption((byte)50, (byte)4, message.yourIP);
			
			// Set your IP to zero
			int[] yourip = {0,0,0,0};
			message.yourIP =  Utility.toBytes(yourip);
			
			// Set option 53 to value 3
			byte[] i = {DHCPMessage.DHCPREQUEST};
			message.addOption((byte)53, (byte)1, i);
			
			// Set option 54 to the IP address of the server
			message.addOption((byte)54, (byte)4, message.serverIP);
			
			// Set option 51 to the lease duration of the asked IP
			message.addOption((byte)51, (byte)4, Utility.toByteArray(leaseDuration));			
		
			// Set option 255 to indicate the end of the message
			int[] j = {0};
			message.addOption((byte) 255, (byte)0, Utility.toBytes(j));
			
			// Send the message to the server
			IPServer = InetAddress.getByAddress(message.serverIP);
			int portServer = receivePacket.getPort();
			DatagramPacket sendingPacket = new DatagramPacket(message.retrieveBytes(), message.getLength(), IPServer, portServer);
			clientSocket.send(sendingPacket);	
			System.out.println("DHCPRequest sent\n");
		}
		catch(Exception e){e.printStackTrace();}
	}
	
	/**
	 * Build and send a DHCPRelease message from scratch
	 */
	public void DHCPRelease()
	{
		try
		{
			System.out.print("Building DHCP Release \n");

			DHCPMessage message = new DHCPMessage();
	
			message.opCode = DHCPMessage.BOOTREQUEST; 
			message.hardWareType = DHCPMessage.ETHERNET;
			message.hardWareAddressLength = MACLENGTH;
			message.hopCount = 0;
			
			// Hashcode 32 bit = 4 byte fills transactionID with exactly 4 byte
			ByteBuffer a = ByteBuffer.allocate(4);
			a.putInt(message.hashCode());
			message.transactionID = a.array();
			
			// Secs to zero
			int[] b = {0,0};
			message.secs = Utility.toBytes(b);
			
			// Flags to zero
			int[] c = {0,0};
			message.flags = Utility.toBytes(c);
			
			// Set the client IP to the IP that was allocated
			message.clientIP = IPAllocated;
			
			// Set your IP to zero
			int[] e = {0,0,0,0};
			message.yourIP =  Utility.toBytes(e);
			
			// Set server IP to zero
			int[] e2 = {0,0,0,0};
			message.serverIP =  Utility.toBytes(e2);
			
			// Gateway ip set to zero
			int[] f = {0,0,0,0};
			message.gateWayIP = Utility.toBytes(f);
			
			// Client hardware address			
			message.clientHardWareAddress = Utility.toBytes(macAddress);
			
			// Optional: set or not set Server Host Name
			int[] g = new int[64];
			for(int i = 0; i < g.length; i++)
				g[i] = 0;
			message.serverHostName = Utility.toBytes(g);
			
			// Optional: set or not set Server Host Name
			int[] h = new int[128];
			for(int i = 0; i < h.length; i++)
				h[i] = 0;
			message.bootFileName = Utility.toBytes(h);
			
			// Set the magic cookie
			message.magicCookie = DHCPMessage.COOKIE;
			
			// Set option 53 to value 7
			byte[] i = {DHCPMessage.DHCPRELEASE};
			message.addOption((byte)53, (byte)1, i);
			
			// Set option 54 to value of the IP of the server that allocated the IP address
			
			message.addOption((byte)54, (byte)4, IPInUse.getBytes());
			// Set option 255 to indicate the end of the message
			int[] j = {0};
			message.addOption((byte) 255, (byte)0, Utility.toBytes(j));
			
			// Send the message to the server
			IPServer = InetAddress.getByName(IPInUse);
			DatagramPacket sendingPacket = new DatagramPacket(message.retrieveBytes(), message.getLength(), IPServer, portServer);
			clientSocket.send(sendingPacket);	
			System.out.println("DHCPRelease sent\n");
		}
		catch(Exception e){e.printStackTrace();}
	}
	
	public static int[] getMacAddress() {
		byte[] byteMacAddress = null;
		int intMacAddress[] = new int[16];
		try {
			InetAddress address = InetAddress.getLocalHost();
			NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
			byteMacAddress = networkInterface.getHardwareAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		assert(byteMacAddress != null);
		for (int i = 0; i < byteMacAddress.length; i++) {
			intMacAddress[i] = Integer.parseInt(String.format("%02X", byteMacAddress[i]), 16);
		}
		return intMacAddress;
	}
}
