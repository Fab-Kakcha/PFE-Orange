<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE ccxml PUBLIC "CCXML/1.0/DTD" "ccxml.dtd" >
<ccxml version="1.0" >

<%@ page import="orange.olps.svi.config.Config" %>
<%@ page import="orange.olps.svi.util.Util" %>
<% String adrOms = request.getParameter("IPADDR");
	// l'adresse IP ser a constituer l'identifiant de l'appel (stats)
	// on enleve les "."

   if (adrOms != null) adrOms = adrOms.replaceAll("[.]","");
   // Svi a jouer pour l attente d'un teleconseiller
   String[] tmp = Config.getInstance().getProperty("ccxml.svi.navigation.attente", "Attente,null").split(",");
   String sviAttente = new String(tmp[0]);
   String navAttente = new String(tmp[1]);
   
   // Svi a jouer pour la mise en garde d un appel par le teleconseiller
   tmp = Config.getInstance().getProperty("ccxml.svi.navigation.miseengarde", "Attente,miseengarde").split(",");
   String sviMiseEnGarde = new String(tmp[0]);
   String navMiseEnGarde = new String(tmp[1]);
   
   // Svi a jouer pour le transfert d un appel par le teleconseiller
   tmp = Config.getInstance().getProperty("ccxml.svi.navigation.transfert", "Attente,null").split(",");
   String sviTransfert = new String(tmp[0]);
   String navTransfert = new String(tmp[1]);
   
   // Svi a jouer pour l appel direct d un poste (IPBX)
   tmp = Config.getInstance().getProperty("ccxml.svi.navigation.ipbx.appel", "Ipbx,null").split(",");
   String sviIpbxAppel = new String(tmp[0]);
   String navIpbxAppel = new String(tmp[1]);
   
   // Svi a jouer pour le parametrage de l IPBX: *21, ...
   tmp = Config.getInstance().getProperty("ccxml.svi.navigation.ipbx.param", "Ipbx,parametrage").split(",");
   String sviIpbxParam = new String(tmp[0]);
   String navIpbxParam = new String(tmp[1]);
   
	//Services autorisés a entrer dans le SVI 
	String serviceAutorise= Config.getInstance().getProperty (Config.SERVICE_AUTORISE);
	// table correspondance numero transmis /service client 
	String tabNumeroTransmis = Config.getInstance().getProperty (Config.APPLI_SERVICE_AUT_TRANSMIS);
	// pattern de recuperation du numero appele dans le numero local
	String patternAppele = Config.getInstance().getProperty (Config.PATTERN_NUM_APPELE);
	// pattern de recuperation du numero appelant dans le numero remote 
	String patternAppelant = Config.getInstance().getProperty (Config.PATTERN_NUM_APPELANT);
	// table de correspondance entre les numéros de service à rerouter et le numero de routage
	String tabServiceReroute = Config.getInstance().getProperty (Config.APPLI_SERVICE_REROUTE);
	// table de correspondance entre les numéros de service ne passant pas par le SVI et le numero de transfert
	String tabServiceCTI = Config.getInstance().getProperty (Config.APPLI_REROUTE_CTI);
  
	// format d'appel de la passerelle: sip:...@... Dépend de la marque de la passerelle -->
	String formatPasserelle = Config.getInstance().getProperty (Config.PATTERN_GATEWAY);
	// DM008 liste des cti externes pour lesquels le champ from sera alimente avec le numero de mobile
	// du client 
	String listeCti = Config.getInstance().getProperty (Config.TRANSFERT_CTI).trim();
	// DM008 numero qui alimentera le champ from lors d'un appel sortant 
	String numDefaut = Config.getInstance().getProperty (Config.TRANSFERT_DEFAUT).trim();
   
   String transferTimeout= Config.getInstance().getProperty(Config.TRANSFERT_TIMEOUT, "30s");
   String patternNumeroSortant = Config.getInstance().getProperty ("appel.sortant.pattern.numeroappele","");
   
   String patternIpbxAppel = Config.getInstance().getProperty ("ipbx.pattern.appel", "");
   String patternIpbxParam = Config.getInstance().getProperty ("ipbx.pattern.param", "star([0-9]+)");
   
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
	<jsp:include page="/js/result_js.jsp" flush="true" />
	<jsp:include page="/js/transfert_js.jsp" flush="true" />
	<jsp:include page="/js/sdcc_js.jsp" flush="true" />
    <jsp:include page="/js/client_js.jsp" flush="true" />
	

