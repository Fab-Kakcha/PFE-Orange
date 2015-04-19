package orange.olps.svi.navigation;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import orange.olps.svi.client.Client;
import orange.olps.svi.config.Config;
import orange.olps.svi.util.Util;

public abstract class Navigation {
	// cle pour l'analyse de navigation --> reconnue par la phase analyserNavigation
	public static final int RIEN = 0;
	public  static final int DECONNEXION = 1;
	public static final int DIFFUSION = 2;
	public static final int MENU_SAISIE = 3; 
	public static final int SAISIE_DTMF = 4;
	public static final int TRANSFERT = 5;
	public static final int ENREG_AUDIO = 6;
	public static final int DISSUASION = 7;
	public static final int DIFFUSION_REJET = 8;
	public static final int DIFFUSION_INACTIVITE = 9;

	// cles pour la version des prompts
	public static final String VERSION_LONGUE = "L";
	public static final String VERSION_COURTE = "C";

	// clef pour la mapAction
	public  static final String SUIVANT = "suivant";
	public  static final String ERREUR = "erreur";
	public  static final String REJET = "REJET";
	public static final String INACTIVITE = "inactivite";
	public static final String NON_TROUVE = "nontrouve";

	//Type de vocalisation pour les prompts dynamiques
	public static final String MODE_LANGUE = "LANGUE";
	public static final String MODE_ENTIER = "ENTIER";
	public static final Object MODE_DATE = "DATE";

	public static final String MODE_TEXTE = "TEXTE";
	public static final String MODE_AUDIO = "AUDIO";
	public static final String PRECEDENT = "_PRECEDENT_"; // mot clef signifiant que l'on remonte au menu précedent
	
	/* mots clef du fichier properties */
	protected static final String ACTION_DTMF = "action";
	protected static final String PROMPT = "prompt";
	protected static final String BARGEIN = "bargein";
	protected static final String ABSORBANT = "absorbant";

	protected static final String PROMPT_ERREUR = "erreur.prompt";
	protected static final String ERREUR_MAX = "erreur.max";
	protected static final String ERREUR_SUIVANT = "erreur.suivant";

	protected static final String NON_TROUVE_SUIVANT = "nontrouve.suivant";
	protected static final String REQUETE = "requete";
	protected static final String VARIABLE = "variable";
	protected static final String VALEUR = "valeur";
	protected static final String VALEUR_DEF = "valeur.defaut";
	protected static final String FORMAT = "format";
	protected static final String FILTRE = "filtre";
	protected static final String URL = "url";
	protected static final String ELEMENT = "element";
	protected static final String METHODE = "methode";
	protected static final String PARAM_FICHIER = "parametre.fichier";
	protected static final String NUM_TRANSFERT = "numero";
	protected static final String PARAMETRE = "parametre";
	protected static final String MODE = "mode";
	protected static final String NOM = "nom";
	protected static final String FICHIER = "fichier";
	protected static final String REPERTOIRE = "repertoire";
	protected static final String PROXY = "proxy";
	protected static final String ALEATOIRE= "aleatoire";
	protected static final String DEFAUT = "DEFAUT";

	protected static final String INACTIVITE_MAX = "inactivite.max";
	protected static final String INACTIVITE_PROMPT = "inactivite.prompt";
	protected static final String INACTIVITE_SUIVANT = "inactivite.suivant";
	protected static final String INACTIVITE_TEMPO = "inactivite.tempo";
	protected static final String AUCUN = "<VIDE>";
	protected static final String VOCALISATION_MODE = "vocalisation.mode";
	protected static final String DUREE_MAX_ENREG = "duree.max";
	protected static final String FINAL_SILENCE ="finalsilence";
	protected static final String FINAL_DTMF ="finaldtmf";
	protected static final String ENREG_REP_TXT = "repertoire.texte";
	protected static final String ENREG_REP_AUDIO = "repertoire.audio";
	protected static final String DATE = "date";
	protected static final String REP_PROMPT = "prompt.repertoire";
	protected static final String MAXAGE = "maxage";
	protected static final String UN_A_UN = "unparun";
	protected static final String TYPE = "type";
	protected static final String STATISTIQUES = "statistiques";
	protected static final String CHARSET = "charset";
	protected static final String CONTENT_TYPE = "contentType";
	


	protected static Log logger = LogFactory.getLog(Navigation.class.getName());


	/* label de l'item de navigation */
	protected String label;


