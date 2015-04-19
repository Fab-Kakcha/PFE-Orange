package orange.olps.svi.bdd;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import orange.olps.svi.client.Client;
import orange.olps.svi.navigation.BaseDonnees;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConnexionUtil {
	protected static Log logger = LogFactory.getLog(ConnexionUtil.class.getName());
	
	public ConnexionUtil() {}
	
	public void executerRequete (Client client, BaseDonnees bd) {
		logger.debug("executerRequete - ("+bd.getLabel()+")  - Entree - ("+ client.getIdent()+")");
		
		// initialisation à base OK par defaut
		client.setValeur(Client.VAR_ERREUR, BaseDonnees.BDD_OK,false);
		Connexion con = ConnexionManager.getConnexion();	
		if (con == null) {
			 client.setValeur(Client.VAR_ERREUR, BaseDonnees.ERR_BASE_KO);
			 mettreAJourClientValeurDefaut(client, bd);
			 return;
		}
		/* recuperation de la cle, associee a la requete de cet item de navigation */
		String key = ConnexionManager.getCle(client.getService(), bd.getLabel());
		/* Ajustement au cas ou la cle soit une reference vers une autre requete */

		key = ConnexionManager.getCleRef(key);
		
		/* Execution de la requete */	    
		if (ConnexionManager.isKeySelect(key)) {
			/* remplacement des ? par les valeurs du client dans la requete */
			positionnerParametreRequete(key, con, client, bd);
			try {
				ResultSet resultat;
								
				resultat = con.executerSelect(key);

				if (resultat != null) {			
			        /* extraction des donnees */
			       mettreAJourClient(client, bd, resultat);			       
			       logger.debug("executerRequete - valeurs lues en BDD pour "+ client.getIdent());
			      
				}
				else {
					logger.debug("executerRequete ("+client.getIdent()+")- pas de valeurs en base pour "+ client.getIdent());
					client.setValeur(Client.VAR_ERREUR, BaseDonnees.ERR_NON_TROUVE);
					mettreAJourClientValeurDefaut(client, bd);
				}
			} catch (SQLException e) {
				/* l'erreur a deja ete tracee, on retourne une erreur de base */
				logger.error("executerRequete ("+client.getIdent()+")- Exception "+e.getMessage());
				client.setValeur(Client.VAR_ERREUR, BaseDonnees.ERR_BASE_KO);
				mettreAJourClientValeurDefaut(client, bd);
			}
		}
		else if (ConnexionManager.isKeyProcedure(key)) {
			// Procedure stockee
			/* remplacement des variables IN par les valeurs du client  */
			positionnerParametreProcedure(key, con, client, bd);
			try {
				
				Boolean boolRetour = con.executerProcedure(key);
				if (boolRetour) {
					// c'est un ResultSet
					ResultSet resultat;
					try {
						resultat = con.getResultSet(key);
						mettreAJourClient(client, bd, resultat);			       
					    logger.debug("executerRequete - valeurs lues en BDD pour "+ client.getIdent());					
						return;
					} catch (SQLException e) {
						logger.error("executerRequete ("+client.getIdent()+") - Exception getResultSet "+e.getMessage());
						client.setValeur(Client.VAR_ERREUR, BaseDonnees.ERR_BASE_KO);
						mettreAJourClientValeurDefaut(client, bd);
					}

					
				}	
				else {
					// le retour se fait par les variables OUT
					 logger.debug("executerRequete - retour de proc pour "+ client.getIdent());
					String tmp = "";
					String val = "";
					String err = "";
					int pos = 0;
					try {
						for (String var : bd.getTabVariable()) {
							 logger.debug("executerRequete - ("+client.getIdent()+") var ("+var+") index ("+pos+")");
							if (var.equals(Client.VAR_ERREUR)) {
								err = con.getValeur(key, bd.getArrayIndexOut().get(pos));
							}
							else if (var.equals(Client.VAR_TMP)) {
								tmp = con.getValeur(key,  bd.getArrayIndexOut().get(pos));								
							}
							else {
								val = con.getValeur(key,  bd.getArrayIndexOut().get(pos));
								if (val != null && !"".equals(val)) {
									client.setValeur(var, val,bd.isStat());
								}
								else {
									client.setValeur(var, bd.getValeurDefaut(var), bd.isStat());
								}
							}
							pos++;
						}
						client.setValeur(Client.VAR_ERREUR, err+" - "+tmp);
					}
					catch (IndexOutOfBoundsException e) {
						logger.error("executerRequete - Erreur  ("+client.getIdent()+") index ("+pos+") "+e.getMessage());
						client.setValeur(Client.VAR_ERREUR, BaseDonnees.ERR_BASE_KO);
						mettreAJourClientValeurDefaut(client, bd);
					}
				}
				
			} catch (SQLException e) {
				/* l'erreur a deja ete tracee, on retourne une erreur de base */
				logger.error("executerRequete - ("+client.getIdent()+") Exception "+e.getMessage());
				client.setValeur(Client.VAR_ERREUR, BaseDonnees.ERR_BASE_KO);
				mettreAJourClientValeurDefaut(client, bd);
			}
		}
		else {
			// ce n'est pas un select ni une procedure
			/* remplacement des ? par les valeurs du client dans la requete */
			positionnerParametreRequete(key, con, client, bd);
			int retour = con.executerMiseAJour(key);
			if (retour == -1) client.setValeur(Client.VAR_ERREUR, BaseDonnees.ERR_BASE_KO);
			
			if (retour == 0) {
				
					client.setValeur(Client.VAR_ERREUR, BaseDonnees.ERR_NON_TROUVE);
			}
			
		}
		con.liberer();	
		return;
	}

	/**
	 * Renseigne les champs de la clause where avant execution de la requete
	 * @param s : donnees du client en ligne
	 * @param bd : objet de navigation de type base de donnees
	 * @param con : connexion vers la base
	 */
	private void positionnerParametreRequete(String key, Connexion con, Client s, BaseDonnees bd) {
		/* valorisation des champs de la requete par les donnes clients */
		logger.debug("positionnerParametreRequete - Entree "+key);

		int pos = 1;
		for (String var : bd.getTabSelecteur()) {
			logger.debug("positionnerParametreRequete - cle("+key+") position ("+pos+") donnees ("+var+") valeur ("+s.getValeur(var)+")");
			con.valoriserParametre(key, pos, s.getValeur(var));
			pos++;
		}
		return ;
	}
	/**
	 * Renseigne les champs IN de la procedure
	 * @param s : donnees du client en ligne
	 * @param bd : objet de navigation de type base de donnees
	 * @param con : connexion vers la base
	 */
	private void positionnerParametreProcedure(String key, Connexion con, Client s, BaseDonnees bd) {
		/* valorisation des champs de la requete par les donnes clients */
		logger.debug("positionnerParametreProcedure - Entree "+key);
		int pos = 0;
		for (String var : bd.getTabSelecteur()) {
			
			logger.debug("positionnerParametreProcedure - cle("+key+")  donnees ("+var+") valeur ("+s.getValeur(var)+")");
			
			con.valoriserParametre(key, // cle du statement
					bd.getArrayIndexIn().get(pos), // position du ?
					s.getValeur(var)); // valeur 
			pos++;
		}
		return ;
	}
	/**
	 * Positionne les donnes du client par les valeurs par defaut
	 * @param client : donnees du client en ligne
	 * @param bd : objet de navigation de type base de donnees
	 */
	private void mettreAJourClientValeurDefaut(Client client, BaseDonnees bd) {
		/* valorisation des champs de la requete par les donnes par defaut */
		ArrayList<String> tabValDef = bd.getTabValeurDef();
		if (tabValDef != null) {
			int pos = 0;
			String val;
			for (String var : bd.getTabVariable()) {
				if (tabValDef.size() > pos) {
					val = tabValDef.get(pos);
					if (val.startsWith("_var")) {
						// c'est une variable
						client.setValeur(var, client.getValeur(val), bd.isStat());
					}
					else {
						client.setValeur(var, val, bd.isStat());
					}
					pos++;
				}
				else {
					logger.error("mettreAJourClient ("+client.getIdent()+")- Pas assez de valeurs par defaut");
					break;
				}
			}
		}
		return ;
	}
	/**
	 * Extrait du resultat de la requete, les donnes pour mettre a jour les donnees client
	 * @param client : donnees du client en ligne
	 * @param bd : objet de navigation de type base de donnees
	 * @param resultat : resultat de la requete
	 * @throws SQLException
	 */
	private void mettreAJourClient(Client client, BaseDonnees bd, ResultSet resultat) throws SQLException {
		int pos = 1;
		ArrayList<String> tabValDef = bd.getTabValeurDef();
		
		if (resultat.next()) {
			try {
				for (String var : bd.getTabVariable()) {					
					client.setValeur(var, resultat.getString(pos),bd.isStat());			
					pos++;
				}
			} catch (Exception e) {
				logger.error("mettreAJourClient ("+client.getIdent()+")- Pas assez de colonnes dans le SELECT");
				client.setValeur(Client.VAR_ERREUR, BaseDonnees.ERR_NON_TROUVE);
				ArrayList<String> tabVar = bd.getTabVariable();
				for (; pos < tabVar.size(); pos++) {
					if (tabValDef.size() > pos) {
						client.setValeur(tabVar.get(pos), tabValDef.get(pos),bd.isStat());								
					}
					else {
						logger.error("mettreAJourClient ("+client.getIdent()+")- Pas assez de valeurs par defaut");
						break;
					}
				}
			}
		}
		else {		
			logger.info("mettreAJourClient - Client non trouve en base "+client.getValeur(Client.VAR_IDENT));
			client.setValeur(Client.VAR_ERREUR, BaseDonnees.ERR_NON_TROUVE);
			mettreAJourClientValeurDefaut(client,  bd);						
		}
		return ;
		
	}

}
