package socs.network.node;
import socs.network.util.Configuration;
import socs.network.message.*;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;


public class Router {

  protected LinkStateDatabase lsd;

  RouterDescription rd = new RouterDescription();
  Timer timer;

  //assuming that all routers are with 4 ports
  Link[] ports = new Link[4];

  public Router(Configuration config) {
    rd.simulatedIPAddress = config.getString("socs.network.router.ip");
    rd.processPortNumber = config.getShort("socs.network.router.port");
    lsd = new LinkStateDatabase(rd);
    
    TimerTask hbt = new HeartbeatTask(this);
    timer = new Timer();
    timer.schedule(hbt, 100, 10000);      
  }

  public void printPort() {
	  for (int i = 0; i < this.ports.length; i++) {
		  if (ports[i] != null) {
			  System.out.println(i + "   " + ports[i]); 
		  }
	  }  
  }
  
  public int getFreePort() {
	  for (int i = 0; i < this.ports.length; i++) {
		  if (ports[i] == null) {
			  return i;
		  }
	  }
	  return -1;
  }
  
  
  public int isRouterInPorts(String targetRouterIP) {
	  for (int i = 0; i < ports.length; i++) {
		  if(ports[i] != null) {
			  if (ports[i].router2.simulatedIPAddress.equals(targetRouterIP)) {
				  return i;
			  }
		  }
	  }
	  return -1;
  }
  
  
  public void addLinkedDescription(RouterDescription target, short weight) {
	  boolean isInLSD = false;
	  for (int i = 0; i < this.lsd._store.get(this.rd.simulatedIPAddress).links.size(); i++ ) {
		  //it's in lsd then update weight
		  if (lsd._store.get(rd.simulatedIPAddress).links.get(i).linkID.equals(target.simulatedIPAddress)) {
			  if(lsd._store.get(rd.simulatedIPAddress).links.get(i).tosMetrics != weight) {
				  lsd._store.get(rd.simulatedIPAddress).links.get(i).tosMetrics = weight;
				  this.lsd._store.get(this.rd.simulatedIPAddress).lsaSeqNumber++;
			  }
			  isInLSD = true;
		  }
	  }
	  if (!isInLSD) {
		  LinkDescription ld = new LinkDescription();
		  ld.linkID = target.simulatedIPAddress;
		  ld.portNum = target.processPortNumber;
		  ld.tosMetrics = weight;
		  lsd._store.get(rd.simulatedIPAddress).links.add(ld);
		  this.lsd._store.get(this.rd.simulatedIPAddress).lsaSeqNumber++;
	  }
	  return;
  }
  
  public void removeLinkedDescription (RouterDescription target) {
	  int index = -1;
	  for (int i = 0; i < this.lsd._store.get(this.rd.simulatedIPAddress).links.size(); i++ ) {
		  //it's in lsd then update weight
		  if (this.lsd._store.get(this.rd.simulatedIPAddress).links.get(i).linkID.equals(target.simulatedIPAddress)) {
			  index = i;
		  }
	  }
	  if (index >= 0 ) {
		  this.lsd._store.get(this.rd.simulatedIPAddress).links.remove(index);
	  }
	  this.lsd._store.get(this.rd.simulatedIPAddress).lsaSeqNumber++;
	  return;
  }
  
