#!/bin/bash
# This script is called by start-cluster.sh through pssh on each worker.
# It expects a single argument: the full path to the cluster config.

CLUSTER_CONFIG="$1"

# import the shared environment variables
source "$(dirname $BASH_SOURCE)/env.sh"

if ps -p $(cat "$WORKER_PID_FILE") >/dev/null 2>&1
then
    echo "Worker on $HOSTNAME appears to be running, stop it first with sbin/stop-cluster.sh"
    exit 1
fi

mkdir -p $LOG_DIR

echo "Starting freamon worker system on $HOSTNAME"
CMD="$JAVA_BIN -Djava.library.path='$LD_LIBRARY_PATH' -cp '$ABSOLUTE_JAR_PATH' '$WORKER_CLASS' -c '$CLUSTER_CONFIG'"
nohup $CMD >>$WORKER_LOG_FILE 2>>$WORKER_ERR_FILE & echo $! >$WORKER_PID_FILE
echo "Started freamon worker system on $HOSTNAME (PID=$(cat $WORKER_PID_FILE))"
