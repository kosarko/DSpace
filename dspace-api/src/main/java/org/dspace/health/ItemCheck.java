/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.health;


import com.google.api.services.analytics.model.GaData;
import com.sun.javafx.binding.StringFormatter;
import org.apache.commons.io.FileUtils;
import org.dspace.app.util.CollectionDropDown;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.workflowbasic.factory.BasicWorkflowServiceFactory;
import org.dspace.workflowbasic.service.BasicWorkflowItemService;

import java.sql.SQLException;
import java.util.Map;

/**
 * @author LINDAT/CLARIN dev team
 */
public class ItemCheck extends Check {

    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private BundleService bundleService =  ContentServiceFactory.getInstance().getBundleService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    private BasicWorkflowItemService basicWorkflowItemService = BasicWorkflowServiceFactory.getInstance().getBasicWorkflowItemService();
    private HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();



    @Override
    public String run( ReportInfo ri ) {
        String ret = "";
        int tot_cnt = 0;
        Context context = new Context();
        Core core = new Core(context);
        try {
            for (Map.Entry<String, Integer> name_count : core.getCommunities()) {
                ret += String.format("Community [%s]: %d\n",
                    name_count.getKey(), name_count.getValue());
                tot_cnt += name_count.getValue();
            }
        } catch (SQLException e) {
            error(e);
        }

        try {
            ret += "\nCollection sizes:\n";
            ret += core.getCollectionSizesInfo();
        } catch (SQLException e) {
            error(e);
        }

        ret += String.format(
            "\nPublished items (archived, not withdrawn): %d\n", tot_cnt);
        try {
            ret += String.format(
                "Withdrawn items: %d\n", itemService.countWithdrawnItems(context));
            ret += String.format(
                "Not published items (in workspace or workflow mode): %d\n",
                itemService.getNotArchivedItemsCount(context));

            for (Object[] row : core.getWorkspaceItemsRows()) {
                ret += String.format("\tIn Stage %s: %s\n",
                    row[0],// "stage_reached"),
                    row[1]// "cnt")
                );
            }

            ret += String.format(
                "\tWaiting for approval (workflow items): %d\n",
                basicWorkflowItemService.countTotal(context));

        } catch (SQLException e) {
            error(e);
        }

        try {
            ret += getObjectSizesInfo(context);
            context.complete();
        } catch (SQLException e) {
            error(e);
        }
        return ret;
    }


    public  String getObjectSizesInfo(Context context) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Count %-14s: %s\n", "Bitstream",
                    String.valueOf(bitstreamService.countTotal(context))));
        sb.append(String.format("Count %-14s: %s\n", "Bundle",
                String.valueOf(bundleService.countTotal(context))));
        sb.append(String.format("Count %-14s: %s\n", "Collection",
                String.valueOf(collectionService.countTotal(context))));
        sb.append(String.format("Count %-14s: %s\n", "Community",
                String.valueOf(communityService.countTotal(context))));
        sb.append(String.format("Count %-14s: %s\n", "MetadataValue",
                String.valueOf(metadataValueService.countTotal(context))));
        sb.append(String.format("Count %-14s: %s\n", "EPerson",
                String.valueOf(ePersonService.countTotal(context))));
        sb.append(String.format("Count %-14s: %s\n", "Item",
                String.valueOf(itemService.countTotal(context))));
        sb.append(String.format("Count %-14s: %s\n", "Handle",
                String.valueOf(handleService.countTotal(context))));
        sb.append(String.format("Count %-14s: %s\n", "Group",
                String.valueOf(groupService.countTotal(context))));
        sb.append(String.format("Count %-14s: %s\n", "BasicWorkflowItem",
                String.valueOf(basicWorkflowItemService.countTotal(context))));
        sb.append(String.format("Count %-14s: %s\n", "WorkspaceItem",
                String.valueOf(workspaceItemService.countTotal(context))));
        return sb.toString();
    }

    public  String getCollectionSizesInfo(Context context) throws SQLException {
        final StringBuffer ret = new StringBuffer();
        List<Object[]> col_bitSizes = query("select col, sum(bit.sizeBytes) as sum from Item i join i.collections col join i.bundles bun join bun.bitstreams bit group by col");
        long total_size = 0;

        Collections.sort(col_bitSizes, new Comparator<Object[]>() {
            @Override
            public int compare(Object[] o1, Object[] o2) {
                try {
                    return CollectionDropDown.collectionPath((Collection) o1[0]).compareTo(
                            CollectionDropDown.collectionPath((Collection) o2[0])
                    );
                } catch (Exception e) {
                    ret.append(e.getMessage());
                }
                return 0;
            }
        });
        for (Object[] row : col_bitSizes) {
            Long size = (Long) row[1];
            total_size += size;
            Collection col = (Collection) row[0];
            ret.append(String.format(
                    "\t%s:  %s\n", CollectionDropDown.collectionPath(col), FileUtils.byteCountToDisplaySize((long) size)));
        }
        ret.append(String.format(
                "Total size:              %s\n", FileUtils.byteCountToDisplaySize(total_size)));

        ret.append(String.format(
                "Resource without policy: %d\n", bitstreamService.countBitstreamsWithoutPolicy(context)));

        ret.append(String.format(
                "Deleted bitstreams:      %d\n", bitstreamService.countDeletedBitstreams(context)));

        String list_str = "";
        List<UUID> bitstreamOrphans = getBitstreamOrphansRows();
        for (UUID id : bitstreamOrphans) {
            list_str += String.format("%d, ", id);
        }
        ret.append(String.format(
                "Orphan bitstreams:       %d [%s]\n", bitstreamOrphans.size(), list_str));

        return ret.toString();
    }

}
