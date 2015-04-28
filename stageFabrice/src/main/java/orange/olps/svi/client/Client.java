package orange.olps.svi.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import orange.olps.svi.navigation.Navigation;
import orange.olps.svi.navigation.NavigationManager;
//import orange.olps.svi.stats.StatManager;
import orange.olps.svi.util.Util;
import orange.olps.svi.config.Config;

public class Client {

	/* variables: cles de mapAttribut */
	public static final String VAR_DUREE = "_varDuree";
	public static final String VAR_DATE = "_varDateDeb";
	public static final String VAR_DATE_OMS = "_varDateOms";
	public static final String VAR_LANGUE = "_varLangue";
	public static final String VAR_APPELANT = "_varNumAppelant";
	public static final String VAR_SVC = "_varSvc";
	public static final String VAR_APPELE = "_varNumAppele";
	public static final String VAR_ERREUR = "_varErreur";
	public static final String VAR_IDENT = "_varIdent";
	public static final String VAR_RAISON = "_varRaison"; // raison de la deconnexion (INACTIVITE/TIMEOUT/REJET)
	public static final String VAR_SEGMENT = "_varSegment";
	public static final String VAR_METIER = "_varMetier";
	public static final String VAR_TMP = "_varTmp";
	public static final String VAR_NAV = "_varNav";
	public static final String VAR_NAV_PREC = "_varNavPre";
	public static final String VAR_TRANSFERT = "_varTransfert"; // numero de transfert (utilisé seulement pour les stats)
	
	public static final String INACTIVITE = "INACTIVITE";
	public static final String TIMEOUT = "TIMEOUT";
	public static final String REJET = "REJET";
	
	
	
		
	/**
	 * Map ayant pour cle le nom d'une variable (langue, numeroAppele, numeroAppelant) et en valeur, la valeur de l'attribut
	 */
	private  Map<String,String> mapAttribut;
	

	private StringBuffer saisie;
	/* action en cours */
	private int actionNavigation = Navigation.RIEN;
	private boolean silenceDemande = false;
	private int nbInactivite = 0;
	private int nbRejet = 0;

	/**
	 * Booléen donnant le fonctionnement du service par rapport au silence à jouer
	 * après détection de dtmf.
	 * true par defaut.
	 */
	private boolean silenceService;
	/**
	 * index permet de stocker un numero de prompt a jouer
	 * Utilise par Saisie lors d'un erreur où il faut jouer le prompt
	 * correspondant à l'erreur dans la liste de prompts
	 */
	private int index = -1;
	/**
	 * donne le nbre de caracteres saisis depuis la derniere saisie
	 */
	private int nbSaisieAjoute = 0;
	
	/**
	 * liste des prompts à jouer
	 */
	private ArrayList<String> tabPrompt = null;
	
	public Client() {};
	
