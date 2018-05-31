package cz.cuni.mff.ufal.dspace.discovery;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.BitstreamContentStream;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class CopyPartsMetadataIndexPlugin implements SolrServiceIndexPlugin {
    private static Logger log = LoggerFactory.getLogger(CopyPartsMetadataIndexPlugin.class);

    final IdentifierService identifierService = new DSpace().getSingletonService
            (IdentifierService.class);


    @Override
    public void additionalIndex(final Context context, DSpaceObject dso, SolrInputDocument document) {
       if(dso.getType() == Constants.ITEM){
            final Item item = (Item)dso;
            if("narrator".equals(item.getMetadata("dc.type"))){
                final Metadatum[] interviews = item.getMetadataByMetadataString("dc.relation.haspart");
                for (Metadatum md : interviews){
                    final String interviewURI = md.value;
                    try{
                        final Item interview = (Item)identifierService.resolve(context, interviewURI);
                        final SolrInputDocument interviewDoc = new SolrServiceImpl(){
                            SolrInputDocument doc;

                            @Override
                            protected void writeDocument(SolrInputDocument doc, List<BitstreamContentStream> streams) throws IOException {
                                this.doc = doc;
                            }

                            public SolrInputDocument getDocument() throws IOException, SQLException {
                                this.buildDocument(context, interview);
                                return doc;
                            }
                        }.getDocument();
                        for(String name : interviewDoc.getFieldNames())
                        {
                            if(name.endsWith("_filter")) {
                                for (Object val : interviewDoc.getFieldValues(name)) {
                                    document.addField(name, val);
                                }
                            }
                        }

                    }catch (IdentifierNotResolvableException | IdentifierNotFoundException e){
                        log.error("Failed to resolve " + interviewURI + "\n" + e.getMessage());
                    }catch (IOException | SQLException e){
                        log.error("Failed to get solr document for " + interviewURI + "\n" + e.getMessage());
                    }
                }
            }

       }
    }
}
