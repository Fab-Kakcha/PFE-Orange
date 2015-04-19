package orange.olps.svi.navigation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;
import orange.olps.svi.config.Config;
import orange.olps.svi.navigation.Navigation;
import orange.olps.svi.navigation.BaseDonnees;
import orange.olps.svi.navigation.Langue;
import orange.olps.svi.util.Util;

public class NavigationManager {

	private static String NAV_RACINE =".navigation.depart";
	public  static final String LANGUE_DEF = ".langue.defaut";
	/* map de stockage des items de navigation
	 * la clef est generee a partir du service et du label de l'item
	 */
	private static  Map <String, Navigation> mapNavigation;
	// stockage des points d'entree des SVI
	// la clef est le service
	private static  Map <String, String> mapRacineSvc;
	private static NavigationManager navManager;
	/**
	 * passe a vrai si un objet base de donnees est utilise
	 */
	private static boolean useBdd = false;
	/**
	 * passe a vrai si un objet Webservice est utilise
	 */
	private boolean useWebSvc = false;
	/**
	 * passe a vrai si un objet SMS est utilise
	 */
	private boolean useSms = false;

	protected static Log logger = LogFactory.getLog(NavigationManager.class.getName());


	private NavigationManager () {
		mapNavigation = new HashMap <String, Navigation>(5);
		mapRacineSvc = new  HashMap <String, String>(1);
	}

