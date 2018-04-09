package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.TimerTask;
import java.util.Timer;
public class HeartbeatTask extends TimerTask {
	private Router rt;
	
	public HeartbeatTask (Router rt) {
		super();
		this.rt = rt;
	}
	
	@Override
	public void run() {
		for (int i = 0; i < rt.ports.length; i++) {
			if(rt.ports[i] != null) {
				try {
					String processIP = rt.ports[i].router2.processIPAddress;
					short processPort = rt.ports[i].router2.processPortNumber;
					
					Socket client = new Socket(processIP, processPort);
					ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());
					ObjectInputStream input = new ObjectInputStream(client.getInputStream());
			        client.setSoTimeout(5000);
			        
					output.writeObject(rt.ports[i].router2.simulatedIPAddress);
			        String incoming;
	          		try {
	          			incoming = (String) input.readObject();
	          			if (incoming.equals("good") || incoming.equals("full")) {
          					continue;
          				}
	          			else {
	          				System.out.println("hmm");
	          			}
	          		} catch (ClassNotFoundException e) {
	          			e.printStackTrace();
	          		} catch (SocketTimeoutException s) {
	          	        rt.removeLinkedDescription(rt.ports[i].router2);
	          	        rt.lsd._store.remove(rt.ports[i].router2.simulatedIPAddress);
	          	        rt.ports[i] = null;
	          	        rt.synchronizeDataBaseRequest();
	          	    } 
			        output.close();
			        input.close();
			        client.close();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}  catch (ConnectException e) {
          	    	rt.removeLinkedDescription(rt.ports[i].router2);
          	        rt.lsd._store.remove(rt.ports[i].router2.simulatedIPAddress);
          	        rt.ports[i] = null;
          	        rt.synchronizeDataBaseRequest();
          	    } catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
		
	}

}
