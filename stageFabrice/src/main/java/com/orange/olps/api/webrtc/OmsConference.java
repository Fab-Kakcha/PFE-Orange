/**
 * This Java's Class is about making conference in OMS.
 */

package com.orange.olps.api.webrtc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;

/**
 * @author JWPN9644
 * 
 */

//Check here and in OmsCall that the Browser is connected before deleting its ressources.

public class OmsConference implements Runnable {

	private final static int ARRAY_SIZE = 1024 * 1024;

	private static Pattern pat1 = Pattern.compile("mediaoutput=\"([^ \t\"]+)");
	private static Pattern pat2 = Pattern.compile("mediainput=\"([^ \t\"]+)");
	private static Pattern pat3 = Pattern.compile("participant id=\"([^ \t\"]+)");
	private static Pattern pat4 = Pattern.compile("conferenceinfo ([^/>]+)");
	private static Pattern pat5 = Pattern.compile("conferenceid=\"([^\\s\"]+)");
	private static Pattern pat6 = Pattern.compile("participant ([^/>]+)");
	private static Pattern pat7 = Pattern.compile("currentconf=\"([^\\s/>]+)");
	private static Pattern pat8 = Pattern.compile("currentpart=\"([^\\s/>]+)");
	private static Pattern pat9 = Pattern.compile("confcreated=\"([^\\s/>]+)");
	private String regexSemiColon = ":";

	private static Logger logger = Logger.getLogger(OmsConference.class);
	private VipConnexion connOMSConf = null;
	private VipConnexion connOMSCall = null;
	private WebSocket websock = null;
	private String confName = null;
	private List<OmsCall> listOmsCallInConf = new ArrayList<OmsCall>();

	private List<Integer> arrayList = new ArrayList<Integer>();
	private OmsCall omsCallRecord = null;
	private Random randomGenerator;
	private String enregFile = "/opt/application/64poms/current/tmp/enregFile";
	private boolean destroyConf = false;
	private Thread t;
	private boolean running = true;
	private boolean isCoachExist = false;
	private boolean isStudentExist = false;
	private byte[] buf;
	private boolean hasCreatedConf = false;

	//On peut avoir plusieurs conférences, dans une même session
	//identifier chaque conférence
	//intenfier les participants de chaque conférence
	protected static HashMap<String, List<OmsCall>> annuaireForConference = 
			new HashMap<String, List<OmsCall>>(); 
	

	public OmsConference(){
		super();
	}
	
	
	/**
	 * To initiate a connection with OMS to do a conference
	 * 
	 * @param hostVipConf
	 *            OMS's IP address
	 * @param portVipConf
	 *            OMS's listening port for the conference
	 * @throws OmsException
	 * @throws IOException
	 */
	public OmsConference(String hostVipConf, String portVipConf)
			throws OmsException, IOException {
		
		connOMSConf = new VipConnexion(hostVipConf, portVipConf);

	}

	/**
	 * To create the conference, and if a conference has already been creaded,
	 * then a message is sent to the Browser letting him know the conference
	 * exists and he is welcome to join it.
	 * @param omsCall creator of the conference
	 * @param param param a String concatenating the name, mode(student, coach, mute) of participants
	 * and the conference name
	 * 	in the form of "firstname:mode:conferencename"
	 * @throws OmsException
	 */
	
	public void create(OmsCall omsCall, String param) throws OmsException {
		
		if(omsCall == null)
			throw new OmsException("OmsCall is null");
		
		if(param == null)
			throw new OmsException("Argument param is null");
		
		//websock = omsCall.getWebSocket();		
		//if(websock == null)
			//throw new OmsException("the client websocket is null");
			
		String[] splitParam = param.split(regexSemiColon);		
		if(splitParam.length != 3)
			throw new OmsException("Argument param must be in the form of "
					+ "firstname:mode:conferencename");
		
		confName = splitParam[2];
		boolean state = status(confName);
		// boolean state = false;
		if (state) {
			websock.send("confAlreadyExists");
		} else {
			String respCreation = connOMSConf
					.getReponse("<conference> <create requestid=\"req1\" conferenceid=\""
							+ confName + "\" /></conference>");

			if (respCreation.indexOf("OK") == -1)
				throw new OmsException("Error cannot create the conference : "
						+ respCreation);

			omsCall.setHasCreatedConf(true);
			add(omsCall, param);
		}
	}
	
