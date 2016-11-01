package org.l3s;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.TreeMap;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/*
 * This is a masure from "An Effective Low cost measure of Semantic Relatedness obtained from Wikipedia"
 *
 *   relatedness(a,b) = \frac{ log( max(|A|,|B|) ) - log(|A \cap B|) } { log(|W|) - log(min(|A|,|B|)) }
 *
 *   a and b are the two articles of interest.
 *   A and B are the sets of all articles that link to a and b, respectively.
 *   W is the set of all articles in Wikipedia.
 */
/**
 * @author Renato Stoffalette Joao
 * @mail renatosjoao@gmail.com
 * @version 1.0
 * @date 10.2016
 *
 */

public class Relatedness {

	String entity_a;
	String entity_b;

	//useless main just for local testing
	public static void main(String[] args) throws IOException, ParseException, CompressorException{
		 Relatedness rt = new Relatedness();
		 rt.calculateRelatedness(args[0], args[1], args[2], args[3]);
	}

	/**
	 *
	 */
	public Relatedness() {
		super();
	}

	/**
	 *
	 * @param pageTitles
	 * @return
	 * @throws CompressorException
	 * @throws IOException
	 */
	public TreeMap<String,String> loadPageTitles(String pageTitles) throws CompressorException, IOException{
		TreeMap<String,String> pageTitlesMap = new TreeMap<>();   //map to store the Entities and entities page ids
		BufferedReader bfR1 = getBufferedReaderForCompressedFile(pageTitles);
		String l = bfR1.readLine();
		int x=0;
		while((l = bfR1.readLine()) != null ){
			x++;
			Charset.forName("UTF-8").encode(l);
			String temp[] = l.split(" \t ");
			String entity = temp[0].trim();
			String entityID = temp[1].trim();
			pageTitlesMap.put(entity, entityID);
		}
		bfR1.close();
		return pageTitlesMap;
	}

	/**
	 *
	 * @param fileIn
	 * @return
	 * @throws FileNotFoundException
	 * @throws CompressorException
	 */
	public BufferedReader getBufferedReaderForCompressedFile(String fileIn)	throws FileNotFoundException, CompressorException {
		FileInputStream fin = new FileInputStream(fileIn);
		BufferedInputStream bis = new BufferedInputStream(fin);
		CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
		BufferedReader br2 = new BufferedReader(new InputStreamReader(input));
		return br2;
	}

	/**
	 *
	 * @param inputFile
	 * @param pgTitles
	 * @param entityA
	 * @param entityB
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 * @throws CompressorException
	 */
	public double calculateRelatedness(String inputFile, String pgTitles, String entityA, String entityB) throws IOException, ParseException, CompressorException{
		JSONParser parser = new JSONParser();
		FileReader reader = new FileReader(inputFile);
		Object obj = parser.parse(reader);
		JSONArray array = (JSONArray) obj;
		boolean linksToA = false;
		boolean linksToB = false;
		int numLinksToA = 0;
		int numLinksToB = 0;
		int numLinksToAandB = 0;
		int numArticles = 0;
		ArrayList<String> listLinksToA = new ArrayList<String>();
		ArrayList<String> listLinksToB = new ArrayList<String>();
		ArrayList<String> listLinksToAandB = new ArrayList<String>();
		TreeMap<String,String> pgTitlesMap = loadPageTitles(pgTitles);
		String entityA_ID;
		String entityB_ID;
		entityA_ID = pgTitlesMap.get(entityA);
		entityB_ID = pgTitlesMap.get(entityB);
		System.out.println(entityA + " : " + entityA_ID);
		System.out.println(entityB + " : "+ entityB_ID);
		int jsonSize = array.size();
		for (int i = 0; i < jsonSize; i++) {
			numArticles++;
			JSONObject jobject = (JSONObject) array.get(i);
			String pageTitleID = (String) jobject.get("pageTitleID");
			ArrayList<String> Links = (ArrayList<String>) jobject.get("links");
			for(String link : Links){
				if(link.equalsIgnoreCase(entityA_ID)){
					linksToA = true;
					numLinksToA++;
					listLinksToA.add(pageTitleID);
				}
				if(link.equalsIgnoreCase(entityB_ID)){
					linksToB = true;
					numLinksToB++;
					listLinksToB.add(pageTitleID);
				}
			} //end of for(String link : Links){
			if((linksToA) && (linksToB)){
				numLinksToAandB++;
				listLinksToAandB.add(pageTitleID);
			}
			linksToA = false;
			linksToB = false;
		} //for (int i = 0; i < jsonSize; i++) {
		System.out.println(numLinksToA);
		System.out.println(numLinksToB);
		System.out.println(numLinksToAandB);
		System.out.println(numArticles);
		double relatedness =  ( Math.log10( Math.max(numLinksToA, numLinksToB)) - Math.log10(numLinksToAandB) ) /
				 ( Math.log10(numArticles) - Math.log10(  Math.min(numLinksToA, numLinksToB) ) );
		for(String s: listLinksToAandB){
			System.out.println(s);
		}
		return relatedness;
	}
}