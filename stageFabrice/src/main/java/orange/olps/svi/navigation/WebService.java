package orange.olps.svi.navigation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;








import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;
import orange.olps.svi.config.Config;
import orange.olps.svi.stats.StatManager;
import orange.olps.svi.util.Util;
import orange.olps.svi.web.WebManager;

public class WebService extends Navigation {

	static final public int GET = 1;
	static final public int POST = 2;
	private static final String OK = "OK";
	private static final String SUCCES = "SUCCES";
	private static final String ACCEPTED = "Accepted";


	private static enum TypeRslt {
		RSLT_XML,
		RSLT_JSON, 
		RSLT_TXT
	}


	private ArrayList<String> url = null;
	/**
	 * liste des variables lues en properties donnant les variables a renseigner
	 */
	private ArrayList<String> tabVariable;
	/**
	 * valeurs par defaut
	 */
	private ArrayList<String> tabValeurDef;
	/**
	 * Elements du xml recu 
	 */
	private ArrayList<String> tabElement;

	/**
	 * type de méthode de la requete à effectuer (GET/POST)
	 */
	private int methode = GET;
	/**
	 * Parametre de la requete
	 */
	private ArrayList<String> param = null;
	/**
	 * contenu d'un fichier comme parametre post
	 */
	private ArrayList<String> paramFic = null;
	private String nomFic = null;
	private boolean boolStat;
	private RequestConfig config = null;
	private TypeRslt typeResult = TypeRslt.RSLT_XML;
	private String charset = "utf-8";
	private String contentType = "text/plain";
	/**
	 * constructeur
	 * @param lab = label de navigation
	 * @param svc = service
	 */
	public WebService(String lab, String svc) {
		super(lab, svc);
		tabVariable = new ArrayList<String>(1);
		tabValeurDef = new ArrayList<String>(1);
		tabElement = new ArrayList<String>(1);
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

		// recuperation de l'URL
		String s = lstProp.getProperty(racinePropriete+URL, "");
		if ("".equals(s)) {
			/* on va lire dans le fichier de properties general */
			s = Config.getInstance().getProperty(racinePropriete+URL, "");
			if ("".equals(s)) {
				// on lit au fichier de properties general la requete commune a tous les services
				s = Config.getInstance().getProperty(Config.WEBSVC_URL, "");
			}
		}
		if (!"".equals(s)) {
			url = Util.decouperString(s);
		}

		// lecture de la méthode (get/post)
		s = lstProp.getProperty(racinePropriete+METHODE, "");
		if ("".equals(s)) {
			/* on va lire dans le fichier de properties general */
			s = Config.getInstance().getProperty(racinePropriete+METHODE, "");
			if ("".equals(s)) {
				// on lit au fichier de properties general la requete commune a tous les services
				s = Config.getInstance().getProperty(Config.WEBSVC_METHODE, "");
			}
		}
		if ("POST".equals(s)) {
			methode=POST;
		}
		else {
			// par defaut
			methode=GET;
		}

		// lecture des parametres eventuels
		s = lstProp.getProperty(racinePropriete+PARAMETRE, "");
		if ("".equals(s)) {
			/* on va lire dans le fichier de properties general */
			s = Config.getInstance().getProperty(racinePropriete+PARAMETRE, "");
			if ("".equals(s)) {
				// on lit au fichier de properties general la requete commune a tous les services
				s = Config.getInstance().getProperty(Config.WEBSVC_PARAM, "");
			}
		}
		if (!"".equals(s)) {
			param= Util.decouperString(s);
		}
		if (methode == POST) {
			// recherche de parametre dans un fichier
			s = lstProp.getProperty(racinePropriete+PARAM_FICHIER, "");
			if ("".equals(s)) {
				/* on va lire dans le fichier de properties general */
				s = Config.getInstance().getProperty(racinePropriete+PARAM_FICHIER, "");
				if ("".equals(s)) {
					// on lit au fichier de properties general la requete commune a tous les services
					s = Config.getInstance().getProperty(Config.WEBSVC_PARAM_FICHIER, "");
				}
			}
			if (!"".equals(s)) {
				// lecture du fichier et découpage
				nomFic  = s;
				paramFic =  Util.decouperString(Util.lireFichier(s));				       
			}
		}

		// lecture des variables a renseigner en sortie
		s = lstProp.getProperty(racinePropriete+VARIABLE, "");
		if ("".equals(s)) {
			/* on va lire dans le fichier de properties general */
			s = Config.getInstance().getProperty(racinePropriete+VARIABLE, "");
			if ("".equals(s)) {
				// on lit au fichier de properties general la requete commune a tous les services
				s = Config.getInstance().getProperty(Config.WEBSVC_VARIABLE, "");
			}
		}
		if (!"".equals(s)) {
			/* les parametres sont stockes sous la forme d'une liste dont le separateur est une virgule */
			String[] tabParam = s.split(",");
			for (String var : tabParam) {
				tabVariable.add(var);
			}
		}
		// lecture des valeurs par defaut
		s = lstProp.getProperty(racinePropriete+VALEUR_DEF, "");
		if ("".equals(s)) {
			/* on va lire dans le fichier de properties general */
			s = Config.getInstance().getProperty(racinePropriete+VALEUR_DEF, "");
			if ("".equals(s)) {
				// on lit au fichier de properties general la requete commune a tous les services
				s = Config.getInstance().getProperty(Config.WEBSVC_DEF, "");
			}
		}
		if (!"".equals(s)) {
			/* les parametres sont stockes sous la forme d'une liste dont le separateur est une virgule */

			String[] tabParam = s.split(",");
			for (String var : tabParam) {
				tabValeurDef.add(var);
			}
		}
		// lecture des elements (noeud) du xml recu en reponse
		s = lstProp.getProperty(racinePropriete+FORMAT, "").trim();
		if ("".equals(s)) {
			/* on va lire dans le fichier de properties general */
			s = Config.getInstance().getProperty(racinePropriete+FORMAT, "").trim();
			if ("".equals(s)) {
				// on lit au fichier de properties general la requete commune a tous les services
				s = Config.getInstance().getProperty(Config.WEBSVC_FORMAT, "").trim();
			}
		}
		if (!"".equals(s)) {
			if ("XML".equals(s)) typeResult = TypeRslt.RSLT_XML;
			else if ("JSON".equals(s)) typeResult = TypeRslt.RSLT_JSON;
			else if ("TXT".equals(s)) typeResult = TypeRslt.RSLT_TXT;

		}
		// lecture des elements (noeud) du xml recu en reponse
		s = lstProp.getProperty(racinePropriete+ELEMENT, "");
		if ("".equals(s)) {
			/* on va lire dans le fichier de properties general */
			s = Config.getInstance().getProperty(racinePropriete+ELEMENT, "");
			if ("".equals(s)) {
				// on lit au fichier de properties general la requete commune a tous les services
				s = Config.getInstance().getProperty(Config.WEBSVC_ELEMENT, "");
			}
		}
		if (!"".equals(s)) {
			/* les parametres sont stockes sous la forme d'une liste dont le separateur est une virgule */

			String[] tabParam = s.split(",");
			for (String var : tabParam) {
				tabElement.add(var);
			}
		}
		// PROXY
		s = lstProp.getProperty(racinePropriete+PROXY, "").trim();
		if ("".equals(s)) {
			/* on va lire dans le fichier de properties general */
			s = Config.getInstance().getProperty(racinePropriete+PROXY, "").trim();
			if ("".equals(s)) {
				// on lit au fichier de properties general la requete commune a tous les services
				s = Config.getInstance().getProperty(Config.PROXY, "").trim();
			}
		}
		if (!"".equals(s)) {
			/* il faut configurer le proxy 
			 * la donnée lue est sous la forme IP:Port*/

			String[] tab = s.split(":");
			int port;
			try {
				port = Integer.parseInt(tab[1]);
			} catch (NumberFormatException e) {
				logger.error("initialiserItem - ("+label+") valeur du proxy port non numérique ("+s+") 8080 par defaut");
				port = 8080;
			}
			catch (NullPointerException e) {
				logger.error("initialiserItem - ("+label+") valeur du proxy port non spécifiée ("+s+") 8080 par defaut");
				port = 8080;
			}
			config = RequestConfig.custom().setProxy(new HttpHost(tab[0], port, "http")).build();
		}
		// charset pour encodage url
		 s = lstProp.getProperty(racinePropriete+CHARSET, "");
		if ("".equals(s)) {
			/* on va lire dans le fichier de properties general */
			s = Config.getInstance().getProperty(racinePropriete+CHARSET, "");
			if ("".equals(s)) {
				// on lit au fichier de properties general la requete commune a tous les services
				s = Config.getInstance().getProperty(Config.WEBSVC_CHARSET, "");
			}
		}
		if (!"".equals(s)) {
			charset = s;
		}
		// contentType 
		 s = lstProp.getProperty(racinePropriete+CONTENT_TYPE, "");
		if ("".equals(s)) {
			/* on va lire dans le fichier de properties general */
			s = Config.getInstance().getProperty(racinePropriete+CONTENT_TYPE, "");
			if ("".equals(s)) {
				// on lit au fichier de properties general la requete commune a tous les services
				s = Config.getInstance().getProperty(Config.WEBSVC_CONTENT_TYPE, "");
			}
		}
		if (!"".equals(s)) {
			contentType = s;
		}
		// --------------------------------------------
		this.setAction(Navigation.ERREUR,lstProp.getProperty(racinePropriete+Navigation.ERREUR_SUIVANT, Navigation.REJET).trim());	
		boolStat = Boolean.parseBoolean(lstProp.getProperty(racinePropriete+STATISTIQUES,"true").trim());
		logger.debug("initialiserItem - Fin pour ("+label+")");

		return ret;
	}


