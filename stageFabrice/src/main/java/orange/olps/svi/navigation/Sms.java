package orange.olps.svi.navigation;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;
import orange.olps.svi.config.Config;
import orange.olps.svi.sms.SMSUtil;
import orange.olps.svi.stats.StatManager;
import orange.olps.svi.util.Util;

public class Sms extends Navigation {

	/**
	 * Elements du texte à envoyer découpé en fonction des _var
	 */
	private ArrayList<String> texte = null;
	
	/**
	 * nom complet du fichier contenant le texte du SMS
	 * découpé en fonction des _var
	 */
	private ArrayList<String> fichierTexte = null;
	
	public Sms(String lab, String svc) {
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
		String s = lstProp.getProperty(racinePropriete+VALEUR, "");
			// mode par defaut
			// le texte du SMS est dans la propriete .valeur		
			
		if ("".equals(s)) {
			/* on va lire dans le fichier de properties general */
			s = Config.getInstance().getProperty(racinePropriete+VALEUR, "");
			if ("".equals(s)) {
				// on lit au fichier de properties general la requete commune a tous les services
				s = Config.getInstance().getProperty(Config.SMS_VALEUR, "");
			}
		}
		if (!"".equals(s)) {
			texte = Util.decouperString(s);
			logger.debug("Texte  -  ("+texte.toString()+")");
		}	
		else {
			// mode fichier
			s = lstProp.getProperty(racinePropriete+FICHIER, "").trim();
			if ("".equals(s)) {
				/* on va lire dans le fichier de properties general */
				s = Config.getInstance().getProperty(racinePropriete+FICHIER, "").trim();
				if ("".equals(s)) {
					// on lit au fichier de properties general la requete commune a tous les services
					s = Config.getInstance().getProperty(Config.SMS_FICHIER, "").trim();
				}
			}			
			if (!"".equals(s)) {
				// on regarde s'il y a un repertoire definit
				String rep = lstProp.getProperty(racinePropriete+REPERTOIRE, "").trim();
				if ("".equals(rep)) {
					/* on va lire dans le fichier de properties general */
					rep = Config.getInstance().getProperty(racinePropriete+REPERTOIRE, "").trim();
					if ("".equals(rep)) {
						// on lit au fichier de properties general la requete commune a tous les services
						rep = Config.getInstance().getProperty(Config.SMS_REPERTOIRE, "").trim();
					}
				}
				if (!"".equals(rep)) {
					s = rep +File.separator+ s;
				}
				if(s.contains("_var")) {
					// le nom du fichier est fonction du client
					fichierTexte = Util.decouperString(s.replaceAll("[\"'+()]", ""));
					logger.debug("Fichier  -  ("+fichierTexte.toString()+")");
				}
				else {
					// on va preparer le travail en lisant le texte
					texte = Util.decouperString(Util.lireFichier(s));
					logger.debug("Texte issu du fichier -  ("+texte.toString()+")");
					
				}
			}
		}
		
		return ret;
	}


	@Override
	public void calculerActionNavigation(Client client) {
		logger.debug("calculerActionNavigation - ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+")");
				
		// Statistiques
		StatManager.getInstance().posterStatistiques(client.getIdent(), 
				label, 
				System.currentTimeMillis(),
				StatManager.NAVIGATION);
		if (texte != null) {
			String sms = Util.reconstituerString(texte, client);
			SMSUtil.getInstance().posterSms(client.getIdent(), label, client.getNumeroAppelant(), sms);
			
		}
		else {
			// le nom du fichier est paramétré
			String nom = Util.reconstituerString(fichierTexte, client);
			String sms = Util.lireFichier(nom);
			
			// nom court du fichier pour les stats
			String racineNomString;
			int pos1 = nom.lastIndexOf('/');
			if (pos1 == -1) pos1=0;
			else pos1++;
			int pos2 = nom.indexOf('.');
			if (pos2 == -1) {
				racineNomString = nom;
			}
			else {
				racineNomString = nom.substring(pos1, pos2);
			}
			if (sms != null && !"".equals(sms)) {
				// le texte peut contenir des variables
				ArrayList<String> l = Util.decouperString(sms);
				sms = Util.reconstituerString(l, client);
				SMSUtil.getInstance().posterSms(client.getIdent(), racineNomString, client.getNumeroAppelant(), sms);
			}
			else {
				StatManager.getInstance().posterStatistiques(client.getIdent(), 
						racineNomString+";Echec:1",
						System.currentTimeMillis(),
						StatManager.SMS);
			}
		}
		
		client.setActionNavigation(Navigation.RIEN);
		client.setNavCourante(getSuivant());

	}
	@Override
	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "sms");
			obj.put("service", getService());
			obj.put("label", label);
			
			if (texte != null) {
				obj.put("valeur",Util.reconstituerString(texte));
			}
			if (fichierTexte != null) {
				obj.put("fichier",Util.reconstituerString(fichierTexte));
			}
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
