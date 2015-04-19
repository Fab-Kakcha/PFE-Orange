package orange.olps.svi.navigation;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;
import orange.olps.svi.config.Config;
import orange.olps.svi.util.Util;

public class Condition extends Navigation {
	protected static final String CONDITION = "condition";
	protected static final String VRAI = "suivant.vrai";
	protected static final String FAUX = "suivant.faux";

	/**
	 * La condition peut etre une'vraie' condition qui sera evaluee
	 */
	private String condition = null;
	/**
	 * condition tel qu'elle apparait dans le properties
	 * juste pour restitution dans le json
	 */
	private String conditionOrigine = null;
	/**
	 * liste des variables (_var*) de la condition
	 */
	private ArrayList<String> tabVar = null;

	/**
	 * passe à vrai si la condition est de déterminer l'existence d'un fichier (ou plusieurs)
	 * Mot clef EXISTE()
	 */
	private boolean existeFichier = false;

	public Condition(String lab, String svc) {
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
		setCondition(lstProp.getProperty(racinePropriete+CONDITION,""));

		setAction("Vrai", lstProp.getProperty(racinePropriete+VRAI,"").trim());
		setAction("Faux", lstProp.getProperty(racinePropriete+FAUX,"").trim());

		return ret;
	}


	public String getCondition() {
		return condition;
	}
	public void setCondition(String cond) {
		conditionOrigine = cond;
		/*
		 * On récupère les variables de la condition
		 */
		tabVar = Util.extraireVar(cond);
		Pattern patExiste = Pattern.compile("EXISTE\\([^\\)]+\\)");		
		Matcher mat = patExiste.matcher(cond);
		if (mat.find()) {
			// condition de test d'un fichier ou de plusieurs fichier
			existeFichier = true;			
		}
		else {
			patExiste = Pattern.compile("(_var[^ ]+)[ ]+(IN||NOT[ ]+IN)[ ]+\\(([^)]+)\\)");		
			mat = patExiste.matcher(cond);
			int d = 0;
			int f = 0;
			StringBuffer buffer = null;
			String[] t1;
			while (mat.find()) {				
				if (buffer==null) {
					buffer=new StringBuffer(100);
				}
				// on recherche le groupe IN dans la condition d'origine
				f = cond.indexOf(mat.group(),d);
				// on ajoute les caracteres avant dans le buffer
				buffer.append(cond.substring(d, f));
				buffer.append(" ");
				// on convertit en tableau l'interieur du IN ()
				t1 = mat.group(3).split(",");
				buffer.append("(");
				for (String x1 : t1) {
					buffer.append(mat.group(1));
					if ("IN".equals(mat.group(2))) {
						buffer.append("==");
						buffer.append(x1);
						buffer.append(" || ");
					}
					else {
						buffer.append("!=");
						buffer.append(x1);
						buffer.append(" && ");
					}
				}
				buffer.setLength(buffer.length()-4);
				buffer.append(")");
				d = f+mat.group().length();
			}			

			if (buffer != null) {
				// on ajoute la fin de la condition
				buffer.append(cond.substring(d));
				condition = buffer.toString();
			}
			else {
				condition = cond;
			}
		}
		logger.debug("setCondition - condition("+label+") ="+condition);
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
		// on evalue la condition
		Boolean bool = new Boolean(false);

		if (condition != null) {           
				bool = Boolean.valueOf(Util.interpreterJavaScript(client, tabVar, condition));	            			
		}
		else if(existeFichier) {
			String f,v;
			String rep = Config.getInstance().getRepertoireAudio();
			if (rep == null) rep="";
			String ext = Config.getInstance().getProperty(Config.PROMPT_EXTENSION,"wav");
			boolean existe = true;
			if (tabVar != null) {
				for (String nomAttribut : tabVar) {
					v = client.getValeur(nomAttribut);
					if (v != null ) {
						if (v.indexOf('.') > 0) {
							// le fichier contient une extension
							f = rep + v;
						}
						else {
							f = rep + v + "."+ ext;
						}
						File fic = new File(f);
						if (!fic.exists() || !fic.isFile()) {
							existe = false;
							break;
						}

					}
				}
			}

			bool = existe;
		}

		if(bool != null && bool.booleanValue()) {
			client.setActionNavigation(Navigation.RIEN);
			client.setNavCourante(getVrai());
		}
		else {
			client.setActionNavigation(Navigation.RIEN);
			client.setNavCourante(getFaux());
		}
	}
	@Override
	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "condition");
			obj.put("service", getService());
			obj.put("label", label);
			obj.put("condition",conditionOrigine);
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