	public static  NavigationManager getInstance() {
		logger.debug("getInstance - Entree ");
		if (navManager == null) {
			synchronized (NavigationManager.class) {
				if (navManager == null) {
					navManager = new NavigationManager();
				}
			}
		}		
		return navManager;
	}
	private void initialiserNavigation(String [] tabService) {
		File ficProp;

		for (String service : tabService) {

			if (!"".equals(service)) {
				ficProp =  Config.getInstance().getFileProperties("Svi"+service+".properties");		
				if (ficProp != null) {							
					navManager.initialiserNavigationService (ficProp, service);
				}

			}
		}	
	}
	private void initialiserNavigation(JSONObject objGlob) {
		// tableau de tous les services de navigation
		JSONArray arrSvc;
		try {
			arrSvc = objGlob.getJSONArray("racines");		

			for (int i = 0; i < arrSvc.length(); i++) {
				navManager.initialiserNavigationService(objGlob, arrSvc.getString(i));
			}
		} catch (JSONException e) {
			logger.error("initialiserNavigation - JSON - pas de tableau racines");
		}
	}
	/**
	 * Chargement du fichier de proprietes decrivant la navigation d'un service
	 * @param ficProp : fichier des proprietes
	 * @param service : service auquel est rattache le fichier de proprietes
	 */
	private void initialiserNavigationService(File ficProp, String service) {
		Properties prop = new Properties();

		try {
			/* lecture du fichier properties */
			FileInputStream fis = new FileInputStream(ficProp);
			prop.clear();
			prop.load(fis);
			fis.close();
			// lecture de la duree max dans le service
			String tempo = prop.getProperty(service+"."+Config.TEMPO, Config.getInstance().getProperty(Config.TEMPO,"-1"));
			Util.addTpsMax(service, tempo);

			/* lecture de la liste des points d'entree du SVI */
			String[] tabRacine = prop.getProperty(service+NAV_RACINE, "bienvenue").split(",");

			/* ajout de l'item de rejet pour le service
			 */
			navManager.setNavigation(service, Navigation.REJET, new Rejet(Navigation.REJET, service));

			for (String racine : tabRacine) {
				logger.debug("initialiserNavigationService - charger navigation de ("+racine+")");
				chargerNavigation(prop, racine.trim(), service);
			}
			// le point d'entrée par défaut est le premier de la liste
			mapRacineSvc.put(service, tabRacine[0].trim());
			// transfert de la langue par defaut pour le service
			// dans le properties general (en memoire seulement)
			Config.getInstance().setProperty(service+LANGUE_DEF,
					prop.getProperty(service+LANGUE_DEF,"FR").trim());

		} catch (IOException ioe) {
			logger.error("initialiserNavigationService - erreur d'initialisation : "+ioe.getMessage());			
		}	

	}
	/**
	 * Chargement du fichier json decrivant la navigation d'un service
	 * @param obj : fichier json
	 * @param service : service auquel est rattache le fichier de proprietes
	 */
	private void initialiserNavigationService(JSONObject obj, String service) {
		Properties prop = new Properties();

		try {
			// on positionne une duree dans le service par defaut
			Util.addTpsMax(service, Config.getInstance().getProperty(Config.TEMPO,"-1"));

			/* ajout de l'item de rejet pour le service
			 */
			navManager.setNavigation(service, Navigation.REJET, new Rejet(Navigation.REJET, service));

			String racineProp = service+"navigation.";

			// tableau de tous les items de navigation
			JSONArray arrNav = obj.getJSONArray("navigations");
			JSONObject nav;
			for (int i = 0; i < arrNav.length(); i++) {
				nav = arrNav.getJSONObject(i);
				if (nav.getString("service").equals(service)) {
					// l'item correspond bien a un item du service
					if (nav.getString("type").equals("depart")) {
						String tmp = nav.getString("tempo");
						if (tmp != null) {
							Util.addTpsMax(service, tmp);// on positionne une duree dans le service
						}
						tmp =  nav.getString("suivant");
						if (tmp != null) {
							// le point d'entrée dans le service
							mapRacineSvc.put(service, tmp);
						}
						tmp =  nav.getString("langue");
						if (tmp != null) {
							// langue par defaut du service
							Config.getInstance().setProperty(service+LANGUE_DEF, tmp);									
						}
						else {
							Config.getInstance().setProperty(service+LANGUE_DEF,"FR");
						}
					}
					else {
						// ce n'est pas un depart
						// on convertit le json en properties
						prop.clear();
						String[] tabAttribut = JSONObject.getNames(nav);
						for (String att : tabAttribut) {
							if (att.equals("actions")) {
								JSONArray arr = nav.getJSONArray("actions");
								for (int k = 0; k<arr.length();k++) {
									prop.setProperty(racineProp+nav.getString("label")+"."+arr.getJSONObject(k).getString("action"),
											arr.getJSONObject(k).getString("suivant"));
								}
							}
							else {
								prop.setProperty(racineProp+nav.getString("label")+"."+att.replaceAll("_", "."), nav.getString(att));
							}
						}
						// on charge ce mini properties
						chargerNavigation(prop, nav.getString("label"), service);
					}

				}
			}

		} catch (JSONException e) {
			logger.error("initialiserNavigationService - mauvais format Json "+e.getMessage());
		}	

	}
	public  void initialiser () {

		if (mapNavigation != null) {
			mapNavigation.clear();
		}
		logger.debug("initialiser - lecture navigation");
		JSONObject obj = Config.getInstance().getFileJson();
		if (obj == null) {
			// chargement à partir du properties
			/* lecture des services autorises */
			String [] tabService=Config.getInstance().getProperty(Config.SERVICE_AUTORISE, "").trim().split(",");
			/* lecture de tous les fichiers de propriétés liés aux numeros autorisés */
			navManager.initialiserNavigation(tabService);

			/* lecture des properties complémentaires */
			tabService=Config.getInstance().getProperty(Config.PROP_COMPL, "").trim().split(",");
			/* lecture de tous les fichiers de proprietés complémentaires*/
			navManager.initialiserNavigation(tabService);
		}
		else {
			navManager.initialiserNavigation(obj);
		}

	}
	private Navigation creerItem (String type, String label, String service) {
		Navigation nav = null;
		if (!"".equals(type)) {
			if ("AFFECTATION".equals(type)) {
				nav = new Affectation(label,service);
			}
			else if ("BDD".equals(type)) {
				useBdd = true;
				nav = new BaseDonnees(label,service);
			}
			else if ("CASE".equals(type)) {
				nav = new Case(label,service);
			}
			else if ("CONDITION".equals(type)) {
				nav = new Condition(label,service);
			}
			else if ("DECONNEXION".equals(type)) {
				nav = new Deconnexion(label,service);
			}
			else if ("ENREG".equals(type)) {
				nav = new Enregistrement(label,service);
			}
			else if ("INFO".equals(type)) {
				nav = new Info (label,service);
			}
			else if ("LANGUE".equals(type)) {
				nav = new Langue (label,service);
			}
			else if ("MENU".equals(type)) {
				nav = new Menu (label,service);
			}
			else if ("PREPARE".equals(type)) {
				nav = new Prepare (label,service);
			}
			else if ("REDIRECT".equals(type)) {
				nav = new Redirection(label,service);
			}
			else if ("SAISIE".equals(type)) {
				nav = new Saisie(label,service);
			}
			else if ("SMS".equals(type)) {
				nav = new Sms(label,service);
				useSms = true;
			}
			else if ("STATISTIQUES".equals(type)) {
				nav = new Statistiques(label,service);			
			}
			else if ("TRANSFERT".equals(type)) {
				nav = new Transfert(label,service);
			}
			else if ("WEBSVC".equals(type)) {
				useWebSvc = true;
				nav = new WebService(label,service);
			}
		}
		return nav;
	}
	/**
	 * Chargement d'un item de navigation et lecture de tous les items fils de celui-ci recursivement
	 * 
	 * @param prop : fichier des properties
	 * @param label : nom du label de navigation
	 * @param service : numero du service
	 */
	private void chargerNavigation (Properties prop, String label, String service) {	
		logger.debug(" chargerNavigation - Entree ("+label+") service ("+service+")");
		String type = prop.getProperty(service+".navigation."+label+".type","");
		Navigation navDepart = creerItem(type, label, service);
		if (navDepart == null) return;

		navDepart.initialiserItem(prop);

		/* ajout de cet item a la liste des dialogues de navigation 
		 * la clef est constituee du nom du service + le label
		 */
		navManager.setNavigation(service, label, navDepart);

		if (navDepart.getClass().getName() != Redirection.class.getName()) {
			/*
			 *  On traite les dependances 
			 *  on va boucler sur tous les labels suivant:
			 *  
			 */
			Iterator<String> it = navDepart.getMapAction().keySet().iterator();
			String dtmf;
			String labelFils;
			Navigation nav;
			while (it.hasNext()) {
				/* lecture du nom du dialogue associe a une dtmf */
				dtmf = it.next();
				logger.debug(" chargerNavigation - dtmf ("+dtmf+") pour ("+label+")");
				labelFils = navDepart.getAction(dtmf);

				if (labelFils != null 
						&& !Navigation.REJET.equals(labelFils) 
						&& !"".equals(labelFils)) {
					logger.debug(" chargerNavigation - traitement de la dependance ("+label+"/"+labelFils+")");
					nav = navManager.getNavigation(service,labelFils);
					if (nav == null) {
						/* cet item n'a pas encore ete ajoute */
						chargerNavigation (prop,labelFils,service);					
					}
				}
			}
		}
		else {
			// sur redirection on ne suit pas le lien suivant. Il  est charge par le service sur lequel on se redirige
		}
	}	

