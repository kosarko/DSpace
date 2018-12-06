package cz.cuni.mff.ufal.dspace.discovery;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.*;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.identifier.IdentifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.cuni.mff.ufal.IsoLangCodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * Keeps most of our search query/index tweaks
 * 
 * @author LINDAT/CLARIN team
 *
 */
public class SolrServiceTweaksPlugin implements SolrServiceIndexPlugin,
        SolrServiceSearchPlugin
{
    private static final Logger log = LoggerFactory
            .getLogger(SolrServiceTweaksPlugin.class);

    private IdentifierService identifierService;

    @Autowired
    @Required
    public void setIdentifierService(IdentifierService identifierService){
        this.identifierService = identifierService;
    }

    @Override
    public void additionalSearchParameters(Context context,
            DiscoverQuery discoveryQuery, SolrQuery solrQuery)
    {
    	// This method is called from SolrServiceImpl.resolveToSolrQuery
    	// at this point the solrQuery object should already have the required query and parameters coming from discoveryQuery
    	
    	// get the current query
        String query = solrQuery.getQuery();
        
        // the query terms must occur in the search results
    	query  = "+(" + query + ")";  

    	// if the query terms are in the title increase the significance of search result
    	query += " OR title:(" + query + ")^2";

    	// if a new version of item available increase the significance of newer item (as the newer version should contain dc.relation.replaces)
    	query += " OR dc.relation.replaces:[* TO *]^2";

    	// if more than one version of the item is available increase the significance of the newest
    	// (should contain dc.relation.replaces but should not contain dc.relation.isreplacedby)
    	query += " OR (dc.relation.replaces:[* TO *] AND -dc.relation.isreplacedby:[* TO *])^2";

    	// set the updated query back to solrQuery
    	solrQuery.setQuery(query);    
    }

    @Override
    public void additionalIndex(Context context, DSpaceObject dso,
            SolrInputDocument document)
    {
        if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item) dso;
            //create our filter values
            List<DiscoveryConfiguration> discoveryConfigurations;
            try
            {
                discoveryConfigurations = SearchUtils
                        .getAllDiscoveryConfigurations(item);
                Map<String, List<DiscoverySearchFilter>> searchFilters = new HashMap<String, List<DiscoverySearchFilter>>();
                // read config
                // partly yanked from SolrServiceImpl
                for (DiscoveryConfiguration discoveryConfiguration : discoveryConfigurations)
                {
                    for (int i = 0; i < discoveryConfiguration
                            .getSearchFilters().size(); i++)
                    {
                        DiscoverySearchFilter discoverySearchFilter = discoveryConfiguration
                                .getSearchFilters().get(i);
                        for (int j = 0; j < discoverySearchFilter
                                .getMetadataFields().size(); j++)
                        {
                            String metadataField = discoverySearchFilter
                                    .getMetadataFields().get(j);
                            List<DiscoverySearchFilter> resultingList;
                            String type = discoverySearchFilter.getType();
                            // Process only our new types
                            if (type.equals(DiscoveryConfigurationParameters.TYPE_RAW)
                                    || type.equals(DiscoveryConfigurationParameters.TYPE_ISO_LANG))
                            {
                                if (searchFilters.get(metadataField) != null)
                                {
                                    resultingList = searchFilters
                                            .get(metadataField);
                                }
                                else
                                {
                                    // New metadata field, create a new list for
                                    // it
                                    resultingList = new ArrayList<DiscoverySearchFilter>();
                                }
                                resultingList.add(discoverySearchFilter);

                                searchFilters.put(metadataField, resultingList);
                            }
                        }
                    }
                }
                
                for (Map.Entry<String, List<DiscoverySearchFilter>> entry : searchFilters
                        .entrySet())
                {
                	//clear any input document fields we are about to add lower
                    //String metadataField = entry.getKey();
                    List<DiscoverySearchFilter> filters = entry.getValue();
                    for (DiscoverySearchFilter filter : filters)
                    {
                    	String name = filter.getIndexFieldName();
                    	String[] names = {name, name + "_filter", name + "_keyword",
                    			name + "_ac"};
                    	for(String fieldName : names){
                    		document.removeField(fieldName);
                    	}
                    }
                }

                for (Map.Entry<String, List<DiscoverySearchFilter>> entry : searchFilters
                        .entrySet())
                {
                    String metadataField = entry.getKey();
                    List<DiscoverySearchFilter> filters = entry.getValue();
                    Metadatum[] mds = item
                            .getMetadataByMetadataString(metadataField);
                    for (Metadatum md : mds)
                    {
                        String value = md.value;
                        for (DiscoverySearchFilter filter : filters)
                        {
                            if (filter
                                    .getFilterType()
                                    .equals(DiscoverySearchFilterFacet.FILTER_TYPE_FACET))
                            {
                                String convertedValue = null;
                                if (filter
                                        .getType()
                                        .equals(DiscoveryConfigurationParameters.TYPE_RAW))
                                {
                                    // no lowercasing and separators for this
                                    // type
                                    convertedValue = value;
                                }
                                else if (filter
                                        .getType()
                                        .equals(DiscoveryConfigurationParameters.TYPE_ISO_LANG))
                                {
                                    String langName = IsoLangCodes
                                            .getLangForCode(value);
                                    if (langName != null)
                                    {
                                        convertedValue = langName.toLowerCase()
                                                + SolrServiceImpl.FILTER_SEPARATOR
                                                + langName;
                                    }
                                    else
                                    {
                                        log.error(String
                                                .format("No language found for iso code %s",
                                                        value));
                                    }
                                }
                                if (convertedValue != null)
                                {
                                    document.addField(
                                            filter.getIndexFieldName()
                                                    + "_filter", convertedValue);
                                }
                            }

                            if (filter
                                    .getType()
                                    .equals(DiscoveryConfigurationParameters.TYPE_ISO_LANG))
                            {

                                String langName = IsoLangCodes
                                        .getLangForCode(value);
                                if (langName != null)
                                {
                                    document.addField(
                                            filter.getIndexFieldName(),
                                            langName);
                                    document.addField(
                                            filter.getIndexFieldName()
                                                    + "_keyword", langName);
                                    document.addField(
                                            filter.getIndexFieldName() + "_ac",
                                            langName);
                                    //this should ensure it's copied into the default search field
                                    document.addField(
                                            "dc.language.name",
                                            langName);
                                }
                                else
                                {
                                    log.error(String
                                            .format("No language found for iso code %s",
                                                    value));
                                }
                            }
                        }
                    }
                }
            }
            catch (SQLException e)
            {
                log.error(e.getMessage());
            }
            //process item metadata
            //just add _comp to local*
            Metadatum[] mds = item.getMetadata("local", Item.ANY, Item.ANY, Item.ANY);
            for(Metadatum meta : mds){
            	String field = meta.schema + "." + meta.element;
                String value = meta.value;
                if (value == null) {
                    continue;
                }
                if (meta.qualifier != null && !meta.qualifier.trim().equals("")) {
                    field += "." + meta.qualifier;
                }
            	document.addField(field + "_comp", value);
            }

            //create handle_title_ac field
            String title = item.getName();
            String handle = item.getHandle();
            document.addField("handle_title_ac", handle + ":" + title);

            final CopyPartsMetadataIndexPlugin copyPartsMetadataIndexPlugin = new CopyPartsMetadataIndexPlugin();
            copyPartsMetadataIndexPlugin.setIdentifierService(identifierService);
            copyPartsMetadataIndexPlugin.additionalIndex(context, dso, document);
        }
    }
}
