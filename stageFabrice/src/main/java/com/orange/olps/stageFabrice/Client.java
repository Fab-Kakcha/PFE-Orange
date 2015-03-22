package com.orange.olps.stageFabrice;

import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;

/*
 * Nouvelle version : Le client est maintenant un thread, ce qui permet
 * de lancer plusieurs appels en meme temps et donc plusieurs clients en
 * parallele. Chaque client a une connexion particuliere a OMS
 */

public class Client extends Thread {

	private static Logger logger = Logger.getLogger(Client.class);

	private String hostVip = null;
	private String portVip = null;
	private WebSocket conn = null; //Pour envoyer des messages au navigateur Chrome
	private VipConnexion vip = null;
	private boolean sucre = false;

	public Client(WebSocket c, String s, String p) {
		hostVip = s;
		portVip = p;
		conn = c;
	}
	
	/*
	 * Lancement du thread
	 * 
	 */
	
	public void run() {
		try {
			vip = new VipConnexion(hostVip, portVip);
		} catch (OmsException e1) {
			logger.error("Connexion a OMS impossible");
		}
	}
	
	public WebSocket getConn() {
		return conn;
	}
	
	public void setVip(VipConnexion v) {
		vip = v;
	}
		
	public VipConnexion getVip() {
		return vip;
	}
	
	/*
	 * Code VIP execute sur appel de la DTMF 1
	 */
	
	public void dtmf1() {
		try {
			if (sucre) {
				vip.getReponse("mt1 shutup");
				vip.getReponse("s say \"Plouf !\"");
				dort(2000);
				vip.getReponse("mt1 shutup");
				vip.getReponse("delete mt1");
				vip.getReponse("delete s");
				vip.getReponse("wait evt=*");
				conn.close();
			} else {
				vip.getReponse("mt1 shutup");
				vip.getReponse("s say \"Attention, le chocolat est trai chaud!\"");
				dort(3000);
				vip.getReponse("mt1 shutup");
				vip.getReponse("delete mt1");
				vip.getReponse("delete s");
				vip.getReponse("wait evt=*");
				conn.close();
			}
		} catch (OmsException e) {
			logger.error("Erreur de vocalisation");
			e.printStackTrace();
		}
	}
	
	/*
	 * Code VIP execute sur appel de la DTMF 2
	 */
	
	public void dtmf2() {	
		try {
			if (sucre) {
				vip.getReponse("mt1 shutup");
				vip.getReponse("s say \"Bon cafez ! Et a la prochaine fois sur OMS.\"");
				dort(3000);
				vip.getReponse("mt1 shutup");
				vip.getReponse("delete mt1");
				vip.getReponse("delete s");
				vip.getReponse("wait evt=*");
				conn.close();
			} else {
				vip.getReponse("mt1 shutup");
				vip.getReponse("s say \"Vous avez demandez du cafez. Voulez-vous du sucre ?\"");
				vip.getReponse("s say \"Pour un sucre, tappez 3. Sans sucre, tappez 4.\"");
				sucre = true;
			}
		} catch (OmsException e) {
			logger.error("Erreur de vocalisation");
			e.printStackTrace();
		}
	}
	
	/*
	 * Code VIP execute sur appel entrant.
	 * Le navigateur fournit son SDP a l'AS qui l'dapte et le renvoie a OMS
	 * OMS retourne son SDP
	 */
	
