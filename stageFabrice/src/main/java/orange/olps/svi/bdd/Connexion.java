package orange.olps.svi.bdd;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import orange.olps.svi.navigation.BaseDonnees;
import orange.olps.svi.navigation.NavigationManager;

public class Connexion {
	private Connection _con = null;
	/* Les requetes sont toujours les memes.
	 * pour optimiser on les prepare dans un PreparedStatement
	 */
	private Map <String, PreparedStatement> mapStmt;
	private  boolean _boolUtilise = false;
	private long _dateDernierAcces = 0;
	private int timeout = 4;

	protected static Log logger = LogFactory.getLog(Connexion.class.getName());

	public Connexion(Connection connection, Map<String, String> mapRequete, int timeout) throws SQLException {
		_con = connection;

		initialiserPrepareStatement(mapRequete);
	}
	/**
	 * Le destructeur permet de fermer la connexion 
	 * a la base
	 */
	public void finalize () {    	
		fermer();    	
	}
	public long getDateDernierAcces() {
		return _dateDernierAcces;
	}
	public boolean getUtilise() {
		return _boolUtilise;
	}
	public void setUtilise(boolean b) {
		_boolUtilise = b;
	}
	/**
	 * test de la connexion
	 */
	public boolean testerConnexion() {
		logger.debug("testerConnexion - Entree");
		boolean ret = false;
		if(_con==null){
			return false;
		}
		ResultSet ping = null;
		try{
			if(_con.isClosed()){return false;}
			ping = _con.createStatement().executeQuery("SELECT 1 from dual");
			ret = ping.next();			
		}catch(SQLException sqle){
			ret = false;
		}
		finally{
			if(ping!=null){try{ping.close();}catch(Exception e){}}
		}  
		return ret;
	}
	/* initialisation des prepare statement */
	private void initialiserPrepareStatement(Map<String, String> mapRequete) throws SQLException {
		logger.debug("initialiserPrepareStatement - Entree");					
		if (_con == null) {
			return;
		}

		mapStmt = new HashMap<String, PreparedStatement>(1);
		/* parcours de la map des requetes pour construire les statements */
		String req;
		String key;
		PreparedStatement st;
		for (Map.Entry<String, String> entry : mapRequete.entrySet()) {
			key = entry.getKey();
			req = entry.getValue();
			if (req != null && !"".equals(req) && mapStmt.get(key) == null) {

				if (ConnexionManager.isKeyReference(key)) {
					logger.debug("initialiserPrepareStatement - Reference ("+key+") vers ("+req+")");
					String keyRef = req; //la requete a ete remplacee par la cle dont la requete est identique
					st = mapStmt.get(keyRef);
					if (st == null) {
						// cette cle n'a pas ete traitee
						st = initialiserPrepareStatement(mapRequete.get(keyRef), keyRef);
						if (st != null) {
							mapStmt.put(keyRef, st);
							mapStmt.put(key, st);
						}
					}
					else {
						mapStmt.put(key, st);
					}
				}
				else {
					st = initialiserPrepareStatement(req, key);
					if (st != null) {
						mapStmt.put(key, st);
					}
				}

			}

		}

		_dateDernierAcces = System.currentTimeMillis();
		return;		
	}
	private PreparedStatement initialiserPrepareStatement(String req, String key) throws SQLException {
		PreparedStatement st = null;
		try {  
			if (ConnexionManager.isKeyProcedure(key)) {
				logger.debug("initialiserPrepareStatement - procedure ("+req+")");    				
				st = _con.prepareCall(req);		
				/*
				 * Il faut enregistrer tous les parametres out
				 * de la procedure
				 */
				for (int indexOut : getArrayIndexOut (key)) {
					// c'est une donnee OUT
					((CallableStatement )st).registerOutParameter(indexOut, java.sql.Types.VARCHAR);						
				}
			}
			else {
				logger.debug("initialiserPrepareStatement - req ("+req+")");
				st = _con.prepareStatement(req);
			}
			st.setQueryTimeout(timeout);

		} catch (SQLException e) {
			logger.error("initialiserPrepareStatement - erreur lors du parsing de "+req+" : "+e.getMessage());
			throw e;
		} catch (NumberFormatException e) {
			logger.info("initialiserPrepareStatement - probleme de format de donnee numerique lors du parsing de "+req);					
		}
		
		return st;		
	}

