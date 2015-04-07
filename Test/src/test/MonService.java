/**
 * 
 */
package test;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
	private static final String WEBRTC_CONF = "/opt/testlab/utils/stageFabrice/src/main/java/";
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
	private OmsConference conf;
	private List<OmsCall> listOmsCall = new ArrayList<OmsCall>();
	private String filePath = "/opt/application/64poms/current/tmp/infosOnConferences.log";
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MonService srv = new MonService();		
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
		//logger.info("ReÃ§u de: " + call + " Ã  l'adresse ip: "
			//	+ call.getIpAddress());
		OmsMessage msg = new OmsMessage(message);
		String typeMesg = msg.getType();
		try{
		switch (typeMesg) {
		case "sdp":
			// Le message est du sdp
			String sdp = msg.getSdp();
				if (!isAnswer) {
					call.connect(hostVip, portVip);
					call.init(sdp);
					call.say("Bienvenue sur le serveur de conference. Pour entrer dans la conference. "
							+ "Tapez conference", true);
					listOmsCall.add(call);
				} else {
					call.answer(sdp);
					logger.info("la mÃ©thode answer a reussit");
				}
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
			case ("call"):
				/*if(annuaire.containsKey(param)){
					webSock.send(param+"Connected");
					OmsCall call2 = calls.get(annuaire.get(param));
					try {
						call.call(call2);
					} catch (OmsException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else{
					webSock.send(param+"NotConnected");
				}*/
				break;
			case "createConf":
				conf.create(call, "conf1");
				conf.notification(listOmsCall);
				//conf.create(param);
				break;
			case "say":			
				call.say("Vous avez cliquez sur say", true);
				break;
			case "play":				
					conf.playRecording();
					//conf.play();
				break;
			case ("record"):
				// Le client demande a ce que l'appel soit enregistre. 
					//call.record(param);
					conf.startRecording();
				break;
			case ("stopRecord"):
				// Le client demande a arreter l'enregistrement. 
					//call.stopRecord();
					conf.stopRecording();
				break;
			case ("joinConf"):
				// Le client demande a entrer dans la confÃ©rennce ouverte dans le constructeur
				// Elle est enregistree dans /tmp/conf1.wav
				// Il aurait pu la creer lui-meme
				// Param sert pour muteOn ou muteOff
				// conf.join(call) ou call.join(conf)
				//call.join(conf, param);
				conf.add(call, param);
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
			case "disconnect":
				//quitter la conf (function unjoin retourne vrai si c'est le dernier Ã  quitter la conf)
				//dÃ©truire la conf si c'est le dernier client Ã  quitter la conf
				conf.list(filePath);
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
