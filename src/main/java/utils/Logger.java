package utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class Logger {
	private Calendar cal = Calendar.getInstance();
	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

	public void logError(String error) {
		String errorLog = error + " " + sdf.format(cal.getTime());
		writeToFile(errorLog);
	}

	private void writeToFile(String errorLog) {
		File file1 = new File("Error logs.txt");
		try {
			file1.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		List<String> lines = Arrays.asList(errorLog);
		Path file = Paths.get("OutputFile.txt");

		try {
			Files.write(file, lines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
		} catch (IOException e) {
			try {
				Files.write(file, lines, StandardCharsets.UTF_8);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
}
