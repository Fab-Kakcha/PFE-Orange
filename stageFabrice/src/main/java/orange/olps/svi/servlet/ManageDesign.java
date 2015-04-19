package orange.olps.svi.servlet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;

import orange.olps.svi.config.Config;
import orange.olps.svi.navigation.NavigationManager;

/**
 * Servlet de gestion du design du service
 * sauve/lecture du fichier json de sauvegarde du json 
 * @author awar6486
 *
 */

public class ManageDesign extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9148673782895240468L;

	protected static Log logger = LogFactory.getLog(ManageDesign.class);
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String action = req.getParameter("action"); 
		
		if ("infoSvi".equals(action)) {
			logger.info("Demande de l'architecture du SVI");
			PrintWriter pw = resp.getWriter();
			pw.append(NavigationManager.getInstance().toJsonString());
			pw.flush();
			pw.close();
			resp.setContentType("application/json");
			resp.setStatus(HttpServletResponse.SC_OK);			
		}
		else  if ("lsteRep".equals(action)) {
			logger.info("Demande des noms de fichiers du répertoire JSON");
			String rep = Config.getInstance().getProperty(Config.REP_JSON, "");
			File r = new File(rep);
			if (r.exists() && r.isDirectory()) {
				String[] tabFic = r.list();
				PrintWriter pw = resp.getWriter();
				resp.setContentType("application/json");

				JSONArray arr = new JSONArray();
				for (String s : tabFic) {
					arr.put(s);
				}	
				pw.append(arr.toString());
				pw.flush();
				pw.close();
				
				resp.setStatus(HttpServletResponse.SC_OK);
			}
			else {
				logger.error("Requete non traitee ("+action+") repertoire ("+rep+") inexistant.");
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
			
		}
		else if ("get".equals(action)) {
			logger.info("Demande d'un fichier JSON");
			String nom = req.getParameter("nom");
			if (nom != null && !"".equals(nom)){
				
				String rep = Config.getInstance().getProperty(Config.REP_JSON, "");
				File r = new File(rep);
				if (r.exists() && r.isDirectory()) {
					File f = new File (rep+System.getProperty("file.separator")+nom);
					BufferedReader br = new BufferedReader(new FileReader(f));
					PrintWriter pw = resp.getWriter();
					String ligne = null;
					while ((ligne=br.readLine()) != null) {
						pw.append(ligne);
					}
					
					br.close();
					pw.flush();
					pw.close();
					resp.setContentType("application/json");
					resp.setStatus(HttpServletResponse.SC_OK);
				}
				else {
					logger.error("Requete non traitee ("+action+") repertoire ("+rep+") inexistant.");
					resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			}
			else {
				logger.error("Requete non traitee "+action+" le nom du fichier JSON est vide.");
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
		}
		else if ("sauve".equals(action)) {
			logger.info("Requete non traitee ("+action+") en HTTP-GET essayer en POST");
			resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
		else {
			logger.info("Requete non traitee ("+action+")");
			resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String action = req.getParameter("action"); 
		
		if ("sauve".equals(action)) {
			
			String nom = req.getParameter("nom");
			if (nom != null && !"".equals(nom)){
				logger.info("Demande de sauvegarde JSON ("+nom+")");
				String rep = Config.getInstance().getProperty(Config.REP_JSON, "");
				File r = new File(rep);
				if (r.exists() && r.isDirectory()) {
					File f = new File (rep+System.getProperty("file.separator")+nom);
					BufferedWriter bw = new BufferedWriter(new FileWriter(f, false));
					ServletInputStream sis = req.getInputStream();
					byte buf[] = new byte[500];
					int l = 0;
					while ((l= sis.readLine(buf, 0, buf.length)) > 0) {
						bw.append(new String(buf,0,l));
					}
					bw.flush();
					bw.close();
					resp.setStatus(HttpServletResponse.SC_OK);
				}
				else {
					logger.error("Requete non traitee ("+action+") repertoire ("+rep+") inexistant.");
					resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			}
			else {
				logger.error("Requete non traitee ("+action+") le nom du fichier JSON est vide.");
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
		}
		else {
			doGet (req,resp);
		}
	}
}