	/* service de rattachement du label (numero appele) */
	protected String service;

	/**
	 * association DTMF/Navigation
	 */
	protected Map<String, String> mapAction ;
	/**
	 * Liste des prompts version longue
	 * En cle on a la langue
	 * en valeur la liste des prompts
	 */
	protected Map<String, ArrayList<String>> mapPromptLong = null;
	/**
	 * Liste des prompts version courte
	 * En cle on a la langue
	 * en valeur la liste des prompts
	 */
	protected Map<String, ArrayList<String>> mapPromptCourt = null;
	/**
	 * Valeur du bargein pour cet item
	 */
	protected boolean bargein = true;
	/**
	 * Vrai si des prompts existent pour cet objet
	 */
	protected boolean prompt = false;
	/**
	 * vrai si un prompt est une variable
	 */
	protected boolean promptDynamique = false;
	/**
	 * Type de vocalisation des variables dynamiques
	 * Par defaut : LANGUE: la regle 
	 * application.vocalisation.saisie.<langue>
	 * s'applique
	 * Autres valeurs:
	 * ENTIER
	 */
	protected String promptDynamiqueMode = MODE_LANGUE;

	/**
	 * Cle = langue
	 * Boolean : vrai si la liste des prompts ne contient que le prompt
	 * d'erreur de fichier audio inexistant
	 */
	protected Map<String, Boolean> mapPromptManquant;


	/**
	 * maxage pour les audios
	 */
	protected String audioMaxage = "";

	/**
	 * Quand le parametre UN_PAR_UN est positionné
	 * les prompt seront lus un par un dans la liste donnée par .prompt
	 * varUnParUn contient le nom de la variable client donnant le dernier prompt lu
	 */
	protected String varUnParUn = null;
	/**
	 * Nombre de prompts lus dans le cas du un par un
	 */
	protected String nbUnParUn  = null;
	/**
	 * timeout d'inactivite
	 */
	protected String timeout = "0s";

	/**
	 * Constructeur
	 * @param lab : nom du menu a lire dans les properties
	 * 
	 */
	public Navigation(String lab, String svc) {
		mapAction = new HashMap <String, String>(1);
		label = lab;
		service = svc;
		audioMaxage = Util.getAudioMaxAge();
		setTimeout(Config.getInstance().getProperty(Config.APPLI_TEMPO_INFO, "0s"));
	}

