package dhcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;

public class DhcpServer {

	public HttpServer server = null;

	public static void main(String[] args) throws IOException, InterruptedException {
		String ip = "";
		// get ip address of localhost
		try (final DatagramSocket socket = new DatagramSocket()) {
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			ip = socket.getLocalAddress().getHostAddress();
			socket.close();
		}
		String port = "8080";

		DhcpServer server = new DhcpServer();
		server.startServer(ip, port);
		System.out.println("Enter key to close server...");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		try {
			br.readLine();
		} catch (IOException e) {
			System.err.println(e.getLocalizedMessage());
		}
		server.stopServer();
	}

	public void startServer(String ip, String port) {
		String serverString = "http://" + ip + ":" + port + "/";

		try {
			server = HttpServerFactory.create(serverString);
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
		}
		server.start();
		System.out.println("started server at IP '" + ip + "' and port '" + port + "'.");
	}	
	
	public void stopServer() {
		System.out.print("closing server...");
		server.stop(0);
		System.out.print("done.");
		System.exit(0);
	}
}
