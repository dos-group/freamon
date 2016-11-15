# Freamon
Lightweight monitoring of the resource usage of distributed data analytics applications

See the [wiki](https://github.com/citlab/freamon/wiki) for information on installation and configuration of Freamon,
information for developers (e.g. building and modules), and additional features like parsing HDFS audit logs and importing existing monitoring data from dstat.

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
To analyze the usage of a single resource used by each of the execution_units of a job,
you can plot it using [dstat-tools](https://github.com/citlab/dstat-tools):

    sbin/graph.sh <application_ID> <type>

`type` is one of the values for `events.kind`, see [events table format](#events).

This creates a file named `application_<ID>_<type>.png`.

#### Database format
You can access the database via SQL.
The data is stored in the following tables:

##### job
```sql
CREATE TABLE job (
    id                   INTEGER     NOT NULL,
    yarn_application_id  VARCHAR(63)         ,
    framework            VARCHAR(63)         ,
    signature            VARCHAR(255)        ,
    input_size           DOUBLE              ,
    num_workers          INTEGER             ,
    cores_per_worker     INTEGER             ,
    memory_per_worker    INTEGER             ,
    start                BIGINT              ,
    stop                 BIGINT              ,
    PRIMARY KEY (id)
)
```

- `start`, `stop`: timestamp when the job was started/stopped, in milliseconds since the Unix epoch
- `signature`: a unique identifier for the application, for example the jarfile hash
- `input_size`: size in MB of the input dataset that was processed

##### execution_unit
```sql
CREATE TABLE execution_unit (
    id                   INTEGER     NOT NULL,
    job_id               INTEGER     NOT NULL,
    hostname             VARCHAR(63)         ,
    is_yarn_container    BOOLEAN             ,
    is_master            BOOLEAN             ,
    container_id         VARCHAR(63)         ,
    PRIMARY KEY (id),
    FOREIGN KEY (job_id) REFERENCES job(id) ON DELETE CASCADE
)
```

- `is_yarn_container`: the entry represents a YARN container, `container_id` is set
- `is_master`: e.g. Flink master, can be used for excluding on plots

##### event
```sql
CREATE TABLE event (
    execution_unit_id INTEGER NOT NULL,
    job_id       INTEGER     NOT NULL,
    kind         VARCHAR(63) NOT NULL,
    millis       BIGINT              ,
    value        DOUBLE              ,
    FOREIGN KEY (execution_unit_id) REFERENCES execution_unit(id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES job(id) ON DELETE CASCADE
)
```

- `kind`: type of the measurement, one of `cpu`, `mem`, `netRx`, `netTx`, `diskRead`, `diskWrite`
- `millis`: timestamp when the measurement occurred, in milliseconds since the Unix epoch

## Akka Messages API
The following messages can be sent to Freamon's Actor System.

##### `FindPreviousRuns(signature: String)`
Search the DB for previous jobs of applications with the same signature.

Freamon will respond with a `PreviousRuns` message with the following fields:
- `scaleOuts`: `Array[Integer]`
- `runtimes`: `Array[Double]`
- `datasetSizes`: `Array[Double]`

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
- `datasetSize`: `Double`, defaults to 0
- `coresPerWorker`: `Int`, defaults to 0
- `memoryPerWorker`: `Int`, defaults to 0
