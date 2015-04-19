package orange.olps.svi.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import orange.olps.svi.client.Client;
import orange.olps.svi.config.Config;
import orange.olps.svi.navigation.Navigation;
import orange.olps.svi.navigation.NavigationManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;



public class Util {

	protected static Log logger = LogFactory.getLog(Util.class.getName());	
	private static final DateFormat df =  new SimpleDateFormat("yyyyMMddHHmmss");

	private static HashMap<String,Long> mapTpsMax = null;
	public static Pattern patVar = Pattern.compile("(_var[A-Za-z0-9]+)");

	
	public Util () {}
	/**
	 * Reinitialisation des constantes
	 */
	public synchronized static void reset() {
		if (mapTpsMax == null) mapTpsMax = new HashMap<String, Long>();
		else mapTpsMax.clear();
	}
	/*
	 * Initialise le timer
	 */	
	public static  Long getTimerApplication() {
		long t = (int)System.currentTimeMillis(); 	
		return new Long(t);
	}
	/**
	 * Calcule la duree en minutes:secondes
	 * @param t0 : top depart
	 * @return long en seconde
	 */
	public static  String getDuree(long t0) {		
		return Long.toString((System.currentTimeMillis() - t0)/1000);
	}
	/*
	 * Verifie le timer global de presence dans l'application en fonction du service
	 * retourne True si echu
	 */
	public static boolean verifierTimerApplicationEchu(Client client) {
		long t = System.currentTimeMillis(); /* on passe en seconde */
		Long tpsMax = mapTpsMax.get(client.getService());
		if (tpsMax == null || tpsMax == -1) return false;
		
		return ((t - client.getTopDepart())/1000 > tpsMax);
	}
	public static long getTimerApplication(String service) {
		return mapTpsMax.get(service);
	}
	/* 
	 * Retourne le nombre max de passage dans l'inactivitï¿½ d'un menu avant deconnexion
	 */
	public static Integer getNbInactiviteMax() {
		try {
			return new Integer (Config.getInstance().getProperty(Config.APPLI_NB_INCTVT_MAX,"5"));
		}
		catch (NumberFormatException e) {
			logger.error("getNbInactiviteMax - format non valide pour "+Config.APPLI_NB_INCTVT_MAX);
			return new Integer (5);
		}
	}

	/* 
	 * Retourne le nombre max de passage dans le rejet d'un menu avant deconnexion
	 */
	public static Integer getRejetMax() {
		return new Integer (Config.getInstance().getProperty(Config.APPLI_NB_RJT_MAX));
	}

	/**
	 * Methode pour mode test
	 * Permet de lire dans le fichier de conf les donnees de type test.
	 * param donne la parametre à lire
	 */
	public static String getTest (String param) {
		/* on relie le fichier de config */
		logger.debug("getTest - param = "+param);
		Properties prop = new Properties();
		File ficProp = Config.getInstance().getFileProperties();
		FileInputStream fis;
		try {
			fis = new FileInputStream(ficProp);

			try {
				prop.load(fis);
				fis.close();
				String p = prop.getProperty("test."+param);
				prop.clear();
				logger.debug("getTest - p = "+p);
				return p;
			} catch (IOException e) {
				logger.error("getTest - Impossible de charger le fichier de config " + e.getMessage());
				return Config.getInstance().getProperty("test."+param);
			}

		} catch (FileNotFoundException e) {
			logger.error("getTest - Impossible de lire le fichier de config " + e.getMessage());
			return Config.getInstance().getProperty("test."+param);
		}
	}
	/**
	 * Methode determinant si on est en mode test
	 * 
	 * 0 = prod
	 * 1 = le numero appelant sera ecrase par la valeur contenue dans le fichier parametre
	 * @return true/false
	 */
	public static boolean isModeDebug () {
		return "1".equals(Config.getInstance().getProperty(Config.DEBUG,"0"));
	}

