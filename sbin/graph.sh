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
    select distinct container_id
    from experiment_event, experiment_job
    where value > 0
      and app_id = '$app_id'
      and experiment_job.id = experiment_event.job_id;"`

echo "Collecting data for each container..."
for container in $containers; do
    query="
        select millis, value
        from experiment_event
        where container_id='$container'
          and kind='$event_kind'
        order by millis asc"
    # filename appears in plot
    csv_path="$tmp_csv_dir/container $container.csv"
    mclient -d freamon -f csv -s "$query;" > "$csv_path"
done

ruby sbin/dstat_plot.rb -l 1 -y 1 "$tmp_csv_dir" -o "$img_path"

rm -rf "$tmp_csv_dir"
