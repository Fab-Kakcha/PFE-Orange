package orange.olps.svi.bdd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PoolConnexion implements Runnable {

	private List<Connexion> _listConnexion = new ArrayList<Connexion>();
	/**
	 *  map de touites les requetes
	 * cle = service + "_"+label
	 */
	private  Map<String, String> mapRequete = new HashMap<String, String>(1);
	/* cette map donne pour la meme clef que la map precedente l'indication si la requete est un select une procedure ou autre */
	private  HashMap<String, String> mapTypeRequete = new HashMap<String, String>(1);

	private boolean _boolDriverInitialise = false;

	private String url;
	private String user;
	private String passwd;

	private  Thread _thread = null;
	private  boolean isServiceOra = false;
	private long tpsAttente = 600000;
	private long cnxMax = 10;
	private int cnxMin = 0;
	private long tpsCnx =600000;
	private int queryTimeout = 4;
	private String driver = "";
	/** 
	 * Time out de connexion à la base 
	 */
	private int timeout = 4;
	

	protected static Log logger = LogFactory.getLog(PoolConnexion.class.getName());


	public PoolConnexion(String driver, String url, String user, String passwd) {
		this.driver = driver;
		this.url = url;
		this.user = user;
		this.passwd = passwd;
		isServiceOra = url.contains(":oracle:");
	}
	/*
	 * Recupere une connexion disponible
	 */
	public Connexion getConnexion() {

		//Parcours de la liste pour voir si des Connexions sont libres        
		Connexion con = null;
		boolean trouve = false;
		logger.debug("getConnexion - Entree");
		synchronized(this){
			Iterator<Connexion> it = _listConnexion.iterator();
			while (it.hasNext() && !trouve) { 

				logger.debug("getConnexion - Recherche connexion existante");
				con = it.next();
				
				if( !con.getUtilise() ){
					logger.debug("getConnexion - connexion libre trouvee");
					if (isServiceOra) {
						// service Oracle, on ne teste pas la con
						con.setUtilise(true);	
						trouve = true;

					}
					else {
						if (con.testerConnexion()) {
							con.setUtilise(true);	
							trouve = true;
						}
						else {
							logger.info("getConnexion - connexion HS");
							con.fermer();
							_listConnexion.remove(it);
						}
					}
				}	                  
			}
		}
		if(trouve) return con;
		logger.debug("getConnexion - connexion Non trouvee");
		con = createConnexion();
		if (con != null) {
			synchronized (this) {
				if (isServiceOra) {
					con.setUtilise(true);
					_listConnexion.add(con);
					lancerThread();
				}
				else {
					if (con.testerConnexion()) {
						con.setUtilise(true);
						_listConnexion.add(con);
						lancerThread();
					}
					else {
						con.fermer();
						con = null;
					}
				}
			}
		}
		return con;
	}
	/**
	 * Enregistrement des requetes 
	 * @param svc : nom du service
	 * @param label : nom de l'item de navigation
	 * @param req : texte de la requete
	 */
	public  void setRequete (String svc, String label, String req) {
		String key = ConnexionManager.getCle(svc,label);
		// verification que la requete n'existe pas deja
		for (Entry<String, String> en : mapRequete.entrySet()) {
			if (req.equals(en.getValue())) {
				// on a trouve la meme requete
				// on stocke la reference
				logger.debug("setRequete - requete deja referencee pour ("+key+") -->("+en.getKey()+")");
				mapRequete.put(key, en.getKey());
				mapTypeRequete.put(key, ConnexionManager.REFERENCE);
				return;
			}
		}
		logger.debug("setRequete - Ajout de requete ("+req+") pour ("+key+")");
		mapRequete.put(key, req);
		mapTypeRequete.put(key,getTypeRequete(req));
	}

	/**
	 * determine si la requete  est un select ou pas
	 * @param req : requete lue du fichier properties
	 * @return Boolean :vrai si select
	 */
	private  String getTypeRequete(String req) {
		if (isReqSelect(req)) {
			return ConnexionManager.SELECT;
		}
		else if (ConnexionManager.isProcedure(req)) {
			return ConnexionManager.PROCEDURE;
		}		 
		else {
			return ConnexionManager.UPDATE;
		}		 
	}
	/**
	 *  determine si la requete lie a la cle (service/label) de navigation est un select ou pas
	 * @param cle
	 * @return
	 */
	public boolean isKeySelect (String cle) {		

		return mapTypeRequete.get(cle) == ConnexionManager.SELECT;
	}
	
	private boolean isReqSelect (String req) {		

		return req.toUpperCase().contains("SELECT");
	}
	/**
	 * determine si la requete lie au service/label de navigation est une procedure stockee ou pas
	 * @param svc : nom du service
	 * @param label  : nom de l'item de navigation
	 * @return vrai si la requete est une procedure, faux sinon
	 */
	public  boolean isKeyProcedure (String key) {		

		return mapTypeRequete.get(key) == ConnexionManager.PROCEDURE;
	}
	/**
	 * determine si la requete lie au service/label de navigation est une reference vers une autre cle
	 * 
	*/
	public  boolean isKeyReference(String key) {	
		return mapTypeRequete.get(key) == ConnexionManager.REFERENCE;
	}
	/**
	 * thread de scrutation de la liste des connexions
	 */
	public void run() {

		logger.debug("run - Entree");
		if (cnxMin > cnxMax) cnxMax = cnxMin;
		/* 
		 * verification du nombre de connexions ouvertes par rapport au minimum requis
		 */
		if (_listConnexion.size() < cnxMin) {
			/*
			 * On cree un pool de connexions
			 */
			Connexion con;
			for (int i= 0; i <cnxMin && _listConnexion.size() < cnxMin; i++) {
				logger.debug("run - Creation d'une connexion");
				con = createConnexion();
				if (con != null) {
					synchronized (this) {
						_listConnexion.add(con);
						con.setUtilise(false);
					}
				}
				else {
					/* Probleme de connexion on arrete */
					break;
				}
			}
			logger.debug("run - Nombre de connexion = "+_listConnexion.size());
		}
		/*
		 * Boucle infinie
		 */
		do {		 

			try {
				logger.debug("run - Attente de "+tpsAttente+" ms");
				Thread.sleep(tpsAttente);
			} catch (InterruptedException e1) {
				logger.info("run - fin du thread :"+e1.getMessage());
				break;
			}
			//Parcours de la liste de connexion
			Connexion con = null;		     
			logger.debug("run - Nbre de cnx ="+_listConnexion.size()+" Nbre de cnxMini="+cnxMin);
			int i = 0;
			synchronized (this) {
							
				while (i < _listConnexion.size()) {
					if (_listConnexion.size() <= cnxMin) break;
					con = (Connexion) _listConnexion.get(i);
					if( System.currentTimeMillis() - con.getDateDernierAcces() > tpsCnx) {
						logger.debug("run - suppression connexion :"+System.currentTimeMillis()+" - "+con.getDateDernierAcces()+" > "+tpsCnx);
						supprimerConnexion(i);		          
					}
					else {
						i++;
					}	    	  
				}
			}
			if( _listConnexion.size() == 0 ){
				/* plus de connexion a scruter on arrete le thread */
				logger.info("run - Arret du Thread ");
				_thread = null;
				break;	       
			}
			else {
				logger.info("run - Nombre de connexions actives = "+_listConnexion.size());
			}
		} while (true);	
	}
	/*
	 * Creation d'une connexion a la BDD
	 */
	private Connexion createConnexion() {
		logger.debug("createConnexion - Entree");
		try{
			Connection conn =null;
			
			if (_listConnexion.size() >= cnxMax) {
				/* on a atteint le max de cnx autorisee */
				logger.info("createConnexion - Max connexion atteint "+cnxMax);
				return null;
			}
			if (!_boolDriverInitialise) {
				initialiserDriver();
			}

			//Si le user n'est pas specifie, on tente une connection avec l'URL seulement
			if (user == null || "".equals(user)) {
				conn = DriverManager.getConnection(url);
			} 		
			else {
				//Sinon, connection en passant le user et le password
				logger.debug("Url="+url +
						" User="+user); 

				conn = DriverManager.getConnection(url,
						user, 
						passwd);
			}
			if( conn == null ) return null;
			//Par defaut, pas de transaction
			conn.setAutoCommit(true);
			Connexion con = new Connexion (conn, mapRequete, queryTimeout);

			return con;
		}catch(SQLException e){
			logger.error("createConnexion - Erreur de connexion BDD - "+e.getMessage());
			return null;
		}
	}

	/*
	 * Verifie si le thread est lance et le demarre si pas deja fait
	 */
	private  void lancerThread () {
		logger.debug("lancerThread - Entree");
		if(_thread == null) {
			_thread = new Thread(this);
			_thread.start();
		}
	}
	private  void stop() {
		if(_thread != null && _thread.isAlive()) {
			_thread.interrupt();			
		}
	}
	/*
	 * Suppression d'une connexion de la liste
	 */
	private void supprimerConnexion (int i) {
		Connexion con = (Connexion)_listConnexion.remove(i);
		logger.debug("supprimerConnexion - Entree " +i);	
		con.fermer();
		con = null;		
	}
	/*
	 * Initialisation du driver de connexion JDBC
	 */
	private void initialiserDriver() {

		logger.debug("initialiserDriver - Entree ("+driver+")");
		if (driver != null && driver.length() > 0) {				
			try {				
				Class.forName(driver);

				logger.debug("initialiserDriver - positionnement du LoginTimeout");
				DriverManager.setLoginTimeout(timeout );

				_boolDriverInitialise = true;
			} catch (ClassNotFoundException e) {
				logger.error("initialiserDriver - Erreur de driver - "+e.getMessage());				
			}
		}
		else {
			logger.error("initialiserDriver - Driver incorrect ("+driver+")");
		}

	}

	public  void supprimer() {
		
		synchronized (this) {	
			stop();
			for (Connexion conn: _listConnexion) {
				conn.fermer();
			}
			_listConnexion.clear();
		}
		return;
	}

	/**
	 * Temps d'attente du thread avant de se reveiller
	 * @param tps
	 */
	public void setTpsAttente(String tps) {
		try {
			tpsAttente = Long.parseLong(tps) * 1000;
		} catch (NumberFormatException e1) {
			logger.info("setTpsAttente - Probleme sur le parametre :"+tps+" valeur par defaut = 600000");	
			/* par defaut on s'endort 10mn */	 
			tpsAttente = 600000;
		}
	}
	/**
	 *  Temps au bout duquel la connexion est fermée si elle reste inactive
	 */	 
	public void setTpsCnx (String tps) {
		try {
			tpsCnx = Long.parseLong(tps)*1000;
		} catch (NumberFormatException e1) {
			logger.info("setTpsCnx - Probleme sur le parametre :"+tps+" valeur par defaut = 600000");			 
			tpsCnx = 600000;
		}
	}
	public void setCnxMin (String min) {
		try {
			cnxMin = Integer.parseInt(min);
		} catch (NumberFormatException e1) {
			logger.info("setCnxMin - Probleme sur le parametre :"+min+" valeur par defaut = 0");
			cnxMin = 0;
		}
	}
	/**
	 * temps de timeout de connexion a la base
	 * @param tps
	 */
	public void setTimeout (String tps) {

		try {		 			
			timeout = Integer.parseInt(tps);
		} catch (NumberFormatException e) {
			logger.info("setTimeout - Erreur parsing "+tps+" valeur par defaut = 4");
			timeout = 4;
		}
	}
	public void setCnxMax (String max) {
		try {
			cnxMax = Long.parseLong(max);
		} catch (NumberFormatException e) {
			/* Par defaut on peut ouvrir jusqu'a 10 cnx simultanees */
			logger.info("setCnxMin - Probleme sur le parametre :"+max+" valeur par defaut = 10");
			cnxMax = 10;
		}
	}
	/**
	 * time out sur une requete base
	 * @param tps
	 */
	public void setQueryTimeout(String tps) {
		try {
			queryTimeout = Integer.parseInt(tps);
		} catch (NumberFormatException e) {
			logger.info("setQueryTimeout - Probleme sur le parametre :"+tps+" valeur par defaut = 4");
			queryTimeout = 4;
		}
		
	}
	public void initialiser() {
		lancerThread();
	}
	/**
	 * Donne la cle referencee
	 * @param key
	 * @return
	 */
	public String getCleRef(String key) {	
		return mapRequete.get(key);
	}
}
