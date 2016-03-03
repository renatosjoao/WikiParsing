package org.l3s;

/**
 * @author Renato Stoffalette Joao
 * @mail renatosjoao@gmail.com
 * @version 1.0
 * @date 02.2016
 * 
 * 
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.l3s.statistics.Statistics;

import edu.jhu.nlp.wikipedia.PageCallbackHandler;
import edu.jhu.nlp.wikipedia.WikiPage;
import edu.jhu.nlp.wikipedia.WikiXMLParser;
import edu.jhu.nlp.wikipedia.WikiXMLParserFactory;

public class WikipediaSAXParser {
	//private static Pattern categoryPattern = Pattern.compile("\\[\\["+ "Category" + ":(.*?)\\]\\]", Pattern.MULTILINE| Pattern.CASE_INSENSITIVE);
	//private static Pattern OtherPattern = Pattern.compile("\\[\\[[A-Z]+:(.*?)\\]\\]", Pattern.MULTILINE	| Pattern.CASE_INSENSITIVE);
    private static Pattern stylesPattern = Pattern.compile("\\{\\|.*?\\|\\}$", Pattern.MULTILINE | Pattern.DOTALL);
    //private static Pattern infoboxCleanupPattern = Pattern.compile("\\{\\{infobox.*?\\}\\}$", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    //private static Pattern curlyCleanupPattern0 = Pattern.compile("^\\{\\{.*?\\}\\}$", Pattern.MULTILINE | Pattern.DOTALL);
    //private static Pattern curlyCleanupPattern1 = Pattern.compile("\\{\\{.*?\\}\\}", Pattern.MULTILINE | Pattern.DOTALL);
	//private static Pattern curlyCleanupPattern0 = Pattern.compile("^\\{\\{.*?\\}\\}$", Pattern.MULTILINE | Pattern.DOTALL);
	//private static Pattern curlyCleanupPattern1 = Pattern.compile("\\{\\{.*?\\}\\}", Pattern.MULTILINE | Pattern.DOTALL);
    private static Pattern refCleanupPattern = Pattern.compile("<ref>.*?</ref>", Pattern.MULTILINE | Pattern.DOTALL);
	// private static Pattern cleanupPattern0 =// Pattern.compile("^\\[\\[.*?:.*?\\]\\]$", Pattern.MULTILINE |// Pattern.DOTALL);
	// private static Pattern cleanupPattern1 =// Pattern.compile("\\[\\[(.*?)\\]\\]", Pattern.MULTILINE | Pattern.DOTALL);
	//private static Pattern refCleanupPattern = Pattern.compile("<ref>.*?</ref>", Pattern.MULTILINE | Pattern.DOTALL);
	//private static Pattern refCleanupPattern1 = Pattern.compile("<ref.*?>.*?</ref>", Pattern.MULTILINE | Pattern.DOTALL);
	//private static Pattern refCleanupPattern1 = Pattern.compile("<ref.*?/.*?>", Pattern.MULTILINE | Pattern.DOTALL);
	//private static Pattern refCleanupPattern2 = Pattern.compile("<ref.*?/>", Pattern.MULTILINE | Pattern.DOTALL);
	//private static Pattern testtranslatedTitle = Pattern.compile("\\[\\[[a-z]+:(.*?)\\]\\]");
	//private static Pattern translatedTitle = Pattern.compile("^\\[\\[[a-z-]+:(.*?)\\]\\]$", Pattern.MULTILINE);
    private static Pattern commentsCleanupPattern = Pattern.compile("<!--.*?-->", Pattern.MULTILINE | Pattern.DOTALL);
    //private static Pattern htmlExtLinkPattern = Pattern.compile("\\[http:\\/\\/(.*?)\\]",Pattern.MULTILINE | Pattern.DOTALL);
	// private static Pattern redirectPattern = Pattern.compile("#\\s*\\[\\[(.*?)\\]\\]", Pattern.CASE_INSENSITIVE);
	private static Pattern redirectPattern = Pattern.compile("#REDIRECT.*\\[\\[(.*?)\\]\\]", Pattern.MULTILINE| Pattern.CASE_INSENSITIVE);
	//private static final Pattern URL = Pattern.compile("http://[^ <]+");
	//private static Pattern stubPattern = Pattern.compile("\\-\\}\\}",Pattern.CASE_INSENSITIVE);
	//private static Pattern disambCatPattern = Pattern.compile("\\{\\{\\}\\}",Pattern.CASE_INSENSITIVE);
	private static Pattern mentionEntityPattern = Pattern.compile("\\[\\[(.*?)\\]\\]", Pattern.MULTILINE);
	//private static final Pattern LANG_LINKS = Pattern.compile("\\[\\[[a-z\\-]+:[^\\]]+\\]\\]");
	//private static final Pattern DOUBLE_CURLY = Pattern.compile("\\{\\{.*?\\}\\}");
	//private static final Pattern HTML_TAG = Pattern.compile("<[^!][^>]*>");
	//private static Pattern namePattern = Pattern.compile("^[a-zA-Z_  -]*",Pattern.MULTILINE);
	private static String pageTitles = "pagesTitles.txt";
	private static String mentionEntityLinks = "mentionEntityLinks.txt";
	private static PrintWriter writer = null;// new
												// PrintWriter(mentionEntityLinks,"UTF-8");

	private static int ENTITYPAGE = 0; 					//This is the total number of page titles, without ##REDIRECT
	private static int TOTAL_PAGE_TITLE = 0;			    //This is the total number of page titles, no matter if it is REDIRECT, SPECIAL, etc
	//				   TOTAL_PAGE_TITLE = REDIRECT_PAGE_TITLE + PAGE_TITLE
	private static int REDIRECTION = 0;				         //This is the total number of #REDIRECT page titles.
	private static int SPECIAL_PAGES =0;
	private static int EMPTY_TITLE_PAGES;
	private static int IN_TITLES_LIST = 0;
	private static int DISAMBIGUATION_PAGE = 0;
	private static int MENTION_ENTITY = 0;
	private static int NOMATCH = 0;
	private static int MATCH = 0;
	private static int TOTAL_MENTION = 0;
	private static int mentionEntityPairs = 0;
	private int DISAMB_PAGE = 0;
	private int LIST_PAGE = 0;
	private static Options options = new Options();
	private static Map<String, String> pageTitlesMap = new TreeMap<String,String>();
	private static Map<String,Integer> duplicatePageTitle = new TreeMap<String, Integer>();
	private static Set<String> titlesSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
	private static List<String> pagesTitlesList = new LinkedList<String>(); 		//This is a list of pages titles without special pages (i.e. Category:, File:, Image:, etc) and without #REDIRECT
	private static List<String> allPagesTitlesList = new LinkedList<String>();   	//This is  a list with all the pages titles.
	private static List<String> specialPagesTitlesList = new LinkedList<String>();  //This is  a list with ONLY the SPECIAL pages titles. (i.e. Category:, File:, Image:, etc)
	private static List<String> redirectPagesTitlesList = new LinkedList<String>(); //This is  a list with ONLY the pages titles with redirection. (i.e. #REDIRECT)

	/**
	 * @throws org.json.simple.parser.ParseException
	 * @throws ParseException
	 * @param args
	 * @throws
	 */
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException, ParseException, org.json.simple.parser.ParseException {
		/**
		 *  * Initially I want to check if the page titles file exists. If it does not exist in the current directory. I will create it.
		 */
		File pgFile = new File(pageTitles);
		if (pgFile.exists()) {
			titlesSet = loadTitlesList(pageTitles);
		 } else {
			writePageTitles(args[0]);
			titlesSet = loadTitlesList(pageTitles);
		}
		/**
		 *  *  I create the mention/entity file without checking whether the entity has a proper entity page.
		 */
		writeMentionEntity_NO_Checking(args[0],	mentionEntityLinks);
		/**
		 *  *  Now I want to check if the page titles redirection map file exists. If it does not exist in the current directory. I will create it.
		 */
		File mpFile = new File("pagesTitles_REDIRECT.json");
		if (mpFile.exists()) {
			pageTitlesMap = loadTitlesRedirectMap("pagesTitles_REDIRECT.json");
		}else{
			writePageTitles(args[0]);
			pageTitlesMap = loadTitlesRedirectMap("pagesTitles_REDIRECT.json");
			}
		/**
		 * * Then I check my mention/entity list against the page titles list and the page titles redirection map.
		 */
		checkTitles(mentionEntityLinks,titlesSet,pageTitlesMap);

		/**
		 * * Counting the frequency for mention/entity pairs.
		 */
		frequencyCount("mentionEntityLinks_SORTED.txt","mentionEntityLinks_SORTED_Freq.txt");

		/**
		 * * Counting the frequency for mention/entity pairs WITHOUT duplicates.
		 */
		frequencyCountUnique("mentionEntityLinks_SORTED.txt","mentionEntityLinks_SORTED_Freq_Uniq.txt");

		/**
		 * * Finally calculating PRIOR probability for mention/entity pairs.
		 */
		calculatePRIOR("mentionEntityLinks_SORTED_Freq.txt","mentionEntityLinks_PRIOR.txt");

	}
