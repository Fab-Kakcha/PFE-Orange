/**
 * This Java's Class is about making conference in OMS.
 */

package com.orange.olps.api.sip;

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

public class OmsConference{

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
	//private String regexSemiColon = ":";

	private static Logger logger = Logger.getLogger(OmsConference.class);
	private VipConnexion connOMSConf = null;
	private VipConnexion connOMSCall = null;
	private WebSocket websock = null;
	private String confName = null;
	private List<OmsCallSip> listOmsCallInConf = new ArrayList<OmsCallSip>();

	private List<Integer> arrayList = new ArrayList<Integer>();
	private OmsCallSip omsCallRecord = null;
	private Random randomGenerator;
	private String enregFile = "/opt/application/64poms/current/tmp/enregFile";
	private boolean destroyConf = false;
	private Thread t, t1;
	private boolean running = true;
	//private boolean isCoachExist = false;
	//private boolean isStudentExist = false;
	private byte[] buf;
	private boolean hasCreatedConf = false;
	//private Annuaire annuaire;

	//On peut avoir plusieurs conférences, dans une même session
	//identifier chaque conférence
	//indentifier les participants de chaque conférence
	protected static HashMap<String, List<OmsCallSip>> annuaireForConference = 
			new HashMap<String, List<OmsCallSip>>(); 
	
	
	/**
	 * To initiate a connection to the confManager
	 * 
	 * @param hostVipConf confManager IP address
	 * @param portVipConf
	 *            confManager port for the conference
	 * @throws OmsException
	 * @throws IOException
	 */
	public OmsConference(String hostVipConf, String portVipConf)
			throws OmsException, IOException {
		
		connOMSConf = new VipConnexion(hostVipConf, portVipConf);
	}	
	
	/**
	 * To create a new conference with name given by argument param
	 * @param param the conference name to be created
	 * @throws OmsException
	 */
	
	public void create(OmsCallSip omsCall, String param) throws OmsException { //param = nom de la conference
		
		if(omsCall == null)
			throw new IllegalArgumentException("Argument OmsCall cannot be null");
		else if(param == null)
			throw new IllegalArgumentException("Argument String cannot be null");
		
		//boolean state = status(param);
		boolean state = checkExist(param);
		if (state) {
			logger.error("conference "+  param + "already exists");
		} else {
			
			String respCreation = connOMSConf
					.getReponse("<conference><create requestid=\"req1\" conferenceid=\""
							+ param + "\"/></conference>");

			if (respCreation.indexOf("OK") == -1)
				throw new OmsException("Error cannot create the conference: "+ respCreation);

			add(omsCall, param);
		}
	}
		
	/**
	 * To create a new conference with name given in argument conferenceParam
	 * @param omsCall OmsCall to create the conference
	 * @param conferenceParam contain all the optional parameters to be used to create a conference
	 * @throws OmsException
	 */
	public void create(OmsCallSip omsCall, ConferenceParameters conferenceParam) throws OmsException{
						
		if(omsCall == null)
			throw new IllegalArgumentException("Argument OmsCall cannot be null");
		
		String conferenceid = conferenceParam.getConferenceid();
		
		boolean state = checkExist(conferenceid);
		if (state) {
			logger.error("conference "+  conferenceid + " already exists");
		}else{
			
			int maxparticipant = conferenceParam.getMaxparticipant();
			int timeout = conferenceParam.getTimeout();
			int relaydtmf = conferenceParam.getRelaydtmf();
			String type = conferenceParam.getType();
			String activetone = conferenceParam.getActivetone();
			
			String respCreation = connOMSConf
					.getReponse("<conference><create requestid=\"req1\" conferenceid=\"" + conferenceid + 
							"\" maxparticipant=\""+maxparticipant+"\" timeout=\""+ timeout+"\" relaydtmf=\"" 
							+relaydtmf+"\" type=\""+type+"\" activatetone=\""+ activetone+"\"/></conference>");

			if (respCreation.indexOf("OK") == -1)
				throw new OmsException("Error cannot create the conference : " + respCreation);
			
			add(omsCall, conferenceParam);					
		}
	}
	
