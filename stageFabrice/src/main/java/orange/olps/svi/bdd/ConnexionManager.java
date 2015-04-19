package orange.olps.svi.bdd;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import orange.olps.svi.config.Config;

public class ConnexionManager  {
	public static final String SELECT = "S";
	public static final String PROCEDURE = "P";
	public static final String UPDATE = "U";
	public static final String REFERENCE = "R";
	private static final String POOL_DEF = "defaut";

	private Map<String, PoolConnexion> mapPool = null;

	private static ConnexionManager conManager = new ConnexionManager();

	protected static Log logger = LogFactory.getLog(ConnexionManager.class.getName());


	/*
	 * Recupere une connexion disponible
	 */
	public static Connexion getConnexion() {
		return getConnexion (POOL_DEF);
	}
	public static Connexion getConnexion(String nomPool) {
		PoolConnexion pool = conManager.mapPool.get(nomPool);
		if (pool == null) return null;
		return pool.getConnexion();
	}

	/**
	 * Enregistrement des requetes 
	 * @param svc : nom du service
	 * @param label : nom de l'item de navigation
	 * @param req : texte de la requete
	 */
	public static void setRequete (String svc, String label, String req) {
		setRequete(svc, label, req, POOL_DEF);
	}
	public static void setRequete (String svc, String label, String req, String nomPool) {
		PoolConnexion pool = conManager.mapPool.get(nomPool);
		if (pool == null) return;
		pool.setRequete(svc, label, req);
	}
	/**
	 * generation d'une cle pour la map des requetes
	 * @param svc : nom du service
	 * @param label : nom de l'item de navigation
	 * @return cle de la map des requetes
	 */
	public static String getCle (String svc, String label) {
		return svc+"_"+label;
	}
	/**
	 * determine si la requete lie au service/label de navigation est un select ou pas
	 * @param svc : nom du service
	 * @param label  : nom de l'item de navigation
	 * @return vrai si la requete est un select, faux sinon
	 */
	public static boolean isKeySelect (String cle) {		
		return isKeySelect(cle, POOL_DEF);
	}
	public static boolean isKeySelect (String cle, String nomPool) {
		PoolConnexion pool = conManager.mapPool.get(nomPool);
		if (pool == null) return false;
		return pool.isKeySelect(cle);
	}

	/**
	 * determine si la requete lie au service/label de navigation est une procedure stockee ou pas
	 * @param svc : nom du service
	 * @param label  : nom de l'item de navigation
	 * @return vrai si la requete est une procedure, faux sinon
	 */
	public static boolean isKeyProcedure (String svc, String label, String nomPool) {
		return isKeyProcedure(ConnexionManager.getCle(svc, label), nomPool);
	}
	public static boolean isKeyProcedure (String key) {		
		return isKeyProcedure(key, POOL_DEF);
	}
	public static boolean isKeyProcedure (String key, String nomPool) {		
		PoolConnexion pool = conManager.mapPool.get(nomPool);
		if (pool == null) return false;
		return pool.isKeyProcedure(key);
	}
	public static boolean isProcedure (String req) {			 
		return req.matches("\\{[ ]*call.*");
	}
	
	/**
	 * determine si la requete lie au service/label de navigation est une reference vers une autre requete
	 * @param svc : nom du service
	 * @param label  : nom de l'item de navigation
	 * @return vrai si la requete est une reference, faux sinon
	 */
	public static boolean isKeyReference (String svc, String label, String nomPool) {		

		return isKeyReference(svc, label, nomPool);
	}
	public static boolean isKeyReference(String key) {	
		return isKeyReference(key, POOL_DEF);
	}
	public static boolean isKeyReference (String key, String nomPool) {	
		PoolConnexion pool = conManager.mapPool.get(nomPool);
		if (pool == null) return false;
		return pool.isKeyReference(key);
	}
	/**
	 * donne la cle referencee par la cle en entree
	 * @param key = cle de reference
	 * @return cle reference
	 */
	public static String getCleRef(String key) {		
		return  getCleRef(key, POOL_DEF);
	}
	public static String getCleRef(String key, String nomPool) {
		
		if (isKeyReference(key)) {
			PoolConnexion pool = conManager.mapPool.get(nomPool);
			if (pool == null) return key;
			return pool.getCleRef(key);
		}
		return key;
	}

