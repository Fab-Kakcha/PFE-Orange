package orange.olps.svi.navigation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;
import orange.olps.svi.client.ClientFormat;
import orange.olps.svi.config.Config;
import orange.olps.svi.util.Util;

public class Enregistrement extends Navigation {

	
	private static final String FMT_BRUT = "BRUT";
	private static final String FMT_CSV = "CSV";
	private static final String FMT_JSON = "JSON";
	

	/**
	 * Mode d'enregistrement: TEXTE ou AUDIO
	 */
	private String mode = MODE_TEXTE;
	private String nomFic = "";
	private String format = FMT_BRUT;
	private String[] tabVariable;
	/**
	 * fichier d'enregistrement
	 */
	private BufferedWriter bw = null;
	/**
	 * date du fichier associe au Writer
	 */
	private String dateWriter = null;
	
	/**
	 * duree max d'enregistrement
	 */
	private String dureeMaxEnreg = null;
	/**
	 * Booleen indiquant si le programme doit creer des sous repertoires
	 * par date pour stocker les fichiers audio
	 */
	private boolean boolSousRepAudio;
	/**
	 * Booleen indiquant si le programme doit creer des sous repertoires
	 * par date pour stocker les fichiers texte
	 */
	private boolean boolSousRepTexte;
	/**
	 * duree max du silence de fin
	 */
	private  String dureeMaxSilence = null;
	
	/**
	 * dtmf de fin d'enregistrement
	 */
	private  String finalDtmf = null;
	/**
	 * repertoire ou seront stocke les fichiers texte
	 */
	private  String repTexte = null;
	
	/**
	 * repertoire ou seront stocke les fichiers audio
	 */
	private  String repAudio = null;
	/**
	 * nom du fichier audio: par defaut ce sera _varNumAppele+'_'+<date>+'_'+_varNumAppelant+"_FR".wav
	 */
	private String nomAudio = null;
	
