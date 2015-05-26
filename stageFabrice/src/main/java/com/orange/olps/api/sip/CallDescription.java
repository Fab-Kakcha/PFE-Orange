package com.orange.olps.api.sip;


public class CallDescription {
	
	private String caller;
	private String callee;
	private String callId;
	private String codec;
	private String port;
	
	public CallDescription(){
		
	}
	
	public String toString(){
		
		System.out.println("Call description: "+ caller + " " + callee + " "+ callId);
		return null;
	}
	public void setCaller(String caller){
		
		this.caller = caller;
	}
	
	public void setCallee(String callee){
		
		this.callee = callee;
	}
	
	public void setCodec(String codec){
		this.codec = codec;
	}
	
	public String getCaller(){
		return caller;
	}
	
	public String getCallee(){
		return callee;
	}
	
	public void setCallId(String callId){
		this.callId = callId;
	}
	
	public String getCallId(){
		return callId;
	}
	
	public String getCodec(){
		return codec;
	}
	
	/*public void setCallerPort(String port){
		caller = port;
	}
	
	public void setCalleePort(String port){
		callee = port;
	}*/
	
}
