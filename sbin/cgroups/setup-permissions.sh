#!/bin/bash
source "$(dirname $BASH_SOURCE)/conf.sh" || exit 1

echo "Settung up directory permissions for $HADOOP_PREFIX"

chmod 755 $HADOOP_PREFIX/bin/
chown root:$USER_GROUP $HADOOP_PREFIX/bin/container-executor
chmod 6050 $HADOOP_PREFIX/bin/container-executor

chown root:$USER_GROUP $HADOOP_PREFIX/etc/hadoop/container-executor.cfg
chmod  644 $HADOOP_PREFIX/etc/hadoop/container-executor.cfg

# every directory from $HADOOP_PREFIX/etc/hadoop up to / has to be owned by root
currentpath=/
for dir in $(tr / ' ' <<< "$HADOOP_PREFIX/etc/hadoop/")
do currentpath=$currentpath$dir/
   chown root:$USER_GROUP $currentpath
   chmod 755 $currentpath
done

# YARN cannot create logs/ directory when $HADOOP_PREFIX/ directory is owned by root
mkdir -p $HADOOP_PREFIX/logs
chown $USER_NAME:$USER_GROUP $HADOOP_PREFIX/logs
