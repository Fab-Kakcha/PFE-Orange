package orange.olps.svi.navigation;


import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;
import orange.olps.svi.config.Config;
import orange.olps.svi.util.Util;

public  class Prepare extends Navigation {
	
		/**
		 * Filtre pour les prompts personnalises
		 *
		 */
		private class FiltrePrompt implements FilenameFilter {
	
			private String filtre;
			public FiltrePrompt (String f) {
				filtre = f;
			}
			public boolean accept(File dir, String name) {		
				if (name.matches(filtre)) return true;
				return false;
			}
		};

		/**
		 * decoupage du filtre
		 */
		private ArrayList<String> decoupeFiltre;		
		
		/** 
		 * Nom de la variable client qui sera valorisée avec la liste des prompts
		 */
		private String varListePrompt;
		/** 
		 * Nom de la variable client qui sera valorisée avec le nombre de prompts
		 */
		private String varNbPrompt;

		private boolean diffusionAleatoire = false;
		
		protected static Log logger = LogFactory.getLog(Prepare.class.getName());

		
		
		/**
		 * Constructeur
		 */
		public Prepare(String lab, String svc) {			
			super(lab, svc);			
			decoupeFiltre = new ArrayList<String>();	
			varListePrompt = "_var"+label+"L";
			varNbPrompt = "_var"+label+"Nb";
			
		}

		/**
		 * Initialisation de la navigation
		 * @return true si c'est un nouveau item false si clone
		 */
		protected boolean initialiserItem(Properties lstProp) {
			logger.debug("initialiserItem - Entree pour ("+label+")");
			super.initialiserItem(lstProp);
			
			boolean ret = true;
			String rep = Config.getInstance().getRepertoireAudio();
			if (rep == null) rep="";
			String ext = Config.getInstance().getProperty(Config.PROMPT_EXTENSION,"wav");
			String racinePropriete = service+".navigation."+label+".";
			diffusionAleatoire = Boolean.parseBoolean(lstProp.getProperty(racinePropriete+ALEATOIRE, "false").trim());
			
			String s = lstProp.getProperty(racinePropriete+FILTRE, "").trim();
			if (!s.equals("")) {
				// recherche des (_var..)
				decoupeFiltre = Util.decouperStringParenthese(s);
				// construction de la regexp qui s'appliquera sur les fichiers wave
				String bufferRegexp = Util.reconstituerString(decoupeFiltre, "[a-zA-Z0-9]+");
				
				if (bufferRegexp == null) {				
					bufferRegexp = s;
				}
				logger.debug("initialiserItem - Regexp ("+bufferRegexp+") pour ("+label+")");
				// Fin de la construction de la regexp
				FiltrePrompt filtre = new FiltrePrompt(bufferRegexp);
				String[] tabFic;
				File dir = new File (rep);
				if (dir.exists() && dir.isDirectory()) {						
					// lecture des fichiers correspondant au filtre
					tabFic = dir.list(filtre);
					Arrays.sort(tabFic);
					
					int lgExtension = ext.length() + 1;
					int p = 0;
					String langue="";
					StringBuffer buff = new StringBuffer();
					
					String[] tabLangue = Config.getInstance().getListeLangue(); // langues du service
					boolean trouve;
					ArrayList<String> arrayPrompt = null;
					for (String fic : tabFic) {
																		
						// le fichier matche avec la regexp construite
						logger.debug("initialiserItem - Fichier ("+fic+") OK pour regexp ("+bufferRegexp+" pour ("+label+")");
						// on enleve l'extension
						buff.append(fic.substring(0, fic.length() - lgExtension));
						trouve = false;
						p = 0;
						// recherche de la langue qui dans ce cas peut être ailleurs qu'en fin
						do {
							p = buff.lastIndexOf("_");
							langue = buff.substring(p+1);
							for (String lgue : tabLangue) {
								if (lgue.equals(langue)) {
									trouve = true;
									break;
								}
							}
							if (trouve)
								break;
							else {
								buff.setLength(p);
							}
						} while (!trouve && p > 0);
						
						if (trouve) {
							// on a trouve la langue
							// on recupere la liste des prompts
							if (mapPromptLong == null) mapPromptLong = new HashMap<String, ArrayList<String>>();
							arrayPrompt = mapPromptLong.get(langue);
							if ( arrayPrompt == null) {
								arrayPrompt = new ArrayList<String>();
								mapPromptLong.put(langue, arrayPrompt);
							}
							arrayPrompt.add(fic.substring(0, fic.length() - lgExtension));							
						}
						buff.setLength(0);
					} // fin de boucle sur les prompts
					mapPromptCourt = mapPromptLong;
					if (decoupeFiltre.get(decoupeFiltre.size() - 1).endsWith(ext)) {
						// on enleve l'extension du filtre
						String dernier = decoupeFiltre.remove(decoupeFiltre.size() - 1);
						decoupeFiltre.add(dernier.substring(0, dernier.lastIndexOf('.')));
					}
				}
			}		
			this.setAction(Navigation.ERREUR,lstProp.getProperty(racinePropriete+Navigation.ERREUR_SUIVANT, Navigation.REJET).trim());
			return ret;
		}


		@Override
		public void calculerActionNavigation(Client client) {
			logger.debug("calculerActionNavigation - ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+") - Entree");
						
			// premier passage dans cet item
			// Statistiques

			boolean isPrompt = false;
			if (client.getValeur(varListePrompt) == null) {
				// la liste des prompts n'a pas été construite pour ce client/cet item
				isPrompt = filtrerPrompt(client);
			}
			else {
				isPrompt = !"0".equals(client.getValeur(varNbPrompt));
			}
			client.setActionNavigation(Navigation.RIEN);
			if (isPrompt) {			
				client.setNavCourante(getSuivant());
			}
			else {
				client.setNavCourante(getAction(Navigation.ERREUR));
			}
		}

		/**
		 * Filtre les prompts du tableau en fonction des données clients
		 * @param client = client appelant
		 * @return liste de prompts séparés par des virgules
		 */
		private boolean filtrerPrompt(Client client) {
			// Creation du masque
				
			String msq = Util.reconstituerString(decoupeFiltre, client);
			logger.debug("filtrerPrompt - appelant="+client.getValeur(Client.VAR_APPELANT)+" navigation ="+client.getNavCourante()+" filtre ("+msq+")");
			
			StringBuffer rslt = new StringBuffer("");
			int nb = 0;
			if (getPrompt(client.getLangue()) != null) {
				ArrayList<String> arr = getPrompt(client.getLangue());
				if (diffusionAleatoire ) {
					Collections.shuffle(arr);
				}
				for (String p : arr) {
					if (p.matches(msq)) {
						if (rslt.length() > 0) rslt.append(',');
						rslt.append(p);
						nb++;
					}					
				}
			}
			client.setValeur(varListePrompt, rslt.toString(), false);
			client.setValeur(varNbPrompt, String.valueOf(nb), false);
			return nb != 0;
		}
		@Override
		public JSONObject toJsonObject() {
			JSONObject obj = new JSONObject();
			try {
				obj.put("type", "prepare");
				obj.put("service", getService());
				obj.put("label", label);
				//obj.put("suivant", getSuivant());
				obj.put("filtre", Util.reconstituerString(decoupeFiltre));
				//obj.put("erreur_suivant", getAction(ERREUR));
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
