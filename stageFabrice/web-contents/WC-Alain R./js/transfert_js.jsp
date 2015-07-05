<script >
	<![CDATA[

	         // retourne le numero (de reroutage ou du service)si le numero appele est dans la liste
	         // Cette fonction sert pour la liste des autorises (liste simple 950,955,..)
	         // et pour la liste des reroutes (4000>0511223344,5000>0677448899,...)
	         // et pour la liste des numeros ne passant pas par le SVI pour atteindre le CTI
	         function isNumeroListe(numero, liste)
	         {
	             var tabService = liste.split(',');
	             var tabNumero;
	             for (var i = 0; i < tabService.length; i++) {
	                 tabNumero = tabService[i].split('>');
	                 if (numero == tabNumero[0]) {
	                     if (tabNumero.length == 1)
	                         // cas d'un numero autorise a entrer dans le service
	                         return tabNumero[0];
	                     else
	                         // cas d un numero reroute
	                         return tabNumero[1];
	                 }
	             }
	             // appel sortant
	             return 'false';
	         } //isNumeroListe
	         // Construction du numero de transfert : sip: ...@...
	         // En fonction de la passerelle on peut ne pas avoir le meme format.
	         // par exemple pour dialogic: sip:0556078450@nn.nn.nn.nn
	         //             pour italtel : sip:+33556078450@nn.nn.nn.nn
	         // numeroSortant = numero tel qu'il a deja ete traite par le filtre application.pattern.numeroappele (fonction extraireNumeroService)       
	         // formatPasserelle = format attendu en sortie: sip:+33XXXXXX@nn.nn.nn.nn ou sip:0XXXXXXX@nn.nn.nn.nn
	         function construireNumeroTransfert(numeroSortant, formatPasserelle){
	             var num = numeroSortant;
	             if (numeroSortant.indexOf("@") == -1) {
	                 // pas de @ dans le numero sortant
	                 // on va remplacer XXXX de formatPasserelle par le numero
	                 var reg = new RegExp ("X+");
	                 var result = "";
	                 result = formatPasserelle.replace (reg,num);
	                 return result;
	             }
	             // le numero sortant contient deja une adresse
	             return num;
	         } // construireNumeroTransfert
	         //construireNumeroTransfertCti
	         // remplace les _varNumAppelant de la chaine numDestination par leur valeur
	         function construireNumeroTransfertCti(numDestination, numeroAppelant, patternExtraireNum) {
	             var num = numeroAppelant;
	             if(patternExtraireNum != '') {
	                 var reg2= new RegExp (patternExtraireNum);
	                 if (reg2.test (numeroAppelant)) {
	                     var tmp = reg2.exec (numeroAppelant);
	                     num = tmp[1];
	                 }
	             }
	             var reg3 = new RegExp ("_varNumAppelant","g");
	             var rslt = numDestination.replace (reg3, num);
	             return rslt;
	         } //construireNumeroTransfertCti
	         //DM008: Si le numéro appelé est un numéro à 4 chiffres, alors presenter le numéro court du poste IP (92xx), sinon
	         //si le numéro appelé est dans la liste A, alors presenter le numéro du client appelant le call centre, sinon
	         //    présenter le numéro B
	         // A : paramétré initialement à 99900984 ou +33153634433
	         // B : paramétré initialement à 90009500
	         function getFromSortant(numDestination, numAppelant, listeCti, numDefaut)
	         {
	             var num = numAppelant.replace (':+',':00');
	             if (numDestination.length == 4) {
	               return num;
	             }
	             var tabCti = listeCti.split(',');
	                 for (var i = 0; i < tabCti.length; i++) {
	                     if (numDestination == tabCti[i]) {
	                         return num;
	                     }
	             }
	             // extraction du numero de l'appelant du from pour le remplacer par le numero par defaut
	             var reg=new RegExp("sip:.*@","i");
	             return num.replace(reg,"sip:"+numDefaut+"@");
	           } //getFromSortant
	           
	           function modifierTransferNumber(transferNumber, cdu) {
	               var result = '';
	               var tab = transferNumber.split(";");
	               for (var i=0; i < tab.length; i++) {
	                   if (i>0) result += ';';
	                   if (tab[i].indexOf("oms_segment_client") != -1) {
	                       result+="oms_segment_client="+cdu;
	                   }
	                   else if (tab[i].indexOf("oms_typ") != -1) {
	                       result+="oms_typ=TG";
	                   }
	                   else {
	                       result+=tab[i];
	                   }
	               }

	               return result;
	           }
		]]>
	</script>