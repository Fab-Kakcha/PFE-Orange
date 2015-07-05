<script >
	<![CDATA[

	         function getSIPConnectionFailedReason(event){
	             var result = "";

	             switch(event.reason) {
	                 case 'baddestination' :
	                     result='baddestination';
	                     break;
	                 case 'normal' :
	                     result='normal';
	                     break;
					case '486' :
	                 case 'busy' :
	                     result='busy';
	                     break;
	                 case 'timeout' :
	                 case 'noanswer' :
	                     result='noanswer';
	                     break;
	                 case '480' :
	                     result='Temporarily Unavailable';
	                     break;					
	                 default :
	                     result=event.reason;
	             }
	             return result;
	         } // getSIPConnectionFailedReason

	         function getresultScenarioValue(event){
	             var result = "";

	             if(typeof(event.values)=='undefined') {
	                 result='null';
	             } else {
	                 if(typeof(event.values.resultScenario)=='undefined'){
	                     result='null';
	                 } else {
	                     result=event.values.resultScenario;
	                 }
	             }
	             return result;
	         } // getresultScenarioValue

		]]>
	</script>