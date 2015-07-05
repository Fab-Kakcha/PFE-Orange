tabAttribut=[
             {"nom" :"label", "element":"INPUT", "classe":"inputText"},  
             {"nom" :"requete","element":"TEXTAREA", "classe":"inputTextArea"},
             {"nom" :"prompt","element":"INPUT", "classe":"inputText"}, 
             {"nom" :"condition","element":"INPUT", "classe":"inputText"}, 
             {"nom" :"maxage","element":"INPUT", "classe":"inputText"},
             {"nom" :"inactivite","element":"INPUT", "classe":"inputText"},
             {"nom" :"erreur_prompt",  "element":"INPUT", "classe":"inputText"}    ,   
             {"nom" :"inactivite_max","element":"INPUT","classe": "inputText"},        
             {"nom" :"erreur_max","element":"INPUT","classe": "inputText"},         
             {"nom" :"bargein","element":"SELECT", "classe":"inputSelect", "donnees":["true", "false"]},       
             {"nom" :"valeurs","element":"INPUT", "classe":"inputText"},
             {"nom" :"inactivite", "element":"INPUT", "classe":"inputText"},          
             {"nom" :"unparun", "element":"SELECT", "classe":"inputSelect", "donnees":["true", "false"]},          
             {"nom" :"absorbant","element":"SELECT","classe": "inputSelect", "donnees":["true", "false"]},
             {"nom" :"mode",  "element":"SELECT","classe": "inputSelect", "donnees":["LANGUE", "NORMAL", "VOCALISATION"]},       
             {"nom" :"valeur_defaut", "element": "INPUT","classe": "inputText"},     
             {"nom" :"variable","element": "INPUT", "classe":"inputText"},
             {"nom" :"filtre","element":"INPUT", "classe":"inputText"},
             {"nom" :"element","element":"INPUT", "classe":"inputText"},
             {"nom" :"methode","element":"INPUT", "classe":"inputText"},
             {"nom" :"url","element":"INPUT", "classe":"inputText"},
             {"nom" :"parametre","element":"INPUT", "classe":"inputText"},
             {"nom" :"parametre_fichier" ,"element": "INPUT", "classe":"inputText"},
             {"nom" :"langue" ,"element": "INPUT", "classe":"inputText"},
             {"nom" :"tempo" ,"element": "INPUT", "classe":"inputText"},
             {"nom" :"statistiques","element":"SELECT", "classe":"inputSelect", "donnees":["true", "false"]},
             ];
var repImg="img/";
var iconFlche = "fleche0.png";
var iconFlcheBas = "fleche1.png";
var iconFlcheVide = "vide.png";
var iconFlcheGche = "fleche2.png";
var iconRetour = "retour.png";
var iconRenvoi = "asuivre.png";
var iconOups = "oups.png";
var COPIER=1;
var COUPER=2;

