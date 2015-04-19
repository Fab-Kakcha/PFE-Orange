package orange.olps.svi.navigation;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;
import orange.olps.svi.util.Util;

public class Statistiques extends Navigation {
	protected static final String SERVICE = "service";

	

	private ArrayList<String> valeur = null;

	private String typeValeur;
	
	public Statistiques(String lab, String svc) {
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
		
		this.valeur = Util.decouperString(lstProp.getProperty(racinePropriete+VALEUR,""));
		this.typeValeur = lstProp.getProperty(racinePropriete+VALEUR+"."+TYPE,"");
		
		return ret;
	}

	@Override
	public void calculerActionNavigation(Client client) {
		logger.debug("calculerActionNavigation - ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+") - Entree");
		
		client.setActionNavigation(Navigation.RIEN);
		client.setNavCourante(getSuivant());
		
	}
	@Override
	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "statistiques");
			obj.put("service", getService());
			obj.put("label", label);
			obj.put("valeur", valeur);
			obj.put("valeur_type", typeValeur);
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
