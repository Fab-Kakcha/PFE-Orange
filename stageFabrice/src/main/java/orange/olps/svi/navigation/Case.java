package orange.olps.svi.navigation;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;
//import orange.olps.svi.stats.StatManager;

public class Case extends Navigation {
	
	
	
	/**
	 * nom de la variable à tester
	 */
	private String variable;

	
	public Case(String lab, String svc) {
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
		
		variable = lstProp.getProperty(racinePropriete+lstProp.getProperty(racinePropriete+VARIABLE),"").trim();
		String s;
		String v;
		for (int i =1; ;i++) {
			s = lstProp.getProperty(racinePropriete+lstProp.getProperty(racinePropriete+i+"."+SUIVANT),"").trim();
			v = lstProp.getProperty(racinePropriete+lstProp.getProperty(racinePropriete+i+"."+VALEUR),"").trim();
			if ("".equals(s) || "".equals(v)) break;
			setAction(v, s);
		}
		s = lstProp.getProperty(racinePropriete+lstProp.getProperty(racinePropriete+DEFAUT),"").trim();
		if (!"".equals(s)) {
			setAction(DEFAUT, s);
		}
		return ret;
	}


	
	/**
	 * Retourne l'action a effectuer si la condition est remplie
	 * @return
	 */
	public String getVrai() {
		return getAction("Vrai");
	}
	/**
	 * Retourne l'action a effectuer si la condition n'est pas remplie
	 * @return
	 */
	public String getFaux() {
		return getAction("Faux");
	}
	@Override
	public void calculerActionNavigation(Client client) {
		logger.debug("calculerActionNavigation ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+") - Entree");
		
		String nav = null;
		if (variable != null && !"".equals(variable)) {
			String val = client.getValeur(variable);
			
			if (val != null) {
				for (Entry<String, String> en : getAction().entrySet()) {
					if (val.equals(en.getKey())) {
						nav = this.getAction(en.getKey());
						client.setActionNavigation(Navigation.RIEN);
						client.setNavCourante(nav);
						break;
					}
				}				
			}
			
		}
		if (nav == null) {
			// on n'a pas pu definir la navigation suivante
			// on va essayer de prendre le defaut
			nav = this.getAction(DEFAUT);
			if (nav == null || "".equals(nav)) {
				client.setActionNavigation(Navigation.RIEN);
				client.setNavCourante(Navigation.REJET);
				client.setNavPrecedente(client.getNavPrecedente());
			}
			else {
				client.setActionNavigation(Navigation.RIEN);
				client.setNavCourante(nav);
			}
		}
		
	}
	@Override
	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "case");
			obj.put("service", getService());
			obj.put("label", label);
			JSONArray arr = new JSONArray();
			JSONObject o ;
			for (Entry<String, String> en : getMapAction().entrySet()) {
				if (!REJET.equals(en.getValue()) 
				&& !"".equals(en.getValue()) ) {
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
