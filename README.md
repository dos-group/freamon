# Freamon
Lightweight monitoring of the resource usage of containerized YARN applications 


## File structure
- [`doc`](https://github.com/citlab/freamon/tree/master/doc): documentation including example configurations
- [`sbin`](https://github.com/citlab/freamon/tree/master/sbin): scripts for installation and usage
- `freamon-collector`: tools for collecting resource usage statistics
- `freamon-monitor`: actor system that manages the distributed statistics collection
- `freamon-results`: tools for storing the collected statistics in a database
- `freamon-yarn-client`: tools for communicating with YARN


## Building

Freamon depends on [monetdb-jdbc-2.19.jar](https://www.monetdb.org/downloads/Java/Jul2015-SP4/monetdb-jdbc-2.19.jar).
Install it from source, or download it and run

    mvn install:install-file -Dfile=/path/to/monetdb-jdbc-2.19.jar -DgroupId=monetdb -DartifactId=monetdb-jdbc -Dversion=2.19 -Dpackaging=jar

Then you can compile and package Freamon:

	git clone https://github.com/citlab/freamon.git
	cd freamon
	mvn package
	# or, without running the tests:
	mvn package -DskipTests=true

If your system has no cgroups (eg. Mac OS X), you cannot run the tests, so you need to disable them with the `-DskipTests=true` option.


## Installation
First you need to decide where which system will run.

The Freamon master and MonetDB need to be on the same node.
This node also needs passwordless SSH access to the nodes where the Yarn containers will run
(they are usually listed in `$HADOOP_PREFIX/etc/hadoop/slaves`).
The Freamon master does not need to be on the same node as the Hadoop Master.

If you did not compile Freamon yourself, you need to put the Jar into `freamon-monitor/target/`
and name it `freamon-monitor-1.0-SNAPSHOT-allinone.jar`, so the start script can find it.
The Jar needs to be available under the same path on every node.

### MonetDB
On the node that Freamon will run on, MonetDB needs to be installed. To install it execute as root:

    curl https://www.monetdb.org/downloads/epel/monetdb.repo > /etc/yum.repos.d/monetdb.repo
    rpm --import https://dev.monetdb.org/downloads/MonetDB-GPG-KEY
    yum install MonetDB-SQL-server5 MonetDB-client

### Sigar
[Download `hyperic-sigar-1.6.4.tar.gz`](https://support.hyperic.com/display/SIGAR/Home#Home-download)
and extract it to `lib/hyperic-sigar-1.6.4/`:

    mkdir lib
    cd lib
    tar xzvf /path/to/hyperic-sigar-1.6.4.tar.gz


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

Otherwise, just setup Hadoop as usual; no special configuration is needed:

- configure `core-site.xml`, `hdfs-site.xml`, `yarn-site.xml`, `slaves` (all in `$HADOOP_PREFIX/etc/hadoop/`)
- set `JAVA_HOME` in `$HADOOP_PREFIX/etc/hadoop/hadoop-env.sh`
- format namenodes: `pssh -h $HADOOP_PREFIX/etc/hadoop/slaves $HADOOP_PREFIX/bin/hdfs namenode -format`

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
Start Hadoop as usual, for example run on the Hadoop master node:

    $HADOOP_PREFIX/start-dfs.sh
    $HADOOP_PREFIX/start-yarn.sh

If MonetDB is not already running, start it:

    monetdbd start "$FARMDIR"

Now you can start Freamon with your cluster configuration. Run this on the master node:

    sbin/start-cluster.sh myCluster.conf

This starts Freamon on the master node and all slave nodes.
You can find the PID and log files at `logs/`.
Note that you can run multiple Freamon instance at the same time, as their
log and PID file names contain the name of the node on which they are running.

### Using with YARN Workload Runner
Start your jobs from YARN Workload Runner as usual.
Freamon will be notified for all jobs individually
and collect the resource usage data of each participating container.

### Stopping
When you are done running your experiments, stop Freamon:

    sbin/stop-cluster.sh myCluster.conf

If you want to stop MonetDB, run:

    monetdbd stop "$FARMDIR"

To stop Hadoop, you can run on the Hadoop master node:

    $HADOOP_PREFIX/stop-yarn.sh
    $HADOOP_PREFIX/stop-dfs.sh

### Results analysis
The collected resource usage statistics are now stored in the database.

#### Graphical analysis
To analyze the usage of a single resource used by each of the containers of a job,
you can plot it using [dstat-tools](https://github.com/citlab/dstat-tools):

    sbin/graph.sh <application_ID> <type>

`type` is one of `cpu`, `mem`, `net`, `blkio`.

This creates a file named `application_<ID>_<type>.png`.

#### Database format
You can access the database via SQL.
The data is stored in the following tables:

##### experiment_jobs
| id | app_id | start | stop | framework | signature | dataset_size | num_containers | cores_per_container | memory_per_container |
|---|---|---|---|---|---|---|---|---|---|
| 234 | application_1465933590123_0001 | 1465933591111 | 1465933690123 | Flink | `cafebabe` | 9001 | 4 | -1 | -1 |
| 345 | application_1465933590123_0002 | 1465933790123 | 1465933890123 | Flink | `cafebabe` | 9001 | 4 | -1 | -1 |
- `start`, `stop`: timestamp when the job was started/stopped, in milliseconds since the Unix epoch
- `signature`: a unique identifier for the application, for example the jarfile hash
- `dataset_size`: size in MB of the dataset that was processed

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

## Akka Messages API
The following messages can be sent to Freamon's Actor System.

##### `FindPreviousRuns(signature: String)`
Search the DB for previous jobs of applications with the same signature.

Freamon will respond with a `PreviousRuns` message with the following fields:
- `scaleOuts`: `Array[Integer]`
- `runtimes`: `Array[Double]`

##### `ApplicationStart(applicationId: String, startTime: Long)`
Tell Freamon to start monitoring for the provided application.
The `startTime` (in milliseconds since epoch) will be stored in the DB and is provided because Freamon currently has no method to find out when an application started.

##### `ApplicationStop(applicationId: String, stopTime: Long)`
Tell Freamon to stop monitoring for the provided application.
The `stopTime` (in milliseconds since epoch) will be stored in the DB and is provided because Freamon currently has no method to find out when an application stopped.

##### `ApplicationMetadata`
Provide Freamon with metadata about a job.
Freamon will store the metadata with the job in the DB for later `FindPreviousRuns` requests.

The message has the following fields:
- `appId`: `String`, must be set
- `framework`: `Symbol`, defaults to `Symbol(null)`
- `signature`: `String`, defaults to `null`
- `datasetSize`: `Int`, defaults to 0
- `coresPerContainer`: `Int`, defaults to 0
- `memoryPerContainer`: `Int`, defaults to 0
