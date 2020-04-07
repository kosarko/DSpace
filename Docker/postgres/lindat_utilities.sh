#!/bin/bash
psql -U postgres -c "CREATE ROLE dspace PASSWORD 'md560f94872869f54c7caa55c7c7c3760c5' NOSUPERUSER NOCREATEDB NOCREATEROLE INHERIT LOGIN;"
createdb -U postgres "dspace" --owner dspace --encoding "UTF-8" --template=template0
createdb -U postgres "utilities" --owner dspace --encoding "UTF-8" --template=template0
psql --set=utildir="/tmp/utilities" "utilities" < /tmp/utilities/utilities.sql

