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

	protected static HashMap<OmsCall, String> annuaire = null;

	public Annuaire() {

		annuaire = new HashMap<OmsCall, String>();
	}
	
	/**
	 * Check whether or not the userName provided by the OmsCall/Client already exists in the annuaire. 
	 * A message will be sent to the client/Browser
	 * if the userName already exists. That message is CilentWebSocket.send("userNameExist").
	 * @param call OmsCall/Client/Browser who is trying to connect to OMS
	 * @param userName userName provided by OmsCall/Client/Browser when connecting to OMS
	 * @return true if the userName already exists, so a message is sent through the WebSocket to the 
	 * Client/Browser letting him know the userName exists, and false otherwise if the userName doesn't exist
	 * in the annuaire.
	 */
	public boolean checkUserName(OmsCall call, String userName) {

		if(call == null)
			throw new IllegalArgumentException("Argument OmsCall cannot be null");
		else if(userName==null)
			throw new IllegalArgumentException("Argument String cannot be null");
		
		if (annuaire.containsValue(userName)){		
			call.getWebSocket().send("userNameExist");
			return true;
		}
		else
			return false;
	}
	
	
	/**
	 * Checking if an OmsCall exists in the annuaire
	 * @param omsCall omsCall to check if exist in the annuaire
	 * @return true if the OmsCall exists in the annuaire, and false otherwise
	 */
	public boolean checkOmsCall(OmsCall omsCall){
		
		if(omsCall == null)
			throw new IllegalArgumentException("Argument OmsCall cannot be null");
		
		if(annuaire.containsKey(omsCall))
			return true;
		else 
			return false;		
	}
	
	/**
	 * set the userName as the client's userName if the userName doesn't 
	 * exit in the conference
	 * @param call OmsCall to associate with the userName
	 * @param userName userName provided by the OmsCall/Client/Browser
	 */
	public void setUserName(OmsCall call, String userName){
		
		if(call == null)
			throw new IllegalArgumentException("Argument OmsCall cannot be null");		
		if(userName==null)
			throw new IllegalArgumentException("Argument String cannot be null");
		
		if (!annuaire.containsValue(userName)){		
			annuaire.put(call, userName);
		}	
	}
	
	
/**
 * To show userNames of all OmsCall/Client/Browser currently connected to OMS. 
 * @param call the latest OmsCall/Client/Browser to connect to OMS, or the call who is to leave OMS. 
 * His userName will be sent to other already connected to OMS, and it will received the userName 
 * of the latter when connecting, or he will only sent his userName to others when disconnecting
 * userNames are send through WebSocket.
 * @param bool true if the parameter OmsCall/Client/Browser has connected to OMS, then 
 *  WebSocket.send("showUserNameConnectedtoOMS:"+userName) is sent to others Clients, so they can 
 *  print out his name. He will print out the latter's userNames as well.
 * and false if OmsCall/Client/Browser has disconnected from OMS, then 
 *  WebSocket.send("deleteUserName:"+userName) is sent to others clients, so they can remove his userName from 
 *  the list of people connected to OMS
 * @throws OmsException 
 */
	public void showPeopleConnectedToOms(OmsCall call, boolean bool) throws OmsException{
		
		if(call == null)
			throw new IllegalArgumentException("Argument OmsCall cannot be null");
			
		if (annuaire.containsKey(call)) {

			String userName;
			OmsCall omsCall;
			WebSocket ws;
			
			//Set<WebSocket> listCli = annuaire.keySet();
			Set<OmsCall> listCli = annuaire.keySet();
			Iterator<OmsCall> ite = listCli.iterator();
			String userName2 = annuaire.get(call);

			Collection<String> c = annuaire.values();
			Iterator<String> ite1;

			if (bool) {

				while (ite.hasNext()) {
					
					omsCall = ite.next();
					ws = omsCall.getWebSocket();
					ite1 = c.iterator();
					while (ite1.hasNext()) {

						userName = ite1.next();
						if (ws.equals(call.getWebSocket())) {
							if (!userName2.equals(userName))
								ws.send("showUserNameConnectedToOMS:" + userName);

						} else {
							if (userName2.equals(userName))
								ws.send("showUserNameConnectedToOMS:" + userName);
						}
					}
				}

			} else {

				while (ite.hasNext()) {

					omsCall = ite.next();
					ws = omsCall.getWebSocket();
					ite1 = c.iterator();

					if (ws != call.getWebSocket()) {
						ws.send("deleteUserName:" + userName2);
					}
				}

				annuaire.remove(call);
			}
		} else
			throw new OmsException(
					"cannot show people connected to OMS. OmsCall/Client/Browser is"
							+ " not in the annuaire");	
	}
	
	/**
	 * Updating param, normally param is in form of "confName:mode", but we need to add the userName given by 
	 * the client when connecting to OMS. This userName will be concatenated to param, and no need for the client 
	 * to provide its userName twice. The userName he previously provided is taken from the annuaire.
	 * @param call the OmsCall/Client/Browser connected to OMS
	 * @param param is the concatenation of confName(conference's name) and mode(mode to enter in a conference),
	 * either speaker, mute, student or coach ) in the form of "confName:mode"
	 * @return new value of String param in the form of "userName:confName:mode" is returned if the user is
	 * found in the annuaire and null is returned otherwise.
	 */
	public String updatingStringParam(OmsCall call, String param){
		
		if(call == null)
			throw new IllegalArgumentException("Argument OmsCall cannot be null");
		else if(param == null)
			throw new IllegalArgumentException("Argument String cannot be null");		
		
		if(annuaire.containsKey(call)){
			
			String name = annuaire.get(call);
			param = name +":"+param;	
			
			return param;			
		}else 
			return null;
	}
	
	
	public OmsCall getOmsCall(String userName){
		
		if(userName == null)
			throw new IllegalArgumentException("Argument String cannot be null");
		
		if(annuaire.containsValue(userName)){
			
			OmsCall call;			
			Set<OmsCall> listCli = annuaire.keySet();
			Iterator<OmsCall> ite = listCli.iterator();
			String userName1;
					
			while (ite.hasNext()) {

				call = ite.next();				
				userName1 = annuaire.get(call);
				if(userName.equals(userName1))
					return call;
			}			
		}
		
		return null;		
	}
	
	
	/**
	 * To get the annuaire 
	 * @return the annuaire
	 */
	public HashMap<OmsCall, String> getAnnuaire(){
		 return annuaire;
	}
	
}
