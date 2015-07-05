<?xml version = "1.0" encoding="UTF-8"?>
<vxml xml:lang="fr" xmlns="http://www.w3.org/2001/vxml" version="2.0" application="common/root.jsp">

<%
// Ce vxml est appele en NETANN par GENESYS pour démarrer l enregistrement 
// du message du client qui n a pas eu un teleconseiller
response.setHeader("Pragma", "No-cache");
response.setDateHeader("Expires", 0);
response.setHeader("Cache-Control","no-cache");
%>
	<form>
		<block>
			<var name="currentState" expr="''"></var>
			<var name="toState" expr="'InitDial'"></var>
			<var name="callReason" expr="'normal'"></var>
			<var name="callingNumber" expr="session.connection.ccxml.values.OMS_ANI"></var>
			<var name="calledNumber" expr="session.connection.ccxml.values.OMS_DNIS"></var>

			<var name="client" expr="'_varSvc:'+session.connection.ccxml.values.OMS_DNIS+'_varIdent:'+session.connection.ccxml.values.OMS_ID+'_varNumAppelant:'+session.connection.ccxml.values.OMS_ANI+'_varNumAppele:'+session.connection.ccxml.values.OMS_DNIS+'_varLangue:'+session.connection.ccxml.values.OMS_LG+'_varDateDeb:'+session.connection.ccxml.values.OMS_DD+'_varNumClient:'+session.connection.ccxml.values.OMS_NUM+'_varMetier:'+session.connection.ccxml.values.OMS_FLUX"></var>

			<var name="navigation" expr="session.connection.ccxml.values.OMS_NAVIGATION"></var>  		
    		<var name="conid" expr="session.connection.ccxml.values.OMS_ID"></var>	

			<var name="etat" expr="'init'"></var>                 
            
            <submit next="/Svi/VxmlService" namelist="etat callingNumber calledNumber conid navigation client" method="post"></submit>

		</block>
	</form>
</vxml>