	/**
	 * To add a OMS call into the conference, and if the conference does not yet
	 * exist, then a message is sent to the Browser for letting him know there
	 * is no conference yet.
	 * 
	 * @param omsCall
	 *            OMS call to join the conference
	 * @param param a String concatenating the name, mode(student, coach, mute) of participants
	 * and the conference name
	 * 	in the form of "firstname:mode:conferencename"
	 * @throws OmsException
	 */
	public void add(OmsCall omsCall, String param) throws OmsException {
				
		String repJoin, mode, username;
		String[] splitParam = param.split(regexSemiColon);
		
		if(splitParam.length != 3)
			throw new OmsException("Argument param must be in the form of "
					+ "firstname:mode:conferencename");
				
		username = splitParam[0];
		mode = splitParam[1];
		confName = splitParam[2];
		
		boolean state = status(confName);
		if (!state) {
			websock = omsCall.getWebSocket();
			websock.send("confDoesNotExist");
		} else {		
			
			connOMSCall = omsCall.getVipConnexion();
			omsCall.setConfName(confName);
			int num = 1;
			Iterator<OmsCall> ite2;
			
			if(!annuaireForConference.isEmpty()){
				
				Set<String> listeCli = annuaireForConference.keySet();
				Iterator<String> ite1 = listeCli.iterator();
				String ws;
				while (ite1.hasNext()) {
					
					ws = ite1.next();
					listOmsCallInConf = annuaireForConference.get(ws);
					ite2 = listOmsCallInConf.iterator();
					
					while (ite2.hasNext()) {
						arrayList.add(ite2.next().getPartNumberConf());
					}
				}
				
				num = Collections.max(arrayList) + 1;			
			}
						
			omsCall.setPartNumberConf(num);									
			
			if(!annuaireForConference.containsKey(confName)){

				List<OmsCall> listOmsCall = new ArrayList<OmsCall>();
				listOmsCall.add(omsCall);
				annuaireForConference.put(confName, listOmsCall);				
			}
			else{
				
				listOmsCallInConf = annuaireForConference.get(confName);
				listOmsCallInConf.add(omsCall);
				annuaireForConference.put(confName, listOmsCallInConf);
			}					
			
			System.out.println("participant number: " + num + " conf name: " + confName);
			
			String respSh = connOMSCall.getReponse("mt1 shutup");
			if (!respSh.equals("OK"))
				throw new OmsException("Cannot shutup mt1" + respSh);

			if (mode.equalsIgnoreCase("mute")) {
				System.out.println(mode);
				repJoin = connOMSConf
						.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""+ getName()
								+ "\"  participantid=\""+ num + "\" name=\""+ username+"\" confrole=\"mute\" /></conference>");
				processingJoinResp(repJoin);
				
			} else if (mode.equalsIgnoreCase("entertone")) {
				repJoin = connOMSConf.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""
								+ confName+ "\"  participantid=\""+ num + "\" entertone=\"false\"/></conference>");
				
				processingJoinResp(repJoin);
				
			}else if(mode.equalsIgnoreCase("coach")){
				
				System.out.println(mode);
				if(!isCoachExist){
					repJoin = connOMSConf
							.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""+ getName()
									+ "\"  participantid=\""+ num + "\" name=\""+ username+"\" confrole=\"coach\" /></conference>");
					isCoachExist = true;	
					
					processingJoinResp(repJoin);
					
				}else
					websock.send("coachExist");
				
			} else if(mode.equalsIgnoreCase("student")){
				
				System.out.println(mode);
				if(!isStudentExist){
					repJoin = connOMSConf
							.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""+ getName()
									+ "\" participantid=\""+ num +"\" name=\""+ username+"\" confrole=\"student\" /></conference>");				
					isStudentExist = true;
					
					processingJoinResp(repJoin);
					
				}else
					websock.send("studentExist");			
			}
			else if(mode.equalsIgnoreCase("speaker")){
				System.out.println(mode);
				repJoin = connOMSConf.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""
						+ getName()+ "\" participantid=\""+ num + "\" name=\""+ username+"\"/></conference>");
				
				processingJoinResp(repJoin);
				
			}else{
				System.out.println(mode);
				throw new OmsException("The mode you entered is unknown. The available mode are: speaker,"
						+ " mute, student or coach");
			}
		}
	}
	
	
	/**
	 * To notify all Browsers connected to OMS that a new conference was
	 * conference. Thus, the latter can join the conference
	 * 
	 * @param listOmsCall
	 *            An ArrayList of Browsers connected to OMS
	 */
	public void notification(List<OmsCall> listOmsCall) {

		List<OmsCall> newList = new ArrayList<OmsCall>();
		newList.addAll(listOmsCall);

		Iterator<OmsCall> ite = newList.iterator();
		int num;
		OmsCall call;

		while (ite.hasNext()) {
			call = ite.next();
			num = call.getPartNumberConf();
			if (num != 1)
				call.getWebSocket().send("confCreated");
		}
	}
	
	private void processingJoinResp(String repJoin) throws OmsException{
				
		if (repJoin.indexOf("OK") != -1) {

			Matcher mat1 = pat1.matcher(repJoin);

			if (mat1.find()) {
				String mediaOutput = "/" + mat1.group(1) + "";
				String respSynt = connOMSCall.getReponse("new s2 synt.synt host=127.0.0.1 port=7777");
				if (respSynt.indexOf("OK") != -1) {
					String respBind = connOMSCall.getReponse("mt1 setparam bind=s2");

					if (respBind.indexOf("OK") != -1) {
						String respCodec = connOMSCall.getReponse("s2 setparam ttscodec=a8k");

						if (respCodec.indexOf("OK") != -1) {
							String respSay = connOMSCall
									.getReponse("s2 say \"cat /opt/application/64poms/"
											+ "current/tmp"	+ mediaOutput + "\"");

							if (respSay.indexOf("OK") == -1)
								throw new OmsException("say cmd failed  : " + respSay);
						} else
							throw new OmsException("cmd s2 setparam ttscodec=a8k : " + respCodec);
					} else
						throw new OmsException("mt1 setparam bind=s2 : " + respBind);
				} else
					throw new OmsException(
							"new s2 synt.synt host=127.0.0.1 port=7777 "+ respSynt);
			} else
				throw new OmsException("mediaoutput :  NO MATCH " + repJoin);

			Matcher mat2 = pat2.matcher(repJoin);

			if (mat2.find()) {

				String mediaInput = "/" + mat2.group(1);
				String respEnreg = connOMSCall.getReponse("new e1 enreg");
				if (respEnreg.indexOf("OK") == -1)
					throw new OmsException(
							"cannot create a new recording ressource: "+ respEnreg);

				String startRec = connOMSCall
						.getReponse("e1 start /opt/application/64poms/current/tmp" + mediaInput + "");
				if (startRec.indexOf("OK") == -1)
					throw new OmsException("cannot record: " + startRec);

			} else
				throw new OmsException("mediaintput :  NO MATCH " + repJoin);

		} else if (repJoin.indexOf("406") != -1) {
			throw new OmsException("Error cannot join the conference : " + repJoin);
		} else if (repJoin.indexOf("411") != -1)
			throw new OmsException(
					"Delete files conf_1.rd and conf_1.wr at /opt/application/64poms/current/tmp");
		else if (repJoin.indexOf("408") != -1)
			throw new OmsException("Error cannot join the conference : " + repJoin);		
	}

	/**
	 * To remove a OMS call from the conference
	 * 
	 * @param omsCall the OmsCall to remove
	 * @throws OmsException
	 */
	public void delete(OmsCall omsCall) throws OmsException {
		
		confName = omsCall.getConfname();
			
		if(annuaireForConference.containsKey(confName)){
			
			listOmsCallInConf = annuaireForConference.get(confName);

			if(!listOmsCallInConf.isEmpty()){
					
				int num = omsCall.getPartNumberConf();
				connOMSCall = omsCall.getVipConnexion();
				hasCreatedConf = omsCall.getHasCreatedConf();
				
				String unJoinRep;
				Iterator<OmsCall> ite = listOmsCallInConf.iterator();
				int num2;
				while (ite.hasNext()) {
					num2 = ite.next().getPartNumberConf();
					if (num2 == num) {
						unJoinRep = getVipConnexion().getReponse(
								"<conference><unjoin conferenceid=\"" + getName()
										+ "\" requestid=\"" + num
										+ "\" participantid=\"" + num + "\"/></conference>");
						if (unJoinRep.indexOf("OK") == -1)
							throw new OmsException("unjoining failed : " + unJoinRep);

						connOMSCall.getReponse("delete s2");
						connOMSCall.getReponse("delete e1");
						ite.remove();
					}
				}

				if (listOmsCallInConf.size() == 0 && !destroyConf || hasCreatedConf) {
					
					destroyConf = true;
					System.out.println("Conference "+ getName() +" destroyed");	
					listOmsCallInConf.clear();
					
					destroy(omsCall);					
				}
			}			
		}	
	}

	/**
	 * To destroy a conference
	 * 
	 * @param omsCall
	 *            destroyer of the conference
	 * @throws OmsException
	 */
	public void destroy(OmsCall omsCall) throws OmsException {
		
		confName = omsCall.getConfname();		
		connOMSCall = omsCall.getVipConnexion();

		String repStatus = connOMSConf.getReponse("<conference><status requestid=\"req2\" conferenceid=\""
						+ getName() + "\" /></conference>");
		if (repStatus.indexOf("OK") != -1) {

			Matcher mat3 = pat3.matcher(repStatus);
			while (mat3.find()) {

				String participantId = "\"" + mat3.group(1) + "\"";
				String unjoinRep = connOMSConf
						.getReponse("<conference><unjoin requestid=\"req2\" conferenceid=\""
								+ getName() + "\"  participantid="
								+ participantId.toString() + "/></conference>");
				if (unjoinRep.indexOf("OK") == -1)
					throw new OmsException("cannot unjoin  participant : " 
									+ participantId + "because: " + unjoinRep);
			}

			String destroyConf = connOMSConf.getReponse("<conference><destroy requestid=\"req3\" conferenceid=\""
							+ getName() + "\" /></conference>");

			if (destroyConf.indexOf("OK") != -1) {
								
				String rep1 = connOMSCall.getReponse("wait evt=mt1.*");

				// if (rep1.indexOf("OK")!=-1){
				// connOMSCall.getReponse("mt1 shutup");
				// connOMSCall.getReponse("delete mt1");
				// connOMSCall.getReponse("delete e1");
				// connOMSCall.getReponse("delete s");
				// connOMSCall.getReponse("delete s2");
				// }
				
				annuaireForConference.remove(getName());				
				if(annuaireForConference.size() == 0){
					System.out.println(" The conference session is finished");
					System.exit(0); 
				}
				
			} else
				throw new OmsException("cannot destroy conference because: " + destroyConf);
		} else
			throw new OmsException("Conference does not exist : " + repStatus);

	}
	
	/**
	 * To start recording a conference
	 * @param conf conference name
	 * @throws OmsException
	 * @throws IOException
	 */
	
	public void startRecording(String conf) throws OmsException, IOException {

		confName = conf;		
		if(annuaireForConference.containsKey(confName)){
			
			listOmsCallInConf = annuaireForConference.get(getName());
			
			activate();
			t = new Thread(this);
			t.start();
		}	
	}

	private void deleteRecorder(String conf) throws OmsException, IOException {

		int num = 0;
		//String conf = getName();
		String unJoinrep = connOMSConf
				.getReponse("<conference><unjoin conferenceid=\"" + conf
						+ "\" requestid=\"" + num + "\" participantid=\"" + num
						+ "\"/></conference>");

		if (unJoinrep.indexOf("OK") == -1)
			throw new OmsException("unjoining failed : " + unJoinrep);

		omsCallRecord.getVipConnexion().close();
	}
	
	/**
	 * To stop recording a conference
	 * @param conf conference name
	 * @throws OmsException
	 * @throws IOException
	 */
	public void stopRecording(String conf) throws OmsException, IOException {

		confName = conf;		
		if(annuaireForConference.containsKey(confName)){
			
			listOmsCallInConf = annuaireForConference.get(confName);
			
			terminate();

			Iterator<OmsCall> ite = listOmsCallInConf.iterator();
			while (ite.hasNext()) {
				ite.next().getWebSocket().send("stopRecordConf");
			}

			deleteRecorder(confName);
		}	
	}

	
	/**
	 * To play the audio recorded file in a conference
	 * @param conf conference name
	 * @throws OmsException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	
	public void playRecording(String conf) throws OmsException, IOException,
			InterruptedException {

		//confName = omsCall.getConfname();		
		confName = conf;
		listOmsCallInConf = annuaireForConference.get(confName);
		
		Process p;
		int returnCode;
		String enregFileRaw = enregFile + ".raw";
		String enregFileWav = enregFile + ".wav";
		String enregFilea8k = enregFile + ".a8k";
		//String testAnimaux = "/opt/application/64poms/current/tmp/Animaux.a8k";

		File fWav = new File(enregFileWav);
		if (fWav.exists()) {
			fWav.delete();
			System.out.println(enregFileWav + " deleted");
			// fWav.createNewFile();
		}

		File fa8k = new File(enregFilea8k);
		if (fa8k.exists()) {
			fa8k.delete();
			System.out.println(enregFilea8k + " deleted");
		}

		String cmd1 = new String("sox -t al -r 8000 -b 8 -c 1 " + enregFileRaw
				+ " " + enregFileWav);
		String cmd2 = new String("sox -t wav " + enregFileWav
				+ " -t raw -r8000 -e a-law -b 8 -c 1 " + enregFilea8k);
		// sox -t wav CON1100528_G711A_sortante.wav -t raw -r8000 -e a-law -b 8
		// -c 1 toto.a8K
		p = Runtime.getRuntime().exec(cmd1);
		returnCode = p.waitFor();
		if (returnCode != 0)
			throw new OmsException("sox command failed " + returnCode);

		p = Runtime.getRuntime().exec(cmd2);
		returnCode = p.waitFor();
		if (returnCode != 0)
			throw new OmsException("sox command failed " + returnCode);

		randomGenerator = new Random();
		omsCallRecord = new OmsCall();

		int index = randomGenerator.nextInt(listOmsCallInConf.size());
		String[] hostPortVip = listOmsCallInConf.get(index).getHostPortVip();
		omsCallRecord.connect(hostPortVip[0], hostPortVip[1]);

		String respJoin;
		int num = 0;

		respJoin = connOMSConf.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""
						+ confName + "\"  participantid=\""
						+ num + "\" entertone=\"false\" exittone=\"false\"/></conference>");

		if (respJoin.indexOf("OK") != -1) {

			Matcher mat2 = pat2.matcher(respJoin);

			if (mat2.find()) {

				String mediaInput = "/" + mat2.group(1);
				String mediaInputPath = "/opt/application/64poms/current/tmp"+ mediaInput;
				InputStream inputStream = new FileInputStream(new File(enregFileRaw));
				OutputStream outputStream = new FileOutputStream(new File(mediaInputPath));

				buf = new byte[ARRAY_SIZE];
				int bytes_read;

				bytes_read = inputStream.read(buf, 0, 160);

				while (bytes_read != -1) {
					outputStream.write(buf, 0, bytes_read);
					Thread.sleep((long) 17);
					bytes_read = inputStream.read(buf, 0, 160);
				}

				outputStream.close();
				inputStream.close();
			} else
				throw new OmsException("mediaintput :  NO MATCH " + respJoin);

			Matcher mat1 = pat1.matcher(respJoin);
			if (mat1.find()) {

				String mediaOutput = "/" + mat1.group(1) + "";
				String mediaOutputPath = "/opt/application/64poms/current/tmp"+ mediaOutput;
				InputStream inputStream = new FileInputStream(new File(mediaOutputPath));

				inputStream.close();
				
			} else
				throw new OmsException("mediaoutput :  NO MATCH " + respJoin);

		} else if (respJoin.indexOf("406") != -1) {
			websock.send("Conference does not yet exist");
			throw new OmsException("Error cannot join the conference : "
					+ respJoin);
		} else if (respJoin.indexOf("411") != -1)
			throw new OmsException(
					"Delete files conf_1.rd and conf_1.wr at /opt/application/64poms/current/tmp");
		else if (respJoin.indexOf("408") != -1)
			throw new OmsException("Error cannot join the conference : "
					+ respJoin);

		deleteRecorder(confName);
	}

	/**
	 * To mute a OmsCall in the conference
	 * @param omsCall OmsCall to mute
	 * @throws OmsException
	 */
	public void mute(OmsCall omsCall) throws OmsException {

		confName = omsCall.getConfname();				
		int num = omsCall.getPartNumberConf();
		
		String muteRep = getVipConnexion().getReponse("<conference><mute requestid=\"req4\" conferenceid=\""+ 
					getName()+ "\" participantid=\""+ num+ "\"/></conference>");
		if (muteRep.indexOf("OK") == -1)
			throw new OmsException("Error: cannot mute participant :" + num + " reason:" + muteRep);
	}

	/**
	 * To unmute a OmsCall in the conference
	 * @param omsCall OmsCall to unmute
	 * @throws OmsException
	 */
	public void unmute(OmsCall omsCall) throws OmsException {

		confName = omsCall.getConfname();
		int num = omsCall.getPartNumberConf();
		
		String muteRep =  getVipConnexion()
				.getReponse("<conference><unmute requestid=\"req5\" conferenceid=\""
						+ getName()+ "\" participantid=\"" + num + "\"/></conference>");
		if (muteRep.indexOf("OK") == -1)
			throw new OmsException("Error: cannot unmute participant :" + num + " reason:" + muteRep);
	}
	
	/**
	 * To mute all participants in a conference
	 * @param conf conference name
	 * @throws OmsException
	 */
	public void muteAll(String conf) throws OmsException {
	
		String muteAllRep = getVipConnexion()
				.getReponse("<conference><muteall requestid=\"req6\" conferenceid=\""
						+ conf + "\"/></conference>");
		if (muteAllRep.indexOf("OK") == -1)
			throw new OmsException("Error: cannot mute all the paricipants");
	}

	/**
	 * To unmute all participants in a conference
	 * @param conf conference name
	 * @throws OmsException
	 */
	
	public void unmuteAll(String conf) throws OmsException {

		String muteAllRep = getVipConnexion()
				.getReponse("<conference><unmuteall requestid=\"req6\" conferenceid=\""
						+ conf + "\"/></conference>");
		if (muteAllRep.indexOf("OK") == -1)
			throw new OmsException("Error: cannot unmute all the paricipants");
	}

	/**
	 * To get the state of the conference
	 * 
	 * @param name
	 *            conference name
	 * @return true if the conference exists, no otherwise
	 * @throws OmsException
	 */
	public boolean status(String name) throws OmsException {

		boolean state;
		String rep = getVipConnexion()
				.getReponse("<conference><status requestid=\"req6\" conferenceid=\""
						+ name + "\"/></conference>");
		if (rep.indexOf("OK") != -1)
			state = true;
		else
			state = false;
		// throw new OmsException("Error: cannot get the status :" + rep);
		return state;
	}
	
	/**
	 * 
	 * @param omsCall
	 * @return
	 * @throws OmsException
	 */
	
	public boolean isClientJoined(OmsCall omsCall) { 

		  try {

		   String Statusrep = connOMSConf
		     .getReponse("<conference><status requestid=\"req2\" conferenceid=\""
		       + getName() + "\" /></conference>");
		   logger.info(Statusrep);
		   if (Statusrep.indexOf("id=\"" + omsCall.getPartNumberConf()) != -1) {
		    return true;
		   } else
		    return false;
		  } catch (OmsException e) {
		   // TODO Auto-generated catch block
		   e.printStackTrace();
		   return false;
		  }
		 }
	
	/**
	 * 
	 * @param conf conference name
	 * @param msg message to send
	 */
	void broadcastToConference(String msg, HashMap<WebSocket, OmsCall> calls) { 

		  Iterator<Entry<WebSocket, OmsCall>> entry = calls.entrySet().iterator();
		  while (entry.hasNext()) {
		   Entry<WebSocket, OmsCall> cc = entry.next();
		   logger.info(isClientJoined(cc.getValue()));
		   if (isClientJoined(cc.getValue())) {
		    logger.info(" m here");
		    cc.getKey().send(msg);
		   }
		  }
		 }


	
	
	/**
	 * To get informations from the conference server about the total number of
	 * ongoing conferences, of participants as well as the
	 * maximum number of participants allow in a conference
	 * @param omsCall omsCall who wants to get the conference info
	 * @param filePath path of log file where the informations will be written
	 * @throws OmsException
	 * @throws IOException
	 */
	
	public void infos(OmsCall omsCall, String filePath) throws OmsException, IOException {
	
		File file;
		FileOutputStream fop;
		
		String infosOnConferences = "";
		String name, rep;
		Matcher mat, mat4, mat5;

		if(omsCall != null)	
			websock = omsCall.getWebSocket();
		
		VipConnexion confVip = getVipConnexion();
		rep = confVip.getReponse("<conference><stats requestid=\"req\"/></conference>");

		if (rep.indexOf("OK") != -1) {
			mat = pat7.matcher(rep);
			mat4 = pat8.matcher(rep);
			mat5 = pat9.matcher(rep);
			
			if (mat5.find()) {
				rep = mat5.group();
				infosOnConferences = infosOnConferences + rep + "\n";
			}			
			if (mat.find()) {
				rep = mat.group();
				infosOnConferences = infosOnConferences + rep + "\n";
			}
			if (mat4.find()) {
				rep = mat4.group();
				infosOnConferences = infosOnConferences + rep + "\n";
			}

			rep = confVip
					.getReponse("<conference><list requestid=\"req\"/></conference>");
			if (rep.indexOf("OK") != -1) {

				Matcher mat1 = pat4.matcher(rep);
				Matcher mat2, mat3;
				
				if(!mat1.find()){
					
					infosOnConferences = "No informations about conferences are available at the conference"
							+ " server right now. Please, create a new conference first.\n";
					buf = new byte[ARRAY_SIZE];
					buf = infosOnConferences.getBytes();
					file = new File(filePath);
					if (!file.exists()) {
						file.createNewFile();
					}
					
					fop = new FileOutputStream(file);
					fop.write(buf);
					fop.flush();
					fop.close();
					
					if(websock != null){
						websock.send(buf);
					}				
				}else{
					do{
				//while (mat1.find()) {
					rep = mat1.group();
					//infosOnConferences = infosOnConferences + rep + "\n";
					mat2 = pat5.matcher(rep);
					rep = "\t" +rep;
					infosOnConferences = infosOnConferences + rep + "\n";

					if (mat2.find()) {
						name = mat2.group(1);
						rep = confVip.getReponse("<conference><status requestid=\"req6\" conferenceid=\""
										+ name + "\"/></conference>");
						mat3 = pat6.matcher(rep);
						while (mat3.find()) {
							rep = "\t\t" + mat3.group();
							infosOnConferences = infosOnConferences + rep + "\n";
						}
						
												
					} else
						logger.error("mat2 not find");
				}while (mat1.find());
					
					buf = new byte[ARRAY_SIZE];
					buf = infosOnConferences.getBytes();
					file = new File(filePath);
					if (!file.exists()) {
						file.createNewFile();
					}
					
					fop = new FileOutputStream(file);
					fop.write(buf);
					fop.flush();
					fop.close();
					
					if(websock != null){
						websock.send(buf);
					}					
				}
			} else
				throw new OmsException("Cannot get the list");
		} else
			throw new OmsException("Cannot get the stats");
	}

	/**
	 * To get the number of participant in the conference
	 * 
	 * @return total number of participants in the conference
	 */
	public int getParticipantsNumber(String conf) {
		
		confName = conf;		
		if(annuaireForConference.containsKey(confName)){
			
			listOmsCallInConf = annuaireForConference.get(getName());
			return listOmsCallInConf.size();
		}
		return 0;
			
	}
 
	/*public boolean checkConfExistence(String conf){
		
		if(annuaireForConference.containsKey(conf))
			return true;
		else return false;		
	}*/
	/**
	 * To get the connection to OMS for the conference
	 * 
	 * @return the connection to OMS
	 */
	public VipConnexion getVipConnexion() {
		return connOMSConf;
	}

	public void setName(String name){
		confName = name;
	}
	
	/**
	 * To get the conference id or name
	 * 
	 * @return conference's name
	 */
	public String getName() {
		return confName;
	}

	/**
	 * For launching the thread responsible to start recording the conference.
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub

		if (!listOmsCallInConf.isEmpty()) {

			randomGenerator = new Random();
			omsCallRecord = new OmsCall();

			int index = randomGenerator.nextInt(listOmsCallInConf.size());
			String[] hostPortVip = listOmsCallInConf.get(index)
					.getHostPortVip();
			try {
				omsCallRecord.connect(hostPortVip[0], hostPortVip[1]);
			} catch (OmsException | IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			String respJoin = null;
			int num = 0;

			try {
				respJoin = connOMSConf
						.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""+ getName()
								+ "\"  participantid=\"" + num
								+ "\" entertone=\"false\" exittone=\"false\"/></conference>");
			} catch (OmsException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			if (respJoin.indexOf("OK") != -1) {

				Matcher mat2 = pat2.matcher(respJoin);

				if (mat2.find()) {

					String mediaInput = "/" + mat2.group(1);
					String mediaInputPath = "/opt/application/64poms/current/tmp" + mediaInput;
					OutputStream outputStream;
					try {
						outputStream = new FileOutputStream(new File(mediaInputPath));
						String string = "Hello";
						buf = new byte[ARRAY_SIZE];
						buf = string.getBytes();
						outputStream.write(buf, 0, buf.length);
						outputStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				Matcher mat1 = pat1.matcher(respJoin);

				if (mat1.find()) {

					Iterator<OmsCall> ite = listOmsCallInConf.iterator();

					while (ite.hasNext()) {
						ite.next().getWebSocket().send("recordConf");
					}

					String mediaOutput = "/" + mat1.group(1) + "";
					String mediaOutputPath = "/opt/application/64poms/current/tmp" + mediaOutput;
					InputStream inputStream;
					try {
						inputStream = new FileInputStream(new File(
								mediaOutputPath));
						buf = new byte[ARRAY_SIZE];
						int bytes_read;

						String enregFileRaw = enregFile + ".raw";
						File fRaw = new File(enregFileRaw);
						if (fRaw.exists()) {
							fRaw.delete();
							System.out.println(enregFileRaw + " deleted");
							fRaw.createNewFile();
						}

						OutputStream outputStream = new FileOutputStream(fRaw);
						while (running) {

							bytes_read = inputStream.read(buf);
							// System.out.println(bytes_read);
							if (bytes_read == -1)
								break;
							outputStream.write(buf, 0, bytes_read);
						}

						inputStream.close();
						outputStream.close();
						logger.info("The .raw file is ready");

					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

				}
			}
		}
	}
	
	private void terminate() {
		running = false;
	}

	private void activate() {
		running = true;
	}
}


/*
 * public void play() throws OmsException{
 * 
 * //String enregFilea8k = enregFile + ".a8k"; String url =
 * "http://10.184.155.57:8080/docs/webRTC/doc/Animaux.a8k"; String
 * testAnimaux = "/opt/application/64poms/current/tmp/Animaux.wav";
 * VipConnexion confVip = getVipConnexion();
 * 
 * String subs =
 * confVip.getReponse("<conference><subscribe requestid=\"101\" conferenceid=\""
 * +
 * getName()+"\"><event type=\"playterminated\"/></subscribe></conference>"
 * );
 * 
 * logger.info(subs); String rep =
 * confVip.getReponse("<conference><play requestid=\"req5\" conferenceid=\""
 * + getName()+"\"><prompt url=\""+ testAnimaux
 * +"\"/></play></conference>");
 * 
 * if (rep.indexOf("OK")==-1) throw new
 * OmsException("Error: cannot play file to participants :" + rep); }
 */
