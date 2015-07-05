<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE ccxml PUBLIC "CCXML/1.0/DTD" "ccxml.dtd" >
<ccxml version="1.0" >

<%@ page import="orange.olps.svi.config.Config" %>
<%@ page import="orange.olps.svi.util.Util" %>
<% 
	String adrOms = request.getParameter("IPADDR");
	// l'adresse IP ser a constituer l'identifiant de l'appel (stats)
	// on enleve les "."
   if (adrOms != null) adrOms = adrOms.replaceAll("[.]","");
   
   	// pattern de recuperation du numero appele dans le numero local
	String patternAppele = Config.getInstance().getProperty (Config.PATTERN_NUM_APPELE);
	
   // Svi a jouer pour le parametrage de l IPBX: *21, ...
   String[] tmp = Config.getInstance().getProperty("ccxml.svi.navigation.ipbx.param", "Ipbx,parametrage").split(",");
   String sviIpbxParam = new String(tmp[0]);
   String navIpbxParam = new String(tmp[1]);
  
   String patternIpbxAppel = Config.getInstance().getProperty ("ipbx.pattern.appel", "");
   String patternIpbxParam = Config.getInstance().getProperty ("ipbx.pattern.param", "star([0-9]+)");

   // table correspondance numero transmis /service client 
   String tabNumeroTransmis = Config.getInstance().getProperty (Config.APPLI_SERVICE_AUT_TRANSMIS);
   %>

<!--
<description :>
- appel entrant
- appel d'un vxml menu
- ccxml appelle le distant
- appel d'un vxml sur echec du transfert d'appel
-->
<!--
<javascript :>
-->
<jsp:include page="/js/commun_js.jsp" flush="true" />
<jsp:include page="/js/client_js.jsp" flush="true" />
<!--
<variables :>
-->
	<!-- customization -->
	<var name="service" expr="'Svi'"/>
	<var name="urlVXMLscenario" expr="'/Svi/index.jsp'" />

	<var name="navigation" expr="'null'" /> <!-- page de navigation de demarrage -->
	
	<var name="client" expr="'null'" /> <!--donnees client issue du vxml -->

	<!-- does not change  -->
	<var name="idConCCXML" />					<!-- id session CCXML -->
	<var name="dialogIdVXML" expr="'null'"/>					<!-- id dialog -->
	<var name="inConnectionId" expr="'null'" />					<!-- number to call -->
	
	<var name="callerId" />						<!-- caller -->
	<var name="numDestination" expr="'null'" /> <!--  numero du service/appel sortant -->	

	<var name="anError" />						<!-- anError -->

	<!-- Set an initial state -->
	<var name="currentState" expr="'init'" />
	<var name="oms" expr="'<%=adrOms%>'" />

