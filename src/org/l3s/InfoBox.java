package org.l3s;

public class InfoBox {
	  String infoBoxWikiText = null;
	  public InfoBox(String infoBoxWikiText) {
	    this.infoBoxWikiText = infoBoxWikiText;
	  }
	
	  public String dumpRaw() {
	    return infoBoxWikiText;
	  }
	}
