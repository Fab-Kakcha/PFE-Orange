
package orange.olps.svi.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*
 * Classe de lecture du fichier de configuration
 * les items etant de la forme <label1>.<Label2>...<labelN>=<valeur du parametre>
 */

public class Config extends Properties  {
	/*
	 * Constantes publiques permettant l'acces au fichier de configuration
	 */

	public  static final String LANGUE = "application.langue";

	public  static final String TEMPO = "application.tempo";

	public  static final String TRSFRT ="transfert.";

	public  static final String APPLI_TEMPO_MENU = "application.inactivite.tempo.menu";
	public  static final String APPLI_TEMPO_INFO = "application.inactivite.tempo.info";
	public  static final String APPLI_TEMPO_DCNX = "application.inactivite.tempo.dcnx";
	public  static final String APPLI_NB_INCTVT_MAX = "application.inactivite.max";
	public  static final String APPLI_NB_RJT_MAX = "application.rejet.max";
	public  static final String SILENCE = "application.silence.";

	public  static final String MAXAGE_AUDIO = "application.maxage.audio";
	public  static final String MAXAGE_DOC = "application.maxage.document";
	public  static final String MAXAGE_CTRL_CACHE = "application.maxage.httpcontrolcache";
	public  static final String FETCHTIMEOUT = "application.fetchtimeout";

	public  static final String PATTERN_NUM_APPELANT = "application.pattern.numeroappelant";
	public  static final String PATTERN_NUM_APPELE = "application.pattern.numeroappele";

	public  static final String TRANSFERT_TIMEOUT = "transfert.timeout";

	//public static final String GDE_PRIOR = "prompt.pattern.categorie.priorite.";
	public static final String PROMPT_REP_AUDIO_ACTF = "prompt.repertoire.actif";
	public static final String PROMPT_REP_AUDIO_REF = "prompt.repertoire.reference";	
	public static final String PROMPT_EXTENSION ="prompt.extension";

	public static final String PROMPT_ERREUR = "XXX.navigation.prompt.erreur";
	public static final String PROMPT_INACTIVITE = "XXX.navigation.prompt.inactivite";
	public static final String DISSUASION_INACTIVITE = "prompt.dissuasion.inactivite";
	public static final String DISSUASION_ERREUR = "prompt.dissuasion.erreur";
	public static final String PROMPT_TMPS_DEPASSE = "prompt.dissuasion.temps_presence";
	public static final String PROMPT_INEXISTANT = "prompt.inexistant";
	public static final String PROMPT_SILENCE = "prompt.silence";

	public static final String BDD_USER = "bdd.user";
	public static final String BDD_PSSWD = "bdd.passwd";
	public static final String BDD_URL = "bdd.url";
	public static final String BDD_DRIVER = "bdd.driver";
	public static final String BDD_CNX_MAX = "bdd.connexion.nbmax";
	public static final String BDD_CNX_MIN = "bdd.connexion.nbmin";
	public static final String BDD_CNX_TMPO = "bdd.connexion.tempo";
	public static final String BDD_THRD_TMPO = "bdd.thread.tempo";
	public static final String BDD_RQ_TMT = "bdd.requete.timeout";
	public static final String BDD_CNX_TIMEOUT = "bdd.connexion.timeout";

	public static final String HEURE_RCHRGMNT = "application.heure.rechargement";

	public static final String SERVICE_AUTORISE = "application.service.autorise";
	public  static final String APPLI_SERVICE_REROUTE = "application.service.reroute.ext";
	public  static final String APPLI_REROUTE_CTI = "application.service.reroute.cti";
	public  static final String APPLI_SERVICE_AUT_TRANSMIS = "application.service.numero.transmis";
	public static final String VARIABLE = "application.variable";

	public  static final String PATTERN_NUM_APPELE_2 = "appel.sortant.pattern.numeroappele";
	public  static final String PATTERN_GATEWAY = "appel.sortant.pattern.gateway";
	public  static final String TRANSFERT_CTI="appel.sortant.from.cti.externe";
	public  static final String TRANSFERT_DEFAUT="appel.sortant.from.defaut";