	@Override
	public void calculerActionNavigation(Client client) {
		// premier passage dans cet item
		// Statistiques
		logger.debug("calculerActionNavigation - ("+label+") - ("+client.getIdent()+") - Entree");
		StatManager.getInstance().posterStatistiques(client.getIdent(), 
				label, 
				System.currentTimeMillis(),
				StatManager.NAVIGATION);
		String reponse = null;

		if(methode == GET) {			
			reponse = passerRequeteGet(client);			
		}
		else {
			reponse = passerRequetePost(client);
		}

		if (reponse != null){
			// l'appel du service a fonctionne
			logger.debug("calculerActionNavigation - Requete OK ("+client.getIdent()+")");
			extraireXmlOuJson(client, reponse);
			client.setNavCourante(getSuivant());			
		}
		else {
			// l'appel du service n'a pas marche
			// on prend les valeurs par defaut
			logger.debug("calculerActionNavigation - Requete KO ("+client.getIdent()+")");
			mettreAJourClientValeurDefaut(client);
			client.setNavCourante(getAction(Navigation.ERREUR));			
		}	
		client.setActionNavigation(Navigation.RIEN);
	}

	/**
	 * Positionne les donnees du client par les valeurs par defaut
	 * @param s : donnees du client en ligne
	 * 	 
	 */
	private void mettreAJourClientValeurDefaut(Client s) {

		if (tabValeurDef != null) {
			int pos = 0;
			for (String var : tabVariable) {
				if (tabValeurDef.size() > pos) {
					s.setValeur(var, tabValeurDef.get(pos), boolStat);			
					pos++;
				}
				else break;
			}
		}
		return ;
	}
	public ArrayList<String> getTabVariable() {
		return tabVariable;
	}
	public String getElement(int pos) {
		if (pos < tabElement.size()) return tabElement.get(pos);
		return "";
	}
	public String getValDef(int pos) {
		if (pos < tabValeurDef.size()) return tabValeurDef.get(pos);
		return "";
	}

