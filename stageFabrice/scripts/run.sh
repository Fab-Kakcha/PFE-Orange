export OMSGATEWAY_HOME=/opt/testlab/utils/Test/src
export CLASSPATH=.:$OMSGATEWAY_HOME:$OMSGATEWAY_HOME/lib/java_websocket.jar:$OMSGATEWAY_HOME/lib/log4j-1.2.9.jar:$OMSGATEWAY_HOME/lib/commons-logging-1.1.jar:$OMSGATEWAY_HOME/lib/gson-2.3.jar:$OMSGATEWAY_HOME/lib/api-oms-webrtc.jar:$OMSGATEWAY_HOME/lib/org.json.jar:$OMSGATEWAY_HOME/lib/api-oms-sip.jar
rm test/*.class
javac test/*.java
java test.MonService
