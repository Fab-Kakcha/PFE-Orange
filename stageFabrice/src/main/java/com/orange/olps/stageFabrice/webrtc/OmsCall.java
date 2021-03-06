package com.orange.olps.stageFabrice.webrtc;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
//import java.util.HashMap;
//import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;

import com.orange.olps.stageFabrice.webrtc.OmsException;

//import com.orange.olps.stageFabrice.OmsException;

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
	private List<OmsCall> listOmsCall = new ArrayList<OmsCall>();
	private boolean isRviSyntExist = false;
	private boolean isRviEnregExist = false;
	private boolean isRviRecogExist = false;
	private boolean isCaller = false;
	private boolean isCallee = false;
	private int nbOfClientConnected = 1;
	private WebSocket conn = null;
	private String ipAddress;
	private int partNumberConf;
	
	private String[] hosPortVip;
	
	/**
	 * 
	 */
	public OmsCall() {

		super();
	}

	/**
	 * 
	 * @param conn client websocket
	 * @param ipAddress client IP address
	 */
	public OmsCall(WebSocket conn, String ipAddress) {
		 
		this.conn = conn;
		this.ipAddress = ipAddress;
	}
	
	/**
	 * To connect to OMS
	 * @param hostVip OMS's IP address 
	 * @param portVip OMS's listening port
	 * @throws OmsException
	 */
	public void connect(String hostVip, String portVip) throws OmsException{
		
		hosPortVip = new String[2];
		hosPortVip[0] = hostVip;
		hosPortVip[1] = portVip;
		
		this.connOMS = new VipConnexion(hostVip, portVip);
		//String respInfo = this.connOMS.getReponse("info ocam");
		String respWebrtcCreation = this.connOMS.getReponse("new mt1 webrtc");
		
		if (respWebrtcCreation.equals("OK")) {
			this.connOMS.getReponse("mt1 setparam escape_sdp_newline=true");
		} else 
			throw new OmsException("Error: Cannot create rvi webrtc "+ respWebrtcCreation);		
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
	
	/**
	 * 
	 * @param rviWebrtc
	 * @param sdpNav
	 * @return
	 * @throws OmsException
	 */
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
	 * To play a file
	 * @param say
	 * @param interrupt
	 * @throws OmsException
	 */
	
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
		
		sleep(600);
		
		String setParam = this.connOMS.getReponse("mt1 setparam bind=s1");
		if(!setParam.equals("OK"))
			throw new OmsException("cannot execute mt1 setparam bind=s1");
		
		String respPlay = connOMS.getReponse("s1 play file=" + filePath); //loop=1
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
	
	/**
	 * To delete all the rvi resources
	 * @throws OmsException
	 */
	
	public void delResources() throws OmsException{
			
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
			
			String delRviWebrtc = this.getVipConnexion().getReponse("delete mt" + num);
			if (delRviWebrtc.equals("OK"))
				this.getVipConnexion().getReponse("wait evt=mt"+ num +".mediadisconnected");
			else
				throw new OmsException("Error cannot delete webrtc rvi mt"+ num +": "+ delRviWebrtc);
			
			String delRviSyn = this.getVipConnexion().getReponse("delete s" + num);
			if(!delRviSyn.equals("OK"))
				throw new OmsException("Error cannot delete synt rvi s " + delRviSyn);
			
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
	 */
	
	public void call(OmsCall omsCall) throws OmsException {

		//int num = this.getNbOfClientConnected();
		//num += 1;
		//this.setNbOfClientConnected(num);
		
		WebSocket calleeWs = omsCall.getWebSocket();
		omsCall.setVipConnexion(this.connOMS);
		//omsCall.setNbOfClientConnected(this.getNbOfClientConnected());
		//omsCall.setIsCallee(true);
		this.listOmsCall.add(omsCall);

		calleeWs.send("incomingCall");
	}
	
	/**
	 * To answer a call with its sdp
	 * @param sdp the callee's sdp
	 * @throws OmsException
	 */

	public void answer(String sdp) throws OmsException {
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
			
	}

	/**
	 * To create the callee's rvi when he answers
	 * @param clientNumber the callee rank in the list of those who has been already called
	 * @throws OmsException
	 */
	private void createNewRvi(int clientNumber) throws OmsException{
		
		String respWebrtcCreation = this.connOMS.getReponse("new mt"+ clientNumber +" webrtc");		
		if (respWebrtcCreation.equals("OK")) {
			this.connOMS.getReponse("mt"+ clientNumber +" setparam escape_sdp_newline=true");
		} else 
			throw new OmsException("Error: Cannot create rvi webrtc "+ respWebrtcCreation);
		
		String resp1 = this.connOMS.getReponse("new s"+ clientNumber +" synt");//pas sûr qu'on ait besoin
		if (!resp1.equals("OK")) 
			throw new OmsException("Error: Cannot create rvi synt " + resp1);
	}
	
	/**
	 * Exchanging one's sdp before connecting to OMS
	 * @param sdp 
	 * @throws OmsException
	 */
	
	public void init(String sdp) throws OmsException{
		
		Sdp sdpAnswer = this.sdpAnswer("mt1", sdp);
		this.conn.send(sdpAnswer.getSdp());

		String mediaConn = connOMS.getReponse("wait evt=mt1.mediaconnected");
		//if(!mediaConn.startsWith("OK"))
			//throw new OmsException("media is not connected " + mediaConn);		
	}
	
	/**
	 * To close the connection to OMS
	 * @throws OmsException
	 * @throws IOException 
	 */
	
	public void closeClient() throws OmsException, IOException {
	
		this.delResources();
		this.connOMS.getSocket().close();
		this.listOmsCall.clear();
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
	 * @return websocket of the clientSSSS
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
