#!/bin/bash

# a directory with this name will be created/used
farmdir="$(dirname $BASH_SOURCE)/../monetdb-farm"

# same as freamon.monetdb.name in hosts.conf
dbname="freamon"

# create the data storage
monetdbd create "$farmdir"
monetdbd start  "$farmdir"

# create the database
monetdb create  "$dbname"
monetdb release "$dbname"

