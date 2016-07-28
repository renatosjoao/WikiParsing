package org.l3s;

/**
 * @author Renato Stoffalette Joao
 * @mail renatosjoao@gmail.com
 * @version 1.0
 * @date 02.2016
 *
 */
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.compress.compressors.CompressorException;


public class WikipediaSAXParser {
    private static String pageTitles = "pagesTitles.tsv.bz2";
    private static String pageTitlesRedirect = "pagesTitles_REDIRECT.json";
	private static String mentionEntityLinks = "mentionEntityLinks.txt.bz2";
	private static Map<String, String> pageTitlesMap = new TreeMap<String,String>();
	private static Set<String> titlesSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);


	public static void main(String[] args) throws IOException, CompressorException, org.json.simple.parser.ParseException{
			//Utils ut = new Utils();
			//preProcessing(args);
			//priorCalculation(args);
			//ut.compareTopK(args[0],args[1],args[2],args[3]);
			//ut.removeSingletons(args[0]);
			//ut.calculateRanking(args[0],args[1],args[2],args[3]);
	}
/********************************** END OF MAIN************************************************************/
/**********************************************************************************************************/
	/**
	 *  Here we are only creating the initial files meant to be used for later processing of the Wikipedia
	 *  dumps. This method will create the page titles related files and load them into data structures in memory.
	 *
	 * @param args
	 * @throws IOException
	 * @throws CompressorException
	 */

	public static void preProcessing(String[] args) throws IOException, CompressorException{
		Utils ut  = new Utils();
		/**
		 * Initially I want to check if the page titles file exists. If it does not exist in the current directory. I will create it.
		 */
		File pgFile = new File(pageTitles); //"pagesTitles.tsv.bz2";
		if (pgFile.exists()) {
			titlesSet = ut.loadTitlesList(pageTitles);
		 } else {
			ut.writePageTitles(args[0]);
			titlesSet = ut.loadTitlesList(pageTitles);
		}

	}

	/**
	 * This is the point where we parse the Wikipedia dump create a file with all the mention/entity pairs
	 * calculate the prior probabilities for the entities and create files with top5 ranked entities.
	 *
	 * @param args
	 * @throws IOException
	 * @throws org.json.simple.parser.ParseException
	 * @throws CompressorException
	 */
	public static void priorCalculation(String[] args) throws IOException, org.json.simple.parser.ParseException, CompressorException{
		Utils ut  = new Utils();
		/**
		 *  *  I create the mention/entity file without checking whether the entity has a proper entity page.
		 */ //args[0] == Wikipedia dump file
		ut.writeMentionEntity_NO_Checking(args[0],	"mentionEntityLinks.txt");
		/**
		 *  *  Now I want to check if the page titles redirection map file exists. If it does not exist in the current directory. I will create it.
		 */
		File mpFile = new File(pageTitlesRedirect);
		if (mpFile.exists()) {
			pageTitlesMap = ut.loadTitlesRedirectMap(pageTitlesRedirect);
		}else{
			ut.writePageTitles(args[0]);
			pageTitlesMap = ut.loadTitlesRedirectMap(pageTitlesRedirect);
		}
		/**
		 * * Then I check my mention/entity list against the page titles list and the page titles redirection map.
		 *
		 */
		ut.checkTitles(mentionEntityLinks,titlesSet,pageTitlesMap);
		/**
		 * * Counting the frequency for mention/entity pairs.
		 */
		ut.frequencyCount("mentionEntityLinks_SORTED.txt.bz2","mentionEntityLinks_SORTED_Freq.txt");
		ut.frequencyCut("mentionEntityLinks_SORTED_Freq.txt.bz2","mentionEntityLinks_SORTED_Freq",10);
		/**
		 * * Counting the frequency for mention/entity pairs WITHOUT duplicates.
		 */
		ut.frequencyCountUnique("mentionEntityLinks_SORTED.txt.bz2","mentionEntityLinks_SORTED_Freq_Uniq.txt");
		/**
		 * * Finally calculating PRIOR probability for mention/entity pairs.
		 */
		ut.calculatePRIOR("mentionEntityLinks_SORTED_Freq_10.txt.bz2","mentionEntityLinks_PRIOR_10.txt");
		/**
		 *  This is supposed to calculate the top K mention/entity disambiguation
		 *
		 */
		//////////ut.calculatePRIOR_topK("mentionEntityLinks_PRIOR_100.txt.bz2","mentionEntityLinks_PRIOR_100_top1",1);
		ut.calculatePRIOR_topK("mentionEntityLinks_PRIOR_10.txt.bz2","mentionEntityLinks_PRIOR_10_top5",5);
	}

	/**
	 * 	This is the most basic comparison in which I compare the wikipedia dumps on the top K entities for each mention
	 * 	between two dumps.
	 *
	 * @param args
	 * @throws IOException
	 * @throws CompressorException
	 */
	public void compareMentionEntityFiles(String[] args) throws IOException, CompressorException{
		Utils ut  = new Utils();
		//HERE we do only comparisons and statisticals calculations
		ut.compareTopK(args[0],args[1],args[2],args[3]);
		//ut.compareDisambiguations(args[0],args[1],args[2],args[3]); //This is only meant to compare top1
		//System.exit(1);
	}
}