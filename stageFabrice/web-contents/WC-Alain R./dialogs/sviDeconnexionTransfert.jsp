<?xml version="1.0" encoding="UTF-8"?>
<%@ page import="orange.olps.svi.config.Config" %>
<%@ page import="orange.olps.svi.client.Client" %>
<%@ page import="orange.olps.svi.client.ClientFormat" %>
<%@ page import="orange.olps.svi.navigation.NavigationManager" %>
<%@ page import="orange.olps.svi.navigation.Transfert" %>
<%@ page import="orange.olps.svi.util.Util" %>
<%@ page import="orange.olps.svi.stats.StatManager" %>


<jsp:useBean id="client" class="orange.olps.svi.client.Client" scope="session"/>

<vxml xml:lang="fr" xmlns="http://www.w3.org/2001/vxml" version="2.0">

<%
    // creation de la chaine de transfert
    // client.navCourante contient le label de l'item de retour dans le SVI
    // client.navPrecedente contient le label de l'item de TRANSFERT 
    Transfert nav= (Transfert) NavigationManager.getInstance().getNavigation(client.getService(), client.getNavPrecedente());
	String numTransfert=nav.getNumeroTransfertAvecParam(client);
	/* on met à jour la duree dans le SVI */
	client.setValeur(Client.VAR_DUREE, Util.getDuree(client.getTopDepart()));

  ClientFormat f = new ClientFormat(client);
  f.formaterBrut();
  // ajout de l'adresse IP pour envoi du hangup
  String c = f.toString()+"_varIP:"+Config.getInstance().getProperty(Config.STATS_ADR_AS);
%>
<var name="resultScenario" expr="'TRANSFERT'" />
<var name="numTransfert" expr="'<%=numTransfert%>'" />
<var name="rappelSvi" expr="'<%=client.getNavCourante()%>'" />
<var name="client" expr="'<%=c %>'"/>

<form>
<block>
<exit namelist="resultScenario numTransfert rappelSvi client"/>
</block>
</form>
</vxml>