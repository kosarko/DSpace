package cz.cuni.mff.ufal.dspace.rest;

import cz.cuni.mff.ufal.dspace.rest.suggest.Counts;
import cz.cuni.mff.ufal.dspace.rest.suggest.FacetCounts;
import cz.cuni.mff.ufal.dspace.rest.suggest.FacetFields;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.rest.Resource;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.utils.DSpace;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Path("/suggestions")
public class Suggestions extends Resource {

    @GET
    @Path("/{facetField}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSuggestion(@PathParam("facetField") String facetField, @DefaultValue("*:*") @QueryParam("query")
            String query){
        if(facetField.startsWith("bitstream")){
            String[] parts = facetField.split("_", 2);
            if(parts.length == 2){
                String field = parts[1];
                Context context = null;
                try {
                    context = new Context();
                    String sql_query = "select text_value, count(text_value) as cnt from metadatavalue natural join " +
                            "metadataschemaregistry natural join metadatafieldregistry" +
                            " where short_id = ?  and  element = ?";
                    String[] md =field.split("\\.",3);
                    if(md.length == 3){
                        sql_query += " and qualifier = ?";
                    }
                    sql_query +=" and resource_type_id = 0 group by text_value;";
                    TableRowIterator tri = null;
                    try {
                        tri = DatabaseManager.query(context, sql_query, md);
                    } catch (SQLException e) {
                        processException(e.getMessage(), context);
                    }
                    List<TableRow> rows = tri.toList();
                    List<String> namesFollowedByCounts = new ArrayList<>(rows.size()*2);
                    for(TableRow row : rows){
                        final int cnt = row.getIntColumn("cnt");
                        final String val = row.getStringColumn("text_value");
                        namesFollowedByCounts.add(val);
                        namesFollowedByCounts.add(Integer.toString(cnt));
                    }
                    final Counts cnt = new Counts();
                    cnt.setFacetCounts(namesFollowedByCounts);
                    final FacetFields facetFields = new FacetFields();
                    facetFields.setFacetFields(cnt);
                    final FacetCounts facetCounts = new FacetCounts();
                    facetCounts.setFacetCounts(facetFields);
                    return Response.ok(facetCounts).build();
                } catch (SQLException e) {
                    processException(e.getMessage(), context);
                }

            }
            else{
                return Response.status(404, "Field not found.").build();
            }

        }

        DiscoverQuery queryArgs = new DiscoverQuery();
        queryArgs.setQuery(query);
        int facetLimit = -1;
        //Using the api that's there; we don't actually need "real" DiscoverFacetField object; TYPE_STANDARD should not modify the facetField, but TYPE_AC would add "_ac"
        queryArgs.addFacetField(new DiscoverFacetField(facetField, DiscoveryConfigurationParameters.TYPE_STANDARD, facetLimit, DiscoveryConfigurationParameters.SORT.COUNT));
        SearchService searchService = new DSpace().getServiceManager().getServiceByName(SearchService.class.getName(), SearchService.class);
        InputStream JSONStream = null;
        Context context = null;
        try{
            context = new Context(Context.READ_ONLY);
            JSONStream = searchService.searchJSON(context, queryArgs, null);
            context.complete();
        }catch (SearchServiceException | SQLException e){
            processException("Error while retrieving JSON from discovery. " + e.getMessage(), context);
        }finally {
            processFinally(context);
        }
        return Response.ok(JSONStream).build();
    }
}