var domlabelParam = null; // param à colorier quand on clique dessus
var rangMax = 0;
//variables pour le copier/coller
var nodeCopie="";
var actionCopie="";
var actionMode=""
	/**
	 * ajoute un noeud dans l'arbre du SVI
	 * @param nodePere = noeud (DIV) du DOM courant (pere de l'item à ajouter)
	 * @param labelNav = label de l'element de navigations à ajouter
	 */
	function ajouterNoeud (nodePere, labelNav) {
	return ajouterNoeudTxt (nodePere, labelNav, "");
}
function ajouterNoeudTxt (nodePere, labelNav, texte) {
	var serviceNav = donnerServiceNoeud(nodePere);
	var boolRacine = false;
	if (serviceNav == "000") {
		// on est a la racine
		serviceNav = labelNav;
		boolRacine = true;
	}
	var rang = eval(donnerRangNoeud(nodePere)) + 1;
	if (rang > rangMax) rangMax=rang;
	var jsonNav = chercherObjetNavigation(serviceNav, labelNav);
	var iconeSpecifique = null;
	var iconeSuplementaire = null;
	if (jsonNav == null) {
		// on n'a pas trouve d'objet json
		// c'est peut-etre un renvoi dynamique
		if (labelNav.substring(0,4) == "_var")	{
			var labelNavReel = chercherAffectation(nodePere, serviceNav, labelNav);
			if (labelNavReel != "") {
				labelNav=labelNavReel;
				jsonNav = chercherObjetNavigation(serviceNav, labelNav);
				iconeSuplementaire = iconRenvoi;
			}
			else iconeSpecifique = iconRenvoi;
		}
		else iconeSpecifique = iconOups;
	}


	// recherche de l'existence de ce noeud dans la hierarchie du DOM 
	var nodeTrouve = chercherElementDomHierarchie(serviceNav, labelNav);
	if (nodeTrouve != null) {
		if (nodePere == nodeTrouve.parent) return nodeTrouve; // le noeud a deja ete insere sur cet item
	}
	// div global va contenir le div de dessin du noeud + div de l'expand 
	var div = document.createElement("DIV");
	div.setAttribute("class",'divArchi');
	div.setAttribute('id', serviceNav+'_d_'+rang+'_'+labelNav);
	if (boolRacine) {
		// pour la racine on ne prend pas le style du css
		//div.style.margin="0px 0px 0px 5px";
		div.setAttribute("class",'divArchi1');
	}
	else {
		div.setAttribute("class",'divArchi');
	}
	div.ondrop=drop;
	div.ondragover=allowDrop;
	div.draggable=false;

	var ssdiv = document.createElement("DIV");
	ssdiv.setAttribute("class",'divSsArchi');
	ssdiv.setAttribute('id', serviceNav+'_s_'+rang+'_'+labelNav);
	ssdiv.draggable=false;

	var fleche = document.createElement("IMG");
	if (isNoeudFinal(jsonNav) || nodeTrouve != null) {
		fleche.setAttribute("src",repImg+iconFlcheVide);
		fleche.setAttribute("class",'flecheArchiVide');
		fleche.setAttribute('id',serviceNav+'_f_'+rang+'_'+labelNav);		
	}
	else {
		fleche.setAttribute("src",repImg+iconFlche);
		fleche.setAttribute("class",'flecheArchi');
		fleche.setAttribute('id',serviceNav+'_f_'+rang+'_'+labelNav);
		fleche.onclick=onClickFleche;
	}

	ssdiv.appendChild(fleche);

	var img = document.createElement("IMG");
	if (nodeTrouve != null) {
		// l'element existe dans la hierarchie
		img.setAttribute("src",repImg+iconRetour);
		img.setAttribute('id',serviceNav+'_i_'+rang+'_'+labelNav);
		img.setAttribute("class",'imgArchi2');
	}
	else {
		if (iconeSpecifique != null) {
			img.setAttribute("src", repImg+iconeSpecifique);
			img.setAttribute("class",'imgArchi2');
		}
		else {
			img.setAttribute("src", donnerIcone(jsonNav));
			img.setAttribute("class",'imgArchi');
			img.onclick=onClickLabel;
			img.addEventListener("contextmenu",menuCtx,false);
		}
		img.setAttribute('id',serviceNav+'_i_'+rang+'_'+labelNav);

	}
	ssdiv.appendChild(img);	
	if (iconeSuplementaire != null){
		var img2 = document.createElement("IMG");
		img2.setAttribute("src",repImg+iconeSuplementaire);
		img2.setAttribute("class",'imgArchi2');
		img2.onclick=onClickLabel;
		ssdiv.appendChild(img2);	

	}

	var label = document.createElement("LABEL");
	label.setAttribute("class",'labelArchi');;
	label.setAttribute('id',serviceNav+'_l_'+rang+'_'+labelNav);
	label.ondbleclick=onClickFleche;
	label.onclick=onClickLabel;
	label.addEventListener("contextmenu",menuCtx,false);

	var t = document.createTextNode(texte+labelNav);
	label.appendChild(t);

	ssdiv.appendChild(label);

	div.appendChild(ssdiv);
	nodePere.appendChild(div);
	return div;

}

/**
 * renvoie le nom de l'icone à afficher pour cet objet de navigations json
 * @param jsonNav
 * @returns {String}
 */
function donnerIcone(jsonNav) {
	return repImg+jsonNav.type+".png";
}

function onClickFleche() {
	var icone = this.src.substring(this.src.lastIndexOf('/')+1);
	var idDiv = this.id.replace("_f_","_d_"); // id du noeud DIV pere
	var nodePere = document.getElementById(idDiv);

	if (icone == iconFlcheVide) {
		return;
	}
	else if (icone == iconFlche) {
		// on developpe le noeud
		this.setAttribute("src",repImg+iconFlcheBas); // changement de visuel de la fleche
		var labelNav =  donnerLabelNoeud(this);	
		var serviceNav = donnerServiceNoeud(this);
		var jsonNav = chercherObjetNavigation(serviceNav, labelNav);
		developperNoeud(nodePere, jsonNav);

	}
	else {
		// suppression du developpement
		this.setAttribute("src",repImg+iconFlche);
		var tabDiv = nodePere.childNodes;		
		var nbFils = tabDiv.length;
		for(var i = nbFils -1; i > 0; i--){
			if (i > 0) {
				nodePere.removeChild(tabDiv[i]);

			}
		}
	}
}
/**
 * gere l'insertion d'un noeud
 * le nouveau noeud a deja ete insere
 * @param nodePere
 * @param jsonNav
 * @returns
 */
function gererInsertionNoeud(nodePere, nodeNew, navPere, navNew) {
	var nodeSup = null;
	var label = "";
	if (nodePere.childNodes.length > 1) {
		// le noeud a deja été développé
		// on cherche s'il y a un noeud a supprimer
		// le child 0 est le div d'image du nodePere

		for (var d= 1; d < nodePere.childNodes.length; d++) {
			var n =nodePere.childNodes[d];
			label = donnerLabelNoeud(n);
			if (label != "Oups") { // c'est le menu de saisi
				var trouve = false;

					for (var a=0; a < navPere.actions.length; a++) {
						if (label == navPere.actions[a].label.replace(/\(.+\) /,'')) { // on enleve (Vrai) (Faux) (0), ...
							// le label a ete trouve dans le json donc pas supprime
							trouve = true;
							break;
						}
					}
				
				if (!trouve) {
					nodeSup = n;//document.getElementById(service+"_d_"+rang+"_"+label);
					break;
				}
			}

		}
		if(nodeSup != null) {
			// un noeud a ete supprime
			nodeNew.appendChild(nodeSup);
			//nodePere.removeChild(nodeSup);
			// on recupere le Noeud label du noeud supprime pour enlever les (...)
			var nodeLabelSup = document.getElementById(nodeSup.childNodes[0].childNodes[2].id);	
			renumeroterRangNoeud(nodeSup, eval(donnerRangNoeud(nodePere)) +1);


			if (navNew.type == 'condition') {
				nodeLabelSup.innerHTML = "(Faux) "+label;
			}
			else if (navNew.type == 'menu') {
				nodeLabelSup.innerHTML = "(0) "+label;
			}
			else {				
				nodeLabelSup.innerHTML = label;
			}
		}
	}
	else {
		developperNoeud(nodePere, navPere);
	}
	return;
}

