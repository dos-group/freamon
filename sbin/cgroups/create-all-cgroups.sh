#!/bin/bash
source "$(dirname $BASH_SOURCE)/conf.sh" || exit 1
# get full path so it will find the script on all slaves
create_cgroup_sh="$(readlink -f "$(dirname $BASH_SOURCE)")/create-cgroup.sh"
pssh -h $HADOOP_PREFIX/etc/hadoop/slaves -P "for s in $CGROUPS_SUBSYSTEMS; do $create_cgroup_sh \"\$s\"; done"
