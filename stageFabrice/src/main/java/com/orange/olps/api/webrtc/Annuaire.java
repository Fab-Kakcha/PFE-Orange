/**
 * 
 */
package com.orange.olps.api.webrtc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.java_websocket.WebSocket;

/**
 * @author JWPN9644
 * 
 */
public class Annuaire {

	protected static HashMap<OmsCall, String> annuaire = null;
	
	
	/**
	 * Constructor to initiate the annuaire used to save the user connected to oms
	 */
	public Annuaire() {

		annuaire = new HashMap<OmsCall, String>();
	}
	
	/**
	 * Check whether or not the name provided by the user is already used, and a message ("userNameExist") 
	 * will be sent to the user in case the name is already used
	 * @param call user on his web Browser who is attempting to connect to OMS
	 * @param userName name provided by the user when connecting to OMS
	 * @return true if the userName already exists, thus a message is sent to the 
	 * user through its WebSocket , and false otherwise
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
	 * Checking if an user exists in the annuaire
	 * @param omsCall client to check if exist in the annuaire
	 * @return true if the client exists, and false otherwise
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
	 * To set the userName given in parameter as the user's name if the userName is not already used
	 * @param call user to link with the userName
	 * @param userName name provided by the user through its web Browser
	 * @throws OmsException 
	 */
	public void setUserName(OmsCall call, String userName) throws OmsException{
		
		if(call == null)
			throw new IllegalArgumentException("Argument OmsCall cannot be null");		
		if(userName==null)
			throw new IllegalArgumentException("Argument String cannot be null");
		
		if (!annuaire.containsValue(userName)){		
			call.setUserName(userName);
			annuaire.put(call, userName);
		}
		else 
			throw new OmsException("userName "+ userName + " already used");
	}
	
	
/**
 * To show userNames of all others users already connected to oms to the user is connecting to oms
 * @param call user to connect to OMS, or the call who is to leave OMS. 
 * His userName will be sent to other already connected to OMS, and it will receive the userName 
 * of the latter when connecting, or he will only sent his userName to others when disconnecting
 * @param bool true if the parameter call is connecting to OMS, then a "showUserNameConnectedtoOMS" message
 * is sent to others clients, so they can print out his name. He will print out others users names as well.
 * and false if the parameter call is disconnecting from OMS, then  a "deleteUserName" message 
 * is sent to others clients, so they can remove his userName from the list of people connected to OMS
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
								//ws.send("showUserNameConnectedToOMS:" + userName+":notInConf");							
						} else {
							if (userName2.equals(userName))
								ws.send("showUserNameConnectedToOMS:" + userName);							
								//ws.send("showUserNameConnectedToOMS:" + userName+":notInConf");
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
	
	/*
	 * Updating param, normally param is in form of "confName:mode", but we need to add the userName given by 
	 * the client when connecting to OMS. This userName will be concatenated to param, and no need for the client 
	 * to provide its userName twice. The userName he previously provided is taken from the annuaire.
	 * @param call the OmsCall/Client/Browser connected to OMS
	 * @param param is the concatenation of confName(conference's name) and mode(mode to enter in a conference),
	 * either speaker, mute, student or coach ) in the form of "confName:mode"
	 * @return new value of String param in the form of "userName:confName:mode" is returned if the user is
	 * found in the annuaire and null is returned otherwise.
	 */
	/*public String updatingStringParam(OmsCall call, String param){
		
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
	}*/
	
