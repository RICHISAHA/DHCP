package DHCPServer;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

import DHCPHelper.Utility;
/**
 * The IPStorrage manages all IP addresses in this storage
 */
public class IPStorage 
{
	private ArrayList<IPContainer> listIPContainer;
	private byte[] IP  = Utility.toBytes(new int[]{128,1,0,1});
	private int IPRange = 5;
	
	/**
	 * Constructor calls the initialization of the pool of IP addresses
	 */
	public IPStorage()
	{
		listIPContainer = new ArrayList<>();
		init();
	}
	
	/**
	 * Initialize the pool of IP addresses to manage
	 */
	private void init()
	{
		byte[] IPIncrementing = IP;
		try
		{
			for(int i = 0; i < IPRange; i++)
			{
				IPContainer ipcontainer = new IPContainer(IPIncrementing);
				IPIncrementing = incrementIPAddress(IPIncrementing);
				listIPContainer.add(ipcontainer);
			}
		}
		catch(Exception e)
		{e.printStackTrace();}
	}
	
	/**
	 * Increment the given IP address and checks if its valid
	 * Assume only in the range of zero to 255
	 * @param IPAddress
	 * @return
	 */
	private byte[] incrementIPAddress(byte[] IPAddress)
	{
		if(IPAddress.length != 4) throw new IllegalArgumentException("Invalid ip4 format");
		int IPInt = Utility.toInt(IPAddress);
		IPInt++;
		return Utility.toByteArray(IPInt);
	}
	
	/**
	 * @return if the given IP is reserved or not
	 * @param ip
	 */
	private boolean isReserved(InetAddress ip)
	{
		for(IPContainer ipcontainer : listIPContainer)
			if(ip.equals(ipcontainer.IPAddress) && ipcontainer.isReserved())
				return true;
		return false;
	}
	
	/**
	 * Look for a reserved or allocated IP address to the given MAC address
	 * @param 	macaddress
	 * @return	the reserved or allocated IP address (if there is one)
	 * 			otherwise null will be returned
	 */
	public byte[] lookUp(byte[] macaddress)
	{
		for(IPContainer ipcontainer : listIPContainer)
			if(ipcontainer.reserver != null && Arrays.equals(ipcontainer.reserver, macaddress)) return ipcontainer.IPAddress;
		return null;
	}
	
	/**
	 * Reserve a free IP address to the given MAC adress for the given duration
	 * @param 	macaddress
	 * @param 	leaseDuration
	 * @return	the allocated IP address
	 */
	public byte[] reserveAddress(byte[] macaddress, int leaseDuration)
	{
		if(lookUp(macaddress) != null) return lookUp(macaddress);
		for(IPContainer ipcontainer : listIPContainer)
			if(!ipcontainer.isReserved() && !ipcontainer.isAllocated())
			{
				ipcontainer.reserve(macaddress, leaseDuration);
				return ipcontainer.IPAddress;
			}
		return null;
	}
	
	/**
	 * Allocate the IP address that is already reserved for the given MAC address
	 * @param 	macaddress
	 * @return	the allocated IP address
	 */
	public byte[] allocateAddress(byte[] macaddress)
	{
		if(lookUp(macaddress) == null) return null;

		for(IPContainer ipcontainer : listIPContainer)
			if(Arrays.equals(ipcontainer.reserver, macaddress))
			{
				ipcontainer.allocate();
				return ipcontainer.IPAddress;
			}
		
		System.out.println("Allocation failed");
		return null;
		
	}
	
	/**
	 * Release the IP address reserved by or allocated to the given MAC address
	 * @param macaddress
	 * @return
	 */
	public boolean release(byte[] macaddress)
	{
		if(lookUp(macaddress) == null) return false;
		for(IPContainer ipcontainer : listIPContainer)
			if(ipcontainer.reserver != null && Arrays.equals(ipcontainer.reserver, macaddress)) 
			{
				ipcontainer.release();
				return true;
			}
		return false;
	}
	
	/**
	 * Print the content of this IP storage
	 */
	public void printContent()
	{
		//System.out.println("Size of IP pool listIPContainer.size());
		int numberReserved = 0, numberAllocated = 0;
		for(IPContainer ipc : listIPContainer)
		{
			if(ipc.isReserved()) ++numberReserved;
			if(ipc.isAllocated()) ++ numberAllocated;
		}
		System.out.println("Size of IP pool: reserved: " + numberReserved + " , allocated: " + numberAllocated);
	}
	
	/**
	 * Update the content of this IP Storage, lower all leasetimes of allocated IP's
	 */
	public void update() 
	{
		for(IPContainer ipc : listIPContainer)
		{
			if(ipc.isAllocated() && ipc.leaseDuration > 0) ipc.update();
			if(ipc.isAllocated() && ipc.leaseDuration <= 0)
			{
				ipc.release();
				System.out.println("IP released ");
				printContent();
			}
			
		}
		
		
	}
}
