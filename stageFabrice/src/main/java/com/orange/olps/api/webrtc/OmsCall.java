/**
 * This Java's Class defines how to connect to OMS in order to play file, to record and many others things
 */


package com.orange.olps.api.webrtc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;

/*
 * Nouvelle version : Le client est maintenant un thread, ce qui permet
 * de lancer plusieurs appels en meme temps et donc plusieurs clients en
 * parallele. Chaque client a une connexion particuliere a OMS
 */

/**
 * @author JWPN9644
 * 
 */

public class OmsCall extends Thread {

	

	private static Logger logger = Logger.getLogger(OmsCall.class);

	private VipConnexion connOMS = null;
	//private List<OmsCall> listOmsCall = new ArrayList<OmsCall>();
	private boolean isRviSyntExist = false;
	private boolean isRviEnregExist = false;
	private boolean isRviRecogExist = false;
	private boolean isRviWebrtcExist = false;
	private boolean isCaller = false;
	//private boolean isCallee = false;
	//private int nbOfClientConnected = 1;
	
	private WebSocket conn = null;
	private String ipAddress = null;
	private int partNumberConf = 0;
	private String confName = null;
	private String userName = null;
	private String exConfName = null;
	private boolean hasClientPressDisc = false;
	private boolean hasCreatedConf = false;
	
	private String filePath = "C:\\opt\\infosOnConferences.log";
	//private List<WebSocket> listOfPeopleCalled = new ArrayList<WebSocket>();
	
	private ConferenceParameters conferenceParam;
	
	
	
	private String[] hosPortVip;
	
	
	private void setExConfName(String exConfName){
		this.exConfName = exConfName;
	}
	
	private String getExConfName(){
		return this.exConfName;
	}
	/**
	 * 
	 */
	public OmsCall() {
		super();
	}

	/**
	 * To get the Browser or client websocket and IP address
	 * @param conn client websocket
	 * @param ipAddress client IP address
	 */
	public OmsCall(WebSocket conn, String ipAddress) {
		 
		this.conn = conn;
		this.ipAddress = ipAddress;
		hasClientPressDisc = false;
		this.start();
	}
	
	/**
	 * To connect to OMS
	 * @param hostVip OMS's IP address 
	 * @param portVip OMS's listening port
	 * @throws OmsException
	 * @throws IOException 
	 */
	public void connect(String hostVip, String portVip) throws OmsException, IOException{
		
		hosPortVip = new String[2];
		hosPortVip[0] = hostVip;
		hosPortVip[1] = portVip;
		
		if(!isRviWebrtcExist){
			this.connOMS = new VipConnexion(hostVip, portVip);
			//String respInfo = this.connOMS.getReponse("info ocam");
			String respWebrtcCreation = this.connOMS.getReponse("new mt1 webrtc");
			
			if (respWebrtcCreation.equals("OK")) {
				this.connOMS.getReponse("mt1 setparam escape_sdp_newline=true");
			} else 
				throw new OmsException("Error: Cannot create rvi webrtc "+ respWebrtcCreation);	
			
			isRviWebrtcExist = true;
		}		
	}
	
	/**
	 * 
	 * @param sdpOfferReceived
	 *            is the sdp received from OMS. Its contains unexpected characters "\\"
	 * @return OMS's sdp is return without unexpected character
	 * @throws IOException
	 */
	private String toSdpOms(String sdpOfferReceived) {

		StringTokenizer str = new StringTokenizer(sdpOfferReceived,"\"");
		str.nextToken();
		String sdpOms = str.nextToken().replaceAll("\\\\n","\\n").replaceAll("\\\\r","\\r");

		return sdpOms;
	}

	/**
	 * 
	 * @param sdpOms is OMS sdp
	 * @param type is either "offer" or "answer"
	 * @return OMS's sdp is adapted to a form that a Browser can recognize
	 */
	private String toSdpBrowser(String sdpOms, String type) {

		String sdpToReturn = "{\"sdp\":{\"type\":\"" + type + "\",\"sdp\":\"" + sdpOms.substring(12) + "\"}}";

		return sdpToReturn;
	}
	