function developperNoeud(nodePere, jsonNav) {

	if (jsonNav.actions != undefined && jsonNav.actions != ""){
		if (jsonNav.actions == undefined || jsonNav.actions == "") return;
		if (jsonNav.type == "menu") {
			// on ordonne les DTMF
			for (var i =0; i <10; i++) {
				for (var j = 0; j < jsonNav.actions.length; j++) {
					if (jsonNav.actions[j].action == i) {
						ajouterNoeudTxt(nodePere, jsonNav.actions[j].label, "("+jsonNav.actions[j].action+") ");
						break;
					}
				}
			}
			// on affiche le reste
			var reg = new RegExp("[0-9]+");
			for (var j = 0; j < jsonNav.actions.length; j++) {
				if (!jsonNav.actions[j].action.match(reg)) {
					ajouterNoeudTxt(nodePere, jsonNav.actions[j].label, "("+jsonNav.actions[j].action+") ");
				}
			}
		}
		else {
			for (var j = 0; j < jsonNav.actions.length; j++) {
					if (jsonNav.actions[j].action == "suivant") {
						ajouterNoeud(nodePere, jsonNav.actions[j].label);
					}
					else {
						ajouterNoeudTxt(nodePere, jsonNav.actions[j].label, "("+jsonNav.actions[j].action+") ");
					}
				
			}
		}
	}
	
}
/**
 * Ajuste la fleche devant l'icone en fonction du json
 * et de la navigation visualisee (noeud deploye)
 * @param node
 * @param nav
 */
function ajusterFleche(node, nav) {
	var fleche = node.childNodes[0].childNodes[0];
	if (isNoeudFinal(nav)) {
		fleche.setAttribute("src",repImg+iconFlcheVide);
	}
	else {
		if (node.childNodes[1] == undefined) 
			fleche.setAttribute("src",repImg+iconFlche);
		else fleche.setAttribute("src",repImg+iconFlcheBas);
		fleche.onclick=onClickFleche;
	}
}
function onClickLabel(){
	if (domlabelParam != null) {
		domlabelParam.style.color="black";
	}
	var id = this.id.replace("_i_","_l_");
	domlabelParam = document.getElementById(id);
	domlabelParam.style.color="#FF5500";

	var parametre = document.getElementById("parametre");
	// on vide
	var tab = parametre.childNodes;		
	var nbFils = tab.length;
	for(var i = nbFils -1; i >= 0; i--){

		parametre.removeChild(tab[i]);

	}
	var label = donnerLabelNoeud(this);
	var service = donnerServiceNoeud(this);
	var jsonNav = chercherObjetNavigation(service, label);

	afficher(jsonNav, parametre);

}
/**
 * donne le rang dans l'arbre
 * @param domElmt = element DOM 
 * @returns rang
 */
function donnerRangNoeud(domElmt) {
	var r = new RegExp("^.+_._([0-9]+)_.*");
	var rslt = r.exec(domElmt.id);
	if (rslt == null) return 0;
	else return rslt[1];
}
function donnerLabelNoeud(domElmt) {
	var r = new RegExp("^.+_._[0-9]+_(.*)");
	var rslt = r.exec(domElmt.id);
	if (rslt == null || rslt.length < 2) return "Oups";
	return rslt[1];
}
function donnerServiceNoeud(domElmt) {
	var r = new RegExp("^(.+)_._[0-9]+_.*");
	var rslt = r.exec(domElmt.id);
	if (rslt == null || rslt.length < 2) return "Oups";
	return rslt[1];
}
function donnerTypeNoeud(domElmt) {
	var r = new RegExp("^.+_(.)_[0-9]+_.*");
	var rslt = r.exec(domElmt.id);
	if (rslt == null || rslt.length < 2) return "Oups";
	return rslt[1];
}
function isNoeudNavigation(id) {
	var r = new RegExp("^.+_._[0-9]+_.*");
	var rslt = r.exec(id);
	if (rslt == null) return false;
	return true;
}
function donnerIdDiv(domElmt) {

	return domElmt.id.replace(/_._/,"_d_");
}
/**
 * determine si le noeud peud etre developpé ou pas
 */
function isNoeudFinal(jsonNav) {
	if (jsonNav == null) return true;
	if (jsonNav.type == "deconnexion" || jsonNav.type == "redirection") { 			
		return true;
	}
	if (jsonNav.actions == undefined) return true;
	if (jsonNav.actions == "") return true;
	
	return false;
}
/**
 * determine si le label de navigation est dans sa hierachie DOM
 * @param domElmt = element du DOM
 * @param labelNav = label de navigation
 * @returns node
 */
function chercherElementDomHierarchie(service, labelNav) {
	var id;
	var node = null;
	for (var i=1;i<=rangMax;i++) {
		id=service+"_d_"+i+"_"+labelNav;
		node = document.getElementById(id);
		if (node != null) {
			return node;
		}
	}
	return null;

}
/**
 * cherche l'item de navigation dans la hierarchie DOM qui a affectee
 * la variable contenue par labelNav
 * @param domElmt
 * @param serviceNav
 * @param labelNav
 * @returns
 */
