# Cgroups setup scripts for YARN
Before using the scripts, update the configuration in `conf.sh`.

### setup-permissions.sh
Sets the necessary file/directory permissions to use cgroups with YARN.

### create-all-cgroups.sh
Creates the cgroup hierarchy for YARN under all configured subsystems on all hadoop slaves.

### create-cgroup.sh
Helper script for `create-all-cgroups.sh`.

Creates the cgroup hierarchy for YARN under one subsystem.

This script takes exactly one parameter, the subsystem name under which to place the new hierarchy.