	public static final String DEBUG = "test.debug";
	public static final String TEST_APPELANT = "test.numeroappelant";

	private static final long serialVersionUID = -4432485141565019437L;

	public static final String VOCALISATION_SAISIE = "application.vocalisation.saisie.";

	public static final String TAILLE_QUEUE = "statistiques.queue.taille";
	public static final String STAT_MODE = "statistiques.mode";
	public static final String STATS_REP = "statistiques.repertoire";
	public static final String STATS_FIC = "statistiques.fichier";
	public static final String STATS_ADR_AS = "statistiques.adresse.as";
	public static final String STAT_BDD_USER = "statistiques.bdd.user";
	public static final String STAT_BDD_URL = "statistiques.bdd.url";
	public static final String STAT_BDD_PASSWD = "statistiques.bdd.passwd";
	public static final String STAT_BDD_CNX_TIMEOUT = "statistiques.bdd.connexion.timeout";
	public static final String STAT_BDD_DRIVER = "statistiques.bdd.driver";
	public static final String STAT_HTTP_URL = "statistiques.http.url";
	public static final String STAT_CNX_MAX = "statistiques.bdd.nbmax";
	public static final String STAT_CNX_MIN = "statistiques.bdd.nbmin";
	public static final String STAT_THRD_TMPO = "statistiques.bdd.thread.tempo";
	public static final String STAT_CNX_TMPO = "statistiques.bdd.connexion.tempo";
	public static final String STAT_RQ_TMT = "statistiques.bdd.requete.timeout";

	public static final String NUMERO_MASQUE = "application.numero.masque";
	public static final String VALEUR_CODE = "application.code.";

	public static final String BARGEIN = "XXX.navigation.bargein";
	public static final String ABSORBANT = "XXX.navigation.absorbant";
	public static final String NUM_TRANSFERT = "XXX.navigation.transfert.numero";

	public static final String REQUETE = "XXX.navigation.bdd.requete";
	public static final String REQUETE_VAR = "XXX.navigation.bdd.variable";
	public static final String REQUETE_VAL_DEF = "XXX.navigation.bdd.valeur.defaut";

	public static final String ENREG_FMT = "XXX.navigation.enreg.format";
	public static final String ENREG_VAR = "XXX.navigation.enreg.variable";
	public static final String DUREE_MAX_ENREG = "XXX.navigation.enreg.duree.max";
	public static final String ENREG_REP_TXT = "XXX.navigation.enreg.repertoire.texte";
	public static final String FINAL_SILENCE = "XXX.navigation.enreg.finalsilence";
	public static final String FINAL_DTMF = "XXX.navigation.enreg.finaldtmf";
	public static final String ENREG_REP_AUDIO = "XXX.navigation.enreg.repertoire.audio";

	public static final String WEBSVC_URL      = "XXX.navigation.websvc.url";
	public static final String WEBSVC_VARIABLE = "XXX.navigation.websvc.variable";
	public static final String WEBSVC_DEF      = "XXX.navigation.websvc.valeur.defaut";
	public static final String WEBSVC_ELEMENT  = "XXX.navigation.websvc.element";
	public static final String WEBSVC_METHODE  = "XXX.navigation.websvc.methode";
	public static final String WEBSVC_PARAM    = "XXX.navigation.websvc.parametre";
	public static final String PROXY           = "XXX.navigation.websvc.proxy";
	public static final String WEBSVC_FORMAT   = "XXX.navigation.websvc.format";
	public static final String WEBSVC_CHARSET  = "XXX.navigation.websvc.charset";
	public static final String WEBSVC_CONTENT_TYPE = "XXX.navigation.websvc.contentType";

	public static final String WEBSVC_PARAM_FICHIER = "XXX.navigation.websvc.parametre.fichier";;
	public static final String WEBSVC_T_CNX = "webservice.timeout.connexion";
	public static final String WEBSVC_T_RQ =  "webservice.timeout.requete";
	public static final String WEBSVC_MAX_CNX = "webservice.connexion.max";
	public static final String WEBSVC_MAX_CNX_ROUTE = "webservice.connexion.route.max";

