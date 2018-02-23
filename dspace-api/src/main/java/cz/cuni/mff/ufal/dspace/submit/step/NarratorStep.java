package cz.cuni.mff.ufal.dspace.submit.step;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class NarratorStep extends AbstractProcessingStep {

    public static final int STATUS_REQUIRED_MISSING = 2;

    @Override
    public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response, SubmissionInfo subInfo) throws ServletException, IOException, SQLException, AuthorizeException {
        int status = STATUS_COMPLETE;
        int resource_id = Util.getIntParameter(request, "submit-narrator-select");
        String title = request.getParameter("submit-title");

        clearErrorFields(request);

        Item submission = subInfo.getSubmissionItem().getItem();
        submission.clearMetadata("viadat", "narrator", Item.ANY, Item.ANY);
        if(isNotBlank(title)){
            submission.clearMetadata("dc", "title", Item.ANY, Item.ANY);
            Metadatum metadatum = new Metadatum();
            metadatum.schema = "dc";
            metadatum.element = "title";
            metadatum.value = title;
            submission.addMetadatum(metadatum);
        }else{
           status = STATUS_REQUIRED_MISSING;
           addErrorField(request, "submit-title");
        }

        Item narratorTemplate = Item.find(context, resource_id);
        if(narratorTemplate != null){
            Metadatum[] mds = narratorTemplate.getMetadata("viadat", "narrator", Item.ANY, Item.ANY);
            if(mds != null){
                for(Metadatum md : mds){
                    submission.addMetadatum(md);
                }
            }
        }
        submission.update();
        context.commit();

        return status;
    }

    @Override
    public int getNumberOfPages(HttpServletRequest request, SubmissionInfo subInfo) throws ServletException {
        return 1;
    }
}
