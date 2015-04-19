package orange.olps.svi.navigation;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.bdd.ConnexionManager;
import orange.olps.svi.bdd.ConnexionUtil;
import orange.olps.svi.client.Client;
import orange.olps.svi.config.Config;
import orange.olps.svi.stats.StatManager;
import orange.olps.svi.util.Util;

public class BaseDonnees extends Navigation {
			
	/*les erreurs */
	public static final String BDD_OK = "BDD000";
	public static final String ERR_NON_TROUVE = "BDD001";
	public static final String ERR_BASE_KO = "BDD002";
	
	
	private String requete;
	/** 
	 * liste des variables de la requete ou de la procedure 
	 * remplacees par des ?
	 */
	private ArrayList<String> tabSelecteur;
	
	/**
	 * liste des variables lues en properties donnant les variables a renseigner
	 * dans l'ordre du select ou des parametres out de la procedure
	 */
	private ArrayList<String> tabVariable;
	private ArrayList<Integer> arrayIndexOut = new ArrayList<Integer>();
	private ArrayList<Integer> arrayIndexIn  = new ArrayList<Integer>();
	/**
	 * valeurs par defaut si erreur d'acces base
	 */
	private ArrayList<String> tabValeurDef;
	private boolean boolStat;
	
	public BaseDonnees(String lab, String svc) {
		super(lab, svc);
		tabSelecteur = new ArrayList<String>(1);
		tabVariable = new ArrayList<String>(1);
		tabValeurDef = new ArrayList<String>(1);
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
		
		// initialisation du ConnexionManager
		ConnexionManager.initialiser();
		
		// on commence par lire la liste des parametres OUT
		// on en aura besoin dans remplacerMotCle
		String s = lstProp.getProperty(racinePropriete+VARIABLE, "");
		if ("".equals(s)) {
			/* on va lire dans le fichier de properties general */
			s = Config.getInstance().getProperty(racinePropriete+VARIABLE, "");
			if ("".equals(s)) {
				/* on va lire dans le fichier de properties general  avec un label general*/
				s = Config.getInstance().getProperty(Config.REQUETE_VAR, "");
			}
		}
		
		if (!"".equals(s)) {
			/* les parametres sont stockes sous la forme d'une liste dont le separateur est une virgule */
			String[] tabParam = s.split(",");
			for (String var : tabParam) {
				tabVariable.add(var);
			}
		}
		
		s = lstProp.getProperty(racinePropriete+REQUETE, "");
		if ("".equals(s)) {
			/* on va lire dans le fichier de properties general */
			s = Config.getInstance().getProperty(racinePropriete+REQUETE, "");
			if ("".equals(s)) {
				// on lit au fichier de properties general la requete commune a tous les services
				s = Config.getInstance().getProperty(Config.REQUETE, "");
			}
		}
		if (!"".equals(s)) {			
			requete = s;
			/* enregistrement dans les requetes a passer */
			ConnexionManager.setRequete(service, label, remplacerMotCle(s));
		}
		
		
		s = lstProp.getProperty(racinePropriete+VALEUR_DEF, "");
		if ("".equals(s)) {
			/* on va lire dans le fichier de properties general */
			s = Config.getInstance().getProperty(racinePropriete+VALEUR_DEF, "");
			if ("".equals(s)) {
				// on lit au fichier de properties general 
				s = Config.getInstance().getProperty(Config.REQUETE_VAL_DEF, "");
			}
		}
		logger.debug("initialiserItem - ("+label+") - valeur defaut ("+s+")");
		if (!"".equals(s)) {
			/* les parametres sont stockes sous la forme d'une liste dont le separateur est une virgule */

			String[] tabParam = s.split(",");
			for (String var : tabParam) {
				tabValeurDef.add(var);
			}
		}
		this.setAction(Navigation.ERREUR,lstProp.getProperty(racinePropriete+Navigation.ERREUR_SUIVANT, Navigation.REJET).trim());
		this.setAction(Navigation.NON_TROUVE,lstProp.getProperty(racinePropriete+Navigation.NON_TROUVE_SUIVANT, Navigation.REJET).trim());
		boolStat = Boolean.parseBoolean(lstProp.getProperty(racinePropriete+STATISTIQUES,"true").trim());
		
		logger.debug("initialiserItem - fin pour ("+label+")");
		return ret;
	}
	/**
	 * La requete du fichier de properties contient des noms de variable.
	 * Il faut les remplacer par le caractere ? et conserver le nom et l'ordre de la variable
	 * @param req : requete
	 * @return requete exploitable
	 */
	private String remplacerMotCle(String req) {
		logger.debug("remplacerMotCle - Entree pour ("+req+")");
		boolean isProc = ConnexionManager.isProcedure(req);
		StringBuffer buf = new StringBuffer(req);		
		Matcher mat = Util.patVar.matcher(buf);
	
		int index = 0;
		int posParam =0;

		while (mat.find(index)) {
			posParam++;
			if (isProc) {
				Boolean trouve = false;
				
				// verification que ce ne soit pas un parametre OUT
				for (String var : tabVariable) {
					if (var.equals(mat.group(1))) {
						// c'est un parametre OUT
						arrayIndexOut.add(posParam);						
						trouve = true;
						break;
					}
				}
				if (!trouve) {
					// c'est un parametre IN
					tabSelecteur.add(mat.group(1));
					arrayIndexIn.add(posParam);
				}				
			}
			else {
				/* on garde le nom de la variable qui sera remplacee a l'execution */
				tabSelecteur.add(mat.group(1));										
			}		
			buf.replace(mat.start(), mat.end(), "?");	
			index = mat.start();
			mat = Util.patVar.matcher(buf);
		}
		logger.debug("remplacerMotCle - Fin pour ("+buf.toString()+")");
		return buf.toString();
	}
	
