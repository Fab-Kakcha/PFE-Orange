/**
 * This Java's Class deals with the format of message exchange between OMS and the Browser. Messages are JSON,
 * and the format is {"cmd":"anything","param":"anyhting else"} 
 */

package com.orange.olps.api.webrtc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * @author JWPN9644
 * 
 */


public class OmsMessage {

	private String message = null;
	private String typeMsg = "aucun";
	
	private boolean isSdp = true;
	private boolean isCmd = true;
	private boolean isJson = true;
	private Msg msg = null;
	private CmdParam cmdParam = null;
	
	//private static Logger logger = Logger.getLogger(OmsMessage.class);

	/**
	 * To determine whether the message is an sdp offer or a command
	 * @param m the message receives from the Browser
	 */
	public OmsMessage(String m){
		message=m;
		final Gson gson = new GsonBuilder().create();
		try {
			msg = gson.fromJson(message, Msg.class);
			if(msg.getSdp() == null){
				isSdp = false;
				cmdParam = gson.fromJson(message, CmdParam.class);
				typeMsg = "cmd";
			}
			else{
				if(msg.getSdp().getSdp() == null)
					isSdp = false;
				else typeMsg = "sdp";
				//logger.info("msg.getSdp().getSdp() : " + msg.getSdp().getSdp());
				//logger.info("msg.getSdp().type() : " + msg.getSdp().getType());
			} 
			
		} catch (JsonSyntaxException j) {	
			isJson = false;
			isSdp = false;
		} catch(NullPointerException n) {
			System.out.println("Exception Null");
			isSdp = false;
		}
	}

	/**
	 * To get the sdp
	 * @return sdp
	 */
	public String getSdp() {
		return msg.getSdp().getSdp();
	}

	public String getUserName(){
		return msg.getUserName();
	}

	private boolean isSdp() {
		return isSdp;
	}

	private boolean isCmd() {
		return isCmd;
	}

	private boolean isJson() {
		return isJson;
	}
	
	/**
	 * To get the type of message sends by a Browser, either a sdp or a command
	 * @return either a sdp or a command
	 */
	public String getType(){	
		return typeMsg;
	}
	
	/**
	 * To get the command sends by a Browser
	 * @return a commande
	 */
	public String getCmd(){
		return cmdParam.getCmd();
	}
	
	/**
	 * To get the parameter sends by the Browser
	 * @return parameter
	 */
	public String getParam(){
		return cmdParam.getParam();
	}
	
	
	/*private List<OmsMessageListener> listenersArray = new ArrayList<OmsMessageListener>();
	
	public synchronized void addOmsMessageListener(OmsMessageListener msgListener){
		
		listenersArray.add(msgListener);		
	}
	
	public synchronized void removeOmsMessageListener(OmsMessageListener msgListener){
		
		listenersArray.remove(msgListener);
	}
	
	
	public synchronized void fireEvent(OmsCall call, String msg) throws OmsException{
		
		OmsMessageListener msgLis = null;
		OmsMessageEvent msgEvt = new OmsMessageEvent(call, msg);
		Iterator<OmsMessageListener> iter = listenersArray.iterator();
		
		while(iter.hasNext()){
			
			msgLis = iter.next();
			msgLis.omsMessagePerformed(msgEvt);
		}
	}*/
	
}

class Msg {
	Sdp sdp = null;
	String userName = null;
	public Sdp getSdp() {
		return sdp;
	}
	public String getUserName(){
		return userName;
	}
}

class Sdp {
	private String type = null;
	private String sdp = null;
	public String getType() {
		return type;
	}
	public String getSdp() {
		return sdp;
	}
	public void setSdp(String sdpBrowser) {
		// TODO Auto-generated method stub
		this.sdp = sdpBrowser;
		
	}
	public void setType(String type) {
		// TODO Auto-generated method stub
		this.type = type;
	}
}

class CmdParam {
	private String cmd = null;
	private String param = null;
	
	public String getParam(){
		return param;	
	}
    public String getCmd() {
		return cmd;
	}
} 
