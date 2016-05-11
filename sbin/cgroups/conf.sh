#!/bin/bash
# configuration for the cgroups scripts

echo "Please update the configuration in sbin/cgroups/conf.sh before running the scripts."
exit 1

export JAVA_HOME="/usr/lib/jvm/jre"
export HADOOP_PREFIX="/home/bbdc/hadoop/<YOUR_HADOOP>"

# TODO used for setting log dir perms in setup-permissions.sh
USER_NAME=bbdc

# user group that the YARN users will run under
# same as yarn.nodemanager.linux-container-executor.group
USER_GROUP=ldapusers

# cgroups hierarchy under which to place YARN processes
# can be any path, not just a single directory
# same as yarn.nodemanager.linux-container-executor.cgroups.hierarchy
CGROUPS_HIERARCHY=hadoop-yarn

# path to where the OS mounts all cgroup subsystems
# same as yarn.nodemanager.linux-container-executor.cgroups.mount-path
CGROUPS_MOUNT=/sys/fs/cgroup

# space-separated subsystems that YARN will use
CGROUPS_SUBSYSTEMS='cpu,cpuacct memory blkio'
