/**
 * 
 */
package com.orange.olps.stageFabrice.webrtc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
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
	private List<OmsCall> listOmsCallInConf = new ArrayList<OmsCall>(); // To send particular msg to users 
	//currently in the conf
	private List<Integer> arrayList = new ArrayList<Integer>();
	private OmsCall omsCallRecord = null;
	private Random randomGenerator;
	private String enregFile = "/opt/application/64poms/current/tmp/enregFile";
	private boolean destroyConf = false;
	
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
		connOMSConf = new VipConnexion(hostVipConf, portVipConf);
		/*String respCreation = connOMSConf.getReponse("<conference> <create requestid=\"req1\" conferenceid=\"" + 
		confName + "\" /></conference>" );
		
		if (respCreation.indexOf("OK") == -1)
			throw new OmsException("Error cannot create the conference : "+ respCreation);*/
	}
	
	public void create() throws OmsException{
		
		String confName = getConfName();
		
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
		String unJoinRep;
		boolean unjoin = false;
			
		Iterator<OmsCall> ite = listOmsCallInConf.iterator();
		int num2;
		while(ite.hasNext()){
			num2 = ite.next().getPartNumberConf();
			if(num2 == num){			
				unJoinRep=getConfVipConnexion().getReponse("<conference><unjoin conferenceid=\""+conf+"\" requestid=\""
						+ num + "\" participantid=\""+ num + "\"/></conference>");
						if (unJoinRep.indexOf("OK")==-1)
							throw new OmsException("unjoining failed : " + unJoinRep);
				ite.remove();
				unjoin = true;
			}			
		}
		
		if(!unjoin){
			logger.info("participant "+ num + " already unjoined");
		}
		
		if(listOmsCallInConf.size() == 0 && !destroyConf){			
			destroyConference(omsCall);
			destroyConf = true;
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
		listOmsCallInConf.clear();
		
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
	
	
	public void recordConf() throws OmsException, IOException{
			
		if(listOmsCallInConf.isEmpty()){		
			throw new OmsException("Cannot start recording the conf (No one in the conf)");
		}
		else{			
			randomGenerator = new Random();
			omsCallRecord = new OmsCall();
			
			int index = randomGenerator.nextInt(listOmsCallInConf.size());
			String [] hostPortVip = listOmsCallInConf.get(index).getHostPortVip();
			omsCallRecord.connect(hostPortVip[0], hostPortVip[1]);
			
			String respJoin;
			int num = 0;
								
			respJoin = connOMSConf.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""+ 
					confName +"\"  participantid=\""+ num +  "\"/></conference>");			
						
			if(respJoin.indexOf("OK") != -1 ){							
			
			Matcher mat2 = pat2.matcher(respJoin);
			
			if(mat2.find()){
				
				String mediaInput = "/" + mat2.group(1);
				String mediaInputPath = "/opt/application/64poms/current/tmp" + mediaInput;
				OutputStream outputStream = new FileOutputStream(new File(mediaInputPath));
				//OutputStreamWriter outputWritter = new OutputStreamWriter(outputStream);
				//Writer writer = new BufferedWriter(outputWritter);
				//writer.write("begin");
				String string = "Hello";
				byte[] buf =  new byte[1024];
				buf = string.getBytes();
				outputStream.write(buf, 0, buf.length);
							
				outputStream.close();
				//String respEnreg = connOMSCall.getReponse("new e1 enreg");				
				//String startRec = connOMSCall.getReponse("e1 start /opt/application/64poms/current/tmp"+ 
					//mediaInput + "");
				
			}else 
				throw new OmsException("mediaintput :  NO MATCH "+respJoin);
			
			Matcher mat1 = pat1.matcher(respJoin);
			
			if(mat1.find()){
			
				long endTime = System.currentTimeMillis() + 15000;
				
				String mediaOutput = "/" + mat1.group(1) + "";
				//String mediaOutputPath = "/opt/application/64poms/current/tmp/conf_1.wr";
				String mediaOutputPath = "/opt/application/64poms/current/tmp" + mediaOutput;
				InputStream inputStream = new FileInputStream(new File(mediaOutputPath));
				//BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
				
				//System.out.println("2");
				byte[] buf =  new byte[1024];
				int bytes_read;
				
				//String line = reader.readLine();			
				String enregFileRaw = enregFile + ".raw";
				File fRaw = new File(enregFileRaw);
				if(fRaw.exists()){
					fRaw.delete();
					System.out.println(enregFileRaw + " deleted");
					fRaw.createNewFile();
				}
				
				//OutputStream outputStream = new FileOutputStream(new File(enregFileRaw), true);
				OutputStream outputStream = new FileOutputStream(fRaw, true);
				//OutputStreamWriter outputWritter = new OutputStreamWriter(outputStream);
				//Writer writer = new BufferedWriter(outputWritter);
				
				//while(line != null){
				while(System.currentTimeMillis() < endTime){
					
					bytes_read = inputStream.read(buf);
					outputStream.write(buf, 0, bytes_read);
					
					//writer.write(line);
					//line = reader.readLine();
					//System.out.println("3");
					//System.out.println(bytes_read);
				}				
					//data = reader.read();
				//}
	
				//writer.write("begin");
				//writer.write(line);
				
				inputStream.close();
				outputStream.close();
				logger.info("Copy finish");
				/*String respSay = connOMSCall.getReponse("s2 say \"cat /opt/application/64poms/"
									+ "current/tmp"+ mediaOutput + "\"");*/			
			}else
				throw new OmsException("mediaoutput :  NO MATCH "+respJoin);
						
		   }
			else if(respJoin.indexOf("406") != -1){			
				websock.send("Conference does not yet exist");
				throw new OmsException("Error cannot join the conference : "+respJoin);
			}
			else if(respJoin.indexOf("411") != -1)
				throw new OmsException("Delete files conf_1.rd and conf_1.wr at /opt/application/64poms/current/tmp");
			else if(respJoin.indexOf("408") != -1)
				throw new OmsException("Error cannot join the conference : "+respJoin);
			
		}
	}
	
	
	public void stopRecordConf() throws OmsException, IOException{
			
		int num = 0;
		String conf = getConfName();
		
		String unJoinrep=connOMSConf.getReponse("<conference><unjoin conferenceid=\""+conf+"\" requestid=\""
		+ num + "\" participantid=\""+ num + "\"/></conference>");
		
		if (unJoinrep.indexOf("OK")==-1)
			throw new OmsException("unjoining failed : " + unJoinrep);

		omsCallRecord.getVipConnexion().close();
		logger.info("Recorder disconnects");
	}
	
	
	public void playEnregConf(OmsCall omsCall) throws OmsException, IOException, InterruptedException{
		
		Process p;
		int returnCode;
		String enregFileRaw = enregFile + ".raw";
		String enregFileWav = enregFile + ".wav";
		String enregFilea8k = enregFile + ".a8k";
		String testAnimaux = "/opt/application/64poms/current/tmp/Animaux.a8k";
		
		File fWav = new File(enregFileWav);
		if(fWav.exists()){
			fWav.delete();
			System.out.println(enregFileWav + " deleted");
			//fWav.createNewFile();
		}
		
		File fa8k = new File(enregFilea8k);
		if(fa8k.exists()){
			fa8k.delete();
			System.out.println(enregFilea8k + " deleted");
		}
		
		//String cmd = new String("sox -t al -r 8000 -b 8 -c 1"+enregFile+".raw "+enregFile+".wav");
		String cmd1 = new String("sox -t al -r 8000 -b 8 -c 1 " + enregFileRaw + " " + enregFileWav);
		String cmd2 = new String("sox -t wav " + enregFileWav + " -t raw -r8000 -e a-law -b 8 -c 1 " + enregFilea8k);
		//sox -t wav CON1100528_G711A_sortante.wav  -t raw -r8000 -e a-law -b 8 -c 1 toto.a8K
		p = Runtime.getRuntime().exec(cmd1);
		returnCode = p.waitFor();
		if(returnCode != 0)		
			throw new OmsException("sox command failed " + returnCode);
		
		p = Runtime.getRuntime().exec(cmd2);
		returnCode = p.waitFor();
		if(returnCode != 0)		
			throw new OmsException("sox command failed " + returnCode);
		
		unJoin(omsCall);
		omsCall.play(enregFilea8k, false);//Ne peux pas lire le fichier alors qu'on est dans la conférence
	}
	
	public int getNbOfPartInConf(){
		//return nbOfPartInConf;
		return listOmsCallInConf.size();
	}
		
	public VipConnexion getConfVipConnexion(){		
		return connOMSConf;
	}
	
	public String getConfName(){		
		return confName;
	}
}
