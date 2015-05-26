package com.orange.olps.api.sip;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class OmsServiceSip {

	private static Logger logger = Logger.getLogger(OmsServiceSip.class);
	boolean omsConnected = false;
	private OmsCallSip omsCallSip;
	protected static HashMap<String, OmsCallSip> sipCalls = null;
	
	private Thread t, t1;
	
	private List<OmsDtmfListener> _listeners = new ArrayList<OmsDtmfListener>();
	
	public synchronized void addEventListener(OmsDtmfListener dtmfListener){
		
		_listeners.add(dtmfListener);		
	}
	
	public synchronized void removeEventListener(OmsDtmfListener dtmfListener){
		
		_listeners.remove(dtmfListener);
	}
		
	public OmsServiceSip(String hostVip, String portVip) {
								
		try {
			omsCallSip = new OmsCallSip();
			omsCallSip.connect(hostVip, portVip);
			//omsCallSip.call(number, ipAddress);
		} catch (OmsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void startListeningConf(String hostVipConf, String portVipConf){
				
		try {
			logger.info("Start listening for incoming conf calls: ");
			omsCallSip.initConference(hostVipConf, portVipConf);
			omsCallSip.listenConf();
		} catch (OmsException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void startListening(){
										
		try {

			logger.info("Start listening for incoming calls: ");
			omsCallSip.listen("1");
			newCall(omsCallSip);
			newDtmf(omsCallSip);
		} catch (OmsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
							
			/*t1 = new Thread(new Runnable(){

				@Override
				public void run() {
					
					try {
						omsCallSip = new OmsCallSip();
						omsCallSip.connect(hostVip, portVip);
						logger.info("Listening for incoming call: " + t1.getName());
						omsCallSip.listen("2");
						newCall(omsCallSip);
						newDtmf(omsCallSip);
					} catch (OmsException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			});*/
			
			//t1.start();
	}
	
	public void newCall(OmsCallSip sipCall){
		
		logger.info("Nouvel appelant: " + omsCallSip.getCaller());
		
		OmsDtmfEvent dtmfEvent = new OmsDtmfEvent(sipCall, "newCall");
		Iterator<OmsDtmfListener> i = _listeners.iterator();
		
		while(i.hasNext())  {
			((OmsDtmfListener) i.next()).omsDtmfPerformed(dtmfEvent);
		}
	}
	
	public void newDtmf(OmsCallSip sipCall) throws OmsException{
		
		String digit = null;
		// boolean isDigitNull = false;

		do {

			digit = omsCallSip.dtmf();
			if (digit == null) {
				logger.info("You press hangup button");
				// isDigitNull = true;
				break;
			}

			OmsDtmfEvent dtmfEvent = new OmsDtmfEvent(sipCall, digit);
			Iterator<OmsDtmfListener> i = _listeners.iterator();

			while (i.hasNext()) {
				((OmsDtmfListener) i.next()).omsDtmfPerformed(dtmfEvent);
			}

		} while (!digit.equals("#"));

		/*if(!isDigitNull)
			try {
				omsCallSip.hangUp();
			} catch (OmsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		System.exit(0);	*/		
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