	public ArrayList<String> getTabSelecteur() {
		return tabSelecteur;
	}
	public ArrayList<String> getTabVariable() {
		return tabVariable;
	}
	public void setTabVariable(ArrayList<String> tabVariable) {
		this.tabVariable = tabVariable;
	}
	public ArrayList<String> getTabValeurDef() {
		return tabValeurDef;
	}
	public void setTabValeurDef(ArrayList<String> tabValeurDef) {
		this.tabValeurDef = tabValeurDef;
	}
	@Override
	public void calculerActionNavigation(Client client) {		
		logger.debug("calculerActionNavigation - ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+") - Entree");
		// Statistiques
		StatManager.getInstance().posterStatistiques(client.getIdent(), 
				label, 
				System.currentTimeMillis(),
				StatManager.NAVIGATION);
		
		ConnexionUtil util = new ConnexionUtil();
		
		util.executerRequete(client, this);
		
		if (isBaseKO(client.getValeur(Client.VAR_ERREUR))) {
			client.setNavCourante(getAction(Navigation.ERREUR));
			client.setActionNavigation(Navigation.RIEN);
		}
		else if (isNonTrouve(client.getValeur(Client.VAR_ERREUR))) {
			String nav = getAction(Navigation.NON_TROUVE);
			if (nav == null || "".equals(nav)) {
				client.setNavCourante(getSuivant());
			}
			else client.setNavCourante(nav);
			client.setActionNavigation(Navigation.RIEN);
		}
		else {
			client.setNavCourante(getSuivant());
			client.setActionNavigation(Navigation.RIEN);
		}
	}
	private boolean isNonTrouve(String valeur) {
			return ERR_NON_TROUVE.equals(valeur);
	}
	private boolean isBaseKO(String valeur) {		
		return ERR_BASE_KO.equals(valeur);
	}
	public ArrayList<Integer> getArrayIndexOut() {
		return arrayIndexOut;
	}
	public ArrayList<Integer> getArrayIndexIn() {
		return arrayIndexIn;
	}
	/**
	 * Retourne la valeur par defaut d'une variable
	 * @param val = nom d'une variable
	 * @return
	 */
	public String getValeurDefaut (String val) {
		int pos = tabVariable.indexOf(val);
		if (pos >= 0) return tabValeurDef.get(pos);
		else return "";
	}
	@Override
	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "bdd");
			obj.put("service", getService());
			obj.put("label", label);
			//obj.put("suivant", getSuivant());
			obj.put("requete", requete);
			obj.put("statistiques", boolStat);
			StringBuffer buf = new StringBuffer();
			for (String v: tabVariable) {
				if (buf.length() == 0) buf.append(v);
				else {
					buf.append(',');
					buf.append(v);
				}
			}
			obj.put("variable", buf.toString());
			buf.setLength(0);
			for (String v: tabValeurDef) {
				if (buf.length() == 0) buf.append(v);
				else {
					buf.append(',');
					buf.append(v);
				}
			}
			obj.put("valeur_defaut", buf.toString());
			buf.setLength(0);
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
	public boolean isStat() {
		
		return boolStat;
	}
}
