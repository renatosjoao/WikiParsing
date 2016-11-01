package org.l3s.model;

public class PageLinksCount {
	int linksIn;
	int linksOut;
	
	public PageLinksCount() {
		super();
	}

	public PageLinksCount(int linksIn, int linksOut) {
		super();
		this.linksIn = linksIn;
		this.linksOut = linksOut;
	}

	/**
	 * @return the linksIn
	 */
	public int getLinksIn() {
		return linksIn;
	}

	/**
	 * @param linksIn the linksIn to set
	 */
	public void setLinksIn(int linksIn) {
		this.linksIn = linksIn;
	}

	/**
	 * @return the linksOut
	 */
	public int getLinksOut() {
		return linksOut;
	}

	/**
	 * @param linksOut the linksOut to set
	 */
	public void setLinksOut(int linksOut) {
		this.linksOut = linksOut;
	}
	
	
	
}
