<script >
	<![CDATA[
		
	         function getTypeFromSipContent(content){
	             var svcAutorise = [ 'decroche', 'transfert', 'hold', 'unhold'];
	             var reg=new RegExp("<service>(.*)<\/service>");
	             var result = reg.exec(content);
	             if (result == null) {
	                 return '';
	             }
	             for (var i =0; i < svcAutorise.length; i++) {
	                 if (result[1] == svcAutorise[i]) {
	                     if (result[1] == 'unhold') {
	                         return 'unmuting';
	                     }
	                     else {
	                         return result[1];
	                     }
	                 }
	             }       
	             return '';
	         }
	         function getTransferCdu(content){
	             var reg=new RegExp("<cdu>(.*)<\/cdu>");
	             var result = reg.exec(content);
	             if (result == null) {
	                 return '';
	             }
	             return result[1];
	         }
	         function getIdentOperateur(content) {
	             var reg=new RegExp("<id>(.*)<\/id>");
	             var result = reg.exec(content);
	             if (result == null) {
	                 return '';
	             }
	             return result[1];
	         }
	         

	         function lancerDialogueAttente(seuil, waitingtime) {
	             if (waitingtime > seuil) return 'true';
	             else return 'false';
	         }
		]]>
	</script>