package socs.network.message;

import java.io.Serializable;

public class LinkDescription implements Serializable {
  public String linkID;
  public int portNum;
  public int tosMetrics;
  
  public LinkDescription(String linkID, int portNum, int tosMetrics) {
		super();
		this.linkID = linkID;
		this.portNum = portNum;
		this.tosMetrics = tosMetrics;
   }
	
   public LinkDescription() {
   }

public String toString() {
	    return linkID + ","  + portNum + "," + tosMetrics;
   }
}
