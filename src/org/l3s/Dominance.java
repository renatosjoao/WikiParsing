package org.l3s;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.compress.compressors.CompressorException;

public class Dominance {

	public Dominance() {
		super();
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args){
		Dominance dm = new Dominance();
		try {
			dm.calculateDominance(args[0], args[1], args[2], args[3]);
		} catch (CompressorException | IOException e) {
			e.printStackTrace();
		}
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
		Utils ut = new Utils();
		HashMap<String,LinkedList<String>> mentionMap1 = new HashMap<>(); //map to store a mention and list of top5 disambiguations
		HashMap<String,LinkedList<String>> mentionMap2 = new HashMap<>(); //map to store a mention and list of top5 disambiguations
		TreeMap<String,String> pageTitlesMap1 = new TreeMap<>();   //map to store the Entities and entities page ids
		TreeMap<String,String> pageTitlesMap2 = new TreeMap<>();	//map to store the Entities and entities page ids
		PrintWriter writerDom = new PrintWriter("./resource/dominance_2014_2016.tsv", "UTF-8");
		BufferedReader bfR1 = ut.getBufferedReaderForCompressedFile(pageTitles1);		//Here I am only reading the page titles list and adding to the pageTitlesMap1 that contains the entity and the entityID
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

		BufferedReader bfR2 = ut.getBufferedReaderForCompressedFile(pageTitles2); 		//Here I am only reading the page titles list and adding to the pageTitlesMap2 that contains the entity and the entityID
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

		BufferedReader buffReader1 = ut.getBufferedReaderForCompressedFile(inputFile1);
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
		BufferedReader buffReader2 = ut.getBufferedReaderForCompressedFile(inputFile2);
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


}
