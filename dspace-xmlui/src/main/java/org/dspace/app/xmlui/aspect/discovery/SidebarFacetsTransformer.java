/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.*;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.handle.HandleManager;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders the sidebar filters in Discovery
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class SidebarFacetsTransformer extends AbstractDSpaceTransformer implements CacheableProcessingComponent {


    private static final Logger log = Logger.getLogger(SidebarFacetsTransformer.class);

    /**
     * Cached query results
     */
    protected DiscoverResult narratorResults, interviewResults;

    /**
     * Cached validity object
     */
    protected SourceValidity validity;
    private static final Message T_FILTER_HEAD = message("xmlui.discovery.AbstractFiltersTransformer.filters.head");
    private static final Message T_VIEW_MORE = message("xmlui.discovery.AbstractFiltersTransformer.filters.view-more");

    protected SearchService getSearchService()
    {
        DSpace dspace = new DSpace();
        
        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;

        return manager.getServiceByName(SearchService.class.getName(),SearchService.class);
    }

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        super.addPageMeta(pageMeta);
        pageMeta.addMetadata("include-library", "bootstrap-toggle");
    }

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if (dso != null)
            {
                return HashUtil.hash(dso.getHandle());
            }else{
                return "0";
            }
        }
        catch (SQLException sqle) {
            // Ignore all errors and just return that the component is not
            // cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     * <p/>
     * The validity object will include the collection being viewed and
     * all recently submitted items. This does not include the community / collection
     * hierarchy, when this changes they will not be reflected in the cache.
     */
    public SourceValidity getValidity() {
        if (this.validity == null) {

            try {
                DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
                DSpaceValidity val = new DSpaceValidity();

                // Retrieve any facet results to add to the validity key
                performSearch();

                // Add the actual collection;
                if (dso != null)
                {
                    val.add(dso);
                }

                val.add("narrators numFound:" + narratorResults.getDspaceObjects().size());
                val.add("interviews numFound:" + interviewResults.getDspaceObjects().size());

                for(DiscoverResult queryResults : new DiscoverResult[]{narratorResults, interviewResults}) {
                    for (DSpaceObject resultDso : queryResults.getDspaceObjects()) {
                        val.add(resultDso);
                    }

                    for (String facetField : queryResults.getFacetResults().keySet()) {
                        val.add(facetField);

                        java.util.List<DiscoverResult.FacetResult> facetValues = queryResults.getFacetResults().get(facetField);
                        for (DiscoverResult.FacetResult facetValue : facetValues) {
                            val.add(facetField + facetValue.getAsFilterQuery() + facetValue.getCount());
                        }
                    }
                }

                this.validity = val.complete();
            }
            catch (Exception e) {
                log.error(e.getMessage(),e);
            }
            //TODO: dependent on tags as well :)
        }
        return this.validity;
    }


    public void performSearch() throws SearchServiceException, UIException, SQLException {
        DSpaceObject dso = getScope();
        Request request = ObjectModelHelper.getRequest(objectModel);
        //If we are on a search page performing a search a query may be used
        String query = request.getParameter("query");
        if(query != null && !"".equals(query)){
            // Do standard escaping of some characters in this user-entered query
            query = DiscoveryUIUtils.escapeQueryChars(query);
        }else{
            query = "*:*";
        }


        String[] filterQueries = DiscoveryUIUtils.getFilterQueries(request, context);

        java.util.List<String> narratorFilterQueries = new ArrayList<>();
        java.util.List<String> interviewFilterQueries = new ArrayList<>();

        for(String fq : filterQueries){
            if(fq.contains("narrator")){
                narratorFilterQueries.add(fq);
            }else if(fq.contains("interview")){
                interviewFilterQueries.add(fq);
            }else{
                log.error("Neither interview nor narrator filter " + fq);
            }
        }

        // _query_ is nested query, let's us combine different query types, normal and join
        // join query is sort of inner join; returns the documents having the `to` field, ie. with haspart narrators
        // `{!join from=identifier_keyword to=haspart_keyword}1650` 1650 is a from_query
        // search through docs having identifier_keyword, using from_query, leave only those that are also in
        // some haspart field, return the documents with haspart field.
        // join in fq can't change the "type" of returned document
        String narratorReturningQuery = query + " OR _query_:{!join from=identifier_keyword to=haspart_keyword}" + query;
        String interviewReturningQuery = query + " OR _query_:{!join from=identifier_keyword to=ispartof_keyword}" + query;

        DiscoverQuery narratorQuery = getQueryArgs(context, dso,
                narratorFilterQueries.toArray(new String[narratorFilterQueries.size()]));
        DiscoverQuery interviewQuery = getQueryArgs(context, dso,
                interviewFilterQueries.toArray(new String[interviewFilterQueries.size()]));
        narratorQuery.addFilterQueries("type_keyword:narrator");
        narratorQuery.setQuery(narratorReturningQuery);
        interviewQuery.addFilterQueries("type_keyword:interview");
        interviewQuery.setQuery(interviewReturningQuery);

        if(!interviewFilterQueries.isEmpty()){
            for (ListIterator<String> it = interviewFilterQueries.listIterator(); it.hasNext();){
                //this query returns narrators so the from query searches for interviews
                String fq = "{!join from=identifier_keyword to=haspart_keyword}" + it.next();
                it.set(fq);
            }
            narratorQuery.addFilterQueries(interviewFilterQueries.toArray(new String[interviewFilterQueries.size()]));
        }

        if(!narratorFilterQueries.isEmpty()){
            for (ListIterator<String> it = narratorFilterQueries.listIterator(); it.hasNext(); ) {
                // this join returns interview
                String fq = "{!join from=identifier_keyword to=ispartof_keyword}" + it.next();
                it.set(fq);
            }
            interviewQuery.addFilterQueries(narratorFilterQueries.toArray(new String[narratorFilterQueries.size()]));
        }

        //We do not need to retrieve any dspace objects, only facets
        interviewQuery.setMaxResults(0);
        narratorQuery.setMaxResults(0);
        narratorResults = getSearchService().search(context, dso, narratorQuery);
        interviewResults = getSearchService().search(context, dso, interviewQuery);
    }

    @Override
    public void addOptions(Options options) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        try {
            performSearch();
        } catch (Exception e) {
            log.error("Error while searching for sidebar facets", e);

            return;
        }
        boolean hasResults = narratorResults.getFacetResults().size() > 0 || interviewResults.getFacetResults().size() > 0;
        if(hasResults) {
            //Since we have a value it is safe to add the sidebar (doing it this way will ensure that we do not end up with an empty sidebar)
            List facetListing = options.addList("discovery");
            facetListing.setHead(T_FILTER_HEAD);

            for (DiscoverResult queryResults : new DiscoverResult[]{narratorResults, interviewResults}) {
                processResults(queryResults, facetListing);
            }
        }
    }

    private void processResults(DiscoverResult queryResults, List browse) throws SQLException, WingException,
            UnsupportedEncodingException {

        Request request = ObjectModelHelper.getRequest(objectModel);

        if (queryResults != null) {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            java.util.List<String> fqs = Arrays.asList(DiscoveryUIUtils.getFilterQueries(request, context));

            DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(dso);
            java.util.List<DiscoverySearchFilterFacet> facets = discoveryConfiguration.getSidebarFacets();

            if (facets != null && 0 < facets.size()) {

                for (DiscoverySearchFilterFacet field : facets) {
                    //Retrieve our values
                    java.util.List<DiscoverResult.FacetResult> facetValues = queryResults.getFacetResult(field.getIndexFieldName());
                    //Check if we are dealing with a date, sometimes the facet values arrive as dates !
                    if(facetValues.size() == 0 && field.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)){
                        facetValues = queryResults.getFacetResult(field.getIndexFieldName() + ".year");
                    }

                    int shownFacets = field.getFacetLimit()+1;

                    //This is needed to make sure that the date filters do not remain empty
                    if (facetValues != null && 0 < facetValues.size()) {

                        Iterator<DiscoverResult.FacetResult> iter = facetValues.iterator();

                        List filterValsList = browse.addList(field.getIndexFieldName());

                        filterValsList.setHead(message("xmlui.ArtifactBrowser.AdvancedSearch.type_" + field.getIndexFieldName()));

                        for (int i = 0; i < shownFacets; i++) {

                            if (!iter.hasNext())
                            {
                                //When we have an hierarchical facet always show the "view more" they may want to filter the children of the top nodes
                                if(field.getType().equals(DiscoveryConfigurationParameters.TYPE_HIERARCHICAL)){
                                    addViewMoreUrl(filterValsList, dso, request, field.getIndexFieldName());
                                }
                                break;
                            }

                            DiscoverResult.FacetResult value = iter.next();

                            if (i < shownFacets - 1) {
                                String displayedValue = value.getDisplayedValue();
                                String filterQuery = value.getAsFilterQuery();
                                String filterType = value.getFilterType();
                                
                                // add discovery filter only if it can narrow down the search
                                if(value.getCount() < queryResults.getTotalSearchResults()) {
                                
	                                if (fqs.contains(getSearchService().toFilterQuery(context, field.getIndexFieldName(), value.getFilterType(), value.getAsFilterQuery()).getFilterQuery())) {
	                                    filterValsList.addItem(Math.random() + "", "selected").addContent(displayedValue + " (" + value.getCount() + ")");
	                                } else {
	                                    String paramsQuery = retrieveParameters(request);
	
	                                    filterValsList.addItem().addXref(
	                                            contextPath +
	                                                    (dso == null ? "" : "/handle/" + dso.getHandle()) +
	                                                    "/discover?" +
	                                                    paramsQuery +
	                                                    "filtertype=" + field.getIndexFieldName() +
	                                                    "&filter_relational_operator="+ filterType  +
	                                                    "&filter=" + encodeForURL(filterQuery),
	                                            displayedValue + " (" + value.getCount() + ")"
	                                    );
	                                }
                                
                                }
                            }
                                
                            
                            //Show a "view more" url should there be more values, unless we have a date
                            if (i == shownFacets - 1 && !field.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)/*&& facetField.getGap() == null*/) {
                                addViewMoreUrl(filterValsList, dso, request, field.getIndexFieldName());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the parameters used so it can be used in a url
     * @param request the cocoon request
     * @return the parameters used on this page
     */
    private String retrieveParameters(Request request) throws UnsupportedEncodingException, UIException {
        java.util.List<String> parameters = new ArrayList<String>();
        if(StringUtils.isNotBlank(request.getParameter("query"))){
            parameters.add("query=" + encodeForURL(request.getParameter("query")));
        }

        if(StringUtils.isNotBlank(request.getParameter("scope"))){
            parameters.add("scope=" + request.getParameter("scope"));
        }
        if(StringUtils.isNotBlank(request.getParameter("sort_by"))){
            parameters.add("sort_by=" + request.getParameter("sort_by"));
        }
        if(StringUtils.isNotBlank(request.getParameter("order"))){
            parameters.add("order=" + request.getParameter("order"));
        }
        if(StringUtils.isNotBlank(request.getParameter("rpp"))){
            parameters.add("rpp=" + request.getParameter("rpp"));
        }
        String showNarratorsParam = request.getParameter("showNarrators");
        boolean showNarrators = !"false".equals(showNarratorsParam);
        parameters.add("showNarrators=" + Boolean.toString(showNarrators));


        Map<String, String[]> parameterFilterQueries = DiscoveryUIUtils.getParameterFilterQueries(request);
        for(String parameter : parameterFilterQueries.keySet()){
            for (int i = 0; i < parameterFilterQueries.get(parameter).length; i++) {
                String value = parameterFilterQueries.get(parameter)[i];
                parameters.add(parameter + "=" + encodeForURL(value));
            }

        }
        //Join all our parameters using an "&" sign
        String parametersString = StringUtils.join(parameters.toArray(new String[parameters.size()]), "&");
        if(StringUtils.isNotEmpty(parametersString)){
            parametersString += "&";
        }
        return parametersString;
    }

    private void addViewMoreUrl(List facet, DSpaceObject dso, Request request, String fieldName) throws WingException, UnsupportedEncodingException {
        String parameters = retrieveParameters(request);
        facet.addItem().addXref(
                contextPath +
                        (dso == null ? "" : "/handle/" + dso.getHandle()) +
                        "/search-filter?" + parameters + BrowseFacet.FACET_FIELD + "=" + fieldName,
                T_VIEW_MORE

        );
    }

    public DiscoverQuery getQueryArgs(Context context, DSpaceObject scope, String... filterQueries) {
        DiscoverQuery queryArgs = new DiscoverQuery();

        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(scope);
        java.util.List<DiscoverySearchFilterFacet> facets = discoveryConfiguration.getSidebarFacets();

        log.debug("facets for scope, " + scope + ": " + (facets != null ? facets.size() : null));




        if (facets != null){
            queryArgs.setFacetMinCount(1);
        }

        //Add the default filters
        queryArgs.addFilterQueries(discoveryConfiguration.getDefaultFilterQueries().toArray(new String[discoveryConfiguration.getDefaultFilterQueries().size()]));
        queryArgs.addFilterQueries(filterQueries);

        /** enable faceting of search results */
        if (facets != null){
            for (DiscoverySearchFilterFacet facet : facets) {
                if(facet.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)){
                    String dateFacet = facet.getIndexFieldName() + ".year";
                    try{
                        //Get a range query so we can create facet queries ranging from our first to our last date
                        //Attempt to determine our oldest & newest year by checking for previously selected filters
                        int oldestYear = -1;
                        int newestYear = -1;
                        for (String filterQuery : filterQueries) {
                            if(filterQuery.startsWith(dateFacet + ":")){
                                //Check for a range
                                Pattern pattern = Pattern.compile("\\[(.*? TO .*?)\\]");
                                Matcher matcher = pattern.matcher(filterQuery);
                                boolean hasPattern = matcher.find();
                                if(hasPattern){
                                    filterQuery = matcher.group(0);
                                    //We have a range
                                    //Resolve our range to a first & last year
                                    int tempOldYear = Integer.parseInt(filterQuery.split(" TO ")[0].replace("[", "").trim());
                                    int tempNewYear = Integer.parseInt(filterQuery.split(" TO ")[1].replace("]", "").trim());

                                    //Check if we have a further filter (or a first one found)
                                    if(tempNewYear < newestYear || oldestYear < tempOldYear || newestYear == -1){
                                        oldestYear = tempOldYear;
                                        newestYear = tempNewYear;
                                    }

                                }else{
                                    if(filterQuery.indexOf(" OR ") != -1){
                                        //Should always be the case
                                        filterQuery = filterQuery.split(" OR ")[0];
                                    }
                                    //We should have a single date
                                    oldestYear = Integer.parseInt(filterQuery.split(":")[1].trim());
                                    newestYear = oldestYear;
                                    //No need to look further
                                    break;
                                }
                            }
                        }
                        //Check if we have found a range, if not then retrieve our first & last year using Solr
                        if(oldestYear == -1 && newestYear == -1){

                            DiscoverQuery yearRangeQuery = new DiscoverQuery();
                            yearRangeQuery.setMaxResults(1);
                            //Set our query to anything that has this value
                            yearRangeQuery.addFieldPresentQueries(dateFacet);
                            yearRangeQuery.addFilterQueries(filterQueries);
                            yearRangeQuery.addSearchField(dateFacet);
                            yearRangeQuery.addStatsField(dateFacet);
                            DiscoverResult yearResult = getSearchService().search(context, scope, yearRangeQuery);
                            if(yearResult.getStatsResult(dateFacet) != null){
                                oldestYear = ((Number)yearResult.getStatsResult(dateFacet).getMin()).intValue();
                                newestYear = ((Number)yearResult.getStatsResult(dateFacet).getMax()).intValue();
                            }

                            //No values found!
                            if(newestYear == -1 || oldestYear == -1)
                            {
                                continue;
                            }

                        }

                        int gap = 1;
                        //Attempt to retrieve our gap using the algorithm below
                        int yearDifference = newestYear - oldestYear;
                        if(yearDifference != 0){
                            while (10 < ((double)yearDifference / gap)){
                                gap *= 10;
                            }
                        }
                        // We need to determine our top year so we can start our count from a clean year
                        // Example: 2001 and a gap from 10 we need the following result: 2010 - 2000 ; 2000 - 1990 hence the top year
                        int topYear = (int) (Math.ceil((float) (newestYear)/gap)*gap);

                        if(gap == 1){
                            //We need a list of our years
                            //We have a date range add faceting for our field
                            //The faceting will automatically be limited to the 10 years in our span due to our filterquery
                            queryArgs.addFacetField(new DiscoverFacetField(facet.getIndexFieldName(), facet.getType(), 10, facet.getSortOrder()));
                        }else{
                            java.util.List<String> facetQueries = new ArrayList<String>();
                            //Create facet queries but limit them to 11 (11 == when we need to show a "show more" url)
                            for(int year = topYear; year > oldestYear && (facetQueries.size() < 11); year-=gap){
                                //Add a filter to remove the last year only if we aren't the last year
                                int bottomYear = year - gap;
                                //Make sure we don't go below our last year found
                                if(bottomYear < oldestYear)
                                {
                                    bottomYear = oldestYear;
                                }

                                //Also make sure we don't go above our newest year
                                int currentTop = year;
                                if((year == topYear))
                                {
                                    currentTop = newestYear;
                                }
                                else
                                {
                                    //We need to do -1 on this one to get a better result
                                    currentTop--;
                                }
                                facetQueries.add(dateFacet + ":[" + bottomYear + " TO " + currentTop + "]");
                            }
                            for (String facetQuery : facetQueries) {
                                queryArgs.addFacetQuery(facetQuery);
                            }
                        }
                    }catch (Exception e){
                        log.error(LogManager.getHeader(context, "Error in Discovery while setting up date facet range", "date facet: " + dateFacet), e);
                    }
                }else{
                    int facetLimit = facet.getFacetLimit();
                    //Add one to our facet limit to make sure that if we have more then the shown facets that we show our "show more" url
                    facetLimit++;
                    queryArgs.addFacetField(new DiscoverFacetField(facet.getIndexFieldName(), facet.getType(), facetLimit, facet.getSortOrder()));
                }
            }
        }
        return queryArgs;
    }

    /**
     * Determine the current scope. This may be derived from the current url
     * handle if present or the scope parameter is given. If no scope is
     * specified then null is returned.
     *
     * @return The current scope.
     */
    private DSpaceObject getScope() throws SQLException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String scopeString = request.getParameter("scope");

        // Are we in a community or collection?
        DSpaceObject dso;
        if (scopeString == null || "".equals(scopeString))
        {
            // get the search scope from the url handle
            dso = HandleUtil.obtainHandle(objectModel);
        }
        else
        {
            // Get the search scope from the location parameter
            dso = HandleManager.resolveToObject(context, scopeString);
        }

        return dso;
    }


    @Override
    public void recycle() {
        narratorResults = null;
        interviewResults = null;
        validity = null;
        super.recycle();
    }
}
