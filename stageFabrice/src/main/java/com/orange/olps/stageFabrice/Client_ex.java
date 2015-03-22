package com.orange.olps.stageFabrice;

//import java...;
//import org....;
/*
*	Librairie orange com.orange.olps.OmsApi.jar
*/

//import com.orange.olps.ServiceOms;
//import com.orange.olps.ClientOms;


/*
 * Nouvelle version : Le client est maintenant un thread, ce qui permet
 * de lancer plusieurs appels en meme temps et donc plusieurs clients en
 * parallele. Chaque client a une connexion particuliere a OMS
 */

/*public class Client_ex extends Thread {

	int nbClients;	// Valeurs r�cup�r�es d'un fichier de properties
	int portClient;
	String serviceName;
	Service srv950 = new Service(nbClients, portClient, serviceName);
	srv950.start();
	//srv950.
	public Client_ex() {
		OmsConnexion oms = new OmsConnexion("10.184.155.224", 2470);
		
	}*/
	
	/*public void connect() {
		oms.getReponse("new t1 sip");
	    oms.getReponse("new mt1 media");
	}*/
	
	// OK evt=t1.incoming withsdpoffer=yes 100rel=none local=sip:950@10.184.155.154 remote=sip:0685198936@dps.com uri-params= mediatypes= call-id=ODY3ZGRlNjU2OGVjY2JmNGM0M2UyOWMyNjhmZWI4ZGU. headertest=

	/*public void listen(Call c) {
	    oms.getReponse("t1 listen sip:9*@");
	    String reponse = oms.getReponse("wait evt=t1.*,mt1.*");
		c.setCaller(reponse.getRemote(� �crire comment on r�cup�re le remote dans la r�ponse)));
		c.setCallee(reponse.getLocal(� �crire comment on r�cup�re le local dans la r�ponse);
		c.setCallId(reponse.getCallId());
	}*/
	
	/*public void play(String fileToPlay) {
		if ( ! existeS3) oms.getReponse("new s3 synt.synt");
		oms.getReponse("s3 play file=\"" + fileToPlay + "\"");
	}*/
	
	/*public void terminaisonPrompt() {
		say("Pour revenir � l'accueil, faites di�se");
		say("Pour r��couter, faites �toile");
	}*/
	// int getDtmf(
	
