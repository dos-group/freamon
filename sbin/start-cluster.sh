#!/bin/bash

# Usage: ./start-cluster.sh <CONFIG>
# - CONFIG: cluster config to use
# Example: sbin/start-cluster.sh doc/freamon/cluster.conf

# Preparation:
# - build system: mvn clean package
# - extract hyperic-sigar-1.6.4.tar.gz to /your/freamon/installation/lib/hyperic-sigar-1.6.4/

# Assumptions:
# - execute this script on the node that runs the master
# - the script assumes passwordless ssh access to all slaves
#   and this project directory to exist on all workers
# - the JAVA_HOME and HADOOP_PREFIX environment variables must be set

# TODO: better argument parsing, add -c flag for the config argument

CLUSTER_CONFIG="$(readlink -f "$1")"

# import the shared environment variables
source "$(dirname $BASH_SOURCE)/util/env.sh"

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

if ps -p $(cat "$MASTER_PID_FILE") >/dev/null 2>&1
then
    echo "System appears to be running, stop it first with sbin/stop-cluster.sh"
    exit 1
fi

mkdir -p $LOG_DIR

echo "Using $JAVA_BIN"

echo "Starting freamon master system"
CMD="$JAVA_BIN -cp $ABSOLUTE_JAR_PATH $MASTER_CLASS -c $CLUSTER_CONFIG"
nohup $CMD >>$MASTER_LOG_FILE 2>>$MASTER_ERR_FILE & echo $! >$MASTER_PID_FILE
echo "Started freamon master on $HOSTNAME (PID=$(cat $MASTER_PID_FILE))"

echo "Starting workers from $SLAVES_FILE"
pssh -i -h $SLAVES_FILE "$FREAMON_DIR/sbin/util/start-worker.sh" "$CLUSTER_CONFIG"
