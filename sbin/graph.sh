#!/bin/bash
sbin="$(dirname $BASH_SOURCE)"

app_id="$1"
event_kind="$2"

if [[ ! "$event_kind" ]]
then
    echo "Usage: $0 <app id> <event kind>"
    exit 1
fi

tmp_csv_dir="/tmp/freamon-graph-csv"
img_path="${app_id}_$event_kind.png"

rm -rf "$tmp_csv_dir"
mkdir -p "$tmp_csv_dir"

echo "Finding workers..."
workers="`mclient -d freamon -f csv -s "
    select distinct worker.container_id
    from event, worker, job
    where value > 0
      and app_id = '$app_id'
      and job.id = worker.job_id
      and worker.id = event.worker_id;"`"

echo "Collecting data for each worker..."
for worker in $workers; do
    query="
        select millis * 0.001, value
        from event, worker
        where kind='$event_kind'
          and worker.container_id='$worker'
          and worker.id = event.worker_id
        order by millis asc;"
    # this filename appears in the plot
    csv_path="$tmp_csv_dir/$worker.csv"
    mclient -d freamon -f csv -s "$query" > "$csv_path"
done

ruby "$sbin/dstat_plot.rb" -l 1 -y 1 "$tmp_csv_dir" -o "$img_path" -t "$event_kind $app_id"

rm -rf "$tmp_csv_dir"
