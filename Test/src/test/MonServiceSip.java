package test;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.orange.olps.api.sip.*;

public class MonServiceSip extends OmsServiceSip implements OmsDtmfListener{
	
	private static Logger logger = Logger.getLogger(MonServiceSip.class);
	//private static String ipAddress = "10.184.50.176"; //Ihda's IP address
	private static String ipAddress = "10.184.139.164"; //Ihda's IP address
	private static String filePath = "/opt/application/64poms/current/tmp/recordingSip.a8k";
	
	private static final String DEFAULT_OMS_HOST = "127.0.0.1";
	private static final String DEFAULT_OMS_PORT = "8080";
	private static final String DEFAULT_WS_PORT = "8887";
	private static final String DEFAULT_CONF_PORT = "10000";
	private static final String WEBRTC_CONF = "/opt/testlab/utils/stageFabrice/src/main/java/";
	//private static final String WEBRTC_CONF = "C:\\Users\\JWPN9644\\opt\\application\\64poms\\current\\conf\\";
	private static String hostVip = "127.0.0.1";
	private static String portVip = "4670";
	private static String portVipConf = "10000";

	static Properties prop = new Properties();
	static FileInputStream propFic = null;
	static BufferedWriter ficSdp = null;

	boolean omsConnected = false;	
	
	public static void main(String[] args) {
		
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
		portVipConf = prop.getProperty("conf.port", DEFAULT_CONF_PORT);
				
		new MonServiceSip(hostVip, portVip,portVipConf);		
	}
	
	/**
	 * To listen a  sip call
	 * @param hostVip
	 * @param portVip
	 */
	public MonServiceSip(String hostVip, String portVip){
				
		super(hostVip, portVip);
		addEventListener(this);
		startListening();
	}
	
	/**
	 * To listen a sip call for a conference
	 * @param hostVip
	 * @param portVip
	 * @param portVipConf
	 */
	public MonServiceSip(String hostVip, String portVip, String portVipConf){
		
		super(hostVip, portVip);
		addEventListener(this);
		startListeningConf(hostVip, portVipConf);
	}
		
	@Override
	public void omsDtmfPerformed(OmsDtmfEvent dtmfEvt) {
		// TODO Auto-generated method stub

		OmsCallSip omsCallSip = dtmfEvt.getOmsCallSip(); 		
		String dtmf = dtmfEvt.getDtmf();
		
		logger.info("Nouveau message: " + dtmf);
		logger.info("Recu de: " + omsCallSip.getCaller());
		
		try {
			switch (dtmf) {
			case "newCall":				
				omsCallSip.say("Bienvenue sur OMS", false);
				omsCallSip.say("Pour ecouter un fichier audio tapez 1", false);
				omsCallSip.say("Pour un enregistrement vocal tapez 2", false);
				omsCallSip.say("Pour finir l'enregistrement vocal tapez 3", false);
				omsCallSip.say("Pour quitter tapez #", false);
				break;
			case "1":
				omsCallSip.say("Vous avez tapez 1", false);
				omsCallSip.play(filePath, false);
				break;
			case "2":
				omsCallSip.say("Vous avez tapez 2", false);
				omsCallSip.enreg(filePath);
				break;
			case "3":
				omsCallSip.say("Vous avez tapez 3", false);
				omsCallSip.stopEnreg();
				break;
			case "4":
				//String digitArray = "003365353535";
				String digitArray = "";
				omsCallSip.say("Vous avez tapez 4", false);
				omsCallSip.say("Entrez un numero s'il vous plait. Faite * pour finir",false);
				
				do {
					dtmf = omsCallSip.dtmf();
					if (!dtmf.equals("*"))
						digitArray = digitArray + dtmf;

				} while (!dtmf.equals("*"));
				 
				logger.info(digitArray);
				omsCallSip.call(digitArray, ipAddress);				
				break;
			case "#":
				omsCallSip.say("Vous avez tapez #", false);
				omsCallSip.say("Au revoir. A bientot sur OMS", false);
				omsCallSip.hangUp();
				break;
			default:
				omsCallSip.say("Touche inconnue", false);
				break;
			}
		} catch (OmsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}