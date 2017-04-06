#!/bin/bash
# sets up some shared variables for start-cluster.sh and start-worker.sh

FREAMON_DIR="$(readlink -f "$(dirname $BASH_SOURCE)/../..")"

ABSOLUTE_JAR_PATH="$FREAMON_DIR/freamon-monitor/target/freamon-monitor-1.1-SNAPSHOT-allinone.jar"
LD_LIBRARY_PATH="$FREAMON_DIR/lib/hyperic-sigar-1.6.4/sigar-bin/lib/"
LOG_DIR="$FREAMON_DIR/logs"

SLAVES_FILE="$HADOOP_PREFIX/etc/hadoop/slaves"
JAVA_BIN="$JAVA_HOME/bin/java"

MASTER_CLASS="de.tuberlin.cit.freamon.monitor.actors.MonitorMasterSystem"
MASTER_LOG_FILE="$LOG_DIR/$HOSTNAME-master.out"
MASTER_ERR_FILE="$LOG_DIR/$HOSTNAME-master.err"
MASTER_PID_FILE="$LOG_DIR/$HOSTNAME-master.pid"

WORKER_CLASS="de.tuberlin.cit.freamon.monitor.actors.MonitorAgentSystem"
WORKER_LOG_FILE="$LOG_DIR/$HOSTNAME-worker.out"
WORKER_ERR_FILE="$LOG_DIR/$HOSTNAME-worker.err"
WORKER_PID_FILE="$LOG_DIR/$HOSTNAME-worker.pid"
