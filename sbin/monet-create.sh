#!/bin/bash

# a directory with this name will be created/used
farmdir="$(dirname $BASH_SOURCE)/../monetdb-farm"
export FARMDIR=$farmdir

# same as freamon.monetdb.name in hosts.conf
dbname="freamon"

echo Creating data storage at "$farmdir" ...
monetdbd create "$farmdir"

echo Starting MonetDB for "$farmdir" ...
monetdbd start  "$farmdir"

echo Creating database "$dbname" ...
monetdb create  "$dbname"
monetdb release "$dbname"

