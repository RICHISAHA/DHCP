package DHCPServer;

import java.net.InetAddress;

import DHCPHelper.Utility;

/**
 * An IPContainer contains an IP and can determine it is Free, Reserved, Allocated and for how long
 * If reserved or allocated this IP is associated with the MAC address of the client
 */
public class IPContainer 
{
	public byte[] IPAddress;
	private enum STATE {FREE, RESERVED, ALLOCATED};
	private STATE stateValue;
	public byte[] reserver;
	public int leaseDuration;
	
	/**
	 * Constructor of the IPContainer containing the given IP address
	 * @param newIPAddress
	 */
	public IPContainer(byte[] newIPAddress)
	{
		IPAddress = newIPAddress;
		reserver = null;
		leaseDuration = -1;
		stateValue = STATE.FREE;
	}
	
	/**
	 * Reserve this IP to the client with the given MAC address for the given duration
	 * @param newReserver
	 * @param newLeaseDuration
	 */
	public void reserve(byte[] newReserver, int newLeaseDuration)
	{
		reserver = newReserver;
		leaseDuration = newLeaseDuration;
		stateValue = STATE.RESERVED;
	}
	
	/**
	 * Allocate the IP address
	 */
	public void allocate()
	{
		stateValue = STATE.ALLOCATED;
	}
	
	/**
	 * Release the IP adress
	 */
	public void release()
	{
		reserver = null;
		leaseDuration = -1;
		stateValue = STATE.FREE;
	}
	
	/**
	 * @return if this IP is allocated
	 */
	public boolean isReserved()
	{
		return stateValue == STATE.RESERVED;
	}
	
	/**
	 * @return if this IP is allocated
	 */
	public boolean isAllocated() {
		
		return stateValue == STATE.ALLOCATED;
	}
	
	/**
	 * Update this IP Container
	 * The reserve time is lowered by one
	 */
	public void update()
	{
		leaseDuration -=1;
	}
	
}
