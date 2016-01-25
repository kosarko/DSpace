/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.health;

import org.apache.commons.io.FileUtils;
import org.dspace.app.util.CollectionDropDown;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

import java.sql.SQLException;
import java.util.*;

/**
 * @author LINDAT/CLARIN dev team
 */
public class Core {

    private static AbstractHibernateDAO dao = new AbstractHibernateDAO<Object>(){};
    private static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

    private final Context context;

    public Core(Context context){
        this.context = context;
    }

    // get info
    //



    // get objects
    //

    public  List<Object[]> getWorkspaceItemsRows() throws SQLException {
        return query("SELECT wi.stageReached, count(*) as cnt from WorkspaceItem wi group by wi.stageReached order by wi.stageReached");
    }

    public  List<UUID> getBitstreamOrphansRows() throws SQLException {
        return query("select bit.id from Bitstream bit where bit.deleted != true" +
                " and bit.id not in (select bit2.id from Bundle bun join bun.bitstreams bit2)" +
                " and bit.id not in (select com.logo.id from Community com)" +
                " and bit.id not in (select col.logo.id from Collection col)" +
                " and bit.id not in (select bun.primaryBitstream.id from Bundle bun)");
    }

    // get sizes
    //

    public  int getBitstreamsWithoutPolicyCount() throws SQLException {
        return count("SELECT count(bit.id) from Bitstream bit where bit.deleted<>true and bit.id not in" +
                " (select res.dSpaceObject from ResourcePolicy res where res.resourceTypeId=" + Constants.BITSTREAM +")");
    }

    public  int getBitstreamsDeletedCount() throws SQLException {
        return count();
    }

    // get more complex information
    //

    public  List<Map.Entry<String, Integer>> getCommunities()
        throws SQLException {

        List<Map.Entry<String, Integer>> cl = new java.util.ArrayList<>();
        List<Community> top_communities = communityService.findAllTop(context);
        for (Community c : top_communities) {
            cl.add(
                new java.util.AbstractMap.SimpleEntry<>(c.getName(), itemService.countItems(context, c))
            );
        }
        return cl;
    }

    //
    //

     int count(String query) throws SQLException {
        return dao.count(dao.createQuery(context, query));
    }

     List query(String query) throws SQLException {
        return dao.createQuery(context, query).list();
    }
}



