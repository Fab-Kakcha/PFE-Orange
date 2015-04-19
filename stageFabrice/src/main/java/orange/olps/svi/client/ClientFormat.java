package orange.olps.svi.client;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.util.Util;


public class ClientFormat {
	



	private static final DateFormat df2 =  new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	
	private Client client;
	private StringBuffer buf = null;
		
	protected static Log logger = LogFactory.getLog(ClientFormat.class.getName());
	

	public ClientFormat (Client c) {
		client = c;
		buf = new StringBuffer();
	}
	/**
	 * retourne le buffer 
	 */
	public String toString () {
		logger.debug(" toString - Sortie ("+buf+")" );
		if (buf != null)
			return buf.toString();
		else 
			return "";
	}
	
	/**
	 * Ecriture dans le buffer au format brut: tous les attributs sont listes
	 * sous la forme var1:valeur1var2:valeur2:...
	 * @param client =client appelant
	 */
	public  void formaterBrut() {

		logger.debug(" formaterBrut - Entree ("+client.getValeur(Client.VAR_APPELANT)+")" );
		buf.setLength(0);
	
		for (String var : client.getListeAttribut()) {	
			
			buf.append(var);
			buf.append(':');
			if (client.getValeur(var) == null || "".equals(client.getValeur(var))) {
				buf.append("null");
			}
			else
				buf.append(client.getValeur(var));	
			
		}
	
		logger.debug(" formaterBrut - Sortie ("+buf+")" );
	}
	/**
	 * Ecriture dans le buffer au format brut: tous les attributs obligatoires sont listes
	 * sous la forme var1:valeur1var2:valeur2:...
	 * @param client =client appelant
	 */
	public  void formaterBrutLeger() {

		logger.debug(" formaterBrut - Entree ("+client.getValeur(Client.VAR_APPELANT)+")" );
		buf.setLength(0);
		
		buf.append(Client.VAR_IDENT);
		buf.append(':');
		if (client.getValeur(Client.VAR_IDENT) == null || "".equals(client.getValeur(Client.VAR_IDENT))) {
			buf.append("null");
		}
		else
			buf.append(client.getValeur(Client.VAR_IDENT));	
		
		buf.append(Client.VAR_APPELANT);
		buf.append(':');
		if (client.getValeur(Client.VAR_APPELANT) == null || "".equals(client.getValeur(Client.VAR_APPELANT))) {
			buf.append("null");
		}
		else
			buf.append(client.getValeur(Client.VAR_APPELANT));	
			
		buf.append(Client.VAR_APPELE);
		buf.append(':');
		if (client.getValeur(Client.VAR_APPELE) == null || "".equals(client.getValeur(Client.VAR_APPELE))) {
			buf.append("null");
		}
		else
			buf.append(client.getValeur(Client.VAR_APPELE));	
		
		
		buf.append(Client.VAR_LANGUE);
		buf.append(':');
		if (client.getValeur(Client.VAR_LANGUE) == null || "".equals(client.getValeur(Client.VAR_LANGUE))) {
			buf.append("null");
		}
		else
			buf.append(client.getValeur(Client.VAR_LANGUE));	
	
		logger.debug(" formaterBrut - Sortie ("+buf+")" );
	}
	/**
	 * Ecriture dans le buffer au format csv: seuls les attributs donnes dans le tableau sont listes
	 * @param client = client appelant
	 * @param tabCle = liste des champs a tracer
	 */
	public  void formaterCsv(String[] tabCle) {
		
		logger.debug(" formaterCsv - Entree ("+client.getValeur(Client.VAR_APPELANT)+")" );
		buf.setLength(0);
		String val;
		for (String cle : tabCle) {
			
			if (buf.length() > 0) {
				buf.append(';');
			}
			
			val = client.getValeur(cle);
			if (Client.VAR_DATE.equals(cle)) {
				buf.append(df2.format(client.getTopDepart()));
			}
			else if(Client.VAR_DUREE.equals(cle)) {		
				if (val == null) {
					logger.debug(" formaterCsv - TOP depart ("+client.getTopDepart()+")" );
					val = Util.getDuree(client.getTopDepart());
					client.setValeur(cle, val);
				}
				buf.append(parseDuree(val));				
			}
			else {
				if (val != null) {			
					buf.append(val);
				}
			}
		}
		logger.debug(" formaterCsv - Sortie ("+buf+")" );
	}
	private String parseDuree(String valeur) {
		long duree;
		try {
			duree = Long.parseLong(valeur);
		} catch (Exception e) {
			logger.error(" parseDuree - Sortie : valeur non numerique ("+valeur+")" );
			return "00:00";
		}
		  StringBuffer d;
		  if (duree < 60) {
			  d = new StringBuffer("00:");
		  }
		  else {
			  if (duree < 600) {
				  d = new StringBuffer("0");
				  d.append(Long.toString(duree/60));
			  }
			  else {
				  d = new StringBuffer (Long.toString(duree/60));
			  }
		
			  d.append(':');
			  
		  }
		  long seconde = duree % 60;
		  if (seconde < 10) {
			  d.append("0");
		  }	  
		  d.append(seconde); 
		  return d.toString();
	}

	/**
	 * Ecriture dans le buffer au format json: tous les attributs  sont listes
	 * @param client = client appelant
	 * 
	 */
	public  void formaterJson() {
		JSONObject obj = new JSONObject();
		logger.debug(" formaterJson - Entree ("+client.getValeur(Client.VAR_APPELANT)+")" );
		buf.setLength(0);
		for (String cle : client.getListeAttribut()) {
			try {
				if (Client.VAR_DATE.equals(cle)) {
					obj.put(cle, df2.format(client.getTopDepart()));
				}
				else if(Client.VAR_DUREE.equals(cle)) {	
					if (client.getValeur(cle) == null) {
						client.setValeur(cle, Util.getDuree(client.getTopDepart()));
					}
					obj.put(cle, parseDuree(client.getValeur(cle)));
				}
				else {
					obj.put(cle, client.getValeur(cle));
				}
			} catch (JSONException e) {
				logger.error(" formaterJson cle ("+cle+") erreur "+e.getMessage());
			}
		}
		buf.append(obj.toString());
		logger.debug(" formaterJson - Buf ("+buf+")" );
	}


}
