package dhcp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class FileHandler {
	private File file;

	public FileHandler() {
		file = new File("server.txt");
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
	}

	// TODO only first entry enough?
	public String getFileEntry() {
		Scanner scanner = null;
		try {
			scanner = new Scanner(file);
		} catch (FileNotFoundException e) {
			return "127.0.0.1:2";
		}
		if (scanner.hasNextLine()) {
			scanner.close();
			return scanner.nextLine();
		} else {
			scanner.close();
			return "127.0.0.1:2";
		}
	}
}
