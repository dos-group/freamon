# Freamon
Lightweight monitoring of the resource usage of containerized YARN applications

See the [wiki](https://github.com/citlab/freamon/wiki) for information on installation and configuration of Freamon, information for developers (e.g. building and modules), and additional features like HDFS audit logging and importing existing dstat data.

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

`type` is one of the values for `events.kind`, see [events table format](#events).

This creates a file named `application_<ID>_<type>.png`.
