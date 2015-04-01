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

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class OmsService extends WebSocketServer {

	protected static HashMap<WebSocket, OmsCall> calls = null;
	protected static HashMap<String, WebSocket> annuaire = null;
	
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
		annuaire = new HashMap<String, WebSocket>();
	}

	@Override
	public void onMessage( WebSocket conn, String message ) {
		System.out.println("Reception : " + conn.getRemoteSocketAddress().getAddress().getHostAddress() 
				+ " : " + message );
		
		//logger.info("NAV ==> AS : " + message );
		OmsCall call = calls.get(conn);
		
		OmsMessageEvent msgEvent = new OmsMessageEvent(call, message);
		Iterator<OmsMessageListener> i = _listeners.iterator();
		while(i.hasNext())  {
			System.out.println("on a trouvé un listener à qui envoyer");
			((OmsMessageListener) i.next()).omsMessagePerformed(msgEvent);
		}
	}

	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
		
		String ipAddress = conn.getRemoteSocketAddress().getAddress().getHostAddress();
		logger.info("NOUVEAU CLIENT : " + ipAddress);
					
		// Arrivee d'un appel. On ne connait que conn
		// On instancie un OmsCall et on le stocke dans la table des OmsCall
		OmsCall call = new OmsCall(conn, ipAddress);
		calls.put(conn, call);
	}

	@Override
	public void onClose( WebSocket conn, int code, String reason, boolean remote ) {		
		logger.info("SORTIE DE CLIENT : "
				+ conn.getRemoteSocketAddress().getAddress().getHostAddress());

		// Quand un client se deconnecte, on detruit tout ce qui lui appartient
		//OmsCall call = calls.get(conn);

		calls.remove(conn);
		conn.close();
	}

	@Override
	public void onError( WebSocket conn, Exception ex ) {
		ex.printStackTrace();
	}
}

