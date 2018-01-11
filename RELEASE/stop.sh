#!/bin/sh

className=server-log.jar

pid=`/usr/local/java/bin/jps | grep -i $className | awk '{print $1}'`


if [ "$pid" != "" ]
then
        kill $pid
        echo "kill pid "$pid
        sleep 15
        pids=`/usr/local/java/bin/jps | grep -i $className | awk '{print $1}'`
        if [ "$pids" != "" ]
        then
                kill -9 $pids
                echo “kill -9”$pids
        fi
    else
    echo "pid: not found"
fi
