package com.orange.olps.api.webrtc;


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


public class OmsServiceSvi extends WebSocketServer{	
	
	/**
	 * @author JWPN9644
	 * 
	 */
	
	private static HashMap<WebSocket, OmsClientSvi> clientsSvi = null;
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

	public OmsServiceSvi(int port) {
		super( new InetSocketAddress( port ) );
		WebSocketImpl.DEBUG = false;
		
		clientsSvi = new HashMap<WebSocket, OmsClientSvi>();
	}

	@Override
	public void onMessage( WebSocket conn, String message ) {
		System.out.println("Reception : " + conn.getRemoteSocketAddress().getAddress().getHostAddress() 
				+ " : " + message );			
		
		OmsClientSvi omsClientSvi = clientsSvi.get(conn);
		if(message.indexOf("disconnect") != -1)
			omsClientSvi.setHasClientPressDisc(true);
		
		OmsMessageEvent msgEvent2 = new OmsMessageEvent(omsClientSvi, message);
		Iterator<OmsMessageListener> i2 = _listeners.iterator();
		while(i2.hasNext())  {
			((OmsMessageListener) i2.next()).omsMessagePerformed(msgEvent2);
		}
	}

	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
		
		String ipAddress = conn.getRemoteSocketAddress().getAddress().getHostAddress();
		logger.info("NOUVEAU CLIENT SVI: " + ipAddress);	
		
		OmsClientSvi clientSvi = new OmsClientSvi(conn, ipAddress,"1", "723", "null");		
		String paramNavigation = NavigationManager.getInstance().getRacineSvc(clientSvi.getService());
		clientSvi.setNavCourante(paramNavigation);
		
		clientsSvi.put(conn, clientSvi);
	}

	@Override
	public void onClose( WebSocket conn, int code, String reason, boolean remote ) {		
		logger.info("SORTIE DE CLIENT : "
				+ conn.getRemoteSocketAddress().getAddress().getHostAddress());
		
		// Quand un client se deconnecte, on detruit tout ce qui lui appartient
		OmsClientSvi omsClientSvi = clientsSvi.get(conn);
		bool = omsClientSvi.getHasClientPressDisc();
		
		if(!bool){
			JsonObject json = new JsonObject();
			json.addProperty("cmd", "disconnect");
			json.addProperty("param", omsClientSvi.getUserName());
			String message = json.toString();
			
			OmsMessageEvent msgEvent = new OmsMessageEvent(omsClientSvi, message);
			Iterator<OmsMessageListener> i = _listeners.iterator();
			while(i.hasNext())  {
				((OmsMessageListener) i.next()).omsMessagePerformed(msgEvent);
			}	
		}
		
		clientsSvi.remove(conn);
		conn.close();
	}

	@Override
	public void onError( WebSocket conn, Exception ex ) {
		ex.printStackTrace();
	}			
}
