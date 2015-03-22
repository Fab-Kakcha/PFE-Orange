package com.orange.olps.stageFabrice.webrtc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class OmsMessage {

	private String message = null;
	private String typeMsg = "aucun";
	
	private boolean isSdp = true;
	private boolean isCmd = true;
	private boolean isJson = true;
	private Msg msg = null;
	private CmdParam cmdParam = null;
	
	private static Logger logger = Logger.getLogger(OmsMessage.class);

	
	public OmsMessage() {		
	}

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

	public String getSdp() {
		return msg.getSdp().getSdp();
	}

	public String getType() {
		return msg.getSdp().getType();
	}

	public boolean isSdp() {
		return isSdp;
	}

	public boolean isCmd() {
		return isCmd;
	}

	public boolean isJson() {
		return isJson;
	}
	
	public String getTypeMsg(){	
		return typeMsg;
	}
	
	public String getCmd(){
		return cmdParam.getCmd();
	}
	public String getParam(){
		return cmdParam.getParam();
	}
	
	
	private List<OmsMessageListener> listenersArray = new ArrayList<OmsMessageListener>();
	
	public synchronized void addOmsMessageListener(OmsMessageListener msgListener){
		
		listenersArray.add(msgListener);		
	}
	
	public synchronized void removeOmsMessageListener(OmsMessageListener msgListener){
		
		listenersArray.remove(msgListener);
	}
	
	
	public synchronized void fireEvent(OmsCall call, String msg) throws OmsException{
		
		OmsMessageListener msgLis = null;
		OmsMessageEvent msgEvt = new OmsMessageEvent(this, call, msg);
		Iterator<OmsMessageListener> iter = listenersArray.iterator();
		
		while(iter.hasNext()){
			
			msgLis = iter.next();
			msgLis.omsMessagePerformed(msgEvt);
		}
	}
	
}

class Msg {
	Sdp sdp = null;
	public Sdp getSdp() {
		return sdp;
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
