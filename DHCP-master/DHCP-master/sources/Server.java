//import java.io.*;
import DHCPServer.DHCPServer;

public class Server {
	public static void main(String[] args)
	{
		String serverIP = args[0];
		DHCPServer server = new DHCPServer(serverIP);
		server.run();
	}
}