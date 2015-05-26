package com.orange.olps.api.sip;

import java.util.EventObject;

public class OmsDtmfEvent extends EventObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String dtmf;
	private OmsCallSip sipCall;
	
	public OmsDtmfEvent(OmsCallSip sipCall, String dtmf) {
		super(dtmf);
		this.sipCall = sipCall;
		this.dtmf = dtmf;
		// TODO Auto-generated constructor stub
	}
	
	public String getDtmf(){		
		return dtmf;
	}
	
	public OmsCallSip getOmsCallSip(){
		return sipCall;
	}
	
}
