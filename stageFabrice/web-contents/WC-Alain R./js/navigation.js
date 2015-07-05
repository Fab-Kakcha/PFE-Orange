var sviJson;
var nomSauve = "";
var navCopie; // item de navigation pour le copier/coler

function Navigation(service, label, type) {
	this.service=service;
	this.label = label;
	this.type = type;
	this.actions="";
}
function Action (action, suivant) {
	this.action=action;
	this.label=suivant;
}
function Affectation (service, label) {
	Navigation.call(this, service, label, "affectation");
	
	this.valeurs="";
	this.statistiques=false;
}
function Bdd (service, label) {
	Navigation.call(this, service, label, "bdd");
	this.nontrouve="";
	this.requete="";
    this.erreur="";
    this.valeur_defaut=""; 
    this.variable="";
    
}
function Condition (service, label) {
	Navigation.call(this, service, label, "condition");
	this.condition="";
	
}
function Deconnexion (service, label) {
	Navigation.call(this, service, label, "deconnexion");
	this.valeur="";
	this.inactivite="";
	this.maxage="";
	this.bargein="";
	this.prompt="";
	this.mode="";
}
function Depart (service, label) {
	Navigation.call(this, service, label, "depart");
}

function Enreg (service, label) {
	Navigation.call(this, service, label, "enreg");
}
function Info (service, label) {
	Navigation.call(this, service, label, "info");
	this.inactivite="";
	this.maxage="";
	this.bargein=true;
	this.prompt="";
	this.mode="";
	this.unparun="";
	this.absorbant="";
}

function Menu (service, label) {
	Navigation.call(this, service, label, "menu");
	
	this.maxage="";
	this.bargein=true;
	this.prompt="";
	this.mode="";
	this.absorbant="";
	
	this.inactivite="";
	this.inactivite_max="";
	this.inactivite_suivant="";
	
	this.erreur_prompt="";
	this.erreur_max="";
	this.erreur_suivant="";

}
function Langue (service, label) {
	Menu.call(this, service, label, "langue");
	this.type="langue";
}
function Prepare (service, label) {
	Navigation.call(this, service, label, "prepare");
	this.erreur="";
	this.filtre="";
}
function Redirection (service, label) {
	Navigation.call(this, service, label, "redirection");
	this.service_redirect="";
}
// ce n'est pas un item de navigation a proprement parler
// il est la pour indiquer que la partie qui suit existe dejà dans 
// l'arbre de navigation
function Retour (service, label) {
	Navigation.call(this, service, label, "retour");	
}
function Saisie (service, label) {
	Navigation.call(this, service, label, "saisie");
}
function Sms (service, label) {
	Navigation.call(this, service, label, "sms");
	this.texte="";
	this.repertoire="";
	this.fichier="";
	this.suivant="";
}

function Statistiques (service, label) {
	Navigation.call(this, service, label, "statistiques");
	this.valeur="";
	this.valeur_type="";
	this.suivant="";
}
function Transfert (service, label) {
	Navigation.call(this, service, label, "transfert");
	this.numero="";
	this.parametre="";

	this.maxage="";
	this.bargein=true;
	this.prompt="";
	this.mode="";
	
	this.suivant="";
}
function Websvc (service, label) {
	Navigation.call(this, service, label, "websvc");
	this.element="";
	this.parametre="";
	this.methode="1";
	this.erreur="";
	this.valeur_defaut=true;
	this.variable="";
	this.url="";
	this.fichier="";
	this.suivant="";
}
/////////////////////////////////////////////////////////////////////////////////////
function ajouterAction (nav, action, suivant) {
	if (nav.actions == undefined || nav.actions == "") nav.actions = new Array();
	nav.actions.push(new Action(action,suivant));
}
/**
 * Recherche l'objet de navigation JSON a partir du label
 * @param label : label de l'objet à chercher
 * @returns
 */
