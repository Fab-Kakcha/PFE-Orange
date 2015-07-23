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

public class MonServiceSvaip extends OmsServiceSip implements OmsDtmfListener{
	
	private static Logger logger = Logger.getLogger(MonServiceSvaip.class);
	private static Pattern pat = Pattern.compile("value=([^/>]+)");
	private static final String osName = System.getProperty("os.name").toLowerCase();
	
	private static final String DEFAULT_OMS_HOST = "127.0.0.1";
	private static final String DEFAULT_OMS_PORT = "8080";
	private static final String DEFAULT_WS_PORT = "8887";
	private static final String DEFAULT_CONF_PORT = "10000";
	//private static String WEBRTC_CONF = "/opt/testlab/utils/stageFabrice/src/main/java/";
	private static String WEBRTC_CONF = "/users/ad64poms/testlab/utils/stageFabrice/src/main/java/";
	private static String hostVip = "";
	private static String portVip = "";
	private static String portVipConf = "";
	private static String portUrl2file = "6667";
	private static String ipaddUrl2file = "10.184.154.47:8080";

	private static Properties prop = new Properties();
	private static FileInputStream propFic = null;
	private static BufferedWriter ficSdp = null;

	private static OmsConference omsConference;
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
						
			MonServiceSvaip[] MonServiceSvaip = new MonServiceSvaip[num];
			String service = "svaip";  //Available services: conf, call and svaip	
			
			for(int i=0; i<num ; i++){
								
				MonServiceSvaip[i] = new MonServiceSvaip("Thread #"+i, hostVip, portVip, service);
				MonServiceSvaip[i].start();
			}
			
			a8kFile = System.getenv("A8KFILES");
			
			if(service.equals("conf"))
				omsConference = new OmsConference(hostVip, portVipConf);
									
		} catch (OmsException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();			
		}				
	}
	
	public MonServiceSvaip(String s, String service){
		
		this(s,hostVip,portVip,service);
	}
	
	public MonServiceSvaip(String s, String hostVip, String portVip, String service){
		
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
			case "newSvaip":
					
				String respInfoEvt = null;
				//do{
					
					respInfoEvt = omsCallSip.waitInfo();
					omsCallSip.sendInfoDialogStarted("1");
					mat = pat.matcher(respInfoEvt);	
					
					if(mat.find()){			
							
						prompt = mat.group(1);
						String [] splitResp = prompt.split("\"");
						
						for (String s : splitResp) {
							
							s = s.substring(0, s.length() - 1);
							prompt = s;			
						}
								
						//prompt = a8kFile + "/" + prompt+ ".a8k";
						//prompt= "/users/ad64poms/testlab/utils/Test/prompts/"+ prompt+ ".a8k";	
						prompt = prompt+ ".a8k";
						prompt = omsCallSip.url2file(ipaddUrl2file,portUrl2file, prompt);
						logger.info("prompt: " + prompt);	
						omsCallSip.play(prompt, true, false);
						
					}else 
						logger.info("No match");					
					
					dtmf = omsCallSip.dtmf();
					
					while(dtmf != null){
						
						omsCallSip.sendInfoDialogExit("1");
						respInfoEvt = omsCallSip.waitInfo();
						omsCallSip.sendInfoDialogStarted("2");
						
						mat = pat.matcher(respInfoEvt);	
						
						if(mat.find()){			
								
							prompt = mat.group(1);
							String [] splitResp = prompt.split("\"");
							
							for (String s : splitResp) {
								
								s = s.substring(0, s.length() - 1);
								prompt = s;			
							}
									
							//prompt = a8kFile + "/" + prompt+ ".a8k";
							//prompt= "/users/ad64poms/testlab/utils/Test/prompts/"+ prompt+ ".a8k";	
							prompt = prompt+ ".a8k";
							prompt = omsCallSip.url2file(ipaddUrl2file, portUrl2file, prompt);
							logger.info("prompt: " + prompt);	
							omsCallSip.play(prompt, true, true);
							
						}else 
							logger.info("No match: " + respInfoEvt);
						
						dtmf = omsCallSip.dtmf();						
					}
						omsCallSip.delete();
						new MonServiceSvaip(this.getName(), this.getService()).start();										
						
				break;
			default:
				
				break;
			}
		} catch (OmsException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}