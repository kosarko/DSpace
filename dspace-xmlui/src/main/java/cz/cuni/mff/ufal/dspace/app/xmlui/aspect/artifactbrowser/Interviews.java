package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.artifactbrowser;

import cz.cuni.mff.ufal.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.ObjectManager;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

public class Interviews extends AbstractDSpaceTransformer {
    private static final org.apache.log4j.Logger log = Logger.getLogger(Interviews.class);
    private static final Message T_HEAD = message("xmlui.item_view.Interviews.head");
    final IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);

    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException{
        final DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if(dso instanceof Item) {
            final Metadatum[] metadata = dso.getMetadataByMetadataString("dc.relation.haspart");
            ReferenceSet set = null;
            if(metadata.length > 0){
                Division div = body.addDivision("item-interviews-container").addDivision("item-interviews",
                        "secondary interviews");
                div.setHead(T_HEAD);
                set = div.addReferenceSet("item-interviews-items", ReferenceSet.TYPE_SUMMARY_LIST, null,
                        "interviews-items");
            }
            for(Metadatum md : metadata){
                try {
                    DSpaceObject relatedDso = identifierService.resolve(context, md.value);
                    set.addReference(relatedDso);
                }catch (IdentifierException e){
                    log.error(e);
                }
            }
            final Metadatum[] ispartof = dso.getMetadataByMetadataString("dc.relation.ispartof");
            if(ispartof.length > 0){
                final ObjectManager objectManager = getObjectManager();
                try {
                    final DSpaceObject parent = identifierService.resolve(context, ispartof[0].value);
                    body.addDivision("item-interviews-back").addPara().addXref(contextPath + "/handle/" + parent.getHandle(),
                            "<<< " + parent.getName());
                } catch (IdentifierNotFoundException | IdentifierNotResolvableException e) {
                    log.error(e);
                }
            }
        }else{
            return;
        }
    }

    @Override
    public void recycle(){
        super.recycle();
    }
}