function chercherAffectation(domElmt, serviceNav, labelNav) {
	if (donnerRangNoeud(domElmt) <= 1) return ""; // on est arrivée à la racine
	var label = donnerLabelNoeud(domElmt);
	var jsonNav = chercherObjetNavigation(serviceNav, label);
	if (jsonNav.type == "affectation") {
		var r = new RegExp(labelNav+"[ ]*=([^,]*)");
		var rslt = r.exec(jsonNav.valeurs);
		if (rslt != null) return rslt[1];
	}
	return chercherAffectation(domElmt.parentNode, serviceNav,labelNav);

}
function afficher(jsonNav, parametre) {
//	for (var i in jsonNav) {
//	alert(i +"="+jsonNav[i]);
//	}
	// parcourt de tous les attributs possibles
	for (var i in tabAttribut) {

		if (jsonNav[tabAttribut[i].nom] != undefined) {
			// l'attribut fait partie de l'objet
			var label = document.createElement("P");
			label.setAttribute("class",'labelParam');
			var t = document.createTextNode(tabAttribut[i].nom);
			label.appendChild(t);
			parametre.appendChild(label);

			var input = document.createElement(tabAttribut[i].element);
			input.setAttribute("class",tabAttribut[i].classe);
			if (tabAttribut[i].element == "TEXTAREA") {

				input.setAttribute("cols","50");
				input.setAttribute("rows","20");
				var t = document.createTextNode(jsonNav[tabAttribut[i].nom]);
				input.appendChild(t);

			}
			else if (tabAttribut[i].element == "SELECT") {
				for (var j in tabAttribut[i].donnees) {
					var opt = document.createElement("OPTION");
					opt.text = tabAttribut[i].donnees[j];
					input.options.add(opt);

				}
			}
			else {
				input.setAttribute("type",'text');
				input.setAttribute("value",jsonNav[tabAttribut[i].nom]);				
			}
			parametre.appendChild(input);
		}
	}
}


function coller(evt, nodePere, navPere) {

	var rang = donnerRangNoeud(nodePere);

	var nav = clone(navCopie); // objet nouveau à inserer
	nav.service = service;

	if (verifierAvantInsertion(navPere, nav)) {
		saisirLabel(evt, nodePere, navPere, nav, rang);
	}
	else {
		alert("Operation interdite");
		return;
	}  
}

/**
 * Suppression du div contenant l'element et toute sa hiérarchie
 * @param element
 */
function supprimerHierarchie(domElmt)
{
	var id = donnerIdDiv(domElmt); // Il faut l'id du DIV (_d_)
	var div = document.getElementById(id);
	var nodePere = div.parentNode;
	var service = donnerServiceNoeud(div);
	var label = donnerLabelNoeud(div);
	var labelPere = donnerLabelNoeud(nodePere);
	var servicePere = donnerServiceNoeud(nodePere);


	nodePere.removeChild(div);

	if (service != servicePere) {
		// on supprime la racine
		supprimerHierarchieJson(service, "", label);
	}
	else {
		supprimerHierarchieJson(service, labelPere, label);
		ajusterFleche(nodePere, chercherObjetNavigation(servicePere, labelPere));
	}

}
/**
 * Suppression du div contenant l'element
 * @param element
 */
function supprimerUnique(domElmt)
{
	var id = donnerIdDiv(domElmt); // Il faut l'id du DIV (_d_)
	var div = document.getElementById(id);
	var nodePere = div.parentNode;
	var service = donnerServiceNoeud(div);
	var label = donnerLabelNoeud(div);
	var labelPere = donnerLabelNoeud(nodePere);
	var servicePere = donnerServiceNoeud(nodePere);
	// le premier fils est le div de representation
	// le second est le noeud suivant
	// on ne supprime pas s'il y a plusieurs suivant (menu condition)
	// on ne supprime pas la racine
	if (div.childNodes.length > 2 || service != servicePere ) {
		alert("Impossible de supprimer");
		return; 
	}


	var divFils = div.childNodes[1];
	if (divFils != null) nodePere.appendChild(divFils);
	nodePere.removeChild(div);
	renumeroterRangNoeud(divFils, eval(donnerRangNoeud(nodePere))+1);
	supprimerUniqueJson(service, labelPere, label);
	ajusterFleche(nodePere, chercherObjetNavigation(servicePere, labelPere));	

}

