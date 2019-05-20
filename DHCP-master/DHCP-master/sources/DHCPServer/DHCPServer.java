package DHCPServer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

import javax.swing.Timer;

import DHCPHelper.Utility;
import DHCPHelper.DHCPMessage;

/**
 * This class represents a DHCPServer that can communicate with client(s) and allocate IP's
 */
public class DHCPServer 
{
	private IPStorage pool;
	private DatagramSocket serverSocket;
	private String serverIP;
	public final int portServer = 1234;
	public final int MAXLEASEDURATION = 3600;
	
	/**
	 * Constructor initializes this server
	 */
	public DHCPServer(String serverIP)
	{
		System.out.print("Initialization of the server started\n");
		try 
		{
			this.serverIP = serverIP;
			serverSocket = new DatagramSocket(portServer);
			pool = new IPStorage();
			System.out.println("Initialization of the server completed\n");
		} 
		catch (SocketException e) 
		{e.printStackTrace();}
	}
	
	/**
	 * After initialization this method will be called by the main server
	 * It will periodically call an update of the pool of IP's
	 * 
	 * Receive messages and determine it's type to construct an answer
	 */
	public void run()
	{
		ActionListener actionListener = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{pool.update();}
		};
		Timer timer = new Timer(1000,actionListener);
		timer.start();
		while(true)
		{
			pool.printContent();
			
			//listen to incoming packet
			byte[] buffer = new byte[576];
			DatagramPacket receivePacket = new DatagramPacket(buffer,buffer.length);
			try 
			{
				serverSocket.receive(receivePacket);
				System.out.print("Server receives packet with a size of " + receivePacket.getLength() +"\n");
			}
			catch (IOException e) 
			{e.printStackTrace();}
		
			Utility.printDataBytes(receivePacket.getData());
			
			//determine type of request
			DHCPMessage response = new DHCPMessage(receivePacket.getData());
			System.out.println(response);
			try
			{
				switch (response.getType())
				{
					case DHCPMessage.DHCPDISCOVER:
					try {
						DHCPOffer(receivePacket);
					} catch (UnknownHostException e) {e.printStackTrace();}
						break;
					case DHCPMessage.DHCPREQUEST:	
						if(canAcceptRequest(receivePacket))
							DHCPAck(receivePacket);
						else
							DHCPNak(receivePacket);
						break;	
					case DHCPMessage.DHCPRELEASE:
						handleDHCPRelease(receivePacket);
						break;
				}
			}
			catch(Exception e)
			{e.printStackTrace();}
		}
	}
	
	/**
	 * Build and send a DHCPOffer message out of the given message which will be a DHCPDiscover
	 * @param 	receivePacket	The received message (DHCPDiscover)
	 * @throws 	UnknownHostException	
	 */
	public void DHCPOffer(DatagramPacket receivePacket) throws UnknownHostException
	{
		System.out.print("Building DHCPOffer\n");
		DHCPMessage message = new DHCPMessage(receivePacket.getData());
		
		message.opCode = DHCPMessage.BOOTREPLY;
		
		//Reserve new IP address
		int leaseDuration = Utility.toInt(message.getOptionData((byte)51));//(int)(Math.random()*300);
		message.yourIP = pool.reserveAddress(message.clientHardWareAddress, leaseDuration);
		
		// Server IP set
		message.serverIP = InetAddress.getByName(this.serverIP).getAddress();
		
		// reset options fields
		message.resetoptions();
		
		// Set option 53 to value 2
		byte[] i = {DHCPMessage.DHCPOFFER};
		message.addOption((byte)53, (byte)1, i);
		
		message.addOption((byte)54, (byte)4, message.serverIP);
		
		// Set option 255
		int[] j = {0};
		message.addOption((byte) 255, (byte)0, Utility.toBytes(j));

		InetAddress IPClient = receivePacket.getAddress();
		int portClient = receivePacket.getPort();
		
		byte[] sendingBytes = message.retrieveBytes();
		DatagramPacket response = new DatagramPacket(sendingBytes, sendingBytes.length, IPClient, portClient);
		try 
		{
			serverSocket.send(response);
			System.out.println("DHCPOffer sent\n");
		} 
		catch (IOException e) 
		{e.printStackTrace();}
	}
	
	/**
	 * Determin if the requested IP can be allocated to the client with the given options, ...
	 * @param 	receivePacket
	 * @return	True or False
	 */
	public boolean canAcceptRequest(DatagramPacket receivePacket)
	{
		try
		{
			DHCPMessage message = new DHCPMessage(receivePacket.getData());
			return Utility.toInt(message.getOptionData((byte)51)) < MAXLEASEDURATION  && Arrays.equals(InetAddress.getByName(this.serverIP).getAddress(), message.serverIP) ;
		}
		catch(Exception e)
		{return false;}
	}
	
	/**
	 * Build and send a DHCPAck out of the received message (DHCPRequest)
	 * @param receivePacket
	 * @throws UnknownHostException
	 */
	public void DHCPAck(DatagramPacket receivePacket) throws UnknownHostException
	{
		try
		{
			System.out.print("Building DHCPAck\n");
			DHCPMessage message = new DHCPMessage(receivePacket.getData());
			
			pool.allocateAddress(message.clientHardWareAddress);
			
			message.opCode = DHCPMessage.BOOTREPLY;

			byte[] data = message.getOptionData((byte)50);
			message.yourIP = data;
			
			// Server IP set
			message.serverIP = InetAddress.getByName(serverIP).getAddress();
			
			// reset options fields
			message.resetoptions();
			
			// Set option 53 to value 5
			byte[] i = {DHCPMessage.DHCPACK};
			message.addOption((byte)53, (byte)1, i);
			
			message.addOption((byte)54, (byte)4,message.serverIP);
			
			// Set option 255
			int[] j = {0};
			message.addOption((byte) 255, (byte)0, Utility.toBytes(j));
	
			InetAddress IPClient = receivePacket.getAddress();
			int portClient = receivePacket.getPort();
			
			byte[] sendingBytes = message.retrieveBytes();
			DatagramPacket response = new DatagramPacket(sendingBytes, sendingBytes.length, IPClient, portClient);
			try 
			{
				serverSocket.send(response);
				System.out.println("DHCPAck sent\n");
			} 
			catch (IOException e) 
			{e.printStackTrace();}
		}
		catch(Exception e)
		{e.printStackTrace();}
	}
	
	/**
	 * Build and send a DHCPNak out of the received message (DHCPRequest)
	 * @param receivePacket
	 * @throws UnknownHostException
	 */
	public void DHCPNak(DatagramPacket receivePacket) throws UnknownHostException
	{
		try
		{
			System.out.print("Building DHCPNak\n");
			DHCPMessage message = new DHCPMessage(receivePacket.getData());
			
			pool.release(message.clientHardWareAddress);
			message.opCode = DHCPMessage.BOOTREPLY;

			byte[] data = message.getOptionData((byte)50);
			message.yourIP = data;
		
			// Server IP set
			message.serverIP = InetAddress.getByName(this.serverIP).getAddress();
			
			// reset options fields
			message.resetoptions();
			
			// Set option 53 to value 5
			byte[] i = {DHCPMessage.DHCPNAK};
			message.addOption((byte)53, (byte)1, i);
			
			message.addOption((byte)54, (byte)4,message.serverIP);
			
			// Set option 255
			int[] j = {0};
			message.addOption((byte) 255, (byte)0, Utility.toBytes(j));
	
			InetAddress IPClient = receivePacket.getAddress();
			int portClient = receivePacket.getPort();
			
			byte[] sendingBytes = message.retrieveBytes();
			DatagramPacket response = new DatagramPacket(sendingBytes, sendingBytes.length, IPClient, portClient);
			try 
			{
				serverSocket.send(response);
				System.out.println("DHCPNak sent\n");
			} 
			catch (IOException e) 
			{e.printStackTrace();}
		}
		catch(Exception e)
		{e.printStackTrace();}
	}
	
	/**
	 * Handling of a DHCPrelease received message
	 * Release the allocated IP-address if there is one
	 * @param receivePacket
	 */
	public void handleDHCPRelease(DatagramPacket receivePacket)
	{
		DHCPMessage message = new DHCPMessage(receivePacket.getData());
		pool.release(message.clientHardWareAddress);
	}	
}
