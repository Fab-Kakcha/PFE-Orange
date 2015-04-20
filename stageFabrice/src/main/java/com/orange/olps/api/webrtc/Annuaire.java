/**
 * 
 */
package com.orange.olps.api.webrtc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.java_websocket.WebSocket;

/**
 * @author JWPN9644
 * 
 */
public class Annuaire {

	// annuaire1 pour identifier le HashMap de chaque conférence.
	//protected static HashMap<String, HashMap<String, WebSocket>> annuaireConf = null;

	protected static HashMap<WebSocket, String> annuaire = null;

	public Annuaire() {

		annuaire = new HashMap<WebSocket, String>();
	}

	/**
	 * Check if the username already exists in the annuaire. Will send a message to the client
	 * if the userName already exist, or the client the userName as the client's userName
	 * @param call the client is trying to connect to OMS
	 * @param userName userName provided by the client when connecting to OMS
	 * @return true if the login already exists, false otherwise
	 */
	public boolean checkUserName(OmsCall call, String userName) {

		//annuaire = annuaireConf.get(call.getConfname());
		if (annuaire.containsKey(userName)){
			
			call.getWebSocket().send("userNameExist");
			return true;
		}
		else{
			annuaire.put(call.getWebSocket(), userName);
			return false;
		}
	}
	

	public void showPeopleConnectedToOms(OmsCall call){
		
		Set<WebSocket> listCli = annuaire.keySet();
		Iterator<WebSocket> ite = listCli.iterator();
		String userName;
		String userName2 = annuaire.get(call.getWebSocket());
		
		Collection<String> c = annuaire.values();
		Iterator<String> ite1;
		
		while(ite.hasNext()){
			
			WebSocket ws = ite.next();
			ite1 = c.iterator();
			while(ite1.hasNext()){
				
				userName = ite1.next();				
				if(ws == call.getWebSocket()){					
					if(userName2 != userName)
						ws.send("showName:"+userName);
					
				}else{
					if(userName2 == userName)
					ws.send("showName:"+userName);
				}				
			}			
		}
	}
	
	/**
	 * Updating param. Normally param is like confName:mode. But we need to add the username the client
	 * has given when connecting to OMS. So the client do not need to provide a username twice.
	 * @param call
	 * @param param
	 * @return
	 */
	public String updatingParam(OmsCall call, String param){
		
		String name = annuaire.get(call.getWebSocket());
		param = name +":"+param;
		
		return param;
	}
	
	
	public void removeUsername(OmsCall call){
		
		annuaire.remove(call.getWebSocket());
	}
	
}