function menuCtx(evt)
{
	var x = document.getElementById('ctxmenu1');
	if(x) x.parentNode.removeChild(x);
	x = document.getElementById('saisieInit');
	if(x) x.parentNode.removeChild(x);

	var element = evt.target;
	var d = document.createElement('div');
	d.setAttribute('class', 'ctxmenu');
	d.setAttribute('id', 'ctxmenu1');
	element.parentNode.appendChild(d);
	d.style.left = evt.clientX + window.pageXOffset + "px";
	d.style.top = evt.clientY + window.pageYOffset + "px";
	d.onmouseover = function(e) { this.style.cursor = 'pointer'; }
	d.onclick = function(e) { element.parentNode.removeChild(d);  }
	document.body.onclick = function(e) { 
		var x = document.getElementById('ctxmenu1');
		if (x) element.parentNode.removeChild(x);  }

	var p = document.createElement('p');
	d.appendChild(p);
	p.onclick=function() { nodeCopie = element.parentNode.parentNode;
	var serviceOrig = donnerServiceNoeud(nodeCopie);
	var labelOrig = donnerLabelNoeud(nodeCopie);
	navCopie  = chercherObjetNavigation(serviceOrig, labelOrig);
	actionCopie=COPIER;
	};
	p.setAttribute('class', 'ctxline');
	p.innerHTML = "Copier";

	p = document.createElement('p');
	d.appendChild(p);
	p.onclick=function() { nodeCopie = element.parentNode.parentNode;
	var serviceOrig = donnerServiceNoeud(nodeCopie);
	var labelOrig = donnerLabelNoeud(nodeCopie);
	navCopie  = chercherObjetNavigation(serviceOrig, labelOrig);
	actionCopie=COUPER; }; 
	p.setAttribute('class', 'ctxline');
	p.innerHTML = "Couper";

	/////////// Coller unique
	p = document.createElement('p');
	d.appendChild(p);
	p.onclick=function(evt) { 
		evt.preventDefault();
		evt.stopPropagation();
		var x = document.getElementById('ctxmenu1');
		if(x) x.parentNode.removeChild(x);

		if (nodeCopie == null) return;
		if (element.parentNode != undefined && element.parentNode.parentNode != undefined) {
			var nodePere = element.parentNode.parentNode;
			var service = donnerServiceNoeud(nodePere);	
			var labelPere = donnerLabelNoeud(nodePere);
			var navPere = chercherObjetNavigation(service, labelPere);
			actionMode ="U";
			coller(evt, nodePere, navPere);
		}

	}; 
	p.setAttribute('class', 'ctxline');
	p.innerHTML = "Coller (Unique)";

	//////////// coller Hiérarchie
	p = document.createElement('p');
	d.appendChild(p);
	p.onclick=function(evt) { 
		evt.preventDefault();
		evt.stopPropagation();
		var x = document.getElementById('ctxmenu1');
		if(x) x.parentNode.removeChild(x);

		if (nodeCopie == null || navCopie == null) return;
		if (element.parentNode != undefined && element.parentNode.parentNode != undefined) {
			var nodePere = element.parentNode.parentNode;
			var service = donnerServiceNoeud(nodePere);	
			var labelPere = donnerLabelNoeud(nodePere);
			var navPere = chercherObjetNavigation(service, labelPere);

			if ((navPere.suivant != undefined && navPere.suivant != "") 
					|| (navPere.actions != undefined && navPere.actions != "")) { 
				actionMode = "";
				alert("		Operation impossible\n\nl'item père ne doit pas avoir de hiérarchie"); 

			}
			else {
				actionMode +="H";
				coller(evt, nodePere, navPere);
			}
		}
	};
	p.setAttribute('class', 'ctxline');
	p.innerHTML = "Coller (Hiérarchie)";


	p = document.createElement('p');
	d.appendChild(p);
	p.onclick=function() { supprimerUnique(element) }; 
	p.setAttribute('class', 'ctxline');
	p.innerHTML = "Supprimer (Unique)";

	p = document.createElement('p');
	d.appendChild(p);
	p.onclick=function() { supprimerHierarchie(element) }; 
	p.setAttribute('class', 'ctxline');
	p.innerHTML = "Supprimer (Hiérarchie)";
	return false;
}

//Gestion du drag and drop

function allowDrop(ev) {
	ev.preventDefault();
}

function drag(ev) {
	ev.dataTransfer.setData("idDrag", ev.target.id); //nom de l'icone
}

function drop(evt) {
	evt.preventDefault();
	evt.stopPropagation();
	var type = evt.dataTransfer.getData("idDrag");

	if (type =="") {
		return;
	}
	var service;	
	var labelPere;
	var rang ;
	var navPere;
	var nav;
	if (type == 'depart') {
		service = 'depart';	
		labelPere ="";
		rang = 1;
		navPere = null;
		nav = genererObjetJson(type,service); // objet nouveau à inserer
	} 
	else {
		service = donnerServiceNoeud(evt.target);	
		labelPere = donnerLabelNoeud(evt.target);
		rang = donnerRangNoeud(evt.target);
		navPere = chercherObjetNavigation(service, labelPere);

		nav = genererObjetJson(type,service); // objet nouveau à inserer
	}
	if (verifierAvantInsertion(navPere, nav)) {
		var nodePere = donnerNodePere(navPere, rang);
		saisirLabel(evt,nodePere, navPere, nav, rang);
	}
	else {
		alert("Operation interdite");
		return;
	}   

}
/**
 * Donne le node pere en fonction des données json
 * @param navPere
 * @returns
 */
function donnerNodePere(navPere, rang) {
	var nodePere;
	if (navPere == null) {
		// c'est un nouveau depart
		nodePere = document.getElementById("000_d_0_archi");
	}
	else {
		nodePere = document.getElementById(navPere.service+"_d_"+rang+"_"+navPere.label);
	}
	if (nodePere.childNodes.length == 1 && navPere != null) {
		// le noeud n'a pas ete développé
		developperNoeud(nodePere, navPere);
	}
	return nodePere;
}
/**
 * Sur le drop permet de saisir les données initiales
 * du nouvel item de navigation dans une nouvelle fenetre
 */
