#!/bin/bash

# assumptions:
# - execute this script on the node that runs the master
# - the script assumes passwordless ssh-access to all slaves and the same project dir on all workers
# - also requires pssh

# arguments
# host configuration to select

# preparation:
# 1. provide a slaves file, e.g. by soft-linking Hadoop's slave file
# 2. build system: mvn clean package

HOST_CONFIG=$1

SCRIPT_DIR="$(dirname $BASH_SOURCE)"
SLAVES_PATH="$PWD/slaves"
RELATIVE_JAR_PATH="freamon-monitor/target/freamon-monitor-1.0-SNAPSHOT-allinone.jar"
ABSOLUTE_JAR_PATH="$SCRIPT_DIR/$RELATIVE_JAR_PATH"

if [ ! -f "$ABSOLUTE_JAR_PATH" ]
then
    echo "Jar not found"
    exit 1
fi

if [ ! -f "$SLAVES_PATH" ]
then
    echo "No slaves file in $PWD"
    exit 1
fi

# TODO: - write outputs into log folder, start java processes with &
# TODO: - write PIDs of the processes in the log folder, provide stop script

echo "Starting freamon master system"
java -cp $ABSOLUTE_JAR_PATH de.tuberlin.cit.freamon.monitor.actors.MonitorMasterSystem -h $HOST_CONFIG


echo "Starting freamon worker systems"
pssh -h $SLAVES_PATH "java -cp $ABSOLUTE_JAR_PATH de.tuberlin.cit.freamon.monitor.actors.MonitorAgentSystem -h $HOST_CONFIG"
