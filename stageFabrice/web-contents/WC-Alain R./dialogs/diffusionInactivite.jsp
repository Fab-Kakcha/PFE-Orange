<?xml version="1.0" encoding="UTF-8"?>

<%@page import="java.util.Arrays"%>
<%@page import="java.util.ArrayList"%>
<%@ page import="orange.olps.svi.client.Client"%>
<%@ page import="orange.olps.svi.navigation.Menu"%>
<%@ page import="orange.olps.svi.navigation.NavigationManager"%>
<%@ page import="orange.olps.svi.util.Util" %>
<%@ page import="orange.olps.svi.stats.StatManager"%>
<%@ page import="orange.olps.svi.config.Config"%>
<jsp:useBean id="client" class="orange.olps.svi.client.Client" scope="session"/>

<%
   
    Menu menu = (Menu)NavigationManager.getInstance().getNavigation(client.getService(), client.getNavCourante());
	
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
			<prompt bargein="<%=menu.isBargein()%>">
			<%

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
                <submit next="VxmlService" namelist="etat" method="post"/>
			</noinput>

			<!-- no match management -->
			<nomatch> 
				<var name="etat" expr="'nomatch'"/>
                <submit next="VxmlService" namelist="etat" method="post" />
			</nomatch>
		  
			<filled> 
				<var name="etat" expr="'filled'"/>
                <submit next="VxmlService" namelist="etat" method="post"/>
			</filled>
		</field>

	</form>
</vxml>