package com.orange.olps.api.webrtc;

import java.util.EventObject;

public class OmsMessageEvent extends EventObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String msg;
	private OmsCall omsCall;

	public OmsMessageEvent(Object source, OmsCall omsCall, String msg) {
		super(source);
		this.omsCall = omsCall;
		this.msg = msg;
		// TODO Auto-generated constructor stub
	}

	
	public String getMessage(){
		
		return msg;
	}	
	
	public OmsCall getOmsCall(){
		
		return omsCall;
	}
}
