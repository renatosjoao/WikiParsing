package org.l3s.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class WriteCompressed {

	public static void main(String[] args) throws IOException {
		
		
		compressTextFile("pageTitles.txt");
		System.exit(1);
		
		
		StringBuffer textBuff = new StringBuffer();
		String text;
		String line;
		int n=0;
		byte[] buffer = new byte[1024];
		
		
		
		FileInputStream fis = new FileInputStream("original.txt");
		FileOutputStream fos = new FileOutputStream("compressed.gz");
		GZIPOutputStream gos = new GZIPOutputStream(fos);

		FileInputStream fis2 = new FileInputStream("compressed.gz");
		GZIPInputStream gis = new GZIPInputStream(fis2);
		FileOutputStream fos2 = new FileOutputStream("uncompressed.txt");
		
		int oneByte;
		while ((oneByte = fis.read()) != -1) {
			fos.write(oneByte);
		}
		fos.close();
		fis.close();
		
		
		FileOutputStream output = new FileOutputStream(new File("articlesTitles1.txt"));
		FileInputStream in = new FileInputStream("articlesTitles.txt_OLD");
		//Writer writer = new OutputStreamWriter(new GZIPOutputStream(output),"UTF-8");
		int len;
		while ((len = in.read(buffer)) > 0) {
			output.write(buffer, 0, len);
		}
		
	}
	
	private static void compressTextFile(String inputFile) throws IOException {
		long start = System.currentTimeMillis();
		int lineCount = 0;
		Process p = null;
		try {
			p = Runtime.getRuntime().exec("bzip2 " + inputFile);
			String s = null;
			//BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			//while ((s = br.readLine()) != null)
			//	lineCount += Integer.parseInt(s.split(" ")[0]);
			p.waitFor();
			p.destroy();
		} catch (Exception e) {
		}
	}

}