	void appelEntrant(String sdp) {
		try {
			vip.getReponse("new mt1 webrtc");
			vip.getReponse("mt1 setparam escape_sdp_newline=true");

			vip.getReponse("mt1 generate type=answer \"content=" + sdp + "\"");
			vip.getReponse("mt1 status");
			String rep = vip.getReponse("wait evt=mt1.answered");

			logger.info("(OMS's sdp received) OMS --> AS : " + rep);//OMS's sdp received by AS
			
			StringTokenizer str =  new StringTokenizer(rep, "\"");
			str.nextToken();
			String sdpOms = str.nextToken().replaceAll("\\\\n","\\n").replaceAll("\\\\r","\\r");
			String sdpToReturn = "{\"sdp\":{\"type\":\"answer\",\"sdp\":\"" + sdpOms.substring(12) + "\"}}";
			conn.send(sdpToReturn);
			logger.info("(OMS's sdp parsed in JSON) AS --> NAV : " + sdpToReturn);

			vip.getReponse("new s synt");
			vip.getReponse("wait evt=mt1.mediaconnected");
			String statusRep = vip.getReponse("mt1 status");
			
			logger.info("OMS --> AS (status rvi webrtc: " + statusRep);
			
			vip.getReponse("mt1 setparam bind=s");
			vip.getReponse("s say \"Bonjour test reussit pour le moment\"");
			vip.getReponse("mt1 shutup");
			dort(3000);			
			vip.getReponse("delete mt1");
//			vip.getReponse("delete mt1");
			dort(10000);
		} catch (OmsException e) {
			logger.error("Probleme OMS : " + e.getMessage());
		}
/*		
		try {
			vip.getReponse("new mt1 webrtc");
			vip.getReponse("mt1 setparam escape_sdp_newline=true");
			vip.getReponse("mt1 generate type=answer \"content=" + sdp + "\"");
			vip.getReponse("mt1 status");
			String rep = vip.getReponse("wait evt=mt1.answered");

			logger.info("OMS --> AS : " + rep);
			
			StringTokenizer str =  new StringTokenizer(rep, "\"");
			str.nextToken();
			String sdpOms = str.nextToken().replaceAll("\\\\n","\\n").replaceAll("\\\\r","\\r");
			String sdpToReturn = "{\"sdp\":{\"type\":\"answer\",\"sdp\":\"" + sdpOms.substring(12) + "\"}}";
			conn.send(sdpToReturn);
			logger.info("AS --> NAV : " + sdpToReturn);

			vip.getReponse("new s synt");
			vip.getReponse("wait evt=mt1.mediaconnected");
			vip.getReponse("mt1 status");
			vip.getReponse("mt1 setparam bind=s");
			vip.getReponse("s say \"Bienvenu sur le distributeur de boisson OMS !\"");
//			vip.getReponse("wait evt=mt1.starving");
//			vip.getReponse("wait status=mt1.starve");
			vip.getReponse("s say \"Pour du chocolat, tappez 1 !\"");
			vip.getReponse("s say \"Pour du cafez, tappez 2 !\"");
			dort(5000);
		} catch (OmsException e) {
			logger.error("Probleme OMS : " + e.getMessage());
		}
*/
	}


	/*
	 * Code VIP execute sur appel demande d'appel sortant.
	 * Le navigateur se connecte a l'AS et envoie l'ordre "Appelle"
	 * L'AS fait une demande d'offre SDP a OMS.
	 * L'AS transfere la reponse au navigateur qui repond avec son SDP
	 * La communication s'etablie.
	 * 
	 * 		C'est bien l'AS qui initialise l'appel
	 */
	
	void traiteOffer() {
		try {
			vip.getReponse("new mt2 webrtc");
			vip.getReponse("mt2 setparam escape_sdp_newline=true");
			vip.getReponse("mt2 generate type=offer");
			String rep = vip.getReponse("wait evt=mt2.offered");
			logger.info("OMS --> AS : " + rep);

			StringTokenizer str =  new StringTokenizer(rep, "\"");
			str.nextToken();
			String sdpOms = str.nextToken().replaceAll("\\\\n","\\n").replaceAll("\\\\r","\\r");
			String sdpToReturn = "{\"sdp\":{\"type\":\"offer\",\"sdp\":\"" + sdpOms.substring(11) + "\"}}";
			conn.send(sdpToReturn);
			logger.info("AS --> NAV : " + sdpToReturn);
		} catch (OmsException e) {
			logger.error("Probleme OMS : " + e.getMessage());
		}
	}
	
	/*
	 * C'est la suite de l'appel precedent. Apres avoir recu le SDP d'OMS
	 * le navigateur retourne le sien 
	 */
	
	void update(String sdp) {
		try {
			vip.getReponse("mt2 update type=remote-answer \"content=" + sdp + "\"");
			vip.getReponse("wait evt=mt2.mediaconnected");
			vip.getReponse("new s synt");
			vip.getReponse("mt2 setparam bind=s");
			vip.getReponse("s say \"coucou. Bienvenu sur le serveur vocal OMS !\"");
			dort(6000);
			vip.getReponse("mt2 status");
			vip.getReponse("mt2 shutup");
			vip.getReponse("delete mt2");
			vip.getReponse("delete s");
		} catch (OmsException e) {
			logger.error("Probleme OMS : " + e.getMessage());
		}
	}

