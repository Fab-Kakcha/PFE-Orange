package orange.olps.svi.servlet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import orange.olps.svi.bdd.ConnexionManager;
import orange.olps.svi.config.Config;
import orange.olps.svi.guide.ConfigGuide;
import orange.olps.svi.navigation.NavigationManager;
//import orange.olps.svi.sms.SMSUtil;
import orange.olps.svi.util.Util;
//import orange.olps.svi.web.WebManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.PropertyConfigurator;

public class InitService extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static java.util.Timer timer = null;;
	private static String nomProperties;
	private static String nomPropertiesLog = null;

	private Date dateRechargement = null;

	/**
	 * nom de la variable d'environnement contenant le chemin 
	 * du 'current'
	 */
	private String varEnvRep = null;

	protected static Log logger = null;

	public void init(ServletConfig config) throws ServletException {

		System.out.println("InitService::init - Entree");
		nomProperties = config.getInitParameter("init-file");
		varEnvRep  =  config.getInitParameter("env-rep");
		if (varEnvRep == null) varEnvRep="HOME_SVI";
				
		String log4jLocation = getLog4JProperties(config.getInitParameter("log4j-init-file"), config.getServletContext());

		if (log4jLocation == null || "".equals(log4jLocation)) {
			System.err.println("InitService::init - propriete introuvable dans web.xml");
			BasicConfigurator.configure();
		} else {

			File ficProp = new File(log4jLocation);
			if (ficProp.exists()) {
				System.out.println("InitService::init -initialisation avec " + log4jLocation);
				PropertyConfigurator.configureAndWatch(log4jLocation, 5000);
				nomPropertiesLog = log4jLocation;
			} else {
				System.err.println("InitService::init -  " + log4jLocation + " fichier introuvable");
				BasicConfigurator.configure();
			}
		}
		super.init(config);

		initialiserSvi();
		
	}
	/**
	 * donne le fichier de propriétés log4j
	 * @param log4jLocation: parametre log4j dans le web.xml
	 */
	private String getLog4JProperties (String log4jLocation, ServletContext sc) {
		String sep = System.getProperty("file.separator");
		String[] tabEnv = {varEnvRep, "HOME_SVI", "HOME_APP", "HOME_SVC"};
		String fic ="";
		String repEnv = null;
		String nomProp = null;

		if (log4jLocation == null || "".equals(log4jLocation)) {
			nomProp = "log4j.properties";
		}
		else {
			int l = log4jLocation.lastIndexOf('/');	

			if (l >=0) {
				nomProp=log4jLocation.substring(l+1);
			}
			else nomProp=log4jLocation;
		}

		for (String s : tabEnv) {
			repEnv = System.getenv(s);
			if (s != null) {
				fic = repEnv+sep+"conf"+sep+nomProp;
				File f = new File (fic);
				if (f.exists() && f.isFile() && f.canRead()) {
					return fic;
				}
			}
		}
		// on n'a pas trouvé de log4j dans les differents chemins possibles
		// onva faire avec celui qui est defini dans web.xml
		fic = sc.getRealPath("/") +sep+ log4jLocation;
		return fic;
	}
	@Override
	public void destroy() {

		//StatManager.getInstance().stop();
		if (NavigationManager.getInstance().isUseBdd()) {
			ConnexionManager.supprimer();            	       
		}
		if (NavigationManager.getInstance().isUseSms()) {
		//	SMSUtil.getInstance().stop();
		}
		timer.cancel();
		super.destroy();
	}
	/**
	 * Rechargement de la conf du SVI par IHM
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {	


		String action = req.getParameter("action"); 

		if ("init".equals(action)) {
			
				logger.info("Demande de reinitialisation manuelle de la configuration");
				initialiserSvi();
		}
		if ("init".equals(action) || "infoInit".equals(action)) {
			// calcul du temps restant avant le prochain rechargement
			DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			resp.setStatus(HttpServletResponse.SC_OK);
			PrintWriter pw = resp.getWriter();
			pw.append("<rechargement><dernier>");
			pw.append(df.format(dateRechargement));
			pw.append("</dernier><prochain>");
			long prochain = Config.getInstance().calculerTempsDodo() / 1000;
			long h = prochain / 3600;
			long m = (prochain%3600) / 60;

			logger.debug("prochain rechargement ("+h+":"+m+")");
			pw.append(Long.toString(h));
			if (h > 1)	pw.append("heures ");
			else pw.append("heure ");

			String tmp = "0"+Long.toString(m);
			pw.append(tmp.substring(tmp.length()-2));
			if (m > 1) pw.append("minutes");
			else pw.append("minute");

			pw.append("</prochain><niveau>");
			if (logger.isDebugEnabled()) {
				pw.append("Debug");
			}
			else if(logger.isInfoEnabled()) {
				pw.append("Info");
			}
			else if(logger.isErrorEnabled()) {
				pw.append("Erreur");
			}
			pw.append("</niveau></rechargement>");
			pw.flush();
			pw.close();
		}
		else if ("log".equals(action)) {
			
			String niveau = req.getParameter("niveau"); 
			logger.info("Demande de changement de niveau de log ("+niveau+")");
			// changement du niveau de log
			File entree = new File(nomPropertiesLog);
			File sortie = new File(nomPropertiesLog+"_1");
			BufferedReader br = new BufferedReader(new FileReader(entree));
			BufferedWriter bw = new BufferedWriter(new FileWriter(sortie));
			String ligne="";

			while ((ligne = br.readLine()) != null){
				if(ligne.startsWith("log4j.logger.orange.olps.svi")){
					if ("Debug".equals(niveau)) {
						bw.write("log4j.logger.orange.olps.svi="+ Level.DEBUG.toString()+",SERVICE\n");
					}
					else if ("Info".equals(niveau)) {
						bw.write("log4j.logger.orange.olps.svi="+ Level.INFO.toString()+",SERVICE\n");
					}
					else if ("Erreur".equals(niveau)) {
						bw.write("log4j.logger.orange.olps.svi="+ Level.ERROR.toString()+",SERVICE\n");
					}

					bw.flush();
				}else{
					bw.write(ligne+"\n");
					bw.flush();
				}
			}
			bw.close();
			br.close();
			sortie.renameTo(new File(nomPropertiesLog));
			sortie.delete();
			
			PrintWriter pw = resp.getWriter();
			pw.append("<rechargement>OK</rechargement>");
			pw.flush();
			pw.close();
			resp.setStatus(HttpServletResponse.SC_OK);
		}		
		else {
			logger.info("Requete non traitee "+action);
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}

	}
	/**
	 * Lancement des fonctions d'initialisation du SVI
	 * @param premierInit : au premier passage le ConnexionManager  a déja ete initialise dès qu'on l'utilise
	 */
	private void initialiserSvi() {		
		if (logger == null) {
			logger = LogFactory.getLog(InitService.class.getName());
		}
		if (timer !=null) {
			timer.cancel();
		}

		Config.getInstance().initialiser(nomProperties, varEnvRep);   /* initialisation fichier properties */

		ConnexionManager.supprimer();
		ConnexionManager.initialiser(); /* initialisation des connexions base (s'il y a lieu)*/

		//StatManager.getInstance().initialiser(); // initialisation des stats

		// Recopie des prompts
		ConfigGuide.getInstance().initialiser();

		Util.reset();

		// chargement de la navigation
		NavigationManager.getInstance().initialiser();

		// rechargement de la config http
		//if (NavigationManager.getInstance().isUseWebSvc() || StatManager.isWriterHttp()) {
			//logger.info("initialiserSvi() - Demarrage du WebManager");
			//WebManager.getInstance().initialiser();
		//}

		if (NavigationManager.getInstance().isUseSms()) {
			logger.info("initialiserSvi() - Demarrage de SMSUtil");
			//SMSUtil.getInstance().initialiser();
		}
		dateRechargement = new Date();
		// timer de reinitialisation periodique
		timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				initialiserSvi();						
			}
		}, Config.getInstance().calculerTempsDodo());

	}
}
