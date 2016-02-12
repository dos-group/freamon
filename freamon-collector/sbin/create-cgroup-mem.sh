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

GROUPPATH=${MOUNTPATH}/memory/${CGROUPSHIERARCHY}
mkdir ${GROUPPATH}
chown :${CONTAINERGROUP} ${GROUPPATH}
chmod g+w ${GROUPPATH}