function saisirLabel(evt,nodePere, navPere, navNew, rang) {
	var x = document.getElementById('saisieInit');
	if(x) x.parentNode.removeChild(x);

	var d = document.createElement('FORM');
	d.setAttribute('name', 'saisieInit');
	d.setAttribute('class', 'saisieInit');
	d.setAttribute('id', 'saisieInit');
	nodePere.appendChild(d);

	if (evt == null) {
		d.style.left =window.innerWidth /2;
		d.style.top = window.innerHeight/2;
	}
	else {
		d.style.left = evt.clientX +20+ window.pageXOffset + "px";
		d.style.top = evt.clientY + window.pageYOffset + "px";
	}
	d.onclick = function(e) {			  
		e.stopPropagation();
	}


	document.body.onclick = function(e) { 
		var x = document.getElementById('saisieInit');		  
		if (x) {
			nodePere.removeChild(x); 
		}
	}
	if (navPere != null) {
		var img = document.createElement("IMG");
		img.setAttribute("src", donnerIcone(navPere));
		img.setAttribute("class",'saisieInitImg');	
		d.appendChild(img);
	}

	var img = document.createElement("IMG");
	img.setAttribute("src", repImg+iconFlcheGche);
	img.setAttribute("class",'saisieInitImg');	
	d.appendChild(img);

	var img = document.createElement("IMG");
	img.setAttribute("src", donnerIcone(navNew));
	img.setAttribute("class",'saisieInitImg');	
	d.appendChild(img);

	var p = document.createElement('p');
	d.appendChild(p);
	p.setAttribute('class', 'saisieInitP');
	p.innerHTML = "Label";

	var l = document.createElement('INPUT');
	l.setAttribute('class', 'saisieInitI');
	l.setAttribute('type', 'text');
	l.setAttribute('value',navNew.label);
	d.appendChild(l);
	if (navPere != null) {
		if(navPere.type == 'condition') {

			p = document.createElement('p');
			p.setAttribute('class', 'saisieRadio');
			p.innerHTML = "Vrai"; 
			d.appendChild(p);
			var rv = document.createElement("INPUT");
			rv.setAttribute("type", "radio");
			rv.setAttribute("name", "saisieRadio");
			rv.setAttribute("id", "condVrai");
			rv.setAttribute("checked",true);
			rv.setAttribute("value","1");
			p.appendChild(rv);

			p = document.createElement('p');		  
			p.setAttribute('class', 'saisieRadio');
			p.innerHTML = "Faux";
			d.appendChild(p);
			var rf = document.createElement("INPUT");
			rf.setAttribute("type", "radio");
			rf.setAttribute("name", "saisieRadio");
			rf.setAttribute("id", "condFaux");
			rf.setAttribute("value","0");
			p.appendChild(rf);
		}
		else if (navPere.type == 'menu') {
			var rv;
			for (var i =0; i<10; i++) {
				p = document.createElement('p');
				p.setAttribute('class', 'saisieRadio');
				p.innerHTML = i; 
				d.appendChild(p);
				rv = document.createElement("INPUT");
				rv.setAttribute("type", "radio");
				rv.setAttribute("name", "saisieRadio");
				if (i==0) rv.setAttribute("checked",true);
				else rv.setAttribute("checked",false);
				rv.setAttribute("value",i);
				p.appendChild(rv);
			}
			p = document.createElement('p');
			p.setAttribute('class', 'saisieRadio');
			p.innerHTML = '*'; 
			d.appendChild(p);
			rv = document.createElement("INPUT");
			rv.setAttribute("type", "radio");
			rv.setAttribute("name", "saisieRadio");
			rv.setAttribute("checked",false);
			rv.setAttribute("value",'*');
			p.appendChild(rv);

			p = document.createElement('p');
			p.setAttribute('class', 'saisieRadio');
			p.innerHTML = '#'; 
			d.appendChild(p);
			rv = document.createElement("INPUT");
			rv.setAttribute("type", "radio");
			rv.setAttribute("name", "saisieRadio");
			rv.setAttribute("checked",false);
			rv.setAttribute("value",'#');
			p.appendChild(rv);
		}
	}

	var d2 = document.createElement('div');
	d2.setAttribute('class', 'saisieInitD');
	d2.setAttribute('id', 'saisieInitD');
	d.appendChild(d2);


	p = document.createElement('INPUT');
	p.setAttribute('class', 'saisieInitB');
	p.setAttribute('type', 'button');
	p.setAttribute('value', 'OK');
	p.onclick = function (e) {
		// insertion du noeud
		gererInsertionNouveauNoeud(l, nodePere, navPere, navNew);

	}
	d2.appendChild(p);

	p = document.createElement('INPUT');
	p.setAttribute('class', 'saisieInitB');
	p.setAttribute('type', 'button');
	p.setAttribute('value', 'Annuler');
	p.onclick = function (e) {
		navNew.label = ""; 
		nodePere.removeChild(d);
	}
	d2.appendChild(p);


	// gestion des touches sur le form
	d.onkeydown = function(e) {
		e.stopPropagation();

		if (e.keyCode == 13) {
			//entree
			e.preventDefault();
			gererInsertionNouveauNoeud(l, nodePere, navPere, navNew);
		}
		else if (e.keyCode == 27) {
			// escape
			navNew.label = ""; 
			nodePere.removeChild(d);
		}
	}
	l.focus();
}
/**
 * Sur le bouton sauver, on saisit un nom
 */
