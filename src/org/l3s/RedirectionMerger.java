package org.l3s;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *
 * This class reads the JSON file from one Wikipedia edition with the titles
 * redirections and merges with another year.
 *
 * i.e.
 *
 * [ { "redirect" : "American Samoa", "title" : "AmericanSamoa" }, { "redirect"
 * : "Applied ethics", "title" : "AppliedEthics" },...
 *
 *
 * @author renatosjoao@gmail.com
 *
 */

public class RedirectionMerger {

	public static void main(String[] args) throws ParseException, IOException {
		Utils utility = new Utils();
		Map<String, String> Map0 = null;
		Map<String, String> Map1 = null;
		Map<String, String> Map2 = null;
		Map<String, String> Map3 = null;
		Map<String, String> Map4 = null;
		Map<String, String> Map5 = null;
		Map0 = utility.loadTitlesRedirectMap(args[0]);
		TreeMap<String, LinkedList<String>> t1 = new TreeMap<>();
		Iterator<?> it0 = Map0.entrySet().iterator();
		while (it0.hasNext()) {
			Map.Entry pair = (Map.Entry) it0.next();
			String title = pair.getKey().toString().trim();
			String redir = pair.getValue().toString().trim();
			LinkedList<String> auxList = t1.get(title);
			if (auxList != null) {
				auxList.add(redir);
			} else {
				auxList = new LinkedList<String>();
				auxList.add(redir);
			}
			t1.put(title, auxList);
			it0.remove();
		}
		Map1 = utility.loadTitlesRedirectMap(args[1]);
		Iterator<?> it1 = Map1.entrySet().iterator();
		while (it1.hasNext()) {
			Map.Entry pair = (Map.Entry) it1.next();
			String title = pair.getKey().toString().trim();
			String redir = pair.getValue().toString().trim();
			LinkedList<String> auxList = t1.get(title);
			if (auxList != null) {
				if (auxList.contains(redir)) {
					continue;
				} else {
					auxList.add(redir);
				}
			} else {
				auxList = new LinkedList<String>();
				auxList.add(redir);
			}
			t1.put(title, auxList);
			it1.remove();
		}
		Map2 = utility.loadTitlesRedirectMap(args[2]);
		Iterator<?> it2 = Map2.entrySet().iterator();
		while (it2.hasNext()) {
			Map.Entry pair = (Map.Entry) it2.next();
			String title = pair.getKey().toString().trim();
			String redir = pair.getValue().toString().trim();
			LinkedList<String> auxList = t1.get(title);
			if (auxList != null) {
				if (auxList.contains(redir)) {
					continue;
				} else {
					auxList.add(redir);
				}
			} else {
				auxList = new LinkedList<String>();
				auxList.add(redir);
			}
			t1.put(title, auxList);
			it2.remove();
		}
		Map3 = utility.loadTitlesRedirectMap(args[3]);
		Iterator<?> it3 = Map3.entrySet().iterator();
		while (it3.hasNext()) {
			Map.Entry pair = (Map.Entry) it3.next();
			String title = pair.getKey().toString().trim();
			String redir = pair.getValue().toString().trim();
			LinkedList<String> auxList = t1.get(title);
			if (auxList != null) {
				if (auxList.contains(redir)) {
					continue;
				} else {
					auxList.add(redir);
				}
			} else {
				auxList = new LinkedList<String>();
				auxList.add(redir);
			}
			t1.put(title, auxList);
			it3.remove();
		}
		Map4 = utility.loadTitlesRedirectMap(args[4]);
		Iterator<?> it4 = Map4.entrySet().iterator();
		while (it4.hasNext()) {
			Map.Entry pair = (Map.Entry) it4.next();
			String title = pair.getKey().toString().trim();
			String redir = pair.getValue().toString().trim();
			LinkedList<String> auxList = t1.get(title);
			if (auxList != null) {
				if (auxList.contains(redir)) {
					continue;
				} else {
					auxList.add(redir);
				}
			} else {
				auxList = new LinkedList<String>();
				auxList.add(redir);
			}
			t1.put(title, auxList);
			it4.remove();
		}
		Map5 = utility.loadTitlesRedirectMap(args[5]);
		Iterator<?> it5 = Map5.entrySet().iterator();
		while (it5.hasNext()) {
			Map.Entry pair = (Map.Entry) it5.next();
			String title = pair.getKey().toString().trim();
			String redir = pair.getValue().toString().trim();
			LinkedList<String> auxList = t1.get(title);
			if (auxList != null) {
				if (auxList.contains(redir)) {
					continue;
				} else {
					auxList.add(redir);
				}
			} else {
				auxList = new LinkedList<String>();
				auxList.add(redir);
			}
			t1.put(title, auxList);
			it5.remove();
		}
		// *******************************************************************************************************************//
		JSONArray Jarray = new JSONArray();
		Iterator<?> iter = t1.entrySet().iterator();
		while (iter.hasNext()) {
			JSONObject jobj = new JSONObject();
			Map.Entry pair = (Map.Entry) iter.next();
			String title = pair.getKey().toString().trim();
			LinkedList<String> redirections = (LinkedList<String>) pair
					.getValue();
			jobj.put("title", title);
			jobj.put("redirections", redirections);
			Jarray.add(jobj);
			iter.remove();
		}
		ObjectMapper jsonMapper = new ObjectMapper();
		try {
			String outputJSON = jsonMapper.writerWithDefaultPrettyPrinter()
					.writeValueAsString(Jarray);
			PrintWriter redirectPagesTitlesMapWriter = new PrintWriter(
					"pagesTitles_ALL_REDIRECT.json", "UTF-8");
			redirectPagesTitlesMapWriter.println(outputJSON);
			redirectPagesTitlesMapWriter.flush();
			redirectPagesTitlesMapWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}