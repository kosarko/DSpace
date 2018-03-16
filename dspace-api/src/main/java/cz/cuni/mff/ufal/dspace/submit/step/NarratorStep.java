package cz.cuni.mff.ufal.dspace.submit.step;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.WorkspaceItem;
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
        String project = request.getParameter("submit-project");
        String output = request.getParameter("submit-output");

        clearErrorFields(request);

        Item submission = subInfo.getSubmissionItem().getItem();
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

        if(isNotBlank(project)){
            submission.clearMetadata("viadat", "project", "name", Item.ANY);
            Metadatum metadatum = new Metadatum();
            metadatum.schema = "viadat";
            metadatum.element = "project";
            metadatum.qualifier = "name";
            metadatum.value = project;
            submission.addMetadatum(metadatum);
        }

        if(isNotBlank(output)){
            submission.clearMetadata("viadat", "output", Item.ANY, Item.ANY);
            Metadatum metadatum = new Metadatum();
            metadatum.schema = "viadat";
            metadatum.element = "output";
            metadatum.value = output;
            submission.addMetadatum(metadatum);
        }

        Item narratorTemplate = Item.find(context, resource_id);
        if(narratorTemplate != null){
            if(narratorTemplate.getID() != submission.getID()){
                submission.clearMetadata("viadat", "narrator", Item.ANY, Item.ANY);
                Metadatum[] mds = narratorTemplate.getMetadata("viadat", "narrator", Item.ANY, Item.ANY);
                if(mds != null){
                    for(Metadatum md : mds){
                        submission.addMetadatum(md);
                    }
                }
            }
        }else{
            submission.clearMetadata("viadat", "narrator", Item.ANY, Item.ANY);
            int stepNumber = subInfo.getStepConfig("narrator").getStepNumber();
            ((WorkspaceItem)subInfo.getSubmissionItem()).setStageReached(stepNumber);
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
