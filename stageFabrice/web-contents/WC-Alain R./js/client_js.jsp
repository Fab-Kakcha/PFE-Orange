<script >
	<![CDATA[
		
		// Remplace la variable _varSvc
		function setVarClient(client, nomVar, valeur) {
			
			var result = "";
			if (client == 'null') {
				return nomVar+":"+valeur; 		
			}
			var i = client.indexOf(nomVar);
			if(i == -1) {				
					result=client+nomVar+":"+valeur; 
			}
			else {
				result = client.substring(0,i);
				var tmp = client.substring(i+nomVar.length+1);
				var j = tmp.indexOf("_var");
				if (j==-1) {
					result+=nomVar+":"+valeur;
				}
				else {
					result+=nomVar+":"+valeur+tmp.substring(j);
				}
			}				
					
			return result;
		}
		// Remplace la variable _varSvc
		function setService(client, service) {
			return setVarClient(client,"_varSvc", service);
		}
		// Remplace la variable _varNumAppelant
		function setNumAppelant(client, num) {
			return setVarClient(client,"_varNumAppelant", num);
		}
		// Remplace la variable _varNumAppele
		function setNumAppele(client, num) {
			return setVarClient(client,"_varNumAppele", num);
		}
		// Remplace la variable _varRetour du createcall (BUSY, NONREP)
        function setRetour(client, val) {
            return setVarClient(client,"_varRetour", val);
        }
		// extraction du service de la variable client
		function donnerSvc(client) {
		     var reg2= new RegExp ("_varSvc:(.*)");
		     var tmp = reg2.exec (client);
		     var i = tmp[1].indexOf("_var");
		     if (i == -1) return tmp[1];
		     else {
		         return tmp[1].substring(0,i);
		     }
		     
		}
		//extraction de l'identifiant de la chaine client qui est de la forme:
        // _varIdent:10184155123CON1094903_varNumAppele:223_varDateDeb:1383640943511_varIdCrm:0_varSegment:DE_varNumClient:80000012_varNumAppelant:80000012_varLangue:FR
        function donnerIdentifiant(client) {
            if (client == 'null') return 'null';
            var reg = new RegExp ("_varIdent:([0-9.]+CON[0-9]+)_var");
            if (reg.test(client)) {
                var tmp = reg.exec(client);
                return tmp[1];
            }
            return 'null';
        }
        function donnerUrlFin(urlFin, client) {
            if (client == 'null') return urlFin;
            var reg = new RegExp ("_varIP:([0-9.:]+)");
            var tmp = reg.exec(client);
            var rst = "http://"+tmp[1]+urlFin;
            return rst;
        }
        // ajout de la position et du temps d'attente aux données client avant de lancer de dialogue d'attente
        // et changement de service
        function setAttenteClient(client, position, waitingtime, service) {
            var minute = Math.floor(waitingtime /60);
            var result = "";
            result = setService(client, service);   
            result += "_varPosition:"+position+"_varAttente:"+minute;           
            return result;
        }
		]]>
	</script>