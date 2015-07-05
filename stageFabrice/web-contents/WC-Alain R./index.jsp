<?xml version = "1.0" encoding="UTF-8"?>
<vxml xml:lang="fr" xmlns="http://www.w3.org/2001/vxml" version="2.0" application="common/root.jsp">
<%
response.setHeader("Pragma", "No-cache");
response.setDateHeader("Expires", 0);
response.setHeader("Cache-Control","no-cache");

%>
<script> <![CDATA[ 
       function determinerService(svcParam, svcSession, numDestination) 
       {
         if (svcParam != null && svcParam != 'null') return svcParam;
		 if (svcSession.svi != undefined) return svcSession.svi;
		 return numDestination;
       } 
       function ajusterId(oms, callingNumber) {
    	   if (oms == null || oms == "null") return callingNumber;
    	   return oms;
       }
    ]]> </script> 
	
	<form>
		<block>

			<var name="callingNumber" expr="session.connection.remote.uri"></var>
			
			<% if (request.getParameter("numDestination") == null) { %>
			<var name="calledNumber" expr="session.connection.ccxml.values.numDestination"></var>
			<% }
			 else {%>
			 <var name="calledNumber" expr="'<%=request.getParameter("numDestination")%>'"></var>
			<% }%>
			<var name="client" expr="session.connection.ccxml.values.client"></var>
			
			<% if (request.getParameter("navigation") == null) { %>
			<var name="navigation" expr="session.connection.ccxml.values.navigation"></var>
			<% }
			 else {%>
			 <var name="navigation" expr="'<%=request.getParameter("navigation")%>'"></var>
			<% }%>	
			
			<% if (request.getParameter("oms") == null) { %>
				<var name="oms" expr="session.connection.ccxml.values.oms"></var>
			<% }
			 else {%>
				<var name="oms" expr="'<%=request.getParameter("oms")%>'"></var>
			<% }%>
			<var name="etat" expr="'init'"></var>
			<var name="conid" expr="ajusterId(oms, callingNumber) + session.connection.connectionid"></var>			
			
			<submit next="VxmlService" namelist="etat callingNumber calledNumber conid navigation client" method="post"></submit>
		</block>
	</form>
</vxml>
