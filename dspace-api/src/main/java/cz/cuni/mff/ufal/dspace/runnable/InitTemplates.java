package cz.cuni.mff.ufal.dspace.runnable;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;

import java.sql.SQLException;

public class InitTemplates {
    public static void main(String[] args) throws SQLException, AuthorizeException {
        final Context context = new Context();
        context.turnOffAuthorisationSystem();

        final Collection[] collections = Collection.findAll(context);
        for (Collection collection : collections){
            Item template = collection.getTemplateItem();

            if (template == null)
            {
                collection.createTemplateItem();
                template = collection.getTemplateItem();

                collection.update();
            }

            template.addMetadata("dc", "type", null, null, collection.getName().toLowerCase().replaceFirst("s$", ""));
            template.update();
            context.commit();
        }
        context.complete();
    }
}