  private boolean addRequest(RouterDescription target, Link newLink, int portNum, short weight) {
	  try {
		  Socket client = new Socket(target.processIPAddress, target.processPortNumber);
		  ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());
		  ObjectInputStream input = new ObjectInputStream(client.getInputStream());
		  
		  SOSPFPacket packet = new SOSPFPacket();
		  packet.dstIP = target.simulatedIPAddress;
		  packet.sospfType = 3;
		  packet.srcIP = this.rd.simulatedIPAddress;
		  packet.srcProcessIP = this.rd.processIPAddress;
		  packet.srcProcessPort = this.rd.processPortNumber;
		  packet.weight = weight;
		  
		  output.writeObject(packet);
		  
		  SOSPFPacket received;
		  try {
				received = (SOSPFPacket) input.readObject();
				if (received.sospfType == 2) {
						System.out.println("target router is full");
						output.close();
				        input.close();
				        client.close();
						return false;
				}
				else {
						ports[portNum] = newLink;
						this.addLinkedDescription(target, weight);
						ports[portNum].router1.status = RouterStatus.TWO_WAY;
						ports[portNum].router2.status = RouterStatus.TWO_WAY;
				}
		  } catch (ClassNotFoundException e) {
				e.printStackTrace();
		  }
          output.close();
          input.close();
          client.close();
	  } catch (UnknownHostException e) {
		e.printStackTrace();
	  } catch (IOException e) {
		e.printStackTrace();
	  }
	  return true;
  }
  
  private boolean deleteRequest(RouterDescription target) {
	  try {
		  Socket client = new Socket(target.processIPAddress, target.processPortNumber);
		  ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());
		  ObjectInputStream input = new ObjectInputStream(client.getInputStream());

		  SOSPFPacket packet = new SOSPFPacket();
		  packet.dstIP = target.simulatedIPAddress;
		  packet.sospfType = 4;
		  packet.srcIP = this.rd.simulatedIPAddress;
		  packet.srcProcessIP = this.rd.processIPAddress;
		  packet.srcProcessPort = this.rd.processPortNumber;
		  output.writeObject(packet);
		  
		  SOSPFPacket received;
		  try {
				received = (SOSPFPacket) input.readObject();
				if (received.sospfType == 2) {
						System.out.println("error delete request");
						output.close();
				        input.close();
				        client.close();
						return false;
				}
				else {
						if (received.lsaArray != null) {
							if(lsd._store.get(target.simulatedIPAddress).lsaSeqNumber < received.lsaArray.get(0).lsaSeqNumber) {
								lsd._store.put(target.simulatedIPAddress, received.lsaArray.get(0));
							}
						}
						this.removeLinkedDescription(target);
				}
		  } catch (ClassNotFoundException e) {
				e.printStackTrace();
		  }
		  
          output.close();
          input.close();
          client.close();
		  } catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  
	  return true;
  }
  
  public void forwardingDataBaseRequest(SOSPFPacket packet) {
	  boolean upToDated = true;
	  for (int i = 0; i < packet.lsaArray.size(); i++) {
		  LSA comparingLSA = packet.lsaArray.get(i);
		  if (lsd._store.containsKey(comparingLSA.linkStateID)) {
			  if (lsd._store.get(comparingLSA.linkStateID).lsaSeqNumber < comparingLSA.lsaSeqNumber) {
				  upToDated = false;
				  lsd._store.replace(comparingLSA.linkStateID, comparingLSA);
			  }
		  }
		  else {
			  upToDated = false;
			  lsd._store.put(comparingLSA.linkStateID, comparingLSA);
		  }  
	  }
	  if (!upToDated) {
		  synchronizeDataBaseRequest();
	  }
  }
  
 

public void synchronizeDataBaseRequest() {
	  for(int i = 0; i < ports.length; i++) {
		  if(ports[i]!= null) {
			  try {
				  RouterDescription target = ports[i].router2;
				  Socket client = new Socket(target.processIPAddress, target.processPortNumber);
				  ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());
				  SOSPFPacket packet = new SOSPFPacket();
				  packet.dstIP = target.simulatedIPAddress;
				  packet.sospfType = 1;
				  packet.srcIP = this.rd.simulatedIPAddress;
				  packet.srcProcessIP = this.rd.processIPAddress;
				  packet.srcProcessPort = this.rd.processPortNumber;
				  Vector<LSA> newlsaArray = new Vector<LSA>();
				  for (String key : lsd._store.keySet()) {
					   newlsaArray.add(lsd._store.get(key));
				  }
				  packet.lsaArray = newlsaArray;
				  output.writeObject(packet);
		          output.close();
		          client.close();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ConnectException e) {
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			  
		  }
	  }
	  return;
  }

  public int getWeightOnTarget(String target) {
	  LinkedList<LinkDescription> links = lsd._store.get(rd.simulatedIPAddress).links;
	  for (int i = 0; i < links.size(); i ++) {
		  if (links.get(i).linkID.equals(target)) {
			  return links.get(i).tosMetrics;
		  }
	  }
	  return 1;
  }
  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {
	  this.lsd.getShortestPath(destinationIP);
	  return;
  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {
	  int portIndx = -1;
	  for (int i = 0; i < ports.length; i++) {
		  if (ports[i] != null) {
			  if(ports[i].router2.processPortNumber == portNumber) {
				  portIndx = i;
				  break;
			  }
		  }
	  }
      //check Basic error
      if (portIndx < 0) {
    	  System.out.println("There is no connection to given port number.");
    	  return;
      }
      //connect given router to synchronize database
      try {
    	  deleteRequest(ports[portIndx].router2);
    	  synchronizeDataBaseRequest();
    	  ports[portIndx] = null;
      } catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
	  }
  }



