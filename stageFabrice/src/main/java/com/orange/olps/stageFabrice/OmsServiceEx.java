package com.orange.olps.api.webrtc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

//import com.orange.olps.OmsException;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;

public class OmsServiceEx extends WebSocketServer {

	private static Logger logger = Logger.getLogger(OmsServiceEx.class);
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
	private static String portVipConf ;

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
	
	
	///private <Class <T> implements OmsMessageListener> t = new <Class <T> implements OmsMessageListener>();
	
	//Définir un truc ici pour dire toutes classes qui implémente une interface
	
	
	private MonServiceWtc monServiceWtc;

	//public static void main(String[] args) {

	/*
	 * Initialisation des parametres depuis le fichier de properties
	 */

	/* PropertyConfigurator.configure(WEBRTC_CONF + "log4j.properties");
	  
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
	  prop.getProperty("ws.port", DEFAULT_WS_PORT);*/
	 

	/*
	 * Demarrage du serveur de websocket en local pour recevoir les appels du
	 * navigateur
	 */

	/*OmsService oms = null;
	WebSocketImpl.DEBUG = false;
	
	  try { 
		  oms = new OmsService(); 
		  oms.start(); 
		  dort(500);
		  logger.info("OmsGateway started on port: " + oms.getPort());
	  
	  } catch (InterruptedException | IOException e) {
		  System.out.println("Erreur au lancement du serveur"); 
	  }
	
	}*/

	public OmsServiceEx() throws InterruptedException, IOException {

		//public OmsService(MonServicewtc monServiceWtc) throws InterruptedException, IOException
		super(new InetSocketAddress(new Integer(portWs).intValue()));

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
			// Demarrage de la conference
			conf = new OmsConference(hostVip, portVipConf);
			//conf.create("conf1");
		} catch (OmsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// enregistrement de la conférence
		// prévoir de demarrer l'enregistrement a l'arrivee d'un participant
		// et arreter a la sortie du dernier
		// conf.record("/tmp/conf1.wav");
		// Initialisation de la table des appels et de l'annuaire
		// L'annuaire pourrait etre une BDD ou un fichier xml ou autre
		// Dans un premier temps, c'est en memoire

		calls = new HashMap<WebSocket, OmsCall>();
		annuaire = new HashMap<String, WebSocket>();
		
		start();
		dort(500);
		logger.info("OmsGateway started on port: " + getPort());
		monServiceWtc = new MonServiceWtc(hostVip,portVip, conf);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		
		String ipAddress = conn.getRemoteSocketAddress().getAddress().getHostAddress();
		logger.info("NOUVEAU CLIENT : " + ipAddress);
			
		
		// Arrivee d'un appel. On ne connait que conn
		// On instancie un OmsCall et on le stocke dans la table des OmsCall
		OmsCall call = new OmsCall(conn, ipAddress);
		calls.put(conn, call);
		
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
	}

	@Override
	public void onMessage( WebSocket conn, String message ) {
		// Arrivee d'un message
		// On instancie la classe Message
		// On regarde de quel type est le message sdp ou commande
		// Si sdp, on etablie automatiquement l'appel
		
		logger.info("NAV ==> AS : " + message );
		OmsCall call = calls.get(conn);
			
		/*try {
			OmsMessage msg = new OmsMessage();		
			msg.addOmsMessageListener(monServiceWtc);
			//msg.addOmsMessageListener(new OmsMessageListener());
			msg.fireEvent(call, message);
			msg.removeOmsMessageListener(monServiceWtc);
		} catch (OmsException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
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