package com.orange.olps.api.sip;

import java.util.EventObject;

public class OmsCallEvent extends EventObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	OmsCallSip omsCallSip;
	
	public OmsCallEvent(Object arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
	
	public OmsCallSip getOmsCallSip(){
		 
		return omsCallSip;
	}
	
	public void setOmsCallSip(OmsCallSip omsCallSip){
		
		this.omsCallSip = omsCallSip;
	}
}