	/**
	 * Initialisation de la navigation
	 * @return true si c'est un nouveau item false si clone
	 */
	protected boolean initialiserItem(Properties lstProp) {
		logger.debug(this.getClass().getName()+":: initialiserItem - Entree pour ("+label+")");
		boolean ret = true;

		String racinePropriete = service+".navigation."+label+".";
		/* lecture des prompts */
		String s = lstProp.getProperty(racinePropriete+PROMPT,"");			
		if (!"".equals(s)) {
			prompt = true;
			logger.debug(this.getClass().getName()+":: initialiserItem - prompt pour ("+service+"/"+label+") ("+s+")");
			verifierPrompt(s.trim());
		}
		/* lecture du barge-in */
		s = lstProp.getProperty(racinePropriete+BARGEIN,"").trim();
		if ("".equals(s)) {
			// on va lire la donnée générale
			s = Config.getInstance().getProperty(Config.BARGEIN, "").trim();
		}
		if (!"".equals(s)) {				
			setBargein(Boolean.parseBoolean(s));			
		}
		// tempo d'inactivite
		s = lstProp.getProperty(racinePropriete+INACTIVITE_TEMPO,"null").trim();
		if ("null".equals(s)) {
			// pas defini, on prend la valeur generale
			if ("Deconnexion".equals(this.getClass().getSimpleName())) {
				s = Config.getInstance().getProperty(Config.APPLI_TEMPO_DCNX, "1s").trim();
			}
			else if ("Menu".equals(this.getClass().getSimpleName())
					|| "Saisie".equals(this.getClass().getSimpleName())
					|| "Langue".equals(this.getClass().getSimpleName())) {
				s = Config.getInstance().getProperty(Config.APPLI_TEMPO_MENU,"3s").trim();
			}
			else {
				s = Config.getInstance().getProperty(Config.APPLI_TEMPO_INFO, "0s").trim();
			}
		}
		setTimeout(s);
		// lecture du maxage
		s = lstProp.getProperty(racinePropriete+MAXAGE,"").trim();
		if (!"".equals(s)) {			
			audioMaxage=s;
		}

		/* lecture du suivant */
		s = lstProp.getProperty(racinePropriete+SUIVANT,"").trim();
		if (!"".equals(s)) {				
			mapAction.put(SUIVANT, s.trim());			
		}	
		s = lstProp.getProperty(racinePropriete+VOCALISATION_MODE,"").trim();
		if (!"".equals(s)) {				
			setPromptDynamiqueMode(s);				
		}	
		else {
			setPromptDynamiqueMode(MODE_LANGUE);
		}
		// lecture des prompts un par un
		s = lstProp.getProperty(racinePropriete+UN_A_UN, "false").trim();
		setUnParUn(s);
		return ret;
	}
	/**
	 * Initialisation des actions DTMF
	 * @return
	 */
	protected boolean initialiserDTMF(Properties lstProp) {
		logger.debug(this.getClass().getName()+":: initialiserDTMF - Entree pour ("+label+")");
		boolean ret = true;
		String racinePropriete = service+".navigation."+label+".";
		String s;

		s = lstProp.getProperty(racinePropriete+ACTION_DTMF+".*","");
		if (!"".equals(s)) {				
			mapAction.put("*", s.trim());			
		}
		else {
			mapAction.put("*", REJET);
		}
		s = lstProp.getProperty(racinePropriete+ACTION_DTMF+".#","");
		if (!"".equals(s)) {				
			mapAction.put("#", s.trim());			
		}
		else {
			mapAction.put("#", REJET);
		}

		for (int i=0; i <10; i++) {
			s = lstProp.getProperty(racinePropriete+ACTION_DTMF+"."+i,"");
			if (!"".equals(s)) {				
				mapAction.put(Integer.toString(i), s.trim());			
			}
			else {
				mapAction.put(Integer.toString(i), REJET);
			}
		}

		return ret;
	}
	/**
	 * 
	 * @return
	 */
	public String getSuivant() {	
		logger.debug(this.getClass().getName()+"::getSuivant - ("+label+") --> ("+mapAction.get(SUIVANT)+")");

		return  mapAction.get(SUIVANT);
	}
	protected void setSuivant(String suivant) {
		mapAction.put(SUIVANT, suivant);
	}
	public ArrayList<String> getPrompt(String langue) {
		return getPrompt(langue, VERSION_LONGUE);
	}
	public ArrayList<String> getPrompt(String langue, String version) {
		logger.debug(this.getClass().getName()+"::getPrompt pour ("+label+") langue ("+langue+") version ("+version+")");

		if(VERSION_COURTE.equals(version)) {
			if (mapPromptCourt == null) {
				logger.debug(this.getClass().getName()+"::getPrompt pas de prompt pour ("+label+") langue ("+langue+") version ("+version+")");
				return null;
			}
			return mapPromptCourt.get(langue);				
		}
		else {
			if (mapPromptLong == null) {
				logger.debug(this.getClass().getName()+":: getPrompt pas de prompt pour ("+label+") langue ("+langue+") version ("+version+")");
				return null;
			}
			return mapPromptLong.get(langue);
		}
	}

	/**
	 * retourne le nom du dialogue de navigation associé a la dtmf
	 * @param dtmf : dtmf saisie
	 * @return nom du prochain item de navigation
	 */
	public String getAction (String dtmf) {
		logger.debug(this.getClass().getName()+":: getAction - Entree ("+label+"/"+dtmf+")");
		String ret = mapAction.get(dtmf);
		logger.debug(this.getClass().getName()+":: getAction - Sortie ("+ret+")");
		return ret;
	}
	/**
	 * Donne la map des actions (action/label assossié)
	 * @return map
	 */
	public  Map<String, String> getAction () {
		return mapAction;
	}
	
	public void setAction(String dtmf, String label) {
		mapAction.put(dtmf, label);
	}

	public Map<String, String> getMapAction() {
		return mapAction;
	}

	public boolean isPromptDynamique() {
		return promptDynamique;
	}

