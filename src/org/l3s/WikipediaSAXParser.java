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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.l3s.dbpedia.DBPediaType;
import org.l3s.test.Cli;

import edu.jhu.nlp.wikipedia.PageCallbackHandler;
import edu.jhu.nlp.wikipedia.WikiPage;
import edu.jhu.nlp.wikipedia.WikiTextParserException;
import edu.jhu.nlp.wikipedia.WikiXMLParser;
import edu.jhu.nlp.wikipedia.WikiXMLParserFactory;

public class WikipediaSAXParser {
	//private static Pattern categoryPattern = Pattern.compile("\\[\\["+ "Category" + ":(.*?)\\]\\]", Pattern.MULTILINE| Pattern.CASE_INSENSITIVE);
	//private static Pattern OtherPattern = Pattern.compile("\\[\\[[A-Z]+:(.*?)\\]\\]", Pattern.MULTILINE	| Pattern.CASE_INSENSITIVE);
    private static Pattern stylesPattern = Pattern.compile("\\{\\|.*?\\|\\}$", Pattern.MULTILINE | Pattern.DOTALL);
    private static Pattern infoboxCleanupPattern = Pattern.compile("\\{\\{infobox.*?\\}\\}$", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static Pattern curlyCleanupPattern0 = Pattern.compile("^\\{\\{.*?\\}\\}$", Pattern.MULTILINE | Pattern.DOTALL);
    private static Pattern curlyCleanupPattern1 = Pattern.compile("\\{\\{.*?\\}\\}", Pattern.MULTILINE | Pattern.DOTALL);
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
	private static final Pattern URL = Pattern.compile("http://[^ <]+");
	private static Pattern stubPattern = Pattern.compile("\\-\\}\\}",Pattern.CASE_INSENSITIVE);
	private static Pattern disambCatPattern = Pattern.compile("\\{\\{\\}\\}",Pattern.CASE_INSENSITIVE);
	private static Pattern mentionEntityPattern = Pattern.compile("\\[\\[(.*?)\\]\\]", Pattern.MULTILINE);
	
	private static final Pattern LANG_LINKS = Pattern.compile("\\[\\[[a-z\\-]+:[^\\]]+\\]\\]");
	private static final Pattern DOUBLE_CURLY = Pattern.compile("\\{\\{.*?\\}\\}");
	private static final Pattern HTML_TAG = Pattern.compile("<[^!][^>]*>");
	private static Pattern namePattern = Pattern.compile("^[a-zA-Z_  -]*",Pattern.MULTILINE);
	private static String pageTitles = "pageTitles.txt";
	private static String articlesMentionsANDLinks = "articlesMentionsANDLinks.txt";
	private static PrintWriter writer = null;// new
												// PrintWriter(articlesMentionsANDLinks,"UTF-8");

	private static int NOREDIRECTION = 0; 					//This is the total number of page titles, without ##REDIRECT
	private static int TOTAL_PAGE_TITLE = 0;			    //This is the total number of page titles, no matter if it is REDIRECT, SPECIAL, etc
	//				   TOTAL_PAGE_TITLE = REDIRECT_PAGE_TITLE + PAGE_TITLE
	private static int REDIRECTION = 0;				         //This is the total number of #REDIRECT page titles.
	private static int SPECIAL_PAGES =0;
	private static int EMPTY_TITLE_PAGES;
	private static int IN_TITLES_LIST = 0;
	private static int IN_MAP_KEYS = 0;
	private static int IN_MAP_VALUES= 0;
	private static int MENTION_ENTITY =0;
	private int DISAMB_PAGE = 0;
	private int LIST_PAGE = 0;
	private String[] args = null;
	private static Options options = new Options();
	private static Map<String, List<String>> pageTitlesMap = new TreeMap<String, List<String>>();

	private static List<String> pagesTitlesList = new LinkedList<String>(); 		//This is a list of pages titles without special pages (i.e. Category:, File:, Image:, etc) and without #REDIRECT
	private static List<String> allPagesTitlesList = new LinkedList<String>();   	//This is  a list with all the pages titles.
	private static List<String> specialPagesTitlesList = new LinkedList<String>();  //This is  a list with ONLY the SPECIAL pages titles. (i.e. Category:, File:, Image:, etc)
	private static List<String> redirectPagesTitlesList = new LinkedList<String>(); //This is  a list with ONLY the pages titles with redirection. (i.e. #REDIRECT)
	/**
	 * @throws ParseException
	 * @param args
	 * @throws
	 */
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException, ParseException {

		// CommandLineParser parser = new DefaultParser();
		// CommandLine cmd = parser.parse( options, args);

		// if(cmd.hasOption("p")) {
		// help();
		// print the date and time
		// }
		// if(cmd.hasOption("a")){
		// System.out.println("Run all the pipeline");
		// }
		// new Cli(args).parse();

		// if (args.length <1) {
		// System.out.println("Usage: Parser <Wikipedia-XML-FILE> <DBPedia-FILE>");
		// System.exit(-1);
		// }

		//File ff = new File(pageTitles);
		//// Basic verification whether the file with all the page titles exists
		//if (ff.exists()) {
		 //set = loadTitlesList(pageTitles);
		 //} else {
		writePageTitles(args[0]);
		//System.exit(1);
		// set = loadTitlesList(pageTitles);
		// }

		// Path path = Paths.get(articlesMentionsANDLinks);
		// if(!Files.exists(path)){
		writeMentionEntity(args[0],	articlesMentionsANDLinks);
		// }

		// sorting first
		//sortList("./resource/listOfMentionsAndEntities.txt","articlesMentionsANDLinks_SORTED.txt");
		sortList(articlesMentionsANDLinks,"articlesMentionsANDLinks_SORTED.txt");

		// frequency count
		frequencyCount("articlesMentionsANDLinks_SORTED.txt","articlesMentionsANDLinks_SORTED_Freq.txt");

		// frequency count without duplicates
		frequencyCountUnique("articlesMentionsANDLinks_SORTED.txt","articlesMentionsANDLinks_SORTED_Freq_Uniq.txt");

		// prior probability
		calculatePRIOR("articlesMentionsANDLinks_SORTED_Freq.txt","articlesMentionsANDLinks_PRIOR.txt");

	

	

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
					if(title.contains("Category:") || title.contains("Help:") || title.contains("Image:") ||title.contains("User:") || title.contains("MediaWiki:") || title.contains("Wikipedia:") || title.contains("Portal:") || title.contains("Template:") || title.contains("File:")
							|| title.length() == 0 || (title == " ")  || title.contains("Book:") || title.contains("Draft:") || title.contains("Module:") || title.contains("TimedText:") || title.contains("Topic:")) {
						//DO NOTHING if it is a Special Page ! Special pages are pages such as Help: , Wikipedia:, User: pages
						//DO NOTHING if it is an empty page title.

						//KATJA asked to run two experiments
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
									
										if (mention.length() == 0 || (mention == "")|| (entitylink.length() == 0)|| (entitylink == "")) {
											continue;
										}
										if (mention.contains(":") || (entitylink.contains(":"))) { // ignoring rubbish such as Image:Kropotkin // Nadar.jpg]
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
										//		IN_MAP_KEYS++;
										//		String mentionEntity = mention + ";"+ entitylink;
										//		writer.println(mentionEntity);
										//		continue;
										//		}else {
													String keyMapping = containsValue(pageTitlesMap,entitylink);
													if(keyMapping != null){
										//				IN_MAP_VALUES++;
														String mentionEntity = mention + " ; "+ keyMapping;
														writer.println(mentionEntity);
										//				continue;
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
			//notinlistwriter.flush();
			//notinlistwriter.flush();
			long stop = System.currentTimeMillis();
			System.out.println("Finished collecting articles Mentions and Entities Links in "+ ((stop - start) / 1000.0) + " seconds.");
			System.out.println("Number of mention/entities pairs : "+MENTION_ENTITY);
			//System.out.println("Number of entities matches in the map keys :"+IN_MAP_KEYS);
			//System.out.println("Number of entities matches in the map values : "+IN_MAP_VALUES);
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
				
		// for (Entry<K,V> e = getFirstEntry(); e != null; e = successor(e))
	    //       if (valEquals(value, e.value))
	    //            return true;
	    //    return false;
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
		BufferedReader buffReader1 = new BufferedReader(new FileReader(inputFile));// "articlesMentionsANDLinks_SORTED_Freq.txt"
		BufferedReader buffReader2 = new BufferedReader(new FileReader(inputFile));// "articlesMentionsANDLinks_SORTED_Freq.txt"
		PrintWriter Pwriter = new PrintWriter(outputFile, "UTF-8"); // "articlesMentionsANDLinks_PRIOR.txt"
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
		try {
			wxsp = WikiXMLParserFactory.getSAXParser(XMLFile);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}
		try {
			wxsp.setPageCallback(new PageCallbackHandler() {

				public void process(WikiPage page) {
					String pTitle = page.getTitle().trim();
					String wikitext = page.getWikiText().trim();
					Matcher mRedirect = redirectPattern.matcher(wikitext);
					if(pTitle.contains("Category:") || pTitle.contains("Help:") || pTitle.contains("Image:") ||	pTitle.contains("User:") || pTitle.contains("MediaWiki:") || pTitle.contains("Wikipedia:") || pTitle.contains("Portal:") || pTitle.contains("Template:") || pTitle.contains("File:") ||
							pTitle.contains("Book:") || pTitle.contains("Draft:") || pTitle.contains("Module:") || pTitle.contains("TimedText:") || pTitle.contains("Topic:") ){
						SPECIAL_PAGES++;
						specialPagesTitlesList.add(pTitle);
					}else{
						allPagesTitlesList.add(pTitle); // allPagesTitlesList excludes and Special pages
						TOTAL_PAGE_TITLE++;
					if (pTitle.length() == 0 || (pTitle == " ")) {
						EMPTY_TITLE_PAGES++;
						//empty title //// useless
						}else{
							if (mRedirect.find()) {
								redirectPagesTitlesList.add(pTitle);
								REDIRECTION++;
/********/
							 Matcher matcher = mentionEntityPattern.matcher(wikitext);	
							 while(matcher.find()){
								 String redirectedTitle = matcher.group(1).trim();
								 if(redirectedTitle.contains("Category:")){
									 continue;

								 }else{
									 List<String> mappedList = pageTitlesMap.get(redirectedTitle); 							
									 if(mappedList == null){ 	
										 mappedList = new ArrayList<String>();
										 mappedList.add(pTitle);
										 pageTitlesMap.put(redirectedTitle,mappedList);
									 }else{
										 mappedList.add(pTitle);
										 pageTitlesMap.put(redirectedTitle,mappedList);
									 }
								 }
							 }
/***********/
							}else{// In case it is not #REDIRECT. So I am getting the page title and adding to list. This is the actual list of page titles I am interested.
								NOREDIRECTION++;
								pagesTitlesList.add(pTitle);
							}
						}
					}
				}
			});
			wxsp.parse();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		//Writing ONLY pages titles with redirection (#REDIRECT) to output file "pageTitles_REDIRECT.txt"
		PrintWriter redirectPagesTitlesWriter = new PrintWriter("pagesTitles_REDIRECT.txt", "UTF-8");
		Collections.sort(redirectPagesTitlesList);
		redirectPagesTitlesWriter.println("<<< Number of redirected pages title: "+REDIRECTION+" >>>");
		for(String title:redirectPagesTitlesList){
			redirectPagesTitlesWriter.println(title);
		}
		redirectPagesTitlesWriter.flush();
		redirectPagesTitlesWriter.close();

		//***************************************************************************************//
		//Writing ONLY pages titles without redirection (#REDIRECT) and without SPECIAL pages to output file "pageTitles.txt"
		PrintWriter pagesTitlesWriter = new PrintWriter("pagesTitles.txt", "UTF-8");
		Collections.sort(pagesTitlesList);
		pagesTitlesWriter.println("<<< Number of pages titles: "+NOREDIRECTION+" >>>");
		for(String title:pagesTitlesList){
			pagesTitlesWriter.println(title);
		}
		pagesTitlesWriter.flush();
		pagesTitlesWriter.close();

		//***************************************************************************************//
		long stop = System.currentTimeMillis();
		
		
		ObjectMapper jsonMapper = new ObjectMapper();
		try {
			String outputJSON = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(pageTitlesMap);
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
		
		System.out.println("Finished writing page titles files in "+ ((stop - start) / 1000.0) + " seconds.");
		System.out.println("Number of SPECIAL pages titles  : "+SPECIAL_PAGES);
		System.out.println("Number of #REDIRECTION pages titles: "+REDIRECTION);
		System.out.println("Number of empty pages titles : "+EMPTY_TITLE_PAGES);
		System.out.println("Number of pages titles without #REDIRECTION :" +NOREDIRECTION);
		System.out.println("Total number of pages  <<<< Excluding SPECIAL pages.>>>> :" +TOTAL_PAGE_TITLE);
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
			String[] words = inpLine.split(";");
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

}