	public static final String SMS_VALEUR = "XXX.navigation.sms.valeur";
	public static final String SMS_MODE = "XXX.navigation.sms.mode";
	public static final String SMS_FICHIER = "XXX.navigation.sms.fichier";
	public static final String SMS_REPERTOIRE = "XXX.navigation.sms.repertoire";

	public static final String SEUIL_ATTENTE = "application.temps.attente";

	public static final String PROP_COMPL = "application.properties";

	public static final String REP_JSON = "ihm.rep.json";


	private static Config config;
	private static boolean sbooIsInit = false;
	private static String repProperties = "/conf/";	
	private static String nomProperties = "Svi.properties";
	private static String repAudio = null;
	private static String repAudioRef = null;
	/**
	 * nom de la variable d'environnement contenant le nom du répertoire de l'appli
	 */
	private String varEnvRep = null;

	protected static Log logger = LogFactory.getLog(Config.class.getName());

	private class FiltreRepJboss implements FilenameFilter {

		private String filtre;
		/**
		 * Filtre les répertoire de deploiement jboss
		 */
		public FiltreRepJboss (String regexp) {
			filtre = regexp;
		}
		public boolean accept(File dir, String name) {		
			if (name.matches(filtre)) {
				File f = new File (dir+System.getProperty("file.separator")+name);
				if (f.isDirectory()) return true;
			}
			return false;
		}

	}
	private class FiltreJson implements FilenameFilter {

		public boolean accept(File dir, String name) {		
			if (name.endsWith(".json")) {
				return true;
			}
			return false;
		}

	}

