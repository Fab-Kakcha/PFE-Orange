package com.orange.olps.stageFabrice;

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
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;


public class OmsGateway extends WebSocketServer {

	private static Logger logger = Logger.getLogger(OmsGateway.class);
	private static final String DEFAULT_OMS_HOST = "127.0.0.1";
	private static final String DEFAULT_OMS_PORT = "8080";
	private static final String DEFAULT_WS_PORT = "8887";
	//private static final String WEBRTC_CONF = "/opt/application/64poms/current/conf/";
	private static final String WEBRTC_CONF = "C:\\Users\\JWPN9644\\opt\\application\\64poms\\current\\conf\\";
	private static String hostVip = "127.0.0.1";
	private static String portVip = "4670";
	private static String portWs = "8887";
	private static HashMap<WebSocket, Client> clients = new HashMap<WebSocket, Client>();

	static Properties prop = new Properties();
	static FileInputStream propFic = null;
	static BufferedWriter ficSdp = null;
	
	static WebSocket thierry = null;

	boolean aucun = true;
	boolean sucre = false;
	boolean appelEntrant = true;
	boolean omsConnected = false;

	public static void main(String[] args) {

		/*
		 * Initialisation des parametres depuis le fichier de properties
		 */

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

		logger.info("host Vip: " + hostVip + "port Vip: " + portVip);
		
		
		/*
		 * Demarrage du serveur de websocket en local pour recevoir les appels du navigateur
		 */

		OmsGateway oms = null; 
		WebSocketImpl.DEBUG = false;
		
		try {
			oms = new OmsGateway();
			oms.start();
			dort(500);
			logger.info( "OmsGateway started on port: " + oms.getPort() );
		} catch (InterruptedException | IOException e) {
			System.out.println("Erreur au lancement du serveur");
		}
	}

	public OmsGateway() throws InterruptedException , IOException {

		super( new InetSocketAddress( new Integer(portWs).intValue() ) );

	}

	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
		logger.info("NOUVEAU CLIENT : " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
	}

	@Override
	public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
		logger.info("SORTIE DE CLIENT : " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
		clients.remove(conn);
	}

	@Override
	public void onMessage( WebSocket conn, String message ) {
		logger.info("Cli(NAV) ==> AS : " + message );

		Message msg = new Message(message);

		if (msg.isSdp()){

			String sdp = msg.getSdp();
			
			logger.info("sdp Cli transformed : " + sdp); //The sdp's client is being put in correct form

			if (conn == thierry) clients.get(conn).updateThierry(sdp);
			else {
				if (appelEntrant) clients.get(conn).appelEntrant(sdp);
				else {
					clients.get(conn).update(sdp);
					appelEntrant = true;
				}
			}

		} else {	

			if (! msg.isJson()) {
				switch (message) {
				case "appel":
					appelEntrant = false;
					newCall(conn);
					logger.info("Demande pour appel sortant");
					clients.get(conn).traiteOffer();
					break;
				case "newCall":
					logger.info("Nouvel appel de : " + conn.getRemoteSocketAddress().getHostString());
					newCall(conn);
					break;
				case "dtmf1":
					clients.get(conn).dtmf1();
					logger.info("Saisie de dtmf 1");
					break;
				case "dtmf2":
					clients.get(conn).dtmf2();
					logger.info("Saisie de dtmf 2");
					break;
				case "Appelle":
					appelEntrant = false;
					logger.info("Appel sortant demand� par navigateur");
					newCall(conn);
					clients.get(conn).traiteOffer();
					break;
				case "appelerExterne":
					logger.info("Aboutement vers un n� externe");
					clients.get(conn).appelExterne();
					break;
				case "appelerMobile":
					logger.info("Demande d'appel vers un fixe ou un mobile ");
					newCall(conn);
					clients.get(conn).appelMobile();
					break;
				case "appelerThierry":
					if (thierry == null) {
						logger.error("Thierry n'est pas connectez");
						clients.get(conn).raccroche();
					}
					clients.get(conn).appele(thierry);
					logger.info("Appel de Thierry");
					break;
				case "connexionThierry":
					logger.info("Connexion de Thierry : " + conn.getRemoteSocketAddress().getHostString());
					thierry = conn;
					Set<WebSocket> listeCli = clients.keySet();
					Iterator<WebSocket> ite = listeCli.iterator();
					
					while (ite.hasNext()) {
						WebSocket ws = ite.next();
						if (ws != thierry) ws.send("ThierryConnected");
					}
					break;
				default:
					if (message.startsWith("TEST")) {
						logger.info("TRACE QUALIF : " + message);
					} else {
						logger.error("Message inconnu : " + message);
					}
				}
			} else {
				logger.error("Message Json inconnu : " + message);
			}
		}
	}

	public void onFragment( WebSocket conn, Framedata fragment ) {
		logger.info( "received fragment: " + fragment );
	}

	@Override
	public void onError( WebSocket conn, Exception ex ) {
		ex.printStackTrace();
	}

	public static void dort( int temps) {
		try {
			Thread.sleep(temps);
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	public static void newCall( WebSocket conn) {
		Client appelant = new Client(conn, hostVip, portVip);
		appelant.run();
		clients.put(conn, appelant);
	}
	
	public static void logError(String attendu, String recu) {
		logger.error("ERREUR TEST : Reponse OMS incorrecte\n" + 
				"        Attendu : " + attendu +
				"\n        Recu    : " + recu);		
	}
	
}

