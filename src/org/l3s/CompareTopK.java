package org.l3s;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.compress.compressors.CompressorException;
import org.json.simple.parser.ParseException;
/**
 * @author Renato Stoffalette Joao
 * @mail renatosjoao@gmail.com
 * @version 1.0
 * @date 11.2016
 *
 */
public class CompareTopK {

	public CompareTopK() {
		super();
	}
	/**
	 * Main entry point
	 *
	 * @param args
	 */
	public static void main(String[] args){
		if(args.length<6){
			System.out.println("Please pass the proper number of args");
		}else{
			CompareTopK cpTK = new CompareTopK();
			try {
				cpTK.compare(args[0],args[1],args[2],args[3],args[4],args[5]);
			} catch (IOException | CompressorException | ParseException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This function compares the mention/entity pairs top K changes  !!!
	 *
	 *
	 * @param inputFile1 			mentionEntity file 1
	 * @param pageTitles1 			page titles file1
	 * @param pageRedirection1		redirection file 1
	 * @param inputFile2			mentionEntity file 2
	 * @param pageTitles2			page titles file2
	 * @param pageRedirection2		redirection file 2
	 * @throws IOException
	 * @throws CompressorException
	 * @throws ParseException
	 */
	public void compare(String inputFile1, String pageTitles1, String pageRedirection1, String inputFile2, String pageTitles2, String pageRedirection2) throws IOException, CompressorException, ParseException{
		TreeMap<String,LinkedList<String>> mentionMap1 = new TreeMap<>(); //map to store a mention and list of top5 disambiguations
		TreeMap<String,LinkedList<String>> mentionMap2 = new TreeMap<>(); //map to store a mention and list of top5 disambiguations

		Utils ut = new Utils();
		int x=0 , y=0, z=0, m=0;

		TreeMap<String,String> pageTitlesMap1 = new TreeMap<>();
		TreeMap<String,String> pageTitlesMap2 = new TreeMap<>();

		TreeMap<String,String> titlesRedirectMap1 = ut.loadTitlesRedirectMap(pageRedirection1);
		TreeMap<String,String> titlesRedirectMap2 = ut.loadTitlesRedirectMap(pageRedirection2);

		//Here I am only reading the page titles list and adding to the pageTitlesMap1 that contains the entity and the entityID
		BufferedReader bfR1 = ut.getBufferedReaderForCompressedFile(pageTitles1);
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
		BufferedReader bfR2 = ut.getBufferedReaderForCompressedFile(pageTitles2);
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
		BufferedReader buffReader1 = ut.getBufferedReaderForCompressedFile(inputFile1);
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
		BufferedReader buffReader2 = ut.getBufferedReaderForCompressedFile(inputFile2);
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
		PrintWriter Pwriter = new PrintWriter("comparison_Results_NOT_changed_top_1.txt", "UTF-8");
		PrintWriter Pwriter2 = new PrintWriter("comparison_Results_changed_top_1.txt", "UTF-8");
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

				if ( (entityFromList1.equalsIgnoreCase(entityFromList2)) || (EntID1.equalsIgnoreCase(EntID2)) ){
					// = = = The disambiguation entity has not changed. We are happy !!! = = =
					double prior_before = Double.parseDouble(valueEntities1.get(0).split(" :=: ")[1].trim());
					double prior_after = Double.parseDouble(valueEntities2.get(0).split(" :=: ")[1].trim());
					Pwriter.println("M="+keyMention1+ "\tE_bef="+valueEntities1.get(0)+"\t"+"E_id:" + EntID1 +"\tE_after="+valueEntities2.get(0)+"\t"+"E_id:" + EntID2);// + "\tMAD:"+Math.abs((prior_before-prior_after)/2));
					priorRatesNOTChanged.add(prior_before/prior_after);
				}else{
					String redir1 = titlesRedirectMap1.get(entityFromList2);
					if(redir1!=null){
						if(redir1.equalsIgnoreCase(entityFromList1)){
							Pwriter.println("M="+keyMention1+ "\tE_bef="+valueEntities1.get(0)+"\t"+"E_id:" + EntID1 +"\tE_after="+valueEntities2.get(0)+"\t"+"E_id:" + EntID2);// + "\tMAD:"+Math.abs((prior_before-prior_after)/2));
							continue;
						}else{
						}
					}
					redir1 = titlesRedirectMap1.get(entityFromList1);
					if(redir1!=null){
						if(redir1.equalsIgnoreCase(entityFromList2)){
							Pwriter.println("M="+keyMention1+ "\tE_bef="+valueEntities1.get(0)+"\t"+"E_id:" + EntID1 +"\tE_after="+valueEntities2.get(0)+"\t"+"E_id:" + EntID2);// + "\tMAD:"+Math.abs((prior_before-prior_after)/2));
							continue;
						}else{
						}
					}
					String redir2 = titlesRedirectMap2.get(entityFromList1);
					if(redir2!=null){
						if(redir2.equalsIgnoreCase(entityFromList2)){
							Pwriter.println("M="+keyMention1+ "\tE_bef="+valueEntities1.get(0)+"\t"+"E_id:" + EntID1 +"\tE_after="+valueEntities2.get(0)+"\t"+"E_id:" + EntID2);// + "\tMAD:"+Math.abs((prior_before-prior_after)/2));
							continue;
						}else{
						}
					}
					redir2 = titlesRedirectMap2.get(entityFromList2);
					if(redir2!=null){
						if(redir2.equalsIgnoreCase(entityFromList1)){
							Pwriter.println("M="+keyMention1+ "\tE_bef="+valueEntities1.get(0)+"\t"+"E_id:" + EntID1 +"\tE_after="+valueEntities2.get(0)+"\t"+"E_id:" + EntID2);// + "\tMAD:"+Math.abs((prior_before-prior_after)/2));
							continue;
						}else{
						}
					}
					// The entity has  REEEEEALLY changed
					//entityChanged++;
					double prior_before = Double.parseDouble(valueEntities1.get(0).split(" :=: ")[1].trim());
					double prior_after = Double.parseDouble(valueEntities2.get(0).split(" :=: ")[1].trim());
					Pwriter2.println("M="+keyMention1+ "\tE_bef="+valueEntities1.get(0)+"\t"+"E_id:" + EntID1 +"\tE_after="+valueEntities2.get(0)+"\t"+"E_id:" + EntID2);// + "\tMAD:"+Math.abs((prior_before-prior_after)/2));
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
		buffReader1 =  ut.getBufferedReaderForCompressedFile(inputFile1);
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
		buffReader2 = ut.getBufferedReaderForCompressedFile(inputFile2);
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

		//PrintWriter Pwriter3 = new PrintWriter("comparison_Results_NOTchanged_top5", "UTF-8");
		//PrintWriter Pwriter4= new PrintWriter("comparison_Results_changed_top5", "UTF-8");
		PrintWriter Pwriter5 = new PrintWriter("comparison_Results_changed_0.txt", "UTF-8");
		PrintWriter Pwriter6 = new PrintWriter("comparison_Results_changed_5.txt", "UTF-8");

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
				for (int i=0; i < list1Size; i++){ //***//
					String entityANDprior1 = listEntities1.get(i);
					String entity1only = entityANDprior1.split(" :=: ")[0].trim();
					//double prior1only = Double.parseDouble(entityANDprior1.split(" :=: ")[1].trim());
					for(int j=0; j<list2Size; j++){
					//for(String entityANDprior2: listEntities2){
						String entityANDprior2 = listEntities2.get(j);
						String entity2only = entityANDprior2.split(" :=: ")[0].trim();
						//double prior2only = Double.parseDouble(entityANDprior2.split(" :=: ")[1].trim());
						String EID1 = pageTitlesMap1.get(entity1only).trim();
						String EID2 = pageTitlesMap2.get(entity2only).trim();
						if( (entity2only.equalsIgnoreCase(entity1only) ) || (EID1.equalsIgnoreCase(EID2))  ){
							gotIt = true; //this means the second map has the entity from the first map. The entity names have not changed.
							//Pwriter3.println("MENTION="+keyMention1+"\tE_BEF="+entity1only+":"+prior1only+"\t"+"E_ID:" + pageTitlesMap1.get(entity1only)+"\tPOS="+(i+1)+"\tE_AFTER ="+entity2only+":"+prior2only+"\t"+"E_ID:" + pageTitlesMap2.get(entity2only) + "\tPOS="+(j+1)+"\tRATIO:"+Math.abs((prior1only-prior2only)/2));
							break;
						}else {
							String redir1 = titlesRedirectMap1.get(entity2only);
							if(redir1!=null){
								if(redir1.equalsIgnoreCase(entity1only)){
									gotIt = true;
									continue;
								}else{
								}
							}
							redir1 = titlesRedirectMap1.get(entity1only);
							if(redir1!=null){
								if(redir1.equalsIgnoreCase(entity2only)){
									gotIt = true;
									continue;
								}else{
								}
							}
							String redir2 = titlesRedirectMap2.get(entity1only);
							if(redir2!=null){
								if(redir2.equalsIgnoreCase(entity2only)){
									gotIt = true;
									continue;
								}else{
								}
							}
							redir2 = titlesRedirectMap2.get(entity2only);
							if(redir2!=null){
								if(redir2.equalsIgnoreCase(entity1only)){
									gotIt = true;
									continue;
								}else{
								}
							}
								//Pwriter1.println("MENTION = "+key1+ " \t E_BEF = "+entity1only+":"+prior1only+" \t "+"E_ID:" + pageTitlesMap1.get(entity1only) +" \t E_AFTER = "+entity2only+":"+prior2only+" \t "+"E_ID:" + pageTitlesMap2.get(entity2only) + " \t RATIO:"+prior1only/prior2only);
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
		//Pwriter3.flush();
		//Pwriter3.close();
		//Pwriter4.flush();
		//Pwriter4.close();
		Pwriter5.flush();
		Pwriter5.close();
		Pwriter6.flush();
		Pwriter6.close();

		PrintWriter pwrt = new PrintWriter(new File("top5EntityChanges"));
		for(Integer elem: topKChanged){
			pwrt.println(elem);
		}
		pwrt.flush();
		pwrt.close();
	}

	/**
	 *
	 *	This utility function as the name says only compares unambiguous mentions.
	 *  It requires the mention/Entity file with only unambigous mention/entity pairs.
	 *
	 * @param mentionEntity1		mentionEntityLinks_PRIOR_100_top5_SINGLETON.bz2
	 * @param pageTitles1			pagesTitles.tsv.bz2
	 * @param titlesRedirection1	pagesTitles_REDIRECT.json
	 * @param mentionEntity2
	 * @param pageTitles2
	 * @param titlesRedirection2
	 * @throws IOException
	 * @throws CompressorException
	 * @throws ParseException
	 */
	public void compareUnambiguous(String mentionEntity1, String pageTitles1, String titlesRedirection1, String mentionEntity2, String pageTitles2,String titlesRedirection2) throws IOException, CompressorException, ParseException{
		Utils ut = new Utils();
		BufferedReader buffReader1 = ut.getBufferedReaderForCompressedFile(mentionEntity1);
		//BufferedReader buffReader2 = new BufferedReader(new FileReader(new File(inputFile2)));
		BufferedReader buffReader2 = ut.getBufferedReaderForCompressedFile(mentionEntity2);

		HashMap<String,LinkedList<String>> mentionMap1 = new HashMap<>(); //map to store a mention and list of top5 disambiguations
		HashMap<String,LinkedList<String>> mentionMap2 = new HashMap<>(); //map to store a mention and list of top5 disambiguations

		//PriorOnlyModel POM  = new PriorOnlyModel();
		Map<String, String> titlesRedMap1 = ut.loadTitlesRedirectMap(titlesRedirection1);
		Map<String, String> titlesRedMap2 = ut.loadTitlesRedirectMap(titlesRedirection2);

		TreeMap<String,String> pageTitlesMap1 = new TreeMap<>();   //map to store the Entities and entities page ids
		TreeMap<String,String> pageTitlesMap2 = new TreeMap<>();	//map to store the Entities and entities page ids

		BufferedReader bfR1 = ut.getBufferedReaderForCompressedFile(pageTitles1);
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
		BufferedReader bfR2 = ut.getBufferedReaderForCompressedFile(pageTitles2);
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


		//Here I iterate over the Map file to start comparing changes for unambiguous mentions
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

}
