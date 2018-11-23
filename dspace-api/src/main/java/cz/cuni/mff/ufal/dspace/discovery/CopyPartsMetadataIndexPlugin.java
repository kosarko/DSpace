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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class CopyPartsMetadataIndexPlugin implements SolrServiceIndexPlugin {
    private static Logger log = LoggerFactory.getLogger(CopyPartsMetadataIndexPlugin.class);


    IdentifierService identifierService;

    @Autowired
    @Required
    public void setIdentifierService(IdentifierService identifierService){
        this.identifierService = identifierService;
    }

    @Override
    public void additionalIndex(final Context context, DSpaceObject dso, SolrInputDocument document) {
       if(dso.getType() == Constants.ITEM){
            final Item item = (Item)dso;
            if("narrator".equals(item.getMetadata("dc.type"))){
                /* Don't want to copy solr fields that are normally present, just want to add _filter, _keyword, _ac
                of the associated interviews.
                When more than one interview, document.getFieldNames contains the _filter etc of the first interview
                added. Keep a copy of the fields before we add anything.
                */
                final Collection<String> noCopyFields = new HashSet<>(document.getFieldNames());
                final Metadatum[] interviews = item.getMetadataByMetadataString("dc.relation.haspart");
                for (Metadatum md : interviews){
                    final String interviewURI = md.value;
                    try{
                        final Item interview = (Item)identifierService.resolve(context, interviewURI);
                        final SolrInputDocument interviewDoc = new MySolrServiceImpl(context, interview).getDocument();
                        for(String name : interviewDoc.getFieldNames())
                        {
                            if(!noCopyFields.contains(name) && (name.endsWith("_filter") || name.endsWith("_keyword")
                                    || name.endsWith("_ac"))) {
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

    static class MySolrServiceImpl extends SolrServiceImpl {
        private final Context context;
        private final Item interview;
        SolrInputDocument doc;

        public MySolrServiceImpl(Context context, Item interview) {
            this.context = context;
            this.interview = interview;
        }

        @Override
        protected void writeDocument(SolrInputDocument doc, List<BitstreamContentStream> streams) throws IOException {
            this.doc = doc;
        }

        public SolrInputDocument getDocument() throws IOException, SQLException {
            this.buildDocument(context, interview);
            return doc;
        }
    }
}