	public Enregistrement(String lab, String svc) {
		super(lab, svc);
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
		setMode(lstProp.getProperty(racinePropriete+MODE,MODE_TEXTE));
		setNomFic(lstProp.getProperty(racinePropriete+NOM,""));
		
		String s = lstProp.getProperty(racinePropriete+FORMAT, "");
		if ("".equals(s)) {
			// lecture du format dans le fichier de config general
			s =  Config.getInstance().getProperty(racinePropriete+FORMAT, "");
			if ("".equals(s)) {
				s = Config.getInstance().getProperty(Config.ENREG_FMT, FMT_BRUT);
			}
		}
		setFormat(s);
		
		// lecture des parametres a ecrire dans le fichier
		s = lstProp.getProperty(racinePropriete+VARIABLE, "");
		if ("".equals(s)) {
			// lecture du format dans le fichier de config general
			s = Config.getInstance().getProperty(racinePropriete+VARIABLE, "");
			if ("".equals(s)) {
				s = Config.getInstance().getProperty(Config.ENREG_VAR, "");
			}
		}
		tabVariable =  s.trim().split(",");
		
		s = lstProp.getProperty(racinePropriete+DUREE_MAX_ENREG, "");
		if ("".equals(s)) {
			// lecture du format dans le fichier de config general
			s = Config.getInstance().getProperty(racinePropriete+DUREE_MAX_ENREG, "");
			if ("".equals(s)) {
				s = Config.getInstance().getProperty(Config.DUREE_MAX_ENREG, "");
			}
		}
		if (s.equals("")) {
			dureeMaxEnreg = "10s";
		}
		else {
			
			long l = Util.parseTimeOut(s);
			if (l == -1) {
				dureeMaxEnreg = "10s";
			}
			else {
				dureeMaxEnreg = s;
			}
		}
		
		s = lstProp.getProperty(racinePropriete+FINAL_SILENCE, "").trim();
		if ("".equals(s)) {
			// lecture du format dans le fichier de config general
			s = Config.getInstance().getProperty(racinePropriete+FINAL_SILENCE, "").trim();
			if ("".equals(s)) {
				s = Config.getInstance().getProperty(Config.FINAL_SILENCE, "").trim();
			}
		}
		if (s.equals("")) {
			dureeMaxSilence = "4s";
		}
		else {
			
			long l = Util.parseTimeOut(s);
			if (l == -1) {
				dureeMaxSilence = "4s";
			}
			else {
				dureeMaxSilence = s.trim();
			}
		}
		
		s = lstProp.getProperty(racinePropriete+FINAL_DTMF, "").trim();
		if ("".equals(s)) {
			// lecture du format dans le fichier de config general
			s = Config.getInstance().getProperty(racinePropriete+FINAL_DTMF, "").trim();
			if ("".equals(s)) {
				s = Config.getInstance().getProperty(Config.FINAL_DTMF, "").trim();
			}
		}
		if (s.equals("")) {
			finalDtmf = "";
		}
		else {
			if (s.length() == 1) {
				finalDtmf = s;
			}
			else {
				finalDtmf = s.substring(0, 1);
			}
			
		}
		
		s = lstProp.getProperty(racinePropriete+ENREG_REP_TXT, "");
		if ("".equals(s)) {
			// lecture du format dans le fichier de config general
			s = Config.getInstance().getProperty(racinePropriete+ENREG_REP_TXT, "");
			if ("".equals(s)) {
				s = Config.getInstance().getProperty(Config.ENREG_REP_TXT, "");
			}
		}
		if (s.equals("")) {
			repTexte = "/tmp/";
		}
		else {
				repTexte = s.trim();			
		}
		s = lstProp.getProperty(racinePropriete+ENREG_REP_TXT+"."+DATE, "");
		if ("".equals(s)) {
			// lecture du format dans le fichier de config general
			s = Config.getInstance().getProperty(racinePropriete+ENREG_REP_TXT+"."+DATE, "");
			if ("".equals(s)) {
				s = Config.getInstance().getProperty(Config.ENREG_REP_TXT+"."+DATE, "");
			}
		}
		if (s.equals("")) {
			boolSousRepTexte = true;
		}
		else {			
			boolSousRepTexte = Boolean.parseBoolean(s.trim());			
		}
		
		s = lstProp.getProperty(racinePropriete+ENREG_REP_AUDIO, "");
		if ("".equals(s)) {
			// lecture du format dans le fichier de config general
			s = Config.getInstance().getProperty(racinePropriete+ENREG_REP_AUDIO, "");
			if ("".equals(s)) {
				s = Config.getInstance().getProperty(Config.ENREG_REP_AUDIO, "");
			}
		}
		if (s.equals("")) {
			repAudio = "/tmp/";
		}
		else {
				repAudio = s.trim();
		}
		
		s = lstProp.getProperty(racinePropriete+ENREG_REP_AUDIO+"."+DATE, "");
		if ("".equals(s)) {
			// lecture du format dans le fichier de config general
			s = Config.getInstance().getProperty(racinePropriete+ENREG_REP_AUDIO+"."+DATE, "");
			if ("".equals(s)) {
				s = Config.getInstance().getProperty(Config.ENREG_REP_AUDIO+"."+DATE, "");
			}
		}
		if (s.equals("")) {
			boolSousRepAudio = true;
		}
		else {			
			boolSousRepAudio = Boolean.parseBoolean(s.trim());			
		}
		// nom du fichier audio
		nomAudio = lstProp.getProperty(racinePropriete+"nom.audio", "").trim().replaceAll("[+'\"]", "");	
		
		return ret;
	}

	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public String getNomFic() {
		return nomFic;
	}
	public void setNomFic(String nomFic) {
		this.nomFic = nomFic;
	}
	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	@Override
	public void calculerActionNavigation(Client client) {
		int actionNav = client.getActionNavigation();
		
		logger.debug("calculerActionNavigation - ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+") action precedente="+actionNav);
		if (actionNav == Navigation.RIEN) {
			// premier passage dans cette fonction pour ce client
			// Statistiques

		}
		if (actionNav == Navigation.RIEN  && this.isPrompt() && MODE_TEXTE.equals(mode)) {
			// premier passage dans cet item de navigation
			// il faut lire les prompts si on est en mode texte
			// En enregistrement Audio le prompt est joue dans la JSP record
			if(preparerPrompt(client) > 0) {
				client.setActionNavigation(DIFFUSION);
			}
			else {
				// mode texte, c'est fini
				enregDansFichier(client);
				client.setActionNavigation(Navigation.RIEN);
				client.setNavCourante(getSuivant());
			}
		}
		else if (Navigation.ENREG_AUDIO == actionNav) {
			// l'enregistrement est termine
			// action suivante
			String nomWave = client.getValeur(Client.VAR_TMP);
			if (nomWave != null && !"".equals(nomWave)) {
				enregDansFichier(client);
			}
			client.setActionNavigation(Navigation.RIEN);
			client.setNavCourante(getSuivant());
		}
		else  {
			
			if (MODE_AUDIO.equals(mode)) {
				// on part en enregistrement AUDIO
				
				client.setActionNavigation(Navigation.ENREG_AUDIO);
				client.resetSaisie();
			}
			else {
				// mode texte, c'est fini
				enregDansFichier(client);
				client.setActionNavigation(Navigation.RIEN);
				client.setNavCourante(getSuivant());
			}
		}	
		
	}
	/**
	 * enregistrement des donnees clients dans un fichier texte
	 *  que l'on soit en mode AUDIO ou TEXTE
	 * @param client : donnees du client 
	 */
	private void enregDansFichier(Client client) {
		if (nomFic != null && !"".equals(nomFic)) {
			
			ClientFormat f = new ClientFormat(client);
			if (FMT_CSV.equals(format)) {
				f.formaterCsv(tabVariable);
			}
			else if(FMT_JSON.equals(format)) {
				f.formaterJson();
			}
			else {
				f.formaterBrut();
			}
			// ecriture dans le fichier
			determinerWriter (client);		
			ecrireFichier(f.toString());
		}
	}
	/**
	 * determine le fichier (ou les fichiers) a ouvrir en fonction d'une date
	 * les ouvre si necessaire apres avoir cree le repertoire yyyymmdd
	 * @param t - date en millisecondes
	 */
	private void determinerWriter(Client client) {
		File f;
		String rep;
		String dt = Util.formaterDate(client.getTopDepart()).substring(0, 8);
		
		if (boolSousRepTexte) {
				
			if (dt.equals(dateWriter) && bw != null) return;// fichier deja ouvert
			fermerFichier();
			rep = repTexte+dt;		
		}
		else {
			if (bw != null) return;// fichier deja ouvert
			rep = repTexte;
		}
		f = new File (rep);
		if (!f.isDirectory())
			f.mkdir();
				
		if (boolSousRepTexte) {
			f =	new File (rep+File.separator+nomFic+dt+".txt");
		}
		else {
			f =	new File (rep+File.separator+nomFic+".txt");
		}
		logger.debug("determinerWriter::determinerWriter - ouverture fichier enreg ("+f.getAbsolutePath()+") ");
	        
     	try {
     		bw =new BufferedWriter(new FileWriter(f, true));
     		dateWriter = dt;
		} catch( IOException e1) {
			logger.error("determinerWriter::determinerWriter - impossible d'ouvrir "+f.getName()+" "+e1.getMessage());
			bw = null;
		} 

	}