<!--
<variables :>
-->
	<!-- customization -->
	<var name="service" expr="'Svi'"/>
	<var name="urlVXMLscenario" expr="'/Svi/index.jsp'" />
	<var name="urlFin" expr="'/Svi/ServletFin'" />

	

	<var name="navigation" expr="'null'" /> <!-- page de navigation de demarrage -->
	<var name="navigationSav" expr="'null'" /> <!-- page de navigation de sauvegarde temporaire -->
	<var name="sviAttente" expr="'<%=sviAttente%>'" /> <!-- properties gérant l'attente -->
	<var name="navigationAttente" expr="'<%=navAttente%>'" /> <!-- item de navigation  d'attente initiale ou de transfert ou mise en garde -->
	
	<var name="client" expr="'null'" /> <!--donnees client issue du vxml -->

	<!-- does not change  -->
	<var name="idConCCXML" />					<!-- id session CCXML -->
	<var name="dialogIdVXML" expr="'null'"/>					<!-- id dialog -->
	<var name="inConnectionId" expr="'null'" />					<!-- number to call -->
	<var name="outConnectionId" expr="'null'"/>				<!-- number to join -->
	<var name="callerId" />						<!-- caller -->
	<var name="numDestination" expr="'null'" /> <!--  numero du service/appel sortant -->	

	<var name="resultScenario" expr="'null'"/>		<!-- result VXML service -->
	<var name="transferNumber" expr="'null'"/>					<!-- transfer destination -->

	<var name="anError" />						<!-- anError -->

	<var name="flagDisconnect" expr="'false'"/>
	<!-- Set an initial state -->
	<var name="currentState" expr="'init'" />
	<var name="oms" expr="'<%=adrOms%>'" />

  <!-- Variable spécifique a la conf et à l'attente -->
  <var name="isWaitingVXMLStartFlag" expr="'false'"/>
  <var name="conf_id" expr="'null'" />
  <var name="confCreated" expr="'null'" /> <!-- init, 0_CONF, IN_CONF, IN_OUT_CONF, OUT_CONF  -->
  <var name="transferCdu" expr="'null'"/>	<!-- transfer Cdu -->

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
				<exit/>
			<else />
				<!-- verifie si le numero appele est un service autorise a entrer dans le SVI  -->
				<assign name="numDestination" expr="isNumeroListe(numeroAppele, '<%=serviceAutorise%>')" />				

				<if cond="numDestination == 'false'">	
	         		<!-- verifie si le numero appele est un service à rerouter  -->
	          		<assign name="numDestination" expr="isNumeroListe(numeroAppele, '<%=tabServiceReroute%>')" />	          		

	          		<if cond="numDestination == 'false'">
						<!-- verifie si le numero appele est un service allant directement au CTI  --> 
						<assign name="numDestination" expr="isNumeroListe(numeroAppele, '<%=tabServiceCTI%>')" />
						
						<if cond="numDestination == 'false'"> 
							<!-- verifie si le numero appele est un appel sortant  --> 
							<assign name="numDestination" expr="isNumeroVerifiePattern(numeroAppele, '<%=patternNumeroSortant%>')" />
							
							<if cond="numDestination == 'false'"> 
								<!-- verifie si le numero appele est une commande IPBX  --> 
								<assign name="numDestination" expr="isNumeroVerifiePattern(numeroAppele, '<%=patternIpbxParam%>')" />
								
								<if cond="numDestination == 'false'"> 
									<!-- verifie si le numero appele est un appel IPBX  --> 
									<assign name="numDestination" expr="isNumeroVerifiePattern(numeroAppele, '<%=patternIpbxAppel%>')" />
									
									<if cond="numDestination == 'false'"> 
										<log expr="logErr(evt,currentState,'numero appele : NON TRAITE:'+numDestination)" />
										<reject/>
									<else/>
										<!-- appel vers un teleconseiller -->
										<!-- ne peut etre traite que par un SVI particulier -->
										<log expr="logMsg(evt,currentState,'appel IPBX: '+numDestination)" />
										<assign name="client" expr="setService(client, '<%=sviIpbxAppel%>')" />
										<assign name="client" expr="setNumAppele(client, numDestination)" />
										<assign name="client" expr="setNumAppelant(client,  extraireSipUser(callerId))" />
										<assign name="navigation" expr="'<%=navIpbxAppel%>'"/>
										<log expr="logMsg(evt,currentState,'numero vers un teleconseiller: '+numDestination)" />
										<assign name="currentState" expr="'initVXML'" />
										<accept />
									</if>
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
							<else /> <!-- Numero Sortant -->
							    <!-- verifie si le numero appele n'est pas un appel IPBX interne  --> 
                                <var name="numDestination2" expr="isNumeroVerifiePattern(numDestination, '<%=patternIpbxAppel%>')" />
                                
                                <if cond="numDestination2 == 'false'">                           
                                     <!-- Veritable appel sortant -->
									<!-- Transformation  au format d'appel sortant en fonction de la passerelle utilisee -->
									<log expr="logMsg(evt,currentState,'Appel sortant: '+numDestination)" />
									<assign name="transferNumber" expr="construireNumeroTransfert(numDestination, '<%=formatPasserelle%>')" />
									<!-- recalcul du champ from a transmettre -->
									<assign name="callerId" expr="getFromSortant(numDestination,evt.connection.remote, '<%=listeCti%>', '<%=numDefaut%>')" />					
									<assign name="currentState" expr="'startOutcall'" />
								<else />
								    <!-- appel vers un teleconseiller -->
                                     <!-- ne peut etre traite que par un SVI particulier -->
                                     <log expr="logMsg(evt,currentState,'appel IPBX: '+numDestination)" />
                                     <assign name="client" expr="setService(client, '<%=sviIpbxAppel%>')" />
                                     <assign name="client" expr="setNumAppele(client, numDestination)" />
                                     <assign name="client" expr="setNumAppelant(client,  extraireSipUser(callerId))" />
                                     <assign name="navigation" expr="'<%=navIpbxAppel%>'"/>
                                     <log expr="logMsg(evt,currentState,'numero vers un teleconseiller: '+numDestination)" />
                                     <assign name="currentState" expr="'initVXML'" />
                                     <accept />
								</if>	
							</if>
						<else /> 
							<!-- appel direct vers le centre d'appel ne passant pas par le SVI -->
							<log expr="logMsg(evt,currentState,'numero Destination vers CTI: '+numDestination)" />									
							<assign name="transferNumber" expr="construireNumeroTransfertCti(numDestination, evt.connection.remote,'<%=patternAppelant%>')" />
							<assign name="currentState" expr="'startOutcall'" />
						</if>
	          		<else /> <!-- Numero a rerouter (numDestination contient le numero de reroutage) -->
						<log expr="logMsg(evt,currentState,'numero Destination Reroute: '+numDestination)" />
	           			<assign name="transferNumber" expr="construireNumeroTransfert(numDestination, '<%=formatPasserelle%>')" />
						<assign name="currentState" expr="'startOutcall'" />
	          		</if>
					<if cond="currentState == 'startOutcall'"> 
						<log expr="logMsg(evt,currentState,'transfert vers : '+transferNumber)" />	         		
						<createcall dest="transferNumber" timeout="<%=transferTimeout%>" callerid="callerId" hints="{dtmfdetect:'true'}" />
					</if>
	      		<else />	<!-- cas 2 : entree dans le SVI -->
					<log expr="logMsg(evt,currentState,'numero Destination autorise: '+numDestination)" />
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
			<log expr="logMsg(evt,currentState,'Raccrochage client')" />
			<assign name="inConnectionId" expr="'null'" />
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
			<!--
			Values of resultScenario
			(timeout,transfer,default=null)
			-->
			<assign name="resultScenario" expr="getresultScenarioValue(evt)" />
			<assign name="dialogIdVXML" expr="'null'" />
			<log expr="logMsg(evt,currentState,'resultScenario='+resultScenario)" />

			<if cond="resultScenario == 'NORMAL'">
				<log expr="logMsg(evt,currentState,'Fin du scenario')" />
				<if cond="inConnectionId != 'null'">
					<disconnect connectionid="inConnectionId" />
				<else/>
					<assign name="currentState" expr="'finished'" />
					<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
				</if>

			<elseif cond="resultScenario == 'TRANSFERT'"/>
				<assign name="transferNumber" expr="evt.values.numTransfert" />
				<assign name="navigation" expr="evt.values.rappelSvi" />
				<assign name="client" expr="evt.values.client" />
				<log expr="logMsg(evt,currentState,'calling transfer number : '+transferNumber)" />
				<assign name="currentState" expr="'callingTransfer'" />
				<createcall dest="transferNumber" timeout="<%=transferTimeout%>"  callerid="callerId" hints="{dtmfdetect:'true'}" />
			<else />
				<if cond="inConnectionId != 'null'">
					<disconnect connectionid="inConnectionId" />
				<else/>
					<assign name="currentState" expr="'finished'" />
					<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
				</if>
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
	<!-- ****** currentState : callingTransfer   ****** -->
	<!-- ********************************************** -->
	<!-- transfer out call failed
	  on relance un vxml pour demander si le client veut etre rappele (cas SVI)
	  ou pour signaler le pb de connexion et router eventuellement vers un autre poste (cas IPBX)
	  navigation a ete initialisé par le dernier dialog.exit
	-->
	<transition state="callingTransfer" event="connection.failed" name="evt">
		<assign name="anError" expr="getSIPConnectionFailedReason(evt)" />
		<log expr="logMsg(evt,currentState,'transfer out call failed')" />
		<assign name="client" expr="setRetour(client,anError)" />
		
		<if cond="isWaitingVXMLStartFlag == 'false'">
			<!-- Pas de dialogue VXML en cours -->
			<if cond="inConnectionId != 'null'">
				<assign name="currentState" expr="'scenarioConnected'" />
				<log expr="logMsg(evt,currentState,'relance dialogue pour : '+client)" />
				<dialogstart connectionid="inConnectionId" src="urlVXMLscenario" namelist="numDestination navigation oms" parameters="client"/>
			<else/>
				<assign name="currentState" expr="'finished'" />
				<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
			</if>
		<else/>
			<assign name="isWaitingVXMLStartFlag" expr="'false'" />
			<dialogterminate dialogid="dialogIdVXML" />
		</if>
	</transition>

			
	<!-- connection in pending -->
	<transition state="callingTransfer" event="connection.progressing" name="evt">

		<log expr="logMsg(evt,currentState,'Progressing...')" />

		<var name="waitingtime" expr="evt.info.tps" />
		<var name="position" expr="evt.info.pos" />
		<if cond="position != '-1'">
			<if cond="isWaitingVXMLStartFlag == 'false'">
				<log expr="logMsg(evt,currentState,'Position et tps '+position+waitingtime)" />
				<if cond="lancerDialogueAttente(<%=Config.getInstance().getProperty(Config.SEUIL_ATTENTE, "5")%>, waitingtime) == 'true'" >
					<assign name="client" expr="setAttenteClient(client, position, waitingtime, '<%=sviAttente%>')"/>
					<assign name="navigationSav" expr="navigation" />
					<assign name="navigation" expr="navigationAttente" />
					<assign name="isWaitingVXMLStartFlag" expr="'true'" />
					<log expr="logMsg(evt,currentState,'Progressing...'+client)" />
					<dialogstart connectionid="inConnectionId" src="urlVXMLscenario" namelist="numDestination navigation oms" parameters="client"/>
				</if>
			</if>
		</if>
	</transition>

	<transition state="callingTransfer" event="dialog.started" name="evt">
		<log expr="logMsg(evt,currentState,'Dialogue VXML d attente client demarre')" />
		<assign name="dialogIdVXML" expr="evt.dialogid" />
		<!-- On remet les valeurs initiales -->
		<assign name="client" expr="setService(client, numDestination)"/>
		<assign name="navigation" expr="navigationSav" />
	</transition>

	<transition state="callingTransfer" event="dialog.failed" name="evt">
			<log expr="logMsg(evt,currentState,'Dialogue d attente en echec')" />
			<assign name="client" expr="setService(client, numDestination)"/>
			<assign name="navigation" expr="navigationSav" />
			<assign name="isWaitingVXMLStartFlag" expr="'false'" />
	</transition>

	<transition state="callingTransfer" event="error.dialog.*" name="evt">
		<log expr="logMsg(evt,currentState,'error dialog VXML')" />
		<assign name="currentState" expr="'finished'" />
		<assign name="client" expr="setService(client, numDestination)"/>
		<assign name="navigation" expr="navigationSav" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>

	<!-- Fin du dialogue  -->
	<transition state="callingTransfer" event="dialog.exit" name="evt">
		<log expr="logMsg(evt,currentState,'Fin du scenario')" />
		<assign name="isWaitingVXMLStartFlag" expr="'false'" />
		<assign name="dialogIdVXML" expr="'null'" />
		<if cond="conf_id != 'null'">
			<assign name="currentState" expr="'creatingConf'" />
			<send target="idConCCXML" targettype="ccxml" data="'creerConf'"/>	
		</if>
	</transition>

	<!-- Connexion de l agent (teleconseille) ou du poste interne -->
	<transition state="callingTransfer" event="connection.connected" name="evt">
		<assign name="outConnectionId" expr="evt.connectionid" />
		<log expr="logMsg(evt,currentState,'agent connecte '+outConnectionId)" />
		<if cond="donnerSvc(client) == '<%=sviIpbxAppel%>'">
            <!-- Fonctionnalité IPBX appel vers un poste interne -->
			<assign name="outConnectionId" expr="evt.connectionid" />
			<log expr="logMsg(evt,currentState,'outConnection done, linking')" />
			
			<if cond="inConnectionId != 'null'">
			
			    <assign name="currentState" expr="'joined'" />
				<if cond="isWaitingVXMLStartFlag == 'true'">
					<!-- Le dialogue d attente client est lancé on l arrete -->					
					<dialogterminate dialogid="dialogIdVXML" />
				<else/>
					<log expr="logMsg(evt,currentState,'join du poste')"/>
					<send target="idConCCXML" targettype="ccxml" data="'dialog.exit'"/>
				</if>		     
			
			<else/>
				<log expr="logMsg(evt,currentState,'inConnection terminee')"/>
				<assign name="currentState" expr="'finished'" />
				<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
			</if>	
		<else/>
			<if cond="dialogIdVXML != 'null'">
				<!-- Le dialogue d attente client est lancé on l arrete -->
				<log expr="logMsg(evt,currentState,'On termine le dialogue')"/>
				<dialogterminate immediate="true" dialogid="dialogIdVXML" />		
			</if>
		</if>
	</transition>
	
	<transition state="callingTransfer" event="decroche" name="evt">
		<log expr="logMsg(evt,currentState,'agent a decroche '+conf_id)" />	
		<if cond="inConnectionId != 'null'">
				<log expr="logMsg(evt,currentState,'creation conference')"/>
				<if cond="dialogIdVXML == 'null'">				
					<assign name="currentState" expr="'creatingConf'" />
					<send target="idConCCXML" targettype="ccxml" data="'creerConf'"/>		
				</if>
				
		<else/>
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	</transition>
	
	<!-- Hangup during out call -->
	<transition state="callingTransfer" event="connection.disconnected" name="evt">
		<log expr="logMsg(evt,currentState,'reason='+evt.reason)" />
		<assign name="inConnectionId" expr="'null'" />

		<if cond="isWaitingVXMLStartFlag == 'true'">
			<dialogterminate dialogid="dialogIdVXML" />
		</if>
		<assign name="currentState" expr="'finished'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		
	</transition>

		<!-- ********************************************** -->
		<!-- ****** currentState : joined            ****** -->
		<!-- ****** IPBX seulement.                  ****** -->
		<!-- ********************************************** -->
		<!-- L agent et le client sont connectés, le dialogue d attente est termine -->
		<transition state="joined" event="dialog.exit" name="evt">
			
			<log expr="logMsg(evt,currentState,'IPBX join')" />
			<assign name="isWaitingVXMLStartFlag" expr="'false'" />
			<assign name="dialogIdVXML" expr="'null'" />
			<if cond="inConnectionId != 'null'">
				<if cond="outConnectionId != 'null'">
					<join id1="inConnectionId" id2="outConnectionId"/>  
				<else/>
					<assign name="currentState" expr="'finished'" />
					<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
				</if>
			<else/>
				<assign name="currentState" expr="'finished'" />
				<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
			</if>
		</transition>
		<!-- joined -->
		<transition state="joined" event="conference.joined" name="evt">
			<log expr="logMsg(evt,currentState,evt.name)" />
		</transition>
		
		<!-- error -->
		<transition state="joined" event="error.conference.join" name="evt">
			<log expr="logMsg(evt,currentState,evt.name)" />
			
			<assign name="currentState" expr="'scenarioConnected'" />
			<dialogstart src="urlVXMLscenario" namelist="numDestination navigation oms"  parameters="client"/>
		</transition>

		<!-- Catch disconnect and exit -->		
		<transition state="joined" event="connection.disconnected" name="evt">
			<log expr="logMsg(evt,currentState,'Abutment - connection.disconnected')" />
			<if cond="evt.connectionid == outConnectionId">
				<!-- deconnexion du CTI -->	
				<assign name="outConnectionId" expr="'null'" />
				<if cond="inConnectionId != 'null'">
					<log expr="logMsg(evt,currentState,'CTI raccroche - ')" />
					<assign name="currentState" expr="'finished'" />
					<disconnect connectionid="inConnectionId" />				
				<else/>					
					<assign name="currentState" expr="'finished'" />
					<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
				</if>			
			<elseif cond="evt.connectionid == inConnectionId"/>
				<log expr="logMsg(evt,currentState,'disconnect outConnectionId')" />
				<assign name="inConnectionId" expr="'null'" />
				<if cond="outConnectionId != 'null'">
					<disconnect connectionid="outConnectionId" />
				<else/>
					<assign name="currentState" expr="'finished'" />
					<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
				</if>
			<else />
				<assign name="currentState" expr="'finished'" />
				<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
			</if>
		</transition>
		
		
	<!-- ********************************************** -->
	<!-- ****** currentState : creatingConf            ****** -->
	<!-- ********************************************** -->
	<!-- L agent et le client sont connectés, le dialogue d attente est termine -->
	<transition state="creatingConf" event="creerConf" name="evt">
		<log expr="logMsg(evt,currentState,'conference ==== : '+conf_id)" />
		<log expr="logMsg(evt,currentState,'Creating conference')" />
		
		<assign name="confCreated" expr="'INIT'" />
		<createconference confname="conf_id" conferenceid="conf_id" />
	</transition>


	<transition state="creatingConf" event="error.conference.create">
		<log expr="logMsg(evt,currentState,'error conference created ')" />
		<if cond="inConnectionId != 'null'">
			<!--  on relance le SVI pour le client -->
			<log expr="logMsg(evt,currentState,'relance dialogue pour : '+client)" />
			<assign name="currentState" expr="'scenarioConnected'" />

			<dialogstart connectionid="inConnectionId" src="urlVXMLscenario" namelist="numDestination navigation oms" parameters="client"/>
		<else/>
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	</transition>

	<!-- conference is created -->
	<transition state="creatingConf" event="conference.created" name="evt" >
		<log expr="logMsg(evt,currentState,'Conference cree '+conf_id)" />
		<assign name="confCreated" expr="'0_CONF'" />
		<join id1="outConnectionId" id2="conf_id" entertone="'true'" />
	</transition>

	<!-- Hangup during out call -->
	<transition state="creatingConf" event="connection.disconnected" name="evt">
		<log expr="logMsg(evt,currentState,'reason='+evt.reason)" />
		<assign name="inConnectionId" expr="'null'" />

		<if cond="evt.connectionid == outConnectionId">
			<assign name="outConnectionId" expr="'null'" />
		<else/>
			<assign name="inConnectionId" expr="'null'" />
		</if>
		<assign name="currentState" expr="'finished'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>

	<transition state="creatingConf" event="conference.joined" name="evt">
		<log expr="logMsg(evt,currentState,'CONFERENCE JOINED by: '+evt.id1)" />

		<if cond="confCreated == '0_CONF'">
			<if cond="evt.id1 == outConnectionId">
				<assign name="confCreated" expr="'IN_CONF'" />
				<join id1="inConnectionId" id2="conf_id" entertone="'true'"  />
			</if>
		<elseif cond="confCreated == 'IN_CONF'" />
			<if cond="evt.id1 == inConnectionId">
				<assign name="confCreated" expr="'IN_OUT_CONF'" />
				<assign name="currentState" expr="'activeConference'" />
			</if>
		</if>
	</transition>

	<!-- Le join de l'agent s'est mal passe -->
	<transition state="creatingConf" event="error.conference.join">
		<log expr="logMsg(evt,currentState,'Conference videe, on la detruit')" />
		<assign name="confCreated" expr="'null'" />
		<destroyconference conferenceid="conf_id" />
	</transition>
	<transition state="creatingConf" event="conference.destroyed" name="evt">
		<log expr="logMsg(evt,currentState,'Conference detruite')" />
		<if cond="inConnectionId != 'null'">
			<!--  on relance le SVI pour le client -->
			<log expr="logMsg(evt,currentState,'relance dialogue pour : '+client)" />

			<assign name="currentState" expr="'scenarioConnected'" />
			<dialogstart connectionid="inConnectionId" src="urlVXMLscenario" namelist="numDestination navigation oms" parameters="client"/>
		<else/>
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	</transition>
	<transition state="creatingConf" event="error.conference.destroy" name="evt">
		<log expr="logMsg(evt,currentState,'Conference NON detruite')" />
		<if cond="inConnectionId != 'null'">
			<!--  on relance le SVI pour le client -->
			<log expr="logMsg(evt,currentState,'relance dialogue pour : '+client)" />
			<assign name="currentState" expr="'scenarioConnected'" />
			<dialogstart connectionid="inConnectionId" src="urlVXMLscenario" namelist="numDestination navigation oms" parameters="client"/>
		<else/>
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	</transition>

	<!-- ********************************************** -->
	<!-- ****** currentState : activeConference  ****** -->
	<!-- ********************************************** -->

	<!-- EVENEMENTs recu par un SIP INFO
	hold -->
	<transition state="activeConference" event="hold" name="evt">
		<log expr="logMsg(evt,currentState,'muting client')" />
		<!-- <join id1="outConnectionId" id2="conf_id" duplex="'half'"/> -->
		<!-- on sort inConnectionId de la conference pour lui jouer de la musique -->
		<assign name="currentState" expr="'activeConferencehold'"/>
		<unjoin id1="inConnectionId" id2="conf_id"/>		
	</transition>
	<!-- unhold -->
	<transition state="activeConference" event="unmuting" name="evt">
		<log expr="logMsg(evt,currentState,'unmuting client')" />
		<join id1="inConnectionId" id2="conf_id" duplex="'full'"/>
	</transition>
	<!--  transfert vers un autre CDU -->
	<transition state="activeConference" event="transfert" name="evt">
		<log expr="logMsg(evt,currentState,'Transfert demande vers le cdu ' +transferCdu)" />

		<assign name="transferNumber" expr="modifierTransferNumber(transferNumber, transferCdu)"/>
		<log expr="logMsg(evt,currentState,'transfer number : '+transferNumber)" />

		<assign name="currentState" expr="'transfertConference'"/>
		<assign name="confCreated" expr="'null'" />
		<destroyconference conferenceid="conf_id" />
	</transition>

	<transition state="activeConference" event="conference.joined" name="evt">
		<log expr="logMsg(evt,currentState,'On reste en conference')" />
		<assign name="confCreated" expr="'IN_OUT_CONF'" />
	</transition>
	<transition state="activeConference" event="error.conference.join">
		<log expr="logMsg(evt,currentState,'On reste en conference')" />
	</transition>
	<!-- deconnection -->
	<transition state="activeConference" event="connection.disconnected" name="evt"> <!-- raccroché pendant la conf -->
		<log expr="logMsg(evt,currentState,'conference - connection.disconnected')" />
		<destroyconference conferenceid="conf_id" />
		<assign name="confCreated" expr="'null'" />
		<if cond="evt.connectionid == outConnectionId">
			<log expr="logMsg(evt,currentState,'disconnect outConnectionId')" />			
			<assign name="outConnectionId" expr="'null'" />
		</if>
		<if cond="evt.connectionid == inConnectionId">
			<log expr="logMsg(evt,currentState,'disconnect inConnectionId')" />
			<assign name="inConnectionId" expr="'null'" />
		</if>
		<assign name="currentState" expr="'finished'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>
	<!-- gestion des touches mute/unmute a repetition -->
	<transition state="activeConference" event="dialog.started" name="evt">
		<log expr="logMsg(evt,currentState,'Dialogue VXML de musique client demarre innattendu')" />
		<assign name="dialogIdVXML" expr="evt.dialogid" />
		<!-- On remet les valeurs initiales -->
		<assign name="client" expr="setService(client, numDestination)"/>
		<assign name="navigation" expr="navigationSav" />	
		<assign name="isWaitingVXMLStartFlag" expr="'fin'" />		
		<dialogterminate immediate="true" dialogid="dialogIdVXML" />
	</transition>
	<transition state="activeConference" event="dialog.exit" name="evt">
		<log expr="logMsg(evt,currentState,'Fin du dialogue ')" />
		<assign name="dialogIdVXML" expr="'null'" />		
		<assign name="isWaitingVXMLStartFlag" expr="'false'" />			
	</transition>
	
	<!-- ************************************************* -->
	<!-- ****** currentState : activeConferencehold ****** -->
	<!-- ************************************************* -->
	<transition state="activeConferencehold" event="conference.unjoined" name="evt">
		<log expr="logMsg(evt,currentState,'inConnectionId sort de la conference')" />
		<if cond="evt.id1 == inConnectionId">
			<if cond="isWaitingVXMLStartFlag == 'false'">		
				<assign name="client" expr="setService(client, '<%=sviMiseEnGarde%>')"/>
				<assign name="navigationSav" expr="navigation" />
				<assign name="navigation" expr="'<%=navMiseEnGarde%>'" />
				<assign name="isWaitingVXMLStartFlag" expr="'true'" />
				<log expr="logMsg(evt,currentState,'Lancement musique attente'+client)" />
				<dialogstart connectionid="inConnectionId" src="urlVXMLscenario" namelist="numDestination navigation oms" parameters="client"/>			
			</if>
		</if>
	</transition>
	<transition state="activeConferencehold" event="error.conference.unjoin">
		<log expr="logMsg(evt,currentState,'On reste en conference active')" />
		<assign name="currentState" expr="'activeConference'" />
		<!-- on met muet le teleconseiller dans la conference -->
		<join id1="outConnectionId" id2="conf_id" duplex="'half'"/>
	</transition>
	<transition state="activeConferencehold" event="dialog.started" name="evt">
		<log expr="logMsg(evt,currentState,'Dialogue VXML de musique client demarre')" />
		<assign name="dialogIdVXML" expr="evt.dialogid" />
		<!-- On remet les valeurs initiales -->
		<assign name="client" expr="setService(client, numDestination)"/>
		<assign name="navigation" expr="navigationSav" />
		<if cond="isWaitingVXMLStartFlag == 'fin'">
			<!-- on a demande un 'unmute' pendant le demarrage du dialogue -->
			<dialogterminate dialogid="dialogIdVXML" />
		</if>
	</transition>

	<transition state="activeConferencehold" event="dialog.failed" name="evt">
			<log expr="logMsg(evt,currentState,'Dialogue d attente en echec')" />
			<assign name="client" expr="setService(client, numDestination)"/>
			<assign name="navigation" expr="navigationSav" />
			<assign name="isWaitingVXMLStartFlag" expr="'false'" />
	</transition>

	<transition state="activeConferencehold" event="error.dialog.*" name="evt">
		<log expr="logMsg(evt,currentState,'error dialog VXML')" />
		<assign name="currentState" expr="'finished'" />
		<assign name="client" expr="setService(client, numDestination)"/>
		<assign name="navigation" expr="navigationSav" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>
	<transition state="activeConferencehold" event="unmuting" name="evt">
		<log expr="logMsg(evt,currentState,'unmuting client')" />
		<if cond="inConnectionId != 'null'">
			<if cond="isWaitingVXMLStartFlag == 'true'">
				<!-- Le dialogue d attente client est lancé on l arrete -->		
				
				<if cond="dialogIdVXML != 'null'" >
					<assign name="isWaitingVXMLStartFlag" expr="'fin'" />	
					<dialogterminate immediate="true" dialogid="dialogIdVXML" />
				<else/>
					<assign name="isWaitingVXMLStartFlag" expr="'false'" />
				</if>
			<else/>
				<log expr="logMsg(evt,currentState,'remise en conference de '+inConnectionId)"/>
				<assign name="isWaitingVXMLStartFlag" expr="'false'" />
				<assign name="currentState" expr="'activeConference'" />
				<join id1="inConnectionId" id2="conf_id" />
			</if>
		<else/>
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	</transition>
	<!-- Fin du dialogue  -->
	<transition state="activeConferencehold" event="dialog.exit" name="evt">
		<log expr="logMsg(evt,currentState,'Fin du scenario de musique')" />
		<assign name="dialogIdVXML" expr="'null'" />
		<if cond="isWaitingVXMLStartFlag == 'fin'">
			<assign name="isWaitingVXMLStartFlag" expr="'false'" />			
			<if cond="inConnectionId != 'null'">
				<log expr="logMsg(evt,currentState,'remise en conference de '+inConnectionId)"/>
				<assign name="currentState" expr="'activeConference'" />
				<join id1="inConnectionId" id2="conf_id" />
			<else/>
				<assign name="currentState" expr="'finished'" />
				<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
			</if>
		<else/>
			<!-- fin du dialogue non attendu, on reste en mute -->
			<assign name="isWaitingVXMLStartFlag" expr="'false'" />
		</if>
	</transition>
	<transition state="activeConferencehold" event="connection.disconnected" name="evt"> <!-- raccroché pendant la conf -->
		<log expr="logMsg(evt,currentState,'conference - connection.disconnected')" />
		<if cond="isWaitingVXMLStartFlag == 'true'">
			<!-- Le dialogue d attente client est lancé on l arrete -->		
			<assign name="isWaitingVXMLStartFlag" expr="'false'" />				
			<dialogterminate dialogid="dialogIdVXML" />
		<else/>
			<destroyconference conferenceid="conf_id" />
			<assign name="confCreated" expr="'null'" />
		</if>
		<if cond="evt.connectionid == outConnectionId">
			<log expr="logMsg(evt,currentState,'disconnect outConnectionId')" />			
			<assign name="outConnectionId" expr="'null'" />
		</if>
		<if cond="evt.connectionid == inConnectionId">
			<log expr="logMsg(evt,currentState,'disconnect inConnectionId')" />
			<assign name="inConnectionId" expr="'null'" />
		</if>
		<assign name="currentState" expr="'finished'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>
	<!-- ********************************************** -->
	<!-- ****** currentState : transfertConference  ****** -->
	<!-- ********************************************** -->

	<transition state="transfertConference" event="conference.destroyed" name="evt">
		<log expr="logMsg(evt,currentState,'destruction conference reussie')" />
		<if cond="outConnectionId != 'null'">
			<disconnect connectionid="outConnectionId" />
		<else/>
			<assign name="currentState" expr="'callingTransfer'" />
			<assign name="sviAttente" expr="'<%=sviTransfert%>'" />
			<assign name="navigationAttente" expr="'<%=navTransfert%>'" />
			<createcall  dest="transferNumber" timeout="<%=transferTimeout%>" hints="{dtmfdetect:'true'}" />
		</if>
	</transition>
	<transition state="transfertConference" event="error.conference.destroy" name="evt">
		<log expr="logMsg(evt,currentState,'destruction conference echec')" />
		<if cond="outConnectionId != 'null'">
			<disconnect connectionid="outConnectionId" />
		<else/>
			<assign name="currentState" expr="'callingTransfer'" />
			<assign name="navigationAttente" expr="'<%=navTransfert%>'" />
			<assign name="sviAttente" expr="'<%=sviTransfert%>'" />
			<createcall  dest="transferNumber" timeout="<%=transferTimeout%>" hints="{dtmfdetect:'true'}" />
		</if>
	</transition>
	<transition state="transfertConference" event="connection.disconnected" name="evt">
		<log expr="logMsg(evt,currentState,'connection.disconnected')" />
		<if cond="evt.connectionid == outConnectionId">
			<log expr="logMsg(evt,currentState,'disconnect outConnectionId')" />
			<assign name="outConnectionId" expr="'null'" />
			<if cond="confCreated == 'null'">
				<assign name="currentState" expr="'callingTransfer'" />
				<assign name="sviAttente" expr="'<%=sviTransfert%>'" />
				<assign name="navigationAttente" expr="'<%=navTransfert%>'" />
				<createcall  dest="transferNumber" timeout="<%=transferTimeout%>" hints="{dtmfdetect:'true'}" />
			</if>
		<else/>
			<if cond="evt.connectionid == inConnectionId">
				<log expr="logMsg(evt,currentState,'disconnect inConnectionId')" />
				<assign name="inConnectionId" expr="'null'" />				
			</if>
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
			
		</if>
	</transition>

	<!-- ********************************************** -->
	<!-- ****** currentState : none              ****** -->
	<!-- ********************************************** -->

	<!-- Detect signal -->
	<transition event="connection.signal" name="evt">
		<log expr="logMsg(evt,currentState,'EVT= '+evt.info.content+' Value='+evt.info.value)" />
		<if cond="evt.info.type == 'dtmf'">
			<log expr="logMsg(evt,currentState,'DTMF '+evt.info.value+' catched')" />
		<else/>
			<var name="type" expr="getTypeFromSipContent(evt.info.content)"/>
			<log expr="logMsg(evt,currentState,'Type= '+type)" />
			<if cond="type == ''">
				<log expr="logMsg(evt,currentState,'Unexpected signal '+evt.info.content+' catched')" />
			<else/>
				<log expr="logMsg(evt,currentState,'signal connu detecte '+type)" />
				<if cond="type == 'transfert'">
					<assign name="transferCdu" expr="getTransferCdu(evt.info.content)" />
				<elseif cond="type == 'decroche'"/>
					<assign name="conf_id" expr="getIdentOperateur(evt.info.content)" />
				</if>
				<send target="idConCCXML" targettype="ccxml" data="type"/>
			</if>
		</if>
	</transition>

		<!-- cas 2 : Appels sortants -->
		<!-- ********************************************** -->
		<!-- ****** currentState : startOutcall     ****** -->
		<!-- ********************************************** -->
		<!-- connexion etablie (createcall en cours) lors de l'appel sortant
		la communication est établie, on accepte l'appel entrant -->
		<transition state="startOutcall" event="connection.connected" name="evt">
			<assign name="outConnectionId" expr="evt.connectionid" />
			<log expr="logMsg(evt,currentState,'connection established with :'+transferNumber)" />
			<assign name="currentState" expr="'acceptInCall'" />
			<accept connectionid="inConnectionId"/>
		</transition>

	<!-- transfer out call failed  -->
		<transition state="startOutcall" event="connection.failed" name="evt">
	  <assign name="failed_reason" expr="getSIPConnectionFailedReason(evt)" />
	  <log expr="logMsg(evt,currentState,'transfer out call failed, reason is '+failed_reason)" />
	  <assign name="currentState" expr="'FailedOutCall'" />
	  <reject connectionid="inConnectionId" />
	  <assign name="inConnectionId" expr="'null'" />
		</transition>
	<!-- transfer out reject  -->
		<transition state="startOutcall" event="connection.reject.failed" name="evt">
	  <assign name="failed_reason" expr="getSIPConnectionFailedReason(evt)" />
	  <log expr="logMsg(evt,currentState,'transfer out call rejected, reason is '+failed_reason)" />
	  <assign name="currentState" expr="'FailedOutCall'" />
	  <reject connectionid="inConnectionId" />
		</transition>
	<!-- disconnect du inConnectionId -->
	<transition state="startOutcall" event="connection.disconnected" name="evt">
		<log expr="logMsg(evt,currentState,'reason='+evt.reason)" />
		<if cond="inConnectionId == evt.connectionid">
			<assign name="inConnectionId" expr="'null'"/>
			<if cond="outConnectionId != 'null'">
				<disconnect connectionid="outConnectionId" />
			<else/>
				<assign name="currentState" expr="'finished'" />
				<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
			</if>
		<elseif cond="outConnectionId == evt.connectionid"/>
			<assign name="outConnectionId" expr="'null'"/>
			<if cond="inConnectionId != 'null'">
				<disconnect connectionid="inConnectionId" />
			<else/>
				<assign name="currentState" expr="'finished'" />
				<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
			</if>
		<else/>
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	</transition>
	<!-- ********************************************** -->
	<!-- ****** currentState : acceptInCall     ****** -->
	<!-- ********************************************** -->
	<!-- Accept du in, il faut faire le join -->
	<transition state="acceptInCall" event="connection.connected" name="evt">
	  <log expr="logMsg(evt,currentState,evt.name)" />

	  <if cond="inConnectionId != 'null'">
		<if cond="outConnectionId != 'null'">
		  <!-- Cas normal -->
		  <assign name="currentState" expr="'activeOutcall'" />
		  <join id1="inConnectionId" id2="outConnectionId"/>
		<else/>
		  <disconnect connectionid="inConnectionId" />
		</if>
	  <else/>
		<if cond="outConnectionId != 'null'">
		  <disconnect connectionid="outConnectionId" />
		<else/>
		  <assign name="currentState" expr="'finished'" />
		  <send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	  </if>
	</transition>

	<transition state="acceptInCall" event="connection.disconnect" name="evt">
	  <log expr="logMsg(evt,currentState,evt.name)" />
	  <if cond="inConnectionId == evt.connectionid">
		<assign name="inConnectionId" expr="'null'"/>
		<if cond="outConnectionId != 'null'">
		  <disconnect connectionid="outConnectionId" />
		<else/>
		  <assign name="currentState" expr="'finished'" />
		  <send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	  <elseif cond="outConnectionId == evt.connectionid"/>
		<assign name="outConnectionId" expr="'null'"/>
		<if cond="inConnectionId != 'null'">
		  <disconnect connectionid="inConnectionId" />
		<else/>
		  <assign name="currentState" expr="'finished'" />
		  <send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	  <else/>
		<assign name="currentState" expr="'finished'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	  </if>
	</transition>

	<transition state="acceptInCall" event="connection.failed" name="evt">
	  <assign name="failed_reason" expr="getSIPConnectionFailedReason(evt)" />
	  <log expr="logMsg(evt,currentState,'transfer out call failed, reason is '+failed_reason)" />
	  <assign name="inConnectionId" expr="'null'"/>
	  <disconnect connectionid="outConnectionId" />
	</transition>
	<!-- ********************************************** -->
		<!-- ****** currentState : activeOutcall     ****** -->
		<!-- ********************************************** -->
		<!-- joined -->
		<transition state="activeOutcall" event="conference.joined" name="evt">
			<log expr="logMsg(evt,currentState,evt.name)" />
		</transition>

		<!-- error -->
		<transition state="activeOutcall" event="error.conference.join">
			<log expr="logMsg(evt,currentState,evt.name)" />
			<disconnect connectionid="inConnectionId" />
		</transition>

		<!-- Catch disconnect and exit -->
		<transition state="activeOutcall" event="connection.disconnected" name="evt">
			<log expr="logMsg(evt,currentState,'Abutment - connection.disconnected')" />
	  <if cond="inConnectionId == evt.connectionid">
		<assign name="inConnectionId" expr="'null'"/>
		<if cond="outConnectionId != 'null'">
		  <disconnect connectionid="outConnectionId" />
		<else/>
		  <assign name="currentState" expr="'finished'" />
		  <send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	  <elseif cond="outConnectionId == evt.connectionid"/>
		<assign name="outConnectionId" expr="'null'"/>
		<if cond="inConnectionId != 'null'">
		  <disconnect connectionid="inConnectionId" />
		<else/>
		  <assign name="currentState" expr="'finished'" />
		  <send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	  <else/>
		<assign name="currentState" expr="'finished'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	  </if>
		</transition>

		<!-- ********************************************** -->
		<!-- ****** currentState : FailedOutCall     ****** -->
		<!-- ********************************************** -->
		<!-- reject du   inConnectionId -->
		<transition state="FailedOutCall" event="connection.failed" name="evt">
	  <assign name="failed_reason" expr="getSIPConnectionFailedReason(evt)" />
	  <log expr="logMsg(evt,currentState,'Reject , reason is '+failed_reason)" />
	  <assign name="currentState" expr="'finished'" />
	  <send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</transition>
		<transition state="FailedOutCall" event="connection.disconnected" name="evt">
	  <assign name="failed_reason" expr="getSIPConnectionFailedReason(evt)" />
	  <log expr="logMsg(evt,currentState,'Disconnected , reason is '+failed_reason)" />
	   <if cond="inConnectionId == evt.connectionid">
		<assign name="inConnectionId" expr="'null'"/>
		<if cond="outConnectionId != 'null'">
		  <disconnect connectionid="outConnectionId" />
		<else/>
		  <assign name="currentState" expr="'finished'" />
		  <send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	  <elseif cond="outConnectionId == evt.connectionid"/>
		<assign name="outConnectionId" expr="'null'"/>
		<if cond="inConnectionId != 'null'">
		  <disconnect connectionid="inConnectionId" />
		<else/>
		  <assign name="currentState" expr="'finished'" />
		  <send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	  <else/>
		<assign name="currentState" expr="'finished'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	  </if>
		</transition>
	<!-- connect de l appele -->
	<transition state="FailedOutCall" event="connection.connected" name="evt">
			<assign name="outConnectionId" expr="evt.connectionid" />
			<log expr="logMsg(evt,currentState,'connection established with :'+transferNumber)" />
			<disconnect connectionid="outConnectionId"/>
		</transition>


		<!-- ********************************************** -->
		<!-- ****** currentState : finished          ****** -->
		<!-- ********************************************** -->

		<!--
		   - connection disconnect
		 -->
	<transition state="finished" event="connection.disconnected" name="evt">
		<log expr="logMsg(evt,currentState,'...')" />
		<if cond="evt.connectionid == outConnectionId">
			<log expr="logMsg(evt,currentState,'disconnect inConnectionId')" />
			<assign name="outConnectionId" expr="'null'" />
		<else/>
			<assign name="inConnectionId" expr="'null'" />
		</if>
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>
	<transition state="finished" event="connection.failed" name="evt">
		<log expr="logMsg(evt,currentState,'disconnect outConnectionId')" />
		<assign name="outConnectionId" expr="'null'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>
	<transition state="finished" event="conference.unjoined" name="evt">
		<log expr="logMsg(evt,currentState,'...')" />
		<if cond="confCreated != 'null'">
			<assign name="confCreated" expr="'null'"/>
			<destroyconference conferenceid="conf_id" />
		<else />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
		
	</transition>
	<transition state="finished" event="error.conference.unjoin" name="evt">
		<log expr="logMsg(evt,currentState,'...')" />
		<if cond="confCreated != 'null'">
			<assign name="confCreated" expr="'null'"/>
			<destroyconference conferenceid="conf_id" />
		<else />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	</transition>
	<transition state="finished" event="conference.created" name="evt">
		<log expr="logMsg(evt,currentState,'sortie de conference')" />
		<assign name="confCreated" expr="'null'"/>
		<destroyconference conferenceid="conf_id" />
	</transition>
	<transition state="finished" event="error.conference.create" name="evt">
		<log expr="logMsg(evt,currentState,'sortie de conference')" />
		<assign name="confCreated" expr="'null'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>
	<transition state="finished" event="conference.destroyed" name="evt">
		<log expr="logMsg(evt,currentState,'sortie de conference')" />
		<assign name="confCreated" expr="'null'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>
	<transition state="finished" event="error.conference.destroy" name="evt">
		<log expr="logMsg(evt,currentState,'erreur sortie de conference')" />
		<assign name="confCreated" expr="'null'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>
	<transition state="finished" event="dialog.exit" name="evt">
		<log expr="logMsg(evt,currentState,'fin de dialogue')" />
		<assign name="dialogIdVXML" expr="'null'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>
	<transition state="finished" event="connection.disconnected" name="evt">
		<log expr="logMsg(evt,currentState,'deconnexion de '+evt.connectionid)" />
		<if cond="inConnectionId == evt.connectionid" >
			<assign name="inConnectionId" expr="'null'"/>
		<else/>
			<assign name="outConnectionId" expr="'null'"/>
		</if>
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>
	<transition state="finished" event="connection.progressing" name="evt">
		<log expr="logMsg(evt,currentState,'Progressing non traite')" />
		<disconnect connectionid="'t2'" />
	</transition>
	<transition state="finished" event="the.end" name="evt">
		<if cond="confCreated != 'null'" >
			<assign name="confCreated" expr="'null'"/>
			<destroyconference conferenceid="conf_id" />		
		<else/>
			<if cond="inConnectionId == 'null'">
				<if cond="outConnectionId == 'null'">

					<if cond="client == 'null'">
						<log expr="logMsg(evt,currentState,'Fin de script')"/>
						<exit/>
					<else/>
						<!-- Le client est parti sur le CTI, on envoie le hangup pour les stats -->
						<var name="identifiant" expr="donnerIdentifiant(client)" />
						<if cond="identifiant == 'null'">
							<log expr="logMsg(evt,currentState,'identifiant de connexion non trouve')" />
							<exit/>
						<else/>
							<!--
								non car l'AS et l'OMS peuvent ne pas etre à la meme heure
								<var name="timestamp" expr="donnerTimeStamp()" />
							-->
							<log expr="logMsg(evt,currentState,'envoi timestamp de fin '+identifiant)" />

							<assign name="currentState" expr="'Arret'" />
							<send data="" targettype="'basichttp'" target="donnerUrlFin(urlFin, client)" namelist="identifiant"/>
						</if>	
					</if>
				<else/>
					<disconnect connectionid="outConnectionId" />
				</if>
			<else/>
				<disconnect connectionid="inConnectionId" />
			</if>
		</if>
	</transition>
	<!-- ********************************************** -->
	<!-- ****** currentState : Arret              ****** -->
	<!-- ********************************************** -->
	<transition state="Arret" event="send.successful" name="evt">
		<log expr="logMsg(evt,currentState,'Send Ok')" />
		<log expr="logMsg(evt,currentState,'----FIN----')" />
		<exit />
	</transition>

	<transition state="Arret" event="error.send.*" name="evt">
		<log expr="logErr(evt,currentState,' error send requete AS modifier')" />
		<exit />
	</transition>
	<transition state="Arret" event="error.*" name="evt">
		<log expr="logErr(evt,currentState,'Erreur non traitee')" />
	</transition>
	<transition state="Arret" event="conference.*" name="evt">
		<log expr="logErr(evt,currentState,'Erreur non traitee')" />
	</transition>
	<transition state="Arret" event="connection.*" name="evt">
		<log expr="logErr(evt,currentState,'Erreur non traitee')" />
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
