package org.l3s.dbpedia;
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
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DBPediaType {
	private static Pattern namePattern = Pattern.compile("^[a-zA-Z_  -]*",Pattern.MULTILINE);
	Map<String, String> mentionType = new TreeMap<String, String>();
	ArrayList<String> listOfMentionsAndEntities = new ArrayList<String>();

	public DBPediaType(String mentionEntityFile, String instance_typesFile) {
		super();
		try {
			loadMentionsAndEntities(mentionEntityFile);
			loadTreeMap(instance_typesFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This utility method compares each mention to elements in the tree map and
	 * writes output to PERSON.txt , LOCATION.txt and ORGANIZATION.txt files
	 * 
	 * @param listOfMentionsAndEntities
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public void writeToFile() throws FileNotFoundException,
			UnsupportedEncodingException {
		PrintWriter pPER = new PrintWriter("PERSON.txt", "UTF-8");
		PrintWriter pLOC = new PrintWriter("LOCATION.txt", "UTF-8");
		PrintWriter pORG = new PrintWriter("ORGANIZATION.txt", "UTF-8");
		for (String s : listOfMentionsAndEntities) {
			String entities[] = s.split(";");
			String entity = entities[0].trim();
			if (entity != null) {
				entity = entity.replaceAll(" ", "_").trim();
				String type = mentionType.get(entity);
				if (type != null) {
					if (type.equalsIgnoreCase("PERSON")) {
						pPER.println(s);
					}
					if (type.equalsIgnoreCase("LOCATION")) {
						pLOC.println(s);
					}
					if (type.equalsIgnoreCase("ORGANISATION")) {
						pORG.println(s);
					}
				}
			}
		}
		pPER.flush();
		pPER.close();
		pLOC.flush();
		pLOC.close();
		pORG.flush();
		pORG.close();

	}

	/***
	 * This method loads the mentions and entities into an array list in memory
	 * 
	 * @param inputFile
	 * @throws IOException
	 */
	public void loadMentionsAndEntities(String inputFile) throws IOException {
		FileReader fr = new FileReader(new File(inputFile));
		BufferedReader bf = new BufferedReader(fr);
		String inputLine = null;
		while ((inputLine = bf.readLine()) != null) {
			listOfMentionsAndEntities.add(inputLine);
		}
		bf.close();

	}

	/**
	 * This function loads a treeMap into memory about the possible entities
	 * types
	 * 
	 * Requires : instance-types-transitive_en.ttl
	 * 
	 * @param inputFile
	 * @throws IOException
	 */
	public void loadTreeMap(String inputFile) throws IOException {
		FileReader fr = new FileReader(new File(inputFile));
		BufferedReader bf = new BufferedReader(fr);
		String inputLine = null;
		while ((inputLine = bf.readLine()) != null) {
			inputLine = inputLine.replaceAll(">", "").replaceAll("<", "").replaceAll("\\.", "");
			String[] uris = inputLine.split(" ");
			String[] links = uris[0].split("/");
			String[] types = uris[uris.length - 1].split("/");
			String name = links[links.length - 1];
			String type = types[types.length - 1];
			type = type.replaceAll("owl#", "");

			if (type.equalsIgnoreCase("Person")) {
				mentionType.put(name, type);
			}
			if (type.equalsIgnoreCase("Location")) {
				mentionType.put(name, type);
			}
			if (type.equalsIgnoreCase("Organisation")) {
				mentionType.put(name, type);
			}
		}

		bf.close();
	}

	/**
	 * Utility function to parse input file, trigger a query to DBPedia and dump
	 * output file with PERSON only
	 * 
	 * @param inputFile
	 * @throws IOException
	 */
	public static void dumpPerson(String inputFile, String outputFile)
			throws IOException {
		DBPediaQuery DBP = new DBPediaQuery();
		FileReader fr = new FileReader(new File(inputFile));
		ArrayList<String> listOfMentionsAndEntities = new ArrayList<String>();
		PrintWriter pWriter = new PrintWriter(outputFile, "UTF-8");
		BufferedReader bf = new BufferedReader(fr);
		String inputLine = null;
		while ((inputLine = bf.readLine()) != null) {
			listOfMentionsAndEntities.add(inputLine);
		}
		for (String str : listOfMentionsAndEntities) {
			String entity = str.split(";")[1].trim();
			if (entity != null) {
				Matcher m = namePattern.matcher(entity);
				while (m.find()) {
					String tmpStr = m.group();
					if (DBP.isPerson(tmpStr)) {
						pWriter.println(str);
					} else {
						continue;
					}
				}
			}
		}

		bf.close();
		pWriter.flush();
		pWriter.close();
	}

	/**
	 * Utility function to parse the input file, trigger a query to DBPedia and
	 * dump output file with LOCATION only
	 * 
	 * @param inputFile
	 * @throws IOException
	 */
	public void dumpLocation(String inputFile, String outputFile)
			throws IOException {
		DBPediaQuery DBP = new DBPediaQuery();
		FileReader fr = new FileReader(new File(inputFile));
		ArrayList<String> listOfMentionsAndEntities = new ArrayList<String>();
		PrintWriter pWriter = new PrintWriter(outputFile, "UTF-8");
		BufferedReader bf = new BufferedReader(fr);
		String inputLine = null;
		int i = 0;
		while ((inputLine = bf.readLine()) != null) {
			listOfMentionsAndEntities.add(inputLine);

		}
		i = 0;
		for (String str : listOfMentionsAndEntities) {
			System.out.println(i++);
			String entity = str.split(";")[1].trim();
			if (entity != null) {
				Matcher m = namePattern.matcher(entity);
				while (m.find()) {
					String tmpStr = m.group();
					if (DBP.isLocation(tmpStr)) {
						pWriter.println(str);
					} else {
						continue;
					}
				}
			}
		}
		bf.close();
		pWriter.flush();
		pWriter.close();
	}
}
