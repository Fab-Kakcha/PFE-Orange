package test;

import com.orange.olps.api.webrtc.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;

public class OmsServiceTest extends WebSocketServer {

	private static Logger logger = Logger.getLogger(OmsServiceTest.class);
	private static final String DEFAULT_OMS_HOST = "127.0.0.1";
	private static final String DEFAULT_OMS_PORT = "8080";
	private static final String DEFAULT_WS_PORT = "8887";
	private static final String DEFAULT_CONF_PORT = "10000";
	
	private static final String WEBRTC_CONF = "/opt/testlab/utils/stageFabrice/src/main/java/";
	//private static final String WEBRTC_CONF = "C:\\Users\\JWPN9644\\opt\\application\\64poms\\current\\conf\\";
	protected static String hostVip = "127.0.0.1";
	protected static String portVip = "4670";
	private static String portWs = "8887";
	//private static String hostVipConf ;
	private static String portVipConf;

	// private static HashMap<WebSocket, ClientServiceWebrtc> clients = new
	// HashMap<WebSocket, ClientServiceWebrtc>();
	protected static HashMap<WebSocket, OmsCall> calls = null;
	protected static HashMap<String, WebSocket> annuaire = null;

	static Properties prop = new Properties();
	static FileInputStream propFic = null;
	static BufferedWriter ficSdp = null;

	boolean callFabrice = false;
	boolean callTest2 = false;
	boolean isAnswer = false;
	boolean hasClientPressDisc = false;
	OmsConference conf;
		
	
	public static void main(String[] args) {

	/*
	 * Initialisation des parametres depuis le fichier de properties
	 */

	 PropertyConfigurator.configure(WEBRTC_CONF + "log4j.properties");
	  
	 try { 
		 propFic = new FileInputStream(WEBRTC_CONF + "webRTC.properties"); 
	 }
	  catch (FileNotFoundException fnfe) {
		  logger.error("Le fichier webRTC.properties n existe pas.");
		  System.exit(2); 
	 } try { 
			  prop.load(propFic); propFic.close(); 
	  } catch (IOException ioe) {
	  logger.error("Impossible de lire le fichier de properties."); 
	  }
	  
	  hostVip = prop.getProperty("oms.host", DEFAULT_OMS_HOST); 
	  portVip = prop.getProperty("oms.port", DEFAULT_OMS_PORT); 
	  portWs = prop.getProperty("ws.port", DEFAULT_WS_PORT);
	  portVipConf = prop.getProperty("conf.port", DEFAULT_CONF_PORT);

	/*
	 * Demarrage du serveur de websocket en local pour recevoir les appels du
	 * navigateur
	 */

	OmsServiceTest oms = null;
	WebSocketImpl.DEBUG = false;
	
	  try { 
		  oms = new OmsServiceTest(); 
		  oms.start(); 
		  dort(500);
		  logger.info("OmsGateway started on port: " + oms.getPort());
	  
	  } catch (InterruptedException | IOException e) {
		  System.out.println("Erreur au lancement du serveur"); 
	  }
	
	}

	public OmsServiceTest() throws InterruptedException, IOException {

		super(new InetSocketAddress(new Integer(portWs).intValue()));
			
		try {
			// Demarrage de la conference
			conf = new OmsConference(hostVip, portVipConf);
			conf.create("conf1");
		} catch (OmsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// enregistrement de la conference
		// prevoir de demarrer l'enregistrement a l'arrivee d'un participant
		// et arreter a la sortie du dernier
		// conf.record("/tmp/conf1.wav");
		// Initialisation de la table des appels et de l'annuaire
		// L'annuaire pourrait etre une BDD ou un fichier xml ou autre
		// Dans un premier temps, c'est en memoire

		calls = new HashMap<WebSocket, OmsCall>();
		annuaire = new HashMap<String, WebSocket>();
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		
		String ipAddress = conn.getRemoteSocketAddress().getAddress().getHostAddress();
		logger.info("NOUVEAU CLIENT : " + ipAddress);
			
		
		// Arrivee d'un appel. On ne connait que conn
		// On instancie un OmsCall et on le stocke dans la table des OmsCall
		OmsCall call = new OmsCall(conn, ipAddress);
		calls.put(conn, call);
		
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		logger.info("SORTIE DE CLIENT : "
				+ conn.getRemoteSocketAddress().getAddress().getHostAddress());

		// Quand un client se deconnecte, on detruit tout ce qui lui appartient
		//OmsCall call = calls.get(conn);
		// call.unjoin(conf);
		/*if (!hasClientPressDisc) {
			try {
				call.closeClient();
			} catch (OmsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
		calls.remove(conn);
		conn.close();
	}

	@Override
	public void onMessage( WebSocket conn, String message ) {
		// Arrivee d'un message
		// On instancie la classe Message
		// On regarde de quel type est le message sdp ou commande
		// Si sdp, on etablie automatiquement l'appel
		
		logger.info("NAV ==> AS : " + message );
		OmsCall call = calls.get(conn);
			
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
					logger.info("la methode answer a reussit");
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
					conf.playRecord(call);
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
				// Le client demande a entrer dans la conferennce ouverte dans le constructeur
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
				//quitter la conf (function unjoin retourne vrai si c'est le dernier a quitter la conf)
				//detruire la conf si c'est le dernier client a quitter la conf
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
	
	
	public void onFragment(WebSocket conn, Framedata fragment) {
		logger.info("received fragment: " + fragment);
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		ex.printStackTrace();
	}

	public static void dort(int temps) {
		try {
			Thread.sleep(temps);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	public static void logError(String attendu, String recu) {
		logger.error("ERREUR TEST : Reponse OMS incorrecte\n"
				+ "        Attendu : " + attendu + "\n        Recu    : "
				+ recu);
	}

}
