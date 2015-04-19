package orange.olps.svi.navigation;

import java.util.Properties;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;

public class Redirection extends Navigation {
	protected static final String SERVICE = "service";

	private String serviceRedirection = null;
	
	public Redirection(String lab, String svc) {
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
		boolean ret = true;
		
		this.serviceRedirection = lstProp.getProperty(racinePropriete+SERVICE,"");
		
		logger.debug("initialiserItem - Fin - service ("+serviceRedirection+")");
		return ret;
	}

	@Override
	public void calculerActionNavigation(Client client) {
		logger.debug("calculerActionNavigation - ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+") - Entree");
		
		// premier passage dans cet item
		// Statistiques

		if (serviceRedirection != null) {
			client.setValeur(Client.VAR_SVC, serviceRedirection);
		}
		client.setActionNavigation(Navigation.RIEN);
		client.setNavCourante(getSuivant());
		
	}
	@Override
	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "redirection");
			obj.put("service", getService());
			obj.put("label", label);
			obj.put("service_redirect", serviceRedirection);
			//obj.put("suivant",getSuivant());
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
			obj.put("actions",arr);
			
		} catch (JSONException e) {
			logger.error("toJsonObject - "+e.getMessage());
		}
		
		return obj;
	}
}
