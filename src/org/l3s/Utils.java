package org.l3s;

/**
 * @author Renato Stoffalette Joao
 * @mail renatosjoao@gmail.com
 * @version 1.0
 * @date 02.2016
 *
 */
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.l3s.Prior.PriorOnlyModel;
import org.l3s.statistics.Statistics;

import edu.jhu.nlp.wikipedia.PageCallbackHandler;
import edu.jhu.nlp.wikipedia.WikiPage;
import edu.jhu.nlp.wikipedia.WikiXMLParser;
import edu.jhu.nlp.wikipedia.WikiXMLParserFactory;

public class Utils {
	private static Pattern stylesPattern = Pattern.compile("\\{\\|.*?\\|\\}$", Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern refCleanupPattern = Pattern.compile("<ref>.*?</ref>", Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern commentsCleanupPattern = Pattern.compile("<!--.*?-->", Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern redirectPattern = Pattern.compile("#REDIRECT.*\\[\\[(.*?)\\]\\]",Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	private static Pattern mentionEntityPattern = Pattern.compile("\\[\\[(.*?)\\]\\]", Pattern.MULTILINE);
	private static Map<String, String> pageTitlesMap = new TreeMap<String, String>();
	private static Map<String, Integer> duplicatePageTitle = new TreeMap<String, Integer>();
	private static List<String> pagesTitlesList = new LinkedList<String>(); // This is a list of pages titles without special pages  (i.e. Category:, File:, Image:,etc) and without #REDIRECT
	private static List<String> allPagesTitlesList = new LinkedList<String>(); // This is a list with all  the pages titles.
	private static List<String> specialPagesTitlesList = new LinkedList<String>(); // This is a list with ONLY the SPECIAL pages titles. (i.e. Category:, File:, Image:, etc)
	private static List<String> redirectPagesTitlesList = new LinkedList<String>(); // This is a list with ONLY the pages titles with redirection. (i.e. #REDIRECT)
	private static int ENTITYPAGE = 0; // This is the total number of page titles, without ##REDIRECT
	private static int TOTAL_PAGE_TITLE = 0; // This is the total number of page titles, no matter if it is REDIRECT, SPECIAL, etc
	private static int REDIRECTION = 0; // This is the total number of #REDIRECT  page titles.
	private static int SPECIAL_PAGES = 0;
	private static int EMPTY_TITLE_PAGES;
	private static int IN_TITLES_LIST = 0;
	private static int DISAMBIGUATION_PAGE = 0;
	private static int MENTION_ENTITY = 0;
	private static int NOMATCH = 0;
	private static int MATCH = 0;
	private static int mentionEntityPairs = 0;

	/***
	 * A function to merge to input files, sort the lines and write to output file
	 *
	 * @param inputFile1
	 * @param inputFile2
	 * @param outputFile
	 * @throws IOException
	 */
	public void mergeFilesAndSort(String inputFile1, String inputFile2, String outputFile) throws IOException {
		ArrayList<String> finalList = new ArrayList<String>();
		BufferedReader bffReader = new BufferedReader(new FileReader(inputFile1));
		String inLine = null;
		while ((inLine = bffReader.readLine()) != null) {
			finalList.add(inLine);
		}
		bffReader.close();
		bffReader = new BufferedReader(new FileReader(inputFile2));
		while ((inLine = bffReader.readLine()) != null) {
			finalList.add(inLine);
		}
		File f = new File(inputFile1);
		f.delete();
		f = new File(inputFile2);
		f.delete();
		Collections.sort(finalList);
		PrintWriter outputFileWriter = new PrintWriter(new File(outputFile));
		for (String str : finalList) {
			outputFileWriter.println(str);
		}
		outputFileWriter.flush();
		outputFileWriter.close();
	}

	/**
	 *
	 *
	 * @param pageTitlesMap2
	 * @param value
	 * @return
	 */
	public String containsValue(Map<String, List<String>> pageTitlesMap2, String value) {
		Iterator<?> it = pageTitlesMap2.entrySet().iterator();
		while (it.hasNext()) {
			@SuppressWarnings("rawtypes")
			Map.Entry pair = (Map.Entry) it.next();
			@SuppressWarnings("unchecked")
			List<String> lTemp = (ArrayList<String>) pair.getValue();
			for (String str : lTemp) {
				if (str.equalsIgnoreCase(value)) {
					return (String) pair.getKey();
				}
			}
		}
		return null;
	}

	/**
	 * This is a utility function to write only the top prior probability for each mention.
	 *
	 * Do not forget that currently I wrote TOP prior probability for mentions
	 * that occurred at least 10 times.
	 *
	 * @param inputFile
	 * @param outputFile
	 * @throws IOException
	 * @throws CompressorException
	 */
	public void mentionEntityDisamb(String inputFile, String outputFile) throws IOException, CompressorException {
		BufferedReader buffReader1 = getBufferedReaderForCompressedFile(inputFile);
		TreeMap<String, HashMap<String, Double>> disambiguation = new TreeMap<>();
		PrintWriter Pwriter = new PrintWriter(outputFile, "UTF-8");
		String line = null;
		while ((line = buffReader1.readLine()) != null) {
			String[] elem = line.split(" ;-; ");
			String currentMention = elem[0];
			String currentEntity = elem[1];
			Double currentPrior = Double.parseDouble(elem[2]);
			HashMap<String, Double> tmpObj = disambiguation.get(elem[0]);
			if (tmpObj == null) {
				tmpObj = new HashMap<>();
				tmpObj.put(currentMention, currentPrior);
				disambiguation.put(currentMention, tmpObj);
			} else {
				Collection<Double> priors = tmpObj.values();
				Object ob = priors.toArray()[0];
				ob.toString();
				if (currentPrior >= Double.parseDouble(ob.toString())) {
					tmpObj = new HashMap<>();
					tmpObj.put(currentEntity, currentPrior);
					disambiguation.put(currentMention, tmpObj);
				}
			}
		}
		buffReader1.close();
		Iterator<?> it = disambiguation.entrySet().iterator();
		while (it.hasNext()) {
			@SuppressWarnings("rawtypes")
			Map.Entry pair = (Map.Entry) it.next();
			@SuppressWarnings("unchecked")
			HashMap<String, Double> entityObj = (HashMap<String, Double>) pair.getValue();
			Set<String> keys = entityObj.keySet();
			Pwriter.println(pair.getKey() + " ;-; " + keys.toArray()[0].toString() + " ;");
			it.remove();
		}
		Pwriter.flush();
		Pwriter.close();
		Compressor cp = new Compressor();
		cp.compressTxtBZ2(outputFile);
	}

	/**
	 * This function compares the mention/entity pairs top K changes  !!!
	 *
	 *
	 * @param inputFile1 			mentionEntity file 1
	 * @param pageTitles1 			page titles file1
	 * @param inputFile2			mentionEntity file 2
	 * @param pageTitles2			page titles file2
	 * @throws IOException
	 * @throws CompressorException
	 */
	public void compareTopK(String inputFile1, String pageTitles1, String inputFile2, String pageTitles2) throws IOException, CompressorException{
		TreeMap<String,LinkedList<String>> mentionMap1 = new TreeMap<>(); //map to store a mention and list of top5 disambiguations
		TreeMap<String,LinkedList<String>> mentionMap2 = new TreeMap<>(); //map to store a mention and list of top5 disambiguations

		int x=0 , y=0, z=0, m=0;

		TreeMap<String,String> pageTitlesMap1 = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		TreeMap<String,String> pageTitlesMap2 = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		//Here I am only reading the page titles list and adding to the pageTitlesMap1 that contains the entity and the entityID
		//BufferedReader bfR1 = new BufferedReader(new FileReader(new File(pageTitles1)));// getBufferedReaderForCompressedFile(pageTitles1);
		BufferedReader bfR1 = getBufferedReaderForCompressedFile(pageTitles1);
		String l = bfR1.readLine();
		while((l = bfR1.readLine()) != null ){
			Charset.forName("UTF-8").encode(l);
			x++;
			String temp[] = l.split(" \t ");
			String entity = temp[0].trim();
			String entityID = temp[1].trim();
			pageTitlesMap1.put(entity, entityID);
		}
		bfR1.close();
		//System.out.println("Number of page titles from pageTitles file "+pageTitles1+" : "+x);

		//Here I am only reading the page titles list and adding to the pageTitlesMap2 that contains the entity and the entityID
		//BufferedReader bfR2 = new BufferedReader(new FileReader(new File(pageTitles2)));// getBufferedReaderForCompressedFile(pageTitles2);
		BufferedReader bfR2 = getBufferedReaderForCompressedFile(pageTitles2);
		String l2 = bfR2.readLine();
		while((l2 = bfR2.readLine()) != null ){
			Charset.forName("UTF-8").encode(l2);
			y++;
			String temp[] = l2.split(" \t ");
			String entity = temp[0].trim();
			String entityID = temp[1].trim();
			pageTitlesMap2.put(entity, entityID);
		}
		bfR2.close();
		//System.out.println("Number of page titles from pageTitles file "+pageTitles2+" : "+y);

		int newMention = 0 ;
		int top5EntityChange = 0;
		//int noz=0;
		int entityDoesNotExist=0;
		int linefile1 =0;
		LinkedList<Integer> topKChanged = new LinkedList<>();
		//reading the first file and populating the first MAP

		//BufferedReader buffReader1 = new BufferedReader(new FileReader(new File(inputFile1)));// getBufferedReaderForCompressedFile(inputFile1);
		BufferedReader buffReader1 = getBufferedReaderForCompressedFile(inputFile1);
		String line1 = null;

		while ((line1 = buffReader1.readLine()) != null) {
			linefile1++;
			Charset.forName("UTF-8").encode(line1);
			String[] tempSplit = line1.split(" ;-; ");
			String mention1 = tempSplit[0].trim();
			String entity1 = tempSplit[1].trim();
			String prior1  = tempSplit[2].trim();

			String EID1 = pageTitlesMap1.get(entity1);

			if((EID1==null) || (EID1=="")){
				// This case the pageTitles list does not have the page ID therefore I am not interested.
				//System.out.println(mention1 + " ===: " + entity1);
				entityDoesNotExist++;
				continue;
			}else{

			LinkedList<String> tempList1 = mentionMap1.get(mention1);

			if(tempList1 == null){
				z++;
				tempList1 = new LinkedList<>();
				tempList1.add(entity1+" :=: "+prior1);
				mentionMap1.put(mention1, tempList1);
			}else{
				//noz++;
				///System.out.println(entity);
				//tempList.add(entity+" : "+prior);
				//tempList1.add(entity1+" :=: "+prior1);
				//mentionMap1.put(mention1, tempList1);
			}
			}
		}

		buffReader1.close();
		//System.out.println(" Number of lines file 1 : "+linefile1);
		System.out.println("Number of different mentions in Map 1 "+z);
		//System.exit(1);

		entityDoesNotExist = 0;
		//reading the second file and populating the second MAP
		//BufferedReader buffReader2 = new BufferedReader(new FileReader(new File(inputFile2)));// getBufferedReaderForCompressedFile(inputFile1);
		BufferedReader buffReader2 = getBufferedReaderForCompressedFile(inputFile2);
		String line2 = null;

		while ((line2 = buffReader2.readLine()) != null) {
			Charset.forName("UTF-8").encode(line2);
			String[] tempSplit = line2.split(" ;-; ");
			String mention2 = tempSplit[0].trim();
			String entity2 = tempSplit[1].trim();
			String prior2  = tempSplit[2].trim();
			String EID2 = pageTitlesMap2.get(entity2);
			if((EID2==null) || (EID2 == "") ){ // This case the pageTitles list does not have the page ID
				//System.out.println(mention2 + " ===: " + entity2);
				entityDoesNotExist++;
				continue;
			}else{
				y++;
			//checking whether the Map has the mention from the second list
			LinkedList<String> tempList2 = mentionMap2.get(mention2);
			if(tempList2 == null){
				m++;
				tempList2 = new LinkedList<>();
				tempList2.add(entity2+" :=: "+prior2);
				mentionMap2.put(mention2, tempList2);
			}else{
				//tempList2.add(entity2+" :=: "+prior2);
				//mentionMap2.put(mention2, tempList2);
				//tempList.add(entity+" : "+prior);
			}
			}
		}
		buffReader2.close();
		System.out.println("Number of different mentions from Map 2 "+m);



		//TOP 1 ***********************************************//
		//Let us iterate over the Map1 !
//		//*****************************************************//
		PrintWriter Pwriter = new PrintWriter("./resource/comparison_Results_NOTchanged_top1", "UTF-8");
		PrintWriter Pwriter2 = new PrintWriter("./resource/comparison_Results_changed_top1", "UTF-8");
		LinkedList<Double> priorRatesNOTChanged = new LinkedList<>();
		LinkedList<Double> priorRatesChanged = new LinkedList<>();
		int mentionOut = 0;
		int mentionIn = 0;
		for(Map.Entry<String,LinkedList<String>> entry : mentionMap1.entrySet()) {
		 //while there is an entry in the map created from the first Wikipedia dump
		  String keyMention1 = entry.getKey();
		  LinkedList<String> valueEntities1 = entry.getValue();

		  String keyMention2 = keyMention1;
		  LinkedList<String> valueEntities2 = mentionMap2.get(keyMention2);
		  if(valueEntities2 != null) { // It means that the mention exists in both map files.
			  mentionIn++; // The current mention(from the second list) is present in the first list treeMapME1

			  String entityFromList1 = valueEntities1.get(0).split(" :=: ")[0].trim();
			  String EntID1 = pageTitlesMap1.get(entityFromList1).trim();

			  String entityFromList2 = valueEntities2.get(0).split(" :=: ")[0].trim();
			  String EntID2 = pageTitlesMap2.get(entityFromList2).trim();

				if ( (entityFromList1.equalsIgnoreCase(entityFromList2)) || (EntID1.equalsIgnoreCase(EntID2)) ){ //The disambiguation entity has not changed. We are happy !!!
					double prior_before = Double.parseDouble(valueEntities1.get(0).split(" :=: ")[1].trim());
					double prior_after = Double.parseDouble(valueEntities2.get(0).split(" :=: ")[1].trim());
					Pwriter.println("MENTION="+keyMention1+ "\tE_BEF="+valueEntities1.get(0)+"\t"+"E_ID:" + EntID1 +"\tE_AFTER="+valueEntities2.get(0)+"\t"+"E_ID:" + EntID2 + "\tMAD:"+Math.abs((prior_before-prior_after)/2));
					priorRatesNOTChanged.add(prior_before/prior_after);
				}else{ // The entity has  REEEEEALLY changed
					//entityChanged++;
					double prior_before = Double.parseDouble(valueEntities1.get(0).split(" :=: ")[1].trim());
					double prior_after = Double.parseDouble(valueEntities2.get(0).split(" :=: ")[1].trim());
					Pwriter2.println("MENTION="+keyMention1+ "\tE_BEF="+valueEntities1.get(0)+"\t"+"E_ID:" + EntID1 +"\tE_AFTER="+valueEntities2.get(0)+"\t"+"E_ID:" + EntID2 + "\tMAD:"+Math.abs((prior_before-prior_after)/2));
					priorRatesChanged.add(prior_before/prior_after);
				}
			}else{
				mentionOut++; 	// The current mention from the second list is NOT present in the first list ( treeMapME1 )
								// This is the case where the mention/entity does not exist in the first list. Therefore a new mention/entity pair was created in the second list.
				//System.out.println("mention : " +current_mention);
			}
			} // for end !
		Pwriter.flush();
		Pwriter.close();
		Pwriter2.flush();
		Pwriter2.close();

		System.out.println("Number of mentions in common : "+mentionIn);
		//System.out.println("Number of mentions out "+mentionOut);

		//*****************************************************//

		//TOP 5 ***********************************************//
		//*****************************************************//
		entityDoesNotExist = 0;
		mentionMap1 = new TreeMap<>();
		newMention = 0;
		//buffReader1 = new BufferedReader(new FileReader(new File(inputFile1)));// getBufferedReaderForCompressedFile(inputFile1);
		buffReader1 =  getBufferedReaderForCompressedFile(inputFile1);
		line1 = null;
		while ((line1 = buffReader1.readLine()) != null) {
			Charset.forName("UTF-8").encode(line1);
			String[] tempSplit = line1.split(" ;-; ");
			String mention1 = tempSplit[0].trim();
			String entity1 = tempSplit[1].trim();
			String prior1  = tempSplit[2].trim();
			String EID1 = pageTitlesMap1.get(entity1);
			if((EID1==null) || (EID1=="")){
				entityDoesNotExist++;
				continue;
			}else{
			LinkedList<String> tempList1 = mentionMap1.get(mention1);
			if(tempList1 == null){
				newMention++;
				tempList1 = new LinkedList<>();
				tempList1.add(entity1+" :=: "+prior1);
			}else{
				tempList1.add(entity1+" :=: "+prior1);
			}
			mentionMap1.put(mention1, tempList1);
			}
		}
		buffReader1.close();
		//System.out.println("Number of different mentions in Map 1 (top5) :"+newMention);

		entityDoesNotExist=0;
		newMention=0;
		mentionMap2 = new TreeMap<>();
		//reading the second file and populating the second MAP
		//buffReader2 = new BufferedReader(new FileReader(new File(inputFile2)));// getBufferedReaderForCompressedFile(inputFile1);
		buffReader2 = getBufferedReaderForCompressedFile(inputFile2);
		line2 = null;
		while ((line2 = buffReader2.readLine()) != null) {
			Charset.forName("UTF-8").encode(line2);
			String[] tempSplit = line2.split(" ;-; ");
			String mention2 = tempSplit[0].trim();
			String entity2 = tempSplit[1].trim();
			String prior2  = tempSplit[2].trim();
			String EID2 = pageTitlesMap2.get(entity2);
			if((EID2==null) || (EID2 == "") ){ // This case the pageTitles list does not have the page ID
				entityDoesNotExist++;
				continue;
			}else{
			LinkedList<String> tempList2 = mentionMap2.get(mention2);
			if(tempList2 == null){
				newMention++;
				tempList2 = new LinkedList<>();
				tempList2.add(entity2+" :=: "+prior2);
			}else{
				tempList2.add(entity2+" :=: "+prior2);
			}
			mentionMap2.put(mention2, tempList2);
			}
		}
		buffReader2.close();
		//System.out.println("Number of different mentions in Map 2 (top5) :"+newMention);

		PrintWriter Pwriter3 = new PrintWriter("./resource/comparison_Results_NOTchanged_top5", "UTF-8");
		//PrintWriter Pwriter4= new PrintWriter("./resource/comparison_Results_changed_top5", "UTF-8");
		PrintWriter Pwriter5 = new PrintWriter("./resource/comparison_Results_changed_0", "UTF-8");
		PrintWriter Pwriter6 = new PrintWriter("./resource/comparison_Results_changed_5", "UTF-8");

		/// running comparisons from map1 mentions
		int commonMention=0;
		int notCommonMention=0;
		int numElemMap1=0;

		//while there is an entry in the map created from the first Wikipedia dump
		for(Map.Entry<String,LinkedList<String>> entry : mentionMap1.entrySet()) {
			numElemMap1++;

			top5EntityChange = 0;
			String keyMention1 = entry.getKey();

			//getting the list of entities for Mention1
			LinkedList<String> listEntities1 = (LinkedList<String>) mentionMap1.get(keyMention1);
			int list1Size = listEntities1.size();
			String keyMention2 = keyMention1;

			boolean gotIt = false;
			// Considering the mention from Map1 is also present in Map2
			if(mentionMap2.containsKey(keyMention2)){

				commonMention++;
				// getting the list of entities from Mention 2
				LinkedList<String> listEntities2 = (LinkedList<String>) mentionMap2.get(keyMention2);

				int list2Size = listEntities2.size(); // getting the size of the list of entities in the map entry for the mention key1=key2
				int counter = 0;
				for (int i=0; i < list1Size; i++){ //***//
					String entityANDprior1 = listEntities1.get(i);
					String entity1only = entityANDprior1.split(" :=: ")[0].trim();
					double prior1only = Double.parseDouble(entityANDprior1.split(" :=: ")[1].trim());

					for(int j=0; j<list2Size; j++){
					//for(String entityANDprior2: listEntities2){
						String entityANDprior2 = listEntities2.get(j);
						String entity2only = entityANDprior2.split(" :=: ")[0].trim();
						double prior2only = Double.parseDouble(entityANDprior2.split(" :=: ")[1].trim());
						String EID1 = pageTitlesMap1.get(entity1only).trim();
						String EID2 = pageTitlesMap2.get(entity2only).trim();

						if( (entity2only.equalsIgnoreCase(entity1only) ) || (EID1.equalsIgnoreCase(EID2))  ){
							gotIt = true; //this means the second map has the entity from the first map. The entity names have not changed.
							Pwriter3.println("MENTION="+keyMention1+"\tE_BEF="+entity1only+":"+prior1only+"\t"+"E_ID:" + pageTitlesMap1.get(entity1only)+"\tPOS="+(i+1)+"\tE_AFTER ="+entity2only+":"+prior2only+"\t"+"E_ID:" + pageTitlesMap2.get(entity2only) + "\tPOS="+(j+1)+"\tRATIO:"+Math.abs((prior1only-prior2only)/2));
							break;
						}else {
								//		Pwriter1.println("MENTION = "+key1+ " \t E_BEF = "+entity1only+":"+prior1only+" \t "+"E_ID:" + pageTitlesMap1.get(entity1only) +" \t E_AFTER = "+entity2only+":"+prior2only+" \t "+"E_ID:" + pageTitlesMap2.get(entity2only) + " \t RATIO:"+prior1only/prior2only);
								//		break;
										gotIt = false;
										//Pwriter2.println("MENTION = "+key1+ " \t E_BEF = "+entity1only+":"+prior1only+" \t "+"E_ID:" + pageTitlesMap1.get(entity1only) +" \t E_AFTER = "+entity2only+":"+prior2only+" \t "+"E_ID:" + pageTitlesMap2.get(entity2only) + " \t RATIO:"+prior1only/prior2only);
										continue;
									}
						}
					if(!gotIt){
						top5EntityChange++;
						//try{
							//String entity2andprior = listEntities2.get(counter);
						//	String entity2only = "";
						//	double prior2only = 0.0;
						//	entity2only = listEntities2.get(counter).split(" : ")[0].trim();
						//	prior2only = Double.parseDouble(listEntities2.get(counter).split(" : ")[1].trim());
						//	if(counter == 0 ){
//							Pwriter4.println("MENTION="++ " \t E_BEF = "+entity1only+":"+prior1only+" \t "+"E_ID:" + pageTitlesMap1.get(entity1only) +" \t E_AFTER = "+entity2only+":"+prior2only+ " \t "+"E_ID:" + pageTitlesMap2.get(entity2only) +" \t RATIO:"+prior2only/prior1only);
							//Pwriter4.println("MENTION="+keyMention1+"\tE_BEF="+entity1only+":"+prior1only+"\t"+"E_ID:" + pageTitlesMap1.get(entity1only)+"\tE_AFTER ="+entity2only+":"+prior2only+"\t"+"E_ID:" + pageTitlesMap2.get(entity2only) + "\tRATIO:"+prior1only/prior2only);
						//	}

						//	Pwriter2.println("MENTION = "+key1+ " \t E_BEF = "+entity1only+":"+prior1only+" \t "+"E_ID:" + pageTitlesMap1.get(entity1only) +" \t E_AFTER = "+entity2only+":"+prior2only+ " \t "+"E_ID:" + pageTitlesMap2.get(entity2only) + " \t RATIO:"+prior2only/prior1only);
						//	counter++;
					//		this means the second map does not have the entity from the first map. Therefore top5 has changed.
						//	continue;
						//}catch(Exception e){

						//	Pwriter2.println("MENTION = "+key1+ " \t E_BEF = "+entity1only+":"+prior1only+" \t "+"E_ID:" + pageTitlesMap1.get(entity1only) +" \t E_AFTER = "+"NIL"+":"+"NIL"+ " \t "+"E_ID:" + "NIL" + " \t RATIO:NIL");
						//	counter++;
							//	continue;
							}
						}//***//

				}else{
					notCommonMention++;
					continue;
				// The mention from Map1 is NOT present in Map2
				}
			topKChanged.add(top5EntityChange);
			if(top5EntityChange == 0){
				Pwriter5.println(keyMention1);
			}
			if(top5EntityChange == 5){
				Pwriter6.println(keyMention1);
			}

		} // no more entries in the map created from the first wikipedia dump.
		int numElemMap2=0;
		for(Map.Entry<String,LinkedList<String>> entry : mentionMap2.entrySet()) {
			numElemMap2++;
		}
		System.out.println("Number of common mentions top5  :"+commonMention);
		System.out.println("Number of NOT common mentions top5 :"+notCommonMention);
		Pwriter3.flush();
		Pwriter3.close();
		//Pwriter4.flush();
		//Pwriter4.close();
		Pwriter5.flush();
		Pwriter5.close();
		Pwriter6.flush();
		Pwriter6.close();

		PrintWriter pwrt = new PrintWriter(new File("./resource/top5EntityChanges"));
		for(Integer elem: topKChanged){
			pwrt.println(elem);
		}
		pwrt.flush();
		pwrt.close();

	}

	/**
	 * * In this method I am aiming to compare two mention/entity disambiguation files only for top disambiguated entity.
	 * This is meant to compare mention/entity disambiguation files side by side from different dumps.
	 *
	 * Initially I am only interested in the mention/entities that changed from the first year to the second year
	 * I am also performing a verification whether the entity changed the name by comparing the entity page ID.
	 *
	 * @param inputFile1
	 * @param pageTitles1
	 * @param inputFile2
	 * @param pageTitles2
	 * @throws CompressorException
	 * @throws IOException
	 */
	public void compareDisambiguations(String inputFile1, String pageTitles1, String inputFile2, String pageTitles2) throws CompressorException, IOException{
		long start = System.currentTimeMillis();
		HashMap<String,String> pageTitlesMap1 = new HashMap<>();
		HashMap<String,String> pageTitlesMap2 = new HashMap<>();

		//Here I am only reading the page titles list and adding to the pageTitlesMap1 that contains the entity and the entityID
		BufferedReader bfR1 = new BufferedReader(new FileReader(new File(pageTitles1)));// getBufferedReaderForCompressedFile(inputFile1);
		String l = bfR1.readLine();
		while((l = bfR1.readLine()) != null ){
			String temp[] = l.split(" \t ");
			String entity = temp[0].trim();
			String entityID = temp[1].trim();
			pageTitlesMap1.put(entity, entityID);
		}
		bfR1.close();

		//Here I am only reading the page titles list and adding to the pageTitlesMap2 that contains the entity and the entityID
		BufferedReader bfR2 = new BufferedReader(new FileReader(new File(pageTitles2)));// getBufferedReaderForCompressedFile(inputFile2);
		String l2 = bfR2.readLine();
		while((l2 = bfR2.readLine()) != null ){
			String temp[] = l2.split(" \t ");
			String entity = temp[0].trim();
			String entityID = temp[1].trim();
			pageTitlesMap2.put(entity, entityID);
		}
		bfR2.close();

		LinkedList<Double> priorRatesNOTChanged = new LinkedList<>();
		LinkedList<Double> priorRatesChanged = new LinkedList<>();

		TreeMap<String,String> treeMapME1 = new TreeMap<>(); //This map here is meant to store mention/entity pairs from the first file
		PrintWriter Pwriter2 = new PrintWriter("./resource/comparison_Results_changed", "UTF-8");
		PrintWriter Pwriter = new PrintWriter("./resource/comparison_Results_NOTchanged", "UTF-8");

		int lineCountFile1 = 0;
		int lineCountFile2 = 0;
		int mentionIn = 0;
		int entityNotChanged = 0;
		int mentionOut = 0;
		int entityChanged = 0;
		//reading the first file and adding to the map
		BufferedReader buffReader1 = new BufferedReader(new FileReader(new File(inputFile1)));// getBufferedReaderForCompressedFile(inputFile1);
		String line1 = null;
		while ((line1 = buffReader1.readLine()) != null) {
			String[] tempSplit = line1.split(" ;-; ");
			String mention = tempSplit[0].trim();
			String entity = tempSplit[1].trim();
			String prior  = tempSplit[2].trim();
			String EntID1 = pageTitlesMap1.get(entity);
			if(EntID1 != null){
				lineCountFile1++;
				treeMapME1.put(mention, entity+" :=: "+prior);
			}else{
				//System.out.println(entity);
			}
		}
		buffReader1.close();

		//reading the second file to check
		int i=0;
		String line2 = null;
		BufferedReader buffReader2 = new BufferedReader(new FileReader(new File(inputFile2)));//getBufferedReaderForCompressedFile(inputFile2);
		while ((line2 = buffReader2.readLine()) != null) { // :)
			String[] tempSplit = line2.split(" ;-; ");
			String current_mention = tempSplit[0].trim();
			String current_entity = tempSplit[1].trim();
			String current_prior  = tempSplit[2].trim();
			String EntID2 = pageTitlesMap2.get(current_entity);
			//System.out.println(current_entity + " : " + EntID2);
			if(EntID2!=null){
				lineCountFile2++;
			//let's check if the mention has changed
			String valueFromMap1 = treeMapME1.get(current_mention);

			if(valueFromMap1 != null) {
				String entityFromList1 = valueFromMap1.split(" :=: ")[0].trim();

				String EntID1 = pageTitlesMap1.get(entityFromList1);
				EntID2 = pageTitlesMap2.get(current_entity).trim();
				//System.out.println("E1:"+entityFromList1 +" ID:"+pageTitlesMap1.get(entityFromList1)+ " E2:"+current_entity +" ID:"+pageTitlesMap2.get(current_entity));
				mentionIn++; // The current mention(from the second list) is present in the first list treeMapME1

				if (current_entity.equalsIgnoreCase(entityFromList1)){ //The disambiguation entity has not changed. We are happy !!!
					entityNotChanged++;
					double prior_before = Double.parseDouble(valueFromMap1.split(" :=: ")[1].trim());
					double prior_after = Double.parseDouble(current_prior.trim());
					Pwriter.println("MENTION = "+current_mention+ " \t E_BEF = "+valueFromMap1.split(" :=: ")[0].trim()+":"+prior_before+" \t "+"E_ID:" + pageTitlesMap1.get(entityFromList1) +" \t E_AFTER = "+current_entity+":"+prior_after+" \t "+"E_ID:" + pageTitlesMap2.get(current_entity) + " \t RATIO:"+prior_before/prior_after);
					priorRatesNOTChanged.add(prior_before/prior_after);
					//The disambiguation entity has changed.  Has it changed to another entity or has it only changed to a new naming ?
					// Let's compare entity page IDs
				}else if(EntID1.trim().equalsIgnoreCase(EntID2.trim())){ // the entity has only changed to a new name, but it is the same Entity as the IDs match.
						entityNotChanged++;
						double prior_before = Double.parseDouble(valueFromMap1.split(" :=: ")[1].trim());
						double prior_after = Double.parseDouble(current_prior.trim());
						Pwriter.println("MENTION = "+current_mention+ " \t E_BEF = "+valueFromMap1.split(" :=: ")[0].trim()+":"+prior_before+" \t "+"E_ID:" + pageTitlesMap1.get(entityFromList1) +" \t E_AFTER = "+current_entity+":"+prior_after+ " \t "+"E_ID:" + pageTitlesMap2.get(current_entity) +" \t RATIO:"+prior_before/prior_after);
						priorRatesNOTChanged.add(prior_before/prior_after);
						}else{ // The entity has  REEEEEALLY changed
							entityChanged++;
							double prior_before = Double.parseDouble(valueFromMap1.split(" :=: ")[1].trim());
							double prior_after = Double.parseDouble(current_prior.trim());
							Pwriter2.println("MENTION = "+current_mention+ " \t E_BEF = "+valueFromMap1.split(" :=: ")[0].trim()+":"+prior_before+" \t "+"E_ID:" + pageTitlesMap1.get(entityFromList1) +" \t E_AFTER = "+current_entity+":"+prior_after+ " \t "+"E_ID:" + pageTitlesMap2.get(current_entity) +" \t RATIO:"+prior_before/prior_after);
							priorRatesChanged.add(prior_before/prior_after);
							//	}
						}
			}else{
				mentionOut++; 	// The current mention from the second list is NOT present in the first list ( treeMapME1 )
								// This is the case where the mention/entity does not exist in the first list. Therefore a new mention/entity pair was created in the second list.
								//System.out.println("mention : " +current_mention);
			}
			}else{
				continue;
			}
		}  // :)
		buffReader2.close();

		Pwriter.flush();
		Pwriter.close();

		Pwriter2.flush();
		Pwriter2.close();
		System.out.println(i);
		System.out.println("+------------------------------------------------------------------------------+");
		System.out.println("| Experiments run using the file with mentions occuring at least 100 times. ");
		System.out.println("| Total num. of mention/entity pairs in Wikipedia snapshot 1 :"+lineCountFile1);
		System.out.println("| Total num. of mention/entity pairs in Wikipedia snapshot 2 :"+lineCountFile2);

		//How many mentions from inputFile2 are also present in inputFile1 ?
		System.out.println("| Num. of mentions from snapshot1 also present in snapshot2 : "+mentionIn);
		System.out.println(mentionIn + " are present ");
		System.out.println(mentionOut+" are NO present ");

		//How many have changed the disambiguation ?
		System.out.println("|### Only taking into consideration the ones that are present ###");
		System.out.println("| Number of mention/entity pairs that changed :"+entityChanged);
		System.out.println("| Number of mention/entity pairs that did not change :" + entityNotChanged);
		//maybe the output file
		//mention ; entity_before: ; entity_after : prior ; prior_ratio_change

		//STATISTICS ...
		//DescriptiveStatistics stats = new DescriptiveStatistics();
		// Add the data from the array
		//for( int i = 0; i < priorRates.size(); i++) {
		//        stats.addValue(priorRates.get(i));
		//}
		// Compute some statistics
		//double mean = stats.getMean();
		//double std = stats.getStandardDeviation();
		//double median = stats.getPercentile(50);
		//double max = stats.getMax();
		//double min = stats.getMin();
		//double var = stats.getVariance();
		//System.out.println("mean :"+mean);
		//System.out.println("std :"+std);
		//System.out.println("median :"+median);
		//S//ystem.out.println("max :"+max);
		//System.out.println("min :"+min);
		//System.out.println("var :"+var);
		long stop = System.currentTimeMillis();
		System.out.println("Finished calculating comparisons between disambiguations in " + ((stop - start) / 1000.0) + " seconds.");
	}

	/**   Utility function to calculate top-k = 5 ??? ranked prior probability of mentions/entity pairs
	 *
	 * @param inputFile
	 * @param outputFile
	 * @param j
	 * @throws IOException
	 * @throws CompressorException
	 */
	public void calculatePRIOR_topK(String inputFile, String outputFile,int j) throws IOException, CompressorException {
		long start = System.currentTimeMillis();
		int k = 0;
		//BufferedReader buffReader1 = new BufferedReader(new FileReader(new File(inputFile)));
		BufferedReader buffReader1 = getBufferedReaderForCompressedFile(inputFile);
		BufferedReader buffReader2 = getBufferedReaderForCompressedFile(inputFile);
		PrintWriter Pwriter = new PrintWriter(outputFile, "UTF-8"); // "mentionEntityLinks_PRIOR.txt"
		String line1 = null;
		String line2 = buffReader2.readLine();
		TreeMap<String, TreeMap<String, Double>> priorMap = new TreeMap<>();
		TreeMap<String, Double> priorMapElem = null;
		TreeMap<String, Double> sortedMap = null;
		String key = null;
		while ((line1 = buffReader1.readLine()) != null) {
			k = 0;
			line2 = buffReader2.readLine();
			String[] elements1 = line1.split(" ;-; ");
			String currentMention = elements1[0];
			String currentEntity = elements1[1];
			Double currentPrior = Double.parseDouble(elements1[2]);
			TreeMap<String, Double> tmpMap = priorMap.get(currentMention);
			if (priorMap.isEmpty()) {
				priorMapElem = new TreeMap<String, Double>();
				priorMapElem.put(currentEntity, currentPrior);
				priorMap.put(currentMention, priorMapElem);
			} else {
				tmpMap = priorMap.get(currentMention);
				if (tmpMap != null) {
					tmpMap.put(currentEntity, currentPrior);
					priorMap.put(currentMention, tmpMap);
					if (line2 == null) {
						key = priorMap.firstKey();
						tmpMap = priorMap.get(key);
						sortedMap = sortByValues(tmpMap);
						Set<?> set = sortedMap.entrySet();
						Iterator<?> i = set.iterator();
						while (i.hasNext()) {
							@SuppressWarnings("rawtypes")
							Map.Entry me = (Map.Entry) i.next();
							if (k < j) {
								//System.out.println(key + " ;-; " + me.getKey() + " ;-; " + me.getValue());
								Pwriter.println(key + " ;-; " + me.getKey() + " ;-; " + me.getValue());
								k++;
							} else {
								break;
							}
						}
						continue;
					}
				} else {
					key = priorMap.firstKey();
					tmpMap = priorMap.get(key);
					sortedMap = sortByValues(tmpMap);
					Set<?> set = sortedMap.entrySet();
					Iterator<?> i = set.iterator();
					// Display elements
					while (i.hasNext()) {
						@SuppressWarnings("rawtypes")
						Map.Entry me = (Map.Entry) i.next();
						if (k < j) {
							//System.out.println(key + " ;-; " + me.getKey() + " ;-; " + me.getValue());
							Pwriter.println(key + " ;-; " + me.getKey() + " ;-; " + me.getValue());
							k++;
						} else {
							break;
						}
					}

					priorMap = new TreeMap<>();
					priorMapElem = new TreeMap<String, Double>();
					priorMapElem.put(currentEntity, currentPrior);
					priorMap.put(currentMention, priorMapElem);
					if (line2 == null) {
						key = priorMap.firstKey();
						tmpMap = priorMap.get(key);
						sortedMap = sortByValues(tmpMap);
						set = sortedMap.entrySet();
						i = set.iterator();
						while (i.hasNext()) {
							@SuppressWarnings("rawtypes")
							Map.Entry me = (Map.Entry) i.next();
							if (k < j) {
								//System.out.println(key + " ;-; " + me.getKey() + " ;-; " + me.getValue());
								Pwriter.println(key + " ;-; " + me.getKey() + " ;-; " + me.getValue());
								k++;
							} else {
								break;
							}
						}
					}
				}
			}
		}
		buffReader1.close();
		buffReader2.close();
		Pwriter.flush();
		Pwriter.close();
		long stop = System.currentTimeMillis();
		Compressor cp = new Compressor();
		cp.compressTxtBZ2(outputFile);
		System.out.println("Finished calculating top" +j+" prior probability in " + ((stop - start) / 1000.0) + " seconds.");

	}

	/**
	 * Sort a TreeMap by values
	 *
	 * @param map
	 * @return
	 */
	public static <K, V extends Comparable<V>> TreeMap<K, V> sortByValues(final TreeMap<K, V> map) {
		Comparator<K> valueComparator = new Comparator<K>() {

			public int compare(K k1, K k2) {
				int compare = map.get(k2).compareTo(map.get(k1));
				// System.out.println(compare);
				if (compare == 0)
					return 1;
				else
					return compare;
			}
		};

		TreeMap<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
		sortedByValues.putAll(map);
		return sortedByValues;
	}

	/**
	 * Utility function to calculate the prior probability of mentions/entity pairs
	 * The PRIOR is given by the number of times each mention occur associated with
	 * each entity divided by the total number of times that that mention occurs in the whole
	 * corpus associated with an entity.
	 *
	 * @param inputFile
	 *            The input file must be the sorted file with frequencies count
	 * 
	 *            (i.e)
	 *            ASA (automobile); ASA (automobile); 1;
	 *            ASA Aluminium Body; ASA  Aluminium Body; 2
	 *            ASA Hall of Fame Stadium; ASA Hall of Fame Stadium; 3
	 *
	 *            Produces output such as
	 *
	 *            Alan Williams; Alan Williams (actor); 0.08333333333333333
	 *            Alan Williams; Alan Williams (criminal); 0.027777777777777776
	 *            Alan Williams; Alan Williams (musician); 0.027777777777777776
	 *            Alan Williams; Alan Williams; 0.8333333333333334
	 *            Alan Williams; Alan Williams (New Zealand diplomat); 0.027777777777777776
	 *
	 * @param outputFile
	 * @throws IOException
	 * @throws CompressorException
	 */
	public void calculatePRIOR(String inputFile, String outputFile) throws IOException, CompressorException {
		long start = System.currentTimeMillis();
		BufferedReader buffReader1 = getBufferedReaderForCompressedFile(inputFile);// "mentionEntityLinks_SORTED_Freq.txt"
		BufferedReader buffReader2 = getBufferedReaderForCompressedFile(inputFile);// "mentionEntityLinks_SORTED_Freq.txt"
		PrintWriter Pwriter = new PrintWriter(outputFile, "UTF-8"); // "mentionEntityLinks_PRIOR.txt"
		String line1 = null;
		String line2 = buffReader2.readLine();
		Map<String, Double> priorMap = new HashMap<String, Double>();
		while ((line1 = buffReader1.readLine()) != null) {
			line2 = buffReader2.readLine();
			if (line2 != null) {
				String[] elements1 = line1.split(" ;-; ");
				String[] elements2 = line2.split(" ;-; ");
				if (priorMap.isEmpty()) {
					priorMap.put(elements1[1], 1.0);
					if (!elements2[0].equalsIgnoreCase(elements1[0])) {
						double sum = 0.0;
						for (double f : priorMap.values()) {
							sum += f;
						}
						Iterator<?> it = priorMap.entrySet().iterator();
						while (it.hasNext()) {
							@SuppressWarnings("rawtypes")
							Map.Entry pair = (Map.Entry) it.next();
							Double priorProb = (Double) pair.getValue() / sum;
							Pwriter.println(elements1[0].trim() + " ;-; " + pair.getKey().toString().trim() + " ;-; "
									+ priorProb.toString().trim());
							it.remove();
						}
						priorMap = new HashMap<String, Double>();
						continue;
					}
				} else {
					if (elements2[0].equalsIgnoreCase(elements1[0])) {
						Double d = priorMap.get(elements1[1]);
						if (d == null) {
							d = 0.0;
						}
						priorMap.put(elements1[1], d + 1.0);
						continue;
					} else {
						Double d = priorMap.get(elements1[1]);
						if (d == null) {
							d = 0.0;
						}
						priorMap.put(elements1[1], d + 1.0);
						double sum = 0.0;
						for (double f : priorMap.values()) {
							sum += f;
						}
						Iterator<?> it = priorMap.entrySet().iterator();
						while (it.hasNext()) {
							@SuppressWarnings("rawtypes")
							Map.Entry pair = (Map.Entry) it.next();
							Double priorProb = (Double) pair.getValue() / sum;
							Pwriter.println(elements1[0].trim() + " ;-; " + pair.getKey().toString().trim() + " ;-; "
									+ priorProb.toString().trim());
							it.remove();
						}
						continue;
					}
				}

			} else {
				String[] elements1 = line1.split(" ;-; ");
				if (priorMap.isEmpty()) {
					priorMap.put(elements1[1], 1.0);
					double sum = 0.0;
					for (double f : priorMap.values()) {
						sum += f;
					}
					Iterator<?> it = priorMap.entrySet().iterator();
					while (it.hasNext()) {
						@SuppressWarnings("rawtypes")
						Map.Entry pair = (Map.Entry) it.next();
						Double priorProb = (Double) pair.getValue() / sum;
						Pwriter.println(elements1[0].trim() + " ;-; " + pair.getKey().toString().trim() + " ;-; "
								+ priorProb.toString().trim());
						it.remove();
					}

				} else {
					Double d = priorMap.get(elements1[1]);
					if (d == null) {
						d = 0.0;
					}
					priorMap.put(elements1[1], d + 1.0);

					double sum = 0.0;
					for (double f : priorMap.values()) {
						sum += f;
					}

					Iterator<?> it = priorMap.entrySet().iterator();
					while (it.hasNext()) {
						@SuppressWarnings("rawtypes")
						Map.Entry pair = (Map.Entry) it.next();
						Double priorProb = (Double) pair.getValue() / sum;
						Pwriter.println(elements1[0].trim() + " ;-; " + pair.getKey().toString().trim() + " ;-; "
								+ priorProb.toString().trim());
					}
				}
			}
		}
		buffReader1.close();
		buffReader2.close();
		Pwriter.flush();
		Pwriter.close();

		long stop = System.currentTimeMillis();
		Compressor cp = new Compressor();
		cp.compressTxtBZ2(outputFile);
		System.out.println("Finished calculating prior probability in " + ((stop - start) / 1000.0) + " seconds.");

	}

	/**
	 * Utility function to receive an input file sort the elements and write
	 * back to disk
	 * 
	 * @param inputListFile
	 * @param outputListFile
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private void sortList(String inputListFile, String outputListFile) throws IOException {
		long start = System.currentTimeMillis();
		BufferedReader buffReader1 = new BufferedReader(new FileReader(inputListFile));
		PrintWriter Pwriter = new PrintWriter(outputListFile, "UTF-8");
		List<String> mentionEntityList = new LinkedList<String>();
		String inputLine = null;
		while ((inputLine = buffReader1.readLine()) != null) {
			mentionEntityList.add(inputLine);
		}
		buffReader1.close();
		if (!mentionEntityList.isEmpty()) {
			Collections.sort(mentionEntityList);
		} else {
			System.out.println("Empty file. There is nothing to be sorted.");
			System.exit(1);
		}
		for (String outputLine : mentionEntityList) {
			Pwriter.println(outputLine);
		}
		Pwriter.flush();
		Pwriter.close();
		long stop = System.currentTimeMillis();
		System.out.println("Finished sorting " + inputListFile + " in " + ((stop - start) / 1000.0) + " seconds.");

	}

	/**
	 * This is a utility method to load the file with the page titles into a
	 * TreeSet in memory. It is used to compare whether a pattern found in the
	 * input file matches a wikipedia page title
	 * 
	 * @param pageTitles
	 * @return
	 * @throws IOException
	 * @throws CompressorException
	 */
	public Set<String> loadTitlesList(String pageTitles) throws IOException, CompressorException {
		Set<String> set = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		BufferedReader bffReader = getBufferedReaderForCompressedFile(pageTitles);// new  BufferedReader(new FileReader(pageTitles));
		String inpLine = null;
		while ((inpLine = bffReader.readLine()) != null) {
			set.add(inpLine.split(" \t ")[0]);
		}
		bffReader.close();
		return set;
	}


	/**
	 *
	 * @param titlesMapJson
	 * @return
	 * @throws org.json.simple.parser.ParseException
	 * @throws IOException
	 */
	public Map<String, String> loadTitlesRedirectMap(String titlesMapJson) throws org.json.simple.parser.ParseException, IOException {
		Map<String, String> TitlesMap = new TreeMap<String, String>();
		FileReader reader = new FileReader(titlesMapJson);
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(reader);
		JSONArray array = (JSONArray) obj;
		int jsonSize = array.size();
		for (int i = 0; i < jsonSize; i++) {
			JSONObject jobject = (JSONObject) array.get(i);
			String pageTitle = (String) jobject.get("title");
			String redirectPage = (String) jobject.get("redirect");
			TitlesMap.put(pageTitle, redirectPage);
		}
		return TitlesMap;
	}

	/**
	 *
	 * This method is meant to write to disk the number of occurrences of each
	 * entity
	 *
	 *
	 * @param mentionEntityLinkSorted_Freq_Unique
	 *            mentionEntityLinks_SORTED_Freq_Uniq.txt.bz2
	 * @throws CompressorException
	 * @throws IOException
	 */
	public void writeLinkOccurrence(String mentionEntityLinkSorted_Freq_Unique)
			throws CompressorException, IOException {
		BufferedReader bffReader = getBufferedReaderForCompressedFile(mentionEntityLinkSorted_Freq_Unique);
		PrintWriter pWriter = new PrintWriter("linkOccurrenceCount.txt", "UTF-8");
		TreeMap<String, Integer> linkTreeMap = new TreeMap<>();
		String inpLine = null;
		while ((inpLine = bffReader.readLine()) != null) {
			// System.out.println(inpLine);
			String[] words = inpLine.split(" ;-; ");
			if (words.length >= 3) {

				Integer linkCount = linkTreeMap.get(words[1]);
				if (linkCount == null) {
					linkTreeMap.put(words[1], Integer.parseInt(words[2]));
				} else {
					linkTreeMap.put(words[1], Integer.parseInt(words[2]) + linkCount);
				}
			}
		}
		bffReader.close();

		Iterator<?> it = linkTreeMap.entrySet().iterator();
		while (it.hasNext()) {
			@SuppressWarnings("rawtypes")
			Map.Entry pair = (Map.Entry) it.next();
			pWriter.println(pair.getKey().toString().trim() + " ;-; " + pair.getValue().toString().trim());
			it.remove();
		}
		pWriter.flush();
		pWriter.close();
		Compressor cp = new Compressor();
		cp.compressTxtBZ2("linkOccurrenceCount.txt");
	}

	/**
	 * Calculate the frequency distribution given the original file with the
	 * frequency count The frequency distribution show a frequency distribution
	 * of each mention entity taking into consideration the total amount of
	 * mentions and entities in the file.
	 * 
	 * @param inputFile
	 * @param outputFile
	 * @throws NumberFormatException
	 * @throws IOException
	 * 
	 *             Produces output like :
	 * 
	 *             ASA (automobile); ASA (automobile); 1; 0.20000 ASA Aluminium
	 *             Body; ASA Aluminium Body; 2; 0.40000 ASA Aluminium Body; ASA
	 *             Aluminium Body; 2; 0.40000 ASA Hall of Fame Stadium; ASA Hall
	 *             of Fame Stadium; 3; 0.60000 ASA Hall of Fame Stadium; ASA
	 *             Hall of Fame Stadium; 3; 0.60000 ASA Hall of Fame Stadium;
	 *             ASA Hall of Fame Stadium; 3; 0.60000
	 */
	public void freqDistribution(String inputFile, String outputFile) throws NumberFormatException, IOException {
		long start = System.currentTimeMillis();
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
		PrintWriter pWriter = new PrintWriter(outputFile, "UTF-8");
		String inpLine = null;
		while ((inpLine = bffReader.readLine()) != null) {
			String[] words = inpLine.split(" ;-; ");
			if (words != null) {
				double count = 0.0;
				count = Double.parseDouble(words[2].trim());
				double freq = count / lineCount;
				pWriter.println(inpLine + ";" + String.format("%.5f", freq));
			}
		}
		pWriter.flush();
		pWriter.close();
		bffReader.close();
		fReader.close();
		long stop = System.currentTimeMillis();
		System.out.println("Finished calculating the frequency distribution in " + ((stop - start) / 1000.0) + " seconds.");

	}

	/**
	 * Utility function to count the frequency of a mention no matter the entity
	 * it is assigned to .
	 * 
	 * @param inputFile takes as input
	 * @param outFile
	 * @throws IOException
	 * 
	 *             i.e. ASA Hall of Fame Stadium; ASA Hall of Fame Stadium; 3
	 *             		ASA Hall of Fame Stadium; ASA; 						3
	 *             		ASA Hall of Fame Stadium; ASA Hall of Fame; 		3
	 * @throws CompressorException
	 */

	public void frequencyCount(String inputFile, String outFile) throws IOException, CompressorException {
		long start = System.currentTimeMillis();
		BufferedReader bffReader = getBufferedReaderForCompressedFile(inputFile);
		String inpLine = null;
		Map<String, Integer> frequency = new HashMap<String, Integer>();
		while ((inpLine = bffReader.readLine()) != null) {
			String[] words = inpLine.split(" ;-; ");
			if (words.length >= 1) {
				String key = words[0].trim();
				if ((!key.isEmpty()) && (key != null) && (key != "")) {
					Integer f = frequency.get(key);
					if (f == null) {
						f = 0;
					}
					frequency.put(key, f + 1);
				}
			}
		}
		bffReader.close();
		PrintWriter pWriter = new PrintWriter(outFile, "UTF-8");
		BufferedReader buffReader = getBufferedReaderForCompressedFile(inputFile);
		String inp = null;
		while ((inp = buffReader.readLine()) != null) {

			String[] keys = inp.split(" ;-; ");
			if (keys.length >= 1) {
				String key = keys[0].trim();
				if ((key != null) && (!key.isEmpty() && (key != " "))) {
					Integer value = frequency.get(key);
					pWriter.println(inp.trim() + " ;-; " + value.toString().trim());
				}
			}
		}
		buffReader.close();
		pWriter.flush();
		pWriter.close();
		long stop = System.currentTimeMillis();
		Compressor cp = new Compressor();
		cp.compressTxtBZ2(outFile);
		System.out.println("Finished calculating the frequency count in " + ((stop - start) / 1000.0) + " seconds.");

	}

	/**
	 * This is a utility class to cut the mention/Entity file by frequency of mention as is done in the WIKIFY !
	 *
	 * It just takes as input mentionEntityLinks_SORTED_Freq.txt.bz2 and cuts off lines that show less than freq and the count of mentions.
	 *
	 * @param inputFile  mentionEntityLinks_SORTED_Freq.txt.bz2
	 * @param outFile
	 * @param freq
	 * @throws IOException
	 * @throws CompressorException
	 */
	public void frequencyCut(String inputFile, String outFile, int freq) throws IOException, CompressorException {
		BufferedReader bffReader = getBufferedReaderForCompressedFile(inputFile);
		PrintWriter pWriter = new PrintWriter(outFile + "_" + freq + ".txt", "UTF-8");
		String inpLine = null;
		while ((inpLine = bffReader.readLine()) != null) {
			String[] words = inpLine.split(" ;-; ");
			if (StringUtils.isNumeric(words[2].trim())) {
				if (Integer.parseInt(words[2].trim()) >= freq) {
					pWriter.println(inpLine);
				} else {
					continue;
				}
			} else { // try the next field
				if (StringUtils.isNumeric(words[3].trim())) {
					if (Integer.parseInt(words[3].trim()) >= freq) {
						pWriter.println(inpLine);
					} else {
						continue;
					}
				}
			}
		}
		bffReader.close();
		pWriter.flush();
		pWriter.close();
		Compressor cp = new Compressor();
		cp.compressTxtBZ2(outFile + "_" + freq + ".txt");
	}

	/**
	 * Utility function to count the number of occurrences of a mention and eliminate repeated occurrences
	 * of the same mention, but it will keep the count of the number of mentions per entity occurrence.
	 * 
	 * @param inputFile
	 * @param outFile
	 * @throws IOException
	 *
	 *          i.e.
	 *          ASA Hall of Fame Stadium; ASA Hall of Fame Stadium; 3
	 *         	ASA Hall of Fame Stadium; ASA Hall of Fame Stadium; 3
	 *         	ASA Hall of Fame Stadium; ASA Hall of Fame Stadium; 3
	 *          ASA; ASA; 2
	 *          ASA; ASA; 2
	 *			ASA ;-; AS Ariana ;-; 803
	 *			ASA ;-; AS Ariana ;-; 803
	 *			ASA ;-; AS Ariana ;-; 803
	 *			ASA ;-; AS Ariana ;-; 803
	 *					...
	 *
	 *			will keep only one mention per entity
	 *
	 *			i.e.
	 *			ASA Hall of Fame Stadium; ASA Hall of Fame Stadium; 3
	 *          ASA; ASA; 2
	 *          ASA; AS Ariana; 803
	 *
	 * @throws CompressorException
	 */

	public void frequencyCountUnique(String inputFile, String outFile) throws IOException, CompressorException {
		long start = System.currentTimeMillis();
		BufferedReader bffReader = getBufferedReaderForCompressedFile(inputFile);
		String inpLine = null;
		Map<String, Integer> frequency = new TreeMap<String, Integer>();
		while ((inpLine = bffReader.readLine()) != null) {
			String[] words = inpLine.split(" ;-; ");
			if (words.length >= 1) {
				String key_value = words[0].trim() + " ;-; " + words[1].trim();
				if ((!key_value.isEmpty()) && (key_value != null) && (key_value != "")) {
					Integer f = frequency.get(key_value);
					if (f == null) {
						f = 0;
					}
					frequency.put(key_value, f + 1);
				}
			}
		}
		bffReader.close();
		PrintWriter pWriter = new PrintWriter(outFile, "UTF-8");
		Iterator<?> it = frequency.entrySet().iterator();
		while (it.hasNext()) {
			@SuppressWarnings("rawtypes")
			Map.Entry pair = (Map.Entry) it.next();
			pWriter.println(pair.getKey().toString().trim() + " ;-; " + pair.getValue().toString().trim());
			// treee.add(pair.getKey() + ";\t" +pair.getValue());
			it.remove();
		}
		pWriter.flush();
		pWriter.close();
		long stop = System.currentTimeMillis();
		Compressor cp = new Compressor();
		cp.compressTxtBZ2(outFile);
		System.out.println("Finished calculating the frequency count unique in " + ((stop - start) / 1000.0) + " seconds.");

	}

	/**
	 *
	 * @param fileIn
	 * @return
	 * @throws FileNotFoundException
	 * @throws CompressorException
	 */
	public BufferedReader getBufferedReaderForCompressedFile(String fileIn)
			throws FileNotFoundException, CompressorException {
		FileInputStream fin = new FileInputStream(fileIn);
		BufferedInputStream bis = new BufferedInputStream(fin);
		CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
		BufferedReader br2 = new BufferedReader(new InputStreamReader(input));
		return br2;
	}

	/**
	 * Utility function to check whether an entity is in the pageTitles list or in the pageTitles redirection map
	 * It writes as output the a compressed mentionEntityLinks_SORTED.txt file that contains mentions and entities sorted,
	 *  but only the entities that have a corresponding entity page.
	 *
	 * @param mentionEntityFile Takes as input the original mentionEntityLinks.txt file
	 * @param titles The set structured list of titles read from pagesTitles.tsv.bz2
	 * @param treemap The map structure with the redirected titles and the redirections read from pagesTitles_REDIRECT.json
	 * @throws IOException
	 * @throws CompressorException
	 */
	public void checkTitles(String mentionEntityFile, Set<String> titles, Map<String, String> treemap) throws IOException, CompressorException {
		long start = System.currentTimeMillis();
		ArrayList<String> finalList = new ArrayList<String>();
		BufferedReader bffReader = getBufferedReaderForCompressedFile(mentionEntityFile);
		PrintWriter notAMatchtFileWriter = new PrintWriter(new File("mentionEntityLinks_NOT_MATCHED.txt"));

		String inLine = null;
		while ((inLine = bffReader.readLine()) != null) {
			String[] aM = inLine.split(" ;-; ");
			if (treemap.containsKey(aM[1])) {
				if ((treemap.get(aM[1]) == " ") || (treemap.get(aM[1]).trim().length() == 0)) {
					continue;
				} else {
					finalList.add(aM[0] + " ;-; " + treemap.get(aM[1]));
					MATCH += 1;
				}
			} else {
				if (titles.contains(aM[1])) {
					finalList.add(inLine);
					MATCH += 1;
				} else {
					NOMATCH++;
					notAMatchtFileWriter.println(inLine);
				}
			}
		}
		notAMatchtFileWriter.flush();
		notAMatchtFileWriter.close();
		bffReader.close();

		Collections.sort(finalList);
		PrintWriter outputFileWriter = new PrintWriter(new File("mentionEntityLinks_SORTED.txt"));
		for (String str : finalList) {
			outputFileWriter.println(str);
		}
		outputFileWriter.flush();
		outputFileWriter.close();
		long stop = System.currentTimeMillis();

		Compressor cp = new Compressor();
		cp.compressTxtBZ2("mentionEntityLinks_SORTED.txt");
		cp.compressTxtBZ2("mentionEntityLinks_NOT_MATCHED.txt");

		Statistics st = new Statistics();
		st.writeMentionEntityStatistics((stop - start) / 1000.0, MATCH, NOMATCH);

	}

	/***
	 * Utility function to check whether an entity is in the map file and
	 * solving the redirection.
	 *
	 * @param mentionEntityFile
	 * @param treemap
	 * @throws IOException
	 */
	public void checkInTitlesMap(String mentionEntityFile, Map<String, String> treemap) throws IOException {
		System.out.println("Checking if entity matches the entity page redirection map ...");
		int total = 0;
		int match = 0;
		BufferedReader bffReader = new BufferedReader(new FileReader(mentionEntityFile));
		PrintWriter ptemp2File = new PrintWriter(new File("mentionEntity.temp2"));
		String inLine = null;
		while ((inLine = bffReader.readLine()) != null) {
			total += 1;
			String[] aM = inLine.split(" ;-; ");
			if (treemap.containsKey(aM[1])) {
				if ((treemap.get(aM[1]) == " ") || (treemap.get(aM[1]).trim().length() == 0)) {
					continue;
				} else {
					ptemp2File.println(aM[0] + " ;-; " + treemap.get(aM[1]));
					match += 1;
				}
			}
		}
		bffReader.close();
		ptemp2File.flush();
		ptemp2File.close();
		System.out.println("Total titles : " + total);
		System.out.println("Number of matches : " + match);
		System.out.println("Number of non matches : " + (total - match));
		System.out.println("Done.");
	}

	/**
	 * This function writes the page titles to output file. It writes the files
	 *
	 * pageTitles_ALL.tsv // All the pages titles in the dump
	 * pagesTitles_SPECIAL.tsv // All the special pages titles in the dump
	 * pagesTitles_REDIRECT.tsv // All the redirected pages titles
	 * pagesTitles.tsv // All the articles titles ( no special pages and no redirections )
	 * pagesTitles_REDIRECT.json // All the redirected pages with its corresponding redirection page
	 *
	 * @param XMLFile
	 * @throws IOException
	 */
	public void writePageTitles(String XMLFile) throws IOException {
		long start = System.currentTimeMillis();
		WikiXMLParser wxsp = null;
		JSONArray Jarray = new JSONArray();
		try {
			wxsp = WikiXMLParserFactory.getSAXParser(XMLFile);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}
		try {
			wxsp.setPageCallback(new PageCallbackHandler() {
				//int i= 0;
				@SuppressWarnings("unchecked")
				public void process(WikiPage page) {
					//System.out.println(i++);
					String pageID = page.getID();
					String pTitle = page.getTitle().replaceAll("_", " ").trim();
					char[] pTitleArray = pTitle.trim().toCharArray();
					if (pTitleArray.length > 0) {
						pTitleArray[0] = Character.toUpperCase(pTitleArray[0]);
						pTitle = new String(pTitleArray);
					}
					TOTAL_PAGE_TITLE++;
					if (isSpecial(pTitle)) {
						SPECIAL_PAGES++;
						specialPagesTitlesList.add(pTitle + " \t "+pageID);
					} else {
						allPagesTitlesList.add(pTitle + " \t " + pageID); // allPagesTitlesList excludesSpecial pages TAB SEPARATED with pageID
						if (pTitle.length() == 0 || (pTitle == " ")) {
							EMPTY_TITLE_PAGES++;
						} else {
							if (pTitle.contains("(disambiguation)")) {
								DISAMBIGUATION_PAGE++;
							} else {
								String wikitext = page.getWikiText().trim();
								Matcher mRedirect = redirectPattern.matcher(wikitext);

								if (mRedirect.find()) {
									redirectPagesTitlesList.add(pTitle + " \t " + pageID);
									REDIRECTION++;
									/********/
									Matcher matcher = mentionEntityPattern.matcher(wikitext);
									while (matcher.find()) {
										String redirectedTitle = matcher.group(1).replaceAll("_", " ").trim();
										if (isSpecial(redirectedTitle)) {
											continue;
										} else {
											char[] redirectionTitleArray = redirectedTitle.toCharArray();
											if (redirectionTitleArray.length > 0) {
												redirectionTitleArray[0] = Character.toUpperCase(redirectionTitleArray[0]);
												redirectedTitle = new String(redirectionTitleArray);
												if (redirectedTitle.contains("#")) {
													if ((redirectedTitle.indexOf("#") != 0)	&& (redirectedTitle.indexOf("#") != -1)) {
														redirectedTitle = redirectedTitle.substring(0,redirectedTitle.indexOf("#"));
													}
												}
												if (pTitle.contains("(disambiguation)") || (redirectedTitle.contains("(disambiguation)"))) { // disambiguation
													continue;
												}
												pageTitlesMap.put(pTitle, redirectedTitle);
												JSONObject jobj = new JSONObject();
												jobj.put("redirect", redirectedTitle);
												jobj.put("title", pTitle);
												//jobj.put();
												Jarray.add(jobj);
											} else {
												continue;
											}
										}
									}
									/***********/
								} else {// In case it is not #REDIRECT. So I am getting the page title and adding to
										// list. This is the actual list of page titles I am interested.
									ENTITYPAGE++;
									if (pTitle.contains("#")) {
										if ((pTitle.indexOf("#") != 0) && (pTitle.indexOf("#") != -1)) {
											pTitle = pTitle.substring(0, pTitle.indexOf("#"));
										}
									}
									pagesTitlesList.add(pTitle+" \t "+pageID);
								}
							}
						}
					}
				}
			});
			wxsp.parse();
		} catch (Exception e) {
			e.printStackTrace();
		}

		long stop = System.currentTimeMillis();

		Compressor cp = new Compressor();
		// ***************************************************************************************//
		// Writing all pages titles to output file "pageTitles_ALL.tsv"
		PrintWriter allPagesTitlesWriter = new PrintWriter("pageTitles_ALL.tsv", "UTF-8");
		Collections.sort(allPagesTitlesList);
		allPagesTitlesWriter.println("<<< Total number of pages: " + TOTAL_PAGE_TITLE + " >>>");
		for (String title : allPagesTitlesList) {
			allPagesTitlesWriter.println(title);
		}
		allPagesTitlesWriter.flush();
		allPagesTitlesWriter.close();
		cp.compressTxtBZ2("pageTitles_ALL.tsv");
		// ***************************************************************************************//
		// Writing all pages titles with REDIRECT to output file "pagesTitles_REDIRECT.tsv"
		//PrintWriter allRedirPagesTitlesWriter = new PrintWriter("pagesTitles_REDIRECT.tsv", "UTF-8");
		//Collections.sort(redirectPagesTitlesList);
		//allRedirPagesTitlesWriter.println("<<< Total number of redirected pages: " + REDIRECTION + " >>>");
		//for (String title : redirectPagesTitlesList) {
		//	allRedirPagesTitlesWriter.println(title);
		//}
		//allRedirPagesTitlesWriter.flush();
		//allRedirPagesTitlesWriter.close();
		//cp.compressTxtBZ2("pagesTitles_REDIRECT.tsv");
		// ***************************************************************************************//
		// Writing ONLY SPECIAL pages titles to output file
		// "pageTitles_SPECIAL.tsv"
		//PrintWriter specialPagesTitlesWriter = new PrintWriter("pagesTitles_SPECIAL.tsv", "UTF-8");
		//Collections.sort(specialPagesTitlesList);
		//specialPagesTitlesWriter.println("<<< Number of special pages titles: " + SPECIAL_PAGES + " >>>");
		//for (String title : specialPagesTitlesList) {
		//	specialPagesTitlesWriter.println(title);
		//}
		//specialPagesTitlesWriter.flush();
		//specialPagesTitlesWriter.close();
		//cp.compressTxtBZ2("pagesTitles_SPECIAL.tsv");
		// ***************************************************************************************//
		// Writing ONLY Entity pages titles( i.e. without redirection
		// (#REDIRECT) and without SPECIAL pages) to output file "pageTitles.tsv"
		PrintWriter pagesTitlesWriter = new PrintWriter("pagesTitles.tsv", "UTF-8");
		Collections.sort(pagesTitlesList);
		pagesTitlesWriter.println("<<< Number of pages titles: " + ENTITYPAGE + " >>>");
		for (String title : pagesTitlesList) {
			pagesTitlesWriter.println(title);
		}
		pagesTitlesWriter.flush();
		pagesTitlesWriter.close();

		cp.compressTxtBZ2("pagesTitles.tsv");
		// ***************************************************************************************//
		ObjectMapper jsonMapper = new ObjectMapper();
		try {
			String outputJSON = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(Jarray);
			PrintWriter redirectPagesTitlesMapWriter = new PrintWriter("pagesTitles_REDIRECT.json", "UTF-8");
			redirectPagesTitlesMapWriter.println(outputJSON);
			redirectPagesTitlesMapWriter.flush();
			redirectPagesTitlesMapWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		allPagesTitlesList.clear();
		specialPagesTitlesList.clear();
		redirectPagesTitlesList.clear();
		System.gc();
		Statistics st = new Statistics();
		st.writeTitlesStatistics(((stop - start) / 1000.0), SPECIAL_PAGES, REDIRECTION, DISAMBIGUATION_PAGE, ENTITYPAGE,
				duplicatePageTitle.size(), EMPTY_TITLE_PAGES, TOTAL_PAGE_TITLE);
	}

	/**
	 * This method is meant to write a page id and a list of links inside that page.
	 * @param inputFile
	 * @param pageTitlesMap
	 * @throws CompressorException
	 * @throws IOException
	 */
	public void writePageLinks(String inputFile, String pageTitlesFile) throws CompressorException, IOException{
		WikiXMLParser wxsp = null;
		JSONArray Jarray = new JSONArray();
		Map<String, String> pageTitlesMap = new TreeMap<String, String>();
		BufferedReader bfR1 = getBufferedReaderForCompressedFile(pageTitlesFile);		//Here I am only reading the page titles list and adding to the pageTitlesMap1 that contains the entity and the entityID
		String l = bfR1.readLine();
		while((l = bfR1.readLine()) != null ){
			Charset.forName("UTF-8").encode(l);
			String temp[] = l.split(" \t ");
			String entity = temp[0].trim();
			String entityID = temp[1].trim();
			pageTitlesMap.put(entity, entityID);
		}
		bfR1.close();

		try {
			wxsp = WikiXMLParserFactory.getSAXParser(inputFile);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		try {
			wxsp.setPageCallback(new PageCallbackHandler() {
				public void process(WikiPage page) {
					String wikitext = page.getWikiText().trim();
					String text = getPlainText(wikitext);
					String pTitle = page.getTitle().replaceAll("_", " ").trim();
					char[] pTitleArray = pTitle.trim().toCharArray();
					if (pTitleArray.length > 0) {
						pTitleArray[0] = Character.toUpperCase(pTitleArray[0]);
						pTitle = new String(pTitleArray);
					}
					if (isSpecial(pTitle)) {
						// DO NOTHING if it is a Special Page ! Special pages are pages such as Help: , Wikipedia:, User: pages
						// DO NOTHING if it is an empty page title.
					} else {
						if (pTitle.contains("(disambiguation)")) {
							// DO NOTHING.  I am not interested in Disambiguation
						} else {
							Matcher mRedirect = redirectPattern.matcher(wikitext);
							if (mRedirect.find()) {
								// DO NOTHING if it is redirect page !
							} else {
								if ((text != null) && (text != " ")) {
									Matcher matcher = mentionEntityPattern.matcher(text);
									ArrayList<String> pageLinks = new ArrayList<String>();
									String pageID = page.getID();
									while (matcher.find()) {
										String[] temp = matcher.group(1).split("\\|");
										if (temp == null || temp.length == 0) {
											continue;
										}
										String mention = null;
										String entitylink = null;
										if (temp.length > 1) {
											entitylink = temp[0].replaceAll("_", " ").trim();
											mention = temp[1].trim();
										} else {
											entitylink = temp[0].replaceAll("_", " ").trim();
											mention = temp[0].trim();
										}
										if (mention.length() == 0 || (mention == " ") || (entitylink.length() == 0) || (entitylink == " ")) {
											continue;
										}
										if (mention.contains(":") || (entitylink.contains(":"))) { // ignoring rubbish such  as Image:Kropotkin Nadar.jpg]
											continue;
										}
										if (mention.contains("(disambiguation)") || (entitylink.contains("(disambiguation)"))) { // disambiguation
											continue;
										}
										if (entitylink.contains("#")) {
											if ((entitylink.indexOf("#") != 0) && (entitylink.indexOf("#") != -1)) {
												entitylink = entitylink.substring(0, entitylink.indexOf("#"));
											}
										}
										int spaceIndex = mention.indexOf("(");
										if ((spaceIndex != 0) && (spaceIndex != -1)) {
											mention = mention.substring(0, spaceIndex);
										}
										char[] entityLinkArray = entitylink.toCharArray();
										if (entityLinkArray.length > 0) {
											entityLinkArray[0] = Character.toUpperCase(entityLinkArray[0]);
											entitylink = new String(entityLinkArray);

											if(pageTitlesMap.containsKey(entitylink)){
												String LinkId = pageTitlesMap.get(entitylink);
												pageLinks.add(LinkId);
											}else{
											}
										} else {
											continue;
										}
									}
									JSONObject jobj = new JSONObject();
									jobj.put("pageTitleID",pageID );
									jobj.put("links",pageLinks);
									Jarray.add(jobj);
								}
							}
						}
					}
				}
			});
			wxsp.parse();
	}catch (Exception e) {
		e.printStackTrace();
	}
		ObjectMapper jsonMapper = new ObjectMapper();
		try {
			String outputJSON = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(Jarray);
			PrintWriter redirectPagesTitlesMapWriter = new PrintWriter("pageLinks.json", "UTF-8");
			redirectPagesTitlesMapWriter.println(outputJSON);
			redirectPagesTitlesMapWriter.flush();
			redirectPagesTitlesMapWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/***
	 * This utility function is meant to dump all mention/entity pairs without
	 * checking if the entity is in the pages titles list or in the redirection
	 * map. It writes a big file with all the mention/entity pairs.* (Unsorted)
	 *
	 * @param inputFile
	 * @param outputFile
	 * @throws IOException
	 */
	public void writeMentionEntity_NO_Checking(String inputFile, String outputFile) throws IOException {
		long start = System.currentTimeMillis();
		PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
		WikiXMLParser wxsp = null;
		try {
			wxsp = WikiXMLParserFactory.getSAXParser(inputFile);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		try {
			wxsp.setPageCallback(new PageCallbackHandler() {
				public void process(WikiPage page) {
					String wikitext = page.getWikiText().trim();
					String text = getPlainText(wikitext);
					String title = page.getTitle().replaceAll("_", " ").trim();

					char[] pTitleArray = title.trim().toCharArray();
					if (pTitleArray.length > 0) {
						pTitleArray[0] = Character.toUpperCase(pTitleArray[0]);
						title = new String(pTitleArray);
					}

					if (isSpecial(title)) {
						// DO NOTHING if it is a Special Page ! Special pages
						// are pages such as Help: , Wikipedia:, User: pages
						// DO NOTHING if it is an empty page title.
					} else {
						if (title.contains("(disambiguation)")) {
							// continue; I am not interested in Disambiguation
						} else {
							Matcher mRedirect = redirectPattern.matcher(wikitext);
							if (mRedirect.find()) {
								// DO NOTHING if it is redirect page !
							} else {
								if ((text != null) && (text != " ")) {
									Matcher matcher = mentionEntityPattern.matcher(text);
									while (matcher.find()) {
										String[] temp = matcher.group(1).split("\\|");
										if (temp == null || temp.length == 0) {
											continue;
										}
										String mention = null;
										String entitylink = null;
										if (temp.length > 1) {
											entitylink = temp[0].replaceAll("_", " ").trim();
											mention = temp[1].trim();
										} else {
											entitylink = temp[0].replaceAll("_", " ").trim();
											// mention =
											// temp[0].replaceAll("_","
											// ").trim();
											mention = temp[0].trim();
										}
										if (mention.length() == 0 || (mention == " ") || (entitylink.length() == 0)
												|| (entitylink == " ")) {
											continue;
										}
										if (mention.contains(":") || (entitylink.contains(":"))) { // ignoring rubbish such  as Image:Kropotkin Nadar.jpg]
											continue;
										}
										if (mention.contains("(disambiguation)")
												|| (entitylink.contains("(disambiguation)"))) { // disambiguation
											continue;
										}
										if (entitylink.contains("#")) {
											if ((entitylink.indexOf("#") != 0) && (entitylink.indexOf("#") != -1)) {
												entitylink = entitylink.substring(0, entitylink.indexOf("#"));
											}
										}
										int spaceIndex = mention.indexOf("(");
										if ((spaceIndex != 0) && (spaceIndex != -1)) {
											mention = mention.substring(0, spaceIndex);
										}
										char[] entityLinkArray = entitylink.toCharArray();
										if (entityLinkArray.length > 0) {
											entityLinkArray[0] = Character.toUpperCase(entityLinkArray[0]);
											entitylink = new String(entityLinkArray);
											String mentionEntity = mention.trim() + " ;-; " + entitylink.trim();
											writer.println(mentionEntity);
											mentionEntityPairs++;
										} else {
											continue;
										}
									}
								}
							}
						}
					}
				}
			});
			wxsp.parse();
		} catch (Exception e) {
			e.printStackTrace();
		}
		writer.flush();
		writer.close();
		long stop = System.currentTimeMillis();
		Compressor cp = new Compressor();
		cp.compressTxtBZ2(outputFile);
		// PrintWriter duplicatePageTitlesWriter = new
		// PrintWriter("pagesTitlesDuplicates.txt", "UTF-8");
		// Iterator<?> it = duplicatePageTitle.entrySet().iterator();
		// while (it.hasNext()) {
		// @SuppressWarnings("rawtypes")
		// Map.Entry pair = (Map.Entry) it.next();
		// if((int) pair.getValue() >= 2){
		// duplicatePageTitlesWriter.println(pair.getKey()+" : "
		// +pair.getValue());
		// }
		// it.remove();
		// }
		// duplicatePageTitlesWriter.flush();
		// duplicatePageTitlesWriter.close();
		Statistics st = new Statistics();
		st.writeMentionEntityStatistics((stop - start) / 1000.0, mentionEntityPairs);

	}

	/**
	 * In "Learning to Link with Wikipedia" by David Milne and Ian H. Witten
	 * they only select pages that had 50 links or more, therefore I am parsing
	 * a Wikipedia dump and only storing pages that has more than 50 links.
	 *
	 * This method is NOT USED yet.
	 *
	 * @param inputFile
	 * @param outputFile
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public void writePagesLinksCount(String inputFile) throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter("pageLinksCount.txt", "UTF-8");
		ArrayList<String> pageLinksOut = new ArrayList<>();
		WikiXMLParser wxsp = null;
		try {
			wxsp = WikiXMLParserFactory.getSAXParser(inputFile);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		try {
			wxsp.setPageCallback(new PageCallbackHandler() {
				public void process(WikiPage page) {
					String wikitext = page.getWikiText().trim();
					String text = getPlainText(wikitext);
					String title = page.getTitle().replaceAll("_", " ").trim();
					int linkCount = 0;
					char[] pTitleArray = title.trim().toCharArray();
					if (pTitleArray.length > 0) {
						pTitleArray[0] = Character.toUpperCase(pTitleArray[0]);
						title = new String(pTitleArray);
					}
					if (isSpecial(title)) {
					} else {
						if (title.contains("(disambiguation)")) {
						} else {
							Matcher mRedirect = redirectPattern.matcher(wikitext);
							if (mRedirect.find()) {
							} else {
								if ((text != null) && (text != " ")) {
									Matcher matcher = mentionEntityPattern.matcher(text);
									while (matcher.find()) {
										String[] temp = matcher.group(1).split("\\|");
										if (temp == null || temp.length == 0) {
											continue;
										}
										String mention = null;
										String entitylink = null;
										if (temp.length > 1) {
											entitylink = temp[0].replaceAll("_", " ").trim();
											mention = temp[1].trim();
										} else {
											entitylink = temp[0].replaceAll("_", " ").trim();
											// mention =
											// temp[0].replaceAll("_","
											// ").trim();
											mention = temp[0].trim();
										}
										if (mention.length() == 0 || (mention == " ") || (entitylink.length() == 0)
												|| (entitylink == " ")) {
											continue;
										}
										if (mention.contains(":") || (entitylink.contains(":"))) { // ignoring rubbish such as Image:Kropotkin Nadar.jpg]
											continue;
										}
										if (mention.contains("(disambiguation)")
												|| (entitylink.contains("(disambiguation)"))) { // disambiguation
											continue;
										}
										if (entitylink.contains("#")) {
											if ((entitylink.indexOf("#") != 0) && (entitylink.indexOf("#") != -1)) {
												entitylink = entitylink.substring(0, entitylink.indexOf("#"));
											}
										}
										int spaceIndex = mention.indexOf("(");
										if ((spaceIndex != 0) && (spaceIndex != -1)) {
											mention = mention.substring(0, spaceIndex);
										}
										linkCount++;
										pageLinksOut.add(title + " ;-; " + entitylink);
									}
									if (linkCount >= 50) {
										writer.println(title + " ;-; " + linkCount);
										linkCount = 0;
									}
								}
							}
						}
					}
				}
			});
			wxsp.parse();
		} catch (Exception e) {
			e.printStackTrace();
		}
		writer.flush();
		writer.close();
		Collections.sort(pageLinksOut);
		PrintWriter pwriter = new PrintWriter(new File("pageLinksOut.txt"));
		for (String s : pageLinksOut) {
			pwriter.println(s);
		}
		pwriter.flush();
		pwriter.close();
	}

	/**
	 * Function to write the mentions and the entities to disk
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 * 
	 */
	public void writeMentionEntity(String inputFile, String outputFile)
			throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
		Utils ut = new Utils();
		long start = System.currentTimeMillis();
		WikiXMLParser wxsp = null;
		try {
			wxsp = WikiXMLParserFactory.getSAXParser(inputFile);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		try {
			wxsp.setPageCallback(new PageCallbackHandler() {
				public void process(WikiPage page) {
					String wikitext = page.getWikiText().trim();
					String text = getPlainText(wikitext);
					String title = page.getTitle().trim();
					Matcher mRedirect = redirectPattern.matcher(wikitext);
					if (ut.isSpecial(title)) {
						// DO NOTHING if it is a Special Page ! Special pages  are pages such as Help: , Wikipedia:, User: pages
						// DO NOTHING if it is an empty page title.

						// 1.counting the mention/entity from disambiguation
						// pages (i.e. Car (Disambiguation)
						// 2.NOT counting the mention/entity from disambiguation
						// pages
					} else {

						if (mRedirect.find()) {
							// DO NOTHING if it is redirect page !

						} else {
							if ((text != null) && (!text.isEmpty()) && (text != "")) {
								Matcher matcher = mentionEntityPattern.matcher(text);
								while (matcher.find()) {
									String[] temp = matcher.group(1).split("\\|");
									if (temp == null || temp.length == 0) {
										continue;
									}
									String mention = null;
									String entitylink = null;
									if (temp.length > 1) {
										entitylink = temp[0].trim();
										mention = temp[1].trim();
									} else {
										entitylink = temp[0].trim();
										mention = temp[0].trim();
									}

									if (mention.length() == 0 || (mention == " ") || (entitylink.length() == 0)
											|| (entitylink == " ")) {
										continue;
									}
									if (mention.contains(":") || (entitylink.contains(":"))) { // ignoring
																								// rubbish
																								// such
																								// as
																								// Image:Kropotkin
																								// //
																								// Nadar.jpg]
										continue;
									}
									if (entitylink.contains("#")) {
										continue;
									}
									// MENTION_ENTITY++;
									// if(allPagesTitlesList.contains(entitylink)){
									if (pagesTitlesList.contains(entitylink)) {
										// IN_TITLES_LIST++;
										String mentionEntity = mention.trim() + " ;-; " + entitylink.trim();
										writer.println(mentionEntity);
										// continue;
										// }else{
										// entitylink is not in titleList
										// notinlistwriter.println(entitylink);
									} else { // if
												// (pageTitlesMap.get(entitylink)!=null)
												// {
										String keyMapping = pageTitlesMap.get(entitylink);
										if (keyMapping != null) {
											String mentionEntity = mention + " ;-; " + keyMapping;
											writer.println(mentionEntity);
										}
									}
								}
							}
						}
					}
				}
			});
			wxsp.parse();
		} catch (Exception e) {
			e.printStackTrace();
		}
		writer.flush();
		writer.close();
		long stop = System.currentTimeMillis();
		System.out.println("Finished collecting articles Mentions and Entities Links in " + ((stop - start) / 1000.0)
				+ " seconds.");
		System.out.println("Number of mention/entities pairs : " + MENTION_ENTITY);
		System.out.println("Number of entities matches in the Titles list : " + IN_TITLES_LIST);
	}

	/**
	 * Utility function to clean the wikipedia page and return clean text
	 * 
	 * @param wikiText
	 * @return
	 */

	public String getPlainText(String wikiText) {
		String text = wikiText.replaceAll("&gt;", ">");
		text = text.replaceAll("&lt;", "<");
		text = text.replaceAll("&quot;", "\"");
		text = text.replaceAll("&amp;nbsp;", " ");
		text = text.replaceAll("&amp;", "&");
		text = text.replaceAll("&nbsp;", " ");
		text = text.replaceAll("&ndash;", "-");
		text = text.replaceAll("&hellip;", "...");
		text = text.replaceAll("\"", "").replaceAll("\'''", "").replaceAll("\''", "");
		text = commentsCleanupPattern.matcher(text).replaceAll("");
		text = stylesPattern.matcher(text).replaceAll("");
		text = refCleanupPattern.matcher(text).replaceAll("");
		text = text.replaceAll("</?.*?>", "");
		// text = curlyCleanupPattern0.matcher(text).replaceAll("");
		// text = curlyCleanupPattern1.matcher(text).replaceAll("");

		return text.trim();
	}

	/**
	 *  This utility function will remove unambiguous mentions from the original mention/entity file (i.e. mention types with only one entity assigned)
	 *
	 *	Al Pacino; Al Pacino; 1.0
	 *
	 * @param mentionEntityFile
	 * @throws CompressorException
	 * @throws IOException
	 */
	public void removeSingletons(String mentionEntityFile) throws CompressorException, IOException{ //mentionEntityLinks_PRIOR_topk
		BufferedReader bffReader = getBufferedReaderForCompressedFile(mentionEntityFile);
		String outputFile = mentionEntityFile.substring(0, mentionEntityFile.lastIndexOf('.'));
		TreeMap<String,LinkedList<String>> mentionMap1 = new TreeMap<>(); //map to store a mention and list of top5 disambiguations
		PrintWriter writer = new PrintWriter(outputFile+"_NOSINGLE", "UTF-8");
		PrintWriter writerS = new PrintWriter(outputFile+"_SINGLETON", "UTF-8");
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
	 * This utility function is meant to calculate a dominance measure.
	 * It takes as input the mentionEntity_top5_2006 , the pageTitles from 2006, the mentionEntiity_top5_2016 and the pageTitles 2016
	 *
	 * @param inputFile1 	mentionEntity_top5
	 * @param pageTitles1
	 * @param inputFile2	mentionEntity_top5
	 * @param pageTitles2
	 * @throws CompressorException
	 * @throws IOException
	 */
	public void calculateDominance(String inputFile1, String pageTitles1, String inputFile2, String pageTitles2) throws CompressorException, IOException{
		HashMap<String,LinkedList<String>> mentionMap1 = new HashMap<>(); //map to store a mention and list of top5 disambiguations
		HashMap<String,LinkedList<String>> mentionMap2 = new HashMap<>(); //map to store a mention and list of top5 disambiguations
		TreeMap<String,String> pageTitlesMap1 = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);   //map to store the Entities and entities page ids
		TreeMap<String,String> pageTitlesMap2 = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);	//map to store the Entities and entities page ids
		PrintWriter writerDom = new PrintWriter("./resource/dominance_2014_2016.tsv", "UTF-8");
		BufferedReader bfR1 = getBufferedReaderForCompressedFile(pageTitles1);		//Here I am only reading the page titles list and adding to the pageTitlesMap1 that contains the entity and the entityID
		String l = bfR1.readLine();
		int x = 0;
		while((l = bfR1.readLine()) != null ){
			Charset.forName("UTF-8").encode(l);
			x++;
			String temp[] = l.split(" \t ");
			String entity = temp[0].trim();
			String entityID = temp[1].trim();
			pageTitlesMap1.put(entity, entityID);
		}
		bfR1.close();
		System.out.println("Num. page titles from "+pageTitles1+"\t : "+x);

		BufferedReader bfR2 = getBufferedReaderForCompressedFile(pageTitles2); 		//Here I am only reading the page titles list and adding to the pageTitlesMap2 that contains the entity and the entityID
		String l2 = bfR2.readLine();
		int y =0;
		while((l2 = bfR2.readLine()) != null ){
			Charset.forName("UTF-8").encode(l2);
			y++;
			String temp[] = l2.split(" \t ");
			String entity = temp[0].trim();
			String entityID = temp[1].trim();
			pageTitlesMap2.put(entity, entityID);
		}
		bfR2.close();
		System.out.println("Num. page titles from "+pageTitles2+"\t : "+y);

		BufferedReader buffReader1 = getBufferedReaderForCompressedFile(inputFile1);
		String line1 = null;
		int linefile1 = 0;
		while ((line1 = buffReader1.readLine()) != null) {
			linefile1++;
			Charset.forName("UTF-8").encode(line1);
			String[] tempSplit = line1.split(" ;-; ");
			String mention1 = tempSplit[0].trim();
			String entity1 = tempSplit[1].trim();
			String prior1  = tempSplit[2].trim();

			String EID1 = pageTitlesMap1.get(entity1);

			if((EID1==null) || (EID1=="")){// This case the pageTitles list does not have the page ID therefore I am not interested.
				//System.out.println(mention1 + " ===: " + entity1);
				//entityDoesNotExist++;
				continue;
			}else{

			LinkedList<String> tempList1 = mentionMap1.get(mention1);

			if(tempList1 == null){
				//z++;
				tempList1 = new LinkedList<>();
				tempList1.add(entity1+" :=: "+prior1);
				mentionMap1.put(mention1, tempList1);
			}else{
				//noz++;
				tempList1.add(entity1+" :=: "+prior1);
				mentionMap1.put(mention1, tempList1);
			}
			}
		}

		buffReader1.close();


		int linefile2 = 0;
		BufferedReader buffReader2 = getBufferedReaderForCompressedFile(inputFile2);
		String line2 = null;
		while ((line2 = buffReader2.readLine()) != null) {
			linefile2++;
			Charset.forName("UTF-8").encode(line2);
			String[] tempSplit = line2.split(" ;-; ");
			String mention2 = tempSplit[0].trim();
			String entity2 = tempSplit[1].trim();
			String prior2  = tempSplit[2].trim();
			String EID2 = pageTitlesMap2.get(entity2);
			if((EID2==null) || (EID2 == "") ){ // This case the pageTitles list does not have the page ID
				//System.out.println(mention2 + " ===: " + entity2);
				//entityDoesNotExist++;
				continue;
			}else{
				//y++;
			//checking whether the Map has the mention from the second list
			LinkedList<String> tempList2 = mentionMap2.get(mention2);
			if(tempList2 == null){
				tempList2 = new LinkedList<>();
				tempList2.add(entity2+" :=: "+prior2);
				mentionMap2.put(mention2, tempList2);
			}else{
				tempList2.add(entity2+" :=: "+prior2);
				mentionMap2.put(mention2, tempList2);
			}
			}
		}
		buffReader2.close();

		int commomMention =0;
		for(Map.Entry<String,LinkedList<String>> entry : mentionMap1.entrySet()) {//while there is an entry in the map created from the first Wikipedia dump
			  String keyMention1 = entry.getKey();
			  String keyMention2 = keyMention1;
			  LinkedList<String> valueEntities1 = mentionMap1.get(keyMention1);
			  LinkedList<String> valueEntities2 = mentionMap2.get(keyMention2);


			  if((valueEntities1!=null) && (valueEntities2 != null)) { // It means that the mention exists in both map files.
				  commomMention++;
				  if((valueEntities1.size()>=2) && (valueEntities2.size()>=2)){
					  String ersteEntityFromList1 = valueEntities1.get(0).split(" :=: ")[0].trim();
					  String ersteEntityID1 = pageTitlesMap1.get(ersteEntityFromList1);
					  double ersteEntityPriorFromList1 = Double.parseDouble(valueEntities1.get(0).split(" :=: ")[1].trim());
					  String zweiteEntityFromList1 = valueEntities1.get(1).split(" :=: ")[0].trim();
					  String zweiteEntityID1 = pageTitlesMap1.get(zweiteEntityFromList1);
					  double zweiteEntityPriorFromList1 = Double.parseDouble(valueEntities1.get(1).split(" :=: ")[1].trim());

					  String ersteEntityFromList2 = valueEntities2.get(0).split(" :=: ")[0].trim();
					  double ersteEntityPriorFromList2 = Double.parseDouble(valueEntities2.get(0).split(" :=: ")[1].trim());
					  String ersteEntityID2 = pageTitlesMap2.get(ersteEntityFromList2);
					  String zweiteEntityFromList2 = valueEntities2.get(1).split(" :=: ")[0].trim();
					  String zweiteEntityID2 = pageTitlesMap2.get(zweiteEntityFromList2);
					  double zweiteEntityPriorFromList2 = Double.parseDouble(valueEntities2.get(1).split(" :=: ")[1].trim());
					  double delta1 = ersteEntityPriorFromList1 - zweiteEntityPriorFromList1;
					  double delta2 = ersteEntityPriorFromList2 - zweiteEntityPriorFromList2;
					  double dominance;
					  double normDelta1 = delta1/ersteEntityPriorFromList1;
					  //System.out.println(normDelta1);
					  double normDelta2 = delta2/ersteEntityPriorFromList2;
					  //System.out.println(normDelta2);

					  if(( ersteEntityFromList1.equals(ersteEntityFromList2) ) || (ersteEntityID1.equals(ersteEntityID2)) ){
						 dominance = normDelta2 - normDelta1;
					  }else{
						  dominance = normDelta2 + normDelta1;
					  }
					  writerDom.println(keyMention1 + "\t" +dominance);
					  //System.out.println(keyMention1 + " : " +dominance);

				  }else{
					  continue;
				  }
			}else{
			  //mention does not exist in both maps.
			}
		} // for end !
		writerDom.flush();
		writerDom.close();
		System.out.println("Commom mentions between ["+inputFile1+"] and \n ["+inputFile2+"] = "+commomMention);
	}
/**********************************************************************************************************/
	/**
	 * This function is meant to calculate Ranking ( Spearman , Kendall tau )
	 *
	 * @param inputFile1  mentionEntityLinks_PRIOR_100_top5_NOSINGLE
	 * @param pageTitles1
	 * @param inputFile2
	 * @param pageTitles2
	 * @throws IOException
	 * @throws CompressorException
	 */

	public void calculateRanking(String inputFile1, String pageTitles1, String inputFile2, String pageTitles2,String year1, String year2) throws IOException, CompressorException{
		//BufferedReader buffReader1 = new BufferedReader(new FileReader(new File(inputFile1)));
		BufferedReader buffReader1 = getBufferedReaderForCompressedFile(inputFile1);
		//BufferedReader buffReader2 = new BufferedReader(new FileReader(new File(inputFile2)));
		BufferedReader buffReader2 = getBufferedReaderForCompressedFile(inputFile2);

		HashMap<String,LinkedList<String>> mentionMap1 = new HashMap<>(); //map to store a mention and list of top5 disambiguations
		HashMap<String,LinkedList<String>> mentionMap2 = new HashMap<>(); //map to store a mention and list of top5 disambiguations

		TreeMap<String,String> pageTitlesMap1 = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);   //map to store the Entities and entities page ids
		TreeMap<String,String> pageTitlesMap2 = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);	//map to store the Entities and entities page ids

		//PrintWriter writerSpearman = new PrintWriter("./resource/Spearman.tsv", "UTF-8");
		PrintWriter writerKendall = new PrintWriter("./resource/Kendall_"+year1+"_"+year2+".tsv", "UTF-8");

		//Here I am only reading the page titles list and adding to the pageTitlesMap1 that contains the entity and the entityID
		//BufferedReader bfR1 = new BufferedReader(new FileReader(new File(pageTitles1)));
		BufferedReader bfR1 = getBufferedReaderForCompressedFile(pageTitles1);
		String l = bfR1.readLine();
		int x = 0;
		while((l = bfR1.readLine()) != null ){
			Charset.forName("UTF-8").encode(l);
			x++;
			String temp[] = l.split(" \t ");
			String entity = temp[0].trim();
			String entityID = temp[1].trim();
			pageTitlesMap1.put(entity, entityID);
		}
		bfR1.close();

		//Here I am only reading the page titles list and adding to the pageTitlesMap2 that contains the entity and the entityID
		//BufferedReader bfR2 = new BufferedReader(new FileReader(new File(pageTitles2)));
		BufferedReader bfR2 = getBufferedReaderForCompressedFile(pageTitles2);
		String l2 = bfR2.readLine();
		int y =0;
		while((l2 = bfR2.readLine()) != null ){
			Charset.forName("UTF-8").encode(l2);
			y++;
			String temp[] = l2.split(" \t ");
			String entity = temp[0].trim();
			String entityID = temp[1].trim();
			pageTitlesMap2.put(entity, entityID);
		}
		bfR2.close();
		//Here I am reading the mention/entity file1  and adding the entities to a hashmap
		String line1 = null;
		while ((line1 = buffReader1.readLine()) != null) {
			Charset.forName("UTF-8").encode(line1);
			String[] tempSplit = line1.split(" ;-; ");
			String mention1 = tempSplit[0].trim();
			String entity1 = tempSplit[1].trim();

			LinkedList<String> tempList1 = mentionMap1.get(mention1);

			if(tempList1 == null){
				tempList1 = new LinkedList<>();
				tempList1.add(entity1);
				mentionMap1.put(mention1, tempList1);
			}else{
				tempList1.add(entity1);
				mentionMap1.put(mention1, tempList1);
				}
			}
		buffReader1.close();

		//Here I am reading the mention/entity file2 and adding the entities to a hashmap
		String line2 = null;
		while ((line2 = buffReader2.readLine()) != null) {
			Charset.forName("UTF-8").encode(line2);
			String[] tempSplit2 = line2.split(" ;-; ");
			String mention2 = tempSplit2[0].trim();
			String entity2 = tempSplit2[1].trim();
			LinkedList<String> tempList2 = mentionMap2.get(mention2);

			if(tempList2 == null){
				tempList2 = new LinkedList<>();
				tempList2.add(entity2);
				mentionMap2.put(mention2, tempList2);
			}else{
				tempList2.add(entity2);
				mentionMap2.put(mention2, tempList2);
				}
			}
		buffReader2.close();

		int numElemMap1 = 0;

		for(Map.Entry<String,LinkedList<String>> entry : mentionMap1.entrySet()) {
			// It is only interesting to calculate measures on common mentions
			numElemMap1++;
			String keyMention1 = entry.getKey();
			LinkedList<String> listEntities1 = (LinkedList<String>) mentionMap1.get(keyMention1);
			LinkedList<String> listEntities2 = (LinkedList<String>) mentionMap2.get(keyMention1);
			int list1Size = listEntities1.size();
			int list2Size = 0;
			if(listEntities2!=null){
				//UNION of the two sets
				Set<String> fullSet = new HashSet<>();
				fullSet.addAll(listEntities1);
				fullSet.addAll(listEntities2);
				int setSize = fullSet.size();
				List<String> fullSetList = new ArrayList<>(fullSet);

				/******************************************************************/
				// Two hashmaps to store the entity link and the position where it
				// appears in the list
				HashMap<String,Integer> topK_List1 = new HashMap<String, Integer>();
				HashMap<String,Integer> topK_List2 = new HashMap<String, Integer>();
				int position = 0;
				for(String elem:listEntities1){
					position++;
					topK_List1.put(elem, position);
				}
				position = 0;
				for(String elem:listEntities2){
					position++;
					topK_List2.put(elem, position);
				}
				/*****************************************************************/
				ArrayList<Combination> allPossibleCombinations = new ArrayList<Combination>();
				for(int k = 0; k < setSize;k++){
					for(int p = k+1; p < setSize; p++){
						allPossibleCombinations.add(new Combination(new Element(fullSetList.get(k)), new Element(fullSetList.get(p))));
					}
				}
				double K_p = 0.0;
				double K_sum = 0.0;
				double n = (double) fullSetList.size();// |D| = S U T
				//Once we have all the combinations created lets calculate Kendall modified
				for(Combination cb : allPossibleCombinations){
					String firstElem = cb.getElem1().getElement();
					String secondElem = cb.getElem2().getElement();
					int posA1,posA2,posB1,posB2;
					if(topK_List1.containsKey(firstElem)) {posA1 = topK_List1.get(firstElem);}else{posA1 = -1;}
					if(topK_List1.containsKey(secondElem)){posA2 = topK_List1.get(secondElem);}else{posA2 = -1;}
					if(topK_List2.containsKey(firstElem)) {posB1 = topK_List2.get(firstElem);}else{posB1 = -1;}
					if(topK_List2.containsKey(secondElem)){posB2 = topK_List2.get(secondElem);}else{posB2 = -1;}
					//CASE 1  i and j appear in both top_k lists. if i and j are in the same other (such as i being ahead of j in both top_k lists) then let K^{(p)}i,j (T1,T2) = 0;
					//this corresponds to "no penalty" for {i,j}.
					//If  i and j are in the opposite order (such as i being ahead of j in T1 and j being ahead of i in T2, then let the penalty be K^{(p)}i,j (T1,T2) =1
					if((posA1>0) && (posA2>0) && (posB1>0) && (posB2>0)){
						if( ( (posA1>posA2) &&  (posB1>posB2) ) || ( (posA1<posA2) &&  (posB1<posB2) ) ){
							//no penalty
							K_p = 0.0;
							K_sum += K_p;
							continue;
						}else{
							//penalty
							K_p = 1.0;
							K_sum += K_p;
							continue;
						}
					}
					//CASE 2  i and j both appear in one top_k list (say T1) and exactly one of them i or j (say j) appears in the other top_k list (T2). If i is ahead of j in T1, then
					//the penalty K^{(p)}(T1,T2) = 0 and otherwise let K^{(p)} (T1,T2) = 1. Intuitively we know that i is ahead of j as far as T2 is concerned, since i appears in T2 but j does not.
					if( ( ((posA1>0) && (posA2>0)) && ((posB1>0) || (posB2>0)) ) || ( ((posB1>0) && (posB2>0)) &&  ((posA1>0) || (posA2>0)) ) ){
						//both appear in the first list
						if((posA1>0) && (posA2>0)){
							if( (posA1<posA2) && (posB1>0)){
								K_p = 0.0;
								K_sum += K_p;
								continue;
							}else{
								K_p = 1.0;
								K_sum += K_p;
								continue;
							}
						}
						//both appear in the second list
						if((posB1>0) && (posB2>0)){
							if( (posB1<posB2) && (posA1>0)){
								K_p = 0.0;
								K_sum += K_p;
								continue;
							}else{
								K_p = 1.0;
								K_sum += K_p;
								continue;
							}
						}
					}
					//CASE 3  i but not j appears in one top_k list (say T1) and j but not i appears in the other top_k list (T2). Then let the penalty K^{(p)} (T1,T2) = 1. Intuitively we know
					//that i is ahead of j as far as T1 is concerned and j is ahead of i as far as T2 is concerned.
					if( (((posA1>0) && (posA2<0)) && ((posB1<0) && (posB2>0))) || (((posA1<0) && (posA2>0)) && ((posB1>0) && (posB2<0))) ){
						K_p = 1.0;
						K_sum += K_p;
						continue;
					}
					//CASE 4  i and j both appear in one top_k list (say T1) but neither i nor j appeas in the other top_k list(T2). This is the interesting case(the only
					//case where there is really an aoption as to what the penalty should be. In this case, we let the penalty  K^{(p)}(T1,T2) = p.
					//Remember that for p=0 K_min and for p=1/2 K_avg.
					if(  ((posA1>0) && (posA2>0) && (posB1<0) && (posB2<0)) || ((posA1<0) && (posA2<0) && (posB1>0) && (posB2>0) )  ){
						// penalty
						//K_p = p;
						//K_p = 0.0; // K_min
						K_p = 0.5;// K_avg
						K_sum += K_p;
						continue;
					}
				}//end of for combinations
				double norm = (double)((n*(n - 1.0))/2.0);
				double K_dist = K_sum / norm;
				//Calculating statistical significance
				//z-value
				double z = (3.0 * K_dist * Math.sqrt(n* (n -1)) ) / (Math.sqrt( 2.0*(2.0*n + 5)));  //Check z-table
				writerKendall.println(keyMention1 + " \t "+ K_dist+ " \t "+z);
				writerKendall.flush();
			}
		}
		//Here I start verifying some stuff
		//for(Map.Entry<String,LinkedList<String>> entry : mentionMap1.entrySet()) {
		//	while there is an entry in the map created from the first Wikipedia dump. Because it is only interesting to calculate kendall
		//	on the mentions that are common in both of the lists.
		//	numElemMap1++;

		//	String keyMention1 = entry.getKey();
		//	LinkedList<String> listEntities1 = (LinkedList<String>) mentionMap1.get(keyMention1); 			//getting the list of entities for Mention1
		//	LinkedList<String> listEntities2 = null;
		//	int list1Size = listEntities1.size();
		//	int list2Size = 0;
		//	int dist[] = new int[list1Size]; //difference between ranks
		//	String keyMention2 = keyMention1;

			// Considering the mention from Map1 is also present in Map2
		//	if(mentionMap2.containsKey(keyMention2)){
		//		int j = 0;
		//		listEntities2 = (LinkedList<String>) mentionMap2.get(keyMention2);
		//		list2Size = listEntities2.size();
		//		for(String s : listEntities1){
		//			String EID1 = pageTitlesMap1.get(s);
		//			if((EID1==null) || (EID1=="")){
		//				continue;
		//			}
		//			for(int i = 0; i < list2Size; i++){
		//				String entity2 = listEntities2.get(i);
		//				String EID2 = pageTitlesMap2.get(entity2);//

		//				if((EID2==null) || (EID2=="")){
		//					continue;
		//				}
		//				if(s.equals(entity2) || EID1.equals(EID2)){ //////////////  check ID maybe the entity changed but the ID is the same.
		//					int diff = j - i ;
		//					//System.out.println(s);
		//					//System.out.println(listEntities2.get(i));
		//					//System.out.println(diff);
		//					dist[j] = diff;
		//				}
		//			}
		//			j++;
		//		}
		//	}
			//***********************************************************************************************//
				//Spearman coefficient
		//	int dSquared[] = new int[list1Size]; //careful here must pay attention to the size of the list
		//	double Sum = 0.0;
		//	for (int i=0; i < dist.length; i++){
		//		dSquared[i] = dist[i]*dist[i];
		//		//System.out.println("d : "+dist[i]+ " \t d^2 :"+dSquared[i]);
		//		Sum += dSquared[i];
		//	}
		//	double rho = 0.0;
		//	int n = 0;
		//	if(dist.length >= list2Size){
		//		n = dist.length;
		//	}else{
		//		n = list2Size;
		//	}
			//System.out.println("n :"+n);
			//System.out.println("Sum :"+Sum);
			//System.out.println("6*Sum :"+(6*Sum));
			//System.out.println(list2Size);
			//rho = 1.0 - (6*Sum (di^2)/ n*(n^2 -1 ) )
		//	double n2 = n*n;
			//System.out.println("n2 :" +n2);
		//	double n3 = n2*n;
			//System.out.println("n3 :" +n3);
		//	rho = (1.0 - (double)( (6*Sum) / (n3 - n) ) );
			//System.out.println("rho :"+rho);
		//	writerSpearman.println(keyMention1 + " \t " +rho);
		//	writerSpearman.flush();
		//}
		//writerSpearman.close();

		//Kendal's tau  ( This is Kendall's tau-A )
		//***********************************************************************************************//
		//for(Map.Entry<String,LinkedList<String>> entry : mentionMap1.entrySet()) {	//while there is an entry in the map created from the first Wikipedia dump
		//	String keyMention1 = entry.getKey();
		//	int Nc=0, Nd=0, n=0;
		//	LinkedList<String> listEntities1 = (LinkedList<String>) mentionMap1.get(keyMention1); 			//getting the list of entities for Mention1
		//	LinkedList<String> listEntities2 = (LinkedList<String>) mentionMap2.get(keyMention1); 			//getting the list of entities for Mention1 from Map2

		//	if(listEntities2!=null){
		//		int list1Size = listEntities1.size();
		//		int xi=-1,xj=-1,yi=-1,yj =-1;
		//		for(int i=0; i<list1Size; i++){
		//			for(int j=i+1;j<list1Size;j++){
		//				xi =i;
		//				xj =j;
		//				String entityFromMap1 = listEntities1.get(i);
		//				String entityFromMap1_II = listEntities1.get(j);
		//				String EID1 = pageTitlesMap1.get(entityFromMap1);
		//				String EID1_II = pageTitlesMap1.get(entityFromMap1_II);
		//				if((EID1==null) || (EID1=="")){
		//					continue;
		//				}
		//				if((EID1_II==null) || (EID1_II=="")){
		//					continue;
		//				}
		//				int index2=0;
		//				for(String entity2 : listEntities2){
		//					String EID2 = pageTitlesMap2.get(entity2);
		//					if((EID2==null) || (EID2=="")){
		//						continue;
		//					}
		//					if(entityFromMap1.equals(entity2) || EID1.equals(EID2)){
		//						yi = index2;
		//						break;
		//					}else{
		//						yi=-1;
		//					}
		//					index2++;
		//				}
		//				index2=0;
		//				for(String entity2_II : listEntities2){
		//					String EID2_II = pageTitlesMap2.get(entity2_II);
		//					if((EID2_II==null) || (EID2_II=="")){
		//						continue;
		//					}
		//					if(entityFromMap1_II.equals(entity2_II) || EID1_II.equals(EID2_II)){
		//						yj = index2;
		//						break;
		//					}else{
		//						yj=-1;
		//					}
		//					index2++;
		//				}//

		//				if((yi>=0) && (yj>=0)){
		//					if(yi<yj){
		//						Nc++;
		//					}else{
		//						Nd++;
		//					}
		//				//	System.out.println();
						//System.out.println("xi : " +xi+ ", xj :"+xj);
			//			//System.out.println("yi : " +yi+ ", yj :"+yj);
		//					}
		//			}
		//		}
		//		//	System.out.println("Nc :"+ Nc + " Nd :"+Nd);
		//		double tau = 0.0;
		//		double numerator = Nc - Nd;
		//		n = list1Size;
		//		//	System.out.println(numerator);
		//		//System.out.println(n);
		//		double denominator = (double) n/2*(n-1);
		//		tau = numerator/denominator;

		//		writerKendall.println(keyMention1 + " \t " +tau);
		//		writerKendall.flush();
		//		}
		//	}
		//********************************************************************************************//
		writerKendall.close();
		//
		//Calculating statistical significance
		//z-value
		//z = (3 * KTau * sqrt(N (N -1) ) ) / (sqrt( 2*(2*N + 5) ))  //Check z-table
	}



	/**
	 *
	 * @param mentionEntity1
	 * @param pageTitles1
	 * @param titlesRedirection1
	 * @param mentionEntity2
	 * @param pageTitles2
	 * @param titlesRedirection2
	 * @throws IOException
	 * @throws CompressorException
	 * @throws ParseException
	 */
	public void compareUnambiguous(String mentionEntity1, String pageTitles1, String titlesRedirection1, String mentionEntity2, String pageTitles2,String titlesRedirection2) throws IOException, CompressorException, ParseException{
		BufferedReader buffReader1 = getBufferedReaderForCompressedFile(mentionEntity1);
		//BufferedReader buffReader2 = new BufferedReader(new FileReader(new File(inputFile2)));
		BufferedReader buffReader2 = getBufferedReaderForCompressedFile(mentionEntity2);

		HashMap<String,LinkedList<String>> mentionMap1 = new HashMap<>(); //map to store a mention and list of top5 disambiguations
		HashMap<String,LinkedList<String>> mentionMap2 = new HashMap<>(); //map to store a mention and list of top5 disambiguations

		PriorOnlyModel POM  = new PriorOnlyModel();
		Map<String, String> titlesRedMap1 = POM.loadTitlesRedirectMap(titlesRedirection1);
		Map<String, String> titlesRedMap2 = POM.loadTitlesRedirectMap(titlesRedirection2);

		TreeMap<String,String> pageTitlesMap1 = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);   //map to store the Entities and entities page ids
		TreeMap<String,String> pageTitlesMap2 = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);	//map to store the Entities and entities page ids

		BufferedReader bfR1 = getBufferedReaderForCompressedFile(pageTitles1);
		String l = bfR1.readLine();
		int x=0;
		while((l = bfR1.readLine()) != null ){
			x++;
			Charset.forName("UTF-8").encode(l);
			String temp[] = l.split(" \t ");
			String entity = temp[0].trim();
			String entityID = temp[1].trim();
			pageTitlesMap1.put(entity, entityID);
		}
		bfR1.close();
		System.out.println("Number of page titles from pageTitles file "+pageTitles1+" : "+x);

		int y=0;
		BufferedReader bfR2 = getBufferedReaderForCompressedFile(pageTitles2);
		String l2 = bfR2.readLine();
		while((l2 = bfR2.readLine()) != null ){
			y++;
			Charset.forName("UTF-8").encode(l2);
			String temp[] = l2.split(" \t ");
			String entity = temp[0].trim();
			String entityID = temp[1].trim();
			pageTitlesMap2.put(entity, entityID);
		}
		bfR2.close();
		System.out.println("Number of page titles from pageTitles file "+pageTitles2+" : "+y);
		//Here I am reading the mention/entity file 1 and adding the entities to a hashmap
		String line1 = null;
		int z=0;
		while ((line1 = buffReader1.readLine()) != null) {
			z++;
			Charset.forName("UTF-8").encode(line1);
			String[] tempSplit = line1.split(" ;-; ");
			String mention1 = tempSplit[0].trim();
			String entity1 = tempSplit[1].trim();
			LinkedList<String> tempList1 = mentionMap1.get(mention1);
			if(tempList1 == null){
				tempList1 = new LinkedList<>();
				tempList1.add(entity1);
				mentionMap1.put(mention1, tempList1);
			}else{
				tempList1.add(entity1);
				mentionMap1.put(mention1, tempList1);
				}
			}
		buffReader1.close();
		System.out.println("Number of unambiguous entities from file "+mentionEntity1+" : "+z);

		//Here I am reading the mention/entity 2 file and adding the entities to a hashmap
		String line2 = null;
		int k=0;
		while ((line2 = buffReader2.readLine()) != null) {
			k++;
			Charset.forName("UTF-8").encode(line2);
			String[] tempSplit = line2.split(" ;-; ");
			String mention2 = tempSplit[0].trim();
			String entity2 = tempSplit[1].trim();
			String prior2  = tempSplit[2].trim();
			String EID2 = pageTitlesMap2.get(entity2);
			LinkedList<String> tempList2 = mentionMap2.get(mention2);
			if(tempList2 == null){
				tempList2 = new LinkedList<>();
				tempList2.add(entity2);
				mentionMap2.put(mention2, tempList2);
			}else{
				tempList2.add(entity2);
				mentionMap2.put(mention2, tempList2);
				}
			}
		buffReader2.close();
		System.out.println("Number of unambiguous entities from file "+mentionEntity2+" : "+k);

		int mentionInCommon = 0;
		for(Map.Entry<String,LinkedList<String>> entry : mentionMap1.entrySet()) {
			String keyMention1 = entry.getKey();
			String keyMention2 = keyMention1;
			// Considering the mention from Map1 is also present in Map2
			if(mentionMap2.containsKey(keyMention1)){
				mentionInCommon++;
				//getting the list of entities for Mention1
				LinkedList<String> listEntities1 = (LinkedList<String>) mentionMap1.get(keyMention1);
				int list1Size = listEntities1.size();

				// getting the list of entities from Mention 2
				LinkedList<String> listEntities2 = (LinkedList<String>) mentionMap2.get(keyMention2);
				int list2Size = listEntities2.size();

				for(String entity1 : listEntities1){
					String EID1 = pageTitlesMap1.get(entity1);
					if((EID1==null) || (EID1=="")){
						break;
					}
					for(String entity2 : listEntities2){
						String EID2 = pageTitlesMap2.get(entity2);
						if((EID2==null) || (EID2=="")){
							break;
						}
						if( (entity1.equalsIgnoreCase(entity2) ) ||  (EID1.equalsIgnoreCase(EID2)) ){
							//entityNOTChanged++;
						}else{
							//checking redirections
							String redirectedEntity = titlesRedMap1.get(entity1);
							//if ((redirectedEntity != null) && (redirectedEntity.equalsIgnoreCase(entity1))) {
								//truePositive++;
							//	break;
							//}
							if ((redirectedEntity != null) && (redirectedEntity.equalsIgnoreCase(entity2))) {
								break;
							}
							redirectedEntity = titlesRedMap1.get(entity2);
							if ((redirectedEntity != null) && (redirectedEntity.equalsIgnoreCase(entity1))) {
								//truePositive++;
								break;
							}
							//if ((redirectedEntity != null) && (redirectedEntity.equalsIgnoreCase(entity2))) {
								//truePositive++;
								//System.out.println(mentionGT + "\t" + entityGT +"\t "+ entityWIKI);
							//	break;
							redirectedEntity = titlesRedMap2.get(entity1);
							if ((redirectedEntity != null) && (redirectedEntity.equalsIgnoreCase(entity2))) {
								break;
							}
							redirectedEntity = titlesRedMap2.get(entity2);
							if ((redirectedEntity != null) && (redirectedEntity.equalsIgnoreCase(entity1))) {
								break;
							}else{
									System.out.println("M :"+keyMention1 + "\t E :"+entity1 + "\t E :"+entity2);
									break;
								}
						}
					}
				}
			}else{
				continue;
			}
		}

		System.out.println("Number of mentions in commom : "+mentionInCommon);
	}

	/**
	 *
	 * @param title
	 * @return
	 */
	public boolean isSpecial(String title) {
		if (title.contains("Category:") || title.contains("Help:") || title.contains("Image:")
				|| title.contains("User:") || title.contains("MediaWiki:") || title.contains("Wikipedia:")
				|| title.contains("Portal:") || title.contains("Template:") || title.contains("File:")
				|| title.contains("Book:") || title.contains("Draft:") || title.contains("Module:")
				|| title.contains("TimedText:") || title.contains("Topic:") ) { //|| title.contains("List of") || title.contains("Table of")
			return true;
		} else {
			return false;
		}
	}
}
