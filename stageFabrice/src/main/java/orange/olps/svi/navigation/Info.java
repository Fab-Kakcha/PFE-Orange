package orange.olps.svi.navigation;

import java.io.File;
import java.util.Properties;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;
import orange.olps.svi.stats.StatManager;
import orange.olps.svi.util.Util;
import orange.olps.svi.config.Config;

public class Info extends Navigation {

	/**
	 * Repertoire des prompts s'ils ne sont pas dans le repertoire par defaut de disserto
	 * Dans ce cas, le logiciel les copiera
	 */
	private String repOrigine = null;
	/**
	 * determine si les saisies anticipées sont valides ou pas
	 */
	private boolean absorbant = false;

	public Info(String lab, String svc) {
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
		
		// définition d'un répertoire de prompts différent du standard
		String s = lstProp.getProperty(racinePropriete+REP_PROMPT, "").trim();
		if (!s.equals("")) {
			File f = new File(s);
			if (f.isDirectory()) {
				if (s.endsWith(File.separator)) {
					repOrigine = s;
				}
				else {
					repOrigine = s+ File.separator;
				}
			}
		}
		
		s = lstProp.getProperty(racinePropriete+ABSORBANT, "").trim();
		if("".equals(s)) {
			s = Config.getInstance().getProperty(Config.ABSORBANT, "");			
		}
		if (!"".equals(s)) {				
			absorbant = Boolean.parseBoolean(s);			
		}
		return true;
	}
	public void setPrompt (String[] tabPrompt) {
		verifierPrompt(tabPrompt);		
	}

	public void calculerActionNavigation(Client client) {
		int actionNav = client.getActionNavigation();
		String saisie = client.getPremierCaractereSaisi();
		logger.debug("calculerActionNavigation - ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+") action precedente="+actionNav+" saisie="+saisie);
		
		if (actionNav == Navigation.RIEN) {
			// premier passage dans cet item
			// Statistiques
			StatManager.getInstance().posterStatistiques(client.getIdent(), 
					label, 
					System.currentTimeMillis(),
					StatManager.NAVIGATION);
		}
		
		if (actionNav == Navigation.RIEN && ("".equals(saisie) || isPromptUnParUn())) {
			// premier passage dans cet item de navigation et pas de dtmf saisie par avance 
			// il faut lire les prompts 
			
			if (preparerPrompt(client) > 0) {
				client.setActionNavigation(DIFFUSION);
				if (repOrigine != null) {
					// le prompt n'est pas dans le repertoire de disserto
					// il faut le copier
					String repOut = Config.getInstance().getRepertoireAudio();
					if (repOut == null) repOut="";
					for (String prompt : this.getPrompt(client.getLangue())) {
						if(prompt.startsWith("_var")) {
							// le prompt est une variable
							prompt = client.getValeur(prompt);
						}
						if(!prompt.endsWith(Config.getInstance().getProperty(Config.PROMPT_EXTENSION,"wav"))) {
							// ajout de l'extension
							prompt +="."+Config.getInstance().getProperty(Config.PROMPT_EXTENSION,"wav");
						}
						Util.copier(repOrigine+prompt, repOut+prompt);
					}
				}
			}
			else {
				// pas de prompt
				logger.warn("calculerActionNavigation - ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+") - pas de prompt");
				if (absorbant || isPromptUnParUn()) {
					// on supprime les saisies anticipees
					client.resetSaisie();
				}
				client.setNavCourante(this.getSuivant());
				client.setActionNavigation(Navigation.RIEN);
			}
		}
		else  {
			// on poursuit la navigation
			if (repOrigine != null) {
				// on supprime des prompts copiés précedemment
				String repOut = Config.getInstance().getRepertoireAudio();
				File f;
				for (String prompt : this.getPrompt(client.getLangue())) {
					if(prompt.startsWith("_var")) {
						// le prompt est une variable
						prompt = client.getValeur(prompt);
					}
					f = new File( repOut+prompt);
					if (f.exists()) f.delete();
				}
			}

			if (absorbant) {
				// on supprime les saisies anticipees
				client.resetSaisie();
			}
			if (isPromptUnParUn()) {
				// on supprime les saisies anticipees
				client.resetSaisie();
				if (actionNav == Navigation.RIEN) {
					// on n'est pas passé dans la phase de lecture du prompt
					// il faut mettre à jour les variables liees au mode un par un
					preparerPrompt(client);
				}
			}
			client.setNavCourante(this.getSuivant());
			client.setActionNavigation(Navigation.RIEN);
		}	
		
		return;
	}
	@Override
	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "info");
			obj.put("service", getService());
			obj.put("label", label);
			//obj.put("suivant", getSuivant());
			obj.put("bargein", bargein);
			obj.put("prompt", reconstituerListePrompt());
			obj.put("mode", promptDynamiqueMode);
			obj.put("maxage", audioMaxage);
			obj.put("prompt_rep", repOrigine);
			obj.put("absorbant", absorbant);
			obj.put("inactivite", timeout);
			if (varUnParUn == null)
				obj.put("unparun", false);
			else
				obj.put("unparun", true);
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
