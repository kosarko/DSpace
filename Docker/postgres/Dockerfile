FROM postgres:9.5
COPY install-pgcrypto.sh /docker-entrypoint-initdb.d/02_install-pgcrypto.sh
COPY lindat_utilities.sh /docker-entrypoint-initdb.d/01_lindat_utilities.sh
COPY utilities /tmp/utilities
