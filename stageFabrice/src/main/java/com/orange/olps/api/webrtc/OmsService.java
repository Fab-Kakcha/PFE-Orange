/**
 * 
 */
package com.orange.olps.api.webrtc;

/**
 * @author JWPN9644
 * 
 */

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import orange.olps.svi.navigation.NavigationManager;

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.JsonObject;

public class OmsService extends WebSocketServer {

	private static HashMap<WebSocket, OmsCall> calls = null;
	private static HashMap<WebSocket, OmsClientSvi> clientsSvi = null;
	private static HashMap<String, WebSocket> annuaire = null;
	private boolean bool;
	
	private static Logger logger = Logger.getLogger(OmsService.class);
	
	static OmsService serveur = null;
	private List<OmsMessageListener> _listeners = new ArrayList<OmsMessageListener>();

	public synchronized void addEventListener(OmsMessageListener listener)  {
		_listeners.add(listener);
	}
	public synchronized void removeEventListener(OmsMessageListener listener)   {
		_listeners.remove(listener);
	}

	public OmsService(int port) {
		super( new InetSocketAddress( port ) );
		WebSocketImpl.DEBUG = false;
		
		calls = new HashMap<WebSocket, OmsCall>();
		clientsSvi = new HashMap<WebSocket, OmsClientSvi>();
		annuaire = new HashMap<String, WebSocket>();
	}


	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
		
		String ipAddress = conn.getRemoteSocketAddress().getAddress().getHostAddress();
		logger.info("NOUVEAU CLIENT: " + ipAddress);
		
		// Arrivee d'un appel. On ne connait que conn
		// On instancie un OmsCall et on le stocke dans la table des OmsCall
		OmsCall call = new OmsCall(conn, ipAddress);
		calls.put(conn, call);
	}

	@Override
	public void onMessage( WebSocket conn, String message ) {
		System.out.println("Reception : " + conn.getRemoteSocketAddress().getAddress().getHostAddress() 
				+ " : " + message );
		
		OmsCall call = calls.get(conn);		
		if(message.indexOf("disconnect") != -1)
			call.setHasClientPressDisc(true);
		
		OmsMessageEvent msgEvent = new OmsMessageEvent(call, message);
		Iterator<OmsMessageListener> i = _listeners.iterator();
		while(i.hasNext())  {
			((OmsMessageListener) i.next()).omsMessagePerformed(msgEvent);
		}
	}

	@Override
	public void onClose( WebSocket conn, int code, String reason, boolean remote ) {		
		logger.info("SORTIE DE CLIENT : "
				+ conn.getRemoteSocketAddress().getAddress().getHostAddress());
		
		// Quand un client se deconnecte, on detruit tout ce qui lui appartient
		OmsCall call = calls.get(conn);
		bool = call.getHasClientPressDisc();
		
		if(!bool){
			JsonObject json = new JsonObject();
			json.addProperty("cmd", "disconnect");
			json.addProperty("param", call.getUserName());
			String message = json.toString();
			
			OmsMessageEvent msgEvent = new OmsMessageEvent(call, message);
			Iterator<OmsMessageListener> i = _listeners.iterator();
			while(i.hasNext())  {
				((OmsMessageListener) i.next()).omsMessagePerformed(msgEvent);
			}	
		}

		calls.remove(conn);
		conn.close();
	}

	@Override
	public void onError( WebSocket conn, Exception ex ) {
		ex.printStackTrace();
	}
}