	/**
	 * Verifie que les prompts definis existent bien
	 * et constitue la mapPrompt en fonction de la langue
	 * @param listePrompt liste des prompts separes par des virgules
	 */
	protected void verifierPrompt(String listePrompt) {
		// liste des prompts lue dans le fichier de properties
		String[] tabPrompt = listePrompt.split(",");
		verifierPrompt(tabPrompt);
	}
	/**
	 * 
	 * @param tabPrompt liste des prompts sous forme tableau
	 */
	protected void verifierPrompt(String[] tabPrompt) {
		String[] tabLangue = Config.getInstance().getListeLangue();			
		File f ;
		String rep = Config.getInstance().getRepertoireAudio();
		if (rep == null) {
			rep = "";
		}
		String ext = Config.getInstance().getProperty(Config.PROMPT_EXTENSION,"wav");
		String promptManquant = Config.getInstance().getProperty(Config.PROMPT_INEXISTANT);

		StringBuffer ficSsLangue = new StringBuffer();
		StringBuffer fic = new StringBuffer();
		StringBuffer ficLong = new StringBuffer();
		StringBuffer ficCourt = new StringBuffer();
		ArrayList<String> promptFinalLong;
		ArrayList<String> promptFinalCourt;
		// initialisation de la table des prompts
		mapPromptLong = new HashMap<String,ArrayList<String>> (tabLangue.length);
		mapPromptCourt = new HashMap<String,ArrayList<String>> (tabLangue.length);
		mapPromptManquant = new HashMap<String, Boolean> (tabLangue.length);

		logger.debug(this.getClass().getName()+":: verifierPrompt pour ("+label+") rep audio ("+rep+")");

		for (String langue : tabLangue) {
			promptFinalLong = new ArrayList<String>();
			promptFinalCourt = new ArrayList<String>();

			for (String p : tabPrompt) {

				if (p.startsWith("_var")) {
					// on a un prompt dynamique: la variable contient un nom de prompt
					promptDynamique = true;
					promptFinalLong.add(p);
					promptFinalCourt.add(p);
				}
				else {
					// construction des noms des fichiers audio
					ficSsLangue.setLength(0);
					fic.setLength(0);
					ficLong.setLength(0);
					ficCourt.setLength(0);

					ficSsLangue.append(p);

					fic.append(p);
					fic.append('_');
					fic.append(langue);
					ficCourt.append(fic);
					ficLong.append(fic);


					ficLong.append('_');
					ficLong.append(VERSION_LONGUE);

					ficCourt.append('_');
					ficCourt.append(VERSION_COURTE);

					// Test de l'existence des quatre fichiers
					f = new File (rep+ficSsLangue.toString()+'.'+ext);
					if (!f.exists()) {
						ficSsLangue.setLength(0);
					}

					f = new File (rep+fic.toString()+'.'+ext);
					if (!f.exists()) {
						fic.setLength(0);
					}
					f = new File (rep+ficLong.toString()+'.'+ext);
					if (!f.exists()) {
						ficLong.setLength(0);
					}					
					f = new File (rep+ficCourt.toString()+'.'+ext);
					if (!f.exists()) {
						ficCourt.setLength(0);
					}
					// Constitution de la liste des fichiers version Longue
					if (ficLong.length() > 0) {
						promptFinalLong.add(ficLong.toString());
					}
					else if (fic.length() > 0){
						promptFinalLong.add(fic.toString());
					}
					else if (ficCourt.length() > 0){
						promptFinalLong.add(ficCourt.toString());
					}
					else if (ficSsLangue.length() > 0) {
						promptFinalLong.add(ficSsLangue.toString());
					}

					// Constitution de la liste des fichiers version courte
					if (ficCourt.length() > 0) {
						promptFinalCourt.add(ficCourt.toString());
					}
					else if (fic.length() > 0){
						promptFinalCourt.add(fic.toString());
					}
					else if (ficLong.length() > 0){
						promptFinalCourt.add(ficLong.toString());
					}
					else if (ficSsLangue.length() > 0) {
						promptFinalCourt.add(ficSsLangue.toString());
					}
				}
			}
			Boolean b;
			if(promptFinalLong.isEmpty()) {
				ficLong.setLength(0);
				ficLong.append(promptManquant);
				ficLong.append('_');
				ficLong.append(langue);
				promptFinalLong.add(ficLong.toString());
				b = new Boolean(true);
			}
			else {
				b = new Boolean(false);
			}
			if(promptFinalCourt.isEmpty()) {
				promptFinalCourt = promptFinalLong;
			}
			// mise a jour de la map des prompts pour la langue
			mapPromptLong.put(langue, promptFinalLong);
			mapPromptCourt.put(langue, promptFinalCourt);
			mapPromptManquant.put(langue, b);
		}
	}
	protected void setTimeout(String s) {
		if (s.trim().endsWith("s")) {
			// on a bien mis l'unité (s, ms)
			timeout = s.trim();
		}
		else {
			timeout= s.trim()+"s";
		}
	}
	public String getTimeout() {
		return timeout;
	}
	public void setBargein(boolean bargein) {
		this.bargein = bargein;
	}