<!--
<main :>
-->
	<eventprocessor statevariable="currentState">

		<!-- ********************************************** -->
		<!-- ****** currentState : init              ****** -->
		<!-- ********************************************** -->

		<!-- Loading CCXML -->
		<transition state="init" event="ccxml.loaded" name="evt">
			<assign name="idConCCXML" expr="evt.sessionid" />
			<log expr="logMsg(evt,currentState,'Loading CCXML, Incoming call waiting ...')" />
			<assign name="currentState" expr="'waiting'" />
		</transition>

		<!-- ********************************************** -->
		<!-- ****** currentState : waiting           ****** -->
		<!-- ********************************************** -->

		<!-- Incoming call -->
		<transition state="waiting" event="connection.alerting" name="evt">
			<assign name="inConnectionId" expr="evt.connectionid" />
			<assign name="callerId" expr="extraireCallerId(evt.connection.remote)" />
			<log expr="logMsg(evt,currentState,'Incoming call')" />
			<var name="numeroAppele" expr="extraireNumeroService(evt.connection.local, '<%=patternAppele%>', '<%=tabNumeroTransmis%>')" />
			<log expr="logMsg(evt,currentState,'numero appele : '+numeroAppele)" />

			<if cond="numeroAppele == 'false'">
				<log expr="logErr(evt,currentState,'numero appele : NON TRAITE')" />
				<reject/>
			<else />
				
				<!-- verifie si le numero appele est une commande IPBX  --> 
				<assign name="numDestination" expr="isNumeroVerifiePattern(numeroAppele, '<%=patternIpbxParam%>')" />
								
				<if cond="numDestination == 'false'"> 
					<log expr="logErr(evt,currentState,'numero appele : NON TRAITE')" />
					<reject/>
				<else />
					<!-- Commande IPBX -->
					<log expr="logMsg(evt,currentState,'Commande IPBX: '+numDestination)" />
					<assign name="client" expr="setService(client, '<%=sviIpbxParam%>')" />
					<log expr="logMsg(evt,currentState,'Client: '+client)" />
					<assign name="client" expr="setNumAppele(client, numDestination)" />
					<!-- on extrait le poste du numero appelant -->
					<assign name="client" expr="setNumAppelant(client, isNumeroVerifiePattern(extraireSipUser(callerId),'<%=patternIpbxAppel%>'))" />
					<assign name="navigation" expr="'<%=navIpbxParam%>'"/>											
					<assign name="currentState" expr="'initVXML'" />
					<accept />									
				</if>
			</if>
		</transition>
	  <!-- Error during accept call -->
		<transition state="waiting" event="connection.accept.failed" name="evt">
			<log expr="logErr(evt,currentState+', '+ inConnectionId)" />
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	  </transition>
	  
	<!-- ********************************************** -->
	<!-- ****** currentState : initVXML          ****** -->
	<!-- ********************************************** -->
	<!-- Gestion d'erreur sur SIP Server -->
	<transition state="initVXML" event="connection.disconnected" name="evt">
		<assign name="anError" expr="'gateway_error'" />
		<log label="inConnectionId" expr="logErr(evt,currentState+', '+ inConnectionId + ', receive hangup from Gateway SIP')" />
		<assign name="currentState" expr="'finished'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>
	<transition state="initVXML" event="connection.accept.failed" name="evt">
		<assign name="anError" expr="'connection.accept.failed'" />
		<log label="inConnectionId" expr="logErr(evt,currentState+', '+ inConnectionId)" />
		<assign name="currentState" expr="'finished'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>
	<!-- VoiceXML Dialog start -->
	<transition state="initVXML" event="connection.connected" name="evt">
		<assign name="inConnectionId" expr="evt.connectionid" />
		<log expr="logMsg(evt,currentState,'Launching VoiceXML scenario')" />
		<assign name="currentState" expr="'scenarioConnected'" />
		<dialogstart src="urlVXMLscenario" namelist="numDestination navigation oms"  parameters="client"/>
	</transition>

	<!-- ********************************************** -->
	<!-- ****** currentState : scenarioConnected ****** -->
	<!-- ********************************************** -->

	<!-- Dialog VXML is active -->
	<transition state="scenarioConnected" event="dialog.started" name="evt">
		<assign name="dialogIdVXML" expr="evt.dialogid" />
		<log expr="logMsg(evt,currentState,'VoiceXML dialog started - dialogIdVXML=' + evt.dialogid)" />
	</transition>

	<!-- Dialog VXML failed -->
	<transition state="scenarioConnected" event="error.dialog.*" name="evt">
		<log expr="logErr(evt,'VXML_dialog_failed')" />
		<if cond="inConnectionId != 'null'">
			<disconnect connectionid="inConnectionId" />
		<else/>
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	</transition>

	<!-- Hangup during VoiceXML dialog -->
	<transition state="scenarioConnected" event="connection.disconnected" name="evt">
		<log expr="logMsg(evt,currentState,'reason='+evt.reason)" />
		<assign name="inConnectionId" expr="'null'" />
		<if cond="dialogIdVXML != 'null'">
			<assign name="currentState" expr="'waitingExit'" />
			<dialogterminate dialogid="dialogIdVXML" />
		<else />
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	</transition>

	<!-- Error during VoiceXML dialog -->
	<transition state="scenarioConnected" event="dialog.disconnect" name="evt">
		<log expr="logErr(evt,currentState+', Error during VoiceXML dialog')" />
		<if cond="inConnectionId != 'null'">
			<disconnect connectionid="inConnectionId" />
		<else/>
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	</transition>

	<!-- Exit VoiceXML Scenario -->
	<transition state="scenarioConnected" event="dialog.exit" name="evt">
		<assign name="dialogIdVXML" expr="'null'" />
		<log expr="logMsg(evt,currentState,'Fin de scenario')" />

		<log expr="logMsg(evt,currentState,'Fin du scenario')" />
		<if cond="inConnectionId != 'null'">
			<disconnect connectionid="inConnectionId" />
		<else/>
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	</transition>
	
	<!-- ********************************************** -->
	<!-- ****** currentState : waitingExit   ****** -->
	<!-- ********************************************** -->
	<!-- Le client a raccroché, attent fin VXML  -->
	<transition state="waitingExit" event="dialog.exit" name="evt">
		<log expr="logMsg(evt,currentState,'Fin...')" />
		<assign name="dialogIdVXML" expr="'null'" />
		<assign name="currentState" expr="'finished'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>



	<!-- ********************************************** -->
	<!-- ****** currentState : none              ****** -->
	<!-- ********************************************** -->

	<!-- Detect signal -->
	<transition event="connection.signal" name="evt">
		<log expr="logMsg(evt,currentState,'EVT= '+evt.info.content+' Value='+evt.info.value)" />
		<if cond="evt.info.type == 'dtmf'">
			<log expr="logMsg(evt,currentState,'DTMF '+evt.info.value+' catched')" />		
		</if>
	</transition>
	<transition event="send.successfull" name="evt">
		<log expr="logMsg(evt,currentState,'send OK')"/>
	</transition>
	<transition event="error.send.*" name="evt">
		<log expr="logErr(evt,currentState,'ERROR send ')" />
	</transition>
	

	<!-- ********************************************** -->
	<!-- ****** currentState : finished          ****** -->
	<!-- ********************************************** -->

	<!--
	   - connection disconnect
	 -->
	<transition state="finished" event="connection.disconnected" name="evt">
		<log expr="logMsg(evt,currentState,'...')" />
		<assign name="inConnectionId" expr="'null'" />	
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>
	
	<transition state="finished" event="dialog.exit" name="evt">
		<log expr="logMsg(evt,currentState,'fin de dialogue')" />
		<assign name="dialogIdVXML" expr="'null'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>
	
	<transition state="finished" event="the.end" name="evt">
		<if cond="inConnectionId == 'null'">
			<log expr="logMsg(evt,currentState,'Fin de script')"/>
			<exit/>
		<else/>
			<disconnect connectionid="inConnectionId" />
		</if>

	</transition>

	<!-- ********************************************** -->
	<!-- ****** currentState : none              ****** -->
	<!-- ********************************************** -->

	<!-- ****** Errors ****** -->
	<!--
	   - error.ccxml
	 -->
	<transition event="error.ccxml" name="evt">
		<log expr="logErr(evt,'error.ccxml')" />
		<!-- Pour eviter de retourner evt.reason=javascript error -->
		<assign name="anError" expr="'VXML not valid'" />
		<if cond="currentState == 'finished'">
			<exit/>
		<else/>
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	</transition>

	<!--
	   - T2 HS (NMS ko / VIP ok) => class failure
	 -->
	<transition event="error.connection" name="evt">
		<assign name="anError" expr="evt.reason" />
		<log expr="logErr(evt,'error.connection')" />
		<assign name="currentState" expr="'finished'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>

	<!--
	   - T2 HS (NMS ko / VIP ok) => class failure
	   - VIP ko : Erreur de connexion au SVIP
	 -->
	<transition event="error.notallowed" name="evt">
		<assign name="anError" expr="evt.reason" />
		<log expr="logErr(evt,'error.notallowed')" />
		<assign name="currentState" expr="'finished'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>

	<!--
	   - error dialog
	 -->
	<transition event="dialog.error" name="evt">
		<log expr="logMsg(evt,currentState,'Erreur dialog.error ...')" />
		<assign name="anError" expr="evt.reason" />
		<log expr="logErr(evt,'dialog.error')" />
		<assign name="currentState" expr="'finished'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>

	<!--
	   - error dialog
	 -->
	<transition event="dialog.*" name="evt">
		<log expr="logMsg(evt,currentState,'Erreur dialog ...')" />
		<assign name="anError" expr="evt.reason" />
		<log expr="logErr(evt,'sous event dialog')" />
		<assign name="currentState" expr="'finished'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>

	<!-- Gestion des cas d'erreurs -->
	<transition event="error.*" name="evt">
		<log expr="logErr(evt,'currentState='+currentState+', Erreur non traitee')" />
		<exit />
	</transition>

	</eventprocessor>
</ccxml>