/********************************** END OF MAIN ************************************************************/

	/***
	 *
	 * @param mentionEntityFile
	 * @param titles
	 * @throws IOException
	 */
	public static void checkTitles(String mentionEntityFile,Set<String> titles, Map<String,String> treemap) throws IOException{
		long start = System.currentTimeMillis();
		ArrayList<String> finalList = new ArrayList<String>();
		BufferedReader bffReader = new BufferedReader(new FileReader(mentionEntityFile));
		PrintWriter notAMatchtFileWriter = new PrintWriter(new File("mentionEntityLinks_NOT_MATCHED.txt"));

		String inLine = null;
		while ((inLine = bffReader.readLine()) != null) {
			TOTAL_MENTION+=1;
			String[] aM = inLine.split(" ; ");
			if(treemap.containsKey(aM[1])){
				if( (treemap.get(aM[1]) == " ") || (treemap.get(aM[1]).trim().length() == 0) ){
					continue;
				}else{
					finalList.add(aM[0] + " ; " + treemap.get(aM[1]) );
					MATCH+=1;
				}
			}else{
			if(titles.contains(aM[1])){
				finalList.add(inLine);
				MATCH+=1;
			}else{
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
		for(String str : finalList){
			outputFileWriter.println(str);
		}
		outputFileWriter.flush();
		outputFileWriter.close();
		long stop = System.currentTimeMillis();
		Statistics st = new Statistics();
		st.writeMentionEntityStatistics((stop - start) / 1000.0,MATCH, NOMATCH);

	}

	/***
	 *	Utility function to check whether an entity is in the map file and solving the redirection.
	 *
	 * @param mentionEntityFile
	 * @param treemap
	 * @throws IOException
	 */
	public static void checkInTitlesMap(String mentionEntityFile, Map<String,String> treemap) throws IOException{
		System.out.println("Checking if entity matches the entity page redirection map ...");
		int total = 0;
		int match = 0;
		BufferedReader bffReader = new BufferedReader(new FileReader(mentionEntityFile));
		PrintWriter ptemp2File = new PrintWriter(new File("mentionEntity.temp2"));
		String inLine = null;
		while ((inLine = bffReader.readLine()) != null) {
			total+=1;
			String[] aM = inLine.split(" ; ");
			if(treemap.containsKey(aM[1])){
				if( (treemap.get(aM[1]) == " ") || (treemap.get(aM[1]).trim().length() == 0) ){
					continue;
				}else{
					ptemp2File.println(aM[0]+" ; "+treemap.get(aM[1]));
					match+=1;
				}
			}
		}
		bffReader.close();
		ptemp2File.flush();
		ptemp2File.close();
		System.out.println("Total titles : "+total);
		System.out.println("Number of matches : "+match);
		System.out.println("Number of non matches : "+(total-match));
		System.out.println("Done.");
	}

	/***
	 *  A function to merge to input files, sort the lines and write to output file
	 *
	 * @param inputFile1
	 * @param inputFile2
	 * @param outputFile
	 * @throws IOException
	 */
	public static void mergeFilesAndSort(String inputFile1, String inputFile2, String outputFile) throws IOException{
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
		for(String str : finalList){
			outputFileWriter.println(str);
		}
		outputFileWriter.flush();
		outputFileWriter.close();
	}

	/**
	 *  Function to compress input file using Bzip2
	 *
	 * @param inputFile
	 * @throws IOException
	 */
	private static void compressTxtBZ2(String inputFile) throws IOException {
		Process p = null;
		try {
			p = Runtime.getRuntime().exec("bzip2 " + inputFile);
			p.waitFor();
			p.destroy();
		} catch (Exception e) {
		}
	}

	/**
	 * Function to write the mentions and the entities to disk
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 * 
	 */
	public static void writeMentionEntity(String inputFile, String outputFile) throws FileNotFoundException, UnsupportedEncodingException {
		writer = new PrintWriter(outputFile, "UTF-8");
		//PrintWriter notinlistwriter = new PrintWriter("Link_NOT_in_LIST.txt","UTF-8");
		long start = System.currentTimeMillis();
		WikiXMLParser wxsp = null;
		try {
			wxsp = WikiXMLParserFactory.getSAXParser(inputFile);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}
		try {
			wxsp.setPageCallback(new PageCallbackHandler() {
				public void process(WikiPage page) {
					String wikitext = page.getWikiText().trim();
					String text = getPlainText(wikitext);
					String title = page.getTitle().trim();
					Matcher mRedirect = redirectPattern.matcher(wikitext);
					if(isSpecial(title)) {
						//DO NOTHING if it is a Special Page ! Special pages are pages such as Help: , Wikipedia:, User: pages
						//DO NOTHING if it is an empty page title.

						// 1.counting the mention/entity from disambiguation pages (i.e. Car (Disambiguation)
						// 2.NOT counting the mention/entity from  disambiguation pages
						}else{

							if(mRedirect.find()) {
								//DO NOTHING if it is redirect page !	
								
							}else{
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
									
										if (mention.length() == 0 || (mention == " ")|| (entitylink.length() == 0)|| (entitylink == " ")) {
											continue;
										}
										if (mention.contains(":") || (entitylink.contains(":"))) { // ignoring rubbish such as Image:Kropotkin // Nadar.jpg]
											continue;
										}
										if(entitylink.contains("#")){
											continue;
										}
										//MENTION_ENTITY++;
										//if(allPagesTitlesList.contains(entitylink)){
										if(pagesTitlesList.contains(entitylink)){
											//IN_TITLES_LIST++;
											String mentionEntity = mention.trim() + " ; "+ entitylink.trim();
											writer.println(mentionEntity);
											//continue;
											//}else{
												//entitylink is not in titleList
												//notinlistwriter.println(entitylink);
										}else{ //if (pageTitlesMap.get(entitylink)!=null) {
												String keyMapping = pageTitlesMap.get(entitylink);
												if(keyMapping != null){
													String mentionEntity = mention + " ; "+ keyMapping;
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
			System.out.println("Finished collecting articles Mentions and Entities Links in "+ ((stop - start) / 1000.0) + " seconds.");
			System.out.println("Number of mention/entities pairs : "+MENTION_ENTITY);
			System.out.println("Number of entities matches in the Titles list : "+IN_TITLES_LIST);
	}

	
	public static String containsValue(Map<String, List<String>> pageTitlesMap2, String value) {
		 Iterator<?> it = pageTitlesMap2.entrySet().iterator();
			while (it.hasNext()) {
				@SuppressWarnings("rawtypes")
				Map.Entry pair = (Map.Entry) it.next();
				List<String> lTemp = (ArrayList<String>) pair.getValue();
				for(String str : lTemp){
					if(str.equalsIgnoreCase(value)){
						return (String) pair.getKey();
					}
				}
			}
			return null;
	 }

	/**
	 * Utility function to calculate the prior probability of mentions, entity
	 * pairs
	 * 
	 * @param inputFile
	 *            The input file must be the sorted file with frequencies count
	 * 
	 *            (i.e)
	 * 
	 *            ASA (automobile); ASA (automobile); 1; ASA Aluminium Body; ASA
	 *            Aluminium Body; 2; ASA Aluminium Body; ASA Aluminium Body; 2;
	 *            ASA Hall of Fame Stadium; ASA Hall of Fame Stadium; 3; ASA
	 *            Hall of Fame Stadium; ASA Hall of Fame Stadium; 3; ASA Hall of
	 *            Fame Stadium; ASA Hall of Fame Stadium; 3;
	 * 
	 * 
	 *            Produces output such as
	 * 
	 *            Alan Williams; Alan Williams (actor); 0.08333333333333333 Alan
	 *            Williams; Alan Williams (criminal); 0.027777777777777776 Alan
	 *            Williams; Alan Williams (musician); 0.027777777777777776 Alan
	 *            Williams; Alan Williams; 0.8333333333333334 Alan Williams;
	 *            Alan Williams (New Zealand diplomat); 0.027777777777777776
	 *
	 * @param outputFile
	 * @throws IOException
	 */
	public static void calculatePRIOR(String inputFile, String outputFile)throws IOException {
		long start = System.currentTimeMillis();
		BufferedReader buffReader1 = new BufferedReader(new FileReader(inputFile));// "mentionEntityLinks_SORTED_Freq.txt"
		BufferedReader buffReader2 = new BufferedReader(new FileReader(inputFile));// "mentionEntityLinks_SORTED_Freq.txt"
		PrintWriter Pwriter = new PrintWriter(outputFile, "UTF-8"); // "mentionEntityLinks_PRIOR.txt"
		String line1 = null;
		String line2 = buffReader2.readLine();
		Map<String, Double> priorMap = new HashMap<String, Double>();
		while ((line1 = buffReader1.readLine()) != null) {
			line2 = buffReader2.readLine();
			if (line2 != null) {
				String[] elements1 = line1.split(" ; ");
				String[] elements2 = line2.split(" ; ");
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
							Pwriter.println(elements1[0].trim() + " ; " + pair.getKey().toString().trim()+ " ; " + priorProb.toString().trim());
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
							Pwriter.println(elements1[0].trim() + " ; " + pair.getKey().toString().trim()	+ " ; " + priorProb.toString().trim());
							it.remove();
						}
						continue;
					}
				}

			} else {
				String[] elements1 = line1.split(" ; ");
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
						Pwriter.println(elements1[0].trim() + " ; " + pair.getKey().toString().trim()	+ " ; " + priorProb.toString().trim());
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
						Pwriter.println(elements1[0].trim() + " ; " + pair.getKey().toString().trim()	+ " ; " + priorProb.toString().trim());
					}
				}
			}
		}
		buffReader1.close();
		buffReader2.close();
		Pwriter.flush();
		Pwriter.close();

		long stop = System.currentTimeMillis();
		System.out.println("Finished calculating prior probability in "	+ ((stop - start) / 1000.0) + " seconds.");

	}

	/**
	 * Utility function to receive an input file sort the elements and write
	 * back to disk
	 * 
	 * @param inputListFile
	 * @param outputListFile
	 * @throws IOException
	 */
	private static void sortList(String inputListFile, String outputListFile) throws IOException {
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
	 * This function writes the page titles to output file. 
	 * It writes  the files :			pageTitles_ALL.txt				// All the pages titles in the dump
	 * 									pagesTitles_SPECIAL.txt			// All the special pages titles  in the dump
	 *									pagesTitles_REDIRECT.txt		// All the redirected pages titles
	 *									pagesTitles.txt					// All the articles titles ( no special pages and no redirections )
	 * @param XMLFile
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 */
	private static void writePageTitles(String XMLFile)	throws FileNotFoundException, UnsupportedEncodingException {
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
				@SuppressWarnings("unchecked")
				public void process(WikiPage page) {
					String pTitle = page.getTitle().replaceAll("_"," ").trim();
					char[] pTitleArray = pTitle.trim().toCharArray();
					if(pTitleArray.length>0){
						pTitleArray[0] = Character.toUpperCase(pTitleArray[0]);
						pTitle = new String(pTitleArray);
					}
					TOTAL_PAGE_TITLE++;
					if(isSpecial(pTitle)){
						SPECIAL_PAGES++;
						specialPagesTitlesList.add(pTitle);
					}else{
						allPagesTitlesList.add(pTitle); // allPagesTitlesList excludesSpecial pages
					if (pTitle.length() == 0 || (pTitle == " ")) {
						EMPTY_TITLE_PAGES++;
					}else{
						if(pTitle.contains("(disambiguation)")){
							DISAMBIGUATION_PAGE++;
						}else{
							String wikitext = page.getWikiText().trim();
							Matcher mRedirect = redirectPattern.matcher(wikitext);

							if (mRedirect.find()) {
								redirectPagesTitlesList.add(pTitle);
								REDIRECTION++;
/********/
							 Matcher matcher = mentionEntityPattern.matcher(wikitext);	
							 while(matcher.find()){
								 String redirectedTitle = matcher.group(1).replaceAll("_"," ").trim();
								 if(isSpecial(redirectedTitle)){
									 continue;
								 }else{
									 char[] redirectionTitleArray = redirectedTitle.toCharArray();
									 if(redirectionTitleArray.length > 0){
										 redirectionTitleArray[0] = Character.toUpperCase(redirectionTitleArray[0]);
										 redirectedTitle = new String(redirectionTitleArray);
										 if(redirectedTitle.contains("#")){
											if ((redirectedTitle.indexOf("#") != 0 ) && (redirectedTitle.indexOf("#") !=-1)){
											    redirectedTitle = redirectedTitle.substring( 0, redirectedTitle.indexOf("#") );
											}
										 }
										 pageTitlesMap.put(pTitle, redirectedTitle);
										 JSONObject jobj = new JSONObject();
										 jobj.put("redirect", redirectedTitle);
										 jobj.put("title", pTitle);
										 Jarray.add(jobj);
									 }else{
										 continue;
									 }
								 }
							 }
/***********/
							}else{// In case it is not #REDIRECT. So I am getting the page title and adding to list. This is the actual list of page titles I am interested.
								ENTITYPAGE++;
								if(pTitle.contains("#")){
									if ((pTitle.indexOf("#") != 0 ) && (pTitle.indexOf("#") !=-1)){
									    pTitle = pTitle.substring( 0, pTitle.indexOf("#") );
									}
								 }
								pagesTitlesList.add(pTitle);
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

		//***************************************************************************************//
		//Writing all pages titles to output file "pageTitles_ALL.txt"
		PrintWriter allPagesTitlesWriter = new PrintWriter("pageTitles_ALL.txt", "UTF-8");
		Collections.sort(allPagesTitlesList);
		allPagesTitlesWriter.println("<<< Total number of pages: "+TOTAL_PAGE_TITLE+" >>>");
		for(String title:allPagesTitlesList){
			allPagesTitlesWriter.println(title);
		}
		allPagesTitlesWriter.flush();
		allPagesTitlesWriter.close();

		//***************************************************************************************//
		//Writing ONLY SPECIAL pages titles to output file "pageTitles_SPECIAL.txt"
		PrintWriter specialPagesTitlesWriter = new PrintWriter("pagesTitles_SPECIAL.txt", "UTF-8");
		Collections.sort(specialPagesTitlesList); 
		specialPagesTitlesWriter.println("<<< Number of special pages titles: "+SPECIAL_PAGES+" >>>");
		for(String title:specialPagesTitlesList){
			specialPagesTitlesWriter.println(title);
		}
		specialPagesTitlesWriter.flush();
		specialPagesTitlesWriter.close();

		//***************************************************************************************//
		//Writing ONLY Entity pages titles( i.e.  without redirection (#REDIRECT) and without SPECIAL pages) to output file "pageTitles.txt"
		PrintWriter pagesTitlesWriter = new PrintWriter("pagesTitles.txt", "UTF-8");
		Collections.sort(pagesTitlesList);
		pagesTitlesWriter.println("<<< Number of pages titles: "+ENTITYPAGE+" >>>");
		for(String title:pagesTitlesList){
			pagesTitlesWriter.println(title);
		}
		pagesTitlesWriter.flush();
		pagesTitlesWriter.close();

		//***************************************************************************************//

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
		st.writeTitlesStatistics(((stop - start) / 1000.0), SPECIAL_PAGES, REDIRECTION,DISAMBIGUATION_PAGE, ENTITYPAGE, duplicatePageTitle.size(),EMPTY_TITLE_PAGES,TOTAL_PAGE_TITLE);
	}

	/**
	 * This is a utility method to load the file with the page titles into a
	 * TreeSet in memory. It is used to compare whether a pattern found in the
	 * input file matches a wikipedia page title
	 * 
	 * @param pageTitles
	 * @return
	 * @throws IOException
	 */
	public static Set<String> loadTitlesList(String pageTitles)	throws IOException {
		Set<String> set = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		BufferedReader bffReader = new BufferedReader(new FileReader(pageTitles));
		String inpLine = null;
		while ((inpLine = bffReader.readLine()) != null) {
			set.add(inpLine);
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
	public static Map<String,String> loadTitlesRedirectMap(String titlesMapJson) throws org.json.simple.parser.ParseException, IOException{
		Map<String, String> TitlesMap = new TreeMap<String,String>();
		FileReader reader = new FileReader(titlesMapJson);
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(reader);
		JSONArray array = (JSONArray)obj;
		int jsonSize =  array.size();
		for(int i=0; i< jsonSize; i++){
			JSONObject jobject = (JSONObject)array.get(i);
			String pageTitle = (String) jobject.get("title");
			String redirectPage = (String) jobject.get("redirect");
			TitlesMap.put(pageTitle, redirectPage);
        }
		return TitlesMap;
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
	public static void freqDistribution(String inputFile, String outputFile) throws NumberFormatException, IOException {
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
			String[] words = inpLine.split(" ; ");
			if (words != null) {
				String mention = null;
				String entity = null;
				double count = 0.0;
				mention = words[0].trim();
				entity = words[1].trim();
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
		System.out.println("Finished calculating the frequency distribution in "	+ ((stop - start) / 1000.0) + " seconds.");

	}

	/**
	 * Utility function to count the frequency of a mention no matter the entity
	 * it is assigned to .
	 * 
	 * @param inputFile
	 * @param outFile
	 * @throws IOException
	 * 
	 *             i.e. ASA Hall of Fame Stadium; ASA Hall of Fame Stadium; 3
	 *             ASA Hall of Fame Stadium; ASA; 3 ASA Hall of Fame Stadium;
	 *             ASA Hall of Fame; 3
	 */

	public static void frequencyCount(String inputFile, String outFile) throws IOException {
		long start = System.currentTimeMillis();
		FileReader fReader = new FileReader(new File(inputFile));
		BufferedReader bffReader = new BufferedReader(fReader);
		String inpLine = null;
		Map<String, Integer> frequency = new HashMap<String, Integer>();
		while ((inpLine = bffReader.readLine()) != null) {
			String[] words = inpLine.split(" ; ");
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
		fReader.close();
		PrintWriter pWriter = new PrintWriter(outFile, "UTF-8");
		FileReader fRead = new FileReader(new File(inputFile));
		BufferedReader buffReader = new BufferedReader(fRead);
		String inp = null;
		while ((inp = buffReader.readLine()) != null) {

			String[] keys = inp.split(" ; ");
			if (keys.length >= 1) {
				String key = keys[0].trim();
				if ((key != null) && (!key.isEmpty() && (key != ""))) {
					Integer value = frequency.get(key);
					pWriter.println(inp.trim() + " ; " + value.toString().trim());
				}
			}
		}
		buffReader.close();
		fRead.close();
		pWriter.flush();
		pWriter.close();
		long stop = System.currentTimeMillis();
		System.out.println("Finished calculating the frequency count in "+ ((stop - start) / 1000.0) + " seconds.");

	}

	/**
	 * Utility function to count the frequency of a mention but not keeping
	 * repeated values
	 * 
	 * @param inputFile
	 * @param outFile
	 * @throws IOException
	 * 
	 *             i.e. ASA Hall of Fame Stadium; ASA Hall of Fame Stadium; 3
	 *             ASA; ASA; 2
	 */

	public static void frequencyCountUnique(String inputFile, String outFile) throws IOException {
		long start = System.currentTimeMillis();
		FileReader fReader = new FileReader(new File(inputFile));
		BufferedReader bffReader = new BufferedReader(fReader);
		String inpLine = null;
		Set<String> treee = new TreeSet<String>();
		Map<String, Integer> frequency = new TreeMap<String, Integer>();
		while ((inpLine = bffReader.readLine()) != null) {
			String[] words = inpLine.split(" ; ");
			if (words.length >= 1) {
				String key_value = words[0].trim() + " ; " + words[1].trim();
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
		fReader.close();
		PrintWriter pWriter = new PrintWriter(outFile, "UTF-8");
		String inp = null;
		Iterator<?> it = frequency.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			pWriter.println(pair.getKey().toString().trim() + " ; " + pair.getValue().toString().trim());
			// treee.add(pair.getKey() + ";\t" +pair.getValue());
			it.remove();
		}
		pWriter.flush();
		pWriter.close();
		long stop = System.currentTimeMillis();
		System.out.println("Finished calculating the frequency count unique in " + ((stop - start) / 1000.0) + " seconds.");

	}

	/**
	 * Utility function to clean the wikipedia page and return clean text
	 * 
	 * @param wikiText
	 * @return
	 */

	public static String getPlainText(String wikiText) {
		String text = wikiText.replaceAll("&gt;", ">");
		text = text.replaceAll("&lt;", "<");
		text = text.replaceAll("&quot;", "\"");
		//text = text.replaceAll("&nbsp;", "");	
		text = text.replaceAll("\"","").replaceAll("\'''","").replaceAll("\''","");
        text = commentsCleanupPattern.matcher(text).replaceAll("");
        text = stylesPattern.matcher(text).replaceAll("");
        text = refCleanupPattern.matcher(text).replaceAll("");
        text = text.replaceAll("</?.*?>", "");
       // text = curlyCleanupPattern0.matcher(text).replaceAll("");
       // text = curlyCleanupPattern1.matcher(text).replaceAll("");
       
		return text.trim();
	}

	
	private static String stripInfoBox(String wikiText) {
		String INFOBOX_CONST_STR = "{{Infobox";
		int startPos = wikiText.indexOf(INFOBOX_CONST_STR);
		if (startPos < 0){
			INFOBOX_CONST_STR = "{{infobox";
			startPos = wikiText.indexOf(INFOBOX_CONST_STR);
		}
		if (startPos < 0)
			return wikiText;
		int bracketCount = 2;
		int endPos = startPos + INFOBOX_CONST_STR.length();
		for (; endPos < wikiText.length(); endPos++) {
			switch (wikiText.charAt(endPos)) {
			case '}':
				bracketCount--;
				break;
			case '{':
				bracketCount++;
				break;
			default:
			}
			if (bracketCount == 0)
				break;
		}

		if (bracketCount != 0) {
			//throw new WikiTextParserException("Malformed Infobox, couldn't match the brackets.");
		}
		wikiText = wikiText.substring(0, startPos-1) + wikiText.substring(endPos + 1);
		return wikiText;
	}
	/**
	 * 
	 * @param text
	 * @return
	 */
    private static String stripCite(String text) {
        String CITE_CONST_STR = "{{cite";
        int startPos = text.indexOf(CITE_CONST_STR);
        if (startPos < 0) return text;
        int bracketCount = 2;
        int endPos = startPos + CITE_CONST_STR.length();
        for (; endPos < text.length(); endPos++) {
            switch (text.charAt(endPos)) {
                case '}':
                    bracketCount--;
                    break;
                case '{':
                    bracketCount++;
                    break;
                default:
            }
            if (bracketCount == 0) break;
        }
        text = text.substring(0, startPos - 1) + text.substring(endPos);
        return stripCite(text);
    }
	
	
	

	private static void help() {
		HelpFormatter formater = new HelpFormatter();
		formater.printHelp("Main", options);
		System.exit(0);
	}


	/***
	 * 	This utility function is meant to dump all mention entity pairs without checking if the entity is in the pages titles list or in the redirection map.
	 *  It writes the a big file with all the mention/entity pairs. (Unsorted)
	 *
	 * @param inputFile
	 * @param outputFile
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public static void writeMentionEntity_NO_Checking(String inputFile, String outputFile) throws FileNotFoundException, UnsupportedEncodingException {
		long start = System.currentTimeMillis();
		writer = new PrintWriter(outputFile, "UTF-8");
		WikiXMLParser wxsp = null;
		try {
			wxsp = WikiXMLParserFactory.getSAXParser(inputFile);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}
		try {
			wxsp.setPageCallback(new PageCallbackHandler() {
				public void process(WikiPage page) {
					String wikitext = page.getWikiText().trim();
					String text = getPlainText(wikitext);
					String title = page.getTitle().replaceAll("_"," ").trim();

					char[] pTitleArray = title.trim().toCharArray();
					if(pTitleArray.length>0){
						pTitleArray[0] = Character.toUpperCase(pTitleArray[0]);
						title = new String(pTitleArray);
					}

					if(isSpecial(title)) {
						//DO NOTHING if it is a Special Page ! Special pages are pages such as Help: , Wikipedia:, User: pages
						//DO NOTHING if it is an empty page title.
					}else{
						if(title.contains("(disambiguation)")){
						//continue; I am not interested in Disambiguation
							}else{
								Matcher mRedirect = redirectPattern.matcher(wikitext);
								if(mRedirect.find()) {
									//DO NOTHING if it is redirect page !
								}else{
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
												entitylink = temp[0].replaceAll("_"," ").trim();
												mention = temp[1].trim();
											} else {
												entitylink = temp[0].replaceAll("_"," ").trim();
												//mention = temp[0].replaceAll("_"," ").trim();
												mention = temp[0].trim();
											}
											if (mention.length() == 0 || (mention == " ") || (entitylink.length() == 0)|| (entitylink == " ")) {
												continue;
											}
											if (mention.contains(":") || (entitylink.contains(":"))) { // ignoring rubbish such as Image:Kropotkin // Nadar.jpg]
												continue;
											}
											if (mention.contains("(disambiguation)") || (entitylink.contains("(disambiguation)"))) { // disambiguation
												continue;
											}
											if(entitylink.contains("#")){
												if ((entitylink.indexOf("#") != 0 ) && (entitylink.indexOf("#") != -1)){
													entitylink = entitylink.substring(0, entitylink.indexOf("#"));
												}
											}
											int spaceIndex = mention.indexOf("(");
											if ((spaceIndex != 0 ) && (spaceIndex!=-1)){
												mention = mention.substring(0, spaceIndex);
											}
											char[] entityLinkArray = entitylink.toCharArray();
											if(entityLinkArray.length>0){
												entityLinkArray[0] = Character.toUpperCase(entityLinkArray[0]);
												entitylink = new String(entityLinkArray);
												String mentionEntity = mention.trim() + " ; "+ entitylink.trim();
												writer.println(mentionEntity);
												mentionEntityPairs++;
											}else{
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
			PrintWriter duplicatePageTitlesWriter = new PrintWriter("pagesTitlesDuplicates.txt", "UTF-8");
			Iterator<?> it = duplicatePageTitle.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry) it.next();
				if((int) pair.getValue() >= 2){
					duplicatePageTitlesWriter.println(pair.getKey()+" : " +pair.getValue());
				}
				it.remove();
			}
			duplicatePageTitlesWriter.flush();
			duplicatePageTitlesWriter.close();
			Statistics st = new Statistics();
			st.writeMentionEntityStatistics((stop - start) / 1000.0,mentionEntityPairs);

	}

	/**
	 *
	 * @param title
	 * @return
	 */
	public static boolean isSpecial(String title) {
		if (title.contains("Category:") || title.contains("Help:")
				|| title.contains("Image:") || title.contains("User:")
				|| title.contains("MediaWiki:") || title.contains("Wikipedia:")
				|| title.contains("Portal:") || title.contains("Template:")
				|| title.contains("File:") || title.contains("Book:")
				|| title.contains("Draft:") || title.contains("Module:")
				|| title.contains("TimedText:") || title.contains("Topic:")) {
			return true;
		} else {
			return false;
		}
	}

}