	/**
	 * Constructeur pour un nouveau client
	 * @param id
	 * @param numeroAppele
	 * @param numeroAppelant
	 * @param label
	 * @param dteOms
	 */
	public Client(String id, String numeroAppele, String numeroAppelant, String label) {
		super();
		mapAttribut = new HashMap<String, String>(5);
		if (Util.isModeDebug()) {
			/* gestion du mode test */
			String num = Util.getTest(numeroAppele+".numeroappelant");
			if (num == null || "".equals(num)) {
				num = Util.getTest("numeroappelant");
				if (num != null && !"".equals(num)) {
					numeroAppelant = num;
				}
			}	
			else numeroAppelant = num;
		}
		
		// On ne passe pas par setValeur pour ne pas envoyer les stats
		// ce sera fait plus bas
		mapAttribut.put(VAR_IDENT,id);
		mapAttribut.put(VAR_APPELANT, numeroAppelant);
		mapAttribut.put(VAR_APPELE, numeroAppele);
		mapAttribut.put(VAR_SVC, numeroAppele);
		mapAttribut.put(VAR_LANGUE, Config.getInstance().getProperty(numeroAppele+NavigationManager.LANGUE_DEF,""));

		mapAttribut.put(VAR_NAV, label);

		this.saisie = new StringBuffer();
	
		mapAttribut.put(VAR_DATE, Long.toString(System.currentTimeMillis())); 
		
		// initialisation des autres attributs defini dans le fichier de config
		String autresVar = Config.getInstance().getProperty(Config.VARIABLE, "");
		if (!"".equals(autresVar)) {
			// de la forme application.variable=_varNom1=CONSTANTE1,_varNom2=_varLangue,...
			String[] tabVar = autresVar.split(",");
			String[] tabCleValeur;
			for (String var : tabVar) {
				tabCleValeur = var.split("=");
				if (tabCleValeur.length == 1) {
					mapAttribut.put(tabCleValeur[0],"");
				}
				else {
					if (tabCleValeur[1].startsWith("_var")) {
						// c'est une variable
						mapAttribut.put(tabCleValeur[0], mapAttribut.get(tabCleValeur[1]));					
					}
					else {
					
						mapAttribut.put(tabCleValeur[0],tabCleValeur[1]);
					}					
				}
			}
		}
		// Statistiques
		ClientFormat f = new ClientFormat(this);
		f.formaterBrut();
		
		setSilenceService(Boolean.parseBoolean(Config.getInstance().getProperty(Config.SILENCE+getService(), "true")));
		
		//StatManager.getInstance().posterStatistiques(getIdent(), 
			//	f.toString(), 
				//System.currentTimeMillis(), 
				//StatManager.CLIENT);
		
	}
	/**
	 * Ce constructeur est appele si le client a deja parcouru le SVI.
	 * La chaine a ete constituee par ClientFormat.formaterBrut() 
	 * @param init : chaine constituee par ClientFormat.formaterBrut()
	 * @param id : identifiant de la connection
	 * @param label: nom de l'item de navigation
	 */
	public Client (String init, String id, String label) {
		/*
		 * ATTENTION les valeurs des variables peuvent contenir des _
		 * notamment pour InfoActive la valeur des segments clients
		 * on remplace donc _var par ~var
		 */
		Pattern pat = Pattern.compile("(~var[a-zA-Z0-9]+):([^~]+)");
		Matcher mat = pat.matcher(init.replaceAll("_var", "~var"));
		this.mapAttribut = new HashMap<String, String>(4);
		this.saisie = new StringBuffer();
		
		while (mat.find()) {
			if ("null".equals(mat.group(2))) mapAttribut.put(mat.group(1).replace('~', '_'), "");
			else {
				String val = mat.group(2).trim();
				if (val.startsWith("CHANGE(")) {
					// valeur a traduire
					int l = val.length() - 1;
					val = val.substring(7,l);
					String valConfig = Config.getInstance().getProperty(Config.VALEUR_CODE+mat.group(1).replace('~', '_')+"."+val,"");				
					if ("".equals(valConfig)) {
						// la valeur à décoder n'a pas ete trouvee
						// on cherche la valeur par defaut							
						valConfig = Config.getInstance().getProperty(Config.VALEUR_CODE+mat.group(1).replace('~', '_')+".defaut", val);
					}
					mapAttribut.put(mat.group(1).replace('~', '_'), valConfig);
				}
				else mapAttribut.put(mat.group(1).replace('~', '_'), val);		
			}
		}
		
		if ("null".equals(label)) {
			mapAttribut.put(VAR_NAV,NavigationManager.getInstance().getRacineSvc(this.getService()));
		}
		else {
			mapAttribut.put(VAR_NAV, label);
		}
		if (getValeur(VAR_IDENT) == null || "".equals(getValeur(VAR_IDENT))) {
			// cas de l'IPBX l'ID n a pas ete transmis
			mapAttribut.put(VAR_IDENT, id);
		}
		if (getValeur(VAR_LANGUE) == null || "".equals(getValeur(VAR_LANGUE))) {
			// cas de l'IPBX la langue n a pas ete chargee
			mapAttribut.put(VAR_LANGUE, Config.getInstance().getProperty(getValeur(VAR_SVC)+NavigationManager.LANGUE_DEF, "FR"));
		}
		if (getValeur(VAR_DATE) == null || "".equals(getValeur(VAR_DATE))) {
			// cas de l'IPBX 
			mapAttribut.put(VAR_DATE, Long.toString(System.currentTimeMillis())); // passage en seconde
		}
		setSilenceService(Boolean.parseBoolean(Config.getInstance().getProperty(Config.SILENCE+getService(), "true")));
	}
	
	public String getNavCourante() {
		
		return mapAttribut.get(VAR_NAV);
	}
	public void setNavCourante(String navCourante) {
		if (navCourante.startsWith("_var")) {
			mapAttribut.put(VAR_NAV, getValeur(navCourante));
		}
		else {
			mapAttribut.put(VAR_NAV, navCourante);
		}
	}

