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

echo "Finding execution units..."
execUnits="`mclient -d freamon -f csv -s "
    select distinct execution_unit.id
    from event, execution_unit, job
    where value > 0
      and yarn_application_id = '$app_id'
      and job.id = execution_unit.job_id
      and execution_unit.id = event.execution_unit_id;"`"

echo "Collecting data for each execution_unit..."
for execUnit in $execUnits; do
    query="
        select millis * 0.001, value
        from event
        where kind='$event_kind'
          and execution_unit_id='$execUnit'
        order by millis asc;"
    # this filename appears in the plot
    csv_path="$tmp_csv_dir/$execUnit.csv"
    mclient -d freamon -f csv -s "$query" > "$csv_path"
done

ruby "$sbin/dstat_plot.rb" -l 1 -y 1 "$tmp_csv_dir" -o "$img_path" -t "$event_kind $app_id"

rm -rf "$tmp_csv_dir"
