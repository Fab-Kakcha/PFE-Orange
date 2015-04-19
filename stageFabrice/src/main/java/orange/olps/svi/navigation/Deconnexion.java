package orange.olps.svi.navigation;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;


public class Deconnexion extends Navigation {
	/**
	 * Valeur retour qui sera renvoyee au CCXML dans le namelist de l'exit VXML
	 */
	private String valeurRetour = "";
	public Deconnexion(String lab, String svc) {
		super(lab, svc);
		
	}
	/**
	 * Initialisation de la navigation
	 * @return true si c'est un nouveau item false si clone
	 */
	public boolean initialiserItem(Properties lstProp) {
		logger.debug("initialiserItem - Entree pour ("+label+")");
		super.initialiserItem(lstProp);
		String racinePropriete = service+".navigation."+label+".";
		setValeurRetour(lstProp.getProperty(racinePropriete+VALEUR,"NORMAL"));	
		return true;
	}
	@Override
	public void calculerActionNavigation(Client client) {
		int actionNav = client.getActionNavigation();
		
		logger.debug("calculerActionNavigation - ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+") action precedente="+actionNav);
		
		
		if (actionNav == Navigation.RIEN && this.isPrompt()) {
			// premier passage dans cet item de navigation
			// il faut lire les prompts 
			if (preparerPrompt(client)>0) {
				client.setActionNavigation(DIFFUSION);
			}
			else {
				client.setActionNavigation(DECONNEXION);
			}
		}
		else  {
			// on poursuit la navigation
			client.setActionNavigation(DECONNEXION);

		}	
		
		return;
		
	}
	public String getValeurRetour() {
		return valeurRetour;
	}
	public void setValeurRetour(String valeurRetour) {
		this.valeurRetour = valeurRetour;
	}
	@Override
	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "deconnexion");
			obj.put("service", getService());
			obj.put("label", label);
			obj.put("bargein", bargein);
			obj.put("maxage", audioMaxage);
			obj.put("mode", promptDynamiqueMode);
			obj.put("valeur", valeurRetour);
			obj.put("prompt", reconstituerListePrompt());
			obj.put("inactivite", timeout);
			
		} catch (JSONException e) {
			logger.error("toJsonObject - "+e.getMessage());
		}
		
		return obj;
	}
}