	public boolean isBargein() {
		logger.debug(this.getClass().getName()+":: isBargein - Entree ("+label+" / "+bargein+")");
		return bargein;
	}
	public String getService() {			
		return service;
	}
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}


	public boolean isPrompt() {
		return prompt;
	}
	public boolean isPromptManquant(String langue) {
		if (mapPromptManquant == null) return false;// pas de prompt sur cet item 
		Boolean b = mapPromptManquant.get(langue);
		if (b== null) return true;
		return b.booleanValue();
	}

	public String getPromptDynamiqueMode() {
		return promptDynamiqueMode;
	}

	public void setPromptDynamiqueMode(String promptDynamiqueMode) {
		this.promptDynamiqueMode = promptDynamiqueMode;
	}


	public String getAudioMaxage() {
		return audioMaxage;
	}

	public void setAudioMaxage(String audioMaxage) {
		this.audioMaxage = audioMaxage;
	}
	public void setUnParUn(String s) {
		Boolean b = Boolean.parseBoolean(s);
		if (b.booleanValue()) {
			varUnParUn = "_var"+label+"P";
			nbUnParUn = "_var"+label+"Nb";
		}

	}
	public boolean isPromptUnParUn() {
		return varUnParUn != null;
	}
	public String getVarUnParUn() {
		return varUnParUn;
	}
	public String getNbUnParUn() {
		return nbUnParUn;
	}
	/**
	 * Preparation des prompts pour le client
	 * @param client
	 * @return nombre de prompts
	 */
	protected int preparerPrompt(Client client) {
		ArrayList<String> tabPrompt;
		if(client.getNbInactivite() == 0 && client.getNbRejet() == 0) {
			tabPrompt = getPrompt(client.getLangue(), Navigation.VERSION_LONGUE);
		}
		else {				
			tabPrompt = getPrompt(client.getLangue(), Navigation.VERSION_COURTE);
		}

		if (isPromptDynamique()) {
			// il y a des prompts dynamiques , il faut retravailler la liste
			tabPrompt = Util.reconstituerListePrompt(tabPrompt, client);
		}
		if (isPromptUnParUn()) {
			// on doit lire les prompts un par un
			if (tabPrompt.size() > 0) {		    	
				String promptPrecedent = client.getValeur(getVarUnParUn()); //_var<Label>P
				if (promptPrecedent == null) {
					// c'est le premier passage de ce client dans cet item
					client.setValeur(getVarUnParUn(), tabPrompt.get(0), false);
					client.setValeur(getNbUnParUn(), "1", false); // un prompt lu
				}
				else {
					int n = Integer.parseInt(client.getValeur(getNbUnParUn())); // nombre de prompts deja lus
					if (n >= tabPrompt.size()) {
						// on a lu tous les prompts, on revient au début
						n=0;
						client.setValeur(getVarUnParUn(), tabPrompt.get(0),false);
						client.setValeur(getNbUnParUn(), "1", false); // un prompt lu
					}
					else {
						client.setValeur(getVarUnParUn(), tabPrompt.get(n), false);
						client.setValeur(getNbUnParUn(), Integer.toString(n+1), false); 
					}    	
				}
				client.setPrompt (client.getValeur(getVarUnParUn()));
				return 1;
			}
			else {
				client.setValeur(getVarUnParUn(), "", false);
				client.setValeur(getNbUnParUn(), "0", false);
				client.setPrompt ("");
				return 0;
			}	   
		}
		if (tabPrompt == null) {
			client.setPrompt ("");
			return 0;
		}
		else {
			client.setPrompt(tabPrompt);
			return tabPrompt.size();
		}

	}
	protected String reconstituerListePrompt () {
		if (mapPromptCourt == null) return "";
		String langue = Config.getInstance().getListeLangue()[0];
		ArrayList<String> prompts = mapPromptCourt.get(langue);
		StringBuffer buf = new StringBuffer();

		for (String p : prompts) {
			if (buf.length() == 0) buf.append(p);
			else {
				buf.append(',');
				buf.append(p);
			}
		}
		return buf.toString();

	}
	public abstract void calculerActionNavigation( Client client);
	public abstract JSONObject toJsonObject();

}
