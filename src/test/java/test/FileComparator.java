package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class FileComparator {
	List<File> files = new ArrayList<File>();

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
	public void filesEqual() {
		boolean output = true;
		for (int i = 0; i < files.size(); i++) {
			if (!(isEqual(files.get(0).toPath(), files.get(i).toPath()))) {
				fail(files.get(0).getName() + " " + files.get(i).getName());
				output = false;
			}
		}
		assertEquals(true, output);
	}

	private boolean isEqual(Path firstFile, Path secondFile) {
		try {
			if (Files.size(firstFile) != Files.size(secondFile)) {
				return false;
			}

			byte[] first = Files.readAllBytes(firstFile);
			byte[] second = Files.readAllBytes(secondFile);
			return Arrays.equals(first, second);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
