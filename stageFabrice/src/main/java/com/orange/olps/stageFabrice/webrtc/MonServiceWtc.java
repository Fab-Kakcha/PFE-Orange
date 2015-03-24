package com.orange.olps.stageFabrice.webrtc;

import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;

/**
 * @author JWPN9644
 * 
 */

public class MonServiceWtc implements OmsMessageListener {

	private static Logger logger = Logger.getLogger(MonServiceWtc.class);
	boolean isAnswer = false;
	private String hostVip;
	private String portVip;
	private OmsConference conf;
	//private static HashMap<String, WebSocket> annuaire = new HashMap<String, WebSocket>();
	//protected static HashMap<WebSocket, OmsCall> calls = new HashMap<WebSocket, OmsCall>();
		
	String filePath ="/var/opt/data/flat/64poms/files/logs/20150210/bonjour.a8k";
	String filePathEnreg ="/var/opt/data/flat/64poms/files/logs/20150210/recording.a8k";
	
	public static void main(String[] args) {
		
		try {
			 new OmsService();
		} catch (InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

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
	public void omsMessagePerformed(OmsMessageEvent msgEvt) throws OmsException {
		// TODO Auto-generated method stub

		OmsCall call = msgEvt.getOmsCall();

		String message = msgEvt.getMessage();
		//logger.info("Nouveau message: " + message);
		//logger.info("Reçu de: " + call + " à l'adresse ip: "
			//	+ call.getIpAddress());
		OmsMessage msg = new OmsMessage(message);
		String typeMesg = msg.getTypeMsg();

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
			} catch (OmsException e) {
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
		//WebSocket webSock = call.getWebSocket();
		switch (cmd) {
			case ("login"):

				break;
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
			case "answer":
				isAnswer = true;
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
					call.play(param, true);
				} catch (OmsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case ("record"):
				// Le client demande a ce que l'appel soit enregistre. 
				try {
					//call.record(param);
					conf.recordConf();
				} catch (OmsException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case ("stopRecord"):
				// Le client demande a arreter l'enregistrement. 
				try {
					call.stopRecord();
					//conf.stopRecordConf();
				} catch (OmsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "recognize":
			try {
				call.recognize();
			} catch (OmsException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
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
				
				conf.join(call, param);
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
					case 2:
					try {
						call.say("vous pouvez ecouter les offre 2", false);
					} catch (OmsException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
						break;
					case 3:
					try {
						call.say("vous pouvez ecouter les offre 3", false);
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
				conf.unJoin(call);
				call.closeClient();
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
