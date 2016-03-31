#!/bin/bash

# user group that the YARN users will run under
# same as yarn.nodemanager.linux-container-executor.group
CONTAINERGROUP=ldapusers

# cgroup hierarchy under which to place YARN proccesses
# can be any path, not just single dir
# same as yarn.nodemanager.linux-container-executor.cgroups.hierarchy
CGROUPSHIERARCHY=hadoop-yarn

# same as yarn.nodemanager.linux-container-executor.cgroups.mount-path
MOUNTPATH=/sys/fs/cgroup

GROUPPATH=${MOUNTPATH}/cpu,cpuacct/${CGROUPSHIERARCHY}
mkdir ${GROUPPATH}
chown -R :${CONTAINERGROUP} ${GROUPPATH}
chmod -R g+w ${GROUPPATH}
