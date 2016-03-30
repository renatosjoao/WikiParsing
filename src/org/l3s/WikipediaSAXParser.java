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
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.compressors.CompressorException;


public class WikipediaSAXParser {
    private static String pageTitles = "pagesTitles.txt.bz2";
	private static String mentionEntityLinks = "mentionEntityLinks.txt.bz2";
	private static Map<String, String> pageTitlesMap = new TreeMap<String,String>();
	private static Set<String> titlesSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

	/**
	 * @throws CompressorException
	 * @throws org.json.simple.parser.ParseException
	 * @throws ParseException
	 * @param args
	 * @throws
	 */
	public static void main(String[] args) throws IOException, ParseException, org.json.simple.parser.ParseException, CompressorException {
		Utils ut  = new Utils();
		/**
		 *  * Initially I want to check if the page titles file exists. If it does not exist in the current directory. I will create it.
		 */
		File pgFile = new File(pageTitles);
		if (pgFile.exists()) {
			titlesSet = ut.loadTitlesList(pageTitles);
		 } else {
			ut.writePageTitles(args[0]);
			titlesSet = ut.loadTitlesList(pageTitles);
		}
		/**
		 *  *  I create the mention/entity file without checking whether the entity has a proper entity page.
		 */
		ut.writeMentionEntity_NO_Checking(args[0],	"mentionEntityLinks.txt");
		/**
		 *  *  Now I want to check if the page titles redirection map file exists. If it does not exist in the current directory. I will create it.
		 */
		File mpFile = new File("pagesTitles_REDIRECT.json");
		if (mpFile.exists()) {
			pageTitlesMap = ut.loadTitlesRedirectMap("pagesTitles_REDIRECT.json");
		}else{
			ut.writePageTitles(args[0]);
			pageTitlesMap = ut.loadTitlesRedirectMap("pagesTitles_REDIRECT.json");
		}
		/**
		 * * Then I check my mention/entity list against the page titles list and the page titles redirection map.
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
		ut.calculatePRIOR("mentionEntityLinks_SORTED_Freq_10.txt.bz2","mentionEntityLinks_PRIOR.txt");

		ut.mentionEntityDisamb("mentionEntityLinks_PRIOR.txt.bz2","disambiguation.txt");
		ut.writeLinkOccurrence("mentionEntityLinks_SORTED_Freq_Uniq.txt.bz2");
	}
/********************************** END OF MAIN ************************************************************/
}