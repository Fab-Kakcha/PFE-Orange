package orange.olps.svi.navigation;

import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import orange.olps.svi.client.Client;
import orange.olps.svi.stats.StatManager;


public class Saisie extends Menu {
	
	private static Pattern patGrammaire = Pattern.compile("GRMR([^_]+)");
	protected static final String VALEUR = "valeur";

	protected static final String VALIDATION = "validation";
	protected static final String CONTROLE = "controle";
	protected static final String PROMPT_VOCALISATION = "prompt.vocalisation";

	private static final String LONGUEUR = "longueur";

	/**
	 * variable contenant l'attribut du client a modifier sur saisie valide
	 */
	private String nomChamp = "";
	/**
	 * Sequence de caracteres permettant de determiner la fin de saisie
	 */
	private String validation = "";
	/**
	 * Tableau des regExp a passer pour verifier la saisie
	 */
	private ArrayList<Pattern> tabControle;

	private boolean boolStat = true;

	private int longueur = -1;
	
	/**
	 * 
	 * @param lab
	 * @param svc
	 */
	public Saisie(String lab, String svc) {
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
		String s;
				
		this.setAction(ERREUR,lstProp.getProperty(racinePropriete+ERREUR_SUIVANT, "").trim());
							
		// champ de Client à mettre à jour
		s = lstProp.getProperty(racinePropriete+VALEUR,"").trim();
		if (!"".equals(s)) {
			setNomChamp(s);
		}
		validation = lstProp.getProperty(racinePropriete+VALIDATION,"#").trim();
		// longueur de saisie
		s = lstProp.getProperty(racinePropriete+LONGUEUR,"").trim();
		if (!"".equals(s)) {
			setLongueur(s);
		}
		// controle de saisie
		s = lstProp.getProperty(racinePropriete+CONTROLE, "").trim();
		if (!"".equals(s)) {
			/* les parametres sont stockes sous la forme expReg1=prompt1#expReg2=prompt2,... */
			/* 1- on decoupe en fonction des virgules */
			String[] tabParam = s.split("#");
			String[] tabCleVal;
			
			Pattern pat;
			Info inf = new Info (label, service);
			StringBuffer buf = new StringBuffer (); // constitution d'une liste de prompt
			tabControle = new ArrayList<Pattern>(1);
			for (String param :  tabParam) {
				/* 2 - on decoupe cle / valeur */
				tabCleVal = param.trim().split("=");
				pat = Pattern.compile(tabCleVal[0]);
				tabControle.add(pat);
				if (buf.length() > 0) {
					buf.append(',');
				}
				buf.append(tabCleVal[1].trim());
				
			}
			inf.setPrompt(buf.toString().split(","));
			this.setErreur(inf);
		}
		boolStat  = Boolean.parseBoolean(lstProp.getProperty(racinePropriete+STATISTIQUES,"true").trim());
		return ret;
	}
	
	private void setLongueur(String s) {
		try {
			longueur = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			longueur=-1;
		}
		
	}
	public String getActionErreur() {	
		logger.debug("getActionEchec - Entree ");
		String s = mapAction.get(ERREUR);
		if (s==null) {
			return "";
		}			
		return s;
	}

	public String getNomChamp() {
		return nomChamp;
	}
	public void setNomChamp(String nomChamp) {
		this.nomChamp = nomChamp;
	}
	public String getValidation() {
		return validation;
	}
	public void setValidation(String validation) {
		this.validation = validation;
	}

