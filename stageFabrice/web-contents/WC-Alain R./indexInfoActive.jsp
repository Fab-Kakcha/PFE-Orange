<?xml version = "1.0" encoding="UTF-8"?>
<vxml xml:lang="fr" xmlns="http://www.w3.org/2001/vxml" version="2.0" application="common/root.jsp">
<%
response.setHeader("Pragma", "No-cache");
response.setDateHeader("Expires", 0);
response.setHeader("Cache-Control","no-cache");

%>
	<form>
	  <block>
		 <var name="callingNumber" expr="session.connection.ccxml.values.OMS_ANI"/>
         <var name="calledNumber" expr="session.connection.ccxml.values.OMS_DNIS"/>
         <var name="userLangue" expr="session.connection.ccxml.values.OMS_LANG"/>
         <var name="userTypologie" expr="session.connection.ccxml.values.OMS_SEGMENT"/>

		<var name="conid" expr="callingNumber + session.connection.connectionid"/>
 		<var name="client" expr="'_varSvc:InfoActive_varIdent:'+conid+'_varNumAppelant:'+callingNumber+'_varNumAppele:'+calledNumber+'_varLangue:CHANGE('+userLangue+')_varSegment:CHANGE('+userTypologie+')'"/>			
		<var name="navigation" expr="'null'"/>
					
		<var name="etat" expr="'init'"/>		
			
		<submit next="VxmlService" namelist="etat callingNumber calledNumber conid navigation client" method="post"></submit>
	  </block>
	</form>
</vxml>
