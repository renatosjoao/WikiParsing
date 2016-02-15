package org.l3s.dbpedia;
/**
 * @author Renato Stoffalette Joao
 * @mail renatosjoao@gmail.com
 * @version 1.0
 * @date 02.2016
 * 
 * 
 */
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;

public class DBPediaQuery {

	public DBPediaQuery(){
		
	}
	
	/**
	 * Utility method to query DBPedia whether a given string represents a person
	 * @param name
	 * @return
	 */
	public boolean isPerson(String name) {
		String stringTest = name.trim();
		boolean result = false;
		stringTest = stringTest.replaceAll("\"","").replaceAll("\'","").replaceAll("`","").replaceAll("´","");
		stringTest = stringTest.replace(" ", "_");
		//System.out.println(stringTest);
		String queryString = "ASK {<http://dbpedia.org/resource/" + stringTest+ "> a <http://dbpedia.org/ontology/Person>}";
		try{
			Query query = QueryFactory.create(queryString);
			QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
			result = qexec.execAsk();
			qexec.close();
			return result;
		}catch(Exception e){
			return result;
		}
		
	}
	
	public boolean isLocation(String name) {
		String stringTest = name.trim();
		stringTest = stringTest.replaceAll("\"","").replaceAll("\'","").replaceAll("`","").replaceAll("´","");
		stringTest = stringTest.replace(" ", "_");
		boolean result = false;
		String queryString = "ASK {<http://dbpedia.org/resource/" + stringTest+ "> a <http://dbpedia.org/ontology/Location>}";
		try {
			Query query = QueryFactory.create(queryString);
			QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
			result = qexec.execAsk();
			qexec.close();
			return result;
			
		} catch (Exception e) {
			return result;
		}
	}

}