function chercherObjetNavigation(serviceNav, labelNav) {
	for (var n = 0; n < sviJson.navigations.length; n++) {

		if (sviJson.navigations[n].label == labelNav && sviJson.navigations[n].service==serviceNav) {
			return sviJson.navigations[n];
		}
	}
	return null;
}
/**
 * Recherche l'objet de navigation JSON a partir du label
 * @param label : label de l'objet à chercher
 * @returns
 */
function chercherIndiceObjetNavigation(serviceNav, labelNav) {
	for (var n = 0; n < sviJson.navigations.length; n++) {

		if (sviJson.navigations[n].label == labelNav && sviJson.navigations[n].service==serviceNav) {
			return n;
		}
	}
	return -1;
}
/**
 * Creation d'un objet Json du type demandé
 * 
 */
function genererObjetJson(type, service) {	
	
	if (type == 'affectation') {
		return new Affectation(service, type);
	}
	else if (type == 'bdd') {
		return new Bdd(service, type);
	}
	else if (type == 'condition') {
		return new Condition(service, type);
	}
	else if (type == 'deconnexion') {
		return new Deconnexion(service, type);
	}	
	else if (type == 'depart') {
		return new Depart(service, type);
	}
	else if (type == 'enreg') {
		return new Enreg(service, type);		
	}
	else if (type == 'info') {
		return new Info(service, type);
	}
	else if (type == 'langue') {
		return new Langue(service, type);
	}
	else if (type == 'menu') {
		return new Menu(service, type);
	}
	else if (type == 'prepare') {
		return new Prepare(service, type);
	}
	else if (type == 'redirection') {
		return new Redirection(service, type);
	}
	else if (type == 'retour') {
		return new Retour(service, type);
	}
	else if (type == 'saisie') {
		return new Saisie(service, type);
	}
	else if (type == 'sms') {
		return new Sms(service, type);
	}
	else if (type == 'statistiques') {
		return new Statistiques(service, type);
	}
	else if (type == 'transfert') {
		return new Transfert(service, type);
	}
	else if (type == 'websvc') {
		return new Websvc(service, type);
	}
}
/**
 * Verification de la possibilité de l'insertion
 * 
 */
function verifierAvantInsertion(navPere, nav) {	
	
	if (navPere == null) {
		if (nav.type == 'depart') {
			return true;
		}
		else {
			return false;
		}
	}
	
	if (navPere.type == 'redirection' || navPere.type == 'deconnexion' || navPere.type == 'retour') {
		// pas d'insertion possible
		return false;
	}
	if (nav.type == 'retour') {
		// le pere ne doit pas avoir de suivant
		if (navPere.suivant == undefined) {
			if (navPere.actions != "" ) {
				if (navPere.type == 'condition' && navPere.actions.length == 2){
					return false;
				}
				else if (navPere.type == 'menu' && navPere.actions.length == 12){
					return false;
				}
			}
		}
		else {
			if (navPere.suivant != "") {
				return false
			}
		}
	}
	return true;
	
}
/**
 * Ajout d'un item de navigation au format json
 * 
 */
function insererJson(navPere, nav, lien){	
	if (navPere.suivant == undefined) { // condition ou menu

		if (navPere.actions != "") {
			for (var i =0; i < navPere.actions.length; i++) {
				if (navPere.actions[i].action == lien) {
					if (nav.suivant == undefined && nav.actions == "") {
						ajouterAction(nav,lien, navPere.actions[i].suivant);
					}
					else if (nav.suivant != undefined) {
						nav.suivant = navPere.actions[i].suivant;
					}
					break;
				}
			}

			if (i == navPere.actions.length) {
				// l'item n'existe pas dans le tableau actions
				ajouterAction (navPere, lien, nav.label);
			}
			else {
				navPere.actions[i].suivant = nav.label;
			}
		}
		else {
			// le tableau actions est vide
			// l'item n'existe pas dans le tableau actions
			ajouterAction (navPere, lien, nav.label);
		}
	}
	else {	
		
		if (nav.suivant == undefined && nav.actions == "") { // nav est une condition ou un menu
			if (navPere.suivant != "") ajouterAction(nav, '0', navPere.suivant);
		}
		else if (nav.suivant != undefined) {
			nav.suivant = navPere.suivant;			
		}
		navPere.suivant = nav.label;
	}
	if (nav.type != 'retour' && chercherObjetNavigation(nav.service, nav.label) == null)
		sviJson.navigations.push(nav);
}
function insererRacineJson(navNew, label) {
	navNew.service = label;
	navNew.label = label;
	sviJson.racines.push(label);
	sviJson.navigations.push(navNew);
}
/**
 * Ajout d'un item de navigation au format json
 * N'ayant PAS un champ 'suivant'
 */