	/**
	 * Requete GET
	 * @param url
	 * @return
	 */
	private String passerRequeteGet (Client client) {

		String ret = null;
		CloseableHttpClient httpClient = WebManager.getInstance().getHttpClient();

		String sUrl = Util.reconstituerString(url, client);

		if (param != null) {
			if (sUrl.contains("?")) {
				
				sUrl+="&"+Util.reconstituerString(param, client);			
			}
			else {			
				sUrl+="?"+Util.reconstituerString(param, client);
			}
			
		}
		HttpGet get;
		CloseableHttpResponse reponse = null;
		try {
			sUrl = Util.encoderUri(sUrl, charset );

			get = new HttpGet(sUrl);
			
			logger.debug("passerRequeteGet - requete ("+sUrl +") ");
			if (config != null) {
				get.setConfig(config);
			}
			// executer requete HTTP
			get.setHeader(HttpHeaders.CONTENT_TYPE, contentType+";charset="+charset);
			HttpContext context = HttpClientContext.create();
			
			reponse = httpClient.execute(get, context);

			if (!OK.equals(reponse.getStatusLine().getReasonPhrase()) 
					&& ! SUCCES.equals(reponse.getStatusLine().getReasonPhrase())
					&& !ACCEPTED.equals(reponse.getStatusLine().getReasonPhrase())) {
				logger.error("passerRequeteGet - status pour "+sUrl +" "+reponse.getStatusLine().getReasonPhrase());						
			}
			else {
				logger.debug("passerRequeteGet - status pour "+sUrl +" "+reponse.getStatusLine().getReasonPhrase());
				HttpEntity entity = reponse.getEntity();
				if (entity != null) {
					ContentType contentType = ContentType.getOrDefault(entity);
					Charset charsetResp = contentType.getCharset();
					if (charsetResp == null) {
						charsetResp=Charset.forName(charset);
					}
					BufferedReader rd = new BufferedReader(new InputStreamReader(entity.getContent(), charsetResp));
					StringBuffer buf = new StringBuffer(50);
					String s;
					while ((s=rd.readLine()) != null) {
						buf.append(s);			
					}
					ret=buf.toString();
					logger.debug("passerRequeteGet - retour pour "+sUrl +" "+buf);
				}
			}
		} catch (ClientProtocolException e) {
			logger.error("passerRequeteGet - erreur pour "+sUrl+" "+e.getMessage());			

		} catch (UnsupportedEncodingException e) {
			logger.error("passerRequeteGet - erreur pour "+sUrl+" "+e.getMessage());

		} catch (IOException e) {
			logger.error("passerRequeteGet - erreur pour "+sUrl+" "+e.getMessage());			
		}
		finally {
			try {
				if (reponse != null) {
					reponse.close();
				}
			} catch (IOException e) {
				logger.warn("passerRequeteGet - pb fermeture reponse pour "+sUrl+" "+e.getMessage());
			}
		}

		return ret;
	}

