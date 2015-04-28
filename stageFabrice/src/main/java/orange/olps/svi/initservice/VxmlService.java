package orange.olps.svi.initservice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.orange.olps.api.webrtc.OmsClientSvi;

import orange.olps.svi.config.Config;
import orange.olps.svi.navigation.Navigation;
import orange.olps.svi.navigation.NavigationManager;
import orange.olps.svi.util.Util;

//public class VxmlService extends HttpServlet {
public class VxmlService {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9148673782895240468L;

	protected static Log logger = LogFactory.getLog(VxmlService.class);
	
	//protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			//throws ServletException, IOException {
		//doPost(req,resp);
	//}
	//protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			//throws ServletException, IOException {

	@SuppressWarnings("unused")
	public VxmlService(){
		
		String paramEtat = null; //=
				//req.getParameter("etat");
		//HttpSession session = req.getSession(true);
		int  actionNavigation = Navigation.RIEN;
		OmsClientSvi client = null;

		//logger.debug("VxmlService::doPost - entree etat ("+paramEtat+")");

		if ("init".equals(paramEtat)) {
			String paramNavigation = null;// = req.getParameter("navigation");
			String paramConId = null; //= req.getParameter("conid");
			String paramClient = null;// = req.getParameter("client");
			if (paramClient == null || "null".equals(paramClient)) {
				
				String paramAppelant = null ;//=   Util.extraireNumeroAppelant(req.getParameter("callingNumber"));
				String paramAppele = null;// =  req.getParameter("calledNumber");

				logger.debug("VxmlService::doPost - nouveau client ("+paramConId+")");
				//client = new OmsClientSvi(paramConId, paramAppele, paramAppelant, paramNavigation);
			}
			else {		
				//client = new Client(paramClient, paramConId, paramNavigation);
			}
		
		
			if (paramNavigation == null || "null".equals(paramNavigation)) {
				// premiere entree dans le service
				paramNavigation = NavigationManager.getInstance().getRacineSvc(client.getService());
				client.setNavCourante(paramNavigation);
			}
			
		}
		else {
			//client = (Client) session.getAttribute("client");
			if ("filled".equals(paramEtat)) {				
				String grammarResult = null; //= req.getParameter("grammarResult");		
				logger.debug("VxmlService::doPost - DTMF ("+grammarResult+")");
				if(grammarResult != null && !"".equals(grammarResult)) {
					client.ajouterSaisie(grammarResult);
				}
			}
		}
		
		// Verification du timer
		// on vérifie le timer de presence dans l'application
		if (Util.verifierTimerApplicationEchu (client)){
			// on a atteind le temps max dans l'application
			String message = Config.getInstance().getProperty(Config.PROMPT_TMPS_DEPASSE,"");
			// on trace la raison de la deconnexion
			//StatManager.getInstance().posterStatistiques(client.getIdent(),Client.TIMEOUT, System.currentTimeMillis(), Client.VAR_RAISON);
			if ("".equals(message)) {
				//on deconnecte
				actionNavigation = Navigation.DECONNEXION;
				
			}
			else {
				// il faut diffuser le message de dissuasion
				client.setPrompt(message+"_"+client.getLangue());
				actionNavigation = Navigation.DISSUASION;
			}
		}
		else {
			// calcul de l'action suivante en fonction de la navigation courante
			actionNavigation = NavigationManager.getInstance().calculerActionNavigation(client);
		}
		
		//session.setAttribute("client", client);
		// calcul de la jsp suivante
		String jsp = Util.getJsp(actionNavigation);
		if (jsp == null || "".equals(jsp)) {
			jsp = Util.getJsp(Navigation.DECONNEXION);
		}
		logger.debug("VxmlService::doPost - envoie JSP "+jsp);
		//this.getServletContext().getRequestDispatcher(jsp).forward(req, resp);
		
	}
		
			
	//}
	
}
