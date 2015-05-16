/**
 * 
 */
package com.orange.olps.api.webrtc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import orange.olps.svi.config.Config;
import orange.olps.svi.navigation.Navigation;
import orange.olps.svi.navigation.NavigationManager;
import orange.olps.svi.util.Util;

import org.java_websocket.WebSocket;

/**
 * @author JWPN9644
 *
 */
public class OmsClientSvi extends OmsCall{
	
	
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
	public static final String VAR_SEGMENT = "_varSegment";
	public static final String VAR_METIER = "_varMetier";
	public static final String VAR_RAISON = "_varRaison"; // raison de la deconnexion (INACTIVITE/TIMEOUT/REJET)
	public static final String VAR_TMP = "_varTmp";
	public static final String VAR_NAV = "_varNav";
	public static final String VAR_NAV_PREC = "_varNavPre";
	public static final String VAR_TRANSFERT = "_varTransfert"; // numero de transfert (utilisé seulement pour les stats)
	
	public static final String INACTIVITE = "INACTIVITE";
	public static final String TIMEOUT = "TIMEOUT";
	public static final String REJET = "REJET";
	
	
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
	private ArrayList<String> tabPrompt = null;

	/**
	 * donne le nbre de caracteres saisis depuis la derniere saisie
	 */
	private int nbSaisieAjoute = 0;
	/**
	 * index permet de stocker un numero de prompt a jouer
	 * Utilise par Saisie lors d'un erreur où il faut jouer le prompt
	 * correspondant à l'erreur dans la liste de prompts
	 */
	private int index = -1;
	
	
	public OmsClientSvi(WebSocket conn, String ipAddress, String id, String numeroAppele, String label){
	
		super(conn, ipAddress);
		
		String numeroAppelant = conn.toString();
		
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
		//ClientFormat f = new ClientFormat(this);
		//f.formaterBrut();
		
		//setSilenceService(Boolean.parseBoolean(Config.getInstance().getProperty(Config.SILENCE+getService(), "true")));
		
	}
	
	public String getService() {
		return mapAttribut.get(VAR_SVC);
	}
	
	public void setSilenceService(boolean silenceService) {
		this.silenceService = silenceService;
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
	
	 /**
     * getter retournant la valeur d'un attribut de la classe en fonction d'un mot cle
     * @param param : mot cle 
     * @return valeur
     */
    public String getValeur(String param) {

		return mapAttribut.get(param);
    }
    
    public String getLangue() {
		return mapAttribut.get(VAR_LANGUE);
	}
    
	public String getNumeroAppelant() {
		return mapAttribut.get(VAR_APPELANT);
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
	
	public String getIdent() {
		return this.getValeur(VAR_IDENT);
	}
	public int getActionNavigation() {
		return actionNavigation;
	}
	public void setActionNavigation(int action) {
		this.actionNavigation = action;
	}
	
	public void setValeur(String param, String valeur) {
 		setValeur(param, valeur, true);
	}
	
	public void setValeur(String param, String valeur, boolean isStat) {
    	mapAttribut.put(param, valeur);    		
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
	
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	
	public int getNbSaisieAjoute() {
		return nbSaisieAjoute;
	}
	public void setNbSaisieAjoute(int nbSaisieAjoute) {
		this.nbSaisieAjoute = nbSaisieAjoute;
	}
	
	public void resetSaisie() {
		this.saisie.setLength(0);
		nbSaisieAjoute = 0;
	}
	
	public boolean isSilenceDemande() {
		return silenceDemande;
	}
	public void setSilenceDemande(boolean silenceDemande) {
		this.silenceDemande = silenceDemande;
	}
	
	public long getTopDepart() {
		return Long.parseLong(this.getValeur(VAR_DATE));
	}
	
	public Set<String> getListeAttribut() {
		return mapAttribut.keySet();
	}

}
