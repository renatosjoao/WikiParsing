package org.l3s;

import info.bliki.wiki.dump.IArticleFilter;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.dump.WikiArticle;
import info.bliki.wiki.dump.WikiXMLParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.SAXException;

public class Wikiparser_OLD {
	private static Pattern categoryPattern = Pattern.compile("\\[\\["+"Category"+":(.*?)\\]\\]", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	private static Pattern commentsCleanupPattern = Pattern.compile("<!--.*?-->", Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern curlyCleanupPattern0 = Pattern.compile("^\\{\\{.*?\\}\\}$", Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern curlyCleanupPattern1 = Pattern.compile("\\{\\{.*?\\}\\}", Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern refCleanupPattern = Pattern.compile("<ref>.*?</ref>", Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern refCleanupPattern1 = Pattern.compile("<ref.*?>.*?</ref>", Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern translatedTitle = Pattern.compile("^\\[\\[[a-z-]+:(.*?)\\]\\]$", Pattern.MULTILINE);	
    private static Pattern stylesPattern = Pattern.compile("\\{\\|.*?\\|\\}$", Pattern.MULTILINE | Pattern.DOTALL);
    private static Pattern mentionEntityPattern = Pattern.compile("\\[\\[(.*?)\\]\\]", Pattern.MULTILINE);
    private static Pattern infoboxCleanupPattern = Pattern.compile("\\{\\{infobox.*?\\}\\}$", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static Pattern linkPattern = Pattern.compile("\\[\\[(.*?)\\]\\]", Pattern.MULTILINE);
    private static Pattern cleanupPattern0 = Pattern.compile("^\\[\\[.*?:.*?\\]\\]$", Pattern.MULTILINE | Pattern.DOTALL);
    private static Pattern cleanupPattern1 = Pattern.compile("\\[\\[(.*?)\\]\\]", Pattern.MULTILINE | Pattern.DOTALL);
	
 

	public static void main(String[] args) throws IOException, SAXException {
		long start = System.currentTimeMillis();
		String data = args[0];
		//String data = "template.xml";
		List<String> lineList = new ArrayList<String>();
		final PrintWriter writer2 = new PrintWriter("articlesMentionsANDLinks.txt", "UTF-8");
		WikiXMLParser parser = new WikiXMLParser(new File(data),new IArticleFilter() {
					@Override
					public void process(WikiArticle article, Siteinfo siteinfo)	throws SAXException {
						String wikiText = article.getText();
						wikiText = getPlainText(wikiText);
						//System.out.println(" ");
						//String text = wikiText.replaceAll("&gt;", ">");
				        //text = text.replaceAll("&lt;", "<");
				        //text = categoryPattern.matcher(text).replaceAll(" ");
				        //text = infoboxCleanupPattern.matcher(text).replaceAll(" ");
				        //text = commentsCleanupPattern.matcher(text).replaceAll(" ");
				        //text = stylesPattern.matcher(text).replaceAll(" ");
				        //text = refCleanupPattern.matcher(text).replaceAll(" ");
				        //text = text.replaceAll("</?.*?>", " ");
				        //text = curlyCleanupPattern0.matcher(text).replaceAll(" ");
				        //text = curlyCleanupPattern1.matcher(text).replaceAll(" ");
				        //text = translatedTitle.matcher(text).replaceAll(" ");
						
						/*
						Matcher catMatcher= categoryPattern.matcher(wikiText);
						while(catMatcher.find()){
							String temp = catMatcher.group();
							if(temp!=null){
							wikiText.replace(temp, "");	
							}
						}
						
						Matcher refCleanup0 = refCleanupPattern.matcher(wikiText); 
						while(refCleanup0.find()){
							String temp = refCleanup0.group();
							if(temp!=null){
							wikiText.replace(temp, "");	
							}
						}
						Matcher refCleanup1 = refCleanupPattern1.matcher(wikiText);
						while(refCleanup1.find()){
							String temp = refCleanup1.group();
							if(temp!=null){
							wikiText.replace(temp, "");	
							}
						}
						
						
						Matcher curlyMatcher0 = curlyCleanupPattern0.matcher(wikiText);
						while(curlyMatcher0.find()){
							String temp = curlyMatcher0.group();
							if(temp!=null){
							wikiText.replace(temp, "");	
							}
						}					
						
						Matcher curlyMatcher1 = curlyCleanupPattern1.matcher(wikiText);
						while(curlyMatcher1.find()){
							String temp = curlyMatcher1.group();
							if(temp!=null){
							wikiText.replace(temp, "");	
							}
						}
						
						Matcher transTitleMatcher = translatedTitle.matcher(wikiText);
						while(transTitleMatcher.find()){
							String temp = transTitleMatcher.group();
							if(temp!=null){
							wikiText.replace(temp, "");	
							}
						}
						
						
						Matcher styleMatcher = stylesPattern.matcher(wikiText);
						while(styleMatcher.find()){
							String temp = styleMatcher.group();
							if(temp!=null){
							wikiText.replace(temp, "");	
							}
							
						}
						Matcher commentsMatcher =  commentsCleanupPattern.matcher(wikiText);
						while(commentsMatcher.find()){
							String temp = commentsMatcher.group();
							if(temp!=null){
							wikiText.replace(temp, "");	
							}
						}						
						
						
						if ((text != null) && (!text.isEmpty()) && (text!="")) {
							
							
							Matcher matcher = mentionEntityPattern.matcher(text);
							
							while (matcher.find()) {
								String[] temp = matcher.group(1).split("\\|");
  								String mention = null;
								String link = null;
								if (temp == null || temp.length == 0) {
									continue;
								}
								if (temp.length > 1) {
									link = temp[0].trim();
									mention = temp[1].trim();
								} else {
									link = temp[0].trim();
									mention = temp[0].trim();
								}
								//mention = mention.replaceAll("'","").replaceAll("\"","").replaceAll("$","").replaceAll("!", "").replaceAll(";","");
								if (mention.length() == 0 || (mention==""))  {
									continue;
								}
								// ignoring rubbish such as [Image:Kropotkin Nadar.jpg]
								if (mention.contains(":")) {
									continue;
								}
								// ignoring if the first letter is not Uppercase
								// if
								// (!Character.isUpperCase(surfaceForm.charAt(0)))
								// {
								// continue;
								// }
								String mentionEntity = mention+ ";\t" +link;
								writer2.println(mentionEntity);
								//this is the list in which I store the elements to be sorted
								//lineList.add(mentionEntity);
								//System.out.println("Title : "+article.getTitle());
								//System.out.println(mentionEntity);
							}
						}
*/
					}
					    
				});
		parser.parse();
		writer2.close();
		
		long stop = System.currentTimeMillis();
		System.err.println("Finished parsing all pages from "+data+" in "+ ((stop - start) / 1000.0) + " seconds.");
		
		System.exit(1);
		
		
		start = System.currentTimeMillis();
		//FileReader fileReader = new FileReader(new File("articlesMentionsANDLinks.txt"));
		FileWriter fileWriter = null;
		PrintWriter out = null;
		
		//BufferedReader bufferedReader = new BufferedReader(fileReader);
		//
		//String inputLine;
		

		//while ((inputLine = bufferedReader.readLine()) != null) {
			
		//}
		
		
		//bufferedReader.close();
		if(!lineList.isEmpty()){
			Collections.sort(lineList);
		}else{
			System.out.println("lineList is empty");
			System.exit(1);
		}
		fileWriter = new FileWriter("articlesMentionsANDLinks_SORTED.txt");
		out = new PrintWriter(fileWriter);
		for (String outputLine : lineList) {
			out.println(outputLine);
		}
		out.flush();
		out.close();
		fileWriter.close();
	
		stop = System.currentTimeMillis();
		System.err.println("Finished sorting articlesMentionsANDLinks.txt in "+ ((stop - start) / 1000.0) + " seconds.");
		
		
		start = System.currentTimeMillis();
		
		frequencyCount("articlesMentionsANDLinks_SORTED.txt","articlesMentionsANDLinks_SORTED_Freq.txt");
		
		stop = System.currentTimeMillis();
		System.err.println("Finished calculating frequency count n "+ ((stop - start) / 1000.0) + " seconds.");
		
		
		
		//Now we are going to calculate the Prior probability
		start = System.currentTimeMillis();
		BufferedReader buffReader1 = new BufferedReader(new FileReader("articlesMentionsANDLinks_SORTED_Freq.txt"));
		BufferedReader buffReader2 = new BufferedReader(new FileReader("articlesMentionsANDLinks_SORTED_Freq.txt"));
		PrintWriter Pwriter = new PrintWriter("articlesMentionsANDLinks_PRIOR.txt","UTF-8");
		String line1 = null;
		String line2 = buffReader2.readLine();
		Map<String, Double> priorMap = new HashMap<String, Double>();

		while ((line1 = buffReader1.readLine()) != null) {
			line2 = buffReader2.readLine();
			if (line2 != null) {
				String[] elements1 = line1.split(";\t");
				String[] elements2 = line2.split(";\t");
				if (priorMap.isEmpty()) {
					priorMap.put(elements1[1], 1.0);
					if(!elements2[0].equalsIgnoreCase(elements1[0])){
						double  sum = 0.0;
						for (double f : priorMap.values()) {  sum += f;
						}
						Iterator<?> it = priorMap.entrySet().iterator();
						while (it.hasNext()) {
							@SuppressWarnings("rawtypes")
							Map.Entry pair = (Map.Entry) it.next();
							Double priorProb = (Double) pair.getValue()/sum;
							Pwriter.println(elements1[0] + ";\t" + pair.getKey()	+ ";\t"+ priorProb);
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
						double  sum = 0.0;
						for (double f : priorMap.values()) {
						    sum += f;
						}
						Iterator<?> it = priorMap.entrySet().iterator();
						while (it.hasNext()) {
							@SuppressWarnings("rawtypes")
							Map.Entry pair = (Map.Entry) it.next();
							Double priorProb = (Double) pair.getValue()/sum;
							Pwriter.println(elements1[0] + ";\t" + pair.getKey() + ";\t" + priorProb);
							it.remove();
						}
						continue;
					}
				}

			}else{
				String[] elements1 = line1.split(";\t");
				if (priorMap.isEmpty()) {
					priorMap.put(elements1[1], 1.0);
					double  sum = 0.0;
					for (double f : priorMap.values()) {  sum += f;
					}
					Iterator<?> it = priorMap.entrySet().iterator();
					while (it.hasNext()) {
						@SuppressWarnings("rawtypes")
						Map.Entry pair = (Map.Entry) it.next();
						Double priorProb = (Double) pair.getValue()/sum;
						Pwriter.println(elements1[0] + ";\t" + pair.getKey()	+ ";\t" + priorProb);
						it.remove();
					}
					
				} else {
					Double d = priorMap.get(elements1[1]);
					if (d == null) {
						d = 0.0;
					}
					priorMap.put(elements1[1], d + 1.0);
					
					double  sum = 0.0;
					for (double f : priorMap.values()) {
					    sum += f;
					}
				
					Iterator<?> it = priorMap.entrySet().iterator();
					while (it.hasNext()) {
						@SuppressWarnings("rawtypes")
						Map.Entry pair = (Map.Entry) it.next();
						Double priorProb = (Double) pair.getValue()/sum;
						Pwriter.println(elements1[0] + ";\t" + pair.getKey()+ ";\t" + priorProb);
					}
				}
			}
		}
		buffReader1.close();
		buffReader2.close();
		Pwriter.flush();
		Pwriter.close();

		stop = System.currentTimeMillis();
		System.err.println("Finished calculating prior probability in "+ ((stop - start) / 1000.0) + " seconds.");
	
	}
	  // private void parseCategories() {
	       // pageCats = new HashSet<String>();
	       // Pattern catPattern = Pattern.compile("\\[\\["+language.getLocalizedCategoryLabel()+":(.*?)\\]\\]", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	        //Matcher matcher = catPattern.matcher(wikiText);
	        //while (matcher.find()) {
	         //   String[] temp = matcher.group(1).split("\\|");
	         //   pageCats.add(temp[0]);
	       // }
	 //   }

	    public static String getPlainText(String wikiText) {
	        String text = wikiText.replaceAll("&gt;", ">");
	        text = text.replaceAll("&lt;", "<");
	        //text = infoboxCleanupPattern.matcher(text).replaceAll(" ");
	        //text = commentsCleanupPattern.matcher(text).replaceAll(" ");
	        //text = stylesPattern.matcher(text).replaceAll(" ");
	        //text = refCleanupPattern.matcher(text).replaceAll(" ");
	        text = text.replaceAll("</?.*?>", " ");
	        //text = curlyCleanupPattern0.matcher(text).replaceAll(" ");
	        //text = curlyCleanupPattern1.matcher(text).replaceAll(" ");
	        //text = cleanupPattern0.matcher(text).replaceAll(" ");

	        //Matcher m = cleanupPattern1.matcher(text);
	        //StringBuffer sb = new StringBuffer();
	        //while (m.find()) {
	        //    // For example: transform match to upper case
	        //   int i = m.group().lastIndexOf('|');
	        //    String replacement;
	        //   if (i > 0) {
	        //      replacement = m.group(1).substring(i - 1);
	        //    } else {
	        //        replacement = m.group(1);
	        //    }
	        //    m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
	       // }
	        //m.appendTail(sb);
	        //text = sb.toString();

	        text = text.replaceAll("'{2,}", "");
	        return text.trim();
	    }

	/**
	 *  This method is only meant to calculate the number of times a mention occurs
	 *  
	 * @param inputFile
	 * @param outFile
	 * @throws IOException
	 */
	public static void frequencyCount(String inputFile, String outFile) throws IOException{
		FileReader fReader = new FileReader(new File(inputFile));
		BufferedReader bffReader = new BufferedReader(fReader);
		String inpLine = null;		
		Map<String,Integer> frequency = new HashMap<String, Integer>();		
		while ((inpLine = bffReader.readLine()) != null) {
			//System.out.println(inpLine);
			String[] words = inpLine.split(";\t");
			if(words.length>=1){
				String key = words[0].trim();
				if((!key.isEmpty()) && (key !=null) && (key!="")){
					Integer f = frequency.get(key);
					if(f==null){
						f=0;
					}
					frequency.put(key,f+1);
				}
			}
		}
		
		bffReader.close();
		fReader.close();
		
		
		PrintWriter pWriter = new PrintWriter(outFile,"UTF-8");
		FileReader fRead = new FileReader(new File(inputFile));
		BufferedReader buffReader = new BufferedReader(fRead);
		String inp = null;
		while ((inp = buffReader.readLine()) != null) {
			
			String[] keys = inp.split(";\t");	
			if(keys.length>=1){
				String key = keys[0].trim();
				if((key!=null) && (!key.isEmpty() && (key!=""))){
					Integer value = frequency.get(key);
					pWriter.println(inp + ";\t" +value);
				}
			}
		}
		buffReader.close();
		fRead.close();
		pWriter.flush();
		pWriter.close();
		
	}
	/**
	 *  Calculate the frequency distribution given the original file with the frequency count
	 *  
	 * @param inputFile
	 * @param outputFile
	 * @throws NumberFormatException
	 * @throws IOException
	 */
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
		while ((inpLine = bffReader.readLine()) != null) {
			String[] words = inpLine.split(";\t");
			if(words!=null){
				String mention  = null;
				String entity = null;
				double  count = 0.0;
				mention = words[0].trim();
				entity = words[1].trim();
				count = Double.parseDouble(words[2].trim());
				double freq = count/lineCount;
				pWriter.println(inpLine+";\t"+String.format( "%.5f", freq));
			}
		}
		pWriter.flush();
		pWriter.close();
		bffReader.close();
		fReader.close();
	}
	
	/***
	 * Sorting Lines Of the Text File and writing to outputFileName
	 * 
	 * OLD VERSION
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * 
	 */

	public static void sortingLines(String inputFileName) throws IOException,
			InterruptedException {
		FileReader fileReader = new FileReader(inputFileName);
		int i = 0;
		int lineCount = 0;
		Process p = null;
		try {
			p = Runtime.getRuntime().exec("wc -l " + inputFileName);
			String s = null;
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((s = br.readLine()) != null)
				lineCount += Integer.parseInt(s.split(" ")[0]);
			p.waitFor();
			p.destroy();
		} catch (Exception e) {
		}
		int splits = lineCount / 10000000;
		int slice = 1;
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String inputLine;
		List<String> lineList = new ArrayList<String>();

		while ((inputLine = bufferedReader.readLine()) != null) {
			i++;
			lineList.add(inputLine);
			if ((i % 10000000) == 0) {
				System.out.println("Processing split " + slice);
				Processor p1 = new Processor(lineList, inputFileName, slice);
				p1.run();
				slice++;
				lineList = new ArrayList<String>();
			}
			if (slice > splits) {
				if (lineCount == i) {
					System.out.println("Processing split " + slice);
					Processor p1 = new Processor(lineList, inputFileName, slice);
					p1.run();
				}
			}
		}
		bufferedReader.close();
		fileReader.close();

	}
	
	/**
	 * External Sorting 
	 * 
	 * OLD VERSION
	 * 
	 * @param file
	 * @param outFile
	 * @throws IOException
	 */

	public static void externalSorting(String[] file, String outFile)
			throws IOException {
		int lineCount = 0;
		FileWriter fileWriter = new FileWriter(outFile);
		PrintWriter out = new PrintWriter(fileWriter);

		for (int i = 0; i < file.length; i++) {
			Process p;
			try {
				p = Runtime.getRuntime().exec("wc -l " + file[i]);
				String s = null;
				BufferedReader br = new BufferedReader(new InputStreamReader(
						p.getInputStream()));
				while ((s = br.readLine()) != null)
					lineCount += Integer.parseInt(s.split(" ")[0]);
				p.waitFor();
				// System.out.println ("exit: " + p.exitValue());
				p.destroy();
				// System.exit(1);
			} catch (Exception e) {
			}
		}

		// external sorting of 12 files
		BufferedReader buffReader1 = new BufferedReader(new FileReader(file[0]));
		BufferedReader buffReader2 = new BufferedReader(new FileReader(file[1]));
		BufferedReader buffReader3 = new BufferedReader(new FileReader(file[2]));
		BufferedReader buffReader4 = new BufferedReader(new FileReader(file[3]));
		BufferedReader buffReader5 = new BufferedReader(new FileReader(file[4]));
		BufferedReader buffReader6 = new BufferedReader(new FileReader(file[5]));
		BufferedReader buffReader7 = new BufferedReader(new FileReader(file[6]));
		BufferedReader buffReader8 = new BufferedReader(new FileReader(file[7]));
		BufferedReader buffReader9 = new BufferedReader(new FileReader(file[8]));
		BufferedReader buffReader10 = new BufferedReader(new FileReader(file[9]));
		BufferedReader buffReader11 = new BufferedReader(new FileReader(file[10]));
		BufferedReader buffReader12 = new BufferedReader(new FileReader(file[11]));

		String line = null;
		String line2 = null;
		String line3 = null;
		String line4 = null;
		String line5 = null;
		String line6 = null;
		String line7 = null;
		String line8 = null;
		String line9 = null;
		String line10 = null;
		String line11 = null;
		String line12 = null;

		Map<String, Integer> map = new HashMap<String, Integer>();

		boolean control = true;
		boolean list1Empty = false, list2Empty = false, list3Empty = false, list4Empty = false;
		boolean list5Empty = false, list6Empty = false, list7Empty = false, list8Empty = false;
		boolean list9Empty = false, list10Empty = false, list11Empty = false, list12Empty = false;
		List<String> tempList = new ArrayList<String>();

		line = buffReader1.readLine();
		line2 = buffReader2.readLine();
		line3 = buffReader3.readLine();
		line4 = buffReader4.readLine();
		line5 = buffReader5.readLine();
		line6 = buffReader6.readLine();
		line7 = buffReader7.readLine();
		line8 = buffReader8.readLine();
		line9 = buffReader9.readLine();
		line10 = buffReader10.readLine();
		line11 = buffReader11.readLine();
		line12 = buffReader12.readLine();

		map.put(line, 1);
		map.put(line2, 2);
		map.put(line3, 3);
		map.put(line4, 4);
		map.put(line5, 5);
		map.put(line6, 6);
		map.put(line7, 7);
		map.put(line8, 8);
		map.put(line9, 9);
		map.put(line10, 10);
		map.put(line11, 11);
		map.put(line12, 12);

		TreeMap<String, Integer> sorted = new TreeMap<>(map);
		int firstElem = sorted.firstEntry().getValue();

		for (int i = 0; i < lineCount; i++) {
			if (list1Empty && list2Empty && list3Empty && list4Empty
					&& list5Empty && list6Empty && list7Empty && list8Empty
					&& list9Empty && list10Empty && list11Empty && list12Empty) {
				break;
			}

			map = new HashMap<String, Integer>();
			if (!list1Empty) {
				map.put(line, 1);
			}
			if (!list2Empty) {
				map.put(line2, 2);
			}
			if (!list3Empty) {
				map.put(line3, 3);
			}

			if (!list4Empty) {
				map.put(line4, 4);
			}
			if (!list5Empty) {
				map.put(line5, 5);
			}
			if (!list6Empty) {
				map.put(line6, 6);
			}

			if (!list7Empty) {
				map.put(line7, 7);
			}
			if (!list8Empty) {
				map.put(line8, 8);
			}
			if (!list9Empty) {
				map.put(line9, 9);
			}

			if (!list10Empty) {
				map.put(line10, 10);
			}
			if (!list11Empty) {
				map.put(line11, 11);
			}
			if (!list12Empty) {
				map.put(line12, 12);
			}

			sorted = new TreeMap<>(map);
			firstElem = sorted.firstEntry().getValue();
			// System.out.println(sorted.firstEntry().getKey());
			if (firstElem == 1) {
				out.println(sorted.firstEntry().getKey());
				line = buffReader1.readLine();
				if (line == null) {
					list1Empty = true;// map.put(line, 1);
				}
				continue;
			}
			if (firstElem == 2) {
				out.println(sorted.firstEntry().getKey());
				line2 = buffReader2.readLine();
				if (line2 == null) {
					list2Empty = true;// map.put(line, 1);
				}
				continue;
			}
			if (firstElem == 3) {
				out.println(sorted.firstEntry().getKey());
				line3 = buffReader3.readLine();
				if (line3 == null) {
					list3Empty = true;// map.put(line, 1);
				}
				continue;
			}
			if (firstElem == 4) {
				out.println(sorted.firstEntry().getKey());
				line4 = buffReader4.readLine();
				if (line4 == null) {
					list4Empty = true;// map.put(line, 1);
				}
				continue;
			}
			if (firstElem == 5) {
				out.println(sorted.firstEntry().getKey());
				line5 = buffReader5.readLine();
				if (line5 == null) {
					list5Empty = true;// map.put(line, 1);
				}
				continue;
			}
			if (firstElem == 6) {
				out.println(sorted.firstEntry().getKey());
				line6 = buffReader6.readLine();
				if (line6 == null) {
					list6Empty = true;// map.put(line, 1);
				}
				continue;
			}
			if (firstElem == 7) {
				out.println(sorted.firstEntry().getKey());
				line7 = buffReader7.readLine();
				if (line7 == null) {
					list7Empty = true;// map.put(line, 1);
				}
				continue;
			}
			if (firstElem == 8) {
				out.println(sorted.firstEntry().getKey());
				line8 = buffReader8.readLine();
				if (line8 == null) {
					list8Empty = true;// map.put(line, 1);
				}
				continue;
			}
			if (firstElem == 9) {
				out.println(sorted.firstEntry().getKey());
				line9 = buffReader9.readLine();
				if (line9 == null) {
					list9Empty = true;// map.put(line, 1);
				}
				continue;
			}
			if (firstElem == 10) {
				out.println(sorted.firstEntry().getKey());
				line10 = buffReader10.readLine();
				if (line10 == null) {
					list10Empty = true;// map.put(line, 1);
				}
				continue;
			}
			if (firstElem == 11) {
				out.println(sorted.firstEntry().getKey());
				line11 = buffReader11.readLine();
				if (line11 == null) {
					list11Empty = true;// map.put(line, 1);
				}
				continue;
			}
			if (firstElem == 12) {
				out.println(sorted.firstEntry().getKey());
				line12 = buffReader12.readLine();
				if (line12 == null) {
					list12Empty = true;// map.put(line, 1);
				}
				continue;
			}
		}
		out.flush();
		out.close();
		fileWriter.close();

		// System.out.println("sorted keys of map : " + sortedIds);
		// System.exit(1);

	}
	
	
	/**
	 * This is a thread meant to split original file and sort each split separately
	 * 
	 */
	public static class Processor implements Runnable {
		private final String inputFileName;
		FileReader fileReader = null;
		BufferedReader buffReader1 = null;
		String line = null;
		FileWriter fileWriter = null;
		PrintWriter out = null;
		int splitNumber;
		List<String> lineList;

		public Processor(List<String> lineList, String inputFileName,
				int splitNumber) {
			super();
			this.lineList = lineList;
			this.inputFileName = inputFileName;
			this.splitNumber = splitNumber;

		}

		@Override
		public void run() {
			try {
				Collections.sort(lineList);
				System.out.println("Split " + splitNumber + " sorted.");
				fileWriter = new FileWriter(inputFileName + "_tmp" + splitNumber);
				out = new PrintWriter(fileWriter);
				for (String outputLine : lineList) {
					out.println(outputLine);
				}
				out.flush();
				out.close();
				fileWriter.close();
				System.out
						.println("Split " + splitNumber + " written to disk.");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}
	
	public static String stripCite(String text) {
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

}
