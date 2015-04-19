package orange.olps.svi.navigation;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;
import orange.olps.svi.config.Config;
import orange.olps.svi.stats.StatManager;
import orange.olps.svi.util.Util;

public class Transfert extends Navigation {

	
	private ArrayList<String> numeroTransfert = null;
	private ArrayList<String> parametre = null;
	private boolean boolStat;
	
	public Transfert(String lab, String svc) {
		super(lab, svc);
			}
	/**
	 * Initialisation de la navigation
	 * @return true 
	 */
	public boolean initialiserItem(Properties lstProp) {
		logger.debug("initialiserItem - Entree pour ("+label+")");
		super.initialiserItem(lstProp);
		String racinePropriete = service+".navigation."+label+".";
		boolean ret = true;
		String s = lstProp.getProperty(racinePropriete+NUM_TRANSFERT, "");
		if ("".equals(s)) {
		// on cherche le numero global au service
			s = Config.getInstance().getProperty(racinePropriete+NUM_TRANSFERT,"");
			if ("".equals(s)) {
				// on cherche le numero global a tous les services
				s = Config.getInstance().getProperty(Config.NUM_TRANSFERT, "");				
			}			
		}
		if (!"".equals(s)) {
			numeroTransfert = Util.decouperString(s);
		}
		
		/* les parametres sont stockes sous la forme cle1=valeur1&cle2=valeur2,... */
		s = lstProp.getProperty(racinePropriete+PARAMETRE, "");
		if (!"".equals(s)) {
			parametre = Util.decouperString(s);
		}
		boolStat = Boolean.parseBoolean(lstProp.getProperty(racinePropriete+STATISTIQUES,"true").trim());
		return ret;
	}

	public String getNumeroTransfertAvecParam(Client client) {
		logger.debug("getNumeroTransfertAvecParam - Client = "+client.getIdent());
		
		String ret = Util.reconstituerString(numeroTransfert, client);
		if (parametre != null) {
			ret += ";"+Util.reconstituerString(parametre, client);
		}
		logger.debug("getNumeroTransfertAvecParam - num="+ret);
		return ret;
	}

	public void calculerActionNavigation(Client client) {
		int actionNav = client.getActionNavigation();
		
		logger.debug("calculerActionNavigation - appelant="+client.getNumeroAppelant()+" navigation ="+client.getNavCourante()+" action precedente="+actionNav);
		if (Navigation.RIEN == actionNav) {
			// premier passage dans cet item
			// Statistiques
			StatManager.getInstance().posterStatistiques(client.getIdent(), 
					label, 
					System.currentTimeMillis(),
					StatManager.NAVIGATION);
		}
		if (Navigation.RIEN ==  actionNav  && this.isPrompt() && preparerPrompt(client) > 0) {
			// premier passage dans cet item de navigation
			// il faut lire les prompts 		
				client.setActionNavigation(DIFFUSION);
				
		}
		else  {
			if (boolStat && numeroTransfert != null) {
				String numero = Util.reconstituerString(numeroTransfert, client); 
				StatManager.getInstance().posterStatistiques(client.getIdent(), 
						numero.split(";")[0], 
						System.currentTimeMillis(),
						Client.VAR_TRANSFERT);
			}
			// on part en transfert
			client.setActionNavigation(TRANSFERT);
			client.setNavPrecedente(label);;
			client.setNavCourante(getSuivant());			
		}	
		
		return;
	}
	@Override
	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "transfert");
			obj.put("service", getService());
			obj.put("label", label);
			//obj.put("suivant", getSuivant());
			obj.put("bargein", bargein);
			obj.put("statistiques", boolStat);
			obj.put("prompt", reconstituerListePrompt());
			obj.put("mode", promptDynamiqueMode);
			obj.put("maxage", audioMaxage);
			obj.put("numero", Util.reconstituerString(numeroTransfert));
			obj.put("parametre", Util.reconstituerString(parametre));
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
