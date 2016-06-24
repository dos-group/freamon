Installing and configuring YARN to use cgroups
==============================================

###Building hadoop

> Wally runs RHEL, where CPU management happens in `/sys/fs/cgroup/cpu,cpuacct/`,
> so a version of hadoop that patches [YARN-2194](https://issues.apache.org/jira/browse/YARN-2194)
> is needed to fix an issue with the comma in that path.
> Until the next hadoop release containing this patch, you need to build it yourself.

Get the latest sources and read [`BUILDING.txt`](https://github.com/apache/hadoop/blob/trunk/BUILDING.txt).
Then run `mvn clean package -Pdist -Pnative -Dtar -DskipTests -Dmaven.javadoc.skip=true`.

Build options:
- `-Pdist` to build the distribution
- `-Pnative` to compile/bundle native code (container-executor binary)
- `-Dtar` to create a TAR with the distribution

Extract the archive to every node at some location where every parent directory can have its owner set to `root` (see [next section](#setting-file-and-directory-permissions)).


###Setting file and directory permissions

Secure Mode requires several files and directories to be set to certain permissions:

| Path after `hadoop-VERSION/`      | Owner          | Permissions
|:----------------------------------|:---------------|:-----------------
| bin/container-executor            | root:ldapusers | `---Sr-s--* 6050`
| etc/hadoop/container-executor.cfg | root:ldapusers | `-r-------*  400`
| etc/hadoop/                       | root:ldapusers | `drwxr-xr-x  755`
| etc/                              | root:ldapusers | `drwxr-xr-x  755`

> On wally, /home/ is mounted from NFS with root squashing enabled.
> Thus, to change file/directory owner/permissions as root, you have to do that from `wally-master`.
> The file/directory owner/permissions then get copied to each node via NFS.

Run `sbin/cgroups/setup-permissions.sh` as root.

This needs to be done only once per hadoop installation.


###Creating the cgroups hierarchies

YARN runs in its own hierarchy (`hadoop-yarn` by default), which has to be created under every subsystem (`cpu,cpuacct`, `memory`, `blkio`, `net_cls`, ...) on every node.

Note that Freamon's collector does not support network statistics at this point (see [wiki/CGroups-in-linux](https://github.com/citlab/freamon/wiki/CGroups-in-linux)).

The cgroup subsystems are empty on system startup, so this has to be done again after a reboot.

For every subsystem you want to use, run `sbin/cgroups/create-cgroup.sh` as root on every slave node, or use `sbin/cgroups/create-all-cgroups.sh` from your master node.


###Configuring YARN

To use cgroups, YARN has to run `LinuxContainerExecutor` instead of `DefaultContainerExecutor`
and `CgroupsLCEResourcesHandler` instead of `DefaultLCEResourcesHandler`,
which in turn requires [Secure Mode](https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/SecureMode.html) to be enabled.

For example configuration settings with explanation, see `doc/hadoop/yarn-site.xml` and `doc/hadoop/container-executor.cfg`.
