#!/bin/bash
export JAVA_HOME=/usr/local/java
export CLASSPATH=./:$JAVA_HOME/lib:$JAVA_HOME/jre/lib
export PATH=$PATH:$JAVA_HOME/bin
export JRE_HOME=$JAVA_HOME/jre

java -jar -Xms4G -Xmx4G -Xmn1024m -XX:NewSize=512m -XX:MaxNewSize=1024m -XX:MaxMetaspaceSize=256m -Xss512k -XX:+UseG1GC -server /data/webapps/dmp-server-log/server-log.jar > /dev/null 2>&1 &
