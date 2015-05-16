	/*
		Helios by HTML5 UP
		html5up.net | @n33co
		Free for personal and commercial use under the CCA 3.0 license (html5up.net/license)
		*/

		(function($) {

			var settings = {

			// Header (homepage only)
			header: {
				fullScreen: true,
				fadeIn: true,
				fadeDelay: 500
			},

			// Carousels
			carousels: {
				speed: 4,
				fadeIn: true,
				fadeDelay: 250
			},

		};

		skel.init({
			reset: 'full',
			breakpoints: {
				'global':	{ range: '*', href: 'css/style.css', containers: 1400, grid: { gutters: 48 } },
				'wide':		{ range: '-1680', href: 'css/style-wide.css', containers: 1200 },
				'normal':	{ range: '-1280', href: 'css/style-normal.css', containers: '100%', grid: { gutters: 36 } },
				'narrow':	{ range: '-960', href: 'css/style-narrow.css', grid: { gutters: 32 } },
				'narrower': { range: '-840', href: 'css/style-narrower.css', containers: '100%!', grid: { collapse: true } },
				'mobile':	{ range: '-736', href: 'css/style-mobile.css', grid: { gutters: 20 }, viewport: { scalable: false } }
			},
			plugins: {
				layers: {
					config: {
						mode: function() { return (skel.vars.isMobile ? 'transform' : 'position'); }
					},
					navPanel: {
						hidden: true,
						breakpoints: 'mobile',
						position: 'top-left',
						side: 'top',
						width: '100%',
						height: 250,
						animation: 'pushY',
						clickToHide: true,
						swipeToHide: false,
						html: '<div data-action="navList" data-args="nav"></div>',
						orientation: 'vertical'
					},
					navButton: {
						breakpoints: 'mobile',
						position: 'top-center',
						side: 'top',
						width: 100,
						height: 50,
						html: '<div class="toggle" data-action="toggleLayer" data-args="navPanel"></div>'
					}
				}
			}
		});

	$(function() {

		var	$window = $(window),
		$body = $('body'),
		$header =  $('#header');
		var inConf=false;
		var showUserNameConnectedToOms = "showUserNameConnectedToOMS";
		var deleteUserNameInConf="deleteUserNameInConf";
		var deleteUserNameconnected = "deleteUserNameconnected";
		var showUserNameInConf = "showUserNameInConf";
		var IncomingCall="incomingCall";
		var playRecording = 0;
		var startRecording = 1;
		var stopPlay = 0;
			// Disable animations/transitions until the page has loaded.
			$body.addClass('is-loading');
			var streamLocal;
			var dtmfSender;
			var serveurWs;
			var socket;
			var socketAS;
			var active = "active";
			var disabled = "disabled";
			var listArrayClient = new Array();
			var listOfPeopleInConf=new Array();
			
			$window.on('load', function() {
				$body.removeClass('is-loading');

			});

			// CSS polyfills (IE<9).
			if (skel.vars.IEVersion < 9)
				$(':last-child').addClass('last-child');

			// Forms (IE<10).
			var $form = $('form');

				/*	if ($form.length > 0) {

				$form.find('.submit2')
					.on('click', function() {
						$(this).parents('form').submit();
						return false;
					});

				if (skel.vars.IEVersion < 10) {
					$.fn.n33_formerize=function(){var _fakes=new Array(),_form = $(this);_form.find('input[type=text],textarea').each(function() { var e = $(this); if (e.val() == '' || e.val() == e.attr('placeholder')) { e.addClass('formerize-placeholder'); e.val(e.attr('placeholder')); } }).blur(function() { var e = $(this); if (e.attr('name').match(/_fakeformerizefield$/)) return; if (e.val() == '') { e.addClass('formerize-placeholder'); e.val(e.attr('placeholder')); } }).focus(function() { var e = $(this); if (e.attr('name').match(/_fakeformerizefield$/)) return; if (e.val() == e.attr('placeholder')) { e.removeClass('formerize-placeholder'); e.val(''); } }); _form.find('input[type=password]').each(function() { var e = $(this); var x = $($('<div>').append(e.clone()).remove().html().replace(/type="password"/i, 'type="text"').replace(/type=password/i, 'type=text')); if (e.attr('id') != '') x.attr('id', e.attr('id') + '_fakeformerizefield'); if (e.attr('name') != '') x.attr('name', e.attr('name') + '_fakeformerizefield'); x.addClass('formerize-placeholder').val(x.attr('placeholder')).insertAfter(e); if (e.val() == '') e.hide(); else x.hide(); e.blur(function(event) { event.preventDefault(); var e = $(this); var x = e.parent().find('input[name=' + e.attr('name') + '_fakeformerizefield]'); if (e.val() == '') { e.hide(); x.show(); } }); x.focus(function(event) { event.preventDefault(); var x = $(this); var e = x.parent().find('input[name=' + x.attr('name').replace('_fakeformerizefield', '') + ']'); x.hide(); e.show().focus(); }); x.keypress(function(event) { event.preventDefault(); x.val(''); }); });  _form.submit(function() { $(this).find('input[type=text],input[type=password],textarea').each(function(event) { var e = $(this); if (e.attr('name').match(/_fakeformerizefield$/)) e.attr('name', ''); if (e.val() == e.attr('placeholder')) { e.removeClass('formerize-placeholder'); e.val(''); } }); }).bind("reset", function(event) { event.preventDefault(); $(this).find('select').val($('option:first').val()); $(this).find('input,textarea').each(function() { var e = $(this); var x; e.removeClass('formerize-placeholder'); switch (this.type) { case 'submit': case 'reset': break; case 'password': e.val(e.attr('defaultValue')); x = e.parent().find('input[name=' + e.attr('name') + '_fakeformerizefield]'); if (e.val() == '') { e.hide(); x.show(); } else { e.show(); x.hide(); } break; case 'checkbox': case 'radio': e.attr('checked', e.attr('defaultValue')); break; case 'text': case 'textarea': e.val(e.attr('defaultValue')); if (e.val() == '') { e.addClass('formerize-placeholder'); e.val(e.attr('placeholder')); } break; default: e.val(e.attr('defaultValue')); break; } }); window.setTimeout(function() { for (x in _fakes) _fakes[x].trigger('formerize_sync'); }, 10); }); return _form; };
					$form.n33_formerize();
				}

			}*/
			if ($form.length > 0) {

				$('#submit2').click(function(e){  
					var idClicked = e.target.id;  

					if(idClicked=="submit2") {
						e.preventDefault();

	           // document.getElementById('submit').click(); 
	           $("span").hide();
	           var login = $("#name").val();
	           if(login == ""){
	           	$("#checkUserName").show();
				 //$("#checkUserName").css("color","red");
				 $("#checkUserName").text("userName must be filled out");
				 $("#checkUserName").css({'font-style': 'italic'});

				}else{		

					$("#checkUserName").hide();	
					$("#banner").hide();

					connectToOMS(login);	
					$("#printUserName").text("userName: " + login);		
				}

			}
		});
				$('#submit2').keypress(function(e){                                       
					if (e.which == 13) {
						e.preventDefault();

	           // document.getElementById('submit').click(); 
	           $("span").hide();
	           var login = $("#name").val();
	           if(login == ""){
	           	$("#checkUserName").show();
				 //$("#checkUserName").css("color","red");
				 //$("#checkUserName").text("userName must be filled out");

				}else{		

					$("#checkUserName").hide();	
					$("#banner").hide();

					connectToOMS(login);			
					$("#printUserName").text("userName: " + login);
				}

			}
		});

				if (skel.vars.IEVersion < 10) {
					$.fn.n33_formerize=function(){var _fakes=new Array(),_form = $(this);_form.find('input[type=text],textarea').each(function() { var e = $(this); if (e.val() == '' || e.val() == e.attr('placeholder')) { e.addClass('formerize-placeholder'); e.val(e.attr('placeholder')); } }).blur(function() { var e = $(this); if (e.attr('name').match(/_fakeformerizefield$/)) return; if (e.val() == '') { e.addClass('formerize-placeholder'); e.val(e.attr('placeholder')); } }).focus(function() { var e = $(this); if (e.attr('name').match(/_fakeformerizefield$/)) return; if (e.val() == e.attr('placeholder')) { e.removeClass('formerize-placeholder'); e.val(''); } }); _form.find('input[type=password]').each(function() { var e = $(this); var x = $($('<div>').append(e.clone()).remove().html().replace(/type="password"/i, 'type="text"').replace(/type=password/i, 'type=text')); if (e.attr('id') != '') x.attr('id', e.attr('id') + '_fakeformerizefield'); if (e.attr('name') != '') x.attr('name', e.attr('name') + '_fakeformerizefield'); x.addClass('formerize-placeholder').val(x.attr('placeholder')).insertAfter(e); if (e.val() == '') e.hide(); else x.hide(); e.blur(function(event) { event.preventDefault(); var e = $(this); var x = e.parent().find('input[name=' + e.attr('name') + '_fakeformerizefield]'); if (e.val() == '') { e.hide(); x.show(); } }); x.focus(function(event) { event.preventDefault(); var x = $(this); var e = x.parent().find('input[name=' + x.attr('name').replace('_fakeformerizefield', '') + ']'); x.hide(); e.show().focus(); }); x.keypress(function(event) { event.preventDefault(); x.val(''); }); });  _form.submit(function() { $(this).find('input[type=text],input[type=password],textarea').each(function(event) { var e = $(this); if (e.attr('name').match(/_fakeformerizefield$/)) e.attr('name', ''); if (e.val() == e.attr('placeholder')) { e.removeClass('formerize-placeholder'); e.val(''); } }); }).bind("reset", function(event) { event.preventDefault(); $(this).find('select').val($('option:first').val()); $(this).find('input,textarea').each(function() { var e = $(this); var x; e.removeClass('formerize-placeholder'); switch (this.type) { case 'submit': case 'reset': break; case 'password': e.val(e.attr('defaultValue')); x = e.parent().find('input[name=' + e.attr('name') + '_fakeformerizefield]'); if (e.val() == '') { e.hide(); x.show(); } else { e.show(); x.hide(); } break; case 'checkbox': case 'radio': e.attr('checked', e.attr('defaultValue')); break; case 'text': case 'textarea': e.val(e.attr('defaultValue')); if (e.val() == '') { e.addClass('formerize-placeholder'); e.val(e.attr('placeholder')); } break; default: e.val(e.attr('defaultValue')); break; } }); window.setTimeout(function() { for (x in _fakes) _fakes[x].trigger('formerize_sync'); }, 10); }); return _form; };
					$form.n33_formerize();
				}

			}

			// Dropdowns.
			$('#nav > ul').dropotron({
				mode: 'fade',
				speed: 350,
				noOpenerFade: true,
				alignment: 'center'
			});

			// Scrolly links.
			$('.scrolly').scrolly();

			// Carousels.
			$('.carousel').each(function() {

				var	$t = $(this),
				$forward = $('<span class="forward"></span>'),
				$backward = $('<span class="backward"></span>'),
				$reel = $t.children('.reel'),
				$items = $reel.children('article');

				var	pos = 0,
				leftLimit,
				rightLimit,
				itemWidth,
				reelWidth,
				timerId;

					// Items.
					if (settings.carousels.fadeIn) {

						$items.addClass('loading');

						$t.onVisible(function() {
							var	timerId,
							limit = $items.length - Math.ceil($window.width() / itemWidth);

							timerId = window.setInterval(function() {
								var x = $items.filter('.loading'), xf = x.first();

								if (x.length <= limit) {

									window.clearInterval(timerId);
									$items.removeClass('loading');
									return;

								}

								if (skel.vars.IEVersion < 10) {

									xf.fadeTo(750, 1.0);
									window.setTimeout(function() {
										xf.removeClass('loading');
									}, 50);

								}
								else
									xf.removeClass('loading');

							}, settings.carousels.fadeDelay);
						}, 50);
					}

					// Main.
					$t._update = function() {
						pos = 0;
						rightLimit = (-1 * reelWidth) + $window.width();
						leftLimit = 0;
						$t._updatePos();
					};

					if (skel.vars.IEVersion < 9)
						$t._updatePos = function() { $reel.css('left', pos); };
					else
						$t._updatePos = function() { $reel.css('transform', 'translate(' + pos + 'px, 0)'); };

					// Forward.
					$forward
					.appendTo($t)
					.hide()
					.mouseenter(function(e) {
						timerId = window.setInterval(function() {
							pos -= settings.carousels.speed;

							if (pos <= rightLimit)
							{
								window.clearInterval(timerId);
								pos = rightLimit;
							}

							$t._updatePos();
						}, 10);
					})
					.mouseleave(function(e) {
						window.clearInterval(timerId);
					});

					// Backward.
					$backward
					.appendTo($t)
					.hide()
					.mouseenter(function(e) {
						timerId = window.setInterval(function() {
							pos += settings.carousels.speed;

							if (pos >= leftLimit) {

								window.clearInterval(timerId);
								pos = leftLimit;

							}

							$t._updatePos();
						}, 10);
					})
					.mouseleave(function(e) {
						window.clearInterval(timerId);
					});

					// Init.
					$window.load(function() {

						reelWidth = $reel[0].scrollWidth;

						skel.change(function() {

							if (skel.vars.isTouch) {

								$reel
								.css('overflow-y', 'hidden')
								.css('overflow-x', 'scroll')
								.scrollLeft(0);
								$forward.hide();
								$backward.hide();

							}
							else {

								$reel
								.css('overflow', 'visible')
								.scrollLeft(0);
								$forward.show();
								$backward.show();

							}

							$t._update();
						});

						$window.resize(function() {
							reelWidth = $reel[0].scrollWidth;
							$t._update();
						}).trigger('resize');

					});

				});

			// Header.
			if ($body.hasClass('homepage')) {

				if (settings.header.fullScreen) {

					$window.bind('resize.helios', function() {
						window.setTimeout(function() {
							var s = $header.children('.inner');
							var sh = s.outerHeight(), hh = $window.height(), h = Math.ceil((hh - sh) / 2) + 1;

							$header
							.css('padding-top', h)
							.css('padding-bottom', h);
						}, 0);
					}).trigger('resize');

				}

				if (settings.header.fadeIn) {

					$.n33_preloadImage = function(url, onload) { var $img = $('<img />'), _IEVersion = (navigator.userAgent.match(/MSIE ([0-9]+)\./) ? parseInt(RegExp.$1) : 99); $img.attr('src', url); if ($img.get(0).complete || _IEVersion < 9) (onload)(); else $img.load(onload); };

					$('<div class="overlay" />').appendTo($header);

					$window
					.load(function() {
						var imageURL = $header.css('background-image').replace(/"/g,"").replace(/url\(|\)$/ig, "");

						$.n33_preloadImage(imageURL, function() {

							if (skel.vars.IEVersion < 10)
								$header.children('.overlay').fadeOut(2000);
							else
								window.setTimeout(function() {
									$header.addClass('ready');
								}, settings.header.fadeDelay);

						});
					});

				}

			}

			$(document).ready(function(){
				$("#leaveConference").hide();
			});
			$(document).on("click", "#leaveConference", function(e){
			
				leaveConference(($("#printUserName").text()).split(" ")[1]);

			});
			$(document).on('click', '#mute', function() {
				alert("m clicking on mute");
				mute();
			});
			$(document).on('click', '#unmute', function() {
				unmute();
			});
			
			$(document).on('click', '#recordingConf', function() {
				recordingConf();
			});
			$(document).on('click', '#playFile', function() {
				playFile();
			});
		$(document).on('click', '#stopRecordingConf', function() {
				stopRecordingConf();
			});

			$(document).on('click', '#Accept', function() {
				console.log("m clicking");
				
				//var ConferenceName=$(this).parent().closest('div').find(".confName").text();
				var name=$(".nameOfCaller").text();

				Answer(name);

				//document.getElementById('Beatles-Hey_Jude').pause();
				//document.getElementById('RingbackTone').currentTime = 0;
				$(".cd-popup").removeClass('is-visible');
				$("#"+name).append($("<img/>",{
					"class":"hangup",
					"id":"hangup"+name,
					"src":"images/hangup.png"


				}));
				$("#"+name).find("#Call"+name).remove();
				$("#"+name).find("#hangup"+name).show();


			});




			$(document).on('click', '#AcceptLeave', function() {
				
				
				//var ConferenceName=$(this).parent().closest('div').find(".confName").text();
				var name=$(".nameOfCaller").text();

				AnswerAndLeave(name);

				//document.getElementById('Beatles-Hey_Jude').pause();
				//document.getElementById('RingbackTone').currentTime = 0;
				$(".cd-popup").removeClass('is-visible');
				$("#"+name).append($("<img/>",{
					"class":"hangup",
					"id":"hangup"+name,
					"src":"images/hangup.png"


				}));
				$("#"+name).find("#Call"+name).remove();
				$("#"+name).find("#hangup"+name).show();


			});


			$(document).on('click', '.hangup', function() {
				console.log("m clicking");
				
				var name2=$(this).attr("id").substring(6);
							//var ConferenceName=$(this).parent().closest('div').find(".confName").text();
							console.log("hang up on "+name2 )
							$("#"+name2).append($("<img/>",{
								"class":"Call",
								"id":"Call"+name2,
								"src":"images/call.png"


							}));
							$("#"+name2).find("#Call"+name).show();
							this.remove();
							hangup(name2);
							inConf=false;

						});

			$(document).on('click', '#Refuse', function() {
				console.log("m clicking");
				
				//var ConferenceName=$(this).parent().closest('div').find(".confName").text();
				
				Reject($(".nameOfCaller").text());
				//document.getElementById('Beatles-Hey_Jude').pause();
		//	document.getElementById('RingbackTone').currentTime = 0;
				$(".cd-popup").removeClass('is-visible');

			});

			$(document).on("appened", ".Conf", function(e){
				
				$(this).append($("<img/>",{
					"class":"icon Call",
					"id":"Call"+$(this).attr("id"),
					"src":"images/call.png"


				}));
				$(this).find(".Call").show();




				$(this).append($("<p/>",{
					"class":"circle",
					"id":"circle"+$(this).attr("id")
					

				}));
				$(this).find(".circle").show();

			});



			var recipient;
			var i=0;
			var k=0;

			$(document).on('click', '.Call', function() {
				console.log("m clicking");

				recipient=$(this).attr("id").substring(4);
				CallConnected(recipient);
				//createConference("speaker:randomConf:"+$("printUserName").text());
				//document.getElementById('RingbackTone').play();

			});

			function CallConnected(name){
				socket.send(JSON.stringify({"cmd":"call" ,"param":name }));
			}
			function Answer(name){

				inConf=true;
				socket.send(JSON.stringify({"cmd":"answerAndStay" ,"param":name }));
			}

			function AnswerAndLeave(name){

				inConf=true;
				socket.send(JSON.stringify({"cmd":"answerAndLeave" ,"param":name }));
			}

			function Reject(name){
				socket.send(JSON.stringify({"cmd":"reject" ,"param":name }));
			}
			function createConference(loginMode) {


				socket.send(JSON.stringify({"cmd":"createConf","param": loginMode}));
			}

			function hangup(name){

				inConf=false;
		//Racrocher en fournissant le nom de l'appelant
		socket.send(JSON.stringify({"cmd":"hangup","param":name}));
	}

	function leaveConference(name){


	}
	function mute(){

	document.getElementById("unmute").disabled=false;
	document.getElementById("mute").disabled=true;

	socket.send(JSON.stringify({"cmd":"mute","param":" "}));
	

}
function playFile(){

	

	socket.send(JSON.stringify({"cmd":"playFile","param":" "}));
}

function unmute(){
	
	//$("#printMode").text("mode: " + "speaker");
	//buttonState2(active, active, disabled, active, active);
	socket.send(JSON.stringify({"cmd":"unmute","param":" "}));
	document.getElementById("unmute").disabled=true;
	document.getElementById("mute").disabled=false;
}


function recordingConf() {

	//buttonState(disabled, active, disabled, disabled, active, disabled, disabled, disabled);

	//socket.send(JSON.stringify({"cmd":"recordConf","param":" "}));


		socket.send(JSON.stringify({"cmd":"recordConf","param":" "}));
		$("#recordingConf").text("stop Recording conf");
		startRecording = 0;

	document.getElementById("recordingConf").disabled=true;
	document.getElementById("stopRecordingConf").disabled=false;
}



function stopRecordingConf(){

	playRecording = 1;
	//buttonState(disabled, active, disabled, active, active, disabled, disabled, disabled);

	socket.send(JSON.stringify({"cmd":"stopRecordConf","param":" "}));
	document.getElementById("stopRecordingConf").disabled=true;
	document.getElementById("recordingConf").disabled=false;
}

function createWebSocket(host) {
	if(window.MozWebSocket) {
		window.WebSocket=window.MozWebSocket;
	}
	if(!window.WebSocket) {
		alert('Votre navigateur ne supporte pas les webSocket!');
		return false;
	} else {
		socket = new WebSocket(host);
			//socket.onopen = function() {
				//trace("Connexion au serveur " + serveurWs);
				//alert("Message to sent...");
				//socket.send("newCall");
				//alert("New call");
			//}
			socket.onclose = function() { 
				//trace("Déconnexion"); 



				var video =  document.getElementById("audio2");
				video.muted = true;
				video.autoplay = false;
				window.location.reload(true);

			}
			socket.onerror = function() { trace("Une erreur est survenue"); }
			socket.onmessage = function(evt){
				//alert(evt.data);
				if (evt.data == "stopRecordConf") {

					//document.getElementById("recordConf").style.display = "block";
					//.getElementById("recordConf").innerHTML = "Recording ended";
					//document.getElementById("recordConf").style.color = "blue";
					$("#recordConf").show().css("color","blue").text("Recording ended");
				} else if(evt.data == "recordConf"){
					


					$("#recordConf").show().css("color","red").text("Recording started");
					
				}else if (evt.data instanceof Blob) {


					var urlInfo = window.URL.createObjectURL(evt.data);
					
					var strWindowFeatures = "menubar=yes,location=yes,resizable=yes,scrollbars=yes,status=yes";
					var windowObjectReference = window.open(urlInfo, "My_Window", strWindowFeatures);

					 /*$(windowObjectReference.document).load(function(){
						window.stop();
					});*/
}
else if(evt.data == "incomingCall"){
					//console.log("incoming Call");

				}else if(evt.data == "confCreated"){


				}else if (evt.data == "confDoesNotExist") {
					alert("No conference available. You can create one by clicking on create");


				}else if (evt.data == "confAlreadyExists") {				
					alert("conference already exists. You can join it by clicking on conference");


				}else if(evt.data == "coachExist"){
					alert("A coach already exists. Please change your mode");


				}else if(evt.data == "studentExist"){
					alert("A student already exists. Please change your mode");

				}else if(evt.data =="userNameExist"){
					alert("userName already exists.");
					socket.close();
				}else if (evt.data == "muteAll") {
					$("#printMode").text("mode: " + "mute");

				}else if (evt.data == "unmuteAll") {
					$("#printMode").text("mode: " + "speaker");

				}
				else if (evt.data.substring(0,showUserNameConnectedToOms.length) == showUserNameConnectedToOms) {

					var str = evt.data.split(":");
					console.log(evt.data);

					if(str[str.length - 1]!="hangup"){

						var nameClient = str[str.length - 1];

						if(listArrayClient.indexOf(nameClient) == -1){

							listArrayClient.push(nameClient);

							console.log("first time:" + nameClient);

							var participantNode = document.createTextNode(nameClient);
						//var node=document.createElement("LI");
						var liste= document.getElementById("listOfPeopleConnected");
						//node.appendChild(participantNode);
						//liste.appendChild(node);
						$("<li/>", {
							"class":"Conf",
							"id": nameClient,
							text: nameClient
						}).appendTo(liste).trigger("appened");



					}
					else  {
						console.log("second time"+ nameClient);
						$("#circle"+nameClient).css({"background-color":"yellow"});

					}


				}
				else  {
					
					console.log("second time hangup"+ str[1]);
					$("#circle"+str[1]).css({"background-color":"white"});
					//$("#hangup"+str[1]).css({'visibility':'hidden'})
					//$("#hangup:"+str[1]).hide();

				}	


			}
			else if (evt.data.startsWith("answer:")) {
					//Récupérer le nom de l'appelé
					//Afficher un msg qui indique l'acception de l'appel

					var str = evt.data.split(":");
					//document.getElementById('RingbackTone').pause();
					//document.getElementById('Beatles-Hey_Jude').currentTime = 0;
					console.log("Call accepted by " + str[str.length - 1]);
					$("#Call"+str[str.length - 1]).remove();
					$("#"+str[str.length - 1]).append($("<img/>",{
						"class":"hangup",
						"id":"hangup"+str[str.length - 1],
						"src":"images/hangup.png"


					}));
					//$("#hangup"+str[str.length - 1]).show();
					$("#"+str[str.length - 1]).find("#hangup"+str[str.length - 1]).show();
				//$("#hangup"+str[str.length - 1]).css({"visibility":"visible"});

				$("#circle"+str[str.length - 1]).css({"background-color":"yellow"});


				inConf=true;
			}

			else if (evt.data.startsWith("hide:")) {
					//Récupérer le nom de l'appelé
					//Afficher un msg qui indique l'acception de l'appel

					var str = evt.data.split(":");
					console.log("you are in conference with " + str[str.length - 1]);

					$("#"+str[str.length - 1]).find("#hangup"+str[str.length - 1]).remove();
					console.log("you are in conference with 2 " + str[str.length - 1]);
					$("#"+str[str.length - 1]).find("#Call"+str[str.length - 1]).remove();
					$("#leaveConference").show();


				}
				else if (evt.data.startsWith("reject:")) {
					//Récupérer le nom de l'appelé
					//Afficher un msg qui indique l'acception de l'appel

					var str = evt.data.split(":");
				//	document.getElementById('RingbackTone').pause();
			//		document.getElementById('Beatles-Hey_Jude').currentTime = 0;
					alert("Call rejected by " + str[str.length - 1]);
					


				}
				/*else if (evt.data.startsWith("callAnswered:")) {
					//Récupérer le nom de l'appelé
					//Afficher un msg qui indique l'acception de l'appel

					
					alert("Call is answered by " + str[str.length - 1]);

					status=true;
					


				}*/
				else if (evt.data.startsWith("hangup:")) {
					//Récupérer le nom de l'appelé
					//Afficher un msg qui indique l'acception de l'appel

					var str = evt.data.split(":");

					$("#"+str[str.length - 1]).find("#hangup"+str[str.length - 1]).remove();
					console.log("hang up by " + str[str.length - 1]);
					$("#"+str[str.length - 1]).append($("<img/>",{
						"class":"Call",
						"id":"Call"+str[str.length - 1],
						"src":"images/call.png"


					}));
					console.log("call should be on ");
					$("#"+str[str.length - 1]).find("Call"+str[str.length - 1]).show();
					inConf=false;

				//	$(".hanging" ).trigger( "hanging" );
					//$("#hangup:"+str[str.length - 1]).addClass('invisible');
				//	document.getElementById("#hangup:"+str[str.length - 1]).style.display="none";
					//$("#"+str[str.length - 1]).find("#hangup:"+str[str.length - 1]).hide();
					//$("#hangup:"+str[str.length - 1]).hide();
				}else if (evt.data.substring(0,showUserNameInConf.length) == showUserNameInConf) {

					var str = evt.data.split(":");
					var nameClient = str[str.length - 1];
					var liste;
					if(listOfPeopleInConf.indexOf(nameClient) == -1){


						listOfPeopleInConf.push(nameClient);
						var participantNode = document.createTextNode(nameClient);
						var node = document.createElement("LI");
						liste= document.getElementById("listOfPeopleInConf");

						node.appendChild(participantNode);
						liste.appendChild(node);

					//	$("ul#listOfPeopleInConf li:contains("+nameClient+")").remove();
				}
			}
				//incomingCall:name
				else if (evt.data.substring(0,IncomingCall.length) == IncomingCall) {
					var str = evt.data.split(":");
					
					var ConfCaller= str[str.length - 1];
					console.log("this is the confCaller"+ConfCaller);
					/*$( "<div/>", {
					"class": "incomingCall",
					"id":"incomingCall"+ConfCaller,
					"css":{
					
					'width':'300px',
					'height':'250px'
					}

				
			})

	.appendTo( "body" );*/



	$(".nameOfCaller").text(ConfCaller);


	if(inConf){
		console.log("in conf is "+inConf);
		$(".cd-buttons").prepend("<li><a href='#0' id='AcceptLeave'>Accept and leave</a></li>");
		$('.cd-popup').addClass('is-visible');
	}
	else{
		console.log("my conf is "+inConf);
		$('.cd-popup').addClass('is-visible');
	}

	//document.getElementById('Beatles-Hey_Jude').play();






}
else if (evt.data.substring(0,deleteUserNameconnected.length) == deleteUserNameconnected) {
	console.log(evt.data+"and" +evt.data.substring(0,deleteUserName.length));
	var str = evt.data.split(":");
	console.log("m deleting people in conference")
	var nameClient = str[str.length - 1];
	var liste;
	if(listArrayClient.indexOf(nameClient) != -1){					


		liste= document.getElementById("listOfPeopleConnected");
		$('ul#listOfPeopleConnected li:contains(' + nameClient + ')').remove();
		listArrayClient.splice(listArrayClient.indexOf(nameClient), 1);
	}

	if(listOfPeopleInConf.indexOf(nameClient) != -1){

		liste = document.getElementById("listOfPeopleInConf");
		$('ul#listOfPeopleInConf li:contains(' + nameClient + ')').remove();
		listOfPeopleInConf.splice(listOfPeopleInConf.indexOf(nameClient), 1);
	}
}
else if (evt.data.substring(0,deleteUserNameInConf.length) == deleteUserNameInConf) {

	var str = evt.data.split(":");

	var nameClient = str[str.length - 1];
	var liste;


	if(listOfPeopleInConf.indexOf(nameClient) != -1){

		liste = document.getElementById("listOfPeopleInConf");
		$('ul#listOfPeopleInConf li:contains(' + nameClient + ')').remove();
		listOfPeopleInConf.splice(listOfPeopleInConf.indexOf(nameClient), 1);
	}
}

else {

	var signal = JSON.parse(evt.data);
					if (signal.sdp) { //distinguer sdp offer ou answer
						trace ("Reception de SDP" + signal.sdp.sdp);

						//document.getElementById("infos").className = active;
						pc.setRemoteDescription(new RTCSessionDescription(signal.sdp));
						//setTimeout(function(){window.close();},6000);
						//window.close();
					}
					else {

						if (signal.candidate) {
							trace ("Reception de CANDIDATES");
							pc.addIceCandidate(new RTCIceCandidate(signal.candidate));
						} else {
							trace ("Reception de data : " + evt.data);
						}
					}
				}
			}
		}
	}
	function conference(loginMode){


		socket.send(JSON.stringify({ "cmd":"joinConf","param": loginMode }));
		
	}
	function leaveConference(name){


		socket.send(JSON.stringify({ "cmd":"unjoin","param": name }));
		
	}
	function call() {

	    // On demande le flux local au navigateur 
		// On l'envoie au correspondant
		// On définit un dtmfSender pour envoyer des dtmfs
		// Et on l'attache au bon curseur local

		navigator.getUserMedia({ "audio": true, "video": false }, 
			function (stream) {
				trace("Lit le flux local et l'envoie au correspondant");
				pc.addStream(stream);
				trace ("createOffer");
				
				// Attache le flux local au casque local. On s'entend parler
				attachMediaStream(audio1, stream);
				trace("On attache le flux local au curseur du haut : audio1");
				//myStream = stream;
				pc.createOffer(gotDescription, function(error) {
					trace("Erreur createOffer")} );

				function gotDescription(desc) {
					trace ("On envoie le SDP");
					pc.setLocalDescription(desc);
				}
			}, 
			function(evt){
				trace("ERREUR : Impossible de lire le media local" + evt);
			}
			);
	}
	var pc;
	var servers = null;
	var pc_constraints = {"optional": []};
	var configuration = { "iceServers": [{ "url": "stun:stun.example.org" }] };

	pc = new RTCPeerConnection(servers,pc_constraints);

	function connectToOMS(userName) {

		//var userName = "Test";
		serveurWs = document.saisie.serveurPort.value;
		createWebSocket("ws://" + serveurWs);
		//createWebSocket("ws://127.0.0.1:8887");


		socket.onopen = function() {
			trace("Connexion au serveur " + serveurWs);
			//socket.send("newCall");
		}

		//pc = new RTCPeerConnection(servers,pc_constraints);
		trace("getStream");

		// Handler sur la reception d'un flux media
		pc.onaddstream = function (evt) {
			trace("onaddstream");
			
			// Attache le flux du distant au casque local
			attachMediaStream(audio2, evt.stream);
		};
		
		// Handler sur la reception des candidates. La derniere == null. 
		pc.onicecandidate = function (evt) {
			if (evt.candidate == null) {
				trace("On envoie le sdp complet");
				var localSDP = pc.localDescription;
				socket.send(JSON.stringify({"sdp": localSDP,"userName":userName})); //envoie du sdp	
				//alert(localSDP);
			}
		};

		call();


	}

});

})(jQuery);

