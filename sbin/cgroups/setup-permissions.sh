#!/bin/bash

if [[ ! "$1" ]]
then
    echo "Usage: $0 </path/to/hadoop-VERSION/>"
    exit 1
fi

installpath="$1"
group=ldapusers

chmod 755 $installpath/bin/
chown root:$group $installpath/bin/container-executor
chmod 6050 $installpath/bin/container-executor

chown root:$group $installpath/etc/hadoop/container-executor.cfg
chmod  644 $installpath/etc/hadoop/container-executor.cfg

# every directory from $installpath/etc/hadoop up to / has to be owned by root
currentpath=/
for dir in $(tr / ' ' <<< "$installpath/etc/hadoop/")
do currentpath=$currentpath$dir/
   chown root:$group $currentpath
   chmod 755 $currentpath
done

# YARN cannot create logs/ directory when $installpath/ directory is owned by root
mkdir $installpath/logs
chown bbdc:$group $installpath/logs
