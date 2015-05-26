package com.orange.olps.api.sip;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class OmsMessage {

	private String message = null;
	private boolean isSdp = true;
	private boolean isCmd = true;
	private boolean isJson = true;
	private Msg msg = null;
	
	public OmsMessage() {		
	}

	public OmsMessage(String m){
		message=m;
		final Gson gson = new GsonBuilder().create();
		try {
			msg = gson.fromJson(message, Msg.class);
			if (msg.getSdp().getSdp() == null) isSdp = false;
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
}

class Msg {
	Sdp sdp = null;
	public Sdp getSdp() {
		return sdp;
	}
}

class Sdp {
	private String type = "";
	private String sdp = "";
	public String getType() {
		return type;
	}
	public String getSdp() {
		return sdp;
	}
}

class Cmd {
	private String commande;
    public String getCmd() {
		return commande;
	}
} 
