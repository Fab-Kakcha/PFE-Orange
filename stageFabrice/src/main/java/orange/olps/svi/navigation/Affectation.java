package orange.olps.svi.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;
import orange.olps.svi.stats.StatManager;
import orange.olps.svi.util.Util;

public class Affectation extends Navigation {

	
	/**
	 * Classe interne stockant les affectation unitaire
	 * de la liste
	 * @author awar6486
	 *
	 */
	private class AffectationUnitaire {
		public static final int AFFECTATION_SIMPLE = 0;
		public static final int AFFECTATION_VALEUR = 1;
		public static final int AFFECTATION_JS = 2;
		
		AffectationUnitaire() {	}
		/**
		 * Variable du client (_var*) qu'il faut valoriser
		 */
		public String var = null;
		/**
		 * Valeur telle qu'elle est écrite dans le fichier de properties
		 */
		public String val = null;
		/**
		 * Liste des variables présentes dans val.
		 * exemple : Ipbx.navigation.renvoiOccup.valeur=_varNumClient=_varNumAppele.substring(2)
		 * var <-- _varNumClient
		 * val <-- _varNumAppele.substring(2)
		 * varVal <-- _varNumAppele
		 */
		public ArrayList<String> varVal = null;
		/**
		 * type de l'affectation.
		 */
		public int type = 0;
		/**
		 * indicateur de trace en statistiques ou pas 
		 */		
		
		public String toString() {
			return var+"="+val;
		}
	}
	
	/**
	 * liste de toutes les affectations a effectuer sur cet item
	 */
	private List<AffectationUnitaire> listAffectation;
	private boolean boolStat = false;;
	
	public Affectation(String lab, String svc) {
		super(lab, svc);
		listAffectation = new ArrayList<AffectationUnitaire>(1);
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
		AffectationUnitaire affectation;
		String[] tab;
		// decoupage de la succession d'affectations
		for (String aff : lstProp.getProperty(racinePropriete+VALEUR,"").split(",")) {
			tab = aff.split("=");
			if (tab.length >=2) {
				affectation = new AffectationUnitaire();
				affectation.var=tab[0].trim();
				affectation.val=tab[1].trim();
				
				affectation.varVal = Util.extraireVar(affectation.val);
				if (affectation.varVal == null || affectation.varVal.size() == 0) {
					// pas de variables dans la valeur
					// on supprime les " et '
					affectation.val = tab[1].trim().replaceAll("[\"']", "");
					affectation.type=AffectationUnitaire.AFFECTATION_SIMPLE;
				}
				else if (affectation.varVal.size() == 1 && affectation.varVal.get(0).equals(affectation.val)) {
					// le decoupage en variable est  egal à la valeur
					// pas de traitement specifique en javascript
					affectation.type=AffectationUnitaire.AFFECTATION_VALEUR;
				}
				else {
					// utilisation de javascript pour evaluer la valeur
					affectation.type=AffectationUnitaire.AFFECTATION_JS;
				}
				listAffectation.add(affectation);
			}
		}
		boolStat = Boolean.parseBoolean(lstProp.getProperty(racinePropriete+STATISTIQUES,"false").trim());
		return ret;
	}


	@Override
	public void calculerActionNavigation(Client client) {
		logger.debug("calculerActionNavigation ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+" - Entree");
				
		// Statistiques
		StatManager.getInstance().posterStatistiques(client.getIdent(), 
				label, 
				System.currentTimeMillis(),
				StatManager.NAVIGATION);
	
	
		for (AffectationUnitaire aff : listAffectation) {
			
			switch (aff.type) {
			case AffectationUnitaire.AFFECTATION_SIMPLE:
				client.setValeur(aff.var,aff.val, boolStat);
				break;
			case AffectationUnitaire.AFFECTATION_VALEUR:
				client.setValeur(aff.var, client.getValeur(aff.val),boolStat);
				break;
			case AffectationUnitaire.AFFECTATION_JS:
				client.setValeur(aff.var, Util.interpreterJavaScript(client, aff.varVal, aff.val), boolStat);
				break;
			default:
				break;
			}
			
		}
		client.setActionNavigation(Navigation.RIEN);
		client.setNavCourante(getSuivant());

	}
	@Override
	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "affectation");
			obj.put("service", getService());
			obj.put("label", label);
			//obj.put("suivant", getSuivant());
			obj.put("statistiques", boolStat);
			StringBuffer buf = new StringBuffer();
			for (AffectationUnitaire a : listAffectation ) {				
			
				if (buf.length() == 0) buf.append(a.toString());
				else {
					buf.append(',');
					buf.append(a.toString());
				}
			}
			obj.put("valeurs", buf.toString());
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
