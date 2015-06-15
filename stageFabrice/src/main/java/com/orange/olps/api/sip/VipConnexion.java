package com.orange.olps.api.sip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

public class VipConnexion extends Connexion {
	
	private Logger logger = Logger.getLogger(VipConnexion.class);
	private Socket socket = null;
	private PrintWriter out = null;
	private BufferedReader in = null;

	
	/**
	 * To create a socket connection with OMS
	 * @param serveur serveur OMS's IP address
	 * @param portStr portStr OMS's port
	 * @throws OmsException
	 */
	public VipConnexion(String serveur, String portStr) throws OmsException {
		
		try {

			int port = new Integer(portStr).intValue();
			socket = new Socket(serveur,port);
			logger.info("Connexion au serveur : " + serveur + ":" + port);
			out = new PrintWriter(socket.getOutputStream());
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			out.println("info \"Client vibot java\"");
			out.flush();
			
			// On recupere la reponse "OK id=CON1082388" mais on ne l'affiche pas
			in.readLine();

		} catch (UnknownHostException e) {
			logger.error("Impossible de se connecter a l'adresse "+socket.getLocalAddress());
			throw new OmsException("Connexion au serveur impossible");
		} catch (IOException e) {
			logger.error("Aucun serveur a l'ecoute du port " + portStr + " sur le serveur " + serveur);
			throw new OmsException("Connexion au serveur impossible");
		}
	}
	
	public String getReponse(String question) throws OmsException {

		out.println(question);
		out.flush();

		String recu = null;
		try {
			recu = in.readLine();
			while (in.ready()) {
				String suite = in.readLine();
				try {
					if ( ! suite.equals("")) System.out.println(suite);
				} catch (NullPointerException n) {
					System.out.println("Deconnexion d'OMS");
					return "KO";
				}
			}
		} catch (IOException e) {
			System.out.println("Lecture impossible");
			throw new OmsException("Connexion au serveur impossible");
		}

		return recu;
	}


	public void send(String question) {
		System.out.println(">VIP: " + question);
		out.println(question);
		out.flush();
	}
	
	public Socket getSocket(){
		
		return this.socket;
	}
	
	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			logger.error("Erreur lors de la fermeture de la connexion OMS ");
			e.printStackTrace();
		}
	}
}
