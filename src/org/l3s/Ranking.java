package org.l3s;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.compress.compressors.CompressorException;

/**
 * @author Renato Stoffalette Joao
 * @mail renatosjoao@gmail.com
 * @version 1.0
 * @date 11.2016
 *
 */
public class Ranking {

	public static void main(String[] args){
		Ranking rk = new Ranking();
		try {
			rk.calculateRanking(args[0],args[1],args[2],args[3],args[4],args[5]);
		} catch (IOException | CompressorException e) {
			e.printStackTrace();
		}
	}

	public Ranking() {
		super();
	}

	/**********************************************************************************************************/
	/**
	 * This function is meant to calculate Ranking ( Spearman , Kendall tau )
	 *
	 * @param inputFile1  mentionEntityLinks_PRIOR_100_top5
	 * @param pageTitles1
	 * @param inputFile2
	 * @param pageTitles2
	 * @throws IOException
	 * @throws CompressorException
	 */

	public void calculateRanking(String inputFile1, String pageTitles1, String inputFile2, String pageTitles2,String year1, String year2) throws IOException, CompressorException{
		Utils tu = new Utils();
		//BufferedReader buffReader1 = new BufferedReader(new FileReader(new File(inputFile1)));
		BufferedReader buffReader1 = tu.getBufferedReaderForCompressedFile(inputFile1);
		//BufferedReader buffReader2 = new BufferedReader(new FileReader(new File(inputFile2)));
		BufferedReader buffReader2 = tu.getBufferedReaderForCompressedFile(inputFile2);

		HashMap<String,LinkedList<String>> mentionMap1 = new HashMap<>(); //map to store a mention and list of top5 disambiguations
		HashMap<String,LinkedList<String>> mentionMap2 = new HashMap<>(); //map to store a mention and list of top5 disambiguations

		TreeMap<String,String> pageTitlesMap1 = new TreeMap<>();   //map to store the Entities and entities page ids
		TreeMap<String,String> pageTitlesMap2 = new TreeMap<>();	//map to store the Entities and entities page ids

		//PrintWriter writerSpearman = new PrintWriter("./resource/Spearman.tsv", "UTF-8");
		PrintWriter writerKendall = new PrintWriter("./resource/Kendall_"+year1+"_"+year2+".tsv", "UTF-8");

		//Here I am only reading the page titles list and adding to the pageTitlesMap1 that contains the entity and the entityID
		//BufferedReader bfR1 = new BufferedReader(new FileReader(new File(pageTitles1)));
		BufferedReader bfR1 = tu.getBufferedReaderForCompressedFile(pageTitles1);
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
		BufferedReader bfR2 = tu.getBufferedReaderForCompressedFile(pageTitles2);
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
}