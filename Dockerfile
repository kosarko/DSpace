FROM repo_base:v1 as copied
USER root
COPY ./ /srv/dspace-src
RUN rm -rf /srv/dspace-src/utilities/project_helpers/sources && \
    ln -s /srv/dspace-src /srv/dspace-src/utilities/project_helpers/sources
COPY Docker/repo_overrides/webapp/rest/web.xml /srv/dspace-src/dspace-rest/src/main/webapp/WEB-INF/web.xml
COPY Docker/repo_overrides/webapp/solr/web.xml /srv/dspace-src/dspace-solr/src/main/webapp/WEB-INF/web.xml
COPY Docker/repo_overrides/variable.makefile /srv/dspace-src/utilities/project_helpers/config
WORKDIR /srv/dspace-src/utilities/project_helpers/scripts
RUN chown -R developer:developer /srv/dspace-src
USER developer
RUN make deploy_guru

#FROM base as final
#COPY --from=copied /srv/dspace /srv/dspace
#COPY --from=copied /opt/lindat-common /opt/lindat-common
USER root
RUN mkdir -p /srv/dspace/assetstore && mkdir -p /srv/dspace/log && chown -R developer:developer /srv/dspace/log /srv/dspace/assetstore
USER developer
WORKDIR /srv/dspace

CMD ["/usr/local/tomcat/bin/catalina.sh", "jpda", "run"]
#VOLUME ["/srv/dspace"] - don't do this newly compiled version will be overriden by the one in VOLUME
VOLUME ["/srv/dspace/assetstore",  "/srv/dspace/log",  "/srv/dspace/solr"]