	private Sdp sdpAnswer(String rviWebrtc, String sdpNav) throws OmsException {

			Sdp sdp = new Sdp();

			String sdpRequest = this.getVipConnexion().getReponse(rviWebrtc +
					" generate type=answer \"content=" + sdpNav + "\"");
			if (sdpRequest.equals("OK")) {

				String respSdpAnswer = connOMS.getReponse("wait evt="+ rviWebrtc +".answered");
				if(!respSdpAnswer.startsWith("OK"))
					throw new OmsException("cmd wait evt answered failed ");
				
				logger.info(respSdpAnswer);
				String sdpOms = toSdpOms(respSdpAnswer);
				String sdpBrowser = toSdpBrowser(sdpOms, "answer");
				sdp.setSdp(sdpBrowser);
				sdp.setType("answer");
			} else
				throw new OmsException("cmd " + rviWebrtc + " generate type=answer failed "+ sdpRequest);

			return sdp;
	}
	
	/**
	 * To synthesize a message
	 * @param say the message to synthesize
	 * @param interrupt true if the message can be interrupted
	 * @throws OmsException
	 */
	
	public void say(String say, boolean interrupt) throws OmsException {

		if (!isRviSyntExist) {
			String resp1 = this.connOMS.getReponse("new s1 synt");
			
			if (resp1.equals("OK")) {
				/*String setParam = this.connOMS.getReponse("mt1 setparam bind=s1");
				if(!setParam.equals("OK"))
					throw new OmsException("cannot execute mt1 setparam bind=s1");*/
				isRviSyntExist = true;
				
			} else
				throw new OmsException("Error: Cannot create rvi synt " + resp1);
		}
		
		String respSh = connOMS.getReponse("mt1 shutup");
		if(!respSh.equals("OK"))
			throw new OmsException("Cannot shutup mt1");
		
		String setParam = this.connOMS.getReponse("mt1 setparam bind=s1");
		if(!setParam.equals("OK"))
			throw new OmsException("cannot execute mt1 setparam bind=s1");
		
		sleep(600);
		
		String respSay = connOMS.getReponse("s1 say \"" + say + "\"");
		if (!respSay.equals("OK"))
			throw new OmsException("Cannot send cmd say to OMS " + respSay);	
		
		if(!interrupt){			
			String resp = connOMS.getReponse("wait evt=mt1.starving");
			if(!resp.startsWith("OK")){
				System.out.println("cmd wait evt=mt1.starving failed: " + resp);
			}
		}

	}
	
	/**
	 * To play an audio file
	 * @param filePath the audio file to play
	 * @param interrupt true if the play file can be interrupted
	 * @throws OmsException
	 */
	
	public void play(String filePath, boolean interrupt) throws OmsException {

		if (!isRviSyntExist) {
			String resp1 = this.connOMS.getReponse("new s1 synt");
			
			if (resp1.equals("OK")) {
				//String setParam = this.connOMS.getReponse("mt1 setparam bind=s1");
				//if(!setParam.equals("OK"))
					//throw new OmsException("cannot execute mt1 setparam bind=s1");
				isRviSyntExist = true;
				
			} else
				throw new OmsException("Error: Cannot create rvi synt " + resp1);
		}
		
		String respSh = connOMS.getReponse("mt1 shutup");
		if(!respSh.equals("OK"))
			throw new OmsException("Cannot shutup mt1");
		
		String setParam = this.connOMS.getReponse("mt1 setparam bind=s1");
		if(!setParam.equals("OK"))
			throw new OmsException("cannot execute mt1 setparam bind=s1");
		
		String respPlay = connOMS.getReponse("s1 play file=" + filePath); //loop=1
		sleep(2000);
		if (!respPlay.equals("OK"))
			throw new OmsException("Cannot execute cmd play file " + respPlay);
		
		if(!interrupt){		
			String resp = connOMS.getReponse("wait evt=mt1.starving");
			if(!resp.startsWith("OK")){
				System.out.println("cmd wait evt=mt1.starving failed: " + resp);
			}
		}		
	}
	
	/**
	 * To record a voice into a file
	 * @param filePathEnreg the file's path 
	 * @throws OmsException
	 */
	public void record(String filePathEnreg) throws OmsException{
		
		if(!isRviEnregExist){
			String respEnreg = this.connOMS.getReponse("new e enreg");
			if(!respEnreg.equals("OK"))
				throw new OmsException("cannot create a rvi enreg " + respEnreg);
			isRviEnregExist = true;
		}
		
		String enreg = this.connOMS.getReponse("e start "+ filePathEnreg);//Start the recording into the file
		if(!enreg.equals("OK"))
			throw new OmsException("cannot create an rvi enreg " + enreg);
		
		//e stop to stop the recording
	}
	