	public Navigation getNavigation(String svc, String label) {
		logger.debug("getNavigation - service ("+svc+") nom ("+label+") ");
		String key = svc+"_"+label;
		Navigation n =  mapNavigation.get(key);
		if( n == null) {
			// cette navigation n'existe pas
			logger.debug("getNavigation - l'item ("+svc+"/"+label+") n'a pas ete charge en memoire");
		}
		return n;
	}

	public void setNavigation(String svc, String label, Navigation nav) {
		String key = svc+"_"+label;
		mapNavigation.put(key, nav);			
	}
	/**
	 * retourne le label de navigation d'entree dans le service
	 * @param service : service (SVI)
	 * @return label de l'item de navigation racine du service
	 */
	public String getRacineSvc (String service) {
		return mapRacineSvc.get(service);
	}
	/**
	 * Determine l'action suivante
	 * @param client : client en ligne
	 * @return
	 */
	public int calculerActionNavigation(Client client) {
		logger.debug("calculerActionNavigation - client ("+client.getIdent()+") Entree");
		Navigation nav;
		do {

			// recuperation de l'objet de navigation courant
			nav = this.getNavigation(client.getService(), client.getNavCourante());
			if (nav == null) {
				logger.error("calculerActionNavigation - client ("+client.getIdent()+") service ("+client.getService()+") pas de navigation");
				client.setActionNavigation(Navigation.DECONNEXION);
				break;				
			}
			else {
				// calculer l'action de navigation suivante
				nav.calculerActionNavigation(client);
			}
		} while (client.getActionNavigation() == Navigation.RIEN);

		return client.getActionNavigation();
	}
	/**
	 * Retourne true si la fonctionnalité est ouverte
	 * @param service = service
	 * @param label : label de navigation
	 * @return true l'item de navigation existe dans la map
	 */
	public boolean isFonctionnaliteOk (String service, String label) {
		logger.debug("isFonctionnaliteOk - Entree ("+label+" "+service+")");

		Navigation nav =this.getNavigation(service, label);
		if (nav == null) {
			logger.debug("isFonctionnaliteOk - retour = KO");
			return false;
		}
		else {
			logger.debug("isFonctionnaliteOk - OK");
			return true;
		}
	}

