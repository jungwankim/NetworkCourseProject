package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import socs.network.message.LSA;
import socs.network.message.SOSPFPacket;

public class RouterRequestListener implements Runnable{
	private Router rt;
	
	public RouterRequestListener (Router rt) {
		this.rt = rt;
	}
	
	public void run() {
        ServerSocket socket;
		try {
			socket = new ServerSocket(rt.rd.processPortNumber);
			while (true) {
				Socket rtListener = socket.accept();
		      	ObjectOutputStream out = new ObjectOutputStream(rtListener.getOutputStream());
			    ObjectInputStream in = null;
			    Object rq;
				try {
					in = new ObjectInputStream(rtListener.getInputStream());
					rq = in.readObject();
					//ip address is sent for check up 
					if (rq instanceof String) {
						String checkIP = (String) rq;
						if (checkIP.equals(rt.rd.simulatedIPAddress)) {
							if (rt.getFreePort() >= 0) {
								out.writeObject("good");
							}
							else{ 
								out.writeObject("full");
							}
						}
						else {
							out.writeObject("bad");
						}
					}
					//packet is sent
					else {
						SOSPFPacket packet = (SOSPFPacket) rq;
						//hello
						if (packet.sospfType == 0) {
							RouterDescription newRouter = new RouterDescription(packet.srcProcessIP, packet.srcProcessPort, packet.srcIP);
							
							int existingRouter = rt.isRouterInPorts(newRouter.simulatedIPAddress);
							int portNum = this.rt.getFreePort();
							if (existingRouter >= 0) {
								portNum = existingRouter;
							}
							
							//port is not full
							if (portNum >= 0) {
								this.rt.ports[portNum] = new Link(rt.rd, newRouter);

								System.out.println("");
								System.out.println("received HELLO from " + packet.srcIP + ";");
								System.out.println("set " + packet.srcIP + " state to INIT");
								
								this.rt.ports[portNum].router1.status = RouterStatus.INIT;
								this.rt.ports[portNum].router2.status = RouterStatus.INIT;
								
								SOSPFPacket packetSending = new SOSPFPacket();
								packetSending.dstIP = packet.srcIP;
								packetSending.srcIP = this.rt.rd.simulatedIPAddress;
								packetSending.srcProcessIP = this.rt.rd.processIPAddress;
								packetSending.srcProcessPort = this.rt.rd.processPortNumber;
								packetSending.sospfType = 0;
								out.writeObject(packetSending);
								packet = (SOSPFPacket) in.readObject();
								System.out.println("received HELLO from " + packet.srcIP + ";");
								this.rt.ports[portNum].router1.status = RouterStatus.TWO_WAY;
								this.rt.ports[portNum].router2.status = RouterStatus.TWO_WAY;
								System.out.println("set " + packet.srcIP + " state to TWO_WAY");
								rt.addLinkedDescription(newRouter, packet.weight);
							}
							//port is full
							else {
								SOSPFPacket packetSending = new SOSPFPacket();
								packetSending.dstIP = packet.srcIP;
								packetSending.srcIP = this.rt.rd.simulatedIPAddress;
								packetSending.srcProcessIP = this.rt.rd.processIPAddress;
								packetSending.srcProcessPort = this.rt.rd.processPortNumber;
								packetSending.sospfType = 2;
								out.writeObject(packetSending);
								System.out.println("received hello but all ports are occupied");
							}
						}
						// broadcast request
						else if (packet.sospfType == 1) {
								  boolean upToDated = true;
								  for (int i = 0; i < packet.lsaArray.size(); i++) {
									  LSA comparingLSA = packet.lsaArray.get(i);
									  if (rt.lsd._store.containsKey(comparingLSA.linkStateID)) {
										  if (rt.lsd._store.get(comparingLSA.linkStateID).lsaSeqNumber < comparingLSA.lsaSeqNumber) {
											  upToDated = false;
											  rt.lsd._store.put(comparingLSA.linkStateID, comparingLSA);
										  }
									  }
									  else {
										  upToDated = false;
										  rt.lsd._store.put(comparingLSA.linkStateID, comparingLSA);
									  }  
								  }
								  if (!upToDated) {
									  rt.synchronizeDataBaseRequest();
								  }
						}
						// add request
						else if (packet.sospfType == 3) {
							RouterDescription newRouter = new RouterDescription(packet.srcProcessIP, packet.srcProcessPort, packet.srcIP);
							
							int existingRouter = rt.isRouterInPorts(newRouter.simulatedIPAddress);
							int portNum = this.rt.getFreePort();
							if (existingRouter >= 0) {
								portNum = existingRouter;
							}
							
							//port is not full
							if (portNum >= 0) {
								rt.ports[portNum] = new Link(rt.rd, newRouter);
								rt.ports[portNum].router1.status = RouterStatus.TWO_WAY;
								rt.ports[portNum].router2.status = RouterStatus.TWO_WAY;
								rt.addLinkedDescription(newRouter, packet.weight);
								SOSPFPacket packetSending = new SOSPFPacket();
								packetSending.dstIP = packet.srcIP;
								packetSending.srcIP = this.rt.rd.simulatedIPAddress;
								packetSending.srcProcessIP = this.rt.rd.processIPAddress;
								packetSending.srcProcessPort = this.rt.rd.processPortNumber;
								packetSending.sospfType = 3;
								out.writeObject(packetSending);
							}
							//port is full
							else {
								SOSPFPacket packetSending = new SOSPFPacket();
								packetSending.dstIP = packet.srcIP;
								packetSending.srcIP = this.rt.rd.simulatedIPAddress;
								packetSending.srcProcessIP = this.rt.rd.processIPAddress;
								packetSending.srcProcessPort = this.rt.rd.processPortNumber;
								packetSending.sospfType = 2;
								out.writeObject(packetSending);
								System.out.println("received hello but all ports are occupied");
							}
						}
						else if (packet.sospfType == 4) {
							RouterDescription newRouter = new RouterDescription(packet.srcProcessIP, packet.srcProcessPort, packet.srcIP);
							
							int existingRouter = rt.isRouterInPorts(newRouter.simulatedIPAddress);
							
							//exist so delete
							if (existingRouter >= 0) {
								rt.ports[existingRouter] = null;
								rt.removeLinkedDescription(newRouter);
								SOSPFPacket packetSending = new SOSPFPacket();
								packetSending.dstIP = packet.srcIP;
								packetSending.srcIP = this.rt.rd.simulatedIPAddress;
								packetSending.srcProcessIP = this.rt.rd.processIPAddress;
								packetSending.srcProcessPort = this.rt.rd.processPortNumber;
								packetSending.sospfType = 4;
								Vector<LSA> lsaA = new Vector<LSA>();
								lsaA.add(rt.lsd._store.get(rt.rd.simulatedIPAddress));
								packetSending.lsaArray = lsaA;
								out.writeObject(packetSending);
							}
						}
					} 
					rtListener.close();
					out.flush();
			      	out.close();
				    in.close(); 
				} catch (ClassNotFoundException e) {
					
				} catch (Exception e) {
					
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
        }
	}
}
