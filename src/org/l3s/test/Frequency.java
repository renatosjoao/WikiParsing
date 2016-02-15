package org.l3s.test;

import info.bliki.api.query.Parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class Frequency {

	
	public static void main(String[] args) throws IOException{
		
		
		freqDistribution("temp3", "temp3_out");
		
		System.exit(1);
		int lineCount = 0;
		Process p = null;
		try {
			p = Runtime.getRuntime().exec("wc -l " + "temp3");
			String s = null;
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((s = br.readLine()) != null)
				lineCount += Integer.parseInt(s.split(" ")[0]);
			p.waitFor();
			p.destroy();
		} catch (Exception e) {
		}
		
		
		
		
		FileReader fReader = new FileReader(new File("temp3"));
		BufferedReader bffReader = new BufferedReader(fReader);
		String inpLine = null;		
		//Map<String,Integer> frequency = new HashMap<String, Integer>();		
		while ((inpLine = bffReader.readLine()) != null) {
			String[] words = inpLine.split(";");
			if(words!=null){
			String mention;
			String entity;
			double  count = 0.0;
			mention = words[0].trim();
			entity = words[1].trim();
			count = Double.parseDouble(words[2].trim());
			
			double freq = count/lineCount;
			System.out.println(String.format( "%.5f", freq));
			//if((!key.isEmpty()) && (key !=null)){
			//Integer f = frequency.get(key);
			//	if(f==null){
			//		f=0;
			//	}
			//	frequency.put(key,f+1);
		//}
			}
		}
		
		bffReader.close();
		fReader.close();
		
		//System.out.println("Control point 2 ");
		
		
		//PrintWriter pWriter = new PrintWriter("articlesMentionsANDLinks_SORTED_Freq.txt","UTF-8");
		//FileReader fRead = new FileReader(new File("temp2"));
		//BufferedReader buffReader = new BufferedReader(fRead);
		//String inp = null;
		//while ((inp = buffReader.readLine()) != null) {
		//	
		//	String[] keys = inp.split(";");			
		//	String keyII = keys[0].trim();
		//	if((keyII!=null) && (!keyII.isEmpty())){
		//		Integer value = frequency.get(keyII);
		//		pWriter.println(inp + ";" +value);
		//	}
		//}
		
		//System.out.println("Control point 3 ");
		
		//buffReader.close();
		//fRead.close();
		//pWriter.flush();
		//pWriter.close();
		
		
	}
	
	
	
	public static void freqDistribution(String inputFile, String outputFile) throws NumberFormatException, IOException{
		int lineCount = 0;
		Process p = null;
		try {
			p = Runtime.getRuntime().exec("wc -l " + inputFile);
			String s = null;
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((s = br.readLine()) != null)
				lineCount += Integer.parseInt(s.split(" ")[0]);
			p.waitFor();
			p.destroy();
		} catch (Exception e) {
			}
	
		FileReader fReader = new FileReader(new File(inputFile));
		BufferedReader bffReader = new BufferedReader(fReader);
		PrintWriter pWriter = new PrintWriter(outputFile,"UTF-8");
		String inpLine = null;		
		//Map<String,Integer> frequency = new HashMap<String, Integer>();		
		while ((inpLine = bffReader.readLine()) != null) {
			String[] words = inpLine.split(";");
			if(words!=null){
				String mention  = null;
				String entity = null;
				double  count = 0.0;
				mention = words[0].trim();
				entity = words[1].trim();
				count = Double.parseDouble(words[2].trim());
				double freq = count/lineCount;
				pWriter.println(inpLine+";"+String.format( "%.5f", freq));
			}
		}
	
		pWriter.flush();
		pWriter.close();
		bffReader.close();
		fReader.close();
	}
}
