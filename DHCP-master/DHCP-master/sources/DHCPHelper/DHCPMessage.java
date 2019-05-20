package DHCPHelper;

import java.util.ArrayList;
import java.util.Arrays;
/**
 * This class represents a DHCP message
 * It can collect all data for a message including options
 * Afterwards it can generate the message represented in bytes
 * Or visa versa, derive the different field from a given message
 * 
 */
public class DHCPMessage 
{
	// Often used variables
	
	/**
	 * General type message
	 * 1=request | 2= reply
	 * No other values possible
	 */
	public final static byte BOOTREQUEST = 1;
	public final static byte BOOTREPLY = 2;

	public byte opCode; 
	
	/**
	 * Hardware type
	 * 1 = 10MB ethernet
     * 6 = IEE802 Network
     * 7 = ARCNET
   	 * 11 = Localtalk
     * 12 = Localnet
     * 14 = SMDS
     * 15 = Frame relay
     * 16 = Asynchronous Transfer mode
     * 17 = HDLC
     * 18 = Fibre channel
     * 19 = Asynchronous Transfer mode
     * 20 = Serial Line
	 */
	public final static byte ETHERNET = 1;
	public final static byte IEEE802 = 6;
    public byte hardWareType;

    /**
     * Hardware address length: length of the MAC-ID
     * ex. PC_ASUS = 6
     */
    public byte hardWareAddressLength; 
    
    /**
     * Hw options
     */
    public byte hopCount;
    
    /**
     * Transaction-ID (5)
     * To recognize the message request VS reply
     */
    public byte[] transactionID = new byte[4]; 
    
    /**
     * elapsed time from trying to boot (3)
     */
    public byte[] secs = new byte[2];
    
    /**
     * flags (3)
     */
    public byte[] flags = new byte[2];
    
    /**
     * Client IP (5)
     * 0 when no IP or invalid
     */
    public byte[] clientIP = new byte[4]; 
    
    /**
     * Your client IP (5)
     * The IP address that the server is assigning to the client
     * */
    public byte[] yourIP = new byte[4];
    
    /**
     * Server IP (5)
     */
    public byte[] serverIP = new byte[4]; 
    
    /**
     * relay agent IP (5)
     */
    public byte[] gateWayIP = new byte[4];
    
    /**
     * Client HardWare address (16)
     */
    public byte[] clientHardWareAddress = new byte[16];
    
    /**
     * Optional server host name (64)
     */
    public byte[] serverHostName = new byte[64];
    
    /**
     * Boot file name (128)
     */
    public byte[] bootFileName = new byte[128];
    
    /**
     * Fix code combination (99.130.83.99), defining start of options zone
     */
    public static final byte[] COOKIE = Utility.toBytes(new int[]{99,130,83,99});
    public byte[] magicCookie = new byte[4];
    
    /**
     * Add option to the HDCP message
     * @param Code		The Code of the option
     * @param Length	The length corresponding with the option
     * @param Data		The actual value of the code
     */
    public void addOption(byte Code, byte Length, byte[] Data){
    	DHCPoption newoption = new DHCPoption(Code, Length, Data);
    	options.add(newoption);
    }
    
    public ArrayList<DHCPoption> options = new ArrayList<>();
    
    public final static int MINLENGTH = 240;   
    public final static int MAXLENGTH = 576;
    
    /**
     * Constructor that constructs an empty message
     */
    public DHCPMessage()
    {
    	
    }
    