function insererJsonSansSuivant(service, labelPere, type, labelFils, champSuivantPere){	
		
	var navPere = chercherObjetNavigation(service, labelPere);
	if (labelFils=="") labelFils=type;
	var nav = new Navigation(service, type, labelFils);
	if (type == 'condition') {
		if (champSuivantPere == 'Vrai') {
			nav.suivant_vrai= labelFils;
		}
		else {
			nav.suivant_faux= labelFils;
		}
	}
	else if (type == 'menu' || type == 'langue') {
		ajouterAction(nav, champSuivantPere, labelFils);
	}
	
	sviJson.navigations.push(nav);
}
/**
 * Suppression de toute une hiérarchie
 * @param service
 * @param labelPere
 * @param label
 */
function supprimerHierarchieJson (service, labelPere, label) {
	 if (labelPere != "") {
		 var indicePere = chercherIndiceObjetNavigation(service, labelPere);
		 var navPere = sviJson.navigations[indicePere];
		 var indice = chercherIndiceObjetNavigation(service, label);
		 var nav = sviJson.navigations[indice];
	  
	 	
		  if (navPere.suivant == undefined) {
			  if (navPere.actions != undefined) {
				  for (var i = 0; i < navPere.actions.length; i++) {
					  if (navPere.actions[i].suivant == label) {
						  navPere.actions[i].suivant = "";
						  break;
					  }
				  }
			  }
		  }
		  else {
			  navPere.suivant = "";
		  }
	  	
	  	supprimerIndiceJson(service, label,indice);
	  }
	 else {
		 // suppression de la racine
		 for (var i = 0; i < sviJson.racines.length; i++) {
			 if (label == sviJson.racines[i]) {
				 sviJson.racines.splice(i,1);
				 break;
			 }
		 }
		 var indice = chercherIndiceObjetNavigation(service, label);
		 supprimerIndiceJson(service, label, indice);
	 }
}
function supprimerUniqueJson(service, labelPere, label) {
	 if (labelPere != "") {
		 var indicePere = chercherIndiceObjetNavigation(service, labelPere);
		 var navPere = sviJson.navigations[indicePere];
		 var indice = chercherIndiceObjetNavigation(service, label);
		 var nav = sviJson.navigations[indice];
		 
		 // recherche du suivant de l'item à supprimer
		 // pour le rarrocher au pere
		 var suivant = "";
		 if (nav.suivant != undefined) {
			 suivant = nav.suivant;
		 }
		 else {
			 if (nav.actions != undefined && nav.actions != "") {
				 for (var i = 0; i < nav.actions.length; i++) {
					 if (nav.actions[i].suivant != undefined && nav.actions[i].suivant != "") {
						 suivant = nav.actions[i].suivant;
						 break;
					 } 
				 }
			 } 
		 }
	 	
		  if (navPere.suivant == undefined) {
			  if (navPere.actions != undefined && navPere.actions != "") {
				  for (var i = 0; i < navPere.actions.length; i++) {
					  if (navPere.actions[i].suivant == label) {
						  navPere.actions[i].suivant = suivant;
						  break;
					  }
				  }
			  }
		  }
		  else {
			  navPere.suivant = suivant;
		  }
	  	
	  	supprimerIndiceJson(service, label, indice);
	  }
	 else {
		 // suppression de la racine
		 for (var i = 0; i < sviJson.racines.length; i++) {
			 if (label == sviJson.racines[i]) {
				 sviJson.racines.splice(i,1);
				 break;
			 }
		 }
		 var indice = chercherIndiceObjetNavigation(service, label);
		 supprimerIndiceJson(service, label, indice);
	 }
}
function supprimerIndiceJson(service, label, indice) {
	// il faut verifier que l'item a supprimer n'est pas utilisé plusieurs fois
	for (var i=0; i < sviJson.navigations.length; i++) {
		if (sviJson.navigations[i].service == service) {
			if (sviJson.navigations[i].suivant != undefined 
				&& sviJson.navigations[i].suivant == label) {
				// on a trouve un item qui reference l'objet à supprimer
				return;
			}
			else if (sviJson.navigations[i].actions != undefined 
				&& sviJson.navigations[i].actions != "") {
				for (var j = 0; j < sviJson.navigations[i].actions.length; j++) {
					if (sviJson.navigations[i].actions[j].suivant == label) {
						// on a trouve un item qui reference l'objet à supprimer
						return;
					}
				}
			}
			
		}
	}
	// on n'a pas trouve d'objet referançant l'item, on le supprime
	sviJson.navigations.splice(indice,1);
}
/**
 * cherche le suivant d'une action
 * @param nav
 * @param action
 */
