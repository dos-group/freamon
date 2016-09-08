#!/bin/bash

# Usage: ./start-cluster.sh <CONFIG>
# - CONFIG: cluster config to use
# Example: sbin/start-cluster.sh doc/freamon/cluster.conf

# Preparation:
# - build system: mvn clean package
# - extract hyperic-sigar-1.6.4.tar.gz to /your/freamon/installation/lib/hyperic-sigar-1.6.4/

# Assumptions:
# - execute this script on the node that runs the master
# - the script assumes passwordless ssh access to all slaves and this project dir on all workers
# - the JAVA_HOME and HADOOP_PREFIX environment variables should be set

# TODO: better argument parsing, add -c flag for the config argument

freamon="$(readlink -f "$(dirname $BASH_SOURCE)/..")"

CLUSTER_CONFIG="$(readlink -f "$1")"

EXTRA_COMMAND=$2

EXTRA_FILE="$(readlink -f "$3")"

SLAVES_FILE="$HADOOP_PREFIX/etc/hadoop/slaves"
ABSOLUTE_JAR_PATH="$freamon/freamon-monitor/target/freamon-monitor-1.1-SNAPSHOT-allinone.jar"
LD_LIBRARY_PATH="$freamon/lib/hyperic-sigar-1.6.4/sigar-bin/lib/"
LOG_FOLDER="$freamon/logs"
MASTER_CLASS="de.tuberlin.cit.freamon.monitor.actors.MonitorMasterSystem"
MASTER_LOG_FILE="$LOG_FOLDER/$HOSTNAME-master.out"
MASTER_ERR_FILE="$LOG_FOLDER/$HOSTNAME-master.err"
MASTER_PID_FILE="$LOG_FOLDER/$HOSTNAME-master.pid"
WORKER_CLASS="de.tuberlin.cit.freamon.monitor.actors.MonitorAgentSystem"
JAVA_BIN="$JAVA_HOME/bin/java"

if [ -z "$CLUSTER_CONFIG" ]
then
    echo "Usage: $0 <cluster config path>"
    exit 1
fi

if [ ! -f "$CLUSTER_CONFIG" ]
then
    echo "Cluster config file not found at $CLUSTER_CONFIG"
    exit 1
fi

if [ ! -f "$ABSOLUTE_JAR_PATH" ]
then
    echo "Jar not found, build freamon-monitor first"
    exit 1
fi

if [ ! -f "$SLAVES_FILE" ]
then
    echo "No slaves file $SLAVES_FILE"
    echo "Did you set HADOOP_PREFIX?"
    exit 1
fi

if [ -f "$MASTER_PID_FILE" ]
then
    echo "System appears to be running, stop it first with sbin/stop-cluster.sh"
    exit 1
fi

mkdir -p $LOG_FOLDER

echo "Using $JAVA_BIN"

echo "Starting freamon master system"
$JAVA_BIN -cp $ABSOLUTE_JAR_PATH $MASTER_CLASS -c $CLUSTER_CONFIG $EXTRA_COMMAND $EXTRA_FILE >>$MASTER_LOG_FILE 2>>$MASTER_ERR_FILE & echo $! >$MASTER_PID_FILE
echo "Started freamon master on $HOSTNAME (PID=$(cat $MASTER_PID_FILE))"

for SLAVE in $(cat $SLAVES_FILE ) ; do

    echo "Starting freamon worker system on $SLAVE"

    WORKER_LOG_FILE="$LOG_FOLDER/$SLAVE-worker.out"
    WORKER_ERR_FILE="$LOG_FOLDER/$SLAVE-worker.err"
    WORKER_PID_FILE="$LOG_FOLDER/$SLAVE-worker.pid"

    CMD="$JAVA_BIN -Djava.library.path='$LD_LIBRARY_PATH' -cp '$ABSOLUTE_JAR_PATH' '$WORKER_CLASS' -c '$CLUSTER_CONFIG'"
    ssh "$SLAVE" "nohup $CMD >>$WORKER_LOG_FILE 2>>$WORKER_ERR_FILE & echo \$!" >$WORKER_PID_FILE

    echo "Started freamon worker system on $SLAVE (PID=$(cat $WORKER_PID_FILE))"

done