    /**
     * Constructor that constructs an instance of DHCP message out of a given message
     * @param 	buffer (message)
     */
    public DHCPMessage(byte[] buffer)
    {
    	opCode = buffer[0];
    	hardWareType = buffer[1];
    	hardWareAddressLength = buffer[2];
    	hopCount = buffer[3];
    	
    	int j = 4;
    	
    	for(int i = 0; i < transactionID.length; i++)
    		transactionID[i] = buffer[i+j];
    	j += transactionID.length;
    	
    	for(int i = 0; i < secs.length; i++)
    		secs[i] = buffer[i + j];
    	j += secs.length;
    	
    	for(int i = 0; i < flags.length; i++)
    		flags[i] = buffer[i+j];
    	j += flags.length;
    	
    	for(int i = 0; i < clientIP.length; i++)
    		clientIP[i] = buffer[i+j];
    	j += clientIP.length;
    	
    	for(int i = 0; i < yourIP.length; i++)
    		yourIP[i] = buffer[i+j];
    	j += yourIP.length;
    	
    	for(int i = 0; i < serverIP.length; i++)
    		serverIP[i] = buffer[i+j];
    	j += serverIP.length;
    	
    	for(int i = 0; i < gateWayIP.length; i++)
    		gateWayIP[i] = buffer[i+j];
    	j += gateWayIP.length;
    	
    	for(int i = 0; i < clientHardWareAddress.length; i++)
    		clientHardWareAddress[i] = buffer[i+j];
    	j += clientHardWareAddress.length;
    	
    	for(int i = 0; i < serverHostName.length; i++)
    		serverHostName[i] = buffer[i+j];
    	j += serverHostName.length;
    	
    	for(int i = 0; i < bootFileName.length; i++)
    		bootFileName[i] = buffer[i+j];
    	j += bootFileName.length;
    	
    	for(int i = 0; i < magicCookie.length; i++)
    		magicCookie[i] = buffer[i+j];
    	j += magicCookie.length;
    		
    	if(j < buffer.length-1)
    	{
	    	int k = buffer.length - j;
	    	byte[] bufferedoptions = new byte[k];
	    	for(int i = 0; i < k; i++)
	    		bufferedoptions[i] = buffer[j+i];
	    	createOptions(bufferedoptions); 
    	}
    }
    
    /**
     * Values for option 53 (length 1)
     */
    public final static byte DHCPDISCOVER = 1;
    public final static byte DHCPOFFER = 	2;
    public final static byte DHCPREQUEST = 	3;
    public final static byte DHCPDECLINE = 	4;
    public final static byte DHCPACK = 		5;
    public final static byte DHCPNAK = 		6;
    public final static byte DHCPRELEASE = 	7;
    public final static byte DHCPINFORM = 	8;

    
    /**
     * @return the type of option 53 (length 1)
     */
    public byte getType()
    {
    	for(DHCPoption opt : options)
    		if(opt.getCode() == 53 && opt.getLength() == 1) return opt.getData()[0];
    	return 0;
    }
    
    /**
     * Create an option in this message that matches the representation in the given list of bytes
     * (if possible: matching length, ...)
     * @param 	Buffer (option in bytes)
     */
    private void createOptions(byte[] Buffer){
    	//1 byte geeft error bij checken buffer.length ==0, want outofbound
    	//2 bytes minimaal aantal bytes :: bv. 2(=code) 0(lengte)
    	if(Buffer[0] == (byte)255)
    	{
    		byte opCode = (byte) 255;
    		byte length= (byte) 0;
    		byte[] data = {0};
    		addOption(opCode, length, data);
    		return;
    	}
    	byte option = Buffer[0];
    	byte length = Buffer[1];
    	int k = length+2;
    	byte[] data = new byte[length];
    	for(int i=0; i<length; i++)
    		data[i] = Buffer[i+2];
    	addOption(option, length, data);
    	int l = Buffer.length - k;
    	byte[] topass = new byte[l];
    	for(int i=0; i<l; i++)
    		topass[i] = Buffer[k+i];
    	createOptions(topass);
    }
    
