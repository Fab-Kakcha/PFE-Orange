package com.orange.olps.stageFabrice.sip;

import org.apache.log4j.Logger;

public class MonServiceSip extends OmsServiceSip implements OmsDtmfListener{

	
	private static Logger logger = Logger.getLogger(MonServiceSip.class);
	private static OmsCallSip omsCallSip;
	private static String digit = null;
	//private static String ipAddress = "10.184.50.176"; //Ihda's IP address
	private static String filePath = "/opt/application/64poms/current/tmp/recordingSip.a8k";

	
	/*public static void main(String[] args) {
		
		try {
			
			MonServiceSip monService = new MonServiceSip();
			OmsDtmf dtmf = new OmsDtmf();
			
			boolean isDigitNull = false;
			do {
				
				digit = omsCallSip.dtmf();
				if(digit == null){
					logger.info("You press hangup button");
					isDigitNull = true;
					break;
				}								
				dtmf.addDtmfListener(monService);
				dtmf.fireEvent(digit);
				dtmf.removeDtmfListener(monService);
				
			}while (!digit.equals("#"));
			
			//goodByePrompt();
			
			if(!isDigitNull)
				omsCallSip.hangUp();			
			
		} catch (OmsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.exit(0);
	}*/
	
	
	public MonServiceSip(){
		super();		
		omsCallSip = getOmsCallSip();
		logger.info("Nouvel appelant: " + omsCallSip.getCallDescription().getCaller());
		welcomePrompt();
		
		OmsDtmf dtmf = new OmsDtmf();
		
		boolean isDigitNull = false;
		do {
			
			try {
				digit = omsCallSip.dtmf();
				if(digit == null){
					logger.info("You press hangup button");
					isDigitNull = true;
					break;
				}
				dtmf.addDtmfListener(this);
				dtmf.fireEvent(digit);
				dtmf.removeDtmfListener(this);
			} catch (OmsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
														
		}while (!digit.equals("#"));
		
		goodByePrompt();
		
		if(!isDigitNull)
			try {
				omsCallSip.hangUp();
			} catch (OmsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		System.exit(0);		
		// TODO Auto-generated constructor stub
	}
	
	
	public void welcomePrompt(){
		
		try {
			omsCallSip.say("Bienvenue sur OMS", false);
			omsCallSip.say("Pour écouter un fichier audio tapez 1", false);
			omsCallSip.say("Pour un enregistrement vocal tapez 2", false);
			omsCallSip.say("Pour finir l'enregistrement vocal tapez 3", false);
			omsCallSip.say("Pour quitter tapez #", false);
		} catch (OmsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	
	public void goodByePrompt() {
		
		try {
			omsCallSip.say("Au revoir. A bientot sur OMS", false);
		} catch (OmsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	@Override
	public void dtmfPerformed(OmsDtmfEvent dtmfEvt) {
		// TODO Auto-generated method stub
		
		String dtmf = dtmfEvt.getDtmf();
		switch (dtmf) {
		case "1":
			try {
				omsCallSip.say("Vous avez tapez 1", false);
				omsCallSip.play(filePath, false);
			} catch (OmsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case "2":
			try {
				omsCallSip.say("Vous avez tapez 2", false);
				omsCallSip.enreg(filePath);
			} catch (OmsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case "3":
			try {
				omsCallSip.say("Vous avez tapez 3", false);
				omsCallSip.stopEnreg();
			} catch (OmsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case "4":
			try {
				String digitArray = "003365353535";
				omsCallSip.say("Vous avez tapez 4", false);
				omsCallSip.say("Entrez un numéro s'il vous plait. Faite * pour finir", false);
				
				/*do{					
					digit = omsCallSip.dtmf();
					if(!digit.equals("*"))
						digitArray = digitArray + digit;
					
				}while(!digit.equals("*"));*/
				
				//omsCallSip.dial(digitArray, ipAddress);
				logger.info(digitArray);		
			} catch (OmsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case "#":
			try {
				omsCallSip.say("Vous avez tapez #", false);
			} catch (OmsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		default:				
			try {
				omsCallSip.say("Touche inconnue", false);
			} catch (OmsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		}		
		
	}
}