	/*
	 * Constructeur
	 */
	private Config() {
		super();				
	}
	/**
	 * Retourne l'instance du singleton Config
	 * @return Config
	 */
	public static Config getInstance() {
		if (config ==null)  {
			synchronized(Config.class) {				
				if (config ==null) {
					config = new Config();	
				}					
			}		
		}
		return config;
	}
	public File getFileProperties() {
		return getFileProperties (nomProperties);
	}
	/**
	 * Retourne le  fichier de proprietes 
	 */
	public File getFileProperties (String nom) {
		String[] tabEnv = {varEnvRep, "HOME_SVI", "HOME_APP", "HOME_SVC", "HOME_OVP", "HOME_INF"};
		String fic ="";
		String repEnv = null;

		for (String s : tabEnv) {
			if (s != null) {
				repEnv = System.getenv(s);
				if (s != null) {
					fic = repEnv+repProperties+nom;
					File f = new File (fic);
					if (f.exists() && f.isFile() && f.canRead()) {
						return f;
					}
				}
			}
		}

		logger.error("getFileProperties -  fichier properties ("+nom+") non trouve");
		return null;
	}
	/**
	 * Retourne le  fichier JSON 
	 */
	public JSONObject getFileJson () {
		String[] tabEnv = {varEnvRep, "HOME_SVI", "HOME_APP", "HOME_SVC", "HOME_OVP", "HOME_INF"};		
		String repEnv = null;
		File rep;
		JSONObject obj = null;
		for (String s : tabEnv) {
			if (s != null) {
				repEnv = System.getenv(s);
				if (repEnv != null) {
					rep = new File (repEnv);
					if (rep.exists() && rep.isDirectory()) {
						File[] tabFic = rep.listFiles(new FiltreJson());
						
						BufferedReader br;
						StringBuffer sb = new StringBuffer();
						String tmp = null;
						for (File f : tabFic) {
							try {
								br = new BufferedReader(new FileReader(f));
								sb.setLength(0);
								while ((tmp=br.readLine()) != null) {
									sb.append(tmp);
								}
								br.close();
								obj = new JSONObject(sb.toString());


							} catch (FileNotFoundException e) {
							} catch (IOException e) {					
							} catch (JSONException e) {
								logger.error("getFileJson -  mauvais format ("+f.getName()+") "+e.getMessage());
							}					

						} // Fin de boucle sur les fichiers json
					}			
				}
			}
		}

		logger.info("getFileJson -  non trouve");
		return obj;
	}
	/** 
	 * Retourne le repertoire des fichiers AUDIO au format UNIX ou Windows
	 */
	public String getRepertoireAudio () {

		if (repAudio == null) {
			String sep = System.getProperty("file.separator");
			String rep =config.getProperty(Config.PROMPT_REP_AUDIO_ACTF);

			if (rep.contains("*") || rep.contains("+")) {
				// cas de JBOSS : le repertoire cible de deploiement varie a charque restart de JBOSS
				String[] tabRep = rep.split(sep);			
				repAudio = construireRepAudio ("", tabRep, 1);

			}
			else {
				if (sep.equals("/")) {
					/* Linux */
					String racine =  System.getenv("RACINE_APP");
					if (racine == null) {
						repAudio = rep+ sep;
					}
					else {
						repAudio = racine+ sep + rep+ sep;
					}

				}
				else {
					/* windows	 */
					if (rep.indexOf(":") != -1) {
						/* chemin absolu */
						repAudio = rep+sep;
					}
					else {
						String tmp = "C:"+ rep;
						repAudio =tmp.replace("/",sep)+sep;
					}
				}
			}
		}

		return repAudio;
	}
	/**
	 * Dans le cas de JBOSS les répertoires de déploiement nesont pas fixes
	 * Il faut aller rechercher celui qui correspond au pattern décrit dans le fichier de properties
	 * @param rep = partie du repertoire cible deja reconstitue
	 * @param tabRep = tableau du repertoire décrit dans le properties et découpe suivant les '/'
	 * @param niveau = niveau d'analyse dans le tableau
	 * @return le repertoire trouve
	 */
	private String construireRepAudio(String rep, String[] tabRep, int niveau) {
		int i;
		logger.debug("construireRepAudio - entree - niveau ("+niveau+")");
		for (i = niveau; i < tabRep.length; i++) {
			if (tabRep[i].contains("*") || tabRep[i].contains("+")) {
				// partie du répertoire avec jocker
				break;
			}
			else {
				rep += System.getProperty("file.separator") + tabRep[i];
			}
		}
		if (i == tabRep.length) {
			// le repertoire a ete reconstitue
			rep += System.getProperty("file.separator");
			logger.debug("construireRepAudio - retour ("+rep+")");
			return rep;
		}
		File f = new File (rep);
		if (f.exists() && f.isDirectory()) {
			logger.debug("construireRepAudio - traitement du repertoire :"+f.getAbsolutePath());
			// lecture des répertoire matchant le pattern
			String[] tabFic = f.list(new FiltreRepJboss(tabRep[i]));
			String retour = null;
			String repBase;
			for (String r : tabFic) {
				repBase = rep+ System.getProperty("file.separator") + r;
				i++;
				retour = construireRepAudio(repBase, tabRep, i);
				if (retour != null)  return retour;
			}
		}
		return null;
	}
	/** 
	 * Retourne le repertoire des fichiers AUDIO de reference au format UNIX ou Windows
	 */
	public String getRepertoireAudioReference () {
		if (repAudioRef == null) {
			String sep = System.getProperty("file.separator");
			String rep = config.getProperty(Config.PROMPT_REP_AUDIO_REF);

			if (sep.equals("/")) {
				/* Linux */
				String racine =  System.getenv("RACINE_APP");
				if (racine == null) {
					repAudioRef =rep + sep;
				}
				else {
					repAudioRef =racine+ sep+rep + sep;
				}

			}
			else {
				/* windows	 */

				if (rep.indexOf(":") != -1) {
					/* chemin absolu */
					repAudioRef = rep+sep;
				}
				else {
					String tmp = "C:"+ rep+rep;
					repAudioRef =tmp.replace("/",sep)+sep;
				}
			}
		}

		return repAudioRef;
	}

	/**
	 * isInitialised : Identifie si l'objet a ete initialise ou pas
	 */
	public static boolean isInitialise() {
		return sbooIsInit;
	}

