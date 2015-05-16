/**
 * 
 */
package test;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.orange.olps.api.webrtc.*;

/**
 * @author JWPN9644
 * 
 */

public class MonService extends OmsService implements OmsMessageListener {

	/**
	 * @param args
	 */
	
	private static Logger logger = Logger.getLogger(MonService.class);
	//private static final String WEBRTC_CONF = "/opt/testlab/utils/stageFabrice/src/main/java/";
	private static final String WEBRTC_CONF = "C:\\opt\\application\\testlab\\utils\\OmsGateway\\";
	protected static String hostVip = "127.0.0.1";
	protected static String portVip = "4670";
	private static String portWs = "8887";
	//private static String hostVipConf ;
	private static String portVipConf ;
	static Properties prop = new Properties();
	static FileInputStream propFic = null;
	static BufferedWriter ficSdp = null;
	
	private static final String DEFAULT_OMS_HOST = "127.0.0.1";
	private static final String DEFAULT_OMS_PORT = "8080";
	private static final String DEFAULT_WS_PORT = "8887";
	private static final String DEFAULT_CONF_PORT = "10000";
	
	boolean isAnswer = false;
	//private List<OmsCall> listOmsCall = new ArrayList<OmsCall>();
	//private String filePath = "/opt/application/64poms/current/tmp/infosOnConferences.log";
	private String filePath = "C:\\opt\\infosOnConferences.log";
    //private String filePath2 = "/opt/application/64poms/current/tmp/Animaux.a8k";
	private String enregFile = "/opt/application/64poms/current/tmp/enregFile";
		
	private OmsConference conf;
	private Annuaire annuaire;
	private ConferenceParameters conferenceParam;
	