	/**
	 * Requete Post
	 * @param url
	 * @return
	 */
	private String passerRequetePost (Client client) {

		String ret = null;
		CloseableHttpClient httpClient = WebManager.getInstance().getHttpClient();
		HttpPost post = new HttpPost(Util.reconstituerString(url, client));
		// executer requete HTTP
		CloseableHttpResponse reponse = null;
		HttpContext context = HttpClientContext.create();

		if (param != null) {
			// il y a des parametres
			List<NameValuePair> listeNomValeur = Util.decouperNomValeur(Util.reconstituerString(param, client));
			try {
				post.setEntity(new UrlEncodedFormEntity(listeNomValeur));
			} catch (UnsupportedEncodingException e) {


			}						
		}
		if (paramFic != null) {
			try {
				post.setEntity(new StringEntity(Util.reconstituerString(paramFic, client)));
			} catch (UnsupportedEncodingException e) {
				logger.error("passerRequetePost- erreur de parametre fichier"+e.getMessage());
				return null;
			}
		}
		if (config != null) {
			post.setConfig(config);
		}
		post.setHeader(HttpHeaders.CONTENT_TYPE, contentType+";charset="+charset);
		try {

			reponse = httpClient.execute(post, context);

			if (reponse.getStatusLine().getStatusCode() >= 300 || reponse.getStatusLine().getStatusCode() < 200) {
				logger.error("passerRequetePost - status pour "+url +" "+reponse.getStatusLine().getReasonPhrase());

			}
			else {
				logger.debug("passerRequetePost - status pour "+url +" "+reponse.getStatusLine().getReasonPhrase());
				HttpEntity entity = reponse.getEntity();
				if (entity != null) {
					ContentType contentType = ContentType.getOrDefault(entity);
					Charset charsetResp = contentType.getCharset();
					if (charsetResp == null) {
						charsetResp=Charset.forName(charset);
					}
					BufferedReader rd = new BufferedReader(new InputStreamReader(entity.getContent(), charsetResp));
					StringBuffer buf = new StringBuffer(50);
					String s;
					while ((s=rd.readLine()) != null) {
						buf.append(s);			
					}
					ret=buf.toString();			

					logger.debug("passerRequetePost - retour pour "+url +" "+buf);
				}
			}
		} catch (ClientProtocolException e) {
			logger.error("passerRequetePost - erreur pour "+url+" "+e.getMessage());

		} catch (IOException e) {
			logger.error("passerRequetePost - erreur pour "+url+" "+e.getMessage());
		}
		finally {
			try {
				if (reponse != null) {
					reponse.close();
				}
			} catch (IOException e) {
				logger.warn("passerRequetePost - pb fermeture reponse pour "+url+" "+e.getMessage());
			}
		}

		return ret;
	}


