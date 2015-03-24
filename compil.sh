export OMSGATEWAY_HOME=/opt/application/testlab/utils/stageFabrice
export CLASSPATH=$OMSGATEWAY_HOME:$OMSGATEWAY_HOME/lib/java_websocket.jar:$OMSGATEWAY_HOME/lib/log4j-1.2.9.jar:$OMSGATEWAY_HOME/lib/commons-logging-1.1.jar:$OMSGATEWAY_HOME/lib/gson-2.3.jar
rm com/orange/olps/stageFabrice/webrtc/*.class
javac com/orange/olps/stageFabrice/webrtc/*.java