	private String mode, username, confName;
	private String[] splitParam;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new MonService();		
	}

	public MonService() {
		
		super(Integer.parseInt(portWs));
		PropertyConfigurator.configure(WEBRTC_CONF + "log4j.properties");

		try {
			propFic = new FileInputStream(WEBRTC_CONF + "webRTC.properties");
		} catch (FileNotFoundException fnfe) {
			logger.error("Le fichier webRTC.properties n existe pas.");
			System.exit(2);
		}
		try {
			prop.load(propFic);
			propFic.close();
		} catch (IOException ioe) {
			logger.error("Impossible de lire le fichier de properties.");
		}

		hostVip = prop.getProperty("oms.host", DEFAULT_OMS_HOST);
		portVip = prop.getProperty("oms.port", DEFAULT_OMS_PORT);
		portWs = prop.getProperty("ws.port", DEFAULT_WS_PORT);
		portVipConf = prop.getProperty("conf.port", DEFAULT_CONF_PORT);
		
		addEventListener(this);
		this.start();
		logger.info("Service  started on port: " + getPort());	
		
		try {
			conf = new OmsConference(hostVip, portVipConf);
			annuaire = new Annuaire();
			//conf.create("conf1");
		} catch (OmsException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void omsMessagePerformed(OmsMessageEvent msgEvt) {
		// TODO Auto-generated method stub

		OmsCall call = msgEvt.getOmsCall();

		String message = msgEvt.getMessage();
		//logger.info("Nouveau message: " + message);
		//logger.info("ReÃ§u de: " + call + "l'adresse ip: "
				//+ call.getIpAddress());
		OmsMessage msg = new OmsMessage(message);
		String typeMesg = msg.getType();
		try{
		switch (typeMesg) {
		case "sdp":
			// Le message est du sdp
			String sdp = msg.getSdp();
			String userName2 = msg.getUserName();			
			
			//if (!isAnswer) {
				if (!annuaire.checkUserName(call, userName2)) {
					call.connect(hostVip, portVip);
					call.init(sdp);
					call.say(
							"Bienvenue sur le serveur de conference. Pour entrer dans la conference. "
									+ "Tapez conference", true);
					annuaire.setUserName(call, userName2);
					annuaire.showPeopleConnectedToOms(call, true);
				}
				//} else {
					//call.answer(sdp);
					//logger.info("la mÃ©thode answer a reussit");
				//}
			break;
		case "cmd":
			// Le message est une commande
			traiteCmd(call, msg);
			break;
		default:
			// Le message est de type inconnu
			logger.error("Format de message inconnu : " + message + ". Ce n'est pas du JSON");
		}
		}catch(OmsException | IOException e){
			e.printStackTrace();
		}
	}

	public void traiteCmd(OmsCall call, OmsMessage msg) {

		String cmd = msg.getCmd();
		String param = msg.getParam();
		try{
		switch (cmd) {
			case "logout":
				//c.send("logout");
				//annuaire.remove(param);
				break;
			case "call":
				call.call(annuaire.getOmsCall(param));
				//Dans call, récupérer l'userName de l'appelant et l'envoyer à l'appelé			
				break;
			case "answer":
				//L'appelé accepte l'appel. Il renvoie l'userName de celui qui l'a appelé afin de lui notifier
				//notifier de son acception
				call.answer(annuaire.getOmsCall(param), conf, true);
				break;
			case "reject":
				//L'appelé accepte l'appel. Il renvoie l'userName de celui qui l'a appelé afin de lui notifier
				//notifier de son rejet
				call.reject(annuaire.getOmsCall(param));
				break;
			case "createConf":				
				if(annuaire.checkOmsCall(call))
					param = annuaire.updatingStringParam(call, param);
				
				splitParam = param.split(":");
				username = splitParam[0];
				//mode = splitParam[1];
				confName = splitParam[1];
				
				conferenceParam = new ConferenceParameters(confName);					
				conf.create(call, conferenceParam);				
				//conf.create(call, confName);
				break;
			case ("joinConf"):
				// Le client demande a entrer dans la confÃ©rennce ouverte dans
				// le constructeur
				// Elle est enregistree dans /tmp/conf1.wav
				// Il aurait pu la creer lui-meme
				// Param sert pour muteOn ou muteOff
				// conf.join(call) ou call.join(conf)
				// call.join(conf, param);

				if (annuaire.checkOmsCall(call))
					param = annuaire.updatingStringParam(call, param);

				splitParam = param.split(":");
				username = splitParam[0];
				mode = splitParam[1];
				confName = splitParam[2];

				conferenceParam = new ConferenceParameters(confName);
				conferenceParam.setConfrole(mode);
				conferenceParam.setName(username);

				if (!conf.isClientJoined(call)) {
					//conf.add(call, param);
					conf.add(call, conferenceParam);
					conf.showParticipant(call.getConfname());
				}
				break;
			case "say":			
				call.say("Vous avez cliquez sur say", true);
				break;
			case "playRecording":
				if(conf.status(call.getConfname()))
					conf.playRecording(call.getConfname());
					//conf.play();
				break;
			case "playFile":
				//conf.myPlay(call.getConfname(), filePath2);
				call.play(enregFile + ".a8k", false);
				break;
			case "stopPlay":
				conf.stopPlay();
				break;
			case ("recordConf"):
				// Le client demande a ce que l'appel soit enregistre. 
					//call.record(param);
				if(conf.status(call.getConfname()))
					conf.startRecording(call.getConfname());
				break;
			case ("stopRecordConf"):
				// Le client demande a arreter l'enregistrement. 
					//call.stopRecord();
				if(conf.status(call.getConfname()))
					conf.stopRecording(call.getConfname());
				break;
			case ("dtmf"):
				// Le client saisit une pseudo dtmf. En fait, il clique sur un bouton
				switch (Integer.parseInt(param)){
					case 1:
						call.say("vous pouvez ecouter les offres 1", false);
						break;
					default :
						call.say("Je ne vous ai pas entendu.", false);
				}
				break;
			case "mute":
				if(conf.isClientJoined(call))
					conf.mute(call);
				break;
			case "unmute":
				if(conf.isClientJoined(call))
					conf.unmute(call);
				break;
			case "muteAll":
				if(conf.status(call.getConfname()))
					conf.muteAll(call.getConfname());
				break;
			case "unmuteAll":
				if(conf.status(call.getConfname()))
					conf.unmuteAll(call.getConfname());
				break;
			case ("confInfos"):
				conf.infos(call,filePath);
				break;
			case "hangup":
				//conf.delete(call);
				call.hangup(annuaire.getOmsCall(param), conf);
				break;
			case "disconnect":
				if(annuaire.checkOmsCall(call))
					annuaire.showPeopleConnectedToOms(call, false);			
				//quitter la conf (function unjoin retourne vrai si c'est le dernier Ã  quitter la conf)
				//dÃ©truire la conf si c'est le dernier client Ã  quitter la conf
				//conf.infos(call,filePath);	
				conf.delete(call);
				call.delete();
				break;
			default :
				// La commande n'est pas connue
				logger.error("Commande et param inconnus. cmd: " + cmd + " param: "+ param);
		}
		} catch(OmsException | IOException | InterruptedException e){
			e.printStackTrace();
		}
	}
	
}