	/*
	 * Remet la connexion dans le pool
	 */
	public void liberer() {
		logger.debug("liberer - Entree");
		if (_con != null) {
			try {
				if (!_con.getAutoCommit()) 	_con.commit();
			} catch (SQLException e) {
				logger.error("liberer - erreur lors du commit : "+e.getMessage());
			}
		}    	
		_boolUtilise = false;
	}
	/**
	 * Methode de positionnement des parametres de requete
	 * key = cle de la requete dans le tableau des PreparedStatment
	 * pos = position du parametre
	 * valeur = valeur du parametre
	 */
	public void valoriserParametre (String key, int pos, String valeur) {    
		PreparedStatement st = mapStmt.get(key);
		if (st == null) {
			logger.error("valoriserParametre - Statement null");
			return;
		}
		try {
			st.setString(pos, valeur);
		} catch (SQLException e) {
			logger.error("valoriserParametre - erreur lors du remplacement de parametre de type String: "+e.getMessage());
		}
	}
	/**
	 * Methode de positionnement des parametres de procedure stokee
	 * key = cle de la requete dans le tableau des PreparedStatment
	 * var = nom du parametre
	 * valeur = valeur du parametre
	 */
	public void valoriserParametre (String key, String var, String valeur) {    
		CallableStatement st = (CallableStatement)mapStmt.get(key);
		if (st == null) {
			logger.error("valoriserParametre - Statement null");
			return;
		}
		try {
			st.setString(var, valeur);
		} catch (SQLException e) {
			logger.error("valoriserParametre - erreur lors du remplacement de parametre de type String: "+e.getMessage());
		}
	}
	/**
	 * Methode de positionnement des parametres de requete
	 *  key = cle de la requete dans le tableau des PreparedStatment
	 * pos = position du parametre
	 * valeur = valeur du parametre
	 */
	public void valoriserParametre (String key, int pos, int valeur) {
		logger.debug("valoriserParametre - Entree");
		PreparedStatement st = mapStmt.get(key);
		if (st == null) return;
		try {
			st.setInt(pos, valeur);
		} catch (SQLException e) {
			logger.error("valoriserParametre - erreur lors du remplacement de parametre de type int: "+e.getMessage());
		}
	}
	/**
	 * Retournle parametre OUT en fonction de sa position
	 * @param key = cle du statement
	 * @param pos = position du parametre OUT dans la procedure
	 * @return la valeur retournee par la procedure
	 */
	public String getValeur(String key, int pos) {
		logger.debug("getValeur - Entree");
		CallableStatement st = (CallableStatement) mapStmt.get(key);
		if (st == null) return "";
		try {
			return st.getString(pos);
		} catch (SQLException e) {
			logger.error("valoriserParametre - erreur lors du remplacement de parametre de type int: "+e.getMessage());
		}
		return "";
	}
	/**
	 * Retournle parametre OUT en fonction de sa position
	 * @param key = cle du statement
	 * @param var = nom du parametre OUT dans la procedure
	 * @return la valeur retournee par la procedure
	 */
	public String getValeur(String key, String var) {
		logger.debug("getValeur - Entree");
		CallableStatement st = (CallableStatement) mapStmt.get(key);
		if (st == null) return "";
		try {
			return st.getString(var);
		} catch (SQLException e) {
			logger.error("valoriserParametre - erreur lors du remplacement de parametre de type int: "+e.getMessage());
		}
		return "";
	}
	/**
	 * Retourne le resultset pour une procedure stockee
	 * @param key  = cle du statement
	 * @return ResultSet resultat de la requete
	 * @throws SQLException 
	 */
	public ResultSet getResultSet(String key) throws SQLException {
		logger.debug("getResultSet - Entree");
		CallableStatement st = (CallableStatement) mapStmt.get(key);
		return st.getResultSet();
	}
	/**
	 * Requete d'interrogation de la base
	 * En cas de pb remonte une exception SQLException
	 */
	public ResultSet executerSelect (String key) throws SQLException {
		logger.debug("executerSelect - Entree "+key);
		_dateDernierAcces = System.currentTimeMillis();
		PreparedStatement st = mapStmt.get(key);
		if (st == null) {
			logger.error("executerSelect - Statement null "+key);
			return null;
		}
		try {				
			return mapStmt.get(key).executeQuery();
		} catch (SQLException e) {
			logger.error("executerSelect - erreur lors de l'execution du select: "+e.getMessage()+" "+st.toString());
			throw e;
		} 	
	}
	/**
	 * Requete de lancement de procedure stockee
	 * En cas de pb remonte une exception SQLException
	 */
	public boolean executerProcedure (String key) throws SQLException {
		logger.debug("executerProcedure - Entree "+key);
		_dateDernierAcces = System.currentTimeMillis();
		CallableStatement st = (CallableStatement) mapStmt.get(key);
		if (st == null) {
			logger.error("executerProcedure - Statement null "+key);
			return false;
		}
		// execution de la requete
		try {				
			return st.execute();
		} catch (SQLException e) {
			logger.error("executerProcedure - erreur lors de l'execution : "+e.getMessage()+" "+st.toString());
			throw e;
		} 	
	}
	/**
	 * Requete de mise a jour (update/insert)?
	 */
	public int executerMiseAJour (String key){
		logger.debug("executerMiseAJour - Entree - req ="+key);
		_dateDernierAcces = System.currentTimeMillis();
		PreparedStatement st = mapStmt.get(key);
		try {

			if (st == null) return -1;
			int nb = st.executeUpdate();
			logger.debug("executerMiseAJour - nb="+nb);
			return nb;

		} catch (SQLException e) {
			logger.error("executerMiseAJour - erreur lors de l'execution de la mise a jour ("+key+"): "+e.getMessage());
		}
		return -1;  	
	}
	/*
	 * fermeture de la connexion
	 */
	public void fermer() {
		logger.debug("fermer - Entree");
		_boolUtilise = true;
		try {

			PreparedStatement st;
			for (Map.Entry<String, PreparedStatement> entry : mapStmt.entrySet()) {
				st = entry.getValue();    		 
				if (st != null) {
					st.close();
					st = null;
				}             	
			}
		} catch (SQLException e) {
			//tracage de l'erreur
			logger.error("fermer - erreur lors de la fermeture des statements: "+e.getMessage());
		}
		try {
			if (_con != null) {
				if (!_con.getAutoCommit())
									_con.commit();				
				_con.close();
			}
		} catch (SQLException e) {
			//tracage de l'erreur
			logger.error("fermer - erreur lors de la fermeture de la connexion: "+e.getMessage());
		}
		_con = null;       
		_dateDernierAcces = 0;
		_boolUtilise = false;
	}
	public static boolean isServiceOra (String req) {		

		return req.matches("\\{[ ]*call.*");
	}
	/**
	 * Retourne la liste des indices des variables OUT de la procedure stockee
	 * @param key = cle (service+"_"+label)
	 * @return liste des indices  des variables OUT
	 */
	private ArrayList<Integer> getArrayIndexOut(String key) {
		String[] tab = key.split("_");
		BaseDonnees bd = (BaseDonnees)NavigationManager.getInstance().getNavigation(tab[0], tab[1]);
		if (bd == null) return null;
		else return bd.getArrayIndexOut();
	}

}
