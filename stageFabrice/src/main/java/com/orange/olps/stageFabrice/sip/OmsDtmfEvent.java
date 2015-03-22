package com.orange.olps.stageFabrice.sip;

import java.util.EventObject;

public class OmsDtmfEvent extends EventObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String dtmf;
	
	public OmsDtmfEvent(Object arg0, String dtmf) {
		super(arg0);
		this.dtmf = dtmf;
		// TODO Auto-generated constructor stub
	}
	
	public String getDtmf(){
		
		return dtmf;
	}
	
}
