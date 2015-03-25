export OMSGATEWAY_HOME=/opt/testlab/utils/stageFabrice/src/main/java
export CLASSPATH=.:$OMSGATEWAY_HOME:$OMSGATEWAY_HOME/lib/java_websocket.jar:$OMSGATEWAY_HOME/lib/log4j-1.2.9.jar:$OMSGATEWAY_HOME/lib/commons-logging-1.1.jar:$OMSGATEWAY_HOME/lib/gson-2.3.jar
java com.orange.olps.stageFabrice.webrtc.MonServiceWtc