/**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort,
                             String simulatedIP, short weight) {
	  RouterDescription target = new RouterDescription(processIP, processPort, simulatedIP);
	  Link newLink = new Link(this.rd, target);
	  int protNum = getFreePort();
	  if (protNum >= 0) {
		  try {
			  Socket client = new Socket(processIP, processPort);
			  ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());
			  ObjectInputStream input = new ObjectInputStream(client.getInputStream());
	          output.writeObject(simulatedIP);
	          String incoming;
	          		try {
	          			incoming = (String) input.readObject();
	          				if (incoming.equals("good")) {
	          					ports[protNum] = newLink;
	          					addLinkedDescription(target, weight);
	          				}
	          				else if (incoming.equals("full")) {
	          					System.out.println("target router's port is full");
	          				}
	          				else {
	          					System.out.println("input wrong ip address.");
	          				}
	          		} catch (ClassNotFoundException e) {
	          			e.printStackTrace();
	          		}
	          output.close();
	          input.close();
	          client.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	  }
	  else {
		  System.out.println("all ports are occupied at the momment. Please try it again");
		  return;
	  }  
  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart() {
	  for (int i = 0; i < ports.length; i++) {
		  if (ports[i] != null) {
			  RouterDescription target = ports[i].router2;
			  try {
					Socket client = new Socket(target.processIPAddress, target.processPortNumber);
					ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
					ObjectInputStream in = new ObjectInputStream(client.getInputStream());
					SOSPFPacket packet = new SOSPFPacket();
					packet.dstIP = target.simulatedIPAddress;
					packet.sospfType = 0;
					packet.srcIP = this.rd.simulatedIPAddress;
					packet.srcProcessIP = this.rd.processIPAddress;
					packet.srcProcessPort = this.rd.processPortNumber;
					packet.weight = (short) this.getWeightOnTarget(target.simulatedIPAddress);
//					packet.weight = this.lsd._store.get(rd.simulatedIPAddress).links.g
					out.writeObject(packet);
					SOSPFPacket received = (SOSPFPacket) in.readObject();
					//target port is full remove 
					if (received.sospfType == 2) {
						removeLinkedDescription(target);
						ports[i] = null;
					}
					else {
						System.out.println("received HELLO from " + received.srcIP + ";");
						ports[i].router1.status = RouterStatus.TWO_WAY;
						ports[i].router2.status = RouterStatus.TWO_WAY;
						System.out.println("set " + received.srcIP + " state to TWO_WAY");
						out.writeObject(packet);
					}
					out.close();
					in.close();
					client.close();
			  } catch (UnknownHostException e) {
				  e.printStackTrace();
			  } catch (IOException e) {
				  e.printStackTrace();
			  } catch (ClassNotFoundException e) {
				  e.printStackTrace();
			  }
		  }
	  }
	  synchronizeDataBaseRequest();
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, short processPort, String simulatedIP, short weight) {
	  if(simulatedIP.equals(rd.simulatedIPAddress)) {
		  System.out.println("connecting to self");
		  return;
	  }
	  RouterDescription target = new RouterDescription(processIP, processPort, simulatedIP);
	  Link newLink = new Link(rd, target);
	  int portNum = getFreePort();
	  int isConnected = isRouterInPorts(simulatedIP);
	  System.out.println(isConnected);
	  if (portNum >= 0 && isConnected < 0) {
		  if(addRequest(target, newLink, portNum, weight)) {
			  synchronizeDataBaseRequest();
		  }
	  }
	  else {
		  System.out.println("all ports are occupied at the momment or it is already connected. Please try it again");
		  return;
	  } 
  }



/**
   * output the neighbors of the routers
   */
  private void processNeighbors() {
	  int count = 1;
	  for (int i = 0; i < ports.length; i++) {
		  if(ports[i] != null && ports[i].router2.status == RouterStatus.TWO_WAY) {
			  System.out.println("IP Address of the neighbor" + count + ": " + ports[i].router2.simulatedIPAddress);
			  count++;
		  }
	  }
  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {
	  for (int i = 0; i < ports.length; i++) {
		  if(ports[i] != null) {
			  processDisconnect(ports[i].router2.processPortNumber);
		  }
	  }
	  System.exit(0);
  }

  public void terminal() {
    try {
      System.out.println(this.rd.simulatedIPAddress);
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isReader);
      System.out.print(">> ");
      String command = br.readLine();
      while (true) {
        if (command.startsWith("detect ")) {
          String[] cmdLine = command.split(" ");
          processDetect(cmdLine[1]);
        } else if (command.startsWith("disconnect ")) {
          String[] cmdLine = command.split(" ");
          processDisconnect(Short.parseShort(cmdLine[1]));
        } else if (command.startsWith("quit")) {
          processQuit();
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("start")) {
          processStart();
        } else if (command.startsWith("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        } else if (command.equals("debug")) {
        	this.printPort();
        	System.out.println(lsd);
        } else {
          System.out.println("invalid command");
//          break;
        }
        System.out.print(">> ");
        command = br.readLine();
      }
//      isReader.close();
//      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}

/*
attach 192.168.1.181 4001 192.168.1.100 1
connect 192.168.1.181 4001 192.168.1.100 1
socs.network.router.ip="192.168.3.1"   socs.network.router.port=6001
socs.network.router.ip="192.168.4.1"

socs.network.router.ip="192.168.4.1"
socs.network.router.port=7001


*/