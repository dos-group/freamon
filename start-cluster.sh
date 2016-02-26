#!/bin/bash

# usage: ./start-cluster [HOST]
# - HOST: host-config to use, if no host is given $HOSTNAME is used by default by the actor systems
#         (see freamon-monitor/src/main/resources/hosts), e.g. ./start-cluster wally

# preparation:
# 1. provide a slaves file, e.g. by soft-linking Hadoop's slave file or
#    one from freamon-monitor/src/main/resources/hosts/
# 2. build system: mvn clean package

# assumptions:
# - execute this script on the node that runs the master
# - the script assumes passwordless ssh-access to all slaves and this project dir on all workers

# TODO: better argument parsing, add -h flag for the host argument

HOST_CONFIG=$1

SCRIPT_DIR="$(dirname $BASH_SOURCE)"
SLAVES_FILE="$PWD/slaves"
RELATIVE_JAR_PATH="freamon-monitor/target/freamon-monitor-1.0-SNAPSHOT-allinone.jar"
ABSOLUTE_JAR_PATH="$PWD/$RELATIVE_JAR_PATH"
LOG_FOLDER="$PWD/logs"
MASTER_CLASS="de.tuberlin.cit.freamon.monitor.actors.MonitorMasterSystem"
MASTER_LOG_FILE="$LOG_FOLDER/$HOSTNAME-master.out"
MASTER_ERR_FILE="$LOG_FOLDER/$HOSTNAME-master.err"
MASTER_PID_FILE="$LOG_FOLDER/$HOSTNAME-master.pid"
WORKER_CLASS="de.tuberlin.cit.freamon.monitor.actors.MonitorAgentSystem"
JAVA_BIN='/usr/lib/jvm/jre-1.8.0/bin/java'

if [ ! -f "$ABSOLUTE_JAR_PATH" ]
then
    echo "Jar not found, build freamon-monitor first"
    exit 1
fi

if [ ! -f "$SLAVES_FILE" ]
then
    echo "No slaves file $SLAVES_FILE"
    exit 1
fi

if [ -f "$MASTER_PID_FILE" ]
then
    echo "System appears to be running, stop it first with ./stop-cluster.sh"
    exit 1
fi

mkdir -p $LOG_FOLDER

echo "Using $JAVA_BIN"

echo "Starting freamon master system"
$JAVA_BIN -cp $ABSOLUTE_JAR_PATH $MASTER_CLASS -h $HOST_CONFIG >>$MASTER_LOG_FILE 2>>$MASTER_ERR_FILE & echo $! >$MASTER_PID_FILE
echo "Started freamon master on $HOSTNAME (PID=$(cat $MASTER_PID_FILE))"

for SLAVE in $(cat $SLAVES_FILE ) ; do

    echo "Starting freamon worker system on $SLAVE"

    WORKER_LOG_FILE="$LOG_FOLDER/$SLAVE-worker.out"
    WORKER_ERR_FILE="$LOG_FOLDER/$SLAVE-worker.err"
    WORKER_PID_FILE="$LOG_FOLDER/$SLAVE-worker.pid"

    CMD="$JAVA_BIN -cp $ABSOLUTE_JAR_PATH $WORKER_CLASS -h $HOST_CONFIG"
    ssh "$SLAVE" "nohup $CMD >>$WORKER_LOG_FILE 2>>$WORKER_ERR_FILE & echo \$!" >$WORKER_PID_FILE

    echo "Started freamon worker system on $SLAVE (PID=$(cat $WORKER_PID_FILE))"

done
