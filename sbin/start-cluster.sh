#!/bin/bash

# Usage: ./start-cluster.sh <CONFIG>
# - CONFIG: cluster config to use
# Example: sbin/start-cluster.sh doc/freamon/myCluster.conf

# Preparation:
# - build system: mvn clean package
# - extract hyperic-sigar-1.6.4.tar.gz to /your/freamon/installation/lib/hyperic-sigar-1.6.4/

# Assumptions:
# - execute this script on the node that runs the master
# - the script assumes passwordless ssh access to all slaves and this project dir on all workers
# - the HADOOP_PREFIX environment variable should be set, both when running this script and
#   when executing commands on the slaves via ssh (export it in ~/.bashrc for example)

# TODO: better argument parsing, add -c flag for the config argument

cd "$(dirname $BASH_SOURCE)/.."

CLUSTER_CONFIG="$(readlink -f "$1")"

SLAVES_FILE="$HADOOP_PREFIX/etc/hadoop/slaves"
ABSOLUTE_JAR_PATH="$(readlink -f freamon-monitor/target/freamon-monitor-1.0-SNAPSHOT-allinone.jar)"
LD_LIBRARY_PATH="$(readlink -f "lib/hyperic-sigar-1.6.4/sigar-bin/lib/")"
LOG_FOLDER="$(readlink -f "logs")"
MASTER_CLASS="de.tuberlin.cit.freamon.monitor.actors.MonitorMasterSystem"
MASTER_LOG_FILE="$LOG_FOLDER/$HOSTNAME-master.out"
MASTER_ERR_FILE="$LOG_FOLDER/$HOSTNAME-master.err"
MASTER_PID_FILE="$LOG_FOLDER/$HOSTNAME-master.pid"
WORKER_CLASS="de.tuberlin.cit.freamon.monitor.actors.MonitorAgentSystem"
JAVA_BIN='/usr/lib/jvm/jre-1.8.0/bin/java'

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
$JAVA_BIN -cp $ABSOLUTE_JAR_PATH $MASTER_CLASS -c $CLUSTER_CONFIG >>$MASTER_LOG_FILE 2>>$MASTER_ERR_FILE & echo $! >$MASTER_PID_FILE
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