	/**
	 * To get the client from its userName, will return null if the the userName doesn't match
	 * any client
	 * @param userName name of the user
	 * @return user who has the parameter userName as name
	 */
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
	 * To show the status of others users already in a conference, a "showUserNameConnectedToOMS:" message will
	 * be sent to user given in parameters
	 * @param omsCall user to send other users status
	 * @throws OmsException
	 */
	public void participantsStatus(String userName, OmsConference conf) throws OmsException{
		
		HashMap<String, List<OmsCall>> annuaireForConference = conf.getAnnuaireForConference();	
		Set<String> listCli = annuaireForConference.keySet();
		Iterator<String> ite = listCli.iterator();
		Iterator<OmsCall> ite1;
		String name;
		OmsCall c;
		OmsCall omsCall = this.getOmsCall(userName);
		
		List<OmsCall> listUserName = new ArrayList<OmsCall>();
			
		if(conf.isClientJoined(omsCall)){
			
			while(ite.hasNext()){
				
				name = ite.next();
				listUserName = annuaireForConference.get(name);
				ite1 = listUserName.iterator();
				
				while(ite1.hasNext()){
					
					c = ite1.next();
					if(!c.equals(omsCall))
						omsCall.getWebSocket().send("showUserNameConnectedToOMS:" + c.getUserName());
					//omsCall.getWebSocket().send("showUserNameConnectedToOMS:" + c.getUserName() + ":inConf");
				}
				
			}			
			
		}else{
			
			while(ite.hasNext()){
				
				name = ite.next();
				listUserName = annuaireForConference.get(name);
				ite1 = listUserName.iterator();
				
				while(ite1.hasNext()){
					
					c = ite1.next();
					//if(!c.equals(omsCall))
						omsCall.getWebSocket().send("showUserNameConnectedToOMS:" + c.getUserName());
					//omsCall.getWebSocket().send("showUserNameConnectedToOMS:" + c.getUserName() + ":inConf");
				}
				
			}
		}	
	}
	
	
	/**
	 * To update the status of the user given in parameter to other users in the same conference, his status will 
	 * be updated when he leaves the conference, and a "updateName" message will be sent to other users. The
	 * method was useful for the service implemented on 13rd May 2015. 
	 * @param omsCall user leaving the conference and whom the name is to be updated by other user in the same
	 * conference 
	 * @throws OmsException
	 */
	public void updateName(String userName, OmsConference conf) throws OmsException {
		
		if (userName == null)
			throw new IllegalArgumentException("Argument String cannot be null");
		else if(conf == null)
			throw new IllegalArgumentException("Argument OmsConference cannot be null");

		HashMap<String, List<OmsCall>> annuaireForConference = conf.getAnnuaireForConference();	
		OmsCall omsCall = this.getOmsCall(userName);
		String confName = omsCall.getConfname();
		
		if (confName != null){
			
			boolean bool = annuaireForConference.containsKey(confName);
			if (!bool)
				throw new OmsException("The conference name for the OmsCall is unknown: "+ confName);
			
			List<OmsCall> listOmsCallInConf = annuaireForConference.get(confName);
			if (listOmsCallInConf.isEmpty())
				throw new OmsException("Cannot delete the participant. Nobody in the conference");
			
			ArrayList<OmsCall> clonedList = new ArrayList<OmsCall>();
			ArrayList<OmsCall> clonedList2 = new ArrayList<OmsCall>();
			clonedList.addAll(listOmsCallInConf);
			clonedList2.addAll(listOmsCallInConf);
			
			Iterator<OmsCall> ite2 = clonedList.iterator();	
			Iterator<OmsCall> ite3;		
			OmsCall c2, c3;
			
			while (ite2.hasNext()) {
				c2 = ite2.next();	
				ite3 = clonedList2.iterator();
				
				if(!omsCall.equals(c2)){
					c2.getWebSocket().send("updateName:" + omsCall.getUserName()+":leftConf");
					
					while(ite3.hasNext()){
						c3 = ite3.next();
						if(!c3.equals(c2))						
								if(!c3.equals(omsCall))
									c2.getWebSocket().send("updateName:" + c3.getUserName());													
					}
					
				}else{
					
					while(ite3.hasNext()){
						c3 = ite3.next();					
						if(!c3.equals(c2))	{
							
							c2.getWebSocket().send("deleteUserNameInConf::" + c3.getUserName());
							c2.getWebSocket().send("updateName:" + c3.getUserName()+":iClickedAndLeft");
						}														
					}
				}						
			}			
		}					
	}
	
		
	/**
	 * To get the annuaire containing all clients connected to OMS
	 * @return annuaire with all clients
	 */
	public HashMap<OmsCall, String> getAnnuaire(){
		 return annuaire;
	}
	
}
