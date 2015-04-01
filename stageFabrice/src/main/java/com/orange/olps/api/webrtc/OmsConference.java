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


public class OmsConference implements Runnable {

	private final static int ARRAY_SIZE = 1024*1024;
	
	private static Pattern pat1 = Pattern.compile("mediaoutput=\"([^ \t\"]+)");
	private static Pattern pat2 = Pattern.compile("mediainput=\"([^ \t\"]+)");
	private static Pattern pat3 = Pattern
			.compile("participant id=\"([^ \t\"]+)");

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
	private boolean isStopRecording = false;
	private Thread t;
	private boolean running = true;
	private byte[] buf;

	
	private void terminate(){
		running = false;
	}
	
	private void activate(){
		running = true;
	}
	
	/**
	 * To initiate a connection with OMS to do a conference
	 * @param hostVipConf OMS's IP address
	 * @param portVipConf OMS's listening port for the conference
	 * @throws OmsException
	 * @throws IOException
	 */
	public OmsConference(String hostVipConf, String portVipConf)
			throws OmsException, IOException {

		///confName = name;
		connOMSConf = new VipConnexion(hostVipConf, portVipConf);
		/*
		 * String respCreation = connOMSConf.getReponse(
		 * "<conference> <create requestid=\"req1\" conferenceid=\"" + confName
		 * + "\" /></conference>" );
		 * 
		 * if (respCreation.indexOf("OK") == -1) throw new
		 * OmsException("Error cannot create the conference : "+ respCreation);
		 */
	}
	
	/**
	 * To create the conference 
	 * @param name conference's name
	 * @throws OmsException
	 */
	
	public void create(String name) throws OmsException {

		confName = name;
		String respCreation = connOMSConf
				.getReponse("<conference> <create requestid=\"req1\" conferenceid=\""
						+ confName + "\" /></conference>");

		if (respCreation.indexOf("OK") == -1)
			throw new OmsException("Error cannot create the conference : "
					+ respCreation);
	}

