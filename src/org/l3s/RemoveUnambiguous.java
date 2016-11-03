package org.l3s;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.compress.compressors.CompressorException;

/**
 * @author Renato Stoffalette Joao
 * @mail renatosjoao@gmail.com
 * @version 1.0
 * @date 11.2016
 *
 */
public class RemoveUnambiguous {

	public RemoveUnambiguous() {
		super();
	}

	/**
	 *  This utility function will remove unambiguous mentions from the original mention/entity file (i.e. mention types with only one entity assigned)
	 *	and will create two mention/entity files (one file with the unambigous mentions only and one file without unambiguous mentions. )
	 *
	 *  Al Pacino; Al Pacino; 1.0
	 *
	 * @param mentionEntityFile   mentionEntityLinks_PRIOR_100_top5.bz2
	 * @throws CompressorException
	 * @throws IOException
	 */
	public void remove(String mentionEntityFile) throws CompressorException, IOException{ //mentionEntityLinks_PRIOR_topk
		Utils ut = new Utils();
		BufferedReader bffReader = ut.getBufferedReaderForCompressedFile(mentionEntityFile);
		String outputFile = mentionEntityFile.substring(0, mentionEntityFile.lastIndexOf('.'));
		TreeMap<String,LinkedList<String>> mentionMap1 = new TreeMap<>();
		PrintWriter writer = new PrintWriter(outputFile+"_NO_UNAMBIGUOUS", "UTF-8");
		PrintWriter writerS = new PrintWriter(outputFile+"_UNAMBIGUOUS", "UTF-8");
		String line = null;
		while ((line = bffReader.readLine()) != null) {
			String[] tempSplit = line.split(" ;-; ");
			String mention1 = tempSplit[0].trim();
			LinkedList<String> tempList1 = mentionMap1.get(mention1);
			if(tempList1 == null){
				tempList1 = new LinkedList<>();
				tempList1.add(line);
			}else{
				tempList1.add(line);
			}
			mentionMap1.put(mention1, tempList1);
		}
		bffReader.close();
		for(Map.Entry<String,LinkedList<String>> entry : mentionMap1.entrySet()) {
			LinkedList<String> valueEntities1 = entry.getValue();
			if(valueEntities1.size()>1){
				for(String s : valueEntities1){
					writer.println(s);
					writer.flush();
				}
			}else{
				writerS.println(valueEntities1.getFirst().toString());
				writerS.flush();
				continue;
			}
		}
		writer.flush();
		writer.close();
		writerS.flush();
		writerS.close();
		Compressor cp = new Compressor();
		cp.compressTxtBZ2(outputFile+"_NOSINGLE");
		cp.compressTxtBZ2(outputFile+"_SINGLETON");
	}

	/**
	 * Main entry point
	 *
	 * @param args
	 */
	public static void main(String[] args){
		if(args.length < 1){
			System.out.println(" Please give appropriate mention/entity file. (i.e. mentionEntityLinks_PRIOR_100_top5.bz2) ");
		}else{
			RemoveUnambiguous RUnamb = new RemoveUnambiguous();
			try {
				RUnamb.remove(args[0]);
			} catch (CompressorException | IOException e) {
				e.printStackTrace();
			}
		}
	}
}