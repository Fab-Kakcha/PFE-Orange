package orange.olps.svi.navigation;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;
import orange.olps.svi.config.Config;

public class Langue extends Menu {
	
	public Langue(String lab, String svc) {
		super(lab, svc);
	}
	/**
	 * Initialisation de la navigation
	 * @return true si c'est un nouveau item false si clone
	 */
	public boolean initialiserItem(Properties lstProp) {
		logger.debug("initialiserItem - Entree pour ("+label+")");
		
		super.initialiserItem(lstProp);
		
				
		return true;
	}


	public void calculerActionNavigation(Client client) {
		int actionNav = client.getActionNavigation();
		String saisie = client.getPremierCaractereSaisi();
		logger.debug("calculerActionNavigation - ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+") action precedente="+actionNav+" saisie="+saisie);
		
		if (actionNav == Navigation.RIEN) {
			// premier passage dans cet item

			
			if (!client.getNavCourante().equals(client.getNavPrecedente())){
				// premier passage
				client.setNbInactivite(0);
				client.setNbRejet(0);
			}
		}
		if (actionNav == Navigation.RIEN && "".equals(saisie)) {
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
			// les prompts ont  ete diffuses(ou pas si saisie anticipee)
			String resultatSaisie = this.getAction(saisie);
			
			client.supprimerPremierCaractereSaisi(); // on a consomme un item
			client.setActionNavigation(Navigation.RIEN);
			client.setSilenceDemande(true);
			
			if (Config.getInstance().isLangue(resultatSaisie)) {
				// c'est une langue
				client.setValeur(Client.VAR_LANGUE,resultatSaisie);
				client.setNavCourante(this.getSuivant());				
				
				client.setNbInactivite(0);
				client.setNbRejet(0);
				
			}
			else {
				client.setNavCourante(resultatSaisie);								
				client.setNavPrecedente(label);
			}
		}
		else {
			// c'est de l'inactivite on rejoue le menu 
			preparerPrompt(client);
			client.setActionNavigation(MENU_SAISIE);
		}
		logger.debug("calculerActionNavigation - retour = "+client.getActionNavigation());
		return;
	}
	public JSONObject toJsonObject() {
		JSONObject obj = super.toJsonObject();
		try {
			obj.put("type", "langue");
			
		} catch (JSONException e) {
			logger.error("toJsonObject - "+e.getMessage());
		}
		
		return obj;
	}

}
