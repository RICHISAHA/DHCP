package DHCPHelper;

import java.util.Arrays;
/**
 * This DHCPoption represent one option with an option code, length and value (data)
 */
public class DHCPoption {
	private byte Code;
	private byte Length;
	private byte[] Data = new byte[Length];
	
	/**
	 * Constructor
	 * @param Code		The option's code
	 * @param Length	The length of the data of the option
	 * @param Data		The value of the option
	 */
	DHCPoption(byte Code, byte Length, byte[] Data){
		this.Code = Code;
		this.Length = Length;
		this.Data = Data;
	}
	
	/**
	 * @return the total length this option will take in the message in bytes
	 */
	public byte getTotalLength(){
		return (byte) (this.Length + 2);
	}
	
	/**
	 * @return the length of the value of the option
	 */
	public byte getLength(){
		return this.Length;
	}
	
	/**
	 * @return the code of the option
	 */
	public byte getCode(){
		return (byte) this.Code;
	}
	
	/**
	 * @return the value (data) of the option
	 */
	public byte[] getData(){
		return this.Data;
	}
	
	/**
	 * @return the whole option as it will be represented in a DHCP-message
	 */
	public byte[] getBytes(){
		int K = 2 + Length;
		byte[] tosend = new byte[K];
		tosend[0] = Code;
		tosend[1] = Length;
		for(int i=0; i<Length; i++){
			tosend[i+2] = Data[i];
		}
		return tosend;
	}
	
	/**
	 * @return a string representing this option
	 * @override standard toString()
	 */
	public String toString()
	{
		return "Option :" + Arrays.toString(new String[]{
			Utility.unsignedByte(Code)+ "",
			Utility.unsignedByte(Length)+ "", 
			Arrays.toString(Utility.unsignedBytes(Data)) 
			});
	}
}