	/**
	 * calcule le temps (en milli secondes)
	 * durant lequel le thread doit etre en 'sleep'
	 * Le parametrage est dans le fichier de config.
	 * Plusieurs heures de reveil peuvent etre configuree sous l forme
	 * HH:MM,HH:MM,HH:MM
	 */
	public long calculerTempsDodo() {
		String val = config.getProperty(Config.HEURE_RCHRGMNT);
		logger.debug("calculerTempsDodo - Entree");
		if (val == null || "".equals(val)) return 44200000; /* attente de 12h (en ms)*/
		else {
			String[] tabHeure = val.split(",");
			long [] tabMinute = new long [tabHeure.length]; 			
			int i = 0;
			String[] result;
			/* conversion de l'heure au format HH:MM en minutes */
			for (i=0; i < tabHeure.length; i++) {
				result = tabHeure[i].split(":");
				tabMinute[i] = Long.parseLong(result[0]) * 60 + Long.parseLong(result[1]);
			}
			/* Recuperation date courante */
			Date d = Calendar.getInstance().getTime();
			/* variable pour extraire les heures */
			SimpleDateFormat sdf1 = new SimpleDateFormat("HH");
			/* variables pour extraire les minutes */
			SimpleDateFormat sdf2 = new SimpleDateFormat("mm");

			long minuteActuelle = Long.parseLong(sdf1.format(d)) * 60 + Long.parseLong(sdf2.format(d));
			long mini = -1;
			long deltaMinute = 0;
			/* parcourt du tableau des heures de relance du chargement pour determiner le plus petit delta
			 * entre l'heure actuelle et les heures desirees
			 */
			for (i=0; i < tabMinute.length; i++) {
				deltaMinute = tabMinute[i] - minuteActuelle;
				if (deltaMinute <= 0) {
					/* On ajoute 24heures*/
					deltaMinute += 1440;
				}
				if (deltaMinute < mini || mini == -1) mini = deltaMinute;
			}
			/* conversion des minutes en millisecondes */
			mini = mini * 60000;
			logger.debug("calculerTempsDodo - temps ="+mini);

			return mini;
		}
	}
	/**
	 * initialiser: Lecture du fichier de configuration
	 */
	private static void chargerProperties(File fic) {
		logger.debug("chargerProperties - Entree");
		if (!sbooIsInit) {

			try {

				FileInputStream fis = new FileInputStream(fic);
				config.clear();
				config.load(fis);
				fis.close();


				//Proprietes initialisees
				sbooIsInit = true;
			} catch (IOException ioe) {
				logger.error("chargerProperties - erreur d'initialisation : "+ioe.getMessage());
				sbooIsInit = false;
			}
		}
	}
	/**
	 * Force la relecture de la config
	 * @param prop: nom du fichier properties
	 * @param varEnvRep : nom de la variable d'environnement contenant le nom du répertoire de l'appli
	 */
	public void initialiser (String prop, String varEnvRep) {
		logger.info("initialiser - Rechargement de la configuration ("+prop+") - Variable d'environnement ("+varEnvRep+")");
		synchronized(Config.class) {			
			sbooIsInit = false;
			nomProperties = prop;
			this.varEnvRep = varEnvRep;
			repAudio = null;
			repAudioRef = null;

			File f = getFileProperties();					

			if (f != null) {
				chargerProperties (f);
			}					
		}		
	}
	/**
	 * Donne la liste des langues 
	 * @return tableau des langues
	 */
	public String[] getListeLangue() {
		return this.getProperty(Config.LANGUE).trim().toUpperCase().split(",");
	}
	public boolean isLangue (String l) {
		for (String langue : this.getProperty(Config.LANGUE).trim().toUpperCase().split(",")) {
			if (langue.equals(l)) return true;
		}
		return false;
	}
	/**
	 * Recherche le service dans l'objet json
	 * @param obj = json
	 * @param service = service
	 * @return
	 */
	public boolean rechercherService(JSONObject obj, String service) {
		if (obj == null) return false;
		JSONArray arr;
		try {
			arr = obj.getJSONArray("racines");
			for (int i = 0; i < arr.length(); i++) {
				if(arr.getString(i).equals(service)) return true;
			}
		} catch (JSONException e) {
			logger.info("rechercherService - probleme parsing json :"+e.getMessage());
		}

		return false;
	}


}
