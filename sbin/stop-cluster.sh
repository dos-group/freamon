#!/bin/bash

# Assumptions:
# - execute this script on the node that runs the master
# - the script assumes passwordless ssh access to all slaves and this project dir on all workers
# - the HADOOP_PREFIX environment variable should be set

freamon="$(readlink -f "$(dirname $BASH_SOURCE)/..")"

SLAVES_FILE="$HADOOP_PREFIX/etc/hadoop/slaves"
LOG_FOLDER="$freamon/logs"
MASTER_PID_FILE="$LOG_FOLDER/$HOSTNAME-master.pid"

if [ ! -f "$MASTER_PID_FILE" ]
then
    echo "Master system does not appear to be running"
else
    MASTER_PID="$(cat "$MASTER_PID_FILE")"
    echo "Stopping master system on $HOSTNAME (PID=$MASTER_PID)"
    kill -9 "$(cat "$MASTER_PID_FILE")"
    rm "$MASTER_PID_FILE"
fi

for SLAVE in $(cat "$SLAVES_FILE"); do

    WORKER_PID_FILE="$LOG_FOLDER/$SLAVE-worker.pid"

    if [ ! -f "$WORKER_PID_FILE" ]
    then
        echo "Worker system does not appear to be running on $SLAVE"
    else
        WORKER_PID="$(cat $WORKER_PID_FILE)"
        echo "Stopping worker system on $SLAVE (PID=$WORKER_PID)"
        ssh $SLAVE "kill $WORKER_PID 2>/dev/null >/dev/null"
        rm $WORKER_PID_FILE
    fi
done
