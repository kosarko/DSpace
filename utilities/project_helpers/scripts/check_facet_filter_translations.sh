#!/usr/bin/env bash

MESSAGES_FILE=/srv/dspace-src/dspace/modules/xmlui/src/main/webapp/i18n/messages_cs.xml

xmllint --xpath '//*[local-name()="property" and @name="indexFieldName"]/@value' /srv/dspace-src/dspace/config/spring/api/discovery.xml | sed -e 's# \?value="\([^"]*\)" \?#\1\n#g' | while read field; do
    cat <<-EOF
xmlui.ArtifactBrowser.SimpleSearch.filter.${field}
xmlui.ArtifactBrowser.ConfigurableBrowse.${field}.column_heading
xmlui.ArtifactBrowser.ConfigurableBrowse.title.metadata.${field}
xmlui.ArtifactBrowser.ConfigurableBrowse.trail.metadata.${field}
xmlui.ArtifactBrowser.AdvancedSearch.type_${field}
xmlui.ArtifactBrowser.SimpleSearch.filter.${field}_filter
xmlui.Discovery.AbstractSearch.type_${field}
EOF
done > /tmp/expected_fields

grep "webui\.browse\.index" /srv/dspace-src/dspace/config/dspace.cfg | grep -v "^#" | cut -d= -f2 | cut -d: -f1 | sed -e 's# ##g' | while read field; do
    echo "xmlui.ArtifactBrowser.Navigation.browse_${field}"
done >>/tmp/expected_fields

xmllint --xpath '//message/@key' $MESSAGES_FILE | sed -e 's# \?key="\([^"]*\)" \?#\1\n#g' > /tmp/present_fields
awk 'NR==FNR{a[$0]=$0;next}{if(!a[$0]){print $0}}' /tmp/present_fields /tmp/expected_fields
#rm /tmp/expected_fields /tmp/present_fields