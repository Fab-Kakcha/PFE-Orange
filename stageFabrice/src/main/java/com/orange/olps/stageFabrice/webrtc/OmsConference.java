/**
 * 
 */
package com.orange.olps.stageFabrice.webrtc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;

/**
 * @author JWPN9644
 *
 */
public class OmsConference {
	
	private static Pattern pat1 = Pattern.compile("mediaoutput=\"([^ \t\"]+)");
	private static Pattern pat2 = Pattern.compile("mediainput=\"([^ \t\"]+)");
	private static Pattern pat3 = Pattern.compile("participant id=\"([^ \t\"]+)");
	
	private static Logger logger = Logger.getLogger(OmsConference.class);
	private VipConnexion connOMSConf = null;
	private VipConnexion connOMSCall = null;
	private WebSocket websock = null;
	private String confName = null;
	private int nbOfPartInConf; //Total number of participant in the conf
	private List<OmsCall> listOmsCall = new ArrayList<OmsCall>(); // To dealt with the connection and reconnection
	//of a user in the conf.
	private List<OmsCall> listOmsCallInConf = new ArrayList<OmsCall>(); // To send particular msg to users 
	//currently in the conf
	private List<Integer> arrayList = new ArrayList<Integer>();
	
	/**
	 * 
	 * @param name
	 * @param hostVipConf
	 * @param portVipConf
	 * @throws OmsException
	 * @throws IOException
	 */
	public OmsConference(String name, String hostVipConf, String portVipConf) throws OmsException, IOException{
		
		confName = name;
		nbOfPartInConf = 0;
		connOMSConf = new VipConnexion(hostVipConf, portVipConf);
		String respCreation = connOMSConf.getReponse("<conference> <create requestid=\"req1\" conferenceid=\"" + 
		confName + "\" /></conference>" );
		
		if (respCreation.indexOf("OK") == -1)
			throw new OmsException("Error cannot create the conference : "+ respCreation);		
	}
	
