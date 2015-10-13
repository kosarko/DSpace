package cz.cuni.mff.ufal.dspace.rest;

import cz.cuni.mff.ufal.dspace.rest.common.Handle;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.dspace.handle.HandlePlugin;
import org.dspace.rest.Resource;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by okosarko on 13.10.15.
 */
@Path("/services")
public class MyHandleResource extends Resource {
    private static Logger log = Logger.getLogger(MyHandleResource.class);

    @GET
    @Path("/handles")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Handle[] getHandles(){
        org.dspace.core.Context context = null;
        List<Handle> result = new ArrayList<>();
        try {
            context = new org.dspace.core.Context();
            String query = "select * from handle where url like '%" + HandlePlugin.magicBean + "%';";
            String[] params = new String[0];
            TableRowIterator tri = DatabaseManager.query(context, query, params);
            List<TableRow> rows = tri.toList();
            for(TableRow row : rows){
                Handle handle = new Handle(row.getStringColumn("handle"), row.getStringColumn("url"));
                result.add(handle);
            }
            context.complete();
        } catch (SQLException e) {
            processException("Could not read /services/handles, SQLException. Message: " + e.getMessage(), context);
        }
        return result.toArray(new Handle[0]);
    }

    @POST
    @Path("/handles")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Handle shortenHandle(Handle handle){
        String url = handle.url;
        org.dspace.core.Context context = null;
        if(url != null && !url.isEmpty()){
            try {
                context = new org.dspace.core.Context();
                String hdl = createHandle(url, context);
                handle.handle = hdl;
                context.complete();
                return handle;
            }catch (SQLException e){
                processException("Could not create handle, SQLException. Message: " + e.getMessage(), context);
            }
        }
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    //XXX this should be read from config or fixed to correct value
    private static final String prefix = "666";
    private String createHandle(String url, org.dspace.core.Context context) throws SQLException{
        String query = "select handle from handle where url like ? ;";
        TableRowIterator tri = DatabaseManager.query(context, query, url);
        if(tri.hasNext()){
            TableRow row = tri.next();
            //if the url is there create it again
            return row.getStringColumn("handle");
        }
        String handle;
        query = "select * from handle where handle like ? ;";
        while(true){
            String rnd = RandomStringUtils.random(4,true,true);
            handle = prefix + "/" + rnd;
            tri = DatabaseManager.query(context, query, handle);
            if(!tri.hasNext()){
                //no row matches stop generation;
                break;
            }
        }
        query = "insert into handle(handle_id, handle, url) values(nextval('handle_seq'), ?, ?)";
        DatabaseManager.query(context, query, handle, url);
        return handle;
    }
}
