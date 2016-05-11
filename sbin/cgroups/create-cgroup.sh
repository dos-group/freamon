#!/bin/bash
if [[ ! "$1" ]]
then
    echo "Usage: $0 <single cgroup hierarchy, eg. \"cpu,cpuacct\">"
    exit 1
fi

source "$(dirname $BASH_SOURCE)/conf.sh" || exit 1

grouppath="$CGROUPS_MOUNT/$1/$CGROUPS_HIERARCHY"
echo "Creating cgroup hierarchy at $grouppath"

mkdir "$grouppath"
chown -R ":$USER_GROUP" "$grouppath"
chmod -R g+w "$grouppath"
