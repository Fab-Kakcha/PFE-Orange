package com.orange.olps.api.sip;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class OmsCallSip extends Thread {

	private static Logger logger = Logger.getLogger(OmsCallSip.class);
	private static Pattern pat = Pattern.compile("value=\"([^\\s/>]+)");
	
	private VipConnexion connOMS = null;
	private boolean isRviSyntExist = false;
	private boolean isRviEnregExist = false;
	private boolean isRviDtmfExist = false;
	private String regexSpace = "\\s";
	private String regexEqual = "=";
	private String regexMult = "[\\s\";]";
	
	private String clientIpAddress = null;
	private String clientPort = null;
	private String clientCodec = null;
	private String acodecParam = null;
	private String omsIpAddress = null;
	private String rviMediaPort = null;
	private int partNumberConf = -10000;

	private String[] array = null;
	private CallDescription callDescription = null;
	private int callNumber = 0;
	private String[] hosPortVip = null;
	private String confName = null;
	private boolean isTrombone = false;
	
	//problème avec l'enregistrement et la lecture vidéo
	
	
	public OmsCallSip() {

		super();
		callNumber =1;	
	}

	/**
	 * To connect to OMS at IP address hostVip and port portVip
	 * @param hostVip OMS IP address
	 * @param portVip OMS port 
	 * @throws OmsException
	 */
	protected void connect(String hostVip, String portVip) throws OmsException {
		
		hosPortVip = new String[2];
		hosPortVip[0] = hostVip;
		hosPortVip[1] = portVip;
		
		callDescription = new CallDescription();
		this.omsIpAddress = hostVip;
		
		this.connOMS = new VipConnexion(hostVip, portVip);
		String rviSip = this.connOMS.getReponse("new t1 sip");
		if (!rviSip.equals("OK"))
			throw new OmsException("cannot create rvi sip :" + rviSip);

		String rviMedia = this.connOMS.getReponse("new mt1 media");
		if (!rviMedia.equals("OK"))
			throw new OmsException("cannot create rvi media " + rviMedia);
		
	}

	public void shutup() throws OmsException{
		
		String shutup = connOMS.getReponse("t1 shutup");
		if(shutup.indexOf("OK") == -1)
			throw new OmsException("shutup command failed: " + shutup);
	}
	
	private void processingWithListen() throws OmsException{
		
		String respWait = connOMS.getReponse("wait evt=t1.*");
		if(respWait.indexOf("OK") == -1)
			throw new OmsException("Issues with the incoming event: " + respWait);
		
		
		String[] splitRespWait = respWait.split(regexSpace);
		for (String s : splitRespWait) {

			if (s.startsWith("local")) {//on récupère le local
				array = s.split("local=");
				logger.info(Arrays.toString(array));
				callDescription.setCallee(array[array.length - 1]);
			} else if (s.startsWith("remote")) {//On récupère le remote
				array = s.split(regexEqual);
				callDescription.setCaller(array[array.length - 1]);
			} else if (s.startsWith("call")) {//On récupère le callId
				array = s.split(regexEqual);
				String callId = array[array.length - 1];
				//if(callId.endsWith(".")) //Faut il supprimer le point à la
				// fin???
				// callId = callId.substring(0, callId.length() - 1);

				callDescription.setCallId(callId);
			}
		}

		String callee = callDescription.getCallee();
		array = callee.split("conf=");
		confName = array[array.length - 1].split("@")[0];
		this.setConfName(confName);
		
		String respMedia = connOMS.getReponse("wait evt=t1.media,t1.hangup");
		
		//Afficher la valeur du codec
		logger.info(respMedia);
		
		if(respMedia.indexOf("OK") == -1)
			throw new OmsException("issues with wait command: " + respMedia);			

		String[] splitRespMedia = respMedia.split(regexMult);
		for (String s : splitRespMedia) {

			if (s.startsWith("content")) {
				array = s.split(regexEqual);
				clientIpAddress = array[array.length - 1];

			} else if (s.startsWith("aport")) {
				array = s.split(regexEqual);
				clientPort = array[array.length - 1];

			} else if (s.startsWith("acodecparam")) {
				array = s.split(regexEqual);
				acodecParam = array[array.length - 1];

			} else if (s.startsWith("acodec")) {
				array = s.split(regexEqual);
				clientCodec = array[array.length - 1];
			}else if(s.startsWith("cause")){// hangup due to a "CANCEL" request			
				deleteRvi();
				System.exit(0);
			}
		}
	
		String respStart = connOMS
				.getReponse("mt1 startlocal type=audio codec=" + clientCodec + " codecparam=payload=" + 
		acodecParam + ",ptime=20");
		
		if(respStart.indexOf("OK") == -1)
			throw new OmsException("Issues with startlocal command: " + respStart);

		String[] splitRespStart = respStart.split(regexSpace);
		for (String s : splitRespStart) {

			if (s.startsWith("local.aport")) {
				array = s.split(regexEqual);
				rviMediaPort = array[array.length - 1];
			}
		}
		
		logger.info("rvi Media port: " + rviMediaPort);
		
		sleep(6000);
		String repAnswer = connOMS.getReponse("t1 answer \"media=ip=" + getOmsIpAddress()
				+ " aport=" + rviMediaPort + " acodec=" + clientCodec + "\"");
		if(repAnswer.indexOf("OK") == -1)
			throw new OmsException("Answer command failed: " + repAnswer);
	
		respStart = connOMS.getReponse("mt1 startremote type=audio host=" + clientIpAddress
				+ " port=" + clientPort + "" + " codec=" + clientCodec
				+ " codecparam=payload=" + acodecParam + ",ptime=20");
		if(respStart.indexOf("OK") == -1)
			throw new OmsException("Issues with startremote command: " + respStart);

		String respWaitEvent = connOMS.getReponse("wait evt=t1*,mt1.*");
		String[] splitResp = respWaitEvent.split(regexSpace);
		for(String s : splitResp){
			
			if(s.startsWith("cause")){ //hangup due to a "CANCEL" request
				logger.info("The call has been cancelled");
				deleteRvi();
				System.exit(0);
			}									
		}
		
		if(respWaitEvent.indexOf("OK") == -1)
			throw new OmsException("connected evt failed: " + respWaitEvent);
		
		
		String respInfoEvt = connOMS.getReponse("wait evt=t1.info");
		if(respInfoEvt.indexOf("OK") == -1)
			throw new OmsException("wait evt=t1.info failed: " + respInfoEvt);
		
		Matcher mat = pat.matcher(respInfoEvt);
		
		do{
			
			logger.info(mat.group());
			
		}while(mat.find());
		
	}
	
	/**
	 * To listen to incoming calls to join a conference
	 * @throws OmsException
	 */
	protected synchronized void listenConf() throws OmsException{
		
		String listen = connOMS.getReponse("t1 listen sip:conf=*@");
		if(listen.indexOf("OK") == -1)
			throw new OmsException("Cannot start listening to incoming calls: " + listen);
				
		processingWithListen();
		
		/*String respWait = connOMS.getReponse("wait evt=t1.*");
		if(respWait.indexOf("OK") == -1)
			throw new OmsException("Issues with the incoming event: " + respWait);
		
		
		String[] splitRespWait = respWait.split(regexSpace);
		for (String s : splitRespWait) {

			if (s.startsWith("local")) {//on récupère le local
				array = s.split("local=");
				logger.info(Arrays.toString(array));
				callDescription.setCallee(array[array.length - 1]);
			} else if (s.startsWith("remote")) {//On récupère le remote
				array = s.split(regexEqual);
				callDescription.setCaller(array[array.length - 1]);
			} else if (s.startsWith("call")) {//On récupère le callId
				array = s.split(regexEqual);
				String callId = array[array.length - 1];
				//if(callId.endsWith(".")) //Faut il supprimer le point à la
				// fin???
				// callId = callId.substring(0, callId.length() - 1);

				callDescription.setCallId(callId);
			}
		}

		String callee = callDescription.getCallee();
		array = callee.split("conf=");
		confName = array[array.length - 1].split("@")[0];
		logger.info("confName: " + confName);
		this.setConfName(confName);
		
		String respMedia = connOMS.getReponse("wait evt=t1.media,t1.hangup");
		logger.info("respMedia: " + respMedia);

		String[] splitRespMedia = respMedia.split(regexMult);
		for (String s : splitRespMedia) {

			if (s.startsWith("content")) {
				array = s.split(regexEqual);
				clientIpAddress = array[array.length - 1];

			} else if (s.startsWith("aport")) {
				array = s.split(regexEqual);
				clientPort = array[array.length - 1];

			} else if (s.startsWith("acodecparam")) {
				array = s.split(regexEqual);
				acodecParam = array[array.length - 1];

			} else if (s.startsWith("acodec")) {
				array = s.split(regexEqual);
				clientCodec = array[array.length - 1];
			}else if(s.startsWith("cause")){// hangup due to a "CANCEL" request			
				deleteRvi();
				System.exit(0);
			}
		}
	
		String respStart = connOMS
				.getReponse("mt1 startlocal type=audio codec=" + clientCodec + " codecparam=payload=" + 
		acodecParam + ",ptime=20");

		String[] splitRespStart = respStart.split(regexSpace);
		for (String s : splitRespStart) {

			if (s.startsWith("local.aport")) {
				array = s.split(regexEqual);
				rviMediaPort = array[array.length - 1];
			}
		}
		
		sleep(6000);
		String repAnswer = connOMS.getReponse("t1 answer \"media=ip=" + getOmsIpAddress()
				+ " aport=" + rviMediaPort + " acodec=" + clientCodec + "\"");
		if(repAnswer.indexOf("OK") == -1)
			throw new OmsException("Answer command failed: " + repAnswer);
	
		connOMS.getReponse("mt1 startremote type=audio host=" + clientIpAddress
				+ " port=" + clientPort + "" + " codec=" + clientCodec
				+ " codecparam=payload=" + acodecParam + ",ptime=20");

		String respWaitEvent = connOMS.getReponse("wait evt=t1*,mt1.*");
		String[] splitResp = respWaitEvent.split(regexSpace);
		for(String s : splitResp){
			
			if(s.startsWith("cause")){ //hangup due to a "CANCEL" request
				logger.info("The call has been cancelled");
				deleteRvi();
				System.exit(0);
			}									
		}
		
		if(respWaitEvent.indexOf("OK") == -1)
			throw new OmsException("connected evt failed: " + respWaitEvent);*/
		
		/*if(omsConference.status(confName))			
			omsConference.add(this, confName);
		else 
			omsConference.create(this, confName);*/		
	}
	
	/**
	 * To listen to incoming calls for voicing services
	 * @throws OmsException
	 */
	protected synchronized void listen() throws OmsException { // Call c
			
			String listen = connOMS.getReponse("t1 listen sip:*@");
			if(listen.indexOf("OK") == -1)
				throw new OmsException("Cannot start listening to incoming calls: " + listen);
			
			processingWithListen();
			
			/*String respWait = connOMS.getReponse("wait evt=t1.*");
			String[] splitRespWait = respWait.split(regexSpace);
			for (String s : splitRespWait) {

				if (s.startsWith("local")) {//on récupère le local
					array = s.split(regexEqual);
					callDescription.setCallee(array[array.length - 1]);
				} else if (s.startsWith("remote")) {//On récupère le remote
					array = s.split(regexEqual);
					callDescription.setCaller(array[array.length - 1]);
				} else if (s.startsWith("call")) {//On récupère le callId
					array = s.split(regexEqual);
					String callId = array[array.length - 1];
					//if(callId.endsWith(".")) //Faut il supprimer le point à la
					// fin???
					// callId = callId.substring(0, callId.length() - 1);

					callDescription.setCallId(callId);
				}
			}

			//callDescription.toString();
			String respMedia = connOMS.getReponse("wait evt=t1.media,t1.hangup");
			//logger.info(respMedia);

			String[] splitRespMedia = respMedia.split(regexMult);
			for (String s : splitRespMedia) {

				if (s.startsWith("content")) {
					array = s.split(regexEqual);
					clientIpAddress = array[array.length - 1];

				} else if (s.startsWith("aport")) {
					array = s.split(regexEqual);
					clientPort = array[array.length - 1];

				} else if (s.startsWith("acodecparam")) {
					array = s.split(regexEqual);
					acodecParam = array[array.length - 1];

				} else if (s.startsWith("acodec")) {
					array = s.split(regexEqual);
					clientCodec = array[array.length - 1];
				}else if(s.startsWith("cause")){// hangup due to a "CANCEL" request			
					deleteRvi();
					System.exit(0);
				}
			}
		
			String respStart = connOMS
					.getReponse("mt1 startlocal type=audio codec=" + clientCodec + " codecparam=payload=" + 
			acodecParam + ",ptime=20");

			String[] splitRespStart = respStart.split(regexSpace);
			for (String s : splitRespStart) {

				if (s.startsWith("local.aport")) {
					array = s.split(regexEqual);
					rviMediaPort = array[array.length - 1];
				}
			}
			
			sleep(6000);
			String repAnswer = connOMS.getReponse("t1 answer \"media=ip=" + getOmsIpAddress()
					+ " aport=" + rviMediaPort + " acodec=" + clientCodec + "\"");		
			if(repAnswer.indexOf("OK") == -1)
				throw new OmsException("Answer command failed: " + repAnswer);
			
			connOMS.getReponse("mt1 startremote type=audio host=" + clientIpAddress
					+ " port=" + clientPort + "" + " codec=" + clientCodec
					+ " codecparam=payload=" + acodecParam + ",ptime=20");

			String respWaitEvent = connOMS.getReponse("wait evt=t1*,mt1.*");
			String[] splitResp = respWaitEvent.split(regexSpace);
			for(String s : splitResp){
				
				if(s.startsWith("cause")){ //hangup due to a "CANCEL" request
					logger.info("The call has been cancelled");
					deleteRvi();
					System.exit(0);
				}			
			}*/					
	}

	/**
	 * To forward a call once a media session has been set up
	 * @param isBlind says is the forwarding is in blind or in consultation
	 * @param ipDestOrRvi IP address or rvi name of the destination
	 * @throws OmsException
	 */
	public void callForwarding(boolean isBlind, String ipDestOrRvi) throws OmsException{
		
		String status ="";
		String redirect;
		if(isBlind){
			
			redirect = connOMS.getReponse("t1 redirect blind sip:" + ipDestOrRvi);
			if(redirect.indexOf("OK") == -1)
				throw new OmsException("Cannot excute redirect command: " + redirect);
			
			String forwardStatus =  connOMS.getReponse("wait evt=t1.redirected");
			if(forwardStatus.indexOf("OK") == -1)
				throw new OmsException("redirected event failed: " + forwardStatus);
			
			String[] split = forwardStatus.split(regexSpace);
			for(String s : split){
				
				if(s.startsWith("status")){
					array = s.split(regexEqual);
					status = array[array.length - 1];
				}
			}
			
		}else{
			
			redirect = connOMS.getReponse("t1 redirect consultation " + ipDestOrRvi);
			if(redirect.indexOf("OK") == -1)
				throw new OmsException("Cannot excute redirect command: " + redirect);
			
			String forwardStatus =  connOMS.getReponse("wait evt=t1.redirected");
			if(forwardStatus.indexOf("OK") == -1)
				throw new OmsException("redirected event failed: " + forwardStatus);
			
			String[] split = forwardStatus.split(regexSpace);
			for(String s : split){
				
				if(s.startsWith("status")){
					array = s.split(regexEqual);
					status = array[array.length - 1];
				}
			}
		}
		
		if(status.equals("OK")){
			String notify = connOMS.getReponse("wait evt=t1.notify");
			if(notify.indexOf("OK") == -1)
				throw new OmsException("notify evts failed: " + notify);
			
			notify = connOMS.getReponse("wait evt=t.hangup");
			if(notify.indexOf("OK") == -1)
				throw new OmsException("notify evts failed: " + notify);
			
		}else if(status.equals("FAILED"))
			logger.error("Cannot redirect call");		
	}
	
	
	private void trombone(String rviM1, String rviM2) throws OmsException{
		
		String repTrom = connOMS.getReponse(rviM1 + " extend trombone " + rviM2);
		if(repTrom.indexOf("OK") == -1)
			throw new OmsException("Cannot trombone the two rvi media: " + repTrom);
	}
	
	private void unTrombone(String rviM1, String rviM2) throws OmsException{
		
		String repUntrom = connOMS.getReponse(rviM1 + " extend untrombone " + rviM2);
		if(repUntrom.indexOf("OK") == -1)
			throw new OmsException("Cannot untrombone the two rvi media: " + repUntrom);		
	}
	
	
	public void hold() throws OmsException{
		
		String hold = connOMS.getReponse("t1 hold");
		if(hold.indexOf("OK") == -1)
			throw new OmsException("hold command failed: " + hold);
	}
	
	
	public void unhold() throws OmsException{
		
		String unhold = connOMS.getReponse("t1 unhold");
		if(unhold.indexOf("OK") == -1)
			throw new OmsException("hold command failed: " + unhold);
	}
	
	/**
	 * To get the media informations from OMS
	 * @return media informations
	 * @throws OmsException cannot get mediatypes: + error messsage
	 */
	public String getMediatypes() throws OmsException{
		
		String media = connOMS.getReponse("t1 getmediatypes");
		if(media.indexOf("OK") == -1)
			throw new OmsException("cannot get mediatypes: " + media);
		
		return media;	
	}
	
	private void deleteRvi() throws OmsException{
		
		int num = 1;
		String delete;
		for(int i =1 ; i <= num ; i++){
		
			String delT1 = connOMS.getReponse("delete t"+i);
			if(!delT1.equals("OK"))
				throw new OmsException("cannot delete rvi sip t1: " + delT1);
			
			connOMS.getReponse("delete mt1");			
		}
				
		/*String delT1 = connOMS.getReponse("delete t1");
		if(!delT1.equals("OK"))
			throw new OmsException("cannot delete rvi sip t1: " + delT1);*/

		if(isRviEnregExist){
			delete = connOMS.getReponse("delete recorder");
			if(delete.indexOf("OK") == -1)
				throw new OmsException("cannot delete rvi enreg: " + delete);
		}	
		else if(isRviSyntExist){
		
			String delS1 = connOMS.getReponse("delete s1");
			if(!delS1.equals("OK"))
				throw new OmsException("cannot delete rvi synt s1: " + delS1);
		}	
		else if(isRviDtmfExist){
			String delD = connOMS.getReponse("delete d");
			if(!delD.equals("OK"))
				throw new OmsException("cannot delete rvi dtmf : " + delD);
		}
		
		/*String stopRemote = connOMS.getReponse("mt1 stop remote");
		if(!stopRemote.equals("OK"))
			throw new OmsException("cannot stop remote media: " + stopRemote);
		
		String stopLocal = connOMS.getReponse("mt1 stop local");
		if(!stopLocal.equals("OK"))
			throw new OmsException("cannot stop remote media: " + stopLocal);
		
		String delLocal = connOMS.getReponse("mt1 delete local");
		if(!delLocal.equals("OK"))
			throw new OmsException("cannot delete local media: " + delLocal);
		
		String delRemote = connOMS.getReponse("mt1 delete remote");
		if(!delRemote.equals("OK"))
			throw new OmsException("cannot delete remote media: " + delRemote);*/
		
		//connOMS.getReponse("delete mt1");
		//connOMS.getReponse("wait evt=mt1.starving");	
	}
	
	/**
	 * To delete an ongoing call to OMS
	 * @throws OmsException
	 * @throws IOException 
	 */
	public void delete() throws OmsException, IOException {
		
		deleteRvi();
		connOMS.getSocket().close();	
	}

	/**
	 * To synthesize a speech
	 * @param say message to synthesize 
	 * @param interrupt true if the message can be interrupt and false otherwise
	 * @throws OmsException
	 */
	public void say(String say, boolean interrupt) throws OmsException {

		if (!isRviSyntExist) {
			String resp1 = connOMS.getReponse("new s1 synt");
			if (!resp1.equals("OK"))
				throw new OmsException("Error: Cannot create rvi synt " + resp1);
			
			String setParam = connOMS.getReponse("s1 setparam bindto=mt1");
			if(setParam.indexOf("OK") == -1)
				throw new OmsException("Error with setparam command: " + setParam);
			
			isRviSyntExist = true;
		}

		/*
		 * String respSh = connOMS.getReponse("mt1 shutup");
		 * if(!respSh.equals("OK")) throw new OmsException("Cannot shutup mt1");
		 */

		String respSay = connOMS.getReponse("s1 say \"" + say + "\"");
		if (!respSay.equals("OK"))
			throw new OmsException("Cannot send cmd say to OMS " + respSay);

		if (!interrupt) {
			String resp = connOMS.getReponse("wait evt=mt1.starving");
			if (!resp.startsWith("OK")) {
				System.out.println("cmd wait evt=mt1.starving failed: " + resp);
			}
		}
	}
	
	/**
	 * To hang up and unjoin an ongoing call between two users
	 * @throws OmsException
	 */
	public void hangupCall() throws OmsException{
		
		int callNum = 2;
		
		unTrombone("mt1", "mt"+callNum);
		isTrombone = false;
		String del = connOMS.getReponse("delete t"+callNum);
		if(del.indexOf("OK") == -1)
			throw new OmsException("cannot delete rvi sip: " + del);
		
		del = connOMS.getReponse("delete mt"+callNum);
		if(del.indexOf("OK") == -1)
			throw new OmsException("cannot delete rvi media: " + del);
	}
	
	/**
	 * To make an outgoing call to a sip number in the form sip:number@ipAddress
	 * @param number field number of the sip address
	 * @param ipAddress ipAddress field in the sip address
	 * @throws OmsException
	 */
	public void call(String number, String ipAddress) throws OmsException {

		//int callNum = getCallNumber();
		int callNum = 2;
		//setCallNumber(callNum);
		String rviSip;
		
		String port = "";
		rviSip = connOMS.getReponse("new t"+ callNum +" sip");
		if (!rviSip.equals("OK"))
			throw new OmsException("cannot create rvi sip :" + rviSip);
		
		rviSip = connOMS.getReponse("new mt"+callNum+" media");
		if (!rviSip.equals("OK"))
			throw new OmsException("cannot create rvi media :" + rviSip);
		
		String reservePort = connOMS.getReponse("mt"+callNum+" reserveport type=audio");
		//String reservePort = connOMS.getReponse("mt"+callNum+" reserveport type=video");
		if(reservePort.indexOf("OK") == -1)
			throw new OmsException("Cannot reserve a local port: " + reservePort);
		
		String[] splitReserve = reservePort.split(regexSpace);		
		for(String s : splitReserve){
			
			if(s.startsWith("local.aport")){
				array = s.split(regexEqual);
				port = array[array.length - 1];
			}
		}
		
		logger.info("rvi media port: "+ port);		
		String respDial = connOMS.getReponse("t"+callNum+" dial sig=to=sip:"+number+"@"+ipAddress+" "
				+ "\"media=ip="+ getOmsIpAddress()+" aport="+port+"\"");
		if(respDial.indexOf("OK") == -1)
			throw new OmsException("Cannot dial: " + respDial);
				
		String respMedia = connOMS.getReponse("wait evt=t"+callNum+".media,t"+callNum+".hangup");
		
		//Afficher la valeur du codec
		logger.info(respMedia);
		
		if(respMedia.indexOf("OK") == -1)
			throw new OmsException("issues with wait command: " + respMedia);
				
		String[] splitRespMedia = respMedia.split(regexMult);
		for (String s : splitRespMedia) {

			if (s.startsWith("content")) {
				array = s.split(regexEqual);
				clientIpAddress = array[array.length - 1];

			} else if (s.startsWith("aport")) {
				array = s.split(regexEqual);
				clientPort = array[array.length - 1];

			} else if (s.startsWith("acodecparam")) {
				array = s.split(regexEqual);
				acodecParam = array[array.length - 1];

			} else if (s.startsWith("acodec")) {
				array = s.split(regexEqual);
				clientCodec = array[array.length - 1];

			}else if(s.startsWith("cause")){
				logger.info("The call was ignored");
				deleteRvi();
				System.exit(0);
			}
		}
			
		String respWaitEvent = connOMS.getReponse("wait evt=t"+callNum+"*,mt"+callNum+".*");
		if(respWaitEvent.indexOf("OK") == -1)
			throw new OmsException("connected evt failed: " + respWaitEvent);
		
		trombone("mt1", "mt"+callNum);
		isTrombone = true;
		//answer();
		// t dial sig=to=sip:96648953@10.184.50.179 "media=ip=10.184.48.159 aport=6010"
	} 
	
	public boolean isTrombone(){
		
			return isTrombone;
	}
	
	/**
	 * To play an a8k extension audio file
	 * @param filePath path of the file
	 * @param interrupt true if the audio file can be interrupt, false otherwise
	 * @throws OmsException
	 */
	public void play(String filePath, boolean interrupt) throws OmsException {

		if (!isRviSyntExist) {
			String resp1 = this.connOMS.getReponse("new s1 synt");

			if(!resp1.equals("OK"))
				throw new OmsException("Error: Cannot create rvi synt " + resp1);
			//String setParam = connOMS.getReponse("mt1 setparam bindto=s1");
			
			String setParam = connOMS.getReponse("s1 setparam bindto=mt1");
			if(setParam.indexOf("OK") == -1)
				throw new OmsException("Error with setparam command: " + setParam);
			
			isRviSyntExist = true;
		}

		String respSh = connOMS.getReponse("mt1 shutup");
		if (!respSh.equals("OK"))
			throw new OmsException("Cannot shutup mt1");

		String respPlay = connOMS.getReponse("s1 play file=" + filePath); // loop=1
		if (!respPlay.equals("OK"))
			throw new OmsException("Cannot execute cmd play file " + respPlay);

		if (!interrupt) {
			String resp = connOMS.getReponse("wait evt=mt1.starving");
			if (!resp.startsWith("OK")) {
				System.out.println("cmd wait evt=mt1.starving failed: " + resp);
			}
		}
	}
	
	/**
	 * To return the key the user pressed on a softphone (X-LITE)
	 * @return the key pressed by the user or null if hangup was pressed
	 * @throws OmsException either "cannot create a dtmf rvi" or "cannot start a dtmf"
	 */
	public String dtmf() throws OmsException {

		String digit = null;

		if (!isRviDtmfExist) {
			
			String newDtmf = connOMS.getReponse("new d dtmf");
			if(!newDtmf.equals("OK"))
				throw new OmsException("cannot create a dtmf rvi: "+ newDtmf);
			
			//String setParam = connOMS.getReponse("mt1 setparam bindto=d");
			String setParam = connOMS.getReponse("d setparam bind=mt1");
			if(setParam.indexOf("OK") == -1)
				throw new OmsException("Error with command setparam: " + setParam);
			
			String dStart = connOMS.getReponse("d start");
			if(!dStart.equals("OK"))
				throw new OmsException("cannot start a dtmf: "+ newDtmf);
			
			isRviDtmfExist = true;
		}

		// do {
		String respWait = connOMS.getReponse("wait evt=d.dtmf,t1.hangup");	
		if(respWait.indexOf("OK") == -1)
			throw new OmsException("dtmf evt failed: " + respWait);
		
		String[] splitRespWait = respWait.split(regexSpace);
		for (String s : splitRespWait) {

			if (s.startsWith("digit")) {
				array = s.split(regexEqual);
				digit = array[array.length - 1];
				
			}else if(s.startsWith("cause")){				
				respWait = connOMS.getReponse("d stop");
				if(respWait.indexOf("OK") == -1)
					throw new OmsException("cannot stop the dtmf rvi");
					
				return null;
			}
		}

		// } while (!digit.equals("#"));

		return digit;
	}

	/**
	 * To record a communication into a audio file with a8k extension
	 * @param filePath path of the file
	 * @throws OmsException
	 */
	public void enreg(String filePath) throws OmsException {

		if (!isRviEnregExist) {
			String newEnreg = connOMS.getReponse("new recorder enreg");
			if(!newEnreg.equals("OK"))
				throw new OmsException("cannot create an enreg rvi: " + newEnreg);
			
			//connOMS.getReponse("mt1 setparam bindto=recorder");
			String setParam = connOMS.getReponse("recorder setparam bind=mt1");
			if(setParam.indexOf("OK") == -1)
				throw new OmsException("Error with command setparam: " + setParam);
			isRviEnregExist = true;
		}

		String startEnreg = connOMS.getReponse("recorder start " + filePath);
		//String startEnreg = connOMS.getReponse("recorder start /opt/application/64poms/current/tmp"
				//+ "/recordingSip.a8k");
		if(startEnreg.indexOf("OK") == -1)
			throw new OmsException("cannot start recording: " + startEnreg);
	}

	/**
	 * To stop recording a communication
	 * @throws OmsException cannot stop recording. Rvi enreg not created
	 */
	public void stopEnreg() throws OmsException {

		if (isRviEnregExist){
			
			String stopRecord = connOMS.getReponse("recorder stop");
			if(stopRecord.indexOf("OK") == -1)
				throw new OmsException("cannot stop recording: " + stopRecord);
		}else
			throw new OmsException("cannot stop recording. You should start recording first");
	}
	
	/**
	 * To get the status of a rvi
	 * @param rviName rvi name
	 * @return the rvi status
	 * @throws OmsException Cannot get the rvi status: + error message
	 */
	public String status(String rviName) throws OmsException{
		
		String resp = connOMS.getReponse(rviName + " status");
		if(resp.indexOf("OK") == -1)
			throw new OmsException("Cannot get the rvi status: " + resp);
		
		return resp;
	}
	
	/**
	 * To start recording a video
	 * @param filePath path of the file to save the recording
	 * @throws OmsException
	 */
	public void videoRecording(String filePath) throws OmsException{
		
		String repRecord = connOMS.getReponse("mt1 startrecord file=" + filePath);
		if(repRecord.indexOf("OK") == -1)
			throw new OmsException("Cannot start recording the video: " + repRecord);
	}
	
	/**
	 * To stop recording a video
	 * @throws OmsException
	 */
	public void stopVideoRecording() throws OmsException{
		
		String repStop = connOMS.getReponse("mt1 stoprecord");
		if(repStop.indexOf("OK") == -1)
			throw new OmsException("Cannot stop recording a video: " + repStop);
	}
	
	/**
	 * To start playing a video file
	 * @param filePath path of the file
	 * @throws OmsException
	 */
	public void playVideo(String filePath) throws OmsException{
		
		String repPlay = connOMS.getReponse("mt1 startplay file=" + filePath);
		if(repPlay.indexOf("OK") == -1)
			throw new OmsException("Cannot start playing the video file: " + repPlay);
	}
	
	/**
	 * To stop playing a video file
	 * @throws OmsException
	 */
	public void stopPlayVideo() throws OmsException{
		
		String repStop = connOMS.getReponse("mt1 stopplay");
		if(repStop.indexOf("OK") == -1)
			throw new OmsException("Cannot stop playing a video file: " + repStop);
	}
	
	protected void setCallNumber(int callNumber){		
		this.callNumber = callNumber;
	}
	
	protected int getCallNumber(){	
		return this.callNumber;
	}
	
	/**
	 * To get the caller ID (SIP number)
	 * @return caller's ID
	 */
	public String getCaller(){
		return callDescription.getCaller();
	}
	
	/**
	 * To get the callee ID (SIP number)
	 * @return callee's ID
	 */
	public String getCallee(){
		return callDescription.getCallee();
	}
	
	/**
	 * To get the call unique identifier
	 * @return call's unique identifier
	 */
	public String getCallId(){
		return callDescription.getCallId();
	}
	
	/**
	 * To get the codec used for the call
	 * @return codec's name
	 */
	public String getCodec(){
		return callDescription.getCodec();
	}
	
	/**
	 * To get the user caller IP address
	 * @return Client's IP address
	 */
	public String getClientIpAddress(){		
		return clientIpAddress;
	}

	/**
	 * To get OMS IP address
	 * @return OMS IP address
	 */
	protected String getOmsIpAddress() {
		return omsIpAddress;
	}
	
	protected VipConnexion getVipConnexion(){
		return connOMS;
	}
	
	protected void setPartNumberConf(int num){	
		partNumberConf = num;
	}
	
	/**
	 * To get the user unique identifier in the conference, 
	 * @return user unique identifier, which is a positive value
	 */
	public int getPartNumberConf(){	
		return partNumberConf;
	}
	
	public void sleep(int milliseconds) {

		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void setConfName(String confName2) {
		// TODO Auto-generated method stub
		confName = confName2;
	}

	/**
	 * To get the conference name if the user has joined a conference
	 * @return name of the conference in case the user has joined a conference
	 */
	public String getConfname() {
		// TODO Auto-generated method stub
		return confName;
	}
	
	protected String[] getHostPortVip(){
		return hosPortVip;
	}

}
