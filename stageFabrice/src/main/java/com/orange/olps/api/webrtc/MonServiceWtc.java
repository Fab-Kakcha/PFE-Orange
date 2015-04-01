package com.orange.olps.api.webrtc;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * @author JWPN9644
 * 
 */

public class MonServiceWtc implements OmsMessageListener {

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
	
	
	public static void main(String[] args) {
			
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
			
		try {
			//MonServiceWtc monServiceWtc = new MonServiceWtc(hostVip, hostVip, null);
			 new OmsServiceEx();
		} catch (InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	private static Logger logger = Logger.getLogger(MonServiceWtc.class);
	boolean isAnswer = false;
	//private String hostVip;
	//private String portVip;
	private OmsConference conf;
		
	/**
	 * To get OMS's IP address and port from OmsService class
	 * @param host
	 * @param port
	 */
	public MonServiceWtc(String host, String port, OmsConference conf) {
		// TODO Auto-generated constructor stub

		hostVip = host;
		portVip = port;
		this.conf = conf;
	}
	
	/**
	 * To perform an action when a message is received on webSocket
	 */
	@Override
	public void omsMessagePerformed(OmsMessageEvent msgEvt) {
		// TODO Auto-generated method stub

		OmsCall call = msgEvt.getOmsCall();

		String message = msgEvt.getMessage();
		//logger.info("Nouveau message: " + message);
		//logger.info("Reçu de: " + call + " à l'adresse ip: "
			//	+ call.getIpAddress());
		OmsMessage msg = new OmsMessage(message);
		String typeMesg = msg.getType();

		switch (typeMesg) {
		case "sdp":
			// Le message est du sdp
			String sdp = msg.getSdp();
			try {
				if (!isAnswer) {
					call.connect(hostVip, portVip);
					call.init(sdp);
					call.say("Bienvenue sur le serveur de conference. Pour entrer dans la conference. "
							+ "Tapez conference", true);
				} else {
					call.answer(sdp);
					logger.info("la méthode answer a reussit");
				}
			} catch (OmsException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case "cmd":
			// Le message est une commande
			traiteCmd(call, msg);
			break;
		default:
			// Le message est de type inconnu
			// logger.error("Type de message inconnu : " + message);
			logger.error("Format de message inconnu : " + message
					+ ". Ce n'est pas du JSON");
		}
	}

	public void traiteCmd(OmsCall call, OmsMessage msg) {

		String cmd = msg.getCmd();
		String param = msg.getParam();
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
			try {
				conf.create(param);
			} catch (OmsException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
				break;
			case "say":
				try {
					call.say("Vous avez cliquez sur say", true);
				} catch (OmsException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				break;
			case "play":
				try {
					conf.playRecording();
					//call.play(param, true);
				} catch (OmsException | IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case ("record"):
				// Le client demande a ce que l'appel soit enregistre. 
				try {
					//call.record(param);
					conf.startRecording();
				} catch (OmsException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case ("stopRecord"):
				// Le client demande a arreter l'enregistrement. 
				try {
					//call.stopRecord();
					conf.stopRecording();
				} catch (OmsException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case ("joinConf"):
				// Le client demande a entrer dans la conférennce ouverte dans le constructeur
				// Elle est enregistree dans /tmp/conf1.wav
				// Il aurait pu la creer lui-meme
				// Param sert pour muteOn ou muteOff
				// conf.join(call) ou call.join(conf)
				//call.join(conf, param);
			try {				
				conf.add(call, param);
			} catch (OmsException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
				break;
			case ("dtmf"):
				// Le client saisit une pseudo dtmf. En fait, il clique sur un bouton
				switch (Integer.parseInt(param)){
					case 1:
					try {
						call.say("vous pouvez ecouter les offres 1", false);
					} catch (OmsException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
						break;
					default :
					try {
						call.say("Je ne vous ai pas entendu.", false);
					} catch (OmsException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				break;
			case "disconnect":
			try {
				//quitter la conf (function unjoin retourne vrai si c'est le dernier à quitter la conf)
				//détruire la conf si c'est le dernier client à quitter la conf
				conf.delete(call);
				call.delete();
			} catch (OmsException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}				
				break;
			default :
				// La commande n'est pas connue
				logger.error("Commande et param inconnus. cmd: " + cmd + " param: "+ param);
		}
	}
}
