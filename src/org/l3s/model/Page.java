package org.l3s.model;

import java.util.ArrayList;


public class Page {
	String title;
	String id;
	String text;
	ArrayList<String> links;
	ArrayList<String> categories;
	
	
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}
	/**
	 * @param text the text to set
	 */
	public void setText(String text) {
		this.text = text;
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
	/**
	 * @return the categories
	 */
	public ArrayList<String> getCategories() {
		return categories;
	}
	/**
	 * @param categories the categories to set
	 */
	public void setCategories(ArrayList<String> categories) {
		this.categories = categories;
	}
	
	
	
}