	/**
	 * getAudioMaxAge() est appelï¿½e dans la jsp root.jsp
	 * pour paramï¿½trer la durï¿½e de vie des fichiers audio
	 * dans le cache de url2file
	 * @return la valeur de maxage
	 */
	public static  String getAudioMaxAge() {
		String max = Config.getInstance().getProperty(Config.MAXAGE_AUDIO);
		if (max == null || "".equals(max)) {
			logger.debug("getAudioMaxAge - Valeur par defaut = 1");
			return "1";			
		}
		else {			
			return max;
		}			
	}
	/**
	 * getDocumentMaxAge() est appelï¿½e dans la jsp root.jsp
	 * pour paramï¿½trer la durï¿½e de vie des documents
	 * dans le cache de url2file
	 * @return la valeur de maxage
	 */
	public static String getDocumentMaxAge() {
		String max = Config.getInstance().getProperty(Config.MAXAGE_DOC);
		if (max == null || "".equals(max)) {
			logger.debug("getDocumentMaxAge - Valeur par defaut = 0");
			return "0";			
		}
		else {			
			return max;
		}			
	}
	/**
	 * getHttpCacheControlMaxAge() est appelï¿½e dans la jsp root.jsp
	 * pour paramï¿½trer la durï¿½e de vie de cette jsp (root.jsp)
	 * dans le cache de url2file
	 * @return la valeur de maxage
	 */
	public static String getHttpCacheControlMaxAge() {
		String max = Config.getInstance().getProperty(Config.MAXAGE_CTRL_CACHE);
		if (max == null || "".equals(max)) {
			logger.debug("getHttpCacheControlMaxAge - Valeur par defaut = 60");
			return "60";			
		}
		else {			
			return max;
		}			
	}
	/**
	 * getFetchtimeout() est appelï¿½e dans la jsp root.jsp
	 * pour paramï¿½trer le timeout d'un fetch
	 * dans le cache de url2file
	 * Attention: doit etre en concordance que le timeout sur les requetes BDD
	 * @return la valeur de timeout
	 */
	public static String getFetchtimeout() {
		String f = Config.getInstance().getProperty(Config.FETCHTIMEOUT);
		if (f == null || "".equals(f)) {
			logger.debug("getFetchtimeout - Valeur par defaut = 10s");
			return "10s";			
		}
		else {		

			return f;
		}			
	}
	/**
	 * Extraction du numero de telephone de l'appelant du remote
	 * @return numero Appelant
	 */
	public static String extraireNumeroAppelant(String num) {

		String p = Config.getInstance().getProperty(Config.PATTERN_NUM_APPELANT);
		String n = "";
		if (num == null) {
			logger.error("extraireNumeroAppelant - numero appelant nul (Pattern="+p+")");
			return Config.getInstance().getProperty(Config.NUMERO_MASQUE);
		}
		if (p != null) {
			n = extraireNumero (num, p);
		}
		if (n.matches("[0-9]+")) {
			return n;
		}
		else 
			return Config.getInstance().getProperty(Config.NUMERO_MASQUE);
	}
	/**
	 * Extraction du numero de telephone de l'appele du local
	 * @return numero Appelant
	 */
	public static String extraireNumeroAppele(String num) {
		String p = Config.getInstance().getProperty(Config.PATTERN_NUM_APPELE);
		if (num == null) {
			logger.error("extraireNumeroAppele - numero appele nul (Pattern="+p+")");
			return "";
		}
		if (p == null || "".equals(p)) return num;
		return extraireNumero (num, p);
	}
	/**
	 * Extraction d'une sous chaine en fonction d'un pattern 
	 * @param num = numero appelant ou appele 
	 * @param p = pattern lu en properties
	 * @return la chaine qui matche 
	 */
	private static String extraireNumero (String num, String p) {
		Pattern pat = Pattern.compile(p);
		Matcher m = pat.matcher(num);
		if (m.find()) {
			return m.group(1);
		}
		else {
			return num;
		}
	}
	/**
	 * Extraction d'une sous chaine en fonction d'un motif
	 * @param src = chaine source
	 * @param pattern = motif d'extraction
	 * @return sous chaine trouvee ou ""
	 */
	public static String extraireChaine (String src, String pattern) {
		Pattern pat = Pattern.compile(pattern);
		Matcher mat = pat.matcher(src);
		if (mat.find()) {
			return mat.group(1);
		}
		else return "";
	}
	/**
	 * Decoupe la chaine en paquet de n caracteres afin de faciliter la vocalisation
	 * ce decoupage depend de la langue
	 * @param chaine : chaine à decouper
	 * @param langue : langue du client
	 * @return tableau de prompts avec la langue
	 */
	public static ArrayList<String> decouperSaisie(String chaine, String langue){
		int nbCar;
		if (chaine == null || "".equals(chaine)) return null;

		try {
			nbCar = Integer.parseInt(Config.getInstance().getProperty(Config.VOCALISATION_SAISIE+langue,"2").trim());
		} catch (NumberFormatException e) {
			logger.error("decouperSaisie - erreur de format de "+Config.VOCALISATION_SAISIE+langue);
			nbCar = 2;
		}
		String suffixe = "_"+langue;
		ArrayList<String> tabPrompt = new ArrayList<String>(1);
		int max = chaine.length();
		for(int i = 0; i < max; i += nbCar) {

			tabPrompt.add(chaine.substring(i, Math.min(i+nbCar, max))+suffixe);
		}
		return tabPrompt;
	}
	/**
	 * Copie de fichiers
	 */
	public static boolean copier (String ficIn, String ficOut) {
		FileChannel in = null; // canal d'entrï¿½e
		FileChannel out = null; // canal de sortie
		logger.debug("copier - entree ficIn = "+ficIn+" ficOut = "+ficOut);

		try {
			// Init
			in = new FileInputStream(ficIn).getChannel();
			out = new FileOutputStream(ficOut).getChannel();

			/* Copie depuis le in vers le out */
			in.transferTo(0, in.size(), out);
		} catch (Exception e) {
			logger.error("copier - Erreur de copie "+e.getMessage());
		} finally { // finalement on ferme
			if(in != null) {
				try {
					in.close();
				} catch (IOException e) {
					logger.error("copier - Erreur de fermeture de in "+e.getMessage());
					return false;
				}
			}
			if(out != null) {
				try {
					out.close();
				} catch (IOException e) {
					logger.error("copier - Erreur de fermeture de out "+e.getMessage());
					return false;
				}
			}
		}
		return true;
	}
	/**
	 * Dans la liste des prompts en entree, identifie ceux qui sont dynamiques (variable de type _var*)
	 * Pour les dynamiques, va rechercher la valeur dans le s donnees client, puis découpe cette donnee
	 * en n prompts
	 * @param tabPrompt : tableau des prompt issu de la Navigation
	 * @param client : client qui a appele
	 * @return : nouveau tableau de prompt
	 */
	public static ArrayList<String> reconstituerListePrompt(ArrayList<String> tabPrompt, Client client) {
		String valeur;
		ArrayList<String> tmp;
		
		if (tabPrompt == null || tabPrompt.size() == 0) return new ArrayList<String>();
		
		ArrayList<String> newTabPrompt = new ArrayList<String>(tabPrompt.size());
		for (String prompt : tabPrompt) {
			if(prompt.startsWith("_var")) {
				valeur = client.getValeur(prompt);
				if (valeur != null) {
					Navigation nav = NavigationManager.getInstance().getNavigation(client.getService(), client.getNavCourante());
					if (valeur.matches("^[0-9]+$")) {
						// c'est une valeur numérique
						// on va la vocaliser en fonction du mode
						if (nav.getPromptDynamiqueMode().equals(Navigation.MODE_LANGUE)) {
							// decoupage de la valeur numerique en fonction du code langue
							tmp = decouperSaisie(valeur, client.getLangue());
							if (tmp != null) newTabPrompt.addAll(tmp);
						}
						else if (nav.getPromptDynamiqueMode().equals(Navigation.MODE_DATE)) {

							Calendar calJour = Calendar.getInstance();
							Calendar cal = Calendar.getInstance();

							try {
								Date d = df.parse(valeur);
								cal.setTime(d);
								// traitement du jour
								if (cal.get(Calendar.DAY_OF_MONTH) == calJour.get(Calendar.DAY_OF_MONTH)
										&& cal.get(Calendar.MONTH) == calJour.get(Calendar.MONTH)
										&& cal.get(Calendar.YEAR) == calJour.get(Calendar.YEAR)) {
									// aujourd'hui
									newTabPrompt.add("Aujourdhui_"+client.getLangue());								
								}
								else  {
									calJour.add(Calendar.DATE, -1);
									if (cal.get(Calendar.DAY_OF_MONTH) == calJour.get(Calendar.DAY_OF_MONTH)
											&& cal.get(Calendar.MONTH) == calJour.get(Calendar.MONTH)
											&& cal.get(Calendar.YEAR) == calJour.get(Calendar.YEAR)) {
										// hier
										newTabPrompt.add("Hier_"+client.getLangue());								
									}
									else {
										newTabPrompt.add("Le_"+client.getLangue());
										if (cal.get(Calendar.DAY_OF_MONTH) == 1) {
											newTabPrompt.add("Premier_"+client.getLangue());
										}
										else {
											newTabPrompt.add(cal.get(Calendar.DAY_OF_MONTH)+"_"+client.getLangue());
										}
										// le mois
										newTabPrompt.add(Util.getMois(cal.get(Calendar.MONTH))+client.getLangue());	
									}

								}
								// l'heure
								newTabPrompt.add("A_"+client.getLangue());
								newTabPrompt.add(cal.get(Calendar.HOUR_OF_DAY)+"_"+client.getLangue());
								newTabPrompt.add("Heure_"+client.getLangue());
								newTabPrompt.add(cal.get(Calendar.MINUTE)+"_"+client.getLangue());
							} catch (ParseException e) {
								logger.error("reconstituerListePrompt - format de date non reconnu pour"+valeur+" (attendu: yyyyMMddHHmmss)");
							}
						}
						else {
							logger.debug("reconstituerListePrompt - ajout de "+valeur+"_"+client.getLangue());
							newTabPrompt.add(valeur+"_"+client.getLangue());
						}
					}
					else {
						// c'est le nom d'un prompt ou une liste de prompt
						String[] tabPrompts = valeur.split(",");
						for (String prt : tabPrompts) {
							if (prt.endsWith(".wav")) {
								// on enleve le .wav
								prt = prt.substring(0, prt.length() - 4);						
							}
							if (prt.contains("_"+client.getLangue()+"_") || prt.endsWith("_"+client.getLangue())) {							
								newTabPrompt.add(prt);
							}
							else {
								newTabPrompt.add(prt+"_"+client.getLangue());
							}
						}
					}
				}
				else {
					logger.warn(" reconstituerListePrompt ("+client.getIdent()+") - Valeur nulle ("+prompt+")");
				}
			}
			else if (prompt.contains("_var")) {
				// le prompt est constitué d'une partie fixe et d'une partie variable
				newTabPrompt.add(Util.reconstituerString(Util.decouperString(prompt), client));
			}
			else {
				newTabPrompt.add(prompt);
			}
		}

		return newTabPrompt;
	}