function donnerNavlabelActions(nav, action) {
	if (nav.suivant == undefined) {
		for (var i =0; i < nav.actions.length; i++) {
			if (nav.actions[i].action == action) {
				return nav.actions[i].suivant;
			}
		}		
		return null;
	}
	else {
		return nav.suivant;
	}
}
/**
 * Determine si le codeAction existe dans le tableau des actions (menu ou condition)
 * @param navPere
 * @param codeAction
 * @returns {Boolean}
 */
function verifierAction(navPere, codeAction){	
	if (navPere.actions != "") {
		for (var i =0; i < navPere.actions.length; i++) {
			if (navPere.actions[i].action == codeAction) {
				return true;
			}
		}
	}
	return false;
}
function clone(srcInstance)
{
	/*Si l'instance source n'est pas un objet ou qu'elle ne vaut rien c'est une feuille donc on la retourne*/
	if(typeof(srcInstance) != 'object' || srcInstance == null)
	{
		return srcInstance;
	}
	/*On appel le constructeur de l'instance source pour crée une nouvelle instance de la même classe*/
	var newInstance = srcInstance.constructor();
	/*On parcourt les propriétés de l'objet et on les recopies dans la nouvelle instance*/
	for(var i in srcInstance)
	{
		newInstance[i] = clone(srcInstance[i]);
	}
	/*On retourne la nouvelle instance*/
	return newInstance;
}
function clonerHierarchie (navPere) {
	if (navPere == null) return;

	if (navPere.suivant != undefined) {
		
		var nav = chercherObjetNavigation(navCopie.service, navPere.suivant);
		if (nav == null) return;
		var navClone = clone(nav);
		navClone.service = navPere.service;
		
		// recherche qu'un element n'existe pas deja avec ce label
		var tmp;
		var i;
		for ( i = 1; i > 0 ;i++) {
			tmp = chercherObjetNavigation(navPere.service, navClone.label + " - Copie ("+i+")" );
			if (tmp == null) break;
		}
		
		navClone.label += " - Copie ("+i+")";
		navPere.suivant = navClone.label;
		sviJson.navigations.push(navClone);
		clonerHierarchie (navClone);
	}
	else if (navPere.actions != undefined) {
		for (var i = 0; i < navPere.actions.length; i++) {
			var nav = chercherObjetNavigation(navCopie.service, navPere.actions[i].suivant);
			if (nav == null) return;
			var navClone = clone(nav);
			navClone.service = navPere.service;
			// recherche qu'un element n'existe pas deja avec ce label
			var tmp;
			var j;
			for ( j = 1; j > 0 ;j++) {
				tmp = chercherObjetNavigation(navPere.service, navClone.label + " - Copie ("+j+")" );
				if (tmp == null) break;
			}
			navClone.label += " - Copie ("+j+")";
			navPere.actions[i].suivant = navClone.label;
			sviJson.navigations.push(navClone);
			clonerHierarchie (navClone);
		}
	}
}