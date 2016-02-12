#!/bin/bash

echo 'Please update the installpath variable!'
exit 1

installpath=/path/to/hadoop-VERSION/

chown root $installpath/bin/container-executor
chmod 6050 $installpath/bin/container-executor

chown root $installpath/etc/hadoop/container-executor.cfg
chmod  400 $installpath/etc/hadoop/container-executor.cfg

# every directory from $installpath/etc/hadoop up to / has to be owned by root
currentpath=/
for dir in $(tr / ' ' <<< "$installpath/etc/hadoop/")
do currentpath=$currentpath$dir/
   chown root:ldapusers $currentpath
   chmod 755 $currentpath
done