	/**
	 * To stop the recording
	 * @throws OmsException
	 */
	public void stopRecord() throws OmsException{
		
		if(isRviEnregExist){
			String stopEnreg = this.connOMS.getReponse("e stop");
			if(!stopEnreg.equals("OK"))
				throw new OmsException("cannot stop the recording: e stop " +  stopEnreg);
		}
		else throw new OmsException("the recording rvi does not exist");
	}
	
	
	/**
	 * Voice recognition 
	 * @throws OmsException
	 */
	public void recognize() throws OmsException{
		
		if(!isRviRecogExist){			
			String respRecog = this.connOMS.getReponse("new r reco");
			if(!respRecog.equals("OK"))
				throw new OmsException("cannot create a rvi reco " + respRecog);
			isRviRecogExist = true;
		}
		
		String reco = this.connOMS.getReponse("r setparam model=villes.gkz heu=30 abp.silDeb=10 "
				+ "abp.silFin=25");
		if(!reco.equals("OK"))
			throw new OmsException("error with the command r setparam model=villes.gkz");
		
		String recoStart = this.connOMS.getReponse("r start");
		if(!recoStart.equals("OK"))
			throw new OmsException("cannot start the voice recognition r start "+ recoStart);
	}
	
	/**
	 * To get the status of the webrtc rvi
	 * @return the rvi webrtc status
	 * @throws OmsException
	 */
	
	public String rviStatus() throws OmsException {

		String rviStatus = connOMS.getReponse("mt1 status");

		if (rviStatus.startsWith("ERROR")) {
			logger.error("cannot get the rvi status " + rviStatus);
			throw new OmsException("cannot get the status of the webrtc rvi "+ rviStatus);
		}

		return rviStatus;
	}

	/**
	 * Deleting all rvi resources created. When a call was made, its deletes for the callee's rvi as well
	 * @throws OmsException
	 */
	
	private void delResources() throws OmsException{
			
		//int i;
		//int num = this.getNbOfClientConnected();
		int num =1;
		
		/*if(this.getIsCaller()){
			
			OmsCall call = null;
			Iterator<OmsCall> iter = this.listOmsCall.iterator();
			
			while(iter.hasNext()){			
				call = iter.next();
				call.getWebSocket().send("logout");
			}
			
		  for(i=1 ; i <= num ; i++){
			
			String delRviWebrtc = this.connOMS.getReponse("delete mt" + i);
			if (delRviWebrtc.equals("OK"))
				this.connOMS.getReponse("wait evt=mt"+ i +".mediadisconnected");
			else
				throw new OmsException("Error cannot delete webrtc rvi mt"+ i +": "+ delRviWebrtc);
			
			String delRviSyn = this.connOMS.getReponse("delete s" + i);
			if(!delRviSyn.equals("OK"))
				throw new OmsException("Error cannot delete synt rvi s " + delRviSyn);
		  }
		
			if(isRviEnregExist){
			
				String delRviEnreg = this.connOMS.getReponse("delete e");
				if(!delRviEnreg.equals("OK"))
					throw new OmsException(" Error cannot delete enreg rvi " + delRviEnreg);			
			}
		
		}else if(this.getIsCallee()){*/
			
			if(isRviWebrtcExist){
				String delRviWebrtc = this.getVipConnexion().getReponse("delete mt" + num);
				if (delRviWebrtc.equals("OK"))
					this.getVipConnexion().getReponse("wait evt=mt"+ num +".mediadisconnected");
				else
					throw new OmsException("Error cannot delete webrtc rvi mt"+ num +": "+ delRviWebrtc);			
			}
					
			if(isRviSyntExist){
				String delRviSyn = this.getVipConnexion().getReponse("delete s" + num);
				if(!delRviSyn.equals("OK"))
					throw new OmsException("Error cannot delete synt rvi s " + delRviSyn);				
			}
			if(isRviEnregExist){
				String delRviEnreg = this.connOMS.getReponse("delete e");
				if(!delRviEnreg.equals("OK"))
					throw new OmsException(" Error cannot delete enreg rvi " + delRviEnreg);
			}			
		//}
	}
		
