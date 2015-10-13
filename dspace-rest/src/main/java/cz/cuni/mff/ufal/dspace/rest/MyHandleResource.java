package cz.cuni.mff.ufal.dspace.rest;

import cz.cuni.mff.ufal.dspace.rest.common.Handle;
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
        } catch (SQLException e) {
            processException("Could not read /services/handles, SQLException. Message: " + e.getMessage(), context);
        }
        return result.toArray(new Handle[0]);
    }
}
