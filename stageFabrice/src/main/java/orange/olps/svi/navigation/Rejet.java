package orange.olps.svi.navigation;

import java.util.ArrayList;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;
import orange.olps.svi.config.Config;
import orange.olps.svi.stats.StatManager;

public class Rejet extends Navigation {

	public Rejet(String lab, String svc) {
		super(lab, svc);
		
	}
	protected boolean initialiserItem(Properties lstProp) {
		logger.debug("initialiserItem - Entree pour ("+label+")");
		return true;	
	}
	@Override
	public void calculerActionNavigation(Client client) {

		Menu menu = (Menu) NavigationManager.getInstance().getNavigation(client.getService(), client.getNavPrecedente());
		int rejetMax = menu.getNbErreurMax();
		
		logger.debug("traiterRejet ("+label+")-  ("+client.getIdent()+") Nb rejets/Max rejets ("+client.getNbRejet()+"/"+rejetMax+")");
		StatManager.getInstance().posterStatistiques(client.getIdent(), label, System.currentTimeMillis(), StatManager.NAVIGATION);
		
		client.incrementerRejet();
		client.resetSaisie();
		client.setNavCourante(menu.getLabel()); // on se repositionne sur le menu qui a provoque le rejet
		
		if(client.getNbRejet() >= rejetMax && rejetMax >=0) {
			// on est arrive au max de rejet
			 String nav = menu.getAction(Navigation.ERREUR);
			 if(nav == null || "".equals(nav) || Navigation.REJET.equals(nav) ) {
				 // pas d'item de navigation prevu
				 String message = Config.getInstance().getProperty(Config.DISSUASION_ERREUR,"");
				 StatManager.getInstance().posterStatistiques(client.getIdent(), Client.REJET, System.currentTimeMillis(), Client.VAR_RAISON);
				 if("".equals(message)) {
					 client.setActionNavigation(Navigation.DECONNEXION);
				 }
				 else {
					 client.setPrompt(message+"_"+client.getLangue());
					 client.setActionNavigation(Navigation.DISSUASION);
				 }			 
			 }
			 else {				 
				 // un item de navigation est identifie
				 client.setNavPrecedente(client.getNavCourante());
				 client.setNavCourante(nav);
				 client.setActionNavigation(Navigation.RIEN);					 
				 client.setNbRejet(0);
				 client.setNbInactivite(0);
			 }
		}
		else {
			// nombre max pas atteint
			ArrayList<String> tabErr = menu.getPromptErreur(client);
			if (tabErr != null && tabErr.size() >0) {
				//on diffuse les messages d'erreurs	
				int index = client.getIndex();
				if (index == -1) {
					  // diffusion de tous les messages d'erreur
					  client.setPrompt(tabErr);
				}
				else {
					client.setPrompt(tabErr.get(index));					
				}
			
				client.setActionNavigation(Navigation.DIFFUSION_REJET);
			}
			else {
				// pas de message à diffuser, on va reboucler	dans NavigationManager			
				client.setActionNavigation(Navigation.RIEN);
			}
			
		}
		
	}
	@Override
	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "rejet");
			obj.put("service", getService());
			obj.put("label", label);
			
		} catch (JSONException e) {
			logger.error("toJsonObject - "+e.getMessage());
		}
		
		return obj;
	}

}
