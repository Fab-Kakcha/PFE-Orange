package com.orange.olps.api.sip;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class OmsCallSip extends Thread {

	private static Logger logger = Logger.getLogger(OmsCallSip.class);
	private VipConnexion connOMS = null;
	private boolean isRviSyntExist = false;
	private boolean isRviEnregExist = false;
	private boolean isRviDtmfExist = false;
	private String regexSpace = "\\s";
	private String regexEqual = "=";
	private String regexMult = "[\\s\";]";
	
	//private static Pattern pat = Pattern.compile("local=([^\\s/>]+)");

	private String clientIpAddress;
	private String clientPort;
	private String clientCodec;
	private String acodecParam;
	private String omsIpAddress;
	private String rviMediaPort;
	private int partNumberConf = 0;

	private String[] array;
	private CallDescription callDescription;
	private OmsConference omsConference;
	
	private int callNumber;
	private String[] hosPortVip;
	private String confName = "conf";
	
	//problème avec l'enregistrement et la lecture vidéo
	

	/**
	 * To get OMS's IP address and port
	 * @return IP address and port of OMS
	 */
	public String[] getHostPortVip(){
		return hosPortVip;
	}
	
	public OmsCallSip() {

		super();
		callNumber =1;	
	}

	/**
	 * To connect to OMS at IP address hostVip and port portVip
	 * @param hostVip OMS's IP address
	 * @param portVip OMS's port 
	 * @throws OmsException
	 */
	public void connect(String hostVip, String portVip) throws OmsException {
		
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

	/**
	 * 
	 * @param hostVip
	 * @param portVip
	 * @param portVipConf
	 * @throws OmsException
	 */
	public void connect(String hostVip, String portVip, String portVipConf) throws OmsException {
		
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
	
	/**
	 * 
	 * @param hostVipConf
	 * @param portVipConf
	 * @throws OmsException
	 * @throws IOException
	 */
	public void initConference(String hostVipConf, String portVipConf) throws OmsException, IOException{
		
		omsConference = new OmsConference(hostVipConf, portVipConf);
	}
	
	public void listenConf() throws OmsException{
		
		connOMS.getReponse("t1 listen sip:conf=*@");
				
		String respWait = connOMS.getReponse("wait evt=t1.*");
		logger.info("respWait: " + respWait);
				
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

		callDescription.toString();
		String callee = callDescription.getCallee();
		array = callee.split("conf=");
		confName = array[array.length - 1].split("@")[0];
		logger.info("confName2: " + confName);
		
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
		connOMS.getReponse("t1 answer \"media=ip=" + getOmsIpAddress()
				+ " aport=" + rviMediaPort + " acodec=" + clientCodec + "\"");
	
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
				
		logger.info("Connected");
		if(omsConference.status(confName))			
			omsConference.add(this, confName);
		else 
			omsConference.create(this, confName);		
		
		logger.info("Conference sucessfully created");
		respWaitEvent = connOMS.getReponse("wait evt=t1*,mt1.*");
		logger.info(respWaitEvent);
	}
	
	/**
	 * To listen to incoming calls and establish a media session
	 * @throws OmsException
	 */
	// il faut chaque OmsCall puisse écouter sur des numéros différents
	public void listen(String c) throws OmsException { // Call c
			
			connOMS.getReponse("t1 listen sip:"+ c +"*@");
			String respWait = connOMS.getReponse("wait evt=t1.*");
			//logger.info(respWait);

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
			connOMS.getReponse("t1 answer \"media=ip=" + getOmsIpAddress()
					+ " aport=" + rviMediaPort + " acodec=" + clientCodec + "\"");
		
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
	}

	/**
	 * To forward a call once a media session is set up
	 * @param isBlind says is the forwarding is in blind or in consultation
	 * @param ipDestOrRvi IP adress or rvi name of the destination
	 * @throws OmsException
	 */
	public void callForwarding(boolean isBlind, String ipDestOrRvi) throws OmsException{
		
		String status ="";
		if(isBlind){
			
			connOMS.getReponse("t1 redirect blind sip:" + ipDestOrRvi);			
			String forwardStatus =  connOMS.getReponse("wait evt=t1.redirected");
			
			String[] split = forwardStatus.split(regexSpace);
			for(String s : split){
				
				if(s.startsWith("status")){
					array = s.split(regexEqual);
					status = array[array.length - 1];
				}
			}
			
		}else{			
			connOMS.getReponse("t1 redirect consultation " + ipDestOrRvi);
			String forwardStatus =  connOMS.getReponse("wait evt=t1.redirected");
			
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
			logger.info(notify);
			connOMS.getReponse("wait evt=t.hangup");
		}else if(status.equals("FAILED"))
			logger.error("Cannot redirect call");
		
	}
	
	/**
	 * To join two media RVIs 
	 * @param rviM1 media RVI
	 * @param rviM2 media RVI
	 * @throws OmsException
	 */
	public void trombone(String rviM1, String rviM2) throws OmsException{
		
		connOMS.getReponse(rviM1 + " extend trombone " + rviM2);
	}
	
	/**
	 * To unjoin two media RVIs
	 * @param rviM1 media RVi
	 * @param rviM2 media RVI
	 * @throws OmsException
	 */
	public void unTrombone(String rviM1, String rviM2) throws OmsException{
		
		connOMS.getReponse(rviM1 + " extend untrombone " + rviM2);
	}
	
	/**
	 * To delete RVIs 
	 * @throws OmsException
	 */
	public void deleteRvi() throws OmsException{
		
		//int num = getCallNumber();
		
		int num = 1;
		for(int i =1 ; i <= num ; i++){
		
			String delT1 = connOMS.getReponse("delete t"+i);
			if(!delT1.equals("OK"))
				throw new OmsException("cannot delete rvi sip t1: " + delT1);
			
			connOMS.getReponse("delete mt1");			
		}
				
		/*String delT1 = connOMS.getReponse("delete t1");
		if(!delT1.equals("OK"))
			throw new OmsException("cannot delete rvi sip t1: " + delT1);*/

		if(isRviEnregExist)
			connOMS.getReponse("delete recorder");
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
		
		try {
			connOMS.getSocket().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new OmsException("Error: cannot close the connection to OMS");
		}		
	}
	
	/**
	 * To hangup a phone call 
	 * @throws OmsException
	 */
	public void hangUp() throws OmsException {

		connOMS.getReponse("t1 hangup");
		connOMS.getReponse("wait evt=t1.hangup");
		deleteRvi();
	}

	/**
	 * To synthesize a speech
	 * @param say the message to synthesize 
	 * @param interrupt true if the message can be interrupt by something else and false otherwise
	 * @throws OmsException
	 */
	public void say(String say, boolean interrupt) throws OmsException {

		if (!isRviSyntExist) {
			String resp1 = connOMS.getReponse("new s1 synt");

			if (!resp1.equals("OK"))
				throw new OmsException("Error: Cannot create rvi synt " + resp1);
			String setParam = connOMS.getReponse("s1 setparam bindto=mt1");//
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
	 * To call someone with sip address sip:number@ipAddress, and trombone the two calls
	 * @param number field number of the sip address
	 * @param ipAddress field ipAddress of the sip address
	 * @throws OmsException
	 */
	public void call(String number, String ipAddress) throws OmsException {

		int callNum = getCallNumber();
		callNum += 1;
		//setCallNumber(callNum);
		
		String port = "";
		connOMS.getReponse("new t"+ callNum +" sip");
		connOMS.getReponse("new mt"+callNum+" media");
		String reservePort = connOMS.getReponse("mt"+callNum+" reserveport type=audio");
		//String reservePort = connOMS.getReponse("mt"+callNum+" reserveport type=video");
		String[] splitReserve = reservePort.split(regexSpace);
		
		for(String s : splitReserve){
			
			if(s.startsWith("local.aport")){
				array = s.split(regexEqual);
				port = array[array.length - 1];
			}
		}
		
		String respDial = connOMS.getReponse("t"+callNum+" dial sig=to=sip:"+number+"@"+ipAddress+" "
				+ "\"media=ip="+ getOmsIpAddress()+" aport="+port+"\"");
		
		logger.info(respDial);
		
		String respMedia = connOMS.getReponse("wait evt=t"+callNum+".media,t"+callNum+".hangup");
		logger.info(respMedia);
		
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
		trombone("mt1", "mt"+callNum);
		//answer();
		// t dial sig=to=sip:96648953@10.184.50.179 "media=ip=10.184.48.159 aport=6010"
	} 
	
	// /var/opt/data/flat/64poms/files/logs/20150210/recording.a8k
	// /opt/application/64poms/current/tmp/recordingSip.a8k

	/**
	 * To play an a8k extension audio file
	 * @param filePath file's path
	 * @param interrupt true if the audio file can be interrupt, false otherwise
	 * @throws OmsException
	 */
	public void play(String filePath, boolean interrupt) throws OmsException {

		if (!isRviSyntExist) {
			String resp1 = this.connOMS.getReponse("new s1 synt");

			if (resp1.equals("OK"))
				throw new OmsException("Error: Cannot create rvi synt " + resp1);
			//String setParam = connOMS.getReponse("mt1 setparam bindto=s1");
			String setParam = connOMS.getReponse("s1 setparam bindto=mt1");
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
	 * To return the pressed key by users on a softphone (X-LITE)
	 * @return the pressed key or null if users pressed hangup
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
			String dStart = connOMS.getReponse("d start");
			if(!dStart.equals("OK"))
				throw new OmsException("cannot start a dtmf: "+ newDtmf);
			
			isRviDtmfExist = true;
		}

		// do {
		String respWait = connOMS.getReponse("wait evt=d.dtmf,t1.hangup");	
		logger.info(respWait);

		String[] splitRespWait = respWait.split(regexSpace);
		for (String s : splitRespWait) {

			if (s.startsWith("digit")) {
				array = s.split(regexEqual);
				digit = array[array.length - 1];
				
			}else if(s.startsWith("cause")){				
				connOMS.getReponse("d stop");
				deleteRvi();
				return null;
			}
		}

		// } while (!digit.equals("#"));

		return digit;
	}

	/**
	 * To record a communication into a a8k extension audio file
	 * @param filePath file's path
	 * @throws OmsException either "cannot create an enreg rvi" or "cannot start recording"
	 */
	public void enreg(String filePath) throws OmsException {

		if (!isRviEnregExist) {
			String newEnreg = connOMS.getReponse("new recorder enreg");
			if(!newEnreg.equals("OK"))
				throw new OmsException("cannot create an enreg rvi " + newEnreg);
			
			//connOMS.getReponse("mt1 setparam bindto=recorder");
			connOMS.getReponse("recorder setparam bind=mt1");

			isRviEnregExist = true;
		}

		String startEnreg = connOMS.getReponse("recorder start " + filePath);
		//String startEnreg = connOMS.getReponse("recorder start /opt/application/64poms/current/tmp"
				//+ "/recordingSip.a8k");
		if(!startEnreg.equals("OK"))
			throw new OmsException("cannot start recording: " + startEnreg);
	}

	/**
	 * To stop recording a communication.
	 * @throws OmsException cannot stop recording. Rvi enreg not created
	 */
	public void stopEnreg() throws OmsException {

		if (isRviEnregExist){
			
			String stopRecord = connOMS.getReponse("recorder stop");
			if(!stopRecord.equals("OK"))
				throw new OmsException("cannot stop recording: " + stopRecord);
		}else
			throw new OmsException("cannot stop recording. Rvi enreg not created");
	}
	
	public String status(String rviName) throws OmsException{
		
		String resp = connOMS.getReponse(rviName + " status");
		return resp;
	}
	
	public void videoRecording(String filePath) throws OmsException{
		
		connOMS.getReponse("mt1 startrecord file=" + filePath);		
	}
	
	public void stopVideoRecording() throws OmsException{
		
		connOMS.getReponse("mt1 stoprecord ");
	}
	
	public void playVideo(String filePath) throws OmsException{
		
		connOMS.getReponse("mt1 startplay file=" + filePath);
	}
	
	public void stopPlayVideo() throws OmsException{		
		connOMS.getReponse("mt1 stopplay");
	}
	
	public void setCallNumber(int callNumber){		
		this.callNumber = callNumber;
	}
	
	public int getCallNumber(){	
		return this.callNumber;
	}
	
	/**
	 * To get the caller's ID (SIP number)
	 * @return caller's ID
	 */
	public String getCaller(){
		return callDescription.getCaller();
	}
	
	/**
	 * To get the callee's ID (SIP number)
	 * @return callee's ID
	 */
	public String getCallee(){
		return callDescription.getCallee();
	}
	
	/**
	 * To get the call's unique identifier
	 * @return call's unique identifier
	 */
	public String getCallId(){
		return callDescription.getCallId();
	}
	
	/**
	 * To get the codec used
	 * @return codec's name
	 */
	public String getCodec(){
		return callDescription.getCodec();
	}
	
	/**
	 * To get the client or caller IP address
	 * @return Client's IP address
	 */
	public String getClientIpAddress(){		
		return clientIpAddress;
	}

	/**
	 * To get OMS's IP address
	 * @return OMS's IP address
	 */
	public String getOmsIpAddress() {
		return omsIpAddress;
	}
	
	public VipConnexion getVipConnexion(){
		return connOMS;
	}
	
	/**
	 * To set a number for the client in the conference
	 * @param num number for the client
	 */
	public void setPartNumberConf(int num){	
		partNumberConf = num;
	}
	
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

}
