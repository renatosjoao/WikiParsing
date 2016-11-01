package org.l3s.model;

import java.util.ArrayList;

public class Node {
	String ID;
	ArrayList<String> links;
	
	
	public Node(String iD) {
		super();
		ID = iD;
	}


	public Node() {
		super();
	}


	/**
	 * @return the iD
	 */
	public String getID() {
		return ID;
	}


	/**
	 * @param iD the iD to set
	 */
	public void setID(String iD) {
		ID = iD;
	}


	/**
	 * @return the links
	 */
	public ArrayList<String> getLinks() {
		return links;
	}


	/**
	 * @param links the links to set
	 */
	public void setLinks(ArrayList<String> links) {
		this.links = links;
	}
	
	
	
}