function saisirNomSauve(evt, nodePere) {
	var x = document.getElementById('saisieInit');
	if(x) x.parentNode.removeChild(x);

	var d = document.createElement('FORM');
	d.setAttribute('name', 'saisieInit');
	d.setAttribute('class', 'saisieInit');
	d.setAttribute('id', 'saisieInit');
	nodePere.appendChild(d);

	//if (evt == null) {
	d.style.left =window.innerWidth /2;
	d.style.top = window.innerHeight/2;
//	}
//	else {
//	d.style.left = evt.clientX +20+ window.pageXOffset + "px";
//	d.style.top = evt.clientY + window.pageYOffset + "px";
//	}
	d.onclick = function(e) {			  
		e.stopPropagation();
	}


	document.body.onclick = function(e) { 
		var x = document.getElementById('saisieInit');		  
		if (x) {
			nodePere.removeChild(x); 
		}
	}

	var p = document.createElement('p');
	d.appendChild(p);
	p.setAttribute('class', 'saisieInitP');
	p.innerHTML = "Sauvegarde";

	var l = document.createElement('INPUT');
	l.setAttribute('class', 'saisieInitI');
	l.setAttribute('type', 'text');
	l.setAttribute('value',nomSauve);
	d.appendChild(l);


	var d2 = document.createElement('div');
	d2.setAttribute('class', 'saisieInitD');
	d2.setAttribute('id', 'saisieInitD');
	d.appendChild(d2);


	p = document.createElement('INPUT');
	p.setAttribute('class', 'saisieInitB');
	p.setAttribute('type', 'button');
	p.setAttribute('value', 'OK');
	p.onclick = function (e) {
		// sauvegarde
		nodePere.removeChild(d);
		nomSauve = l.value;
		lancerReqSauve();
	}
	d2.appendChild(p);

	p = document.createElement('INPUT');
	p.setAttribute('class', 'saisieInitB');
	p.setAttribute('type', 'button');
	p.setAttribute('value', 'Annuler');
	p.onclick = function (e) {

		nodePere.removeChild(d);
	}
	d2.appendChild(p);


	// gestion des touches sur le form
	d.onkeydown = function(e) {
		e.stopPropagation();

		if (e.keyCode == 13) {
			//entree
			e.preventDefault();
			nodePere.removeChild(d);
			nomSauve = l.value;
			lancerReqSauve();
		}
		else if (e.keyCode == 27) {
			// escape
			navNew.label = ""; 
			nodePere.removeChild(d);
		}
	}
	l.focus();
}
function renumeroterRangNoeud(node, rang) {

	var id = node.id;
	var type;
	if (id != null) {
		node.setAttribute('id', node.id.replace(/_[0-9]+_/,"_"+rang+"_"));
	}
	for (var i=0;i<node.childNodes.length;i++) {
		type = donnerTypeNoeud(node.childNodes[i]);
		if (type == "d") {
			renumeroterRangNoeud(node.childNodes[i], eval(rang) +1);
		}
		else {
			renumeroterRangNoeud(node.childNodes[i], rang);
		}
	}
}
//retour de la requète de sauvegarde du json du svi
function retourReqSauve() {
	if (xmlhttp.readyState == 4) {
		if (xmlhttp.status == 200)	alert(nomSauve + " sauvegarde OK");
		else alert("Erreur de sauvegarde code: "+xmlhttp.status);
	}
}
function lancerReqSauve() {
	xmlhttp.open("post", "design?action=sauve&nom="+nomSauve, true);
	xmlhttp.setRequestHeader('Content-Type',
	'application/x-www-form-urlencoded,application/json');
	xmlhttp.onreadystatechange = retourReqSauve;
	xmlhttp.send(JSON.stringify(sviJson));
}
/**
 * Choix d'un fichier dans une liste
 * 
 * @param tabFic
 */
function choisirFichier(tabFic, nodePere) {
	var x = document.getElementById('saisieInit');
	if(x) x.parentNode.removeChild(x);

	var d = document.createElement('FORM');
	d.setAttribute('name', 'saisieInit');
	d.setAttribute('class', 'saisieInit');
	d.setAttribute('id', 'saisieInit');
	nodePere.appendChild(d);

	//if (evt == null) {
	d.style.left =window.innerWidth /2;
	d.style.top = window.innerHeight/2;
//	}
//	else {
//	d.style.left = evt.clientX +20+ window.pageXOffset + "px";
//	d.style.top = evt.clientY + window.pageYOffset + "px";
//	}
	d.onclick = function(e) {			  
		e.stopPropagation();
	}


	document.body.onclick = function(e) { 
		var x = document.getElementById('saisieInit');		  
		if (x) {
			nodePere.removeChild(x); 
		}
	}

	var p = document.createElement('p');
	d.appendChild(p);
	p.setAttribute('class', 'saisieInitP');
	p.innerHTML = "Ouvrir";

	for (var i in tabFic) {
		p = document.createElement('p');
		p.setAttribute('class', 'saisieRadio');
		p.innerHTML = tabFic[i]; 
		d.appendChild(p);
		rv = document.createElement("INPUT");
		rv.setAttribute("type", "radio");
		rv.setAttribute("name", "saisieRadio");
		if (i==0) rv.setAttribute("checked",true);
		else rv.setAttribute("checked",false);
		rv.setAttribute("value",i);
		p.appendChild(rv);
	}

	var d2 = document.createElement('div');
	d2.setAttribute('class', 'saisieInitD');
	d2.setAttribute('id', 'saisieInitD');
	d.appendChild(d2);


	p = document.createElement('INPUT');
	p.setAttribute('class', 'saisieInitB');
	p.setAttribute('type', 'button');
	p.setAttribute('value', 'OK');
	p.onclick = function (e) {
		// sauvegarde		
		recupererFic(tabFic);
	}
	d2.appendChild(p);

	p = document.createElement('INPUT');
	p.setAttribute('class', 'saisieInitB');
	p.setAttribute('type', 'button');
	p.setAttribute('value', 'Annuler');
	p.onclick = function (e) {

		nodePere.removeChild(d);
	}
	d2.appendChild(p);


	// gestion des touches sur le form
	d.onkeydown = function(e) {
		e.stopPropagation();

		if (e.keyCode == 13) {
			//entree
			e.preventDefault();
			recupererFic(tabFic);
		}
		else if (e.keyCode == 27) {
			// escape

			nodePere.removeChild(d);
		}
	}

}
function recupererFic(tabFic) {
	for (var i=0; i<document.saisieInit.saisieRadio.length; i++) {
		var y = document.saisieInit.saisieRadio[i];// 				  
		if (y.checked) {
			xmlhttp.open("get", "design?action=get&nom="+tabFic[i], true);
			xmlhttp.setRequestHeader('Content-Type',
			'application/x-www-form-urlencoded');
			xmlhttp.onreadystatechange = retourInfoSvi;
			xmlhttp.send(null);
		}
	}
}

