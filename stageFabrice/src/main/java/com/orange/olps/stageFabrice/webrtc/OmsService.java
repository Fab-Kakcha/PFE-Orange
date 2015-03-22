package com.orange.olps.stageFabrice.webrtc;

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

//import com.orange.olps.OmsException;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;

public class OmsService extends WebSocketServer {

	private static Logger logger = Logger.getLogger(OmsService.class);
	private static final String DEFAULT_OMS_HOST = "127.0.0.1";
	private static final String DEFAULT_OMS_PORT = "8080";
	private static final String DEFAULT_WS_PORT = "8887";
	// private static final String WEBRTC_CONF =
	// "/opt/application/64poms/current/conf/";
	private static final String WEBRTC_CONF = "C:\\Users\\JWPN9644\\opt\\application\\64poms\\current\\conf\\";
	protected static String hostVip = "127.0.0.1";
	protected static String portVip = "4670";
	private static String portWs = "8887";

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
	// String filePath ="/var/opt/data/flat/64poms/files/logs/20150210/bonjour.a8k";
	// String filePathEnreg ="/var/opt/data/flat/64poms/files/logs/20150210/recording.a8k";
	private OmsCall call;
	private MonServiceWtc monServiceWtc;

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
	  
	  hostVip = prop.getProperty("oms.host", DEFAULT_OMS_HOST); portVip =
	  prop.getProperty("oms.port", DEFAULT_OMS_PORT); portWs =
	  prop.getProperty("ws.port", DEFAULT_WS_PORT);
	 

	/*
	 * Demarrage du serveur de websocket en local pour recevoir les appels du
	 * navigateur
	 */

	OmsService oms = null;
	WebSocketImpl.DEBUG = false;
	
	  try { 
		  oms = new OmsService(); 
		  oms.start(); 
		  dort(500);
		  logger.info("OmsGateway started on port: " + oms.getPort());
	  
	  } catch (InterruptedException | IOException e) {
		  System.out.println("Erreur au lancement du serveur"); 
	  }
	
	}

	public OmsService() throws InterruptedException, IOException {

		super(new InetSocketAddress(new Integer(portWs).intValue()));

		// Demarrage de la conference
		// conf = new OmsConference("conf1");
		// enregistrement de la conférence
		// prévoir de demarrer l'enregistrement a l'arrivee d'un participant
		// et arreter a la sortie du dernier
		// conf.record("/tmp/conf1.wav");
		// Initialisation de la table des appels et de l'annuaire
		// L'annuaire pourrait etre une BDD ou un fichier xml ou autre
		// Dans un premier temps, c'est en memoire

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

		calls = new HashMap<WebSocket, OmsCall>();
		annuaire = new HashMap<String, WebSocket>();
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		logger.info("NOUVEAU CLIENT : " +
		conn.getRemoteSocketAddress().getAddress().getHostAddress());
		
		String ipAddress = conn.getRemoteSocketAddress().getAddress().getHostAddress();
		// Arrivee d'un appel. On ne connait que conn
		// On instancie un OmsCall et on le stocke dans la table des OmsCall
		OmsCall call = new OmsCall(conn, ipAddress);
		calls.put(conn, call);
		monServiceWtc = new MonServiceWtc(hostVip,portVip);
		
		/*try {
	
			OmsCall ca = new OmsCall();		
			ca.addOmsCallListener(monServiceWtc);
			ca.fireEvent(ca, ip);
			ca.removeOmsCallListener(monServiceWtc);
		} catch (OmsException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		
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
		//hasClientPressDisc = false;
	}

	@Override
	public void onMessage( WebSocket conn, String message ) {
		// Arrivee d'un message
		// On instancie la classe Message
		// On regarde de quel type est le message sdp ou commande
		// Si sdp, on etablie automatiquement l'appel
		logger.info("NAV ==> AS : " + message );
		OmsCall call = calls.get(conn);
		
		//OmsMessage msg = new OmsMessage(message);
		//String typeMesg = msg.getTypeMsg();
		/*switch (typeMesg) {
			case "sdp":
				// Le message est du sdp
				String sdp = msg.getSdp();
			try {				
				if(!isAnswer){
					call.connect(hostVip,portVip);
					dort(1000);
					call.init(sdp);
					call.say("Bienvenue sur le serveur de conference", false);
				}else{
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
				//traiteCmd(conn, msg);
				break;
			default :
				// Le message est de type inconnu
				logger.error("Format de message inconnu : " + message + ". Ce n'est pas du JSON");*/
			
		try {
			//MonServiceWtc monServiceWtc = new MonServiceWtc(call,hostVip,portVip);
			OmsMessage msg = new OmsMessage();		
			msg.addOmsMessageListener(monServiceWtc);
			msg.fireEvent(call, message);
			msg.removeOmsMessageListener(monServiceWtc);
		} catch (OmsException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
		
	//}

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
	
	public OmsCall getOmsCall() {
		return call;
	}

	public void setOmsCall(OmsCall omsCall) {
		call = omsCall;
	}

	public static void logError(String attendu, String recu) {
		logger.error("ERREUR TEST : Reponse OMS incorrecte\n"
				+ "        Attendu : " + attendu + "\n        Recu    : "
				+ recu);
	}

}
