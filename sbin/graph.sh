#!/bin/bash
cd "$(dirname $BASH_SOURCE)/.."

app_id="$1"
event_kind="$2"

tmp_csv_dir="/tmp/tmp_csv"
img_path="${app_id}_$event_kind.png"

if [[ ! "$event_kind" ]]
then
    echo "Usage: $0 <app id> <event kind>"
    exit 1
fi

rm -rf "$tmp_csv_dir"
mkdir -p "$tmp_csv_dir"

echo "Finding containers..."
containers=`mclient -d freamon -f csv -s "
    select distinct experiment_container.container_id
    from experiment_event, experiment_container, experiment_job
    where value > 0
      and app_id = '$app_id'
      and experiment_job.id = experiment_container.job_id
      and experiment_container.job_id = experiment_event.job_id;"`

echo "Collecting data for each container..."
for container in $containers; do
    query="
        select millis * 0.001, value
        from experiment_event, experiment_container
        where kind='$event_kind'
          and experiment_container.container_id='$container'
          and experiment_container.id = experiment_event.container_id
        order by millis asc"
    # this filename appears in the plot
    csv_path="$tmp_csv_dir/$container.csv"
    mclient -d freamon -f csv -s "$query;" > "$csv_path"
done

ruby sbin/dstat_plot.rb -l 1 -y 1 "$tmp_csv_dir" -o "$img_path" -t "$event_kind $app_id"

rm -rf "$tmp_csv_dir"