	@Override
	public void calculerActionNavigation(Client client) {
		int actionNav = client.getActionNavigation();
		String saisie = client.getSaisie();
		client.setIndex(-1);
		logger.debug("calculerActionNavigation - ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+") action precedente="+actionNav+" saisie="+saisie);
		
		if (actionNav == Navigation.RIEN) {
			// premier passage dans cet item
			// Statistiques
			StatManager.getInstance().posterStatistiques(client.getIdent(), 
					label, 
					System.currentTimeMillis(),
					StatManager.NAVIGATION);
			
			if (!client.getNavCourante().equals(client.getNavPrecedente())){
				// premier passage
				client.setNbInactivite(0);
				client.setNbRejet(0);
			}
		}
		if (Navigation.RIEN == actionNav && "".equals(saisie)) {
			// premier passage dans cet item de navigation
			// il faut lire les prompts du menu
			preparerPrompt(client);
			if (isPromptManquant(client.getLangue())) {
				client.setActionNavigation(DIFFUSION);
			}
			else {
				client.setActionNavigation(MENU_SAISIE);
			}
		}
		else if (Navigation.DIFFUSION == actionNav) {
			// on vient de diffuser le prompt manquant
			client.setSaisie("");
			client.setNavCourante(client.getNavPrecedente());
			client.setActionNavigation(Navigation.RIEN);
		}
		else if (!"".equals(saisie)) {
			
			if (client.getNbSaisieAjoute() > 0) {
				// des caracteres ont bien ete saisis depuis le dernier passage dans ce code
			
				if (saisie.endsWith(validation) || (longueur > 0 && saisie.length() >= longueur)) {
					// la saisie est terminee
					client.setSilenceDemande(true);
					
					// on enleve les caracteres de validation					
					if (saisie.endsWith(validation)) saisie = saisie.substring(0,saisie.length()-validation.length());
					logger.debug("calculerActionNavigation - saisie valide client "+saisie);
					// verification de la saisie
					if (verifierSaisie(saisie, client)) {
						// sauvegarde de la saisie
						client.setValeur(nomChamp, saisie,boolStat);
										
						logger.debug("calculerActionNavigation - saisie valide systeme "+saisie);
						client.setActionNavigation(Navigation.RIEN);
						client.setNavPrecedente(label);
						client.setNavCourante(getSuivant());
						
						client.resetSaisie();
						client.setNbInactivite(0);
						client.setNbRejet(0);
					}
					else {
						// erreur de saisie
						logger.debug("calculerActionNavigation - erreur saisie ("+saisie+") ");
						client.resetSaisie();
						client.setNavPrecedente(label);
						client.setNavCourante(Navigation.REJET);
						client.setActionNavigation(Navigation.RIEN);
						
					}
				}
				else {
					// saisie non validee 
					// on regarde si un digit saisi ne correspont pas a une action de navigation
					int i;
					String nav =null;
					for (i= saisie.length() - client.getNbSaisieAjoute();
						 i < saisie.length();
						 i++) {
						nav = getAction(saisie.substring(i, i+1));					
						if (nav != null && !REJET.equals(nav)) {
							break;
						}
						else nav = null;
					}
					if (nav == null) {
						// Pas de navigation associee a une des dtmf saisie
						logger.debug("calculerActionNavigation - ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+") poursuite saisie  "+saisie);
						client.setActionNavigation(SAISIE_DTMF);
						client.setNbSaisieAjoute(0);
					}
					else {
						
						// une touche de navigation a ete activee, arret de saisie
						client.setSilenceDemande(true);
						client.setActionNavigation(Navigation.RIEN);
						client.setNavPrecedente(label);
						client.setNavCourante(nav);					
						client.resetSaisie(i+1); 
					}
				}
			}
			else {
				// pas de saisie de dtmf supplementaires depuis le dernier traitement
				// =>Inactivite
				client.resetSaisie();
				client.setSilenceDemande(true);
				client.setActionNavigation(MENU_SAISIE);
			}
			
		}
		else if (actionNav == Navigation.DIFFUSION_REJET) {
			// on a diffuse un message d'erreur
			// il faut relire les prompts
			logger.debug("calculerActionNavigation - ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+") Retour REJET ");
			if (isPromptUnParUn()) {
				client.setPrompt(client.getValeur(varUnParUn));
			}
			else {
				preparerPrompt(client);
			}
			client.resetSaisie();
			client.setActionNavigation(MENU_SAISIE);
		}
		else {
			// c'est de l'inactivte on rejoue le menu 
			logger.debug("calculerActionNavigation - ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+") RAF "+saisie);
			client.resetSaisie();
			client.setActionNavigation(MENU_SAISIE);
		}
		logger.debug("calculerActionNavigation - ("+label+") - ("+client.getValeur(Client.VAR_IDENT)+") retour = "+client.getActionNavigation());
		return;
	}
	private boolean verifierSaisie(String saisie, Client client) {
		if (tabControle == null) return true;
		if (tabControle.isEmpty()) return true;
		int i = 0;
		Matcher mat;
		for (Pattern pat : tabControle) {
			if ("GRMR".equals(pat.pattern())) {
				// le pattern de controle est dans le prompt
				String p = client.getValeur(varUnParUn); // dernier prompt joué en mode PREPARE
				if (p == null) continue;
				Matcher m = patGrammaire.matcher(p);
				if (m.find()) {
					pat = Pattern.compile("["+m.group(1)+"]+");
				}
				else continue;
			}
			mat = pat.matcher(saisie);
			if (!mat.find()) {
				client.setIndex(i);
				return false;
			}
			i++;
		}
		if (longueur > 0 && saisie.length() != longueur) return false;
		return true;
	}
	public JSONObject toJsonObject() {
		JSONObject obj = super.toJsonObject();
		try {
			obj.put("type", "saisie");
			obj.put("valeur", nomChamp);
			obj.put("validation", validation);
			obj.put("controle", validation);
			obj.put("statistiques", boolStat);
			obj.put("longueur", longueur);
			String[] tabPrompt = getErreur().reconstituerListePrompt().split(",");
			StringBuffer buf = new StringBuffer();
			int i = 0;
			for (Pattern r : tabControle) {
				if (buf.length() > 0) buf.append('#');
				buf.append(r.pattern());
				buf.append('=');
				buf.append(tabPrompt[i]);
				i++;
			}
			
		} catch (JSONException e) {
			logger.error("toJsonObject - "+e.getMessage());
		}
		
		return obj;
	}


}