	public boolean isUseBdd() {
		logger.debug("isUseBdd - "+useBdd);
		return useBdd;
	}

	public boolean isUseWebSvc() {
		return useWebSvc;
	}

	public boolean isUseSms() {
		return useSms;
	}

	public void setUseSms(boolean useSms) {
		this.useSms = useSms;
	}
	/**
	 * Transformation des objets du svi en objet json:
	 * {"racines":["950","951"],"navigations":[{item1},{item2},...]}
	 * @return
	 */
	public String toJsonString() {
		String ret = null;
		JSONObject obj = new JSONObject();
		try {
			JSONArray arrNavigations = new JSONArray();
			JSONArray arr = new JSONArray(); // tableau des services (racines)
			JSONObject objNav ; // element de navigation
			for (Entry<String,String> en : mapRacineSvc.entrySet()) {
				// le point d'entree est transforme en objet de navigation
				objNav = new JSONObject(); // element de navigation
				objNav.put("service", en.getKey());
				objNav.put("label", en.getKey());
				objNav.put("type", "depart");

				JSONArray arrActions = new JSONArray();
				JSONObject o = new JSONObject();
				o.put("action", "suivant");
				o.put("suivant", en.getValue());
				arrActions.put(o);
				objNav.put("actions",arrActions);

				objNav.put("langue", Config.getInstance().getProperty(en.getKey()+LANGUE_DEF,"FR"));
				objNav.put("tempo", Util.getTimerApplication(en.getKey()));
				// ajout au tableau de navigations
				arrNavigations.put(objNav);
				// ajout au tableau des points d'entree
				arr.put(en.getKey());
			}		
			// ajout du tableau des points d'entree a l'objet global
			obj.put("racines", arr);
			// parcours de la map des items de navigation
			for (Entry<String, Navigation> en : mapNavigation.entrySet()) {
				arrNavigations.put(en.getValue().toJsonObject());				
			}
			obj.put("navigations", arrNavigations);
			ret = obj.toString();
			logger.debug("toJsonString - "+ret);
		} catch (JSONException e) {
			logger.error("toJsonString - "+e.getMessage());
		}

		return ret;
	}

}
