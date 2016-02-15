#!/bin/bash

SCRIPT_DIR="$(dirname $BASH_SOURCE)"
SLAVES_PATH="$SCRIPT_DIR/slaves"
LOG_FOLDER="$SCRIPT_DIR/logs"
MASTER_PID_FILE="$LOG_FOLDER/$HOSTNAME-master.pid"

if [ ! -f "$MASTER_PID_FILE" ]
then
    echo "System does not appear to be running."
    exit 1
fi

MASTER_PID=$(cat $MASTER_PID_FILE)
echo "Stopping master system on $HOSTNAME (PID=$MASTER_PID)"
kill -9 $(cat $MASTER_PID_FILE)
rm $MASTER_PID_FILE

while read slave; do
    WORKER_PID_FILE="$LOG_FOLDER/$slave-worker.pid"
    WORKER_PID="$(cat $WORKER_PID_FILE)"
    echo "Stopping master system on $slave (PID=$WORKER_PID)"
    ssh $slave "kill $WORKER_PID 2>/dev/null >/dev/null"
    rm $WORKER_PID_FILE
done <$SLAVES_PATH