	private static String getMois(int m) {

		switch (m) {
		case 0:
			return "Janvier_";
		case 1:
			return "Fevrier_";
		case 2:
			return "Mars_";
		case 3:
			return "Avril_";
		case 4:
			return "Mai_";
		case 5:
			return "Juin_";
		case 6:
			return "Juillet_";
		case 7:
			return "Aout_";
		case 8:
			return "Septembre_";
		case 9:
			return "Octobre_";
		case 10:
			return "Novembre_";
		case 11:
			return "Decembre_";
		default:
			break;
		}
		return null;
	}
	/**
	 * Conversion d'une chaine de type timeout (60, 50s, 45ms,10mn) en long (nombre de millisecondes)
	 * @param val = chaine a convertir en long
	 * @return -1 si la conversion a echouee, la valeur en long si OK
	 */
	static public long parseTimeOut (String val) {

		if (val == null || "".equals(val)) {
			logger.error(" parseTimeOut - parametre non numerique "+val);
			return -1;
		}
		String v = val.trim();
		long facteur = 1000; /* par defaut on est en seconde */
		Pattern pat = Pattern.compile("([0-9]+)([mns]*)");
		Matcher mat = pat.matcher(v);
		if (mat.find()) {
			if (mat.group(2) != null) {
				if ("ms".equals(mat.group(2))) {
					facteur = 1;
				}
				else if ("mn".equals(mat.group(2))) {
					facteur = 60000;
				}
				else if ("s".equals(mat.group(2))) {
					facteur = 1000;
				}
				else {
					logger.error(" parseTimeOut - unite non reconnue "+v);
					return -1;
				}
			}
			long delai = -1;
			try {
				delai = Long.parseLong(mat.group(1)) * facteur;
			} catch (NumberFormatException e) {
				logger.error(" parseTimeOut - format non numerique "+v);
			}
			return delai;
		}
		else {
			logger.error(" parseTimeOut - format non correct "+val);
			return -1;
		}		

	} 
	public static String formaterDate (long d) {
		return df.format(new Date(d));
	}
	/**
	 * retourne l'élément suivant d'une liste par rapport a un element donné
	 * @param tab = liste 
	 * @param courant = element courant
	 * @return element suivant de courant ou null si fin de liste;
	 */
	public static String getItemSuivant(List<String> tab, String courant) {
		if (tab == null || tab.size() == 0)  return null;
		if (courant == null || "".equals(courant)) return tab.get(0);

		int i;
		for (i = 0; i < tab.size(); i++) {
			if (courant.equals(tab.get(i))) {
				break;
			}
		}
		if (i < tab.size() - 1) return tab.get(i+1);

		return null;
	}
	public static String getJsp(int actionNavigation) {

		switch (actionNavigation) {
		case Navigation.DIFFUSION:
			return "/dialogs/diffusionInfo.jsp";
		case Navigation.MENU_SAISIE:
			return "/dialogs/diffusionMenu.jsp";
		case Navigation.SAISIE_DTMF:
			return "/dialogs/diffusionMenu.jsp";
		case Navigation.TRANSFERT:
			return "/dialogs/sviDeconnexionTransfert.jsp";
		case Navigation.ENREG_AUDIO:
			return "/dialogs/enregistrementAudio.jsp";
		case Navigation.DIFFUSION_REJET:
			return "/dialogs/diffusionRejet.jsp";
		case Navigation.DIFFUSION_INACTIVITE:
			return "/dialogs/diffusionInactivite.jsp";
		case Navigation.DECONNEXION:
			return "/dialogs/sviDeconnexion.jsp";
		case Navigation.DISSUASION:
			return "/dialogs/diffusionDissuasion.jsp";
		default:
			break;
		}
		return null;
	}
	