	/**
	 * To add an user in a already existing conference with name param
	 * @param omsCall
	 *            client to join the conference
	 * @param param conference name
	 * @throws OmsException
	 */
	public void add(OmsCallSip omsCall, String param) throws OmsException {
		
		if(omsCall == null)
			throw new IllegalArgumentException("Argument OmsCall cannot be null");
		else if(param == null)
			throw new IllegalArgumentException("Argument String cannot be null");
		
		String repJoin;	
		confName = param;
		
		boolean state = status(confName);
		if (!state) {			
			logger.info("conférence does not exist");
			
		} else {		
					
			if(!isClientJoined(omsCall)){
				
				connOMSCall = omsCall.getVipConnexion();
				int num = 1;
				Iterator<OmsCallSip> ite2;
				
				/*String repStatus = getVipConnexion()
						.getReponse("<conference><status requestid=\"req6\" conferenceid=\""
								+ confName + "\"/></conference>");				
				
				if (repStatus.indexOf("OK") != -1) {

					Matcher mat3 = pat3.matcher(repStatus);
					while (mat3.find()) {

						String participantId = mat3.group(1);						
						arrayList.add(Integer.parseInt(participantId));
					}
					
					if(!arrayList.isEmpty()){
						
						num = Collections.max(arrayList) + 1;
					}					
				}*/
				
				if (!annuaireForConference.isEmpty()) {

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
								
				String respSh = connOMSCall.getReponse("mt1 shutup");
				if (!respSh.equals("OK"))
					throw new OmsException("Cannot shutup mt1: " + respSh);
				
				repJoin = connOMSConf
						.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""+ getName()
								+ "\"  participantid=\""+ num + "\" /></conference>");
				
				processingJoinResp(repJoin);				
				omsCall.setConfName(confName);
				omsCall.setPartNumberConf(num);
				
				if (!annuaireForConference.containsKey(confName)) {

					List<OmsCallSip> listOmsCall = new ArrayList<OmsCallSip>();
					listOmsCall.add(omsCall);
					annuaireForConference.put(confName, listOmsCall);
				} else {

					listOmsCallInConf = annuaireForConference.get(confName);
					listOmsCallInConf.add(omsCall);
					annuaireForConference.put(confName, listOmsCallInConf);
				}
							
			}else				
				throw new OmsException("Cannot add user to conference " + getName() +
						", because omsCall has already joined that conference");						
		}
	}
	
