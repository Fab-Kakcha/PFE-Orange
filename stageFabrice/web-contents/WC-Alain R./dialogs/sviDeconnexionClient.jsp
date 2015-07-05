<?xml version="1.0" encoding="UTF-8"?>
<%@ page import="orange.olps.svi.stats.StatManager" %>
<%@ page import="orange.olps.svi.client.Client"%>
<%@ page import="orange.olps.svi.util.Util" %>

<jsp:useBean id="client" class="orange.olps.svi.client.Client" scope="session"/>

<%
//on envoie la stat de duree
client.setValeur(Client.VAR_DUREE, Util.getDuree(client.getTopDepart()));

%>
<vxml xml:lang="fr" xmlns="http://www.w3.org/2001/vxml" version="2.0">
<var name="resultScenario" expr="'NORMAL'" />
<form>
<block>
<exit namelist="resultScenario "/>
</block>
</form>
</vxml>
