package cz.cuni.mff.ufal.dspace.discovery;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.*;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CopyPartsMetadataIndexPlugin implements SolrServiceIndexPlugin {
    private static Logger log = LoggerFactory.getLogger(CopyPartsMetadataIndexPlugin.class);


    IdentifierService identifierService;

    @Autowired
    @Required
    public void setIdentifierService(IdentifierService identifierService){
        this.identifierService = identifierService;
    }

    @Override
    public void additionalIndex(final Context context, DSpaceObject dso, SolrInputDocument document) {
       if(dso.getType() == Constants.ITEM){
            final Item item = (Item)dso;
            if("narrator".equals(item.getMetadata("dc.type"))){
                /* In here we copy interview's solr fields to narrator's document. We don't want to copy solr fields
                that are normally present (we'd have multiple titles, handles, types..), just want to add _filter,
                _keyword, _ac of the associated interviews.
                sharedIndex handles the case where both narrator and interview have the field and we must make sure
                it's copied over
                */
                Set<String> sharedIndex = sharedIndex(item);
                /* When more than one interview, document.getFieldNames contains the _filter etc of the first interview
                added. Keep a copy of the fields before we add anything.
                */
                final Collection<String> noCopyFields = new HashSet<>(document.getFieldNames());
                final Metadatum[] interviews = item.getMetadataByMetadataString("dc.relation.haspart");
                for (Metadatum md : interviews){
                    final String interviewURI = md.value;
                    try{
                        final Item interview = (Item)identifierService.resolve(context, interviewURI);
                        final SolrDocument interviewDoc = new MySolrServiceImpl(context, interview).getDocument();
                        for(String name : interviewDoc.getFieldNames())
                        {
                            if(shouldCopy(name, noCopyFields, sharedIndex)) {
                                for (Object val : interviewDoc.getFieldValues(name)) {
                                    document.addField(name, val);
                                }
                            }
                        }

                    }catch (IdentifierNotResolvableException | IdentifierNotFoundException | SolrServerException e){
                        log.error("Failed to resolve " + interviewURI + "\n" + e.getMessage());
                    }catch (IOException | SQLException e){
                        log.error("Failed to get solr document for " + interviewURI + "\n" + e.getMessage());
                    }
                }
            }

       }
    }

    private Set<String> sharedIndex(Item item){
        HashSet<String> sharedIndex = new HashSet<>();
        sharedIndex.add("search_text");
        final List<DiscoveryConfiguration> discoveryConfigurations;
        try {
            discoveryConfigurations = SearchUtils.getAllDiscoveryConfigurations(item);
            for(DiscoveryConfiguration discoveryConfiguration : discoveryConfigurations){
                final List<DiscoverySearchFilter> searchFilters = discoveryConfiguration.getSearchFilters();
                for(DiscoverySearchFilter searchFilter : searchFilters){
                    String metadataFields = StringUtils.join(searchFilter.getMetadataFields(), ",");
                    if(metadataFields.contains("narrator") && metadataFields.contains("interview")){
                        String name = searchFilter.getIndexFieldName();
                        for(String n : new String[]{name, name + "_ac", name + "_filter", name + "_keyword"}) {
                            sharedIndex.add(n);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return sharedIndex;
    }

    private boolean shouldCopy(String name, Collection<String> noCopyFields, Set<String> sharedIndex){
        boolean indexField =
                name.endsWith("_filter") || name.endsWith( "_keyword") || name.endsWith("_ac") || name.equals( "search_text");
        boolean browseIndex = name.startsWith("bi_");
        boolean copy = !noCopyFields.contains(name);
        boolean shared = sharedIndex.contains(name);
        return (copy || shared) && !browseIndex && indexField;
    }

    static class MySolrServiceImpl extends SolrServiceImpl {
        private final Context context;
        private final Item interview;

        public MySolrServiceImpl(Context context, Item interview) {
            this.context = context;
            this.interview = interview;
        }

        public SolrDocument getDocument() throws IOException, SQLException, SolrServerException {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery("handle:\"" + interview.getHandle() + "\"");
            QueryResponse rsp = getSolr().query(solrQuery);
            SolrDocumentList docs = rsp.getResults();
            if(docs.isEmpty()){
                this.buildDocument(context, interview);
                rsp = getSolr().query(solrQuery);
                docs = rsp.getResults();
            }
            return docs.get(0);
        }
    }
}
