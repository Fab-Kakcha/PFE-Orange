<?xml version="1.0" encoding="UTF-8"?>

<%@ page import="orange.olps.svi.config.Config" %>
<%@ page import="orange.olps.svi.stats.StatManager" %>
<%@ page import="orange.olps.svi.client.Client" %>
<%@ page import="orange.olps.svi.util.Util" %>
<%@ page import="orange.olps.svi.navigation.Deconnexion" %>
<%@ page import="orange.olps.svi.navigation.Navigation" %>
<%@ page import="orange.olps.svi.navigation.NavigationManager" %>


<jsp:useBean id="client" class="orange.olps.svi.client.Client" scope="session"/>

<vxml xml:lang="fr" xmlns="http://www.w3.org/2001/vxml" version="2.0" application="common/root.jsp">

<% 
	
	//on envoie la stat de duree
	if (client.getValeur(Client.VAR_DUREE) == null) {
		// la durée n'a pas déjà été calculée
	    client.setValeur(Client.VAR_DUREE, Util.getDuree(client.getTopDepart()));
	}
    String retour = "NORMAL";
    Navigation nav= NavigationManager.getInstance().getNavigation(client.getService(), client.getNavCourante());
    if (nav != null) {
        if (nav.getClass().getName().equals(Deconnexion.class.getName())) {
    	   retour = ((Deconnexion) nav).getValeurRetour();
    	}        
    }
   // envoie de la statistique de raccroche
   StatManager.getInstance().posterStatistiques(client.getIdent(), "OVP", System.currentTimeMillis(), StatManager.HANGUP);
    
%>

<var name="resultScenario" expr="'<%=retour%>'" />
<form>
<block>
<exit namelist="resultScenario"/>
</block>
</form>
  

</vxml>

