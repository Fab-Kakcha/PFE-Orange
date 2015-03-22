package com.orange.olps.stageFabrice.webrtc;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;

/**
 * @author JWPN9644
 * 
 */

public class MonServiceWtc implements OmsCallListener, OmsMessageListener {

	private static Logger logger = Logger.getLogger(MonServiceWtc.class);
	boolean isAnswer = false;
	private String hostVip;
	private String portVip;
	private static HashMap<String, WebSocket> annuaire = new HashMap<String, WebSocket>();
	protected static HashMap<WebSocket, OmsCall> calls = new HashMap<WebSocket, OmsCall>();


	public MonServiceWtc(String host, String port) {
		// TODO Auto-generated constructor stub

		hostVip = host;
		portVip = port;
	}

	@Override
	public void omsCallPerformed(OmsCallEvent callEvt) throws OmsException {
		// TODO Auto-generated method stub

	}

	@Override
	public void omsMessagePerformed(OmsMessageEvent msgEvt) throws OmsException {
		// TODO Auto-generated method stub

		OmsCall omsCall = msgEvt.getOmsCall();

		String message = msgEvt.getMessage();
		logger.info("Nouveau message: " + message);
		logger.info("Reçu de: " + omsCall + " à l'adresse ip: "
				+ omsCall.getIpAddress());
		OmsMessage msg = new OmsMessage(message);
		String typeMesg = msg.getTypeMsg();

		switch (typeMesg) {
		case "sdp":
			// Le message est du sdp
			String sdp = msg.getSdp();
			try {
				if (!isAnswer) {
					omsCall.connect(hostVip, portVip);
					omsCall.init(sdp);
					omsCall.say("Bienvenue sur le serveur de conference", false);
				} else {
					omsCall.answer(sdp);
					logger.info("la méthode answer a reussit");
				}
			} catch (OmsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case "cmd":
			// Le message est une commande
			traiteCmd(omsCall, msg);
			break;
		default:
			// Le message est de type inconnu
			// logger.error("Type de message inconnu : " + message);
			logger.error("Format de message inconnu : " + message
					+ ". Ce n'est pas du JSON");
		}
	}

	public void traiteCmd(OmsCall call, OmsMessage m) {
		OmsMessage msg = m;
		String cmd = msg.getCmd();
		String param = msg.getParam();
		WebSocket webSock = call.getWebSocket();
		switch (cmd) {
			case ("login"):
				// Le client s'identifie. param est l'identifiant			
				if(annuaire.containsKey(param)){
					logger.error("Le prénom est déjà utilisé");
					webSock.send("echecEnreg");
				}else{				
					annuaire.put(param, webSock);
					calls.put(webSock, call);
				}
				break;
			case "logout":
				//c.send("logout");
				annuaire.remove(param);
				break;
			case ("call"):
				if(annuaire.containsKey(param)){
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
				}
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
					call.record(param);
				} catch (OmsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case ("stopRecord"):
				// Le client demande a arreter l'enregistrement. 
				try {
					call.stopRecord();
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
			case ("joinConf"):
				// Le client demande a entrer dans la conférennce ouverte dans le constructeur
				// Elle est enregistree dans /tmp/conf1.wav
				// Il aurait pu la creer lui-meme
				// Param sert pour muteOn ou muteOff
				// conf.join(call) ou call.join(conf)
				//call.join(conf, param);
				break;
			case "disconnect":
			try {
				call.say("Au revoir, et a bientot sur OMS", false);
				call.closeClient();
			} catch (OmsException e) {
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