	/**
	 * 
	 * @param omsCall
	 * @param param
	 * @throws OmsException
	 */
	public void join(OmsCall omsCall, String param) throws OmsException{
		
		String repJoin;
		int num = 1;
		connOMSCall = omsCall.getVipConnexion();
		websock = omsCall.getWebSocket();
		
		/* To deal with the connection and reconnexion of a client into the same conference*/
		if(listOmsCallInConf.isEmpty()){

			listOmsCallInConf.add(omsCall);
			omsCall.setPartNumberConf(num);
		}else{
	
			Iterator<OmsCall> ite = listOmsCallInConf.iterator();		
			while(ite.hasNext()){
		
				arrayList.add(ite.next().getPartNumberConf());
			}
			
			num = Collections.max(arrayList) + 1;			
			listOmsCallInConf.add(omsCall);
			omsCall.setPartNumberConf(num);
		}
		
		String respSh = connOMSCall.getReponse("mt1 shutup");
		if(!respSh.equals("OK"))
			throw new OmsException("Cannot shutup mt1");
		
		if(param.equalsIgnoreCase("mute")){
			logger.info("m muted");
			 repJoin = connOMSConf.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""+ 
			confName +"\"  participantid=\""+ num +  "\" confrole=\"mute\" /></conference>");
		}
		else
			repJoin = connOMSConf.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""+
		confName+"\"  participantid=\""+ num + "\" /></conference>");
		
		if(repJoin.indexOf("OK") != -1 ){
			
			Matcher mat1 = pat1.matcher(repJoin);
			
			if(mat1.find()){
				String mediaOutput = "/" + mat1.group(1) + "";
				//logger.info("mediaOutput: " + mediaOutput);
				//String answer = new String("s2 say \"cat /opt/application/64poms/current/tmp"+ mediaOutput + "\"");
				
				String respSynt = connOMSCall.getReponse("new s2 synt.synt host=127.0.0.1 port=7777");
				if (respSynt.indexOf("OK")!=-1){					
					String respBind = connOMSCall.getReponse("mt1 setparam bind=s2");
					
					if (respBind.indexOf("OK")!=-1){
						String respCodec = connOMSCall.getReponse("s2 setparam ttscodec=a8k");
						
						if (respCodec.indexOf("OK")!=-1){
							String respSay = connOMSCall.getReponse("s2 say \"cat /opt/application/64poms/"
									+ "current/tmp"+ mediaOutput + "\"");
							
							if(respSay.indexOf("OK")==-1)
								throw new OmsException("say cmd failed  : "+ respSay);							
						}else
							throw new OmsException("cmd s2 setparam ttscodec=a8k : "+ respCodec);							
					}else 
						throw new OmsException("mt1 setparam bind=s2 : "+ respBind);
				}else
					throw new OmsException("new s2 synt.synt host=127.0.0.1 port=7777 "+ respSynt);			
			}else
				throw new OmsException("mediaoutput :  NO MATCH "+repJoin);			
		
		Matcher mat2 = pat2.matcher(repJoin);
		
		if(mat2.find()){
			
			String mediaInput = "/" + mat2.group(1);
			//logger.info("mediaInput: " + mediaInput);
			String respEnreg = connOMSCall.getReponse("new e1 enreg");
			if(respEnreg.indexOf("OK") == -1)
				throw new OmsException("cannot create a new recording ressource: "+ respEnreg);
			
			String startRec = connOMSCall.getReponse("e1 start /opt/application/64poms/current/tmp"+ 
				mediaInput + "");
			if(startRec.indexOf("OK") == -1)
				throw new OmsException("cannot record: "+ startRec);
			
		}else 
			throw new OmsException("mediaintput :  NO MATCH "+repJoin);	
		
	   }
		else if(repJoin.indexOf("406") != -1){			
			websock.send("Conference does not yet exist");
			throw new OmsException("Error cannot join the conference : "+repJoin);
		}
		else if(repJoin.indexOf("411") != -1)
			throw new OmsException("Delete files conf_1.rd and conf_1.wr at /opt/application/64poms/current/tmp");
		else if(repJoin.indexOf("408") != -1)
			throw new OmsException("Error cannot join the conference : "+repJoin);
	}
	
	
	/**
	 * 
	 * @param omsCall
	 * @throws OmsException
	 */
	public void unJoin(OmsCall omsCall) throws OmsException{
		
		int num = omsCall.getPartNumberConf();
		String conf = getConfName();
		
		String unJoinrep=getConfVipConnexion().getReponse("<conference><unjoin conferenceid=\""+conf+"\" requestid=\""
		+ num + "\" participantid=\""+ num + "\"/></conference>");
		if (unJoinrep.indexOf("OK")==-1)
			throw new OmsException("unjoining failed : " + unJoinrep);
			
		Iterator<OmsCall> ite = listOmsCallInConf.iterator();
		int num2;
		while(ite.hasNext()){
			num2 = ite.next().getPartNumberConf();
			if(num2 == num)
				ite.remove();
		}
		
		if(listOmsCallInConf.size() == 0){
			
			destroyConference(omsCall);
			logger.info("Conference destroyed");
		}	
	}
	
	/**
	 * 
	 * @param omsCall
	 * @throws OmsException
	 */
	public void destroyConference(OmsCall omsCall) throws OmsException{
		
		connOMSCall = omsCall.getVipConnexion();
		listOmsCall.clear();
		
		String repStatus = connOMSConf.getReponse("<conference><status requestid=\"req2\" conferenceid=\""+ 
		confName+"\" /></conference>");
		if(repStatus.indexOf("OK")!=-1){
			
			Matcher mat3 = pat3.matcher(repStatus);
			while(mat3.find()){
				
				String participantId = "\""+ mat3.group(1)+"\"";
				String unjoinRep= connOMSConf.getReponse("<conference><unjoin requestid=\"req2\" conferenceid=\""+ 
				confName+"\"  participantid=" +participantId.toString() +"/></conference>");
				if(unjoinRep.indexOf("OK") == -1)
					throw new OmsException("cannot unjoin  participant : " + participantId + "because: " + unjoinRep);
			}
			
			String destroyConf = connOMSConf.getReponse("<conference><destroy requestid=\"req3\" conferenceid=\"" +
			confName+"\" /></conference>");
			
			if(destroyConf.indexOf("OK") != -1){
				
				String rep1= connOMSCall.getReponse("wait evt=mt1.*" );
				
				//if (rep1.indexOf("OK")!=-1){
					//connOMSCall.getReponse("mt1 shutup");
					//connOMSCall.getReponse("delete mt1");
					connOMSCall.getReponse("delete e1");
					//connOMSCall.getReponse("delete s");
					connOMSCall.getReponse("delete s2");
					//}
			}else
				throw new OmsException("cannot destroy conference because: " + destroyConf);
		}else
			throw new OmsException("Conference does not exist : "+ repStatus);
		
	}
	
	
	public void recordConf(){
		
		return;
	}
	
	public void setNbOfPartInConf(int num){
		nbOfPartInConf = num;
	}
	
	public int getNbOfPartInConf(){
		return nbOfPartInConf;
	}
	
	
	public VipConnexion getConfVipConnexion(){		
		return connOMSConf;
	}
	
	public String getConfName(){		
		return confName;
	}
}
