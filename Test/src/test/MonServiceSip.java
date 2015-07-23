package test;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.orange.olps.api.sip.*;

public class MonServiceSip extends OmsServiceSip implements OmsDtmfListener{
	
	private static Logger logger = Logger.getLogger(MonServiceSip.class);
	private static Pattern pat = Pattern.compile("value=([^/>]+)");
	private static final String osName = System.getProperty("os.name").toLowerCase();
	//private static String ipAddress = "10.184.50.176"; //Ihda's IP address
	private static final String ipAddress = "10.184.176.1"; //Laptop's IP address
	private static String filePath = "/opt/application/64poms/current/tmp/recordingSip.a8k";
	
	private static final String DEFAULT_OMS_HOST = "127.0.0.1";
	private static final String DEFAULT_OMS_PORT = "8080";
	private static final String DEFAULT_WS_PORT = "8887";
	private static final String DEFAULT_CONF_PORT = "10000";
	private static String WEBRTC_CONF = "/opt/testlab/utils/stageFabrice/src/main/java/";
	//private static String WEBRTC_CONF = "/users/ad64poms/testlab/utils/stageFabrice/src/main/java/";
	private static String hostVip = "127.0.0.1";
	private static String portVip = "4670000";
	private static String portVipConf = "10000";

	private static Properties prop = new Properties();
	private static FileInputStream propFic = null;
	private static BufferedWriter ficSdp = null;

	private static OmsConference omsConference;
	private String pathVideoFile = "/opt/application/64poms/current/tmp/recordedVideo.h264";
	private static int num = 1;
	private static String a8kFile;
	private String prompt;
	private Matcher mat;
	
	public static void main(String[] args) {
		
		System.out.println("OS = " + osName);		
		if(osName.indexOf("windows") != -1){
			
			WEBRTC_CONF = "C:\\Users\\JWPN9644\\opt\\application\\64poms\\current\\conf\\";
			Scanner scan = null;
			
			try{				
				scan = new Scanner(System.in);
				System.out.print("Enter the number of Threads to launch: ");  
				String s = scan.next();
				num = Integer.valueOf(s);				
			}finally{				
				if(scan != null)
					scan.close();
			}
		}
			
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
				
		if(args.length > 0){
			
			try{				
				num = Integer.valueOf(args[0]);
			}catch(NumberFormatException e){
				logger.error("cannot convert " + args[0] + " to an Integer");
				num = 1;
			}							
		}
			
		try {
						
			MonServiceSip[] monsServiceSip = new MonServiceSip[num];
			String service = "svaip";  //Available services: conf, call and svaip	
			
			for(int i=0; i<num ; i++){
								
				monsServiceSip[i] = new MonServiceSip("Thread #"+i, hostVip, portVip, service);
				monsServiceSip[i].start();
			}
			
			a8kFile = System.getenv("A8KFILES");
			
			if(service.equals("conf"))
				omsConference = new OmsConference(hostVip, portVipConf);
									
		} catch (OmsException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();			
		}				
	}
	
	/*public MonServiceSip(String hostVip, String portVip, int num){
				
		//super(hostVip, portVip);		
		
		for(int i=0; i<num ; i++){
			
			//addEventListener(this);
			MyThread myThread = new MyThread("Thread #"+i, hostVip, portVip, this);
			myThread.start();
		} 		
	}*/
	
	public MonServiceSip(String s, String service){
		
		this(s,hostVip,portVip,service);
	}
	
	public MonServiceSip(String s, String hostVip, String portVip, String service){
		
		super(s, hostVip, portVip, service);
		addEventListener(this);		
	}
	
		
	@Override
	public void omsDtmfPerformed(OmsDtmfEvent dtmfEvt) {
		// TODO Auto-generated method stub
			
		OmsCallSip omsCallSip = dtmfEvt.getOmsCallSip(); 		
		String dtmf = dtmfEvt.getDtmf();
			
		logger.info("Nouveau message: " + dtmfEvt.getDtmf());
		logger.info("Recu de: " + omsCallSip.getCaller());
		
		try {
			switch (dtmf) {
			case "newCall":	
								
				omsCallSip.say("Bienvenue sur OMS", false);
				omsCallSip.say("Pour ecouter un fichier audio tapez 1", false);
				//omsCallSip.say("Pour un enregistrement vocal tapez 2", false);
				//omsCallSip.say("Pour finir l'enregistrement vocal tapez 3", false);
				omsCallSip.say("Pour quitter tapez #", false);
				
				break;
			case "newConf":		
				
				String conferenceName = omsCallSip.getConfname();
				
				omsCallSip.say("Bienvenue sur la conference", false);
				//omsCallSip.say("Taper 1 pour l'enregistrement", false);
				//omsCallSip.say("Taper 2 pour arreter l'enregistrement", false);
				//omsCallSip.say("Taper 3 pour mute", false);
												
				boolean bool = omsConference.status(conferenceName);				
				if(!bool)
				   omsConference.create(omsCallSip, conferenceName);
				else 
				   omsConference.add(omsCallSip, conferenceName);
				   
				//while(true);
				break;
			case "1":
				//omsCallSip.say("Vous avez tapez 1", false);
				//omsCallSip.play(filePath, false);
				//omsConference.startRecording(omsCallSip);
				break;
			case "2":
				//omsCallSip.say("Vous avez tapez " + dtmf, false);				
				//omsCallSip.enreg(filePath);
				//omsConference.stopRecording(omsCallSip.getConfname());
				break;
			case "3":
				//omsCallSip.say("Vous avez tapez " + dtmf, false);
				//omsCallSip.stopEnreg();
				//omsConference.mute(omsCallSip);
				break;
			case "4":
				omsConference.mute(omsCallSip);
				break;
			case "5":
				//omsConference.playRecording(omsCallSip);
				omsConference.unmute(omsCallSip);
				break;
			case "6":
				String digitArray = "003365353535";
				//String digitArray = "";
				//omsCallSip.say("Vous avez tapez "+ dtmf, false);
				//omsCallSip.say("Entrez un numero s'il vous plait. Faite * pour finir",false);
				
				do {
					dtmf = omsCallSip.dtmf();
					if (!dtmf.equals("*"))
						digitArray = digitArray + dtmf;

				} while (!dtmf.equals("*"));
				 
				digitArray = "530256";
				//omsCallSip.call(digitArray, ipAddress);
				
				bool = omsConference.status("conf1");				
				if(!bool)
				   omsConference.create(omsCallSip, "conf1"); 
				else
					omsConference.add(omsCallSip, "conf1"); 
				
				//omsConference.create(omsCallSip, "conf1");
				
				OmsCallSip sipCall = omsCallSip.call1(digitArray, ipAddress);
				sipCall.say("Bienvenu, vous allez entrer dans un conferencee", false);
				
				omsConference.add(sipCall, "conf1");
				
				break;
			case "#":
				//omsCallSip.say("Vous avez tapez #", false);
				//omsCallSip.say("Au revoir. A bientot sur OMS", false);
				if(omsCallSip.isTrombone())
					omsCallSip.hangupCall();
				
				//omsConference.delete(omsCallSip);				
				omsCallSip.delete();
				break;
			case "newThread":
				new MonServiceSip(this.getName(), this.getService()).start();
				break;
			case "*":		
				
				break;
			default:
				omsCallSip.say("Touche inconnue", false);				
				break;
			}
		} catch (OmsException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}