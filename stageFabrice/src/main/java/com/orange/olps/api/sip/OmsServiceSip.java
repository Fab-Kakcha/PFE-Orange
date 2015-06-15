package com.orange.olps.api.sip;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class OmsServiceSip extends Thread{

	private static Logger logger = Logger.getLogger(OmsServiceSip.class);
	boolean omsConnected = false;
	private OmsCallSip omsCallSip;
	protected static HashMap<String, OmsCallSip> sipCalls = null;
	private String hostVip;
	private String portVip;
	private String service;
	private String threadName;
	
	//private Thread t, t1;
	
	private List<OmsDtmfListener> _listeners = new ArrayList<OmsDtmfListener>();
	
	public synchronized void addEventListener(OmsDtmfListener dtmfListener){
		
		_listeners.add(dtmfListener);		
	}
	
	public synchronized void removeEventListener(OmsDtmfListener dtmfListener){
		
		_listeners.remove(dtmfListener);
	}
	

	public OmsServiceSip(String s, String hostVip, String portVip, String service) {
				
		super(s);

		this.hostVip = hostVip;
		this.portVip = portVip;
		omsCallSip = new OmsCallSip();
		this.service = service;
	}
	
	/*public void startListeningConf(){
				
		try {
			logger.info("Start listening for incoming conf calls: ");
			omsCallSip.listenConf();
			newCall(omsCallSip,"newConf");
			newDtmf(omsCallSip);
		} catch (OmsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	
	//public void startListening(String hostVip, String portVip, int num){
										
		/*try {
			logger.info("Start listening for incoming calls: ");
			omsCallSip.listen();
			newCall(omsCallSip,"newCall");
			newDtmf(omsCallSip);
		} catch (OmsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
							
		/*for(int i=0; i<num ; i++){
			
			MyThread myThread = new MyThread("Thread #"+i, hostVip, portVip);
			myThread.start();
		}	*/	
	//}
	
	protected void newCall(OmsCallSip sipCall, String msg){
		
		logger.info("Nouvel appelant: " + sipCall.getCaller());
		
		OmsDtmfEvent dtmfEvent = new OmsDtmfEvent(sipCall, msg);
		Iterator<OmsDtmfListener> i = _listeners.iterator();
		
		while(i.hasNext())  {
			((OmsDtmfListener) i.next()).omsDtmfPerformed(dtmfEvent);
		}
	}
	
	protected void newDtmf(OmsCallSip sipCall) throws OmsException{
		
		String digit = null;
		// boolean isDigitNull = false;
		do {

			digit = sipCall.dtmf();
			if (digit == null) {
				logger.info("You press hangup button");
				// isDigitNull = true;
				digit = "#";
				//break;
			}

			OmsDtmfEvent dtmfEvent = new OmsDtmfEvent(sipCall, digit);
			Iterator<OmsDtmfListener> i = _listeners.iterator();

			while (i.hasNext()) {
				((OmsDtmfListener) i.next()).omsDtmfPerformed(dtmfEvent);
			}

		} while (!digit.equals("#"));
		
		OmsDtmfEvent dtmfEvent = new OmsDtmfEvent(sipCall, "newThread");
		Iterator<OmsDtmfListener> i = _listeners.iterator();

		while (i.hasNext()) {
			((OmsDtmfListener) i.next()).omsDtmfPerformed(dtmfEvent);
		}	
	}
	
	public void run() {

		try {
			
			threadName = this.getName();
			System.out.println("Run: " + threadName);			

			omsCallSip.connect(hostVip, portVip);
			if(service.equals("call")){
				
				logger.info("Start listening for incoming voice calls: ");
				omsCallSip.listen();
				newCall(omsCallSip, "newCall");
				
			}else if(service.equals("conf")){
				
				logger.info("Start listening for incoming conference calls: ");
				omsCallSip.listenConf();
				newCall(omsCallSip,"newConf");
			}
						
			newDtmf(omsCallSip);
			
			
		} catch (OmsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			
			//this.s
		}
	}
		
	
	public String getService(){		
		return service;
	}
	
	public static void dort(int temps) {
		try {
			Thread.sleep(temps);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	
	public static void logError(String attendu, String recu) {
		logger.error("ERREUR TEST : Reponse OMS incorrecte\n"
				+ "        Attendu : " + attendu + "\n        Recu    : "
				+ recu);
	}
}

