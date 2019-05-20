//import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

import DHCPClient.DHCPClient;

public class Client {
	
	/**
	 * Initialize the client and run the clients run() method
	 * @param args
	 */
	public static void main(String[] args)
	{
		String serverIP = args[0];
		DHCPClient client = new DHCPClient(serverIP);
		client.run();
		

	}
}