	/**
	 * To sleep the thread
	 * @param milliseconds time in milliseconds
	 *  
	 */
	public void sleep(int milliseconds) {

		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * To call someone
	 * @param omsCall the callee's identifiers
	 * @throws OmsException
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	
	public void call(OmsCall callee, OmsConference conf) throws OmsException, IOException, InterruptedException {

		//int num = this.getNbOfClientConnected();
		//num += 1;
		//this.setNbOfClientConnected(num);
		
		if(callee == null)
			throw new IllegalArgumentException("Argument cannot be null");
		
		WebSocket calleeWs = callee.getWebSocket();
		//omsCall.setVipConnexion(this.connOMS);
		//omsCall.setNbOfClientConnected(this.getNbOfClientConnected());
		//omsCall.setIsCallee(true);
		//this.listOmsCall.add(omsCall);

		String userName = this.getUserName();	
		calleeWs.send("incomingCall:"+userName);		
		
		this.play("/opt/application/64poms/current/tmp/Ringback_Tone.a8k", true);
		//callee.play("/opt/application/64poms/current/tmp/Beatles-Hey_Jude.a8k", true);
		
		if(conf.isClientJoined(callee)){			
			conf.myPlay(callee.getConfname(), "/opt/application/64poms/current/tmp/Beatles-Hey_Jude.a8k");
		}
		else
			callee.play("/opt/application/64poms/current/tmp/Beatles-Hey_Jude.a8k", true);		
		
	}
	

	/**
	 * 
	 * @param caller
	 * @param conf OmsConference
	 * @param bool true answer the call and get out of the conference we are, and false bring the caller
	 * into the conference we are. 
	 * @throws OmsException
	 * @throws IOException 
	 */
	public void answer(OmsCall caller, OmsConference conf, boolean bool) throws OmsException, IOException{	
		
		//Mettre un troisième paramètre qui permet en cas de réponse à appel, de savoir si l'appelé veux 
		//faire rentrer l'appelant dans sa conference
				
		if(caller == null)
			throw new IllegalArgumentException("Argument OmsCall cannot be null");
		
		if(conf == null)
			throw new IllegalArgumentException("Argument OmsConference cannot be null");
		
		WebSocket callerWs = caller.getWebSocket();
		callerWs.send("answer:" + this.getUserName());
		
		caller.setIsCaller(true);
		//set le parametre caller pour caller
		
		Random ran = new Random();
		int randomNb = ran.nextInt();
		
		OmsCall callee = this;
							
		if(conf.isClientJoined(callee)){
			
			//Accepter l'appel de Claude en le faisant rentrer dans la conférence
			// ou bien sortie de la conférence, aller discuter puis revenir dans la conférence			
			//faire entrer Claude das la conférence où il se trouve
			//conf.add(caller, conf.getConfName());
			
			if(bool){//vrai, alors j'accepte de sortir de ma conf
								
				conferenceParam = new ConferenceParameters(Integer.toString(randomNb));	
				conferenceParam.setEntertone("false");
				conferenceParam.setExittone("false");
								
				if(conf.isClientJoined(caller)){
					
					caller.setExConfName(caller.getConfname());
					conf.delete(caller);			
				}
				
				conf.create(caller, conferenceParam);
				callee.setExConfName(this.getConfname()); //Methode setExConfName private
				conf.delete(callee);// Attention détruira la conférence si c'est lui qui l'a crée
				callee.setHasCreatedConf(false);
				conf.add(callee, conferenceParam);
			}
			else //Je refuse de sortir de ma conf, et j'invite l'appelant à la rejoindre				
				conf.add(caller, callee.getConfname());		
			
		}else{
			
			if(conf.isClientJoined(caller)){
				
				caller.setExConfName(caller.getConfname());
				conf.delete(caller);			
			}
			
			conferenceParam = new ConferenceParameters(Integer.toString(randomNb));	
			conferenceParam.setEntertone("false");
			conferenceParam.setExittone("false");
			
			conf.create(caller, conferenceParam);
			conf.add(callee, conferenceParam);
		}		
		
		conf.showParticipant(caller.getConfname(), true);
		conf.participantsStatus(caller);
		conf.participantsStatus(callee);
		
		conf.infos(filePath);
	}
	
	
	public void answerAndLeave(OmsCall caller, OmsConference conf, Annuaire annuaire) throws OmsException, 
	IOException{
		
		if(caller == null)
			throw new IllegalArgumentException("Argument OmsCall cannot be null");
		if(conf == null)
			throw new IllegalArgumentException("Argument Annuaire cannot be null");
		else if(annuaire == null)
			throw new IllegalArgumentException("Argument OmsConference cannot be null");
				
		
		List<OmsCall> listOmsCallInConf = new ArrayList<OmsCall>();
		HashMap<OmsCall, String> omsListOms = annuaire.getAnnuaire();
		Set<OmsCall> listOms = omsListOms.keySet();
		
		WebSocket callerWs = caller.getWebSocket();
		callerWs.send("answer:" + this.getUserName());
		caller.setIsCaller(true);
		
		Random ran = new Random();
		int randomNb = ran.nextInt();
		
		OmsCall callee = this;
		
		if(conf.isClientJoined(callee)){
			
			caller.getVipConnexion().getReponse("mt1 shutup");
			conf.stopPlay();
			
			conferenceParam = new ConferenceParameters(Integer.toString(randomNb));	
			conferenceParam.setEntertone("false");
			conferenceParam.setExittone("false");
			
			if(conf.isClientJoined(caller)){ //Au cas où celui qui m'appelle est aussi dans une conférence
				// On le fait sortir de sa conférence
				caller.setExConfName(caller.getConfname());
				conf.delete(caller);			
			}
			
			conferenceParam.setName(caller.getUserName());
			conf.create(caller, conferenceParam);
			callee.setExConfName(this.getConfname()); //Methode setExConfName private
			
			//if(callee.getHasCreatedConf()){
				
				//Si l'appelé a crée la conférence, la conférece sera détruire avec tous ses participants
				//Il récuperer la liste des participants et mettre à jours leur status
				
				listOmsCallInConf = conf.getListOmsCallInConf(callee.getConfname());											
						
				Iterator<OmsCall> ite;
				Iterator<OmsCall> ite2 = listOmsCallInConf.iterator();
				OmsCall c, c2;
				String userName;				
				
				while (ite2.hasNext()) {

					c2 = ite2.next();
					userName = c2.getUserName();
					c2.getWebSocket().send("hangup:" + callee.getUserName());

					callee.getWebSocket().send("hangup:" + c2.getUserName());
					
					ite = listOms.iterator();
					while (ite.hasNext()) {

						c = ite.next();
						if (!c.equals(c2)){
							
							c.getWebSocket().send("showUserNameConnectedToOMS:" + userName
									+ ":hangup");
						}														
					}
				}				
			//}
			
			conf.delete(callee);// Attention détruira la conférence si c'est lui qui l'a crée
			//callee.setHasCreatedConf(false);
			conferenceParam.setName(callee.getUserName());
			conf.add(callee, conferenceParam);		
			
			conf.showParticipant(caller.getConfname(),true);
			//conf.participantsStatus(caller);
			//conf.participantsStatus(callee);
			
			//conf.infos(filePath);
			//logger.info(filePath + " created");
			
			Iterator<OmsCall> ite3 = listOms.iterator();
			//OmsCall c;
			
			while(ite3.hasNext()){
				
				c = ite3.next();
				conf.participantsStatus(c);
			}
		}else
			throw new OmsException("Cannot leave the conference, because you are not in a conference");
	}
	
	//Answser par défaut, si le callee n'est pas une conférence, alors une nouvelle conférence
	//sera crée
	public void answerAndStay(OmsCall caller, OmsConference conf, Annuaire annuaire) throws OmsException, 
	IOException{
		
		if(caller == null)
			throw new IllegalArgumentException("Argument OmsCall cannot be null");
		if(conf == null)
			throw new IllegalArgumentException("Argument Annuaire cannot be null");
		else if(annuaire == null)
			throw new IllegalArgumentException("Argument OmsConference cannot be null");
		
		//this.getVipConnexion().getReponse("mt1 shutup");
		//caller.getVipConnexion().getReponse("mt1 shutup");
		
		HashMap<OmsCall, String> omsList = annuaire.getAnnuaire();
		Set<OmsCall> listOms = omsList.keySet();
		
		WebSocket callerWs = caller.getWebSocket();
		callerWs.send("answer:" + this.getUserName());
		caller.setIsCaller(true);
		
		Random ran = new Random();
		int randomNb = ran.nextInt();
		
		OmsCall callee = this;
		List<OmsCall> listOmsCallInConf2 =  new ArrayList<OmsCall>();
		
		if(conf.isClientJoined(callee)){
						
			caller.getVipConnexion().getReponse("mt1 shutup");
			conf.stopPlay();
			
			conferenceParam = new ConferenceParameters(callee.getConfname());
			conferenceParam.setName(caller.getUserName());			
			conf.add(caller, conferenceParam);				
			
			listOmsCallInConf2 = conf.getListOmsCallInConf(callee.getConfname());
			
			ArrayList<OmsCall> clonedList = new ArrayList<OmsCall>();
			ArrayList<OmsCall> clonedList2 = new ArrayList<OmsCall>();
			clonedList.addAll(listOmsCallInConf2);
			clonedList2.addAll(listOmsCallInConf2);
			
			Iterator<OmsCall> ite2 = clonedList.iterator();	
			Iterator<OmsCall> ite3;		
			OmsCall c2, c3;
			
			while (ite2.hasNext()) {
				c2 = ite2.next();	
				ite3 = clonedList2.iterator();
				while(ite3.hasNext()){
					c3 = ite3.next();
					if(!c3.equals(c2))
						c2.getWebSocket().send("hide:" + c3.getUserName());
				}			
			}
			
		} else {
			
			caller.getVipConnexion().getReponse("mt1 shutup");
			this.getVipConnexion().getReponse("mt1 shutup");
			
			if (conf.isClientJoined(caller)) {
				caller.setExConfName(caller.getConfname());
				conf.delete(caller);
			}

			conferenceParam = new ConferenceParameters(Integer.toString(randomNb));
			conferenceParam.setEntertone("false");
			conferenceParam.setExittone("false");
			
			conferenceParam.setName(caller.getUserName());
			conf.create(caller, conferenceParam);
			conferenceParam.setName(callee.getUserName());
			conf.add(callee, conferenceParam);
		}
		
		conf.showParticipant(caller.getConfname(), true);
		//conf.participantsStatus(caller);
		//conf.participantsStatus(callee);
		
		//conf.infos(filePath);
		//logger.info(filePath + " is available");
		
		Iterator<OmsCall> ite = listOms.iterator();
		OmsCall c;
		
		while(ite.hasNext()){
			
			c = ite.next();
			conf.participantsStatus(c);
		}			
	}
	
	/*public void updateStatusAfterAnswer(Annuaire annuaire, OmsConference conf) throws OmsException{
		
		HashMap<OmsCall, String> omsList = annuaire.getAnnuaire();		
		Set<OmsCall> listOms = omsList.keySet();
				
		Iterator<OmsCall> ite = listOms.iterator();
		OmsCall c;
					
		while(ite.hasNext()){
			
				c = ite.next();
				conf.showPeopleInConf(c);
			}
			
		}*/
				
	/*public void updateStatusAfterHangup(Annuaire annuaire,OmsConference conf) throws OmsException{
		
		HashMap<OmsCall, String> omsList = annuaire.getAnnuaire();
		
		Set<OmsCall> listOms = omsList.keySet();
				
		Iterator<OmsCall> ite = listOms.iterator();
		OmsCall c;
					
		while(ite.hasNext()){
				
				c = ite.next();
				conf.showPeopleAfterHangup(c);
			}
			
		}*/
	
	public void reject(OmsCall caller, OmsConference conf) throws OmsException{
		
		if(caller == null)
			throw new IllegalArgumentException("Argument cannot be null");
		
		caller.getVipConnexion().getReponse("mt1 shutup");
		
		caller.setIsCaller(false);
		WebSocket callerWs = caller.getWebSocket();
		String userName = this.getUserName();
		
		callerWs.send("reject:" + userName);
				
		if(conf.isClientJoined(this)){			
			conf.stopPlay();
		}
		else
			this.getVipConnexion().getReponse("mt1 shutup");
	}
	
	
	public void hangup(OmsCall omsCall, OmsConference conf, Annuaire annuaire) throws OmsException, 
	IOException{
					
		//Vérifier le status de la conférence conf.status(call.getConfname())		
		if(omsCall == null)
			throw new IllegalArgumentException("Argument OmsCall cannot be null");
		if(conf == null)
			throw new IllegalArgumentException("Argument Annuaire cannot be null");
		else if(annuaire == null)
			throw new IllegalArgumentException("Argument OmsConference cannot be null");
		
		List<OmsCall> listOmsCallInConf2 = new ArrayList<OmsCall>();
		
		OmsCall callee, caller;
		String userName = this.getUserName();
		String userName2 = omsCall.getUserName();
		WebSocket callerWs = omsCall.getWebSocket();
		
		HashMap<OmsCall, String> omsList = annuaire.getAnnuaire();
		Set<OmsCall> listOms = omsList.keySet();
		
		Iterator<OmsCall> ite = listOms.iterator();
		OmsCall c;		
					
		while(ite.hasNext()){
						
			c = ite.next();
			
			//if(!c.equals(omsCall) && !this.equals(c)){
				
				c.getWebSocket().send("showUserNameConnectedToOMS:" + userName+ ":hangup");
				c.getWebSocket().send("showUserNameConnectedToOMS:" + userName2+ ":hangup");
			//}
				
		}
							
			if(this.getIsCaller()){
				
				listOmsCallInConf2 = conf.getListOmsCallInConf(this.getConfname());
				//conf.delete(omsCall);
				//conf.delete(this);
				this.setIsCaller(false);
				callee = omsCall;
				caller = this;								
			}
			else{
				
				listOmsCallInConf2 = conf.getListOmsCallInConf(omsCall.getConfname());
				//conf.delete(this);
				//conf.delete(omsCall);
				omsCall.setIsCaller(false);
				callee = this;
				caller = omsCall;
			}
			//callerWs.send("hangup:" + userName);
			
			
			//Iterator<OmsCall> ite;
			Iterator<OmsCall> ite2 = listOmsCallInConf2.iterator();
			OmsCall c2;
			//String userName;				
						
			while (ite2.hasNext()) {
				
				c2 = ite2.next();
				//userName = c2.getUserName();
				
				if(!this.equals(c2)){
					
					c2.getWebSocket().send("hangup:" + this.getUserName());
					c2.getWebSocket().send("deleteUserNameInConf:" + this.getUserName());
					this.getWebSocket().send("deleteUserNameInConf:" + c2.getUserName());
					
				}									
			}
			
			conf.delete(callee);
			conf.delete(caller);
						
			//Vérifier si je faisais parti d'une conférence avant l'appel que j'ai reçu
			 //trouver le nom de son ancienne conférence
			
			if(callee.getExConfName() != null){//method getExConfName private		
				
				String exConfName = callee.getExConfName();
				
				conferenceParam = new ConferenceParameters(callee.getExConfName());	
				conferenceParam.setName(callee.getUserName());
				if(conf.status(exConfName))
					conf.add(callee, conferenceParam);
				else 
					conf.create(callee, conferenceParam);
				
				callee.setExConfName(null);
				//conf.add(callee, conferenceParam);	
				logger.info("Callee back in his conference");			
			}		
			
			if(caller.getExConfName() != null){
				
				caller.setExConfName(null);
				conferenceParam = new ConferenceParameters(caller.getExConfName());
				conferenceParam.setName(caller.getUserName());
				conf.add(caller, conferenceParam);
			}
			
			//conf.infos(filePath);
			
			ite = listOms.iterator();				
			while(ite.hasNext()){
				
				c = ite.next();
				conf.participantsStatus(c);
			}	
	}
	
	public boolean getIsCaller(){
		return isCaller;
	}
	
	public void setIsCaller(boolean bool){
		isCaller = bool;;
	}
		
	/**
	 * To answer a call with its sdp
	 * @param sdp the callee's sdp
	 * @throws OmsException
	 */

	/*public void answer(String sdp) throws OmsException {
		// TODO Auto-generated method stub
		
		//int clientNumber = this.getNbOfClientConnected();
		int clientNumber =0;
		createNewRvi(clientNumber);
		Sdp sdpAnswer = this.sdpAnswer("mt"+ clientNumber, sdp);
		this.conn.send(sdpAnswer.getSdp());
		
		String mediaConn = this.connOMS.getReponse("wait evt=mt"+clientNumber+".mediaconnected");
		if(!mediaConn.startsWith("OK"))
			throw new OmsException("media is not connected " + mediaConn);
		
		String setParam = this.connOMS.getReponse("mt"+clientNumber+" setparam bind=s"+clientNumber);
		if(!setParam.equals("OK"))
			throw new OmsException("cannot excute cmd setparam bind=s"+ clientNumber +" " + setParam);
		
		String trombonne = this.connOMS.getReponse("mt1 extend trombone mt"+ clientNumber);
		if(!trombonne.equals("OK"))
			throw new OmsException("cannot trombonne calls");
			
	}*/

	/**
	 * To create the callee's rvi when he answers
	 * @param clientNumber the callee rank in the list of those who has been already called
	 * @throws OmsException
	 */
	/*private void createNewRvi(int clientNumber) throws OmsException{
		
		String respWebrtcCreation = this.connOMS.getReponse("new mt"+ clientNumber +" webrtc");		
		if (respWebrtcCreation.equals("OK")) {
			this.connOMS.getReponse("mt"+ clientNumber +" setparam escape_sdp_newline=true");
		} else 
			throw new OmsException("Error: Cannot create rvi webrtc "+ respWebrtcCreation);
		
		String resp1 = this.connOMS.getReponse("new s"+ clientNumber +" synt");//pas sûr qu'on ait besoin
		if (!resp1.equals("OK")) 
			throw new OmsException("Error: Cannot create rvi synt " + resp1);
	}*/
	
	/**
	 * Exchanging one's sdp before connecting to OMS
	 * @param sdp 
	 * @throws OmsException
	 */
	
	public void init(String sdp) throws OmsException{
		
		Sdp sdpAnswer = this.sdpAnswer("mt1", sdp);
		this.conn.send(sdpAnswer.getSdp());

		String mediaConn = connOMS.getReponse("wait evt=mt1.mediaconnected");
		if(!mediaConn.startsWith("OK"))
			throw new OmsException("media is not connected " + mediaConn);		
	}
	
	/**
	 * To close the connection to OMS
	 * @throws OmsException
	 * @throws IOException 
	 */
	
	public void delete() throws OmsException, IOException {
	
		VipConnexion vipConn = getVipConnexion();	
		if(vipConn != null){		
			delResources();
			vipConn.getSocket().close();
			//listOmsCall.clear();
		}					
	}

	public void setVipConnexion(VipConnexion connOMS) {
		this.connOMS = connOMS;
	}

	/**
	 * To get the connection to OMS
	 * @return the connection to OMS
	 */
	public VipConnexion getVipConnexion() {
		return this.connOMS;
	}
	
	/**
	 * To get the client websocket
	 * @return websocket of the client
	 */
	public WebSocket getWebSocket(){	
		return this.conn;
	}
	
	/**
	 * To get the client IP address 
	 * @return IP address of the client
	 */
	public String getIpAddress(){	
		return ipAddress;
	}
	
	/**
	 * To get OMS's IP address and port
	 * @return IP address and port of OMS
	 */
	public String[] getHostPortVip(){
		return hosPortVip;
	}
	
	/**
	 * To set a number for the client in the conference
	 * @param num number for the client
	 */
	public void setPartNumberConf(int num){	
		partNumberConf = num;
	}
	
	/**
	 * To get the number for the client in the conference
	 * @return the number for the client
	 */
	public int getPartNumberConf(){	
		return partNumberConf;
	}
	
	public void setHasCreatedConf(boolean bool){
		hasCreatedConf = bool;		
	}
	
	public boolean getHasCreatedConf(){
		return hasCreatedConf;
	}
	
	public void setConfName(String conf){
		confName = conf;
	}
	
	public String getConfname(){	
		return confName;
	}
	
	public void setUserName(String userName){
		this.userName = userName;
	}
	
	public String getUserName(){
		return userName;
	}
	
	/**
	 * To dealt with the case where a Browser either clicks on the disconnect button or just close 
	 * its web page. But, this is implemented on the OmsService Class, thus do not care about this method if
	 * you want to develop the service.
	 * @return a boolean checking whether the Browser clicks on disconnect or not
	 */
	public boolean getHasClientPressDisc(){
		return hasClientPressDisc;
	}
	
	/**
	 * To dealt with the case where a Browser either clicks on the disconnect button or just close 
	 * its web page. But, this is implemented on the OmsService Class, thus do not care about this method if
	 * you want to develop the service.
	 * @param bool is set to true if client clicks on true before leaving the web page
	 */
	public void setHasClientPressDisc(boolean bool){
		hasClientPressDisc = bool;
	}
	
	
	/*public void setNbOfClientConnected(int num){
		this.nbOfClientConnected = num;
	}
	

	public int getNbOfClientConnected(){
		return this.nbOfClientConnected;
	}*/
	

	/*public void setIsCaller(boolean isCaller){
		this.isCaller = isCaller;
	}
	
	public void setIsCallee(boolean isCallee){
		this.isCallee = isCallee;
	}
	

	public boolean getIsCaller(){
		return this.isCaller;
	}
	

	public boolean getIsCallee(){
		return this.isCallee;
	}*/
	
}
