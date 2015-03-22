package com.orange.olps.stageFabrice.webrtc;

import org.java_websocket.WebSocket;


public class IdentifiersCall {

	//protected WebSocket caller = null;
	CalleeId calleeId;
	CallerId callerId;
	
	public IdentifiersCall(){
			
	}
	
	/**
	 * 
	 * @param caller
	 * @param sdpCaller
	 */
	public IdentifiersCall(WebSocket caller, String sdpCaller){
		
		this.callerId = new CallerId(caller, sdpCaller);
	}
	
	/**
	 * 
	 * @param caller
	 * @param sdpCaller
	 * @param callee
	 * @param calleeWebrtc
	 */
	public IdentifiersCall(WebSocket caller, String sdpCaller, WebSocket callee, OmsCall calleeWebrtc){

		this.callerId = new CallerId(caller, sdpCaller);
		this.calleeId = new CalleeId(callee, calleeWebrtc);	
	}

	public IdentifiersCall(WebSocket caller, String sdpCaller, WebSocket callee){

		this.callerId = new CallerId(caller, sdpCaller);
		this.calleeId = new CalleeId(callee, null);	
	}
	
	/**
	 * 
	 * @return
	 */
	public CallerId getCallerId(){
		
		return this.callerId;
	}
	
	/**
	 * 
	 * @param calleeId
	 */
	public void setCalleeId(CalleeId calleeId){
		
		this.calleeId = calleeId;
	}
	
	/**
	 * 
	 * @return
	 */
	 public CalleeId getCalleeId(){
		 
		 return this.calleeId;
	 }
	
}


class CallerId{
	
	protected WebSocket callerWs = null;
	protected String sdpCaller = null;
	
	public CallerId(WebSocket caller, String sdpCaller){
		
		this.callerWs = caller;
		this.sdpCaller = sdpCaller;
	}
	
	public WebSocket getCallerWs(){
		
		return this.callerWs;
	}
	
	public String getSdpCaller(){
		
		return this.sdpCaller;
	}
}

class CalleeId {
	
	private WebSocket calleeWs = null;
	OmsCall calleeClientWebrtc = null;
	//private String ipAdress;
	//private String identifier;
	
	
	public CalleeId(WebSocket callee, OmsCall calleeClientWebrtc){
		
		this.calleeWs = callee;
		this.calleeClientWebrtc = calleeClientWebrtc;
	}
	
	public void setWebSocket(WebSocket callee){
		
		this.calleeWs = callee;
	}
	
	public void setOmsClientWebrtc(OmsCall calleeClientWebrtc){
		
		this.calleeClientWebrtc = calleeClientWebrtc;
	}
	
	public WebSocket getCalleeWs(){
		
		return this.calleeWs;
	}
	
	public OmsCall getOmsClientWebrtc(){
		
		return this.calleeClientWebrtc;
	}
}