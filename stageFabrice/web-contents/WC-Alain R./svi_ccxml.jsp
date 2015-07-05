<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE ccxml PUBLIC "CCXML/1.0/DTD" "ccxml.dtd" >
<ccxml version="1.0" >

<%@ page import="orange.olps.svi.config.Config" %>
<% String adrOms = request.getParameter("IPADDR");
	// l'adresse IP ser a constituer l'identifiant de l'appel (stats)
	// on enleve les "."
	
   if (adrOms != null) adrOms = adrOms.replaceAll("[.]","");
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
    <jsp:include page="/js/client_js.jsp" flush="true" />

<!--
<variables :>
-->
	<!-- customization -->
	<var name="service" expr="'Svi'"/>
	<var name="urlVXMLscenario" expr="'/Svi/index.jsp'" />
	<var name="urlFin" expr="'/Svi/ServletFin'" />
	
	<var name="transferTimeout" expr="'<%=Config.getInstance().getProperty(Config.TRANSFERT_TIMEOUT, "30s")%>'"/>
	
	<var name="navigation" expr="'null'" /> <!-- page de navigation de demarrage -->
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

 	<!-- Services autorisés a entrer dans le SVI -->
 	<var name="serviceAutorise" expr="'<%=Config.getInstance().getProperty (Config.SERVICE_AUTORISE)%>'"/>
  <!-- table correspondance numero transmis /service client -->
  <var name="tabNumeroTransmis" expr="'<%=Config.getInstance().getProperty (Config.APPLI_SERVICE_AUT_TRANSMIS)%>'"/>
  <!-- pattern de recuperation du numero appele dans le numero local -->
 	<var name="patternAppele"   expr="'<%= Config.getInstance().getProperty (Config.PATTERN_NUM_APPELE)%>'"/>
	 <!-- pattern de recuperation du numero appelant dans le numero remote -->
 	<var name="patternAppelant"   expr="'<%= Config.getInstance().getProperty (Config.PATTERN_NUM_APPELANT)%>'"/>
  <!-- table de correspondance entre les numéros de service à rerouter et le numero de routage -->
 	<var name="tabServiceReroute"  expr="'<%= Config.getInstance().getProperty (Config.APPLI_SERVICE_REROUTE)%>'"/>
 <!-- table de correspondance entre les numéros de service ne passant pas par le SVI et le numero de transfert -->
 	<var name="tabServiceCTI"  expr="'<%= Config.getInstance().getProperty (Config.APPLI_REROUTE_CTI)%>'"/>

 	<!-- deuxième pattern d'extraction du numéro de destination tel qu'il sera recopié dans la chaine sip:... -->
	<var name="patternExtraireNum" expr="'<%= Config.getInstance().getProperty (Config.PATTERN_NUM_APPELE_2)%>'"/>	
  <!-- format d'appel de la passerelle: sip:...@... Dépend de la marque de la passerelle -->
	<var name="formatPasserelle" expr="'<%= Config.getInstance().getProperty (Config.PATTERN_GATEWAY)%>'"/>
	<!-- DM008 liste des cti externes pour lesquels le champ from sera alimente avec le numero de mobile
  du client -->
  <var name="listeCti" expr="'<%= Config.getInstance().getProperty (Config.TRANSFERT_CTI).trim()%>'"/>
  <!-- DM008 numero qui alimentera le champ from lors d'un appel sortant -->
  <var name="numDefaut" expr="'<%= Config.getInstance().getProperty (Config.TRANSFERT_DEFAUT).trim()%>'"/>
  
  
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
			<var name="numeroAppele" expr="extraireNumeroService(evt.connection.local, patternAppele, tabNumeroTransmis)" />
			<log expr="logMsg(evt,currentState,'numero appele : '+numeroAppele)" />
			
			<if cond="numeroAppele == 'false'">
        		<log expr="logMsg(evt,currentState,'numero appele : NON TRAITE')" />
       			<reject/>
      		<else />
				<!-- verifie si le numero appele est un service autorise a entrer dans le SVI  -->
       			<assign name="numDestination" expr="isNumeroListe(numeroAppele, serviceAutorise)" />
       			<log expr="logMsg(evt,currentState,'numero Destination autorise: '+numDestination)" />
			
				<if cond="numDestination == 'false'">	<!-- cas 1 : appels sortants -->				         
	         		<!-- verifie si le numero appele est un service à rerouter  -->
	          		<assign name="numDestination" expr="isNumeroListe(numeroAppele, tabServiceReroute)" />
	          		<log expr="logMsg(evt,currentState,'numero Destination Reroute: '+numDestination)" />
	        
	          		<if cond="numDestination == 'false'"> 
						<assign name="numDestination" expr="isNumeroListe(numeroAppele, tabServiceCTI)" />
						<log expr="logMsg(evt,currentState,'numero Destination vers CTI: '+numDestination)" />
						<if cond="numDestination == 'false'"> <!-- Appel sortant -->
							<!--  Le numero appele n'a pas a être retraite  -->
							<assign name="numDestination" expr="numeroAppele" />
							<!-- Transformation  au format d'appel sortant en fonction de la passerelle utilisee -->
							<assign name="transferNumber" expr="construireNumeroTransfert(numDestination, patternExtraireNum, formatPasserelle)" />
							<!-- recalcul du champ from a transmettre -->
							<assign name="callerId" expr="getFromSortant(numDestination,evt.connection.remote,listeCti,numDefaut)" />
						<else /> <!-- appel vers le cti ne passant pas par le SVI -->
							<!-- Transformation  au format d'appel sortant en fonction de la passerelle utilisee -->
							<assign name="transferNumber" expr="construireNumeroTransfertCti(numDestination, evt.connection.remote,patternAppelant)" />
						</if>
	          		<else /> <!-- numDestination contient deja le numero de reroutage -->
	           			<assign name="transferNumber" expr="construireNumeroTransfert(numDestination, '', formatPasserelle)" />
	          		</if>                 
	          		<log expr="logMsg(evt,currentState,'transfert vers : '+transferNumber)" />
	         		<assign name="currentState" expr="'startOutcall'" />
	          		<createcall dest="transferNumber" timeout="transferTimeout" callerid="callerId" hints="{dtmfdetect:'true'}" />
	      		<else />	<!-- cas 2 : entree dans le SVI -->
          			<assign name="currentState" expr="'initVXML'" />
          			<accept />
       			 </if>
    	  </if>
		</transition>
      <!-- Error during accept call -->
    	<transition state="waiting" event="connection.accept.failed" name="evt">
			<log expr="logErr(evt,currentState+', '+ inConnectionId)" />
			<assign name="inConnectionId" expr="'null'" />			
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
      </transition>
    <!-- ********************************************** -->
		<!-- ****** currentState : initVXML          ****** -->
		<!-- ********************************************** -->		

    
    <!-- Gestion d'erreur sur SIP Server -->
		<transition state="initVXML" event="connection.disconnected" name="evt">
			<log expr="logMsg(evt,currentState,'Raccrochage client '+ inConnectionId)" />
			<assign name="inConnectionId" expr="'null'" />
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</transition>		
   		<transition state="initVXML" event="connection.accept.failed" name="evt">
			<assign name="anError" expr="'connection.accept.failed'" />
			<log label="inConnectionId" expr="logErr(evt,currentState+', '+ inConnectionId)" />
			<assign name="inConnectionId" expr="'null'" />
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
				<createcall dest="transferNumber" timeout="transferTimeout"  callerid="callerId" hints="{dtmfdetect:'true'}" />				
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

	<!-- connection with the gateway -->
	<transition state="waitingExit" event="dialog.exit" name="evt">
		<log expr="logMsg(evt,currentState,'Fin...')" />	
		<assign name="dialogIdVXML" expr="'null'" />
		<assign name="currentState" expr="'finished'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>
		
	<!-- ********************************************** -->
	<!-- ****** currentState : callingTransfer   ****** -->
	<!-- ********************************************** -->

	<transition state="callingTransfer" event="connection.connected" name="evt">
		<assign name="outConnectionId" expr="evt.connectionid" />
		<log expr="logMsg(evt,currentState,'outConnection done, linking')" />
		
		<if cond="inConnectionId != 'null'">
			<if cond="outConnectionId != 'null'">
				<assign name="currentState" expr="'joined'" />
				<join id1="inConnectionId" id2="outConnectionId"/>	
			<else/>
				<log expr="logMsg(evt,currentState,'outConnection terminee')"/>
				<assign name="currentState" expr="'finished'" />
				<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
			</if>
		<else/>
			<log expr="logMsg(evt,currentState,'inConnection terminee')"/>
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	</transition>

		<!-- transfer out call failed  
      on relance un vxml pour demander si le client veut etre rappele
    -->	
	<transition state="callingTransfer" event="connection.failed" name="evt">
		<assign name="anError" expr="getSIPConnectionFailedReason(evt)" />
		<log expr="logMsg(evt,currentState,'transfer out call failed')" />       
		<assign name="currentState" expr="'scenarioConnected'" />
		<if cond="inConnectionId != 'null'">
			<log expr="logMsg(evt,currentState,'relance dialogue pour : '+client)" />
			<dialogstart connectionid="inConnectionId" src="urlVXMLscenario" namelist="numDestination navigation oms" parameters="client"/>
		<else/>
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
		</if>
	</transition>

	<!-- Hangup during out call -->
	<transition state="callingTransfer" event="connection.disconnected" name="evt">
		<log expr="logMsg(evt,currentState,'reason='+evt.reason)" />
		<assign name="inConnectionId" expr="'null'" />
		<assign name="currentState" expr="'finished'" />
		<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
	</transition>

		<!-- ********************************************** -->
		<!-- ****** currentState : joined            ****** -->
		<!-- ********************************************** -->
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
		<!-- unjoined -->
		<transition state="unjoined" event="conference.unjoined" name="evt">
			<log expr="logMsg(evt,currentState,evt.name)" />		
			<if cond="inConnectionId != 'null'">
				<disconnect connectionid="inConnectionId" />
			</if>
		</transition>
		<!-- unjoined -->
		<transition state="unjoined" event="connection.disconnected" name="evt">
			<log expr="logMsg(evt,currentState,evt.name)" />
			<assign name="inConnectionId" expr="'null'" />			
			<assign name="currentState" expr="'finished'" />
			<send target="idConCCXML" targettype="ccxml" data="'the.end'"/>
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
    <!-- connect de l'appele -->
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

	<transition state="finished" event="the.end" name="evt">
		<if cond="inConnectionId == 'null'">
			<if cond="outConnectionId == 'null'">			
				
				<if cond="client == 'null'">
					<log expr="logMsg(evt,currentState,'Fin de script')"/>
					<exit/>
				<else/>
					<!-- Le client est parti sur le CTI, on envoie le hangup pour les stats -->
					<var name="identifiant" expr="donnerIdentifiant(client)" />	
					<if cond="resultScenario == 'TRANSFERT'">
					   <log expr="logMsg(evt,currentState,'envoi timestamp de fin '+identifiant)" />
                       <assign name="currentState" expr="'Arret'" />
                       <send data="" targettype="'basichttp'" target="donnerUrlFin(urlFin, client)" namelist="identifiant"/>
                    <else/>
					   <log expr="logMsg(evt,currentState,'identifiant de connexion non trouve')" />
                        <exit/>
					</if> 				
				</if>
			<else/>
				<disconnect connectionid="outConnectionId" />
			</if>
		<else />
			<disconnect connectionid="inConnectionId" />
		</if>
	</transition>
		
	<transition state="Arret" event="send.successful" name="evt">
		<log expr="logMsg(evt,currentState,'Send Ok')" />
		<exit />			
	</transition>

	<!-- 
	   - Sending error,
	-->
	<transition state="Arret" event="error.send.*" name="evt">
		<log expr="logErr(evt,currentState,' error send requete AS modifier')" />
		<exit />
	</transition>
	<transition state="Arret" event="error.*" name="evt">
        <log expr="logErr(evt,currentState,'Erreur non traitee')" />
        <exit />
    </transition>
		<!-- ********************************************** -->
		<!-- ****** currentState : none              ****** -->
		<!-- ********************************************** -->

		<!-- Detect signal -->		
		<transition event="connection.signal" name="evt">
			<if cond="evt.info.type == 'dtmf'">
				<log expr="logMsg(evt,currentState,'DTMF '+evt.info.value+' catched')" />
			<else />
				<log expr="logMsg(evt,currentState,'Unexpected signal '+evt.info.value+' catched')" />
			</if>
		</transition>

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
