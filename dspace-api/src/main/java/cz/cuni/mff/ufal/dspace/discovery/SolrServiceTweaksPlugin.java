package cz.cuni.mff.ufal.dspace.discovery;

import java.sql.SQLException;
import java.util.*;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Bitstream;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.cuni.mff.ufal.IsoLangCodes;

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

    static final String[] generatedIndexFieldSuffixes = {"_filter", "_ac", "_keyword", ".year"};

    private static final int FIFTEEN_MINUTES = 15 * 60;
    private static final int FORTYFIVE_MINUTES = 45 * 60;


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
            copyBitstreamMetadataToItem(document, item);
            processAdditionalConfigFilters(document, item);
            createCompFieldsFromLocalMetadata(document, item);
            createHandleTitleAcField(document, item);
            cleanupNarratorInterviewFields(document, item);
        }
    }

    private void copyBitstreamMetadataToItem(SolrInputDocument document, Item item) {
        List<Bitstream> bitstreams = new ArrayList<>();
        try {
            bitstreams.addAll(Arrays.asList(item.getNonInternalBitstreams()));
        }catch (SQLException e){
            log.error(e.getMessage());
        }
        for(Bitstream bitstream : bitstreams){
            List<Metadatum> metadata = bitstream.getMetadata("local", "bitstream", Item.ANY, Item.ANY, Item.ANY);
            for(Metadatum md : metadata){
                String field = md.schema + "." + md.element;
                if(md.qualifier != null && !md.qualifier.isEmpty()){
                    field += "." + md.qualifier;
                }
                document.addField(field, md.value);
            }
        }
    }

    private void processAdditionalConfigFilters(SolrInputDocument document, Item item) {
        // metadataField to list of filters
        Map<String, List<DiscoverySearchFilter>> searchFilters;
        Map<String, List<DiscoverySearchFilter>> bitstreamSearchFilters;
        try {
            Map<String, Map<String, List<DiscoverySearchFilter>>> maps = getFieldToFiltersMaps(item);
            searchFilters = maps.get("search");
            bitstreamSearchFilters = maps.get("bitstream");

            handleOurNewFilterTypes(document, item, searchFilters);

            handleOurBitstreamFilters(document, item, bitstreamSearchFilters);
        }catch (SQLException e){
            log.error(e.getMessage());
        }
    }

    private void handleOurBitstreamFilters(SolrInputDocument document, Item item, Map<String, List<DiscoverySearchFilter>> bitstreamSearchFilters) throws SQLException {
        //clear any input document fields we are about to add lower
        clearFieldsWeAreAdding(document, bitstreamSearchFilters);
        for (Map.Entry<String, List<DiscoverySearchFilter>> entry : bitstreamSearchFilters.entrySet()){
            String mdField = entry.getKey();
            List<DiscoverySearchFilter> filtersUsingMdField = entry.getValue();
            List<Metadatum> metadataForMdFieldFromAllBitstreams = new LinkedList<>();
            for(Bitstream bitstream : item.getNonInternalBitstreams()){
               metadataForMdFieldFromAllBitstreams.addAll(bitstream.getMetadata(mdField, Item.ANY));
            }
            for (DiscoverySearchFilter filter : filtersUsingMdField) {
                for (Metadatum md : metadataForMdFieldFromAllBitstreams) {
                    List<String> fieldNames = generateFieldNamesFromFilterIndexName(filter.getIndexFieldName());
                    //XXX bitstream filters don't work with dates
                    CollectionUtils.filter(fieldNames , new Predicate() {
                                @Override
                                public boolean evaluate(Object object) {
                                    return object instanceof String && !((String)object).contains(".year");
                                }
                    });
                    for (String fieldName : fieldNames){
                        document.addField(fieldName, md.value);
                    }

                }
            }
        }
    }

    private void handleOurNewFilterTypes(SolrInputDocument document, Item item, Map<String, List<DiscoverySearchFilter>> searchFilters) {
        //clear any input document fields we are about to add lower
        clearFieldsWeAreAdding(document, searchFilters);
        for (Map.Entry<String, List<DiscoverySearchFilter>> entry : searchFilters .entrySet())
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
                        else if(filter.getType().equals(DiscoveryConfigurationParameters.TYPE_TIMELENGTH)){
                            int hours = 0;
                            int minutes = 0;
                            int seconds = 0;
                            String[] parts = value.split(":", 3);
                            if(parts.length == 3){
                                hours = Integer.parseInt(parts[0]);
                                minutes = Integer.parseInt(parts[1]);
                                seconds = Integer.parseInt(parts[2]);
                            }
                            int totalSeconds = seconds + minutes * 60 + hours * 60 * 60;
                            if(totalSeconds <= FIFTEEN_MINUTES){
                                convertedValue = "=< 15 min";
                            }else if(totalSeconds <= FORTYFIVE_MINUTES){
                                convertedValue = "=< 45 min";
                            }else {
                                convertedValue = "> 45 min";
                            }
                            for(String suff : new String[]{"", "_filter", "_keyword", "_ac"}){
                                document.addField(filter.getIndexFieldName() + suff, convertedValue);
                            }
                            convertedValue = null;
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

            for(DiscoverySearchFilter filter : filters){
                if(!filter.getType().equals(DiscoveryConfigurationParameters.TYPE_PRESENT)){
                    continue;
                }
                String convertedValue = mds.length > 0 ? "Yes" : "No";
                for(String suff : new String[]{"", "_filter", "_keyword", "_ac"}){
                    document.addField(filter.getIndexFieldName() + suff, convertedValue);
                }

            }

        }
    }

    private Map<String, Map<String, List<DiscoverySearchFilter>>> getFieldToFiltersMaps(Item item) throws SQLException {
        // metadataField to list of filters
        Map<String, List<DiscoverySearchFilter>> searchFilters = new HashMap<>();
        Map<String, List<DiscoverySearchFilter>> bitstreamSearchFilters = new HashMap<>();
        List<DiscoveryConfiguration> discoveryConfigurations = SearchUtils
                .getAllDiscoveryConfigurations(item);
        // read config
        // partly yanked from SolrServiceImpl
        // go through configurations
        for (DiscoveryConfiguration discoveryConfiguration : discoveryConfigurations)
        {
            // go through filters in configuration
            for (int i = 0; i < discoveryConfiguration
                    .getSearchFilters().size(); i++)
            {
                DiscoverySearchFilter discoverySearchFilter = discoveryConfiguration
                        .getSearchFilters().get(i);
                // go through metadata fields the filter uses
                for (int j = 0; j < discoverySearchFilter
                        .getMetadataFields().size(); j++)
                {
                    String metadataField = discoverySearchFilter
                            .getMetadataFields().get(j);
                    // list of filters for metadataField
                    List<DiscoverySearchFilter> resultingList;
                    String type = discoverySearchFilter.getType();
                    // Process only our new types
                    if (type.equals(DiscoveryConfigurationParameters.TYPE_RAW)
                            || type.equals(DiscoveryConfigurationParameters.TYPE_ISO_LANG)
                            || type.equals(DiscoveryConfigurationParameters.TYPE_TIMELENGTH)
                            || type.equals(DiscoveryConfigurationParameters.TYPE_PRESENT))
                    {
                        if (searchFilters.get(metadataField) != null)
                        {
                            resultingList = searchFilters.get(metadataField);
                        }
                        else
                        {
                            // New metadata field, create a new list for it
                            resultingList = new ArrayList<DiscoverySearchFilter>();
                        }
                        resultingList.add(discoverySearchFilter);

                        searchFilters.put(metadataField, resultingList);
                    }
                    else if (type.equals(DiscoveryConfigurationParameters.TYPE_BITSTREAM))
                    {
                        if(bitstreamSearchFilters.get(metadataField) != null){
                            resultingList = searchFilters.get(metadataField);
                        } else{
                            resultingList = new ArrayList<>();
                        }
                        resultingList.add(discoverySearchFilter);
                        bitstreamSearchFilters.put(metadataField, resultingList);
                    }
                }
            }
        }
        Map<String, Map<String, List<DiscoverySearchFilter>>> ret = new HashMap<>();
        ret.put("search", searchFilters);
        ret.put("bitstream", bitstreamSearchFilters);
        return ret;
    }

    private void createCompFieldsFromLocalMetadata(SolrInputDocument document, Item item) {
        //just add _comp to local*, these have special treatment in schema.xml
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
    }

    private void createHandleTitleAcField(SolrInputDocument document, Item item) {
        //create handle_title_ac field
        String title = item.getName();
        String handle = item.getHandle();
        document.addField("handle_title_ac", handle + ":" + title);
    }

    private void cleanupNarratorInterviewFields(SolrInputDocument document, Item item) {
        String type = item.getMetadata("dc.type");
        List<String> toDelete = new LinkedList<>();
        for (String fieldName: document.getFieldNames()){
            if("narrator".equals(type) && fieldName.startsWith("interview")){
                toDelete.add(fieldName);
            }else if("interview".equals(type) && fieldName.startsWith("narrator")){
                toDelete.add(fieldName);
            }
        }
        for(String fieldName : toDelete){
            document.removeField(fieldName);
        }
    }

    static List<String> generateFieldNamesFromFilterIndexName(String name) {
        List<String> names = new ArrayList<>();
        names.add(name);
        for(String suffix : generatedIndexFieldSuffixes){
            names.add(name + suffix);
        }
        return names;
    }

    private void clearFieldsWeAreAdding(SolrInputDocument document,
                                        Map<String, List<DiscoverySearchFilter>> mdFieldToFilters){
            for (Map.Entry<String, List<DiscoverySearchFilter>> entry : mdFieldToFilters.entrySet()) {
                //String metadataField = entry.getKey();
                List<DiscoverySearchFilter> filters = entry.getValue();
                for (DiscoverySearchFilter filter : filters) {
                    for (String fieldName : generateFieldNamesFromFilterIndexName(filter.getIndexFieldName())) {
                        document.removeField(fieldName);
                    }
                }
            }
    }
}
