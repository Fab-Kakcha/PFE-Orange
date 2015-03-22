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

//public class Client950 extends Client implements DtmfListener{

	//Call call = null;
	//Static Conf conf950 = null;
	//Client client = null;

	/*public Client950() {
		// Connexion a OMS
		super();
		
		connect();
		addDtmfListener(this);
		listen(call);
		Enreg enreg = new Enreg("record.wav");
		waitCall();
		logger.info("Numero appelant : " + call.getCaller();
		//R�cup�rer les donn�es de l'appel : appelant, appel�
		accueil();
	}*/
	
	
	
	/*public void DtmfPerformed(DtmfEvent evt) {
		String dtmf = evt.getDtmf();
		switch (position) [
			case "accueil" :
				internetMenu(dtmf);
				break;
			case "internet" :
				internet(dtmf);
				break;
			case "tv" : 
				tv(dtmf);
				break;
			default : 
				badChoice(dtmf);
		}
	}*/
	
	/*public void internetMenu(Dtmf dtmf) {
		switch (dtmf) {
			case "1" :
				enreg.start();
				callTC("internet");
				break;
			case "2" :
				play("offres.a8k");
				break;
			case "3" :
				try {
					conf = new Conf("1234", ...);
					conf.join();*/
/*
 *	Appel sortant. 
 *  Chaque type de r�sultat (occupation, messagerie, erreur) correspond � une exception 
*/
						
	/*				client = new Client();
					try {
						client.call("0612345678");
					} catch (CallBusyException c) {
						say "Votre correspondant est occupe");
						end();
					} catch (CallRecorderException r) {
						client.say("Bonjour, votre coli est arrive a notre agence. Merci de le retirer");
						end();
					} catch (CallFailledException f) {
						log ("Erreur lors de l'appel au 0612345678"):
						end();
					}

					client
					conf.join(client);
				catch (ConfAlreadyExistsException e) {
				}
				conf.add(this);
				conf.muteMe(this);
				break;
			default :
				badChoice(dtmf);
		}*/
	
	
	/*public void accueil() {
		setBargin(true);
		String position = "accueil";
		say("Bonjour, bienvenue sur le 1014");
		say("Pour internet, tappez 1");
		say("Pour la TV d'orange, tappez 2");
		say("pour rentrer en conference avec les autres participants, tappez 3");
		terminaisonPrompt();
	}*/
	
	/*public void internet() {
		setBargin(true);
		String position = "internet";
		say("Pour ecouter les offres du moment, tappez 1");
		say("Pour un TC, tappez 2");
		terminaisonPrompt();
	}*/
	
	/*public void badChoice(dtmf) {
		switch (dtmf) {
			case "*" :
			case "#" :
			default :
	}
}*/

/*
 *	Description d'un appel
 *
*/
/*public class Call {
	String caller = null;
	String callee = null;
	String codec = null;
	...
	
	public String getCaller() {
		return caller;
	}
	...
}*/
	