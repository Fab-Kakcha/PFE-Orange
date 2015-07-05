<?xml version="1.0" encoding="UTF-8"?>

<%@page import="java.util.ArrayList"%>
<%@ page import="orange.olps.svi.util.Util" %>
<%@ page import="orange.olps.svi.client.Client" %>
<%@ page import="orange.olps.svi.config.Config" %>
<%@ page import="orange.olps.svi.navigation.NavigationManager" %>
<%@ page import="orange.olps.svi.navigation.Enregistrement" %>


<jsp:useBean id="client" class="orange.olps.svi.client.Client" scope="session"/>


<vxml xml:lang="fr" xmlns="http://www.w3.org/2001/vxml" version="2.0">

<%
	
	Enregistrement enreg = (Enregistrement) NavigationManager.getInstance().getNavigation(client.getService(), client.getNavCourante());
	
	ArrayList<String> tabPrompt =  null;
	if (enreg.isPrompt()) {
		tabPrompt = enreg.getPrompt(client.getLangue());
		if (enreg.isPromptDynamique()) {
		    // il y a des prompts dynamiques , il faut retravailler la liste
		    tabPrompt = Util.reconstituerListePrompt(tabPrompt, client);		   
		}
	}
	boolean isDtmf = false;
	if (!"".equals(enreg.getFinalDtmf())) {
		isDtmf = true;
	}
%>

<var name="toState" expr="''"></var>
<var name="currentState"  expr="'EnregistrementAudio'"></var>
<var name="callReason"  expr="'normal'"></var>

<%
	if (isDtmf) {
		// dtmf de fin d'enreg
%>
	<property name="termchar" value="<%=enreg.getFinalDtmf()%>"/>
<%} %>

<form id ="record">

	<record name="rec" maxtime="<%=enreg.getDureeMaxEnreg()%>" beep="true" finalsilence="<%=enreg.getDureeMaxSilence()%>" dtmfterm="<%=isDtmf%>" type="audio/x-wav">
			
		<% if (tabPrompt != null) { %>
			<prompt>
				<% for (String prompt :  tabPrompt) {%>
				    <audio expr="'prompts/<%=prompt+".wav"%>'" />
				<% } %> 
			</prompt>
		<% }  %> 
		<noinput>				
				<var name="etat" expr="'FinEnregAudio'"/>
				<submit next="VxmlServlet" namelist="etat" method="post"></submit>
		</noinput>
	</record>
							
		<filled> 

				<var name="etat"  expr="'FinEnregAudio'"></var>
				<submit next="RecordServletSvi" namelist="etat rec" method="post" enctype="multipart/form-data"></submit>			
		</filled>
</form>
	<catch event="connection.disconnect.hangup">
		<log expr="'connection.disconnect.hangup :' + _event" />
		<if cond="rec$.duration == 0">			
	
			<submit next="dialogs/DeconnexionClient.jsp" ></submit>
		<else />
			<var name="etat" expr="'EnregDeconnexion'"/>
			<submit next="RecordServletSvi" namelist="etat rec" method="post" enctype="multipart/form-data"></submit>
		</if>
		
	</catch>

</vxml>

