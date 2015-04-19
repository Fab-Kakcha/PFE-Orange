package orange.olps.svi.navigation;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;
import orange.olps.svi.config.Config;
import orange.olps.svi.util.Util;


public class Menu extends Navigation {
	
	
	/**
	 * Ces messages seront diffuses lorsque le client aura fait une erreur de saisie
	 */
	protected Info erreur = null;
	/**
	 * Nombre d'erreurs max
	 */
	protected int nbErreurMax = -1;
	/**
	 * Nbre de repetitions max du menu
	 */
	protected int nbInactiviteMax = 0;

	/**
	 * Prompt a lire a chaque passage dans l'inactivite
	 */
	protected Info inactivite = null;
	
	
	/**
	 * 
	 * @param lab
	 * @param svc
	 */
	public Menu(String lab, String svc) {
		super(lab, svc);
		
	}
	/**
	 * Initialisation de la navigation
	 * @return true si c'est un nouveau item false si clone
	 */
	public boolean initialiserItem(Properties lstProp) {
		logger.debug(this.getClass().getName()+"::initialiserItem - Entree pour ("+label+")");
		super.initialiserItem(lstProp);
		String racinePropriete = service+".navigation."+label+".";
		boolean ret = true;
		String s;

		/* initialisation des actions */
		initialiserDTMF(lstProp);
		
		// GESTION DES ERREURS
		// nbre d'erreurs max
		s = lstProp.getProperty(racinePropriete+ERREUR_MAX,"null").trim();
		if ("null".equals(s)) {
			// pas defini, on prend la valeur generale
			s = Config.getInstance().getProperty(Config.APPLI_NB_RJT_MAX, "1000").trim();
		}
		try {
			setNbErreurMax(Integer.parseInt(s));
		} catch (NumberFormatException e) {
			logger.error(this.getClass().getName()+"::initialiserItem - erreur de format pour "+racinePropriete+INACTIVITE_MAX);
			setNbErreurMax(1000);
		}
		// item de navigation sur trop d'erreurs
		this.setAction(ERREUR,lstProp.getProperty(racinePropriete+ERREUR_SUIVANT, Navigation.REJET).trim());
		
		// Detection de la presence de prompt d'erreur
		s = lstProp.getProperty(racinePropriete+PROMPT_ERREUR, "");
		if ("".equals(s)) {
			// non renseigne, on prend le general
			s = Config.getInstance().getProperty(Config.PROMPT_ERREUR,"").trim();
		}
		if (!"".equals(s) && !AUCUN.equals(s)) {
			erreur = new Info (label, service);
			erreur.setPrompt(s.trim().split(","));
		}
	
		
		// GESTION DE L'INACTIVITE
		// nbre de repetitions max
		s = lstProp.getProperty(racinePropriete+INACTIVITE_MAX,"null").trim();
		if ("null".equals(s)) {
			// pas defini, on prend la valeur generale
			s = Config.getInstance().getProperty(Config.APPLI_NB_INCTVT_MAX, "1000").trim();
		}
		try {
			setNbInactiviteMax(Integer.parseInt(s));
		} catch (NumberFormatException e) {
			logger.error(this.getClass().getName()+"::initialiserItem - erreur de format pour "+racinePropriete+INACTIVITE_MAX);
			setNbInactiviteMax(1000);
		}
 
		// prompts de boucle d'inactivité
		s = lstProp.getProperty(racinePropriete+INACTIVITE_PROMPT,"").trim();
		if ("".equals(s)) {
			// non renseigné, on prend le general
			s = Config.getInstance().getProperty(Config.PROMPT_INACTIVITE,AUCUN).trim();
		}
		logger.debug(this.getClass().getName()+"::initialiserItem - prompt d'inactivite ="+s);
		if (!"".equals(s) && !AUCUN.equals(s)) {
			inactivite = new Info (label, service);
			inactivite.setPrompt(s.trim().split(","));
			
		}
		// l'item de navigation a activer suite à trop d'inactivité
		setInactivite(lstProp.getProperty(racinePropriete+INACTIVITE_SUIVANT,"").trim());
	
		
		return ret;
	}


	public int getNbInactiviteMax() {
		return nbInactiviteMax;
	}
	public void setNbInactiviteMax(int nbInactiviteMax) {
		this.nbInactiviteMax = nbInactiviteMax;
	}

