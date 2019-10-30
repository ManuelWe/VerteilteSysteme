package eu.boxwork.dhbw.examples.webservice;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


@XmlRootElement
public class HelloToSend {
	
	private String name = null;
	private String[] listOfStates = {"Baden","Wuerttemberg"};
	private File file;
	
	public HelloToSend() {
		super();
		file = new File("OutputFile.txt");
	}
	public String getName() {
		return tail();
	}
	public void setName(String name) {
		this.name = name;

		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

		List<String> lines = Arrays.asList(name + " " + sdf.format(cal.getTime()));
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
	public String[] getListOfStates() {
		return listOfStates;
	}
	public void setListOfStates(String[] listOfStates) {
		this.listOfStates = listOfStates;
	}
	
	public String toString()
	{
		return "hello "+tail();
	}
	
	public String toHTML() {
		String ret = "<HTML><HEADER><TITLE>HTTP REST RESPONSE HELLO WORLD</TITLE></HEADER>";
			ret = ret + "<BODY><H1>HELLO: "+ tail() +"</H1></BODY>";
		ret = ret + "</HTML>";
		return ret;
	}

	public String tail() {
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
