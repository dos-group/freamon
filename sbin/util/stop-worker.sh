#!/bin/bash
# Kills a worker and deletes its pid file.

# import the shared environment variables
source "$(dirname $BASH_SOURCE)/env.sh"

if [ ! -f "$WORKER_PID_FILE" ]
then
    echo "Worker system does not appear to be running on $HOSTNAME"
    exit
fi

PID="$(cat $WORKER_PID_FILE)"
if ps -p $(cat "$WORKER_PID_FILE") >/dev/null 2>&1
then
    # TODO check if it's still the freamon process
    echo "Stopping worker system on $HOSTNAME (PID=$PID)"
    kill -9 $PID 2>/dev/null >/dev/null
    rm $WORKER_PID_FILE
else
    echo "Worker already stopped on $HOSTNAME (PID=$PID)"
fi