	/**
	 * To add an user in a already existing conference with name given in conferenceParam
	 * @param omsCall client to join the conference
	 * @param conferenceParam contain all optional parameters to be used to join a conference
	 * @throws OmsException
	 */
	public void add(OmsCallSip omsCall, ConferenceParameters conferenceParam) throws OmsException{
		
		if(omsCall == null)
			throw new IllegalArgumentException("Argument OmsCall cannot be null");
				
		String conferenceid = conferenceParam.getConferenceid();		
		boolean state = status(conferenceid);
		
		if (!state) {			
			logger.info("conférence does not exist");
			
		}else{
			
			String entertone = conferenceParam.getEntertone();
			String exittone = conferenceParam.getExittone();
			
			String codec = conferenceParam.getCodec();
			String name = conferenceParam.getName();		
			String confrole = conferenceParam.getConfrole();
			
			if(!isClientJoined(omsCall)){
				
				connOMSCall = omsCall.getVipConnexion();
				int num = 1;
				Iterator<OmsCallSip> ite2;
				confName = conferenceid;
				
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
				
				String repJoin = connOMSConf.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""
						+ conferenceid+ "\" participantid=\""+ num + "\" entertone=\""+entertone+"\" exittone=\""
						+ exittone+"\" codec=\""+codec+"\" name=\""+ name+"\" confrole=\""+confrole+"\"/></conference>");
							
				processingJoinResp(repJoin);
				//omsCall.setConfName(confName);
				//omsCall.setUserName(name);
				omsCall.setPartNumberConf(num);
				
				if (!annuaireForConference.containsKey(confName)) {

					List<OmsCallSip> listOmsCall = new ArrayList<OmsCallSip>();
					listOmsCall.add(omsCall);
					annuaireForConference.put(confName, listOmsCall);
				} else {

					listOmsCallInConf = annuaireForConference.get(confName);
					listOmsCallInConf.add(omsCall);
					annuaireForConference.put(confName, listOmsCallInConf);
				}
				
			}else
				throw new OmsException("Cannot add user to conference " + getName() +
						", because omsCall has already joined that conference");						
		}					
	}

	/**
	 * To remove an user from an existing conference
	 * @param omsCall user to leave the conference
	 * @throws OmsException
	 */
	public void delete(OmsCallSip omsCall) throws OmsException {

		if (omsCall == null)
			throw new IllegalArgumentException("Argument cannot be null");

		confName = omsCall.getConfname();
		connOMSCall = omsCall.getVipConnexion();
		boolean bool = status(confName);		
		
		if (bool) {

			// hasCreatedConf = omsCall.getHasCreatedConf();
			
			listOmsCallInConf = annuaireForConference.get(confName);
			if (listOmsCallInConf.isEmpty())
				throw new OmsException("Cannot delete the participant. Nobody in the conference");
			
			Iterator<OmsCallSip> ite = listOmsCallInConf.iterator();			
			
			int num = omsCall.getPartNumberConf();
			connOMSCall = omsCall.getVipConnexion();
			
			int num2;
			String unJoinRep;
			OmsCallSip call;
			
			while (ite.hasNext()) {
				
				call = ite.next();
				num2 = call.getPartNumberConf();
				
				if (num2 == num) {
					
					unJoinRep = getVipConnexion().getReponse(
							"<conference><unjoin conferenceid=\"" + getName()
									+ "\" requestid=\"" + num + "\" participantid=\""
									+ num + "\"/></conference>");

					if (unJoinRep.indexOf("OK") == -1)
						throw new OmsException("unjoining failed : " + unJoinRep);

					connOMSCall.getReponse("delete s2");
					connOMSCall.getReponse("delete e1");
					
					ite.remove();				
				}							
			}
			
			if(listOmsCallInConf.isEmpty())
				destroy(omsCall);						
			
			// connOMSCall.getReponse("wait evt=mt1.*");			
			/*arrayList.clear();
			String repStatus = getVipConnexion()
					.getReponse("<conference><status requestid=\"req6\" conferenceid=\""
							+ confName + "\"/></conference>");				
			
			if (repStatus.indexOf("OK") != -1) {

				Matcher mat3 = pat3.matcher(repStatus);
				while (mat3.find()) {

					String participantId = mat3.group(1);						
					arrayList.add(Integer.parseInt(participantId));
				}
				
				if(arrayList.isEmpty())					
					destroy(omsCall);
				else 
					System.exit(0);
			}	*/		
		}					
	}
	
	/**
	 * To destroy an existing conference 
	 * @param omsCall destroyer of the conference
	 * @throws OmsException
	 */
	public void destroy(OmsCallSip omsCall) throws OmsException {
		
		if(omsCall == null)
			throw new IllegalArgumentException("The argument cannot be null");			
		
		confName = omsCall.getConfname();
		String repStatus = connOMSConf.getReponse("<conference><status requestid=\"re2\" conferenceid=\""
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
						
				annuaireForConference.remove(getName());
				if(annuaireForConference.size() == 0){
					logger.info(" The conference session is finished");
					//System.exit(0); 
				}

				//String rep1 = connOMSCall.getReponse("wait evt=mt1.*");

				// if (rep1.indexOf("OK")!=-1){
				// connOMSCall.getReponse("mt1 shutup");
				// connOMSCall.getReponse("delete mt1");
				// connOMSCall.getReponse("delete e1");
				// connOMSCall.getReponse("delete s");
				// connOMSCall.getReponse("delete s2");
				// }
				
			} else
				throw new OmsException("cannot destroy conference because: " + destroyConf);
		} else
			throw new OmsException("Conference does not exist : " + repStatus);
	}
	
		
	/**
	 * To start recording participants in an ongoing conference
	 * @param omsCallSip user to record the conference
	 * @throws OmsException
	 * @throws IOException
	 */
	
	public void startRecording(final String conf) throws OmsException, IOException {

		if (conf == null)
			throw new IllegalArgumentException("The argument cannot be null");
		
		confName = conf;
		boolean bool = annuaireForConference.containsKey(confName);
		if (!bool)
			throw new OmsException("The conference name is unknwwon");

		listOmsCallInConf = annuaireForConference.get(getName());
		if(listOmsCallInConf.isEmpty())
			throw new OmsException("Cannot start recording. Nobody in the conference");

		activate();

		t = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				//confName = omsCallSip.getConfname();
				try {
					
					//boolean bool = status(confName);
					//if (bool) {
					randomGenerator = new Random();
					omsCallRecord = new OmsCallSip();
					int index = randomGenerator.nextInt(listOmsCallInConf.size());
					
					String[] hostPortVip = listOmsCallInConf.get(index).getHostPortVip();
							//omsCallSip.getHostPortVip();

					omsCallRecord.connect(hostPortVip[0], hostPortVip[1]);
					String respJoin = null;
					int num = 0;

					respJoin = connOMSConf
								.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""
										+ getName()
										+ "\"  participantid=\"" + num
										+ "\" entertone=\"false\" exittone=\"false\"/></conference>");

					if (respJoin.indexOf("OK") != -1) {

							Matcher mat2 = pat2.matcher(respJoin);

							if (mat2.find()) {

								String mediaInput = "/" + mat2.group(1);
								String mediaInputPath = "/opt/application/64poms/current/tmp"
										+ mediaInput;
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

								String mediaOutput = "/" + mat1.group(1) + "";
								String mediaOutputPath = "/opt/application/64poms/current/tmp"
										+ mediaOutput;
								InputStream inputStream;

								inputStream = new FileInputStream(new File(mediaOutputPath));
								buf = new byte[ARRAY_SIZE];
								int bytes_read;

								String enregFileRaw = enregFile + ".raw";
								File fRaw = new File(enregFileRaw);
								if (fRaw.exists()) {
									fRaw.delete();
									System.out.println(enregFileRaw+ " deleted");
									fRaw.createNewFile();
								}

								OutputStream outputStream = new FileOutputStream(
										fRaw);
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
							}
						}

						deleteRecorder(confName);

					//}else
						//throw new OmsException("Cannot start recording: unknow conference name: " + confName);

				} catch (OmsException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		t.start();
	}

	/**
	 * To stop playing a file, but this method is likely to be removed and replaced by ConfManager stopPlay
	 * method.
	 */
	public void stopPlay(){
		
		terminate();
	}
	
	/**
	 * 
	 * @param conf
	 * @throws OmsException
	 */
	public void stopplay(String conf) throws OmsException{
		
		if(conf == null)
			throw new IllegalArgumentException("Argument cannot be null");
		
		boolean bool = annuaireForConference.containsKey(conf);
		if (!bool)
			throw new OmsException("The conference name "+ conf +" is unknown");
		
		String rep = getVipConnexion()
				.getReponse("<conference><stopplay requestid=\"req5\" conferenceid=\"" + conf
						+ "\"/></conference>");
		
		if(rep.indexOf("OK") == -1)
			throw new OmsException("cannot stopplay");			
	}
	
	/**
	 * ConfManager stopPlay() method, this method i not working yet, because the confManager's 
	 * play method is not working
	 * @param conf
	 * @param conferenceParam
	 * @throws OmsException
	 */
	public void stopplay(ConferenceParameters conferenceParam) throws OmsException{
		
		String conf = conferenceParam.getConferenceid();
		String participantid = conferenceParam.getParticipantid();
		String rep;
		
		if(participantid == null)
			stopplay(conf);
		
		else{			
			rep = getVipConnexion()
					.getReponse("<conference><stopplay requestid=\"req5\" conferenceid=\""+ conf
							+ " participantid=\""+participantid+"\"/></conference>");
			
			if(rep.indexOf("OK") == -1)
				throw new OmsException("cannot stopplay");
		}		
	}
	
	/**
	 * To subscribe to an event
	 * @param conf conference's name
	 * @param type event's name to subscribe
	 * @throws OmsException
	 */
	public void subscribe(String conf, String type) throws OmsException{
		
		String subs = getVipConnexion()
				.getReponse("<conference><subscribe requestid=\"101\" conferenceid=\""
						+ conf + "\"><event type=\""+type+"\"/></subscribe></conference>");
		if(subs.indexOf("OK") == -1)
			throw new OmsException("cannot subscribe");
	}
	
	/**
	 * To unsubscribe to an event
	 * @param conf conference name
	 * @param type event name to unsubscribe
	 * @throws OmsException
	 */
	public void unsubscribe(String conf, String type) throws OmsException{
		
		String unsubs = getVipConnexion()
				.getReponse("<conference><unsubscribe requestid=\"101\" conferenceid=\""
						+ conf + "\"><event type=\""+type+"\"/></unsubscribe></conference>");
		
		if(unsubs.indexOf("OK") == -1)
			throw new OmsException("cannot subscribe");		
	}
	
	/**
	 * 
	 * @return
	 * @throws OmsException
	 */
	public String list() throws OmsException{
		
		String rep = getVipConnexion()
				.getReponse("<conference><list requestid=\"req\"/></conference>");
		
		if(rep.indexOf("OK") == -1)
			throw new OmsException("cannot list");
		
		return rep;
	}
	
	/**
	 * To get the entire conference's status, or just a specific participant status
	 * @param conf conference's name
	 * @param conferenceParam contain optional parameters to provide for the status request
	 * @return the status in XML format
	 * @throws OmsException
	 */
	public String status(ConferenceParameters conferenceParam) throws OmsException{
		
		String conf = conferenceParam.getConferenceid();
		String participantid = conferenceParam.getParticipantid();
		String rep;
		
		if(participantid == null){
			
			rep = getVipConnexion()
					.getReponse("<conference><status requestid=\"req6\" conferenceid=\""+ conf 
							+ "\"/></conference>");
			if (rep.indexOf("OK") == -1)
				throw new OmsException("cannot get the status" + rep);
		}else{
			
			rep = getVipConnexion()
					.getReponse("<conference><status requestid=\"req6\" conferenceid=\""+ conf 
							+ "\" participantid=\""+participantid+"\"/></conference>");
			
			if (rep.indexOf("OK") == -1)
				throw new OmsException("cannot get the status" + rep);
		}
		
		return rep;
	}
	
	/**
	 * To get the stats from the conference's server
	 * @return
	 * @throws OmsException
	 */
	public String stats() throws OmsException{
		
		String rep = getVipConnexion().getReponse("<conference><stats requestid=\"req\"/></conference>");
		
		if (rep.indexOf("OK") == -1)
			throw new OmsException("cannot get the stats" + rep);
		
		return rep;
	}
	
	/**
	 * Set the usermode, either admin or user.
	 * @param conferenceParam contains the optionals parameters to provide for the set request
	 * @throws OmsException
	 */
	public void set(ConferenceParameters conferenceParam) throws OmsException{
		
		String user = conferenceParam.getUser();
		String rep = getVipConnexion().getReponse("<conference><set requestid=\"req\" user=\""+user+"\"/></conference>");
	
		if (rep.indexOf("OK") == -1)
			throw new OmsException("cannot edit the session parameters: " + rep);
	}
	
	/**
	 * To stop playing a audio file to a participant in an existing conference
	 * @param call the user
	 * @throws OmsException 
	 */
	public void stopplay(OmsCallSip call) throws OmsException{
		
		if(call == null)
			throw new IllegalArgumentException("Argument cannot be null");
		
		if(isClientJoined(call)){
			
			String rep = getVipConnexion()
					.getReponse("<conference><stopplay requestid=\"req5\" conferenceid=\"" + getName()
							+ "\" participantid=\"" +call.getPartNumberConf()+"\"/></conference>");		
		}else
			throw new OmsException("Cannot stop playing file, because participant is not in a conference");		
	}
	
	/**
	 * To stop recording participants of an existing conference
	 * @param conf conference name
	 * @throws OmsException
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void stopRecording(String conf) throws OmsException, IOException, InterruptedException {

		if (conf == null)
			throw new IllegalArgumentException("The argument cannot be null");

		boolean bool = status(conf);
		if(bool){
			
			terminate();
			
			Process p;
			int returnCode;
			String enregFileRaw = enregFile + ".raw";
			String enregFileWav = enregFile + ".wav";
			String enregFilea8k = enregFile + ".a8k";
			
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
		}
		else 
			throw new OmsException("Cannot stop recording. Unknown conference name: " + conf);						
	}

	
	/**
	 * To play the audio file recorded in an existing conference
	 * @param sipCall the user to play the file
	 * @throws OmsException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	
	public void playRecording(OmsCallSip sipCall) throws OmsException, IOException,
			InterruptedException {

		if(sipCall == null)
			throw new IllegalArgumentException("The argument cannot be null");

		String enregFileRaw = enregFile + ".raw";

		myPlay(sipCall.getConfname(), enregFileRaw);
	}

	
	/**
	 * To play a file to all participants into a conference, and this mehod is not working yet, but is going
	 * to work pretty soon.
	 * @param conferenceName the conference's name
	 * @param filePath path towards the file to play in the conference
	 * @throws OmsException
	 */
	public void play(String conferenceName, String filePath) throws OmsException {
		
		if(conferenceName == null)
			throw new IllegalArgumentException("First String argument cannot be null");
		else if(filePath == null)
			throw new IllegalArgumentException("Second String argument cannot be null");
		
		if(!status(conferenceName))
			throw new OmsException("The conference with name "+conferenceName+" doesn't exist");
		
		filePath = "/opt/application/64poms/current/tmp/Animaux.wav";

		String rep = getVipConnexion()
				.getReponse("<conference><play requestid=\"req5\" conferenceid=\"" + getName()
						+ "\"><prompt url=\"" + filePath + "\"/></play></conference>");

		if (rep.indexOf("OK") == -1)
			throw new OmsException("Error: cannot play file to participants :"+ rep);
	}
	 
	/**
	 * To play an audio file to all participants in the conference, or to a particular participant
	 * @param conf conference's name
	 * @param filePath path of file to play
	 * @param conferenceParam optional parameters useful for the play command 
	 * @throws OmsException
	 */
	public void play(String filePath, ConferenceParameters conferenceParam) throws OmsException{
		
		if(filePath == null)
			throw new IllegalArgumentException("Argument String cannot be null");
		
		String conf = conferenceParam.getConferenceid();
		
		String participantid = conferenceParam.getParticipantid();
		Boolean mixplay = conferenceParam.isMixplay();
		int priority = conferenceParam.getPriority();
		String repeat = conferenceParam.getRepeat();
		String rep;
		
		if(Integer.parseInt(repeat) <= 0)
			throw new OmsException("repeat must greater than 0");
		else if(priority < 0 || priority > 1)
			throw new OmsException("priority must be either 1 or 2");
		else if(participantid == null){
			
			rep = getVipConnexion()
					.getReponse("<conference><play requestid=\"req5\" conferenceid=\"" + conf +
							"\"><prompt url=\"" + filePath + "\" mixplay=\""+mixplay +"\" priority=\""
							+priority+"\" repeat=\"" +repeat +
							"\"/></play></conference>");
		}else{ 
						
			rep = getVipConnexion()
					.getReponse("<conference><play requestid=\"req5\" conferenceid=\"" + conf + " participantid=\""
			+ participantid+ "\"><prompt url=\"" + filePath + "\" mixplay=\""+mixplay +"\" priority=\""+priority
			+"\" repeat=\"" +repeat +"\"/></play></conference>");
		}
		
		if (rep.indexOf("OK") == -1)
			throw new OmsException("Error: cannot play file: "+ rep);
	}
	
	
	/**
	 * For playing audio file in a conference, this method was developed because the initial confManager
	 * method for playing file is still not working. So this method is to be remove.
	 * @param conferenceName conference's name
	 * @param filePath path of the file to play
	 * @throws OmsException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void myPlay(final String conferenceName, final String filePath)
			throws OmsException, IOException, InterruptedException {

		if (conferenceName == null)
			throw new IllegalArgumentException(
					"Argument OmsCallSip cannot be null");
		else if (filePath == null)
			throw new IllegalArgumentException("Argument String cannot be null");
		
		if(!status(conferenceName))
			throw new OmsException("The conference's name " + conferenceName +" is unknown");

		listOmsCallInConf = annuaireForConference.get(conferenceName);
		if(listOmsCallInConf.isEmpty())
			throw new OmsException("Cannot start recording. Nobody in the conference");
		
		activate();

		t1 = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				//String conferenceName = omsCallSip.getConfname();
				try {
					boolean bool = status(confName);

					//if (bool) {

						confName = conferenceName;
						randomGenerator = new Random();
						omsCallRecord = new OmsCallSip();

						int index = randomGenerator.nextInt(listOmsCallInConf.size());
						String[] hostPortVip = listOmsCallInConf.get(index).getHostPortVip();
						
						//$String[] hostPortVip = omsCallSip.getHostPortVip();
						omsCallRecord.connect(hostPortVip[0], hostPortVip[1]);

						String respJoin;
						int num = 0;

						respJoin = connOMSConf
								.getReponse("<conference><join requestid=\"req1\" conferenceid=\""
										+ confName+ "\"  participantid=\""
										+ num + "\" entertone=\"false\" exittone=\"false\"/></conference>");

						if (respJoin.indexOf("OK") != -1) {

							Matcher mat2 = pat2.matcher(respJoin);

							if (mat2.find()) {

								String mediaInput = "/" + mat2.group(1);
								String mediaInputPath = "/opt/application/64poms/current/tmp" + mediaInput;
								InputStream inputStream;

								inputStream = new FileInputStream(filePath);
								OutputStream outputStream;
								outputStream = new FileOutputStream(new File(mediaInputPath));

								buf = new byte[ARRAY_SIZE];
								int bytes_read;

								bytes_read = inputStream.read(buf, 0, 160);

								while (running) {
									if (bytes_read == -1)
										break;
									// System.out.println(bytes_read);
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
								throw new OmsException(
										"mediaoutput :  NO MATCH " + respJoin);

						} else if (respJoin.indexOf("406") != -1) {
							websock.send("Conference does not yet exist");
							throw new OmsException("Error cannot join the conference : "+ respJoin);
						} else if (respJoin.indexOf("411") != -1)
							throw new OmsException(
									"Delete files conf_*.rd and conf_*.wr at /opt/application/64poms/current/tmp");
						else if (respJoin.indexOf("408") != -1)
							throw new OmsException("Error cannot join the conference : "+ respJoin);

						deleteRecorder(confName);
					//}

				} catch (OmsException | IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		t1.start();
	}
	
	/**
	 * To mute an user in a conference
	 * @param omsCall user to mute
	 * @throws OmsException
	 */
	public void mute(OmsCallSip omsCall) throws OmsException {

		if(omsCall == null)
			throw new IllegalArgumentException("The argument cannot be null");
		
		confName = omsCall.getConfname();
		//confName = "conf1";
		if(!status(confName))
			throw new OmsException("The name of conference for that client is unknown");
		
		int num = omsCall.getPartNumberConf();
		
		String muteRep = getVipConnexion().getReponse("<conference><mute requestid=\"req4\" conferenceid=\""+ 
					getName()+ "\" participantid=\""+ num+ "\"/></conference>");
		if (muteRep.indexOf("OK") == -1)
			throw new OmsException("Error: cannot mute participant " + num + ". Reason: " + muteRep);
	}

	/**
	 * To unmute an user in a conference
	 * @param omsCall user to unmute
	 * @throws OmsException
	 */
	public void unmute(OmsCallSip omsCall) throws OmsException {

		if(omsCall == null)
			throw new IllegalArgumentException("The argument cannot be null");
		
		confName = omsCall.getConfname();
		int num = omsCall.getPartNumberConf();
		
		if(!status(confName))
			throw new OmsException("The name of conference for that client is unknown");
		
		String muteRep =  getVipConnexion()
				.getReponse("<conference><unmute requestid=\"req5\" conferenceid=\""
						+ getName()+ "\" participantid=\"" + num + "\"/></conference>");
		if (muteRep.indexOf("OK") == -1)
			throw new OmsException("Error: cannot unmute participant :" + num + ". Reason:" + muteRep);
	}
	
	/**
	 * To mute all participants of a conference
	 * @param conf conference name
	 * @throws OmsException
	 */
	public void muteAll(String conf) throws OmsException {
	
		if(conf == null)
			throw new IllegalArgumentException("The argument cannot be null");
		
		boolean bool = annuaireForConference.containsKey(conf);
		if (!bool)
			throw new OmsException("The conference name " +conf +" is unknown");
		
		listOmsCallInConf = annuaireForConference.get(conf);
		if(listOmsCallInConf.isEmpty())
			throw new OmsException("Cannot cannot muteAll. Nobody in the conference");
		
		String muteAllRep = getVipConnexion()
				.getReponse("<conference><muteall requestid=\"req6\" conferenceid=\""
						+ conf + "\"/></conference>");
		
		if (muteAllRep.indexOf("OK") == -1)
			throw new OmsException("Error: cannot mute all the paricipants. Reason: " + muteAllRep);		
	}
	
	/**
	 * To mute all participants of a conference
	 * @param conf conference name
	 * @param conferenceParam optional parameters useful for the muteall command
	 * @throws OmsException
	 */
	public void muteAll(ConferenceParameters conferenceParam) throws OmsException{
		
		String conf = conferenceParam.getConferenceid();
		String exceptlist = conferenceParam.getExceptlist();
		
		if(exceptlist == null)
			throw new OmsException("You should provide a list of except participants in the form"
					+ " particpantsid1;paricipantid2;...;participantidN");
		
		String muteAllRep = getVipConnexion()
				.getReponse("<conference><muteall requestid=\"req6\" conferenceid=\""+ conf 
						+ "exceptlist=\""+exceptlist+"\"/></conference>");
		
		if (muteAllRep.indexOf("OK") == -1)
			throw new OmsException("Error with function muteAll. Reason: " + muteAllRep);		
	}
	
	/**
	 * To unmute all participants of a conference
	 * @param conf conference name
	 * @throws OmsException
	 */
	public void unmuteAll(String conf) throws OmsException {

		if(conf == null)
			throw new IllegalArgumentException("The argument cannot be null");
		
		String muteAllRep = getVipConnexion()
				.getReponse("<conference><unmuteall requestid=\"req6\" conferenceid=\""
						+ conf + "\"/></conference>");
		if (muteAllRep.indexOf("OK") == -1)
			throw new OmsException("Error: cannot unmute all the paricipants. Reason: " + muteAllRep);
	}

	/**
	 * To unmute all participants in a conference
	 * @param conf conference name
	 * @param conferenceParam optional parameters to provide for unmute command
	 * @throws OmsException
	 */
	public void unmuteAll(ConferenceParameters conferenceParam) throws OmsException{
		
		String conf = conferenceParam.getConferenceid();
		boolean bool = annuaireForConference.containsKey(conf);
		if (!bool)
			throw new OmsException("The conference name " + conf + " is unknown");
		
		
		String exceptlist = conferenceParam.getExceptlist();
		if(exceptlist == null)
			throw new OmsException("You should provide a list of except participants in the form"
					+ " particpantsid1;paricipantid2;...;participantidN");
		
		String muteAllRep = getVipConnexion()
				.getReponse("<conference><unmuteall requestid=\"req6\" conferenceid=\""+ conf 
						+ "exceptlist=\""+exceptlist+"\"/></conference>");
		
		if (muteAllRep.indexOf("OK") == -1)
			throw new OmsException("Error with function unmuteAll. Reason: " + muteAllRep);
	}
	
	/**
	 * To activate the entry tone of a participant in a conference
	 * @param conf conference name
	 * @throws OmsException
	 */
	public void activatetone(String conf) throws OmsException{
		
		if(conf == null)
			throw new IllegalArgumentException("The argument cannot be null");
		
		if(status(conf)){
			
			String rep = getVipConnexion()
					.getReponse("<conference><activatetone requestid=\"req6\" conferenceid=\""+ conf + "\"/></conference>");
			
			if (rep.indexOf("OK") != -1)
				throw new OmsException("cannot activate tone for participants");	
		}
	}
	
	/**
	 * To deactivate the entry tone of a participant in a conference
	 * @param conf conference name
	 * @throws OmsException
	 */
	public void deactivatetone(String conf) throws OmsException{
		
		if(conf == null)
			throw new IllegalArgumentException("The argument cannot be null");
		
		if(status(conf)){
			
			String rep = getVipConnexion()
					.getReponse("<conference><deactivatetone requestid=\"req6\" conferenceid=\""+ conf + "\"/></conference>");
			
			if (rep.indexOf("OK") != -1)
				throw new OmsException("cannot activate tone for participants");
		}				
	}
	
	/**
	 * To get a conference status
	 * @param name conference name
	 * @return true if the conference exists, no otherwise
	 * @throws OmsException
	 */
	public boolean status(String name) throws OmsException {

		if(name == null)
			throw new IllegalArgumentException("The argument cannot be null");
		
		boolean state;
		String rep = getVipConnexion()
				.getReponse("<conference><status requestid=\"req6\" conferenceid=\""+ name + "\"/></conference>");
		
		if (rep.indexOf("OK") != -1)
			state = true;
		else
			state = false;
		return state;
	}
	
	
	/**
	 * check whether or not an user has already joined a conference
	 * @param omsCall user to check if he joined a conference
	 * @return true if the user has already joined conference, and false otherwise
	 * @throws OmsException 
	 */
	public boolean isClientJoined(OmsCallSip omsCall) throws OmsException  { 
		
		if(omsCall == null)
			throw new IllegalArgumentException("The argument cannot be null");
					
		/*String nameConf = omsCall.getConfname();

		String rep = connOMSConf.getReponse("<conference><status requestid=\"req2\" "
				+ "conferenceid=\""+nameConf+"\" /></conference>");
		
		int num = omsCall.getPartNumberConf();
		if(rep.indexOf("id=\""+num)!=-1)
			return true;
		else 
			return false;*/
		
		String nameConf = omsCall.getConfname();
		if(!checkExist(nameConf))
			return false;
		else{
			
			OmsCallSip call;
			int numPart = omsCall.getPartNumberConf();
			int numPart2;
			listOmsCallInConf = annuaireForConference.get(nameConf);
			Iterator<OmsCallSip> ite2 = listOmsCallInConf.iterator();
			
			while(ite2.hasNext()){
				
				call = ite2.next();
				numPart2 = call.getPartNumberConf();
				if(numPart == numPart2)
					return true;
			}
			
			return false;	
		}
	}
					
	/**
	 * 
	 * @param conf conference name
	 * @param msg message to send
	 * @throws OmsException 
	 */
	void broadcastToConference(String msg, HashMap<WebSocket, OmsCallSip> calls) throws OmsException { 

		  Iterator<Entry<WebSocket, OmsCallSip>> entry = calls.entrySet().iterator();
		  
		  while (entry.hasNext()) {
			  
		   Entry<WebSocket, OmsCallSip> cc = entry.next();	   
		   if (isClientJoined(cc.getValue())) {
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
	
	public void infos(String filePath) throws OmsException, IOException {
	
		//if(omsCall == null)
			//throw new IllegalArgumentException("Argument OmsCall cannot be null");
		
		if(filePath == null)
			throw new IllegalArgumentException("Argument String cannot be null");
			
		File file;
		FileOutputStream fop;
		
		String infosOnConferences = "";
		String name, rep;
		Matcher mat, mat4, mat5;
		
		rep = getVipConnexion().getReponse("<conference><stats requestid=\"req\"/></conference>");

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

			rep = getVipConnexion()
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
						rep = getVipConnexion()
								.getReponse("<conference><status requestid=\"req6\" conferenceid=\""
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
				}
			} else
				throw new OmsException("Cannot get the list");
		} else
			throw new OmsException("Cannot get the stats");
	}
 
	/**
	 * To get the conference id or name
	 * 
	 * @return conference name
	 */
	public String getName() {
		return confName;
	}	
	
	/**
	 * Return the list of all partcipants in a conference
	 * @param conf conference name
	 * @return list of participants of a conference
	 * @throws OmsException
	 */
	public List<OmsCallSip> getListOmsCallInConf(String conf) throws OmsException{
		
		if(conf == null)
			throw new IllegalArgumentException("Argument cannot be null");
		
		boolean bool = annuaireForConference.containsKey(conf);
		if (!bool)
			throw new OmsException("The conference name " +conf +" is unknown");
		
		listOmsCallInConf = annuaireForConference.get(conf);		
		return listOmsCallInConf;
	}
	
	
	/**
	 * To get the connection to OMS for the conference
	 * 
	 * @return the connection to OMS
	 */
	protected VipConnexion getVipConnexion() {
		return connOMSConf;
	}

	protected void setName(String name){
		confName = name;
	}

	private void processingJoinResp(String repJoin) throws OmsException{
		
		if (repJoin.indexOf("OK") != -1) {

			Matcher mat1 = pat1.matcher(repJoin);

			if (mat1.find()) {
				String mediaOutput = "/" + mat1.group(1) + "";
				String respSynt = connOMSCall.getReponse("new s2 synt.synt host=127.0.0.1 port=7777");
				//String respSynt = connOMSCall.getReponse("new s2 synt.synt");
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
	
	private boolean checkExist(String conf){
		
		if(annuaireForConference.containsKey(conf))
			return true;
		else 
			return false;
	}
	
	
	private void deleteRecorder(String conf) throws OmsException, IOException {

		if(conf == null)
			throw new IllegalArgumentException("The argument cannot be null");
		
		int num = 0;
		String unJoinrep = connOMSConf
				.getReponse("<conference><unjoin conferenceid=\"" + conf
						+ "\" requestid=\"" + num + "\" participantid=\"" + num
						+ "\"/></conference>");

		if (unJoinrep.indexOf("OK") == -1)
			throw new OmsException("unjoining failed : " + unJoinrep);

		omsCallRecord.getVipConnexion().close();
	}
	
	
	private void terminate() {
		running = false;
	}

	private void activate() {
		running = true;
	}
}
