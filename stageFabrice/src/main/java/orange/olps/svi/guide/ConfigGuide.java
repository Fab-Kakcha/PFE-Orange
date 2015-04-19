package orange.olps.svi.guide;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import orange.olps.svi.config.Config;
import orange.olps.svi.util.Util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConfigGuide  {

	private static  boolean sbooIsInit = false;

	/* liste des prompts presents jusqu'au prochain rechargement */
	private static List<String> listePromptOld;
	// singleton 
	private static ConfigGuide config ;


	protected static Log logger = LogFactory.getLog(ConfigGuide.class.getName());

	private ConfigGuide() {}

	/**
	 * Fonction permettant d'onbtenir l'instance du singleton
	 * elle est 'synchornized' pour que:
	 * 1) au premier appel un seul thread client fasse l'init
	 */
	public static  ConfigGuide getInstance() {		
		if (config == null) {
			synchronized (ConfigGuide.class) {
				if (config == null) config = new ConfigGuide();	
			}
		}		
		return config;
	}
	/**
	 * initialiser : passe le booleen a faux pour
	 * relancer l'init des fichiers audio
	 */
	public  void initialiser() {
		logger.debug("setInitialise - Entree - Relecture des prompts");
		synchronized (ConfigGuide.class) {			
			if (sbooIsInit) {
				logger.debug("getInstance - rechargement des prompts");
				/*
				 * Rechargement du fichiers fichiers audio
				 * demarre par le thread de Config
				 */
				rechargerFichierAudio();
				logger.debug("getInstance - rechargement Fini "+sbooIsInit);
			}					
			else {
				logger.debug("getInstance - initialisation du singleton");
				config = new ConfigGuide();	
				
				sbooIsInit = initialiserRepPrompt();;
			}

		}

	}
	/**
	 * Fonction de rechargement des fichiers audio demarre par le 
	 * thread de config
	 */
	private static void rechargerFichierAudio() {

		logger.debug("rechargerFichierAudio - Entree ");

		/* 
		 * On supprime les anciens fichiers audio
		 */
		if (listePromptOld != null) {
			supprimerAncienAudio();
			listePromptOld.clear();
		}
		else {
			listePromptOld = new ArrayList<String>();
		}
		String nomRepRef = Config.getInstance().getRepertoireAudioReference();
		File repRef = new File(nomRepRef);
		String [] tabFicRef = repRef.list();

		if (tabFicRef != null && tabFicRef.length != 0) {
			/* le repertoire de reference des fichiers audio n'est pas vide **/

			/*
			 * Il faut effectuer le delta entre les anciens et les nouveaux fichiers
			 */
			/* lecture du repertoire des fichiers audio actifs */
			String nomRepAudio = Config.getInstance().getRepertoireAudio();
			if (nomRepAudio == null)  {
				logger.error("rechargerFichierAudio - Repertoire cible inexistant");
				return;
			}
			File repAudio = new File(nomRepAudio);
			String [] tabFicAudio = repAudio.list();
			boolean boolTrouve = false;

			if (tabFicAudio != null) {
				int i,j;

				for (i = 0; i < tabFicAudio.length; i++) {
					boolTrouve = false;
					for (j = 0; j < tabFicRef.length; j++) {
						if (tabFicAudio[i].equals(tabFicRef[j])) {
							/* le meme fichier existe dans le referentiel et dans les actifs */
							boolTrouve = true;
							break;
						}
					}
					if (boolTrouve) {
						/* on recopie quand meme le fichier au cas ou le contenu aurait evolue*/
						Util.copier(nomRepRef+tabFicRef[j], nomRepAudio+tabFicRef[j]);
						tabFicRef[j] ="";
					}
					else {
						/* 
						 * Le fichier audio n'existe plus au referentiel
						 * On le conserve encore au cas ou des clients connectes
						 * l'utiliserait. Il ne sera supprime qu'au prochain rechargement
						 */
						listePromptOld.add(tabFicAudio[i]);
					}
				}
			}
			/* enfin on recopie tous les nouveaux fichiers */
			for (String ficRef : tabFicRef) {
				boolTrouve = false;
				if (!"".equals(ficRef)) {
					Util.copier(nomRepRef+ficRef, nomRepAudio+ficRef);
				}
			}
		}
		//Proprietes initialisees
		sbooIsInit = true;

	}

	/**
	 * Methode permettant de copier TOUS les fichiers audio 
	 * du repertoire de reference vers le repertoire actif
	 * Normalement elle ne les copie qu'au demarrage apres un redeploiment
	 * du logiciel sous jonas
	 */
	private static boolean  initialiserRepPrompt() {
		logger.debug("initialiserRepPrompt - Entree");		

		String nomRepOut = Config.getInstance().getRepertoireAudio();
		
		if (nomRepOut == null) {
			logger.error("initialiserRepPrompt - Repertoire cible inexistant");
			return false;
		}
		File repOut = new File(nomRepOut);
		File [] tabFic = repOut.listFiles();

		if (tabFic != null && tabFic.length != 0) {
			/* suppression des fichiers du repertoire de travail sous jonas */			
			for (int i =0; i< tabFic.length; i++) tabFic[i].delete();
		}

		/*
		 * Repertoire des fichiers audio vide
		 * Il faut recopier le repertoire de reference 
		 */

		String nomRepIn = Config.getInstance().getRepertoireAudioReference();
		File repIn = new File(nomRepIn);
		String[] tabFicIn = repIn.list();
		logger.debug("initialiserRepPrompt - Repertoire reference = "+nomRepIn+" Repertoire Actif = "+nomRepOut);

		if (tabFicIn != null) {
			logger.debug("initialiserRepPrompt - copie des fichiers audio de : "+nomRepIn+" vers : "+nomRepOut);

			for (String ficRef : tabFicIn) {

				if (!"".equals(ficRef)) {
					Util.copier(nomRepIn+ficRef, nomRepOut+ficRef);

				}
			}

		}
		else {
			logger.info("initialiserRepPrompt - pas de fichier audio dans : "+Config.getInstance().getRepertoireAudioReference());
		}


		logger.debug("initialiserRepPrompt - OK");
		return true;
	}

	/**
	 * Suppression des anciens fichiers audio
	 * Ils avaient ete conserves dans l'hypothese que pendant le rechargement
	 * ils pourraient y avoir des clients connectes au servise et utilisant
	 * ces fichiers
	 */
	private static void supprimerAncienAudio() {
		logger.debug("supprimerAncienAudio - Entree");
		String[] extension = Config.getInstance().getProperty(Config.PROMPT_EXTENSION).split(",");
		Iterator<String> it = listePromptOld.iterator();
		String rep = Config.getInstance().getRepertoireAudio();
		if (rep == null) rep="";
		String r;
		File f;		
		while (it.hasNext()) {
			r = (String)it.next();
			for (int i=0; i<extension.length; i++) {
				f = new File (rep+r+"."+extension[i]);
				if (f.exists()) {
					logger.debug("supprimerAncienAudio - suppression de "+f.getName());
					f.delete();
					break;
				}
			}				
		}
	}
}