	/**
	 * To add a OMS call into the conference
	 * @param omsCall OMS call
	 * @param param
	 * @throws OmsException
	 */
	public void add(OmsCall omsCall, String param) throws OmsException {

		String repJoin;
		int num = 1;
		connOMSCall = omsCall.getVipConnexion();
		websock = omsCall.getWebSocket();

		/*
		 * To deal with the connection and reconnexion of a client into the same
		 * conference
		 */
		if (listOmsCallInConf.isEmpty()) {

			listOmsCallInConf.add(omsCall);
			omsCall.setPartNumberConf(num);
		} else {

			Iterator<OmsCall> ite = listOmsCallInConf.iterator();
			while (ite.hasNext()) {

				arrayList.add(ite.next().getPartNumberConf());
			}

			num = Collections.max(arrayList) + 1;
			listOmsCallInConf.add(omsCall);
			omsCall.setPartNumberConf(num);
		}

		String respSh = connOMSCall.getReponse("mt1 shutup");
		if (!respSh.equals("OK"))
			throw new OmsException("Cannot shutup mt1");

		if (param.equalsIgnoreCase("mute")) {
			logger.info("m muted");
			repJoin = connOMSConf.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""+ confName
							+ "\"  participantid=\""+ num + "\" confrole=\"mute\" /></conference>");
		}else if(param.equalsIgnoreCase("entertone")){			
			repJoin = connOMSConf.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""+ confName
					+ "\"  participantid=\""+ num + "\" entertone=\"false\"/></conference>");			
		}
		else
			repJoin = connOMSConf.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""+ confName
							+ "\"  participantid=\"" + num + "\" /></conference>");

		if (repJoin.indexOf("OK") != -1) {

			Matcher mat1 = pat1.matcher(repJoin);

			if (mat1.find()) {
				String mediaOutput = "/" + mat1.group(1) + "";
				String respSynt = connOMSCall.getReponse("new s2 synt.synt host=127.0.0.1 port=7777");
				if (respSynt.indexOf("OK") != -1) {
					String respBind = connOMSCall
							.getReponse("mt1 setparam bind=s2");

					if (respBind.indexOf("OK") != -1) {
						String respCodec = connOMSCall.getReponse("s2 setparam ttscodec=a8k");

						if (respCodec.indexOf("OK") != -1) {
							String respSay = connOMSCall.getReponse("s2 say \"cat /opt/application/64poms/"
											+ "current/tmp"+ mediaOutput + "\"");

							if (respSay.indexOf("OK") == -1)
								throw new OmsException("say cmd failed  : " + respSay);
						} else
							throw new OmsException("cmd s2 setparam ttscodec=a8k : " + respCodec);
					} else
						throw new OmsException("mt1 setparam bind=s2 : " + respBind);
				} else
					throw new OmsException("new s2 synt.synt host=127.0.0.1 port=7777 " + respSynt);
			} else
				throw new OmsException("mediaoutput :  NO MATCH " + repJoin);

			Matcher mat2 = pat2.matcher(repJoin);

			if (mat2.find()) {

				String mediaInput = "/" + mat2.group(1);
				// logger.info("mediaInput: " + mediaInput);
				String respEnreg = connOMSCall.getReponse("new e1 enreg");
				if (respEnreg.indexOf("OK") == -1)
					throw new OmsException(
							"cannot create a new recording ressource: "
									+ respEnreg);

				String startRec = connOMSCall.getReponse("e1 start /opt/application/64poms/current/tmp"+ 
				mediaInput + "");
				if (startRec.indexOf("OK") == -1)
					throw new OmsException("cannot record: " + startRec);

			} else
				throw new OmsException("mediaintput :  NO MATCH " + repJoin);

		} else if (repJoin.indexOf("406") != -1) {
			websock.send("Conference does not yet exist");
			throw new OmsException("Error cannot join the conference : "
					+ repJoin);
		} else if (repJoin.indexOf("411") != -1)
			throw new OmsException(
					"Delete files conf_1.rd and conf_1.wr at /opt/application/64poms/current/tmp");
		else if (repJoin.indexOf("408") != -1)
			throw new OmsException("Error cannot join the conference : "
					+ repJoin);
	}

	/**
	 * To remove a OMS call from the conference
	 * @param omsCall a OMS call 
	 * @throws OmsException
	 */
	public void delete(OmsCall omsCall) throws OmsException {

		int num = omsCall.getPartNumberConf();
		connOMSCall = omsCall.getVipConnexion();
		String conf = getName();
		String unJoinRep;
		boolean unjoin = false;

		Iterator<OmsCall> ite = listOmsCallInConf.iterator();
		int num2;
		while (ite.hasNext()) {
			num2 = ite.next().getPartNumberConf();
			if (num2 == num) {
				unJoinRep = getVipConnexion().getReponse("<conference><unjoin conferenceid=\"" + conf
								+ "\" requestid=\"" + num+ "\" participantid=\"" + num + "\"/></conference>");
				if (unJoinRep.indexOf("OK") == -1)
					throw new OmsException("unjoining failed : " + unJoinRep);
				
				connOMSCall.getReponse("delete s2");
				connOMSCall.getReponse("delete e1");
				ite.remove();
				unjoin = true;
			}
		}

		if (!unjoin) {
			logger.info("participant " + num + " already unjoined");
		}

		if (listOmsCallInConf.size() == 0 && !destroyConf) {
			destroy(omsCall);
			destroyConf = true;
			logger.info("Conference destroyed");
			System.exit(0);			
		}
	}

	/**
	 * To destroy a conference
	 * @param omsCall destroyer of the conference
	 * @throws OmsException
	 */
	public void destroy(OmsCall omsCall) throws OmsException {

		connOMSCall = omsCall.getVipConnexion();
		listOmsCallInConf.clear();

		String repStatus = connOMSConf
				.getReponse("<conference><status requestid=\"req2\" conferenceid=\""
						+ confName + "\" /></conference>");
		if (repStatus.indexOf("OK") != -1) {

			Matcher mat3 = pat3.matcher(repStatus);
			while (mat3.find()) {

				String participantId = "\"" + mat3.group(1) + "\"";
				String unjoinRep = connOMSConf
						.getReponse("<conference><unjoin requestid=\"req2\" conferenceid=\""
								+ confName
								+ "\"  participantid="
								+ participantId.toString() + "/></conference>");
				if (unjoinRep.indexOf("OK") == -1)
					throw new OmsException("cannot unjoin  participant : "
							+ participantId + "because: " + unjoinRep);
			}

			String destroyConf = connOMSConf
					.getReponse("<conference><destroy requestid=\"req3\" conferenceid=\""
							+ confName + "\" /></conference>");

			if (destroyConf.indexOf("OK") != -1) {
				
				String rep1 = connOMSCall.getReponse("wait evt=mt1.*");

				// if (rep1.indexOf("OK")!=-1){
				// connOMSCall.getReponse("mt1 shutup");
				// connOMSCall.getReponse("delete mt1");
				//connOMSCall.getReponse("delete e1");
				// connOMSCall.getReponse("delete s");
				//connOMSCall.getReponse("delete s2");
				// }
			} else
				throw new OmsException("cannot destroy conference because: "
						+ destroyConf);
		} else
			throw new OmsException("Conference does not exist : " + repStatus);

	}

	/**
	 * To record a conference for some time
	 * @throws OmsException
	 * @throws IOException
	 */
	public void startRecording() throws OmsException, IOException {

		activate();
		t = new Thread(this);
		t.start();
		
		/*if (listOmsCallInConf.isEmpty()) {
			throw new OmsException(
					"Cannot start recording the conf (No one in the conf)");
		} else {
			randomGenerator = new Random();
			omsCallRecord = new OmsCall();

			int index = randomGenerator.nextInt(listOmsCallInConf.size());
			String[] hostPortVip = listOmsCallInConf.get(index).getHostPortVip();
			omsCallRecord.connect(hostPortVip[0], hostPortVip[1]);

			String respJoin;
			int num = 0;

			respJoin = connOMSConf.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""+ confName
							+ "\"  participantid=\""+ num + "\" entertone=\"false\" exittone=\"false\"/></conference>");

			if (respJoin.indexOf("OK") != -1) {

				Matcher mat2 = pat2.matcher(respJoin);

				if (mat2.find()) {

					String mediaInput = "/" + mat2.group(1);
					String mediaInputPath = "/opt/application/64poms/current/tmp"+ mediaInput;
					OutputStream outputStream = new FileOutputStream(new File(mediaInputPath));
					// OutputStreamWriter outputWritter = new
					// OutputStreamWriter(outputStream);
					// Writer writer = new BufferedWriter(outputWritter);
					// writer.write("begin");
					String string = "Hello";
					byte[] buf = new byte[1024];
					buf = string.getBytes();
					outputStream.write(buf, 0, buf.length);

					outputStream.close();

				} else
					throw new OmsException("mediaintput :  NO MATCH "
							+ respJoin);

				Matcher mat1 = pat1.matcher(respJoin);

				if (mat1.find()) {
					
					Iterator<OmsCall> ite = listOmsCallInConf.iterator();
		
					while (ite.hasNext()) {
						 ite.next().getWebSocket().send("recordConf");					
					}					
					
					long endTime = System.currentTimeMillis() + 15000;

					String mediaOutput = "/" + mat1.group(1) + "";
					// String mediaOutputPath =
					// "/opt/application/64poms/current/tmp/conf_1.wr";
					String mediaOutputPath = "/opt/application/64poms/current/tmp" + mediaOutput;
					InputStream inputStream = new FileInputStream(new File(mediaOutputPath));
					// BufferedReader reader = new BufferedReader(new
					// InputStreamReader(inputStream));

					byte[] buf = new byte[1024];
					int bytes_read;

					// String line = reader.readLine();
					String enregFileRaw = enregFile + ".raw";
					File fRaw = new File(enregFileRaw);
					if (fRaw.exists()) {
						fRaw.delete();
						System.out.println(enregFileRaw + " deleted");
						fRaw.createNewFile();
					}

					OutputStream outputStream = new FileOutputStream(fRaw, true);
					// OutputStreamWriter outputWritter = new
					// OutputStreamWriter(outputStream);
					// Writer writer = new BufferedWriter(outputWritter);
					
					
					do{
						bytes_read = inputStream.read(buf);
						outputStream.write(buf, 0, bytes_read);
						System.out.println(bytes_read);
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}while(bytes_read != -1 || !getIsStopRecording());
					
					//while (System.currentTimeMillis() < endTime) {
						//bytes_read = inputStream.read(buf);
						//outputStream.write(buf, 0, bytes_read);
						// writer.write(line);
						// line = reader.readLine();
						// System.out.println(bytes_read);
					//}

					inputStream.close();
					outputStream.close();
					logger.info("Wrtting finish");
				} else
					throw new OmsException("mediaoutput :  NO MATCH "+ respJoin);

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
		}*/
	}

	private void deleteRecorder() throws OmsException, IOException{
		
		int num = 0;
		String conf = getName();
		
		String unJoinrep = connOMSConf.getReponse("<conference><unjoin conferenceid=\"" + conf
				+ "\" requestid=\"" + num + "\" participantid=\"" + num + "\"/></conference>");

		if (unJoinrep.indexOf("OK") == -1)
			throw new OmsException("unjoining failed : " + unJoinrep);

		omsCallRecord.getVipConnexion().close();
	}
	
	
	/**
	 * To stop recording the conference
	 * @throws OmsException
	 * @throws IOException
	 */
	public void stopRecording() throws OmsException, IOException {

		//int num = 0;
		//String conf = getName();
		
		terminate();
		
		Iterator<OmsCall> ite = listOmsCallInConf.iterator();		
		while (ite.hasNext()) {
			 ite.next().getWebSocket().send("stopRecordConf");					
		}
		
		/*String unJoinrep = connOMSConf.getReponse("<conference><unjoin conferenceid=\"" + conf
						+ "\" requestid=\"" + num + "\" participantid=\"" + num + "\"/></conference>");

		if (unJoinrep.indexOf("OK") == -1)
			throw new OmsException("unjoining failed : " + unJoinrep);

		omsCallRecord.getVipConnexion().close();*/
		//logger.info("Recorder disconnects");
		
		deleteRecorder();
	}

	/**
	 * To play the audio recorded file
	 * @param omsCall wants to play the file
	 * @throws OmsException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void playRecording() throws OmsException,
			IOException, InterruptedException {

		Process p;
		int returnCode;
		String enregFileRaw = enregFile + ".raw";
		String enregFileWav = enregFile + ".wav";
		String enregFilea8k = enregFile + ".a8k";
		String testAnimaux = "/opt/application/64poms/current/tmp/Animaux.a8k";

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

		String cmd1 = new String("sox -t al -r 8000 -b 8 -c 1 " + enregFileRaw + " " + enregFileWav);
		String cmd2 = new String("sox -t wav " + enregFileWav + " -t raw -r8000 -e a-law -b 8 -c 1 " + enregFilea8k);
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

		//delete(omsCall);// Ne peux pas lire le fichier alors qu'on est dans la
						// conférence
		//omsCall.play(enregFilea8k, false);
		
		//List<OmsCall> newList = new ArrayList<OmsCall>(listOmsCallInConf);
		//listOmsCallInConf.clear();
		
		//Iterator<OmsCall> ite = newList.iterator();
		//int num;
		//String unJoinRep;
		//OmsCall call;
		/*while (ite.hasNext()) {
			call = ite.next();
			num = call.getPartNumberConf();
			
			unJoinRep = connOMSConf.getReponse("<conference><unjoin conferenceid=\"" + getName()
					+ "\" requestid=\"" + num + "\" participantid=\"" + num + "\"/></conference>");
				if (unJoinRep.indexOf("OK") == -1)
					throw new OmsException("unjoining failed : " + unJoinRep);
				
				call.getVipConnexion().getReponse("delete s2");
				call.getVipConnexion().getReponse("delete e1");		
		}*/
	
		randomGenerator = new Random();
		omsCallRecord = new OmsCall();

		int index = randomGenerator.nextInt(listOmsCallInConf.size());
		String[] hostPortVip = listOmsCallInConf.get(index).getHostPortVip();
		omsCallRecord.connect(hostPortVip[0], hostPortVip[1]);

		String respJoin;
		int num = 0;

		respJoin = connOMSConf.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""+ confName
						+ "\"  participantid=\""+ num + "\" entertone=\"false\" exittone=\"false\"/></conference>");

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
				System.out.println(bytes_read);
				
				while(bytes_read != -1){			
					outputStream.write(buf, 0, bytes_read);
					Thread.sleep((long)17);
					bytes_read = inputStream.read(buf, 0, 160);
					//System.out.println("In the loop: " + bytes_read);
				}
				
				outputStream.close();
				inputStream.close();
			} else
				throw new OmsException("mediaintput :  NO MATCH " + respJoin);

			Matcher mat1 = pat1.matcher(respJoin);
			if (mat1.find()) {					
				
				String mediaOutput = "/" + mat1.group(1) + "";
				String mediaOutputPath = "/opt/application/64poms/current/tmp" + mediaOutput;
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
				
		deleteRecorder();
	}

	
	/**
	 * 
	 * @param omsCall
	 * @throws OmsException
	 */
	public void mute(OmsCall omsCall) throws OmsException{
		
		int num = omsCall.getPartNumberConf();
		VipConnexion confVip = getVipConnexion();
		String muteRep=confVip.getReponse("<conference><mute requestid=\"req4\" conferenceid=\""+getName()+"\" participantid=\"" 
		+ num + "\"/></conference>");
		if (muteRep.indexOf("OK")!=-1){
			logger.info("mutting successful"); 
			}
		else
			throw new OmsException("Error: cannot mute participant :" + num);
	}
	
	/**
	 * 
	 * @param omsCall
	 * @throws OmsException
	 */
	public void unmute(OmsCall omsCall) throws OmsException{
		
		int num = omsCall.getPartNumberConf();
		VipConnexion confVip = getVipConnexion();
		String muteRep=confVip.getReponse("<conference><unmute requestid=\"req5\" conferenceid=\""+getName()+"\" participantid=\"" 
		+ num + "\"/></conference>");
		if (muteRep.indexOf("OK")==-1)
			throw new OmsException("Error: cannot unmute participant :" + num);
	}
	
	
	/**
	 * 
	 * @throws OmsException
	 */
	public void muteAll() throws OmsException{
		
		VipConnexion confVip = getVipConnexion();
		String muteAllRep=confVip.getReponse("<conference><muteall requestid=\"req6\" conferenceid=\""+ 
		getName()+"\"/></conference>");
		if (muteAllRep.indexOf("OK") == -1)
			throw new OmsException("Error: cannot mute all the paricipants");			
	}
	
	/**
	 * 
	 * @throws OmsException
	 */
	public void unmuteAll() throws OmsException{
		
		VipConnexion confVip = getVipConnexion();
		String muteAllRep=confVip.getReponse("<conference><unmuteall requestid=\"req6\" conferenceid=\""+ 
		getName()+"\"/></conference>");
		if (muteAllRep.indexOf("OK") == -1)
			throw new OmsException("Error: cannot unmute all the paricipants");			
	}
		
	/**
	 * 
	 * @throws OmsException
	 */
	/*public void play() throws OmsException{
		
		//String enregFilea8k = enregFile + ".a8k";
		String url = "http://10.184.155.57:8080/docs/webRTC/doc/Animaux.a8k";
		String testAnimaux = "/opt/application/64poms/current/tmp/Animaux.wav";
		VipConnexion confVip = getVipConnexion();
		
		String subs = confVip.getReponse("<conference><subscribe requestid=\"101\" conferenceid=\""+
						getName()+"\"><event type=\"playterminated\"/></subscribe></conference>");
		
		logger.info(subs);
		String rep = confVip.getReponse("<conference><play requestid=\"req5\" conferenceid=\""+ 
				getName()+"\"><prompt url=\""+ testAnimaux +"\"/></play></conference>");
		
		if (rep.indexOf("OK")==-1)
			throw new OmsException("Error: cannot play file to participants :" + rep);
	}*/
		
	/**
	 * 
	 * @throws OmsException
	 */
	public void status() throws OmsException{
		
		VipConnexion confVip = getVipConnexion();
		String rep = confVip.getReponse("<conference><status requestid=\"req6\" conferenceid=\""+ 
				getName()+"\"/></conference>");
		
		if (rep.indexOf("OK")==-1)
			throw new OmsException("Error: cannot get the status :" + rep);
	}
	
	/**
	 * To get the number of participant in the conference
	 * @return total number of participants in the conference
	 */
	public int getParticipantsNumber() {
		// return nbOfPartInConf;
		return listOmsCallInConf.size();
	}

	/**
	 * To get the connection to OMS for the conference
	 * @return the connection to OMS
	 */
	public VipConnexion getVipConnexion() {
		return connOMSConf;
	}

	/**
	 * To get the conference id or name
	 * @return conference's name
	 */
	public String getName() {
		return confName;
	}

	/**
	 * For launching the thread responsible to start the recording.
	 */
	@Override
	public void run(){
		// TODO Auto-generated method stub
		
		if (!listOmsCallInConf.isEmpty()) {

			randomGenerator = new Random();
			omsCallRecord = new OmsCall();

			int index = randomGenerator.nextInt(listOmsCallInConf.size());
			String[] hostPortVip = listOmsCallInConf.get(index).getHostPortVip();
			try {
				omsCallRecord.connect(hostPortVip[0], hostPortVip[1]);
			} catch (OmsException | IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			String respJoin = null;
			int num = 0;

			try {
				respJoin = connOMSConf.getReponse("<conference><join  requestid=\"req1\" conferenceid=\""+ confName
								+ "\"  participantid=\""+ num + "\" entertone=\"false\" exittone=\"false\"/></conference>");
			} catch (OmsException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			if (respJoin.indexOf("OK") != -1) {

				Matcher mat2 = pat2.matcher(respJoin);

				if (mat2.find()) {

					String mediaInput = "/" + mat2.group(1);
					String mediaInputPath = "/opt/application/64poms/current/tmp"+ mediaInput;
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
						inputStream = new FileInputStream(new File(mediaOutputPath));
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
						while(running){
							
							bytes_read = inputStream.read(buf);
							//System.out.println(bytes_read);
							if(bytes_read == -1)
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
}
