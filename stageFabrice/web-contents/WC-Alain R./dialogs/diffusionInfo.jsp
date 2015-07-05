<?xml version="1.0" encoding="UTF-8"?>

<%@page import="java.util.ArrayList"%>
<%@page import="orange.olps.svi.navigation.NavigationManager"%>
<%@ page import="orange.olps.svi.client.Client"%>
<%@ page import="orange.olps.svi.util.Util" %>
<%@ page import="orange.olps.svi.navigation.Navigation"%>
<%@ page import="orange.olps.svi.stats.StatManager"%>
<%@ page import="orange.olps.svi.config.Config"%>
<jsp:useBean id="client" class="orange.olps.svi.client.Client" scope="session"/>

<%
Navigation nav = NavigationManager.getInstance().getNavigation(client.getService(), client.getNavCourante());
String promptSilence = Config.getInstance().getProperty(Config.PROMPT_SILENCE,"");
ArrayList<String> tabPrompt = client.getPrompt();
boolean silence = false;

if (client.isSilenceService() && client.isSilenceDemande() && ! "".equals(promptSilence) ){
    // on doit jouer un prompt blancs pour permettre au client de porter son telephone a l'oreille
    client.setSilenceDemande(false); // action de silence consommée
    silence = true;
}

%>

<vxml xml:lang="fr" xmlns="http://www.w3.org/2001/vxml" version="2.0" application="common/root.jsp">
	<form>
                <!-- properties  -->
		<property name='audiomaxage' value='<%=nav.getAudioMaxage()%>'/>
		<property name='interdigittimeout' value='<%=Config.getInstance().getProperty("vxml.interdigittimeout","500ms")%>'/>
		<property name="timeout" value="<%=nav.getTimeout()%>"/>

		<!-- events management -->
		<catch event="connection.disconnect.hangup">				
			<submit next="dialogs/disconnect.jsp" />
		</catch>
       
   		<field name="grammarResult">
   	
			<!-- prompts  -->
			<prompt bargein="<%=nav.isBargein()%>">
			<% if (silence) {
			%>				
				<audio src="prompts/<%=promptSilence%>.wav"></audio>
			<%} 
			 for (String prompt :  tabPrompt) {
					    // on trace les prompts  que si ce n'est pas un chiffre
						if (!prompt.matches("^[0-9]+.*$")) {
							StatManager.getInstance().posterStatistiques(client.getIdent(), prompt, System.currentTimeMillis(), StatManager.PROMPT);
						}	
				%>
						<audio src="prompts/<%=prompt%>.wav"></audio>
			<%} // fin de for
			%>
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
				<submit next="VxmlService" namelist="etat" method="post"/>
			</nomatch>
		  
			<filled> 
			    <assign name="grammarResult" expr="application.lastresult$[0].utterance"/>
				<var name="etat" expr="'filled'"/>
				<submit next="VxmlService" namelist="etat grammarResult" />
			</filled>
		</field>

	</form>
</vxml>