    /**
     * @return a liest of bytes that represent this message in the DHCP standards
     */
    public byte[] retrieveBytes()
    {
    	byte[] toReturn = new byte[getLength() + 1];
    	
    	toReturn[0] = opCode;
    	toReturn[1] = hardWareType;
    	toReturn[2] = hardWareAddressLength;;
    	toReturn[3] = hopCount;
    	
    	int j = 4;
    	
    	for(int i = 0; i < transactionID.length; i++)
    		toReturn[i + j] = transactionID[i];
    	j += transactionID.length;
    	
    	for(int i = 0; i < secs.length; i++)
    		toReturn[i + j] = secs[i];
    	j += secs.length;
    	
    	for(int i = 0; i < flags.length; i++)
    		toReturn[i + j] = flags[i];
    	j += flags.length;
    	
    	for(int i = 0; i < clientIP.length; i++)
    		toReturn[i + j] = clientIP[i];
    	j += clientIP.length;
    	
    	for(int i = 0; i < yourIP.length; i++)
    		toReturn[i + j] = yourIP[i];
    	j += yourIP.length;
    	
    	for(int i = 0; i < serverIP.length; i++)
    		toReturn[i + j] = serverIP[i];
    	j += serverIP.length;
    	
    	for(int i = 0; i < gateWayIP.length; i++)
    		toReturn[i + j] = gateWayIP[i];
    	j += gateWayIP.length;
    	
    	for(int i = 0; i < clientHardWareAddress.length; i++)
    		toReturn[i + j] = clientHardWareAddress[i];
    	j += clientHardWareAddress.length;
    	
    	for(int i = 0; i < serverHostName.length; i++)
    		toReturn[i + j] = serverHostName[i];
    	j += serverHostName.length;
    	
    	for(int i = 0; i < bootFileName.length; i++)
    		toReturn[i + j] = bootFileName[i];
    	j += bootFileName.length;
    	
    	for(int i = 0; i < magicCookie.length; i++)
    		toReturn[i + j] = magicCookie[i];
    	j += magicCookie.length;
    	
    	for(int i = 0; i < options.size(); i++){
    		System.arraycopy(options.get(i).getBytes(), 0, toReturn, j, options.get(i).getTotalLength());
    		j += options.get(i).getTotalLength();
    	}

    	return toReturn;	
    }
    
    /**
     * @return the length of this message in bytes conform the DHCP standards
     */
    public int getLength()
    {
    	return MINLENGTH + getOptionsLength();
    }
    
    /**
     * @return the length of this message's options in bytes conform the DHCP standards
     */
	private int getOptionsLength() {
		int length = 0;
		for(int i = 0; i < options.size(); i++){
    		length += options.get(i).getTotalLength();
    	}
		return length;
	}
	
	/**
	 * return a string representing this message for human readability
	 */
	public String toString()
	{
		String toStringOptions = "\n";
		
		for(DHCPoption opt : options)
			toStringOptions += opt.toString() +"\n";
		
		return "OpCode: " + Utility.unsignedByte(opCode) + "\nHardwaretype: " + Utility.unsignedByte(hardWareType) 
				+ "\nHardwareaddresslength: "	+  Utility.unsignedByte(hardWareAddressLength) + "\nHopcount: " + Utility.unsignedByte(hopCount) 
				+"\nTransaction ID:  " + Arrays.toString(Utility.unsignedBytes(transactionID)) + "\nSecs: " +  Arrays.toString(Utility.unsignedBytes(secs)) + "\nFlags: " +  Arrays.toString(Utility.unsignedBytes(flags)) 
				+ "\nClient IP: " +  Arrays.toString(Utility.unsignedBytes(clientIP)) + "\nYour IP: " +  Arrays.toString(Utility.unsignedBytes(yourIP))+ "\nServer IP: " +  Arrays.toString(Utility.unsignedBytes(serverIP))
				+ "\nGateway IP: " +  Arrays.toString(Utility.unsignedBytes(gateWayIP)) + "\nClienthardwareaddress: " +  Arrays.toString(Utility.unsignedBytes(clientHardWareAddress))
				+ "\nServerhostname: "  +  Arrays.toString(Utility.unsignedBytes(serverHostName)) + "\nBootfilename :" +  Arrays.toString(Utility.unsignedBytes(bootFileName))
				+ "\nMagiccookie: " +  Arrays.toString(Utility.unsignedBytes(magicCookie)) 
				+ toStringOptions;	
	}
	
	/**
	 * Reset the options of this message
	 */
	public void resetoptions() {
		options.clear();
	}
	
	/**
	 * Return the value of the given option type (if this options is part of this message)
	 * @param 	opCode
	 * @return	Value of the option in a list of bytes
	 */
	public byte[] getOptionData(byte opCode)
	{
		for(DHCPoption opt : options)
		{
			if(opt.getCode() == opCode)
				return opt.getData();
		}
		return null;
	}
}
