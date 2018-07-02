package cz.cuni.mff.ufal.dspace.submit.step;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class FillIsPartOfStep extends AbstractProcessingStep {

    final ConfigurationService configurationService = new DSpace().getConfigurationService();

    /**
     * Do any processing of the information input by the user, and/or perform
     * step processing (if no user interaction required)
     * <p>
     * It is this method's job to save any data to the underlying database, as
     * necessary, and return error messages (if any) which can then be processed
     * by the doPostProcessing() method.
     * <p>
     * NOTE: If this step is a non-interactive step (i.e. requires no UI), then
     * it should perform *all* of its processing in this method!
     *
     * @param context  current DSpace context
     * @param request  current servlet request object
     * @param response current servlet response object
     * @param subInfo  submission info object
     * @return Status or error flag which will be processed by
     * doPostProcessing() below! (if STATUS_COMPLETE or 0 is returned,
     * no errors occurred!)
     */
    @Override
    public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response, SubmissionInfo subInfo) throws ServletException, IOException, SQLException, AuthorizeException {
        final String itemIDParam = request.getParameter("itemID");
        try {
            final int id = Integer.parseInt(itemIDParam);
            final Item containingItem = Item.find(context, id);
            final Item item = subInfo.getSubmissionItem().getItem();
            String handle_prefix = configurationService.getProperty("handle.canonical.prefix");
            String url_of_item = String.format("%s%s", handle_prefix, containingItem.getHandle());
            item.addMetadata("dc", "relation", "ispartof", Item.ANY, url_of_item);
            item.update();
            context.commit();
        }catch (NumberFormatException e){
            ;
        }
        return STATUS_COMPLETE;
    }

    /**
     * Retrieves the number of pages that this "step" extends over. This method
     * is used by the SubmissionController to build the progress bar.
     * <p>
     * This method may just return 1 for most steps (since most steps consist of
     * a single page). But, it should return a number greater than 1 for any
     * "step" which spans across a number of HTML pages. For example, the
     * configurable "Describe" step (configured using input-forms.xml) overrides
     * this method to return the number of pages that are defined by its
     * configuration file.
     * <p>
     * Steps which are non-interactive (i.e. they do not display an interface to
     * the user) should return a value of 1, so that they are only processed
     * once!
     *
     * @param request The HTTP Request
     * @param subInfo The current submission information object
     * @return the number of pages in this step
     */
    @Override
    public int getNumberOfPages(HttpServletRequest request, SubmissionInfo subInfo) throws ServletException {
        return 1;
    }
}
