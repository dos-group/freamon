#!/bin/bash
#
# Kills Freamon master and all Agents that are currently running,
# and deletes their pid files.
#
# Assumptions:
# - execute this script on the node that runs the master
# - the script assumes passwordless ssh access to all slaves
#   and this project directory to exist on all workers
# - the HADOOP_PREFIX environment variables must be set

# import the shared environment variables
source "$(dirname $BASH_SOURCE)/util/env.sh"

if [ ! -f "$MASTER_PID_FILE" ]
then
    echo "Master system does not appear to be running"
else
    MASTER_PID="$(cat "$MASTER_PID_FILE")"
    echo "Stopping master system on $HOSTNAME (PID=$MASTER_PID)"
    kill -9 $MASTER_PID
    rm "$MASTER_PID_FILE"
fi

pssh -i -h $SLAVES_FILE "$FREAMON_DIR/sbin/util/stop-worker.sh"