	public ArrayList<String> getPromptErreur(Client client) {
		if (erreur == null) return null;
		if (erreur.isPromptDynamique()) {
			// il y a des prompts dynamiques , il faut retravailler la liste
			return Util.reconstituerListePrompt(erreur.getPrompt(client.getLangue()), client);
		}
		return erreur.getPrompt(client.getLangue());
	}
	public ArrayList<String> getPromptInactivite(Client client) {
		if (inactivite == null) return null;
		if (inactivite.isPromptDynamique()) {
			// il y a des prompts dynamiques , il faut retravailler la liste
			return Util.reconstituerListePrompt(inactivite.getPrompt(client.getLangue()), client);
		}
		return inactivite.getPrompt(client.getLangue());
	}
	public void setErreur(Info erreur) {
		this.erreur = erreur;
	}
	public Info getErreur() {
		return erreur;
	}
	public String getInactivite() {	
		logger.debug(this.getClass().getName()+"::getInactivite - Entree ("+mapAction.get(INACTIVITE)+")");
		String s = mapAction.get(INACTIVITE);
		if (s==null) {
			return "";
		}			
		return s;
	}
	protected void setInactivite(String inac) {
		setAction(INACTIVITE, inac);
	}

	public int getNbErreurMax() {
		return nbErreurMax;
	}
	public void setNbErreurMax(int nbErreurMax) {
		this.nbErreurMax = nbErreurMax;
	}
	public void calculerActionNavigation(Client client) {
		int actionNav = client.getActionNavigation();
		String saisie = client.getPremierCaractereSaisi();
		logger.debug("calculerActionNavigation - ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+") action precedente="+actionNav+" saisie="+saisie);
		
		if (Navigation.RIEN == actionNav) {			
			// Statistiques
			
			if (!client.getNavCourante().equals(client.getNavPrecedente())){
				// premier passage
				client.setNbInactivite(0);
				client.setNbRejet(0);
			}
		}
		
		if (Navigation.RIEN == actionNav && "".equals(saisie)) {
			// premier passage dans cet item de navigation
			// il faut lire les prompts du menu
			preparerPrompt(client);
			if (isPromptManquant(client.getValeur(Client.VAR_LANGUE))) {
				client.setActionNavigation(DIFFUSION);
			}
			else {
				client.setActionNavigation(MENU_SAISIE);
			}
		}
		else if (Navigation.DIFFUSION == actionNav) {
			// on vient de diffuser le prompt manquant
			client.resetSaisie();
			client.setNavCourante(client.getNavPrecedente());
			client.setActionNavigation(Navigation.RIEN);
		}
		else if (!"".equals(saisie)) {
			// les prompts ont  été  diffusés (ou pas si saisie anticipee)
			String resultatSaisie = this.getAction(saisie);

			if (PRECEDENT.equals(resultatSaisie)) {
				client.setNavCourante(client.getNavPrecedente());
			}
			else {
				client.setNavCourante(resultatSaisie);
			}
			
			client.supprimerPremierCaractereSaisi(); // on a consommé un item
			client.setActionNavigation(Navigation.RIEN);
			client.setNavPrecedente(label);
			client.setSilenceDemande(true);
			if (client.getNavCourante() != Navigation.REJET) {
				// on quitte reellement cet item
				client.setNbInactivite(0);
				client.setNbRejet(0);
			}
		}
		else {
			// c'est de l'inactivte on rejoue le menu 
			preparerPrompt(client);
			client.setActionNavigation(MENU_SAISIE);
		}
		
		return;
	}
	@Override
	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		logger.debug("toJsonObject - ("+label+")");
		try {
			obj.put("type", "menu");
			obj.put("service", getService());
			obj.put("label", label);
			obj.put("suivant", getSuivant());
			obj.put("bargein", bargein);
			obj.put("prompt", reconstituerListePrompt());
			obj.put("mode", promptDynamiqueMode);
			obj.put("maxage", audioMaxage);
			
			obj.put("inactivite", timeout);
			obj.put("inactivite_max", getNbInactiviteMax());

			if (inactivite != null) {
				obj.put("inactivite_prompt", inactivite.reconstituerListePrompt());
			}
			

			obj.put("erreur_max", getNbErreurMax());
	
			if (erreur != null) {
				obj.put("erreur_prompt", erreur.reconstituerListePrompt());
			}
			JSONArray arr = new JSONArray();
			JSONObject o ;
			for (Entry<String, String> en : getMapAction().entrySet()) {
				if (!REJET.equals(en.getValue()) 
				&& !"".equals(en.getValue())) {
					o = new JSONObject();
					o.put("action", en.getKey());
					o.put("label", en.getValue());
					arr.put(o);
				}
			}
			obj.put("actions",	arr);
		} catch (JSONException e) {
			logger.error("toJsonObject - "+e.getMessage());
		}
		
		return obj;
	}

}
