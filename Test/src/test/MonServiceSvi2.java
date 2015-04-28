/**
 * 
 */
package test;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import orange.olps.svi.client.ClientFormat;
import orange.olps.svi.navigation.Deconnexion;
import orange.olps.svi.navigation.Navigation;
import orange.olps.svi.navigation.NavigationManager;
import orange.olps.svi.navigation.Transfert;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.orange.olps.api.webrtc.*;

/**
 * @author JWPN9644
 * 
 */

public class MonServiceSvi2 extends OmsService implements OmsMessageListener {

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
	
	private int actionNavigation;
	ArrayList<String> tabPrompt;
	String a8kFile;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		new MonServiceSvi2();		
	}

	public MonServiceSvi2() {
		
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
			new OmsConference(hostVip, portVipConf);
			new Annuaire();
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
		
		OmsClientSvi call = (OmsClientSvi)msgEvt.getOmsCall();

		String message = msgEvt.getMessage();
		OmsMessage msg = new OmsMessage(message);
		String typeMesg = msg.getType();
		String param = "";
		
		try{
		switch (typeMesg) {
			case "sdp":
				// Le message est du sdp
				String sdp = msg.getSdp();
				msg.getUserName();
				actionNavigation = Navigation.RIEN;
				call.connect(hostVip, portVip);
				call.init(sdp);
				//param = "";

				// actionNavigation = NavigationManager.getInstance().calculerActionNavigation(call);
				 //logger.info("actionNavigation: " + actionNavigation);
				// tabPrompt = call.getPrompt();
				 
				/*for (String prompt : tabPrompt) {
					prompt = a8kFile + "/" + prompt + ".a8k";
					logger.info("prompt: " + prompt);
					call.play(prompt, true);
				}*/
				
				break;
		case "cmd":
			
			//String cmd = msg.getCmd(); //On recevra toujours des dtmf
			param = msg.getParam();
			// Le message est une commande
			call.setSaisie(param);
			//actionNavigation = NavigationManager.getInstance().calculerActionNavigation(call);
			//logger.info("actionNavigation: " + actionNavigation);									
			
			break;						
		default:
			// Le message est de type inconnu
			logger.error("Format de message inconnu : " + message + ". Ce n'est pas du JSON");
		}
		
		//while(!param.equals("disconnect")){
			
			actionNavigation = NavigationManager.getInstance().calculerActionNavigation(call);
			 logger.info("actionNavigation: " + actionNavigation);
			 
			 switch (actionNavigation) {
				case Navigation.DIFFUSION:
					
					tabPrompt = call.getPrompt();
					
					for (String prompt : tabPrompt) {
						prompt = a8kFile + "/" + prompt + ".a8k";
						logger.info("prompt: " + prompt);
						call.play(prompt, true);
					}
					
					actionNavigation = NavigationManager.getInstance().calculerActionNavigation(call);
					logger.info("actionNavigation: " + actionNavigation);
					tabPrompt = call.getPrompt();
					
					for (String prompt : tabPrompt) {
						prompt = a8kFile + "/" + prompt + ".a8k";
						logger.info("prompt: " + prompt);
						call.play(prompt, true);
					}			
					break;
				case Navigation.MENU_SAISIE:
					tabPrompt = call.getPrompt();
					for (String prompt : tabPrompt) {
						prompt = a8kFile + "/" + prompt + ".a8k";
						logger.info("prompt: " + prompt);
						call.play(prompt, true);
					}
					break;
				case Navigation.SAISIE_DTMF:
					tabPrompt = call.getPrompt();
					break;
				case Navigation.DIFFUSION_INACTIVITE:
					tabPrompt = call.getPrompt();
					break;
				case Navigation.TRANSFERT:
					
					break;
				case Navigation.DECONNEXION:
					tabPrompt = call.getPrompt();
					
					actionNavigation = NavigationManager.getInstance().calculerActionNavigation(call);
					 logger.info("actionNavigation: " + actionNavigation);
					
					//String retour = "NORMAL";
				    //Navigation nav= NavigationManager.getInstance().getNavigation(call.getService(), call.getNavCourante());
				    //if (nav != null) {
				      //  if (nav.getClass().getName().equals(Deconnexion.class.getName())) { call.getNavPrecedente()
				    //	   retour = ((Deconnexion) nav).getValeurRetour();
				    	//}        
				    //}
									    
				    Transfert nav1= (Transfert)NavigationManager.getInstance().getNavigation(call.getService(), call.getNavPrecedente());
									    
				    String numTransfert=nav1.getNumeroTransfertAvecParam(call);
					logger.info("numTransfert: " + numTransfert);
					
					ClientFormat f = new ClientFormat(call);
					f.formaterBrut();						
				    
					break;
				case Navigation.DISSUASION:
					tabPrompt = call.getPrompt();
					break;
				default:
					break;
				}
		//}
				 								
		}catch(OmsException | IOException e){
			e.printStackTrace();
		}
	}
}
