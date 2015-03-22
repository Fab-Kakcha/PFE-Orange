package com.orange.olps.stageFabrice.webrtc;

import java.util.EventObject;

public class OmsCallEvent extends EventObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	OmsCall omsCall;
	private String ip;
	
	public OmsCallEvent(Object arg0, OmsCall omsCall, String ip) {
		super(arg0);
		this.omsCall = omsCall;
		this.ip = ip;
		// TODO Auto-generated constructor stub
	}
	
	public OmsCall getOmsCall(){
		 
		return omsCall;
	}
	
	public String getIP(){
		
		return ip;
	}
}
