<script >
	<![CDATA[

	function logErr(event,msg){
		var retour='*** ';
		retour+='ERROR ';
		retour+='['+service+'] ';
		retour+='[idConCCXML='+idConCCXML+'] ';
		retour+='[name='+event.name+'] ';
		retour+='[reason='+event.reason+'] ';
		retour+='['+msg+'] ';
		retour+='***';
		return retour;
	} // logErr


	function logMsg(event,state,msg){
		var retour='*** ';
		retour+='['+service+'] ';
		retour+='[idConCCXML='+idConCCXML+'] ';
		retour+='['+msg+'] ';
		retour+='[event='+event.name+'] ';
		retour+='[currentState='+state+'] ';
		retour+='***';
		return retour;
	} // logMsg


  // extraction du numero appelant evt.connection.remote
  // en fonction du pattern.
  //  evt.connection.remote est de la forme sip:88888@xx.xx.xx.xx;transport
  // il s'agit d'eliminer le ';transport'
  // et le cas échéant de remplacer +33 par 0033
	function extraireCallerId (num) {

	var result = num.split(';');
	// on enleve le + s'il existe (remplace par 00)
	var r = result[0].replace ('+','00');
		return r;
	}//extraireCallerId
	// Extraction du numero appelant de la sip URI
	function extraireSipUser(callerId) {
		var reg=new RegExp(":(.*)@","i");
		if (!reg.test(callerId)) return '*****';
		var result = reg.exec (callerId);
	    return result[1];
	}
	// extraction du numero service de la chaine evt.connection.local
	  // en fonction du pattern.
	  // ensuite traduction du numero transmis par le reseau en numero de service
		function extraireNumeroService (string_to_call, pattern, tabNumeroTransmis) {

			var reg=new RegExp(pattern,"i");
			if (!reg.test(string_to_call)) return 'false';
			var result = reg.exec (string_to_call);
			var num = result[1];
			if (tabNumeroTransmis == "") return num;
			// on va appliquer la table de correspondance numero transmis par reseau = numero du service client
			var tab = tabNumeroTransmis.split(',');
			var tabNumero;

			for (var i = 0; i < tab.length; i++) {
				tabNumero = tab[i].split('>');
				// tabNumero[0]  contient le numero transmis par le reseau
				// tabNumero[1]  contient le numero de service client
				if (tabNumero[0] == "*") return tabNumero[1];
				if (num == tabNumero[0]) return tabNumero[1];
			}
			// pas de correspondance trouvee
			// numero reseau == numero service client
			return num;
		}//extraireNumeroService


		// fonction regardant si le numero appele matche avec 
		//appel.sortant.pattern.numeroappele
		function isNumeroVerifiePattern(numeroAppele, pat) {
		    if(pat != '') {
		        var reg2= new RegExp (pat);
		        if (reg2.test (numeroAppele)) {
		            var tmp = reg2.exec (numeroAppele);
		            for (i=tmp.length - 1; i >= 0; i--) {
		            
		            	if (tmp[i] != undefined) {		               
		            		  return tmp[i];
		                }
		            }		            
		        }
		    }		    
		    return 'false';		    
		}
		
	     
        function donnerTimeStamp() {
            var d = new Date();
            var M = "0"+(d.getMonth()+1);
            var jj = "0"+d.getDate();
            var hh = "0"+d.getHours();
            var mm = "0"+d.getMinutes();
            var ss = "0"+d.getSeconds();

            return d.getFullYear()+"/"+M.substring(M.length-2,M.length)+"/"+jj.substring(jj.length-2,jj.length)+" "+hh.substring(hh.length-2,hh.length)+":"+mm.substring(mm.length-2,mm.length)+":"+ss.substring(ss.length-2,ss.length);
        }
        function donnerTimeStamp2() {
            var d = new Date();
            var M = "0"+(d.getMonth()+1);
            var jj = "0"+d.getDate();
            var hh = "0"+d.getHours();
            var mm = "0"+d.getMinutes();
            var ss = "0"+d.getSeconds();

            return d.getFullYear()+M.substring(M.length-2,M.length)+jj.substring(jj.length-2,jj.length)+hh.substring(hh.length-2,hh.length)+mm.substring(mm.length-2,mm.length)+ss.substring(ss.length-2,ss.length);
        }
		]]>
	</script>