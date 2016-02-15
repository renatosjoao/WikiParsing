package org.l3s.test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TwoBuffers {

	public static void main(String[] args) throws IOException {

		BufferedReader buffReader1 = new BufferedReader(new FileReader("temp1"));
		BufferedReader buffReader2 = new BufferedReader(new FileReader("temp1"));

		PrintWriter Pwriter = new PrintWriter("temp1.out", "UTF-8");
		String line1 = null;
		String line2 = buffReader2.readLine();
		Map<String, Double> priorMap = new HashMap<String, Double>();

		while ((line1 = buffReader1.readLine()) != null) {
			line2 = buffReader2.readLine();
			if (line2 != null) {
				String[] elements1 = line1.split(";");
				String[] elements2 = line2.split(";");
				if (priorMap.isEmpty()) {
					priorMap.put(elements1[1], 1.0);
					if(!elements2[0].equalsIgnoreCase(elements1[0])){
						double  sum = 0.0;
						for (double f : priorMap.values()) {  sum += f;
						}
						Iterator it = priorMap.entrySet().iterator();
						while (it.hasNext()) {
							Map.Entry pair = (Map.Entry) it.next();
							Double priorProb = (Double) pair.getValue()/sum;
							Pwriter.println(elements1[0] + ";" + pair.getKey()	+ "; "+ priorProb);
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
						Iterator it = priorMap.entrySet().iterator();
						while (it.hasNext()) {
							Map.Entry pair = (Map.Entry) it.next();
							Double priorProb = (Double) pair.getValue()/sum;
							Pwriter.println(elements1[0] + ";" + pair.getKey() + "; " + priorProb);
							// System.out.println(pair.getKey() + " = " +
							// pair.getValue());
							it.remove();
						}
						// atualizo
						// escrevo no disco e vazo
						// continue;
						priorMap = new HashMap<String, Double>();
						continue;
					}
				}

			}else{
				String[] elements1 = line1.split(";");
				if (priorMap.isEmpty()) {
					priorMap.put(elements1[1], 1.0);
					double  sum = 0.0;
					for (double f : priorMap.values()) {  sum += f;
					}
					Iterator it = priorMap.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry pair = (Map.Entry) it.next();
						Double priorProb = (Double) pair.getValue()/sum;
						Pwriter.println(elements1[0] + ";" + pair.getKey()	+ "; " + priorProb);
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
				
					Iterator it = priorMap.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry pair = (Map.Entry) it.next();
						Double priorProb = (Double) pair.getValue()/sum;
						Pwriter.println(elements1[0] + ";" + pair.getKey()+ "; " + priorProb);
						// System.out.println(pair.getKey() + " = " +
						// pair.getValue());
						it.remove();
					}
					
					
					
				}
			}
		}
		Pwriter.flush();
		Pwriter.close();
	}

}
