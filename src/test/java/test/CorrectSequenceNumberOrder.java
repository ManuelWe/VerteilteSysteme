package test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;

public class CorrectSequenceNumberOrder {
	List<File> files = new ArrayList<File>();
	Scanner sc = null;

	@Before
	public void getFiles() {
		try {
			for (File file : new File("OutputFiles").listFiles())
				if (!file.isDirectory())
					files.add(file);
		} catch (NullPointerException e) {

		}
	}

	@Test
	public void rightOrder() {
		boolean output = true;
		for (int i = 0; i < files.size(); i++) {
			try {
				sc = new Scanner(files.get(i));
				String line = null;
				int fileLine = 0;

				while (sc.hasNextLine()) {
					line = sc.nextLine();
					String[] message = line.split(" ");
					int messageNumber = Integer.parseInt(message[0]);
					if (messageNumber == fileLine) {
						fileLine++;
					} else {
						output = false;
						break;
					}
				}

			} catch (FileNotFoundException e) {
			}
		}
		assertEquals(true, output);
	}
}
