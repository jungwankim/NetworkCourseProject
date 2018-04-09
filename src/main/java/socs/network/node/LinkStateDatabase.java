package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

public class LinkStateDatabase {

  //linkID => LSAInstance
  HashMap<String, LSA> _store = new HashMap<String, LSA>();

  private RouterDescription rd = null;

  public LinkStateDatabase(RouterDescription routerDescription) {
    rd = routerDescription;
    LSA l = initLinkStateDatabase();
    _store.put(l.linkStateID, l);
  }

  /**
   * output the shortest path from this router to the destination with the given IP address
   */
  String getShortestPath(String destinationIP) {
	  HashMap<String, ArrayList<LinkDescription>> visited = new HashMap<String, ArrayList<LinkDescription>>();
      LinkedList<String> stack = new LinkedList<String>();
      
	  String source = rd.simulatedIPAddress;
	  for(int i = 0; i < _store.get(source).links.size(); i++) {
		  if(_store.get(source).links.get(i).linkID.equals(rd.simulatedIPAddress)) {
			  ArrayList<LinkDescription> lists = new ArrayList<LinkDescription>();
			  lists.add(_store.get(source).links.get(i));
			  visited.put(source, lists);
		  }
		  else {
			  ArrayList<LinkDescription> lists = new ArrayList<LinkDescription>();
			  lists.add(_store.get(source).links.get(i));
			  visited.put(_store.get(source).links.get(i).linkID, lists);
			  stack.add(_store.get(source).links.get(i).linkID);
		  }
	  }
	  
	  while(!stack.isEmpty()) {
		  int minNodeInd = minDist(stack, visited);
		  String node = stack.get(minNodeInd);
		  stack.remove(minNodeInd);
		  LSA nodeLsa = _store.get(node);
		  int distToNode = calculateDistanceFromList(visited.get(node));
		  
		  for(int i = 0; i < nodeLsa.links.size(); i ++) {
			  if(!nodeLsa.links.get(i).equals(node)) {
				  if (visited.containsKey(nodeLsa.links.get(i).linkID)) {
					  int oldDist = calculateDistanceFromList(visited.get(nodeLsa.links.get(i).linkID));
					  if (distToNode + nodeLsa.links.get(i).tosMetrics < oldDist) {
						  ArrayList<LinkDescription> newPath = new ArrayList<LinkDescription>();
						  newPath = (ArrayList<LinkDescription>) visited.get(node).clone();
						  newPath.add(nodeLsa.links.get(i));
						  visited.put(nodeLsa.links.get(i).linkID, newPath);
					  }
				  }
				  else {
					  	ArrayList<LinkDescription> newPath = new ArrayList<LinkDescription>();
					  	newPath = (ArrayList<LinkDescription>) visited.get(node).clone();
						newPath.add(nodeLsa.links.get(i));
						visited.put(nodeLsa.links.get(i).linkID, newPath);
						stack.add(nodeLsa.links.get(i).linkID);
				  }
			  }
		  }
	  }
	  if (visited.containsKey(destinationIP)) {
		  ArrayList<LinkDescription> path = visited.get(destinationIP);
		  System.out.print(this.rd.simulatedIPAddress);
		  for (int i = 0; i < path.size(); i++) {
			  System.out.print(" -> ");
			  System.out.print("(" + path.get(i).tosMetrics + ") ");
			  System.out.print(path.get(i).linkID);
		  }
		  System.out.println(" ");
	  }
	  else {
		  System.out.println("there is no path to target destination");
	  }
    return null;
  }
  
  private int minDist(LinkedList<String> stack, HashMap<String, ArrayList<LinkDescription>> visited) {
	int minDist = Integer.MAX_VALUE;
	int minDistIdx = 0;
	for(int i = 0; i < stack.size(); i++) {
		String target = stack.get(i);
		int dist = calculateDistanceFromList(visited.get(target));
		if(dist < minDist) {
			minDist = dist;
			minDistIdx = i;
		}
	}
	return minDistIdx;
}

private int calculateDistanceFromList(ArrayList<LinkDescription> distlist) {
	  int dist = 0;
	  for(int i = 0; i < distlist.size(); i++) {
		  dist = dist + distlist.get(i).tosMetrics;
	  }
	  return dist;
  }

  //initialize the linkstate database by adding an entry about the router itself
  private LSA initLinkStateDatabase() {
    LSA lsa = new LSA();
    lsa.linkStateID = rd.simulatedIPAddress;
    lsa.lsaSeqNumber = Integer.MIN_VALUE;
    LinkDescription ld = new LinkDescription();
    ld.linkID = rd.simulatedIPAddress;
    ld.portNum = -1;
    ld.tosMetrics = 0;
    lsa.links.add(ld);
    return lsa;
  }


  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (LSA lsa: _store.values()) {
      sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
      for (LinkDescription ld : lsa.links) {
        sb.append(ld.linkID).append(",").append(ld.portNum).append(",").
                append(ld.tosMetrics).append("\t");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

}
