# Freamon
Monitoring the resource usage of distributed YARN containers


## File structure
- [`doc`](https://github.com/citlab/freamon/tree/master/doc): documentation including example configurations
- [`sbin`](https://github.com/citlab/freamon/tree/master/sbin): scripts for installation and usage
- `freamon-collector`: tools for collecting resource usage statistics
- `freamon-monitor`: actor system that manages the statistics collection
- `freamon-results`: tools for storing the collected statistics in a database
- `freamon-yarn-client`: tools for communicating with YARN


## Building

You need [monetdb-jdbc-2.19.jar](https://www.monetdb.org/downloads/Java/Jul2015-SP4/monetdb-jdbc-2.19.jar)
and [YarnWorkloadRunner](https://github.com/citlab/yarn-workload-runner/).
Install them from source, or download them and run

    mvn install:install-file -Dfile=/path/to/monetdb-jdbc-2.19.jar -DgroupId=monetdb -DartifactId=monetdb-jdbc -Dversion=2.19 -Dpackaging=jar
    mvn install:install-file -Dfile=/path/to/YarnWorkloadRunner-1.0-SNAPSHOT.jar -DgroupId=de.tuberlin.cit -DartifactId=YarnWorkloadRunner -Dversion=1.0-SNAPSHOT -Dpackaging=jar

Then you can create the Freamon Jar: `mvn package`

If your system has no cgroups (eg. Mac OS X), you cannot run the tests, so you need to disable them with the `-DskipTests=true` option.


## Installation
First you need to decide where which system will run.

The Freamon master and MonetDB need to be on the same node.
This node also needs passwordless SSH access to the nodes where the Yarn containers will run
(they are usually listed in `$HADOOP_PREFIX/etc/hadoop/slaves`).

> TODO hadoop master vs freamon master

> TODO where to put freamon jar, works as-is because start-cluster.sh looks in target/ which is cloned on every node

### MonetDB
On the node that Freamon will run on, execute as root:

    curl https://www.monetdb.org/downloads/epel/monetdb.repo > /etc/yum.repos.d/monetdb.repo
    rpm --import https://dev.monetdb.org/downloads/MonetDB-GPG-KEY
    yum install MonetDB-SQL-server5 MonetDB-client

### Sigar
> TODO extract, see LD_LIBRARY_PATH in start-cluster.sh


## Configuration
The `JAVA_HOME` and `HADOOP_PREFIX` environment variables are used by the start script and by Hadoop.
Add them to your `~/.bashrc`:

    export JAVA_HOME="/usr/lib/jvm/jre"
    export HADOOP_PREFIX="/path/to/hadoop"

If you need to manage multiple Freamon/Hadoop instances at the same time,
you can set them manually each time you run anything.

### Configuring Hadoop
If you want to use cgroups, follow [doc/hadoop/YARN-with-cgroups.md](https://github.com/citlab/freamon/blob/master/doc/hadoop/YARN-with-cgroups.md).
Example configuration files for this can be found at [doc/hadoop/](https://github.com/citlab/freamon/tree/dev/doc/hadoop) as well.

Otherwise, just setup Hadoop as usual; no special configuration is needed.

### Configuring MonetDB
> For using the default configuration, just run `sbin/monet-create.sh`.
> It creates the DB farm directory at `monetdb-farm` and a database named `freamon`.

First, generate a new directory for the DB farm:

    monetdbd create "$FARMDIR"

You then need to start the daemon:

    monetdbd start "$FARMDIR"

Now you can create a new database for Freamon:

    monetdb create "$DBNAME"
    monetdb release "$DBNAME"

If you do not want it to run for now, you can stop it:

    monetdbd stop "$FARMDIR"

### Configuring Freamon
Create a copy of [`doc/freamon/cluster.conf`](https://github.com/citlab/freamon/blob/master/doc/freamon/cluster.conf) and configure it according to your setup.

- `freamon.hosts.master.hostname` should be an address that can be resolved from every slave.
- `freamon.monetdb.name` is the database name you used when [configuring MonetDB](#configuring-monetdb).

### Configuring YARN Workload Runner
Just set `<notifyFreamon>true</notifyFreamon>` in `config.xml`
and fill in the other configuration values according to your `cluster.conf`.


## Usage
Make sure the `JAVA_HOME` and `HADOOP_PREFIX` environment variables are set, they are used by the start script and by Hadoop.

### Starting
If MonetDB is not already running, start it:

    monetdbd start "$FARMDIR"

Now you can start Freamon with your cluster configuration. Run this on the master node:

    sbin/start-cluster.sh myCluster.conf

This starts Freamon on the master node and all slave nodes.
You can find the PID and log files at `logs/`.
Note that you can run multiple Freamon instance at the same time.

> TODO what if startup fails (logs, pids)

### Using with YARN Workload Runner
Start your jobs from YARN Workload Runner as usual.
Freamon will be notified for all jobs individually
and collect the resource usage data of each participating container.

### Stopping
When you are done running your experiments, stop Freamon:

    sbin/start-cluster.sh myCluster.conf

If you want to stop MonetDB, run:

    monetdbd stop "$FARMDIR"

### Results analysis
The collected resource usage statistics are now stored in the database.

#### Graphical analysis
To analyze the usage of a single resource, compared across all nodes,
you can plot it as a graph using [dstat-tools](https://github.com/citlab/dstat-tools):

    sbin/graph.sh <application_ID> <type>

`type` is one of `cpu`, `mem`, `net`, `blkio`.

This creates a file named `application_<ID>_<type>.png`.

#### Database format
You can access the database via SQL to create your own insights.
The data is distributed across the following tables:

##### experiment_jobs
| id | app_id | start | stop | framework | num_containers | cores_per_container | memory_per_container |
|---|---|---|---|---|---|---|---|
| 234 | application_1465933590123_0001 | 1465933591111 | 1465933690123 | Flink | 4 | -1 | -1 |
| 345 | application_1465933590123_0002 | 1465933790123 | 1465933890123 | Flink | 4 | -1 | -1 |
- `start`, `stop`: timestamp when the job was started/stopped, in milliseconds since the Unix epoch

Note that `framework`, `cores_per_container`, and `memory_per_container` are not collected yet,
so they are always set to `Freamon` and `-1` respectively.

##### experiment_containers
| id | container_id | job_id | hostname |
|---|---|---|---|
| 123 | container_1465933590123_0001_01_000001 | 234 | `monitorSystem@node1.example.com:4321` |
| 124 | container_1465933590123_0001_01_000002 | 234 | `monitorSystem@node2.example.com:4321` |

##### experiment_events
| container_id | job_id | kind | millis | value |
|---|---|---|---|---|
| 987 | 234 | cpu | 1465933592209 | 1.234 |
| 876 | 234 | mem | 1465933592209 | 4321 |

- `kind`: type of the measurement, one of `cpu`, `mem`, `net`, `blkio`
- `millis`: timestamp when the measurement occured, in milliseconds since the Unix epoch