	/**
	 * extraction des données du XML ou Json recu
	 * @param client : données client
	 * @param webService : objet de navigation
	 */
	private void extraireXmlOuJson(Client client, String buf) {

		int pos = 0;		
		Pattern pat;
		Matcher mat;
		String element; //element a rechercher
		String result;

		switch (typeResult) {
		case RSLT_XML:
			// boucle sur les variables a valoriser
			for (String var : getTabVariable()) {
				// element a chercher
				element =getElement(pos);
				pat = Pattern.compile("<"+element+">(.*)</"+element+">");
				logger.debug("extraireXmlOuJson - var ("+pat.toString()+")");
				mat = pat.matcher(buf);
				if (mat.find()) {
					client.setValeur(var, mat.group(1).trim(), boolStat);
				}
				else {
					// non trouve, on met la valeur par defaut
					client.setValeur(var, getValDef(pos), boolStat);
				}				
				pos++;
			}
			break;
		case RSLT_JSON:
			try {
				JSONObject obj = new JSONObject(buf);

				// boucle sur les variables a valoriser
				for (String var : getTabVariable()) {
					// element a chercher
					element =getElement(pos);
					result = obj.getString(element);

					if (result != null) {
						logger.debug("extraireXmlOuJson - var ("+element+") - result ("+result+")");
						client.setValeur(var, result, boolStat);
					}
					else {
						// non trouve, on met la valeur par defaut
						logger.debug("extraireXmlOuJson - var ("+element+") - result (null)");
						client.setValeur(var, getValDef(pos), boolStat);
					}

					pos++;
				}
			} catch (JSONException e) {
				logger.error("extraireXmlOuJson - erreur de parsing json ("+buf+") - "+e.getMessage());
			}
			break;
		case RSLT_TXT:
			// boucle sur les variables a valoriser
			for (String var : getTabVariable()) {

				client.setValeur(var, buf, boolStat);
				pos++;
			}
			break;
		default:
			break;
		} 



	}
	@Override
	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "websvc");
			obj.put("service", getService());
			obj.put("label", label);
			//obj.put("suivant",getSuivant());
			obj.put("methode", methode);
			//obj.put("erreur_suivant", getAction(ERREUR));
			obj.put("statistiques", boolStat);
			obj.put("url", Util.reconstituerString(url));

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
			
			StringBuffer buf = new StringBuffer();
			for (String v: tabVariable) {
				if (buf.length() == 0) buf.append(v);
				else {
					buf.append(',');
					buf.append(v);
				}
			}
			obj.put("variable",buf.toString());
			buf.setLength(0);
			switch (typeResult) {
			case RSLT_XML:
				obj.put("format","XML");
				break;
			case RSLT_JSON:
				obj.put("format","JSON");
				break;
			case RSLT_TXT:
				obj.put("format","TXT");
				break;
			default:
				break;
			}  

			for (String v: tabElement) {
				if (buf.length() == 0) buf.append(v);
				else {
					buf.append(',');
					buf.append(v);
				}
			}

			obj.put("element",buf.toString());
			buf.setLength(0);

			for (String v: tabValeurDef) {
				if (buf.length() == 0) buf.append(v);
				else {
					buf.append(',');
					buf.append(v);
				}
			}
			obj.put("valeur_defaut",buf.toString());
			buf.setLength(0);

			if (param != null) obj.put("parametre",Util.reconstituerString(param));
			if (nomFic != null)	obj.put("parametre_fichier",nomFic);
			if (config != null) obj.put("proxy", config.getProxy().getHostName()+":"+config.getProxy().getPort());
		} catch (JSONException e) {
			logger.error("toJsonObject - "+e.getMessage());
		}

		return obj;
	}
}