/**
 * insertion d'un nouveau noeud apres saisie du label
 * @param nodeLabel = noeud label du nouveau noeud
 * @param nodePere = noeud pere du nouveau noeud
 * @param navPere = objet de navigation pere
 * @param navNew = nouvel objet de navigation
 */
function gererInsertionNouveauNoeud(nodeLabel, nodePere, navPere, navNew) {
	if (nodeLabel.value != "") { // le label de l'item a ete saisi
		if (navPere == null) {
			// insertion d'un nouveau service 
			insererRacineJson(navNew, nodeLabel.value);
			var nodeNew = ajouterNoeud (nodePere, navNew.label);
			// suppression de la fenetre de saisie
			var x = document.getElementById('saisieInit');		  
			if (x) {
				nodePere.removeChild(x); 
			}
			if (actionMode == "H") {
				// il faut cloner tous les fils du nouveau noeud inséré
				clonerHierarchie (navNew);

			}
		}
		else {
			var nav = chercherObjetNavigation(navPere.service, nodeLabel.value);
			if( (nav == null && navNew.type != 'retour') // le label n'existe pas dejà	
					|| (nav != null && navNew.type == 'retour')) {			

				navNew.label = nodeLabel.value; 			 

				var nodeNew;
				if(navPere.type == 'condition'|| navPere.type == 'menu') {
					for (var i=0; i<document.saisieInit.saisieRadio.length; i++) {
						var y = document.saisieInit.saisieRadio[i];// 				  
						if (y.checked) {
							// insertion dans le Json
							insererJson(navPere, navNew, y.value);
							if (navPere.type == 'condition' && y.value == '1') 
								nodeNew = ajouterNoeudTxt (nodePere, navNew.label, "(Vrai) ");
							else if (navPere.type == 'condition' && y.value == '0')
								nodeNew = ajouterNoeudTxt (nodePere, navNew.label, "(Faux) ");
							else nodeNew = ajouterNoeudTxt (nodePere, navNew.label, "("+y.value+") ");				  
						}			  
					}
					gererInsertionNoeud(nodePere, nodeNew, navPere, navNew);

				}
				else if (navNew.type == 'retour') {
					insererJson(navPere, nav, ''); // dans le json on doit pointer vers le bon element
					nodeNew = ajouterNoeud (nodePere, navNew.label);
					gererInsertionNoeud(nodePere, nodeNew, navPere, navNew);
				}
				else {
					// insertion dans le Json				  
					insererJson(navPere, navNew, ''); 
					nodeNew = ajouterNoeud (nodePere, navNew.label);
					gererInsertionNoeud(nodePere, nodeNew, navPere, navNew);

				}

				// suppression de la fenetre de saisie
				var x = document.getElementById('saisieInit');		  
				if (x) {
					nodePere.removeChild(x); 
				}
				if (actionMode == "H") {
					// il faut cloner tous les fils du nouveau noeud inséré
					if (navNew.suivant != undefined ) {
						navNew.suivant = navCopie.suivant;
					}
					else {
						navNew.actions = navCopie.actions;
					}
					clonerHierarchie (navNew);

				}
				ajusterFleche(nodePere, navPere);// fleche d'expand
				ajusterFleche(nodeNew, navNew);// fleche d'expand
			}
			else {
				if (nav == null) {
					// l'item drop est un 'retour'

					alert("Label doit exister");
					nodeLabel.focus();
				}
				else {

					alert("Label existe");
					nodeLabel.focus();
				}
			}		
		}
	}
	else {

		alert("Label vide");		
		nodeLabel.focus();
	}
	actionMode = "";
}
function raz() {
	var depart = document.getElementById("000_d_0_archi");

	if (depart != undefined && depart != null) {
		while( depart.firstChild) {
			// La liste n'est pas une copie, elle sera donc réindexée à chaque appel
			depart.removeChild( depart.firstChild);
		}
	}
}