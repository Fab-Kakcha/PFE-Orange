/**
 * 
 */
package test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import orange.olps.svi.initservice.InitService;
import orange.olps.svi.navigation.Navigation;
import orange.olps.svi.navigation.NavigationManager;
import orange.olps.svi.util.Util;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.orange.olps.api.webrtc.*;


/**
 * @author JWPN9644
 * 
 */

public class MonServiceSvi extends OmsService implements OmsMessageListener {


	/**
	 * @param args
	 */
	
	private static Logger logger = Logger.getLogger(MonServiceSvi.class);
	private static final String WEBRTC_CONF = "/opt/testlab/utils/stageFabrice/src/main/java/";
	//private static final String WEBRTC_CONF = "C:\\Users\\JWPN9644\\opt\\application\\64poms\\current\\conf\\";
	protected static String hostVip = "127.0.0.1";
	protected static String portVip = "4670";
	private static String portWs = "8887";
	//private static String hostVipConf ;
	private static String portVipConf ;
	static Properties prop = new Properties();
	static FileInputStream propFic = null;
	static BufferedWriter ficSdp = null;
	
	private static final String DEFAULT_OMS_HOST = "127.0.0.1";
	private static final String DEFAULT_OMS_PORT = "8080";
	private static final String DEFAULT_WS_PORT = "8887";
	private static final String DEFAULT_CONF_PORT = "10000";
	
	boolean isAnswer = false;
	//private List<OmsCall> listOmsCall = new ArrayList<OmsCall>();
	//private String filePath = "/opt/application/64poms/current/tmp/infosOnConferences.log";
	//private String filePath = "C:\\Users\\JWPN9644\\Documents\\infosOnConferences.log";
	private String filePath2 = "/opt/application/64poms/current/tmp/Animaux.a8k";
	
	private OmsConference conf;
	private Annuaire annuaire;	
	private int actionNavigation;
	ArrayList<String> tabPrompt;
	String a8kFile;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		new MonServiceSvi();		
	}

	public MonServiceSvi() {
		
		super(Integer.parseInt(portWs));
		PropertyConfigurator.configure(WEBRTC_CONF + "log4j.properties");

		try {
			propFic = new FileInputStream(WEBRTC_CONF + "webRTC.properties");
		} catch (FileNotFoundException fnfe) {
			logger.error("Le fichier webRTC.properties n existe pas.");
			System.exit(2);
		}
		try {
			prop.load(propFic);
			propFic.close();
		} catch (IOException ioe) {
			logger.error("Impossible de lire le fichier de properties.");
		}

		hostVip = prop.getProperty("oms.host", DEFAULT_OMS_HOST);
		portVip = prop.getProperty("oms.port", DEFAULT_OMS_PORT);
		portWs = prop.getProperty("ws.port", DEFAULT_WS_PORT);
		portVipConf = prop.getProperty("conf.port", DEFAULT_CONF_PORT);
		
		addEventListener(this);
		this.start();
		logger.info("Service  started on port: " + getPort());	
		
		try {
			conf = new OmsConference(hostVip, portVipConf);
			annuaire = new Annuaire();
			new InitService();
			
			//conf.create("conf1");
		} catch (OmsException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		a8kFile = System.getenv("A8KFILES");
	}
	
	public void omsMessagePerformed(OmsMessageEvent msgEvt) {
		// TODO Auto-generated method stub

		//OmsCall call = msgEvt.getOmsCall();
		
		OmsClientSvi call = (OmsClientSvi)msgEvt.getOmsCall();
		//String[] tabPrompt = {infoBienvenue, menuLangue};

		String message = msgEvt.getMessage();
		//logger.info("Nouveau message: " + message);
		//logger.info("Reçu de: " + call + "l'adresse ip: "
				//+ call.getIpAddress());
		OmsMessage msg = new OmsMessage(message);
		String typeMesg = msg.getType();
		try{
		switch (typeMesg) {
			case "sdp":
				// Le message est du sdp
				String sdp = msg.getSdp();
				String userName = msg.getUserName();

				if (!isAnswer) {

					actionNavigation = Navigation.RIEN;
					call.connect(hostVip, portVip);
					call.init(sdp);

					actionNavigation = NavigationManager.getInstance().calculerActionNavigation(call);
					logger.info("actionNavigation: " + actionNavigation);

					switch (actionNavigation) {
					case Navigation.DIFFUSION:
						tabPrompt = call.getPrompt();

						for (String prompt : tabPrompt) {
							prompt = a8kFile + "/" + prompt + ".a8k";
							call.play(prompt, false);
						}
						
						actionNavigation = NavigationManager.getInstance().calculerActionNavigation(call);
						tabPrompt = call.getPrompt();
						for (String prompt : tabPrompt) {
							prompt = a8kFile + "/" + prompt + ".a8k";
							call.play(prompt, false);
						}					
						break;
					// return "/dialogs/diffusionInfo.jsp";
					case Navigation.MENU_SAISIE:
						break;
						// return "/dialogs/diffusionMenu.jsp";
					case Navigation.SAISIE_DTMF:
						break;
						// return "/dialogs/diffusionMenu.jsp";
					case Navigation.TRANSFERT:
						break;
						// return "/dialogs/sviDeconnexionTransfert.jsp";
					case Navigation.DECONNEXION:
						break;
						// return "/dialogs/sviDeconnexion.jsp";
					case Navigation.DISSUASION:
						break;
						// return "/dialogs/diffusionDissuasion.jsp";
					default:
						break;
					}
				}
				break;
		case "cmd":
			// Le message est une commande
			traiteCmd(call, msg);
			break;
		default:
			// Le message est de type inconnu
			logger.error("Format de message inconnu : " + message + ". Ce n'est pas du JSON");
		}
		}catch(OmsException | IOException e){
			e.printStackTrace();
		}
	}

	public void traiteCmd(OmsCall c, OmsMessage msg) {

		OmsClientSvi call = (OmsClientSvi)c;
		String cmd = msg.getCmd();
		String param = msg.getParam();
		
		try{
		switch (cmd) {
			case ("dtmf"):
				call.setSaisie(param);
				actionNavigation = NavigationManager.getInstance().calculerActionNavigation(call);
				logger.info("actionNavigation: " + actionNavigation);
				tabPrompt = call.getPrompt();
				for (String prompt : tabPrompt) {

					prompt = a8kFile + "/" + prompt + ".a8k";
					logger.info("prompt: " + prompt);
					call.play(prompt, false);
				}

				break;
			case "disconnect":		
				//quitter la conf (function unjoin retourne vrai si c'est le dernier à quitter la conf)
				//détruire la conf si c'est le dernier client à quitter la conf
				//conf.infos(call,filePath);
				conf.delete(call);			
				call.delete();
				break;
			default :
				// La commande n'est pas connue
				logger.error("Commande et param inconnus. cmd: " + cmd + " param: "+ param);
		}
		} catch(OmsException | IOException e){
			e.printStackTrace();
		}
	}
	
}
