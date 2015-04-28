package com.orange.olps.api.webrtc;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import orange.olps.svi.bdd.ConnexionManager;
import orange.olps.svi.config.Config;
import orange.olps.svi.guide.ConfigGuide;
import orange.olps.svi.navigation.NavigationManager;
//import orange.olps.svi.sms.SMSUtil;
import orange.olps.svi.util.Util;
//import orange.olps.svi.web.WebManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class InitService {
	
	
	private static java.util.Timer timer = null;;
	private static String nomProperties = "Svi.properties";
			//"C:\\Users\\JWPN9644\\workspace\\stageFabrice";
	/**
	 * nom de la variable d'environnement contenant le chemin 
	 * du 'current'
	 */
	private String varEnvRep = "PROPERTIES_FILES";

	protected static Log logger = null;

	/**
	 * Lancement des fonctions d'initialisation du SVI
	 * @param premierInit : au premier passage le ConnexionManager a déja ete initialise dès qu'on l'utilise
	 */
	
	public InitService(){
		
		initialiserSvi();
	}
	
	public void initialiserSvi() {		
		if (logger == null) {
			logger = LogFactory.getLog(InitService.class.getName());
		}
		if (timer !=null) {
			timer.cancel();
		}

		Config.getInstance().initialiser(nomProperties, varEnvRep);   /* initialisation fichier properties */

		ConnexionManager.supprimer();
		ConnexionManager.initialiser(); /* initialisation des connexions base (s'il y a lieu)*/

		//StatManager.getInstance().initialiser(); // initialisation des stats

		// Recopie des prompts
		ConfigGuide.getInstance().initialiser();

		Util.reset();

		// chargement de la navigation
		NavigationManager.getInstance().initialiser();

		// rechargement de la config http
		//if (NavigationManager.getInstance().isUseWebSvc() || StatManager.isWriterHttp()) {
			//logger.info("initialiserSvi() - Demarrage du WebManager");
			//WebManager.getInstance().initialiser();
		//}

		if (NavigationManager.getInstance().isUseSms()) {
			logger.info("initialiserSvi() - Demarrage de SMSUtil");
			//SMSUtil.getInstance().initialiser();
		}
		new Date();
		// timer de reinitialisation periodique
		timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				initialiserSvi();						
			}
		}, Config.getInstance().calculerTempsDodo());

	}
}