	/**
	 * Decoupage d'une chaine en fonction des variables (_varXXX) entre parenthese
	 * @param s : chaine a decouper
	 * @return tableau des morceaux
	 */
	public static ArrayList<String> decouperStringParenthese(String s) {
		// recherche des (_var..)
		ArrayList<String> rst = new ArrayList<String>();
		Pattern pat = Pattern.compile("(\\(_var[^\\)]+\\))");		
		Matcher mat = pat.matcher(s);
		int d = 0;
		int f = 0;


		while (mat.find()) {				

			// on recherche le groupe dans la chaine d'origine
			f = s.indexOf(mat.group(),d);
			if (f > 0) {			
				// on ajoute dans la liste des morceaux de filtre le texte avant le _var
				if (f-6 >= 0) {
					// on verifie qu'il n'y ait pas de CHANGE avant le _var
					if (s.substring(f-6,f).equals("CHANGE")) {
						if (f-6 > 0) rst.add(s.substring(d, f - 6));
						rst.add("CHANGE");
					}
					else {
						rst.add(s.substring(d, f));
					}
				}
				else {
					rst.add(s.substring(d, f));
				}
			}

			// on stocke la variable
			rst.add(mat.group().substring(1, mat.group().length() - 1));
			// on stocke la position					
			d = f+mat.group().length();
		}
		rst.add(s.substring(d));
		return rst;
	}
	/**
	 * Extraction dans un tableau des _var* d'une chaine
	 * @param s
	 * @return
	 */
	public static ArrayList<String> extraireVar(String s) {

		Matcher mat = patVar.matcher(s);
		ArrayList<String> rst = new ArrayList<String>();
		while (mat.find()) {		
			/* verification que la variable ne soit pas déjà en tableau */
			if (!rst.contains(mat.group())) {
				rst.add(mat.group());
			}						
		} 
		logger.debug("extraireVar - tableau des _var*  ("+rst.toString()+")");
		return rst;
	}
	/**
	 * Decoupage d'une chaine en fonction des variables _varXXX
	 * @param s : chaine a decouper
	 * @return tableau des morceaux
	 */
	public static ArrayList<String> decouperString(String s) {
		// recherche des (_var..)
		ArrayList<String> rst = new ArrayList<String>();	

		if (s==null || "".equals(s)) return rst;

		Matcher mat = patVar.matcher(s);
		int d = 0;
		int f = 0;
		boolean change;

		while (mat.find()) {				
			change = false;
			// on recherche le groupe dans la chaine d'origine
			f = s.indexOf(mat.group(),d);
			if (f > 0) {			
				// on ajoute dans la liste des morceaux de filtre le texte avant le _var
				if (f-7 >= 0) {
					// on verifie qu'il n'y ait pas de CHANGE avant le _var
					if (s.substring(f-7,f-1).equals("CHANGE")) {
						if (f-7 > 0) rst.add(s.substring(d, f - 7));
						rst.add("CHANGE");
						change = true;
					}
					else {
						rst.add(s.substring(d, f));
					}
				}
				else {
					rst.add(s.substring(d, f));
				}
			}

			// on stocke la variable
			rst.add(mat.group());
			// on stocke la position	
			if (change) {
				d = f+mat.group().length() + 1;
			}
			else {
				d = f+mat.group().length();
			}
		}
		rst.add(s.substring(d));

		return rst;
	}
	/**
	 * Methode inverse de decouperString: reconstitue la chaine avec les valeurs du client
	 * @param tab : chaine d'origine découpee
	 * @param client : client a traiter
	 * @return la chaine reconstituee
	 */
	public static String reconstituerString(ArrayList<String> tab, Client client) {
		if (tab == null) return "";
		boolean change = false;
		StringBuffer buf = new StringBuffer();
		for (String s : tab) {

			if (s.startsWith("_var")){
				if (change) {
					// if faut transformer la valeur de _var
					change = false;
					String v = client.getValeur(s);
					String valConfig = Config.getInstance().getProperty(Config.VALEUR_CODE+s+"."+v,"");
					if ("".equals(valConfig)) {
						// la valeur à décoder n'a pas ete trouvee
						// on cherche la valeur par defaut
						valConfig = Config.getInstance().getProperty(Config.VALEUR_CODE+s+".defaut", v);
					}
					if (valConfig.contains("_var")) {
						// la valeur contient une variable
						valConfig = reconstituerString(decouperString(valConfig),client);
					}
					buf.append(valConfig);
				}
				else {
					buf.append(client.getValeur(s));
				}
			}
			else {
				if (s.equals("CHANGE")) {
					// la prochaine variable doit etre decodee
					change = true;
				}
				else {
					buf.append(s);
				}
			}
		}
		logger.debug("reconstituerString - client ("+client.getIdent()+") - buffer ("+buf.toString()+")");
		return buf.toString();
	}
	/**
	 * Methode inverse de decouperString: reconstitue la chaine initiale en remplaçant les _var par une constante
	 * @param tab : chaine d'origine découpee
	 * @param cste : chaine constante
	 * @return la chaine reconstituee
	 */
	public static String reconstituerString(ArrayList<String> tab, String cste) {
		if (tab == null) return "";
		boolean change = false;
		StringBuffer buf = new StringBuffer();
		for (String s : tab) {
			if (s.startsWith("_var")){
				if (change) {
					// if faut transformer la valeur de _var
					change = false;					
					String valConfig = Config.getInstance().getProperty(Config.VALEUR_CODE+s+"."+cste,"");
					if ("".equals(valConfig)) {
						// la valeur à décoder n'a pas ete trouvee
						// on cherche la valeur par defaut
						valConfig = Config.getInstance().getProperty(Config.VALEUR_CODE+s+".defaut", cste);
					}
					buf.append(valConfig);
				}
				else {
					buf.append(cste);
				}
			}
			else {
				if (s.equals("CHANGE")) {
					// la prochaine variable doit etre decodee
					change = true;
				}
				else {
					buf.append(s);
				}
			}
		}
		return buf.toString();
	}
	public static String reconstituerString(ArrayList<String> tab) {
		if (tab == null) return "";
		StringBuffer buf = new StringBuffer();
		boolean change = false;
		for (String s : tab) {
			if (change) {
				buf.append(s);
				buf.append(')');
				change=false;
			}
			else {
				buf.append(s);
				if (s.equals("CHANGE")) {
					// la prochaine variable doit etre decodee
					change = true;
					buf.append('(');
				}
				
			}
		}
		return buf.toString();
	}
	/**
	 * Transforme une chaine de type var1=val1,var2=val2,.. (ou & comme separateur)
	 * en tableau de NameValuePair
	 * @param s
	 * @return
	 */
	public static List<NameValuePair> decouperNomValeur(String s) {
		if (s== null || "".equals(s)) return null;
		List<NameValuePair> listNomValeur = new ArrayList<NameValuePair>(1);
		String[] tabNomValeur = s.split("[&,]");
		String[] tab;
		for (String nomVal : tabNomValeur) {
			tab = nomVal.split("=");
			if (tab.length >= 2) {
				listNomValeur.add(new BasicNameValuePair(tab[0],tab[1]));
			}
		}
		return listNomValeur;
	}
	public static String lireFichier (String f) {
		BufferedReader rd;
		try {
			rd = new BufferedReader(new FileReader(new File(f)));
			StringBuffer buf = new StringBuffer(50);
			String b;
			try {
				while ((b=rd.readLine()) != null) {
					buf.append(b);			
				}
			} catch (IOException e) {
				logger.error("initialiserItem - impossible de lire le fichier ("+f+")");
			}
			rd.close();
			return buf.toString().replaceAll("\n", "");
		} catch (FileNotFoundException e) {
			logger.error("initialiserItem - impossible d'ouvrir le fichier ("+f+")");
			return null;
		} catch (IOException e) {
			logger.error("initialiserItem - impossible de fermer le fichier ("+f+")");
			return null;
		}

	}
	/**
	 * Interprete une commande javascript
	 * @param client = donnees du client appelant
	 * @param tabVar = tableau des _var* present dans la commande
	 * @param cmde = commande javascript
	 * @return le resultat de la commande sous forme de chaine
	 */
	public static String interpreterJavaScript (Client client, ArrayList<String> tabVar, String cmde) {
		Object result = "";
		Context cx = ContextFactory.getGlobal().enterContext();
		try {

			cx.setLanguageVersion(Context.VERSION_1_7);
			Scriptable scope = cx.initStandardObjects();

			String val;
			for (String v : tabVar) {
				val = client.getValeur(v);
				if (v.endsWith("Nb")) {
					// variable à interpreter comme une valeur numérique
					if (val == null) {
						logger.debug("interpreterJavaScript ("+client.getNavCourante()+") - ("+client.getValeur(Client.VAR_IDENT)+") "+v+"=0");
						cx.evaluateString(scope, v+"=0",
								"MaCmde", 1, null);
					}
					else {
						logger.debug("interpreterJavaScript ("+client.getNavCourante()+") - ("+client.getValeur(Client.VAR_IDENT)+") "+v+"="+val);
						cx.evaluateString(scope, v+"="+val,
								"MaCmde", 1, null);
					}
				}
				else {

					if (val == null) {
						logger.debug("interpreterJavaScript ("+client.getNavCourante()+") - ("+client.getValeur(Client.VAR_IDENT)+") "+v+"=''");
						cx.evaluateString(scope, v+"=''",
								"MaCmde", 1, null);	 
					}
					else {
						logger.debug("interpreterJavaScript ("+client.getNavCourante()+") - ("+client.getValeur(Client.VAR_IDENT)+") "+v+"='"+val+"'");
						cx.evaluateString(scope, v+"='"+val+"'",
								"MaCmde", 1, null);
					}
				}	  	            		            		            	    	
			}
			
			result = cx.evaluateString(scope, cmde,
					"MaCmde", 1, null);	 
		}
		catch (Exception e) {
			logger.error("interpreterJavaScript ("+client.getNavCourante()+")- ("+client.getValeur(Client.VAR_IDENT)+") - cmde ("+cmde+") - "+e.getMessage());

		} finally {
			Context.exit();
		}
		return Context.toString(result);	 
	}
	/**
	 * Interprète une commande javascript: retourne une valeur d'un objet json
	 * @param json = donnee json
	 * @param cmde = donnée à extraire
	 * @return
	 */
	public static String interpreterJavaScript (String json, String cmde) {
		if (json == null) return "";
		Object result = "";
		Context cx = ContextFactory.getGlobal().enterContext();
		try {

			cx.setLanguageVersion(Context.VERSION_1_7);
			Scriptable scope = cx.initStandardObjects();

			cx.evaluateString(scope, "v='"+json.substring(json.indexOf('{'), json.lastIndexOf('}'))+"'",
								"MaCmde1", 1, null);
			
			cmde = "v."+cmde;
			result = cx.evaluateString(scope, cmde,
					"MaCmde1", 1, null);	 
		}
		catch (Exception e) {
			logger.error("interpreterJavaScript  - cmde ("+cmde+") - "+e.getMessage());
			return null;

		} finally {
			Context.exit();
		}
		return Context.toString(result);	 
	}
	/**
	 * Ajoute la duree max de presence dans le service dans la map
	 * @param service
	 * @param tempo
	 */
	public static synchronized void addTpsMax(String service, String tempo) {
		if (mapTpsMax == null) {
			 mapTpsMax = new HashMap<String, Long>();						
		}
		Long tpsMax;

		if(tempo == null || "".equals(tempo)) {
			tpsMax = (long) -1;
		}
		else {
			try {
				tpsMax = Long.parseLong(tempo);
			} catch (NumberFormatException e) {
				logger.error("addTpsMax - duree max de presence sur le service ("+service+") non numérique :"+tempo);
				tpsMax = (long) -1;
			}
		}
		mapTpsMax.put(service, tpsMax);	
	}
	public static String encoderUri(String uri, String charset) throws UnsupportedEncodingException {
		StringBuffer newUri = new StringBuffer();
		String[] tab = uri.split("[?&=]");
		newUri.append(tab[0]);
		if (tab.length > 1) {
			newUri.append('?');	
			for (int i = 1; i < tab.length; i++) {
				if (i%2 == 0) {
					// c'est une valeur				
					newUri.append(URLEncoder.encode(tab[i], charset));
				}
				else {
					// c'est le nom d'un parametre
					if (i != 1) newUri.append('&');
					newUri.append(tab[i]);
					newUri.append('=');
				}
			}
		}
		return newUri.toString();
	}
}