	private void fermerFichier() {
		if (bw != null) {
			try {
				bw.flush();
				bw.close();
				bw = null;
			} catch (IOException e) {
				logger.error("determinerWriter::determinerWriter - impossible de fermer le fichier de stats "+e.getMessage());
			}
			
		}
	}
	private void ecrireFichier(String don) {
		if (bw != null) {
			try {
				bw.write(don);
				bw.write('\n');
				bw.flush();
			} catch (IOException e) {
				logger.error("determinerWriter::ecrireFichier - impossible d'ecrire dans le fichier "+e.getMessage());			
			}
		}
		else {
			logger.info("determinerWriter::ecrireFichier - impossible d'ecrire dans le fichier");
		}
		
	}
	public String getDureeMaxEnreg() {
		return dureeMaxEnreg;
	}
	public void setDureeMaxEnreg(String dureeMaxEnreg) {
		this.dureeMaxEnreg = dureeMaxEnreg;
	}
	public String getDureeMaxSilence() {
		return dureeMaxSilence;
	}
	public  String getRepTexte() {
		return repTexte;
	}
	// fonction utilisée par la RecordServletSvi
	public String getRepAudio() {
		return repAudio;
	}
	public boolean isBoolSousRepAudio() {
		return boolSousRepAudio;
	}
	public String getFinalDtmf() {
		return finalDtmf;
	}
	public String getNomAudio() {
		return nomAudio;
	}
	@Override
	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "enreg");
			obj.put("service", getService());
			obj.put("label", label);
			//obj.put("suivant", getSuivant());
			obj.put("mode", mode);
			obj.put("nom_texte", nomFic);
			obj.put("format", format);
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
			
			obj.put("bargein", bargein);
			obj.put("duree_max", dureeMaxEnreg);
			obj.put("duree_final_silence", dureeMaxSilence);
			obj.put("final_dtmf", finalDtmf);
			obj.put("repertoire_texte", repTexte);
			obj.put("repertoire_audio", repAudio);
			obj.put("repertoire_audio_date", boolSousRepAudio);
			obj.put("nom_audio", nomAudio);

			obj.put("prompt", reconstituerListePrompt());
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
			
		} catch (JSONException e) {
			logger.error("toJsonObject - "+e.getMessage());
		}
		
		return obj;
	}

}
