<?xml version = "1.0" encoding="UTF-8"?>
<vxml xml:lang="fr" xmlns="http://www.w3.org/2001/vxml" version="2.0" application="common/root.jsp">
	
	<!--
		Ce VXML permet de jouer en boucle une musique d'attente
		passée en parametre.
	-->
	<var name="musique" expr="'prompts/'+session.connection.ccxml.values.musique"></var>

	<form id="boucle">		
		<field name="bidon">
			<prompt bargein="false" timeout="500ms">
				<audio expr="musique"/>
			</prompt>	
			<grammar mode="dtmf" src="grammars/gramGenerale.grxml" type="application/srgs+xml"/>
			<noinput>
				<goto next="#boucle"/>
			</noinput>
			<nomatch> 
				<goto next="#boucle"/>
			</nomatch>
		  
			<filled> 
			    <goto next="#boucle"/>
			</filled>
		</field>	
	</form>
</vxml>
