<?xml version="1.0" encoding="UTF-8"?>

<%@page import="java.util.Arrays"%>
<%@page import="java.util.ArrayList"%>
<%@ page import="orange.olps.svi.client.Client"%>
<%@ page import="orange.olps.svi.util.Util" %>
<%@ page import="orange.olps.svi.stats.StatManager"%>
<%@ page import="orange.olps.svi.config.Config"%>
<jsp:useBean id="client" class="orange.olps.svi.client.Client" scope="session"/>

<%
    String promptSilence = Config.getInstance().getProperty(Config.PROMPT_SILENCE,"");

	boolean silence = false;
	if (client.isSilenceService() && client.isSilenceDemande() && ! "".equals(promptSilence) ){
	    // on doit jouer un prompt blancs pour permettre au client de porter son telephone a l'oreille
	    client.setSilenceDemande(false); // action de silence consommée
	    silence = true;
	}
	ArrayList<String> tabPrompt = client.getPrompt();
	for (String prompt :  tabPrompt) {
         StatManager.getInstance().posterStatistiques(client.getIdent(), prompt, System.currentTimeMillis(), StatManager.PROMPT);
	}
	//on envoie la stat de duree
    client.setValeur(Client.VAR_DUREE, Util.getDuree(client.getTopDepart()));
    //envoie de la statistique de raccroche
    StatManager.getInstance().posterStatistiques(client.getIdent(), "OVP", System.currentTimeMillis(), StatManager.HANGUP);
%>
<vxml xml:lang="fr" xmlns="http://www.w3.org/2001/vxml" version="2.0" application="common/root.jsp">
	<form>
	   <var name="resultScenario" expr="'NORMAL'" />
		<!-- properties management -->
		<property name="timeout" value="<%=Config.getInstance().getProperty(Config.APPLI_TEMPO_DCNX, "1s")%>"/>

   		<field name="grammarResult">
   	
			<!-- prompts add -->
			<prompt bargein="false">
			<% 
               
                if (silence){
                    // on doit jouer un prompt blancs pour permettre au client de porter son telephone a l'oreille
                    client.setSilenceDemande(false); // action de silence consommée
             %>
                
                <audio src="prompts/<%=promptSilence%>.wav">
                </audio>
             <%}

             for (String prompt :  tabPrompt) {
                                  
              %>
                
                <audio src="prompts/<%=prompt%>.wav">
                </audio>
              <%}%>
			</prompt>
		 
			<!-- grammars add -->
   		 	<grammar mode="dtmf" src="grammars/gramGenerale.grxml" type="application/srgs+xml"/>
	
			<!-- no input management-->
			<noinput>				
				<exit namelist="resultScenario"/>
			</noinput>

			<!-- no match management -->
			<nomatch> 
				<exit namelist="resultScenario"/>
			</nomatch>
		  
			<filled> 
				<exit namelist="resultScenario"/>
			</filled>
		</field>

	</form>
</vxml>