	public String getNavPrecedente() {
		String navPrecedente = mapAttribut.get(VAR_NAV_PREC);

		if(navPrecedente == null || "".equals(navPrecedente)) return "";
		return navPrecedente;
	}
	public void setNavPrecedente(String navPrecedente) {
		mapAttribut.put(VAR_NAV_PREC, navPrecedente);
	}
	public String getSaisie() {
		return saisie.toString();
	}
	public void setSaisie(String saisie) {
		this.saisie.setLength(0);
		this.saisie.append(saisie);
		nbSaisieAjoute = saisie.length();
	}
	public void ajouterSaisie(String saisie) {
		this.saisie.append(saisie);
		nbSaisieAjoute = saisie.length();
	}
	public void resetSaisie() {
		this.saisie.setLength(0);
		nbSaisieAjoute = 0;
	}
	/**
	 * Suppression de i premiers caracteres
	 * @param i
	 */
	public void resetSaisie(int i) {
		saisie.delete(0, i);
		nbSaisieAjoute -= i;
	}
	public String getPremierCaractereSaisi() {
		if (saisie.length() >=1) {
			return saisie.substring(0, 1);
		}
		else return "";
	}
	public void supprimerPremierCaractereSaisi() {
		if (saisie.length() >=1) {
			saisie.delete(0, 1);
			nbSaisieAjoute--;
		}
	}
	public long getTopDepart() {
		return Long.parseLong(this.getValeur(VAR_DATE));
	}

	public String getIdent() {
		return this.getValeur(VAR_IDENT);
	}
	public int getActionNavigation() {
		return actionNavigation;
	}
	public void setActionNavigation(int action) {
		this.actionNavigation = action;
	}
    public void enregistrerNavigation (String nav) {
    	String navOld =  mapAttribut.get(VAR_NAV);
       	
       	mapAttribut.put(VAR_NAV_PREC,navOld);
       	mapAttribut.put(VAR_NAV, nav);
       	actionNavigation = Navigation.RIEN;
    }
    /**
     * getter retournant la valeur d'un attribut de la classe en fonction d'un mot cle
     * @param param : mot cle 
     * @return valeur
     */
    public String getValeur(String param) {

		return mapAttribut.get(param);
    }
    /**
     * setter permettant de valoriser les attributs de la classe en fonction d'un mot clef
     * met a jour les donnees statistiques
     * @param param : mot cle
     * @param valeur : valeur a attribuer a l'attribut de la classe correspondant a param
     * @param isStat : true/false --> indique s'il faut mettre en stat ou pas 
     */
    public void setValeur(String param, String valeur, boolean isStat) {
    	mapAttribut.put(param, valeur);    	
 		if (isStat) {
			// Statistiques
		//	StatManager.getInstance().posterStatistiques(this.getIdent(), 
			//		valeur, 
				//	System.currentTimeMillis(),
					//param);
 		}
		return;		
	}
    public void setValeur(String param, String valeur) {
 		setValeur(param, valeur, true);
    }
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	/* Raccourci */
	public String getService() {
		return mapAttribut.get(VAR_SVC);
	}
	public String getLangue() {
		return mapAttribut.get(VAR_LANGUE);
	}
	public String getNumeroAppelant() {
		return mapAttribut.get(VAR_APPELANT);
	}
	/**
	 * Liste des attributs du client
	 * @return
	 */
	public Set<String> getListeAttribut() {
		return mapAttribut.keySet();
	}
	public boolean isSilenceDemande() {
		return silenceDemande;
	}
	public void setSilenceDemande(boolean silenceDemande) {
		this.silenceDemande = silenceDemande;
	}
	public int getNbSaisieAjoute() {
		return nbSaisieAjoute;
	}
	public void setNbSaisieAjoute(int nbSaisieAjoute) {
		this.nbSaisieAjoute = nbSaisieAjoute;
	}
	public boolean isSilenceService() {
		return silenceService;
	}
	public void setSilenceService(boolean silenceService) {
		this.silenceService = silenceService;
	}
	public int getNbInactivite() {
		return nbInactivite;
	}
	public void setNbInactivite(int nbInactivite) {
		this.nbInactivite = nbInactivite;
	}
	public int getNbRejet() {
		return nbRejet;
	}
	public void setNbRejet(int nbRejet) {
		this.nbRejet = nbRejet;
	}	
	public void incrementerInactivite() {
		nbInactivite++;
		
	}
	public void incrementerRejet() {
		nbRejet++;
		
	}
	public void setPrompt(String valeur) {
		tabPrompt = new ArrayList<String>();
		tabPrompt.add(valeur);		
	}
	public void setPrompt(ArrayList<String> tab) {
		tabPrompt = new ArrayList<String>(tab);			
	}
	public ArrayList<String> getPrompt() {
		return tabPrompt;
	}
	
}
