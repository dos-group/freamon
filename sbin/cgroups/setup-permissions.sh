#!/bin/bash

echo 'Please update the installpath variable!'
exit 1

installpath=/path/to/hadoop-VERSION/
chmod 755 $installpath/bin/

chown root $installpath/bin/container-executor
chmod 6050 $installpath/bin/container-executor

chown root $installpath/etc/hadoop/container-executor.cfg
chmod  644 $installpath/etc/hadoop/container-executor.cfg

# every directory from $installpath/etc/hadoop up to / has to be owned by root
currentpath=/
for dir in $(tr / ' ' <<< "$installpath/etc/hadoop/")
do currentpath=$currentpath$dir/
   chown root:ldapusers $currentpath
   chmod 755 $currentpath
done

# YARN cannot create log folder when directory is owned by root
mkdir $installpath/logs
chown bbdc:ldapusers $installpath/logs
