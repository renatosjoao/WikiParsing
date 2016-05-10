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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.l3s.statistics.Statistics;
import edu.jhu.nlp.wikipedia.PageCallbackHandler;
import edu.jhu.nlp.wikipedia.WikiPage;
import edu.jhu.nlp.wikipedia.WikiXMLParser;
import edu.jhu.nlp.wikipedia.WikiXMLParserFactory;

public class Utils {
	private static Pattern stylesPattern = Pattern.compile("\\{\\|.*?\\|\\}$", Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern refCleanupPattern = Pattern.compile("<ref>.*?</ref>", Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern commentsCleanupPattern = Pattern.compile("<!--.*?-->", Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern redirectPattern = Pattern.compile("#REDIRECT.*\\[\\[(.*?)\\]\\]",
			Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
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
	 * A function to merge to input files, sort the lines and write to output
	 * file
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
	 * This is a utility function to write only the top prior probability for
	 * each mention.
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
			Pwriter.println(pair.getKey() + " ;-; " + keys.toArray()[0].toString());
			it.remove();
		}
		Pwriter.flush();
		Pwriter.close();
		Compressor cp = new Compressor();
		cp.compressTxtBZ2(outputFile);
	}

	/**
	 * Utility function to calculate top-k = 5 ??? ranked prior probability of
	 * mentions/entity pairs
	 *
	 */
	public void calculatePRIOR_topK(String inputFile, String outputFile) throws IOException, CompressorException {
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
							if (k < 5) {
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
						if (k < 5) {
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
							if (k < 5) {
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
		System.out.println("Finished calculating top 5 prior probability in " + ((stop - start) / 1000.0) + " seconds.");

	}

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
	public Map<String, String> loadTitlesRedirectMap(String titlesMapJson)
			throws org.json.simple.parser.ParseException, IOException {
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
		System.out.println(
				"Finished calculating the frequency distribution in " + ((stop - start) / 1000.0) + " seconds.");

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
	 * This is a utility class to cut the mention/Entity file by frequency of
	 * mention as is done in the WIKIFY !
	 *
	 * It just takes as input mentionEntityLinks_SORTED_Freq.txt.bz2 and cuts
	 * off lines that show less than freq and the count of mentions.
	 *
	 * @param inputFile
	 *            mentionEntityLinks_SORTED_Freq.txt.bz2
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
	 * Utility function to count the frequency of a mention but not keeping
	 * repeated values
	 * 
	 * @param inputFile
	 * @param outFile
	 * @throws IOException
	 * 
	 *             i.e. ASA Hall of Fame Stadium; ASA Hall of Fame Stadium; 3
	 *             ASA; ASA; 2
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
		System.out.println(
				"Finished calculating the frequency count unique in " + ((stop - start) / 1000.0) + " seconds.");

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

	/***
	 *
	 * @param mentionEntityFile
	 * @param titles
	 * @throws IOException
	 * @throws CompressorException
	 */
	public void checkTitles(String mentionEntityFile, Set<String> titles, Map<String, String> treemap)
			throws IOException, CompressorException {
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
	 * : pageTitles_ALL.txt // All the pages titles in the dump
	 * pagesTitles_SPECIAL.txt // All the special pages titles in the dump
	 * pagesTitles_REDIRECT.txt // All the redirected pages titles
	 * pagesTitles.txt // All the articles titles ( no special pages and no
	 * redirections )
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
				@SuppressWarnings("unchecked")
				public void process(WikiPage page) {
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
						specialPagesTitlesList.add(pTitle);
					} else {
						allPagesTitlesList.add(pTitle + " ;-; " + pageID); // allPagesTitlesList
																			// excludesSpecial
																			// pages
						if (pTitle.length() == 0 || (pTitle == " ")) {
							EMPTY_TITLE_PAGES++;
						} else {
							if (pTitle.contains("(disambiguation)")) {
								DISAMBIGUATION_PAGE++;
							} else {
								String wikitext = page.getWikiText().trim();
								Matcher mRedirect = redirectPattern.matcher(wikitext);

								if (mRedirect.find()) {
									redirectPagesTitlesList.add(pTitle);
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
												redirectionTitleArray[0] = Character
														.toUpperCase(redirectionTitleArray[0]);
												redirectedTitle = new String(redirectionTitleArray);
												if (redirectedTitle.contains("#")) {
													if ((redirectedTitle.indexOf("#") != 0)
															&& (redirectedTitle.indexOf("#") != -1)) {
														redirectedTitle = redirectedTitle.substring(0,
																redirectedTitle.indexOf("#"));
													}
												}
												if (pTitle.contains("(disambiguation)")
														|| (redirectedTitle.contains("(disambiguation)"))) { // disambiguation
													continue;
												}
												pageTitlesMap.put(pTitle, redirectedTitle);
												JSONObject jobj = new JSONObject();
												jobj.put("redirect", redirectedTitle);
												jobj.put("title", pTitle);
												Jarray.add(jobj);
											} else {
												continue;
											}
										}
									}
									/***********/
								} else {// In case it is not #REDIRECT. So I am
										// getting the page title and adding to
										// list. This is the actual list of page
										// titles I am interested.
									ENTITYPAGE++;
									if (pTitle.contains("#")) {
										if ((pTitle.indexOf("#") != 0) && (pTitle.indexOf("#") != -1)) {
											pTitle = pTitle.substring(0, pTitle.indexOf("#"));
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

		// ***************************************************************************************//
		// Writing all pages titles to output file "pageTitles_ALL.txt"
		PrintWriter allPagesTitlesWriter = new PrintWriter("pageTitles_ALL.txt", "UTF-8");
		Collections.sort(allPagesTitlesList);
		allPagesTitlesWriter.println("<<< Total number of pages: " + TOTAL_PAGE_TITLE + " >>>");
		for (String title : allPagesTitlesList) {
			allPagesTitlesWriter.println(title);
		}
		allPagesTitlesWriter.flush();
		allPagesTitlesWriter.close();

		// ***************************************************************************************//
		// Writing ONLY SPECIAL pages titles to output file
		// "pageTitles_SPECIAL.txt"
		PrintWriter specialPagesTitlesWriter = new PrintWriter("pagesTitles_SPECIAL.txt", "UTF-8");
		Collections.sort(specialPagesTitlesList);
		specialPagesTitlesWriter.println("<<< Number of special pages titles: " + SPECIAL_PAGES + " >>>");
		for (String title : specialPagesTitlesList) {
			specialPagesTitlesWriter.println(title);
		}
		specialPagesTitlesWriter.flush();
		specialPagesTitlesWriter.close();

		// ***************************************************************************************//
		// Writing ONLY Entity pages titles( i.e. without redirection
		// (#REDIRECT) and without SPECIAL pages) to output file
		// "pageTitles.txt"
		PrintWriter pagesTitlesWriter = new PrintWriter("pagesTitles.txt", "UTF-8");
		Collections.sort(pagesTitlesList);
		pagesTitlesWriter.println("<<< Number of pages titles: " + ENTITYPAGE + " >>>");
		for (String title : pagesTitlesList) {
			pagesTitlesWriter.println(title);
		}
		pagesTitlesWriter.flush();
		pagesTitlesWriter.close();
		Compressor cp = new Compressor();
		cp.compressTxtBZ2("pagesTitles.txt");
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

	/***
	 * This utility function is meant to dump all mention entity pairs without
	 * checking if the entity is in the pages titles list or in the redirection
	 * map. It writes the a big file with all the mention/entity pairs.
	 * (Unsorted)
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
										if (mention.contains(":") || (entitylink.contains(":"))) { // ignoring
																									// rubbish
																									// such
																									// as
																									// Image:Kropotkin
																									// //
																									// Nadar.jpg]
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
										if (mention.contains(":") || (entitylink.contains(":"))) { // ignoring
																									// rubbish
																									// such
																									// as
																									// Image:Kropotkin
																									// //
																									// Nadar.jpg]
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
						// DO NOTHING if it is a Special Page ! Special pages
						// are pages such as Help: , Wikipedia:, User: pages
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
	 *
	 * @param title
	 * @return
	 */
	public boolean isSpecial(String title) {
		if (title.contains("Category:") || title.contains("Help:") || title.contains("Image:")
				|| title.contains("User:") || title.contains("MediaWiki:") || title.contains("Wikipedia:")
				|| title.contains("Portal:") || title.contains("Template:") || title.contains("File:")
				|| title.contains("Book:") || title.contains("Draft:") || title.contains("Module:")
				|| title.contains("TimedText:") || title.contains("Topic:")) {
			return true;
		} else {
			return false;
		}
	}
}