	// Appel sortant vers Thierry. Demande le SDP OMS et l'envoie a Thierry
	void appele(WebSocket thierry) {
		try {
			vip.getReponse("new mt2 webrtc");
			vip.getReponse("mt2 setparam escape_sdp_newline=true");
			vip.getReponse("mt2 generate type=offer");
			String rep = vip.getReponse("wait evt=mt2.offered");
			logger.info("OMS --> AS : " + rep);

			StringTokenizer str =  new StringTokenizer(rep, "\"");
			str.nextToken();
			String sdpOms = str.nextToken().replaceAll("\\\\n","\\n").replaceAll("\\\\r","\\r");
			String sdpToReturn = "{\"sdp\":{\"type\":\"offer\",\"sdp\":\"" + sdpOms.substring(12) + "\"}}";
			thierry.send(sdpToReturn);
			logger.info("AS --> NAV : " + sdpToReturn);
		} catch (OmsException e) {
			logger.error("Probleme OMS : " + e.getMessage());
		}
	}

	// Message : Thierry n'est pas connectez
	void raccroche() {
		try {
			vip.getReponse("s say \"sory, Thierry n'est pas connectez.\"");
			dort(2000);
			vip.getReponse("mt1 shutup");
			vip.getReponse("delete mt1");
			vip.getReponse("delete s");
			conn.close();
		} catch (OmsException e) {
			logger.error("Probleme OMS : " + e.getMessage());
		}
	}
	
	// Appel sortant vers Thierry qui vient de nous renvoyer son SDP
	void updateThierry(String sdp) {
		try {
			vip.getReponse("mt2 update type=remote-answer \"content=" + sdp + "\"");
			vip.getReponse("wait evt=mt2.mediaconnected");
			vip.getReponse("new s synt");
			vip.getReponse("mt2 setparam bind=s");
			vip.getReponse("s say \"coucou. Bienvenu sur le serveur vocal OMS !\"");
			dort(6000);
			vip.getReponse("mt2 status");
			vip.getReponse("mt2 shutup");
			vip.getReponse("delete mt2");
			vip.getReponse("delete s");
		} catch (OmsException e) {
			logger.error("Probleme OMS : " + e.getMessage());
		}
	}

	// Appel sur 10.184.155.224 G7 webRTC
	void appelExterne() {
		try {
			vip.getReponse("new t sip");
			vip.getReponse("new mt media");
			vip.getReponse("mt reserveport type=audio");
			vip.getReponse("t dial sig=to=sip:0556530256@10.184.153.56 \"media=ip=10.184.155.224 aport=6012\"");
			vip.getReponse("wait evt=*");
			vip.getReponse("mt startlocal type=audio codec=G711A codecparam=payload=8;ptime=20;dtmfpayload=116");
			vip.getReponse("mt startremote type=audio host=10.184.153.55 port=8308 codec=G711A codecparam=payload=8;ptime=20;dtmfpayload=116");
			vip.getReponse("new s2 synt");
			vip.getReponse("mt setparam bind=s2");
			vip.getReponse("s2 say \"Quelqu'un cherche a vous joindre !\"");
			dort(6000);
			vip.getReponse("mt1 setparam bind=mt");			
		} catch (OmsException e) {
			logger.error("Probleme OMS : " + e.getMessage());
		}
	}

	// Appel vers 05 56 53 02 56 seul
	void appelMobile() {
		try {
			vip.getReponse("new t sip");
			vip.getReponse("new mt media");
			vip.getReponse("new s synt");
			String rep = vip.getReponse("mt reserveport type=audio");
			String port = rep.substring(rep.length()-4);
			vip.getReponse("t dial sig=to=sip:0556530256@10.184.153.56 \"media=ip=10.184.155.226 aport=" + port + "\"");
			rep = vip.getReponse("wait evt=*");
			int pos = rep.indexOf("aport");
			port = rep.substring(pos+6, pos+10);
			vip.getReponse("mt startlocal type=audio codec=G711A codecparam=payload=8;ptime=20;dtmfpayload=116");
			vip.getReponse("mt startremote type=audio host=10.184.153.55 port=" + port + " codec=G711A codecparam=payload=8;ptime=20;dtmfpayload=116");
			vip.getReponse("wait evt=*");
			vip.getReponse("s say \"Quelqu'un cherche a vous joindre !\"");
			vip.getReponse("wait evt=mt.starving");
		} catch (OmsException e) {
			logger.error("Probleme OMS : " + e.getMessage());
		}
	}


	public static void dort( int temps) {
		try {
			Thread.sleep(temps);
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
}
