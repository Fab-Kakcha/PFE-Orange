package com.orange.olps.stageFabrice.sip;

import java.util.EventObject;

public class OmsMessageEvent extends EventObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String message;

	public OmsMessageEvent(Object source) {
		super(source);
		// TODO Auto-generated constructor stub
	}

	
	public String getMessage(){
		
		return message;
	}
	
	public void setMessage(String msg){
		
		message = msg;
	}
}