	/* Constructeur du singleton */
	private ConnexionManager() { 
		mapPool = new HashMap<String, PoolConnexion>(1);
	}

	public  static void supprimer() {
		synchronized (ConnexionManager.class) {		

			if (conManager.mapPool != null && conManager.mapPool.size() > 0) {
				for (Entry<String, PoolConnexion> en: conManager.mapPool.entrySet()) {
					en.getValue().supprimer();
				}
				conManager.mapPool.clear();
			}
		}
	}

	/**
	 * initialise le driver par defaut
	 */
	public static void initialiser() {

		logger.debug("initialiser - Entree");
		if (conManager.mapPool.size() == 0) {
			synchronized (ConnexionManager.class) {	
				if (conManager.mapPool.size() == 0) {
					String driver = Config.getInstance().getProperty(Config.BDD_DRIVER);
					if (driver != null && driver.length() > 0) {
						String url = Config.getInstance().getProperty(Config.BDD_URL,"");
						String user = Config.getInstance().getProperty(Config.BDD_USER,"");
						String passwd = Config.getInstance().getProperty(Config.BDD_PSSWD,"");
						PoolConnexion pool = new PoolConnexion(driver, url, user, passwd);
						if (pool != null) {
							pool.setCnxMax(Config.getInstance().getProperty(Config.BDD_CNX_MAX,""));
							pool.setCnxMin(Config.getInstance().getProperty(Config.BDD_CNX_MIN,""));
							pool.setTimeout(Config.getInstance().getProperty(Config.BDD_CNX_TIMEOUT,""));
							pool.setTpsAttente(Config.getInstance().getProperty(Config.BDD_THRD_TMPO,""));
							pool.setTpsCnx(Config.getInstance().getProperty(Config.BDD_CNX_TMPO,""));
							pool.setQueryTimeout(Config.getInstance().getProperty(Config.BDD_RQ_TMT,""));
							conManager.mapPool.put(POOL_DEF, pool);
						}
					}
				}
			}
		}
		else {
			logger.debug("initialiser - Pool defaut deja initialise");
		}

		return;
	}
	/**
	 * Creation d'un pool de connexion autre que le defaut
	 * @param driver
	 * @param url
	 * @param user
	 * @param passwd
	 * @param nom
	 * @return
	 */
	public static void creerPoolConnexion(String driver, String url, String user, String passwd, String nom) {
		PoolConnexion pool = new PoolConnexion(driver, url, user, passwd);
		synchronized (ConnexionManager.class) {	
			conManager.mapPool.put(nom, pool);
		}
		return;
	}
	public static void fermerPool (String nom) {
		PoolConnexion pool = null;
		synchronized (ConnexionManager.class) {	
			pool = conManager.mapPool.remove(nom);
		}
		if (pool == null) return;
		pool.supprimer();
	}
	public static void setParamPool(String nomPool, 
			String cnxMax,
			String cnxMin,
			String cnxTimeOut,
			String threadTempo,
			String cnxTempo,
			String reqTimeout) {
		PoolConnexion pool = conManager.mapPool.get(nomPool);
		if (pool == null) return;
		pool.setCnxMax(cnxMax);
		pool.setCnxMin(cnxMin);
		pool.setTimeout(cnxTimeOut);
		pool.setTpsAttente(threadTempo);
		pool.setTpsCnx(cnxTempo);
		pool.setQueryTimeout(reqTimeout);
	}
	public static void demarrerPool(String nomPool) {
		PoolConnexion pool = conManager.mapPool.get(nomPool);
		if (pool == null) return;
		pool.initialiser();		
	}

}
