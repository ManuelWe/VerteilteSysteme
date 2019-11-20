package dhcp;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileHandler {
	private File file;
	private List<String> clientAddresses = new ArrayList<String>();
	private Socket socket = null;
	private DataOutputStream out;
	private String lastServerDown;

	public FileHandler() {
		super();
		file = new File("server.txt");
	}

	public void addClientAddress(String address) {
		if (!clientAddresses.contains(address)) {
			clientAddresses.add(address);
			try {
				out.writeUTF(address);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Connected Clients:" + Arrays.toString(clientAddresses.toArray()));
	}

	public void removeClientAddress(String address) {
		clientAddresses.remove(address);
		System.out.println("Connected Clients:" + Arrays.toString(clientAddresses.toArray()));
	}

	public void setServerAddress(String address) {
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Path file = Paths.get("server.txt");
		List<String> lines = Arrays.asList(address);

		try {
			Files.write(file, lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String ip = address.split(":")[0];
		int port = Integer.parseInt(address.split(":")[1]);
		try {
			socket = new Socket(ip, port);
		} catch (IOException u) {
			System.out.println("Trying local address!");
			try {
				// workaround to still be able to launch on laptop; not needed on raspberry
				socket = new Socket("127.0.0.1", port);
			} catch (IOException e) {
				System.out.println(e);
			}
		}
		try {
			out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void getHighestClientAddress() {
		String highestAddressString = "";
		int highestAddressInt = 0;
		for (String num : clientAddresses) {
			String cleanedAddress = num.replaceAll("[^0-9]", "");
			int i = Integer.parseInt(cleanedAddress);
			if (i > highestAddressInt) {
				highestAddressInt = i;
				highestAddressString = num;
			}
		}
		System.out.println("New Server should be " + highestAddressString);
	}

	public String electNewServer(String clientAddress, String serverAddress) {
		if (serverAddress.equals(lastServerDown)) {
			return getLastFileEntry();
		} else {
			lastServerDown = serverAddress;
			setServerAddress(clientAddress);
			removeClientAddress(clientAddress);
			return clientAddress;
		}
	}

	// TODO only first entry enough?
	public String getLastFileEntry() {
		RandomAccessFile fileHandler = null;
		try {
			fileHandler = new RandomAccessFile(file, "r");
			long fileLength = fileHandler.length() - 1;
			StringBuilder sb = new StringBuilder();

			for (long filePointer = fileLength; filePointer != -1; filePointer--) {
				fileHandler.seek(filePointer);
				int readByte = fileHandler.readByte();

				if (readByte == 0xA) {
					if (filePointer == fileLength) {
						continue;
					}
					break;

				} else if (readByte == 0xD) {
					if (filePointer == fileLength - 1) {
						continue;
					}
					break;
				}

				sb.append((char) readByte);
			}

			String lastLine = sb.reverse().toString();
			return lastLine;
		} catch (java.io.FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (java.io.IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (fileHandler != null)
				try {
					fileHandler.close();
				} catch (IOException e) {
					/* ignore */
				}
		}
	}
}
