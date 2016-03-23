package org.l3s;

/**
 * @author Renato Stoffalette Joao
 * @mail renatosjoao@gmail.com
 * @version 1.0
 * @date 03.2016
 * 
 */
import java.io.IOException;

public class Compressor {

	public Compressor() {
	}

	/**
	 * Function to compress input file using Bzip2
	 *
	 * @param inputFile
	 * @throws IOException
	 */
	private void compressTxtBZ2(String inputFile) throws IOException {
		Process p = null;
		try {
			p = Runtime.getRuntime().exec("bzip2 " + inputFile);
			p.waitFor();
			p.destroy();
		} catch (Exception e) {
		}
	}
}