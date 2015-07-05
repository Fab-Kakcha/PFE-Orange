<?xml version="1.0" encoding="UTF-8"?>

<%@page import="java.util.Arrays"%>
<%@page import="java.util.ArrayList"%>
<%@ page import="orange.olps.svi.client.Client"%>
<%@ page import="orange.olps.svi.navigation.Navigation"%>
<%@ page import="orange.olps.svi.navigation.NavigationManager"%>
<%@ page import="orange.olps.svi.util.Util" %>
<%@ page import="orange.olps.svi.stats.StatManager"%>
<%@ page import="orange.olps.svi.config.Config"%>
<jsp:useBean id="client" class="orange.olps.svi.client.Client" scope="session"/>

<%
    String promptSilence = Config.getInstance().getProperty(Config.PROMPT_SILENCE,"");
    Navigation nav =NavigationManager.getInstance().getNavigation(client.getService(), client.getNavCourante());

	boolean silence = false;
	if (client.isSilenceService() && client.isSilenceDemande() && ! "".equals(promptSilence) ){
	    // on doit jouer un prompt blancs pour permettre au client de porter son telephone a l'oreille
	    client.setSilenceDemande(false); // action de silence consommée
	    silence = true;
	}
	ArrayList<String> tabPrompt = client.getPrompt();
	
	
%>
<vxml xml:lang="fr" xmlns="http://www.w3.org/2001/vxml" version="2.0" application="common/root.jsp">
	<form>
	   <var name="resultScenario" expr="'NORMAL'" />
		<!-- properties management -->
		<property name="timeout" value="0s"/>

		<!-- events management -->
		<catch event="connection.disconnect.hangup">			
		
			<submit next="dialogs/disconnect.jsp" />
		</catch>

   		<field name="grammarResult">
   	
			<!-- prompts add -->
			<prompt bargein="<%=nav.isBargein()%>">
			<% 
               
                if (silence){
                    // on doit jouer un prompt blancs pour permettre au client de porter son telephone a l'oreille
                    client.setSilenceDemande(false); // action de silence consommée
             %>
                
                <audio src="prompts/<%=promptSilence%>.wav">
                </audio>
             <%}

             for (String prompt :  tabPrompt) {
                  StatManager.getInstance().posterStatistiques(client.getIdent(), prompt, System.currentTimeMillis(), StatManager.PROMPT);
                  
              %>
                
                <audio src="prompts/<%=prompt%>.wav">
                </audio>
              <%}%>
			</prompt>
		 
			<!-- grammars add -->
   		 	<grammar mode="dtmf" src="grammars/gramGenerale.grxml" type="application/srgs+xml"/>
	
			<!-- no input management-->
			<noinput>				
				<var name="etat" expr="'noinput'"/>
                <submit next="VxmlService" namelist="etat"/>
			</noinput>

			<!-- no match management -->
			<nomatch> 
				<var name="etat" expr="'nomatch'"/>
                <submit next="VxmlService" namelist="etat"/>
			</nomatch>
		  
			<filled> 
                <assign name="grammarResult" expr="application.lastresult$[0].utterance"/>
                <var name="etat" expr="'filled'"/>
                <submit next="VxmlService" namelist="etat grammarResult" />
			</filled>
		</field>

	</form>
</vxml>