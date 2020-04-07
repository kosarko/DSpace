#!/bin/bash
set -o verbose
set -o xtrace
function init_repo {
        for i in `seq 1 11`; do
            if psql -t -U dspace -h postgres -c 'select 1;' >/dev/null; then
                break
            else
                >&2 echo "Waiting for the database ${i}s"
                sleep 1
            fi
        done
        if [ $i -eq 11 ]; then
            >&2 echo "Couldn't connect to database under 10s."
            exit 1
        fi
        adm_email=`psql -t -U dspace -h postgres -c 'select email from eperson where eperson_id = 1;'`
        if [ -z "$adm_email" ]; then
                ADMIN_EMAIL=dspace@lindat.cz
                IMPORT_DEF=/dspace_after_install_init/dspace-import-structure.xml
                IMPORT_OUTPUT=/tmp/import_output.xml
                dspace database migrate
                dspace create-administrator -e "$ADMIN_EMAIL" -f "Mr." -l "Lindat" -p "dspace" -c "en"
                dspace structure-builder -f $IMPORT_DEF -o $IMPORT_OUTPUT -e "$ADMIN_EMAIL"
        fi
}

function deploy_repo {
        pushd /srv/dspace-src/utilities/project_helpers/scripts
        make deploy_guru || true
        popd
}
if readlink -e /srv/dspace/bin/dspace > /dev/null;then
        init_repo;
else
        sudo ln -s /srv/dspace/bin/dspace /usr/bin/dspace
        deploy_repo;
        init_repo;
fi
