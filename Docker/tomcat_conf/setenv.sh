#export CATALINA_OPTS="$CATALINA_OPTS -Xms512m"
export CATALINA_OPTS="$CATALINA_OPTS -Xms1024m"
export CATALINA_OPTS="$CATALINA_OPTS -Xmx4096m"
#export CATALINA_OPTS="$CATALINA_OPTS -XX:MaxPermSize=1024m"

#Use non-blocking entropy source
export CATALINA_OPTS="$CATALINA_OPTS -Djava.security.egd=file:/dev/./urandom"

#Enabling Hotswap Agent
export JPDA_OPTS="-agentlib:jdwp=transport=dt_socket,address=1043,server=y,suspend=n -XXaltjvm=dcevm -javaagent:/usr/lib/hotswapagent/hotswap-agent.jar" 

#Enabling PSI probe to collect more data from JMX
export CATALINA_OPTS="$CATALINA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=8000 -Dcom.sun.management.jmxremote.rmi.port=8000 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Djava.rmi.server.hostname=127.0.0.1"

