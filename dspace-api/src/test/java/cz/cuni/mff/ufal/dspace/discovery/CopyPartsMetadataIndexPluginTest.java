package cz.cuni.mff.ufal.dspace.discovery;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.util.MockUtil;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.MockIndexEventConsumer;
import org.dspace.identifier.IdentifierService;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.services.ConfigurationService;
import org.dspace.storage.rdbms.MockDatabaseManager;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import cz.cuni.mff.ufal.dspace.discovery.CopyPartsMetadataIndexPlugin.MySolrServiceImpl;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DSpace.class, CopyPartsMetadataIndexPlugin.class})
public class CopyPartsMetadataIndexPluginTest {
    private ConfigurationService configurationService;
    private IdentifierService identifierService;
    private CopyPartsMetadataIndexPlugin plugin;
    private MySolrServiceImpl mySolrService;

    private Item narrator;
    private Item interview1;

    private static final String id_int1 = "http://hdl.handle.net/123456789/mock-interview-1";



    @BeforeClass
    public static void initOnce(){
        // Applies/initializes our mock database by invoking its constructor
        // (NOTE: This also initializes the DatabaseManager, which in turn
        // calls DatabaseUtils to initialize the entire DB via Flyway)
        new MockDatabaseManager();

        // Initialize mock indexer (which does nothing, since Solr isn't running)
        new MockIndexEventConsumer();

        // Initialize mock Util class
        new MockUtil();
    }

    @Before
    public void initTest() throws Exception{
        configurationService = new DSpaceConfigurationService();
        DSpace mockDSpace =  PowerMockito.spy(new DSpace());
        PowerMockito.doReturn(configurationService).when(mockDSpace, "getConfigurationService");
        PowerMockito.whenNew(DSpace.class).withNoArguments().thenReturn(mockDSpace);

        interview1 = PowerMockito.mock(Item.class);

        identifierService = PowerMockito.mock(IdentifierService.class);
        PowerMockito.when(identifierService, "resolve", null, id_int1).thenReturn(interview1);


        mySolrService = PowerMockito.mock(MySolrServiceImpl.class);
        PowerMockito.whenNew(MySolrServiceImpl.class).withAnyArguments().thenReturn(mySolrService);

        Metadatum[] interviews = new Metadatum[1];

        Metadatum md = new Metadatum();
        md.schema = "dc";
        md.element = "relation";
        md.qualifier = "haspart";
        md.value = id_int1;

        interviews[0] = md;

        narrator = PowerMockito.mock(Item.class);
        PowerMockito.when(narrator, "getMetadata", "dc.type").thenReturn("narrator");
        PowerMockito.when(narrator, "getMetadataByMetadataString", "dc.relation.haspart").thenReturn(interviews);
        PowerMockito.when(narrator, "getType").thenReturn(Constants.ITEM);

        plugin = new CopyPartsMetadataIndexPlugin();
        plugin.setIdentifierService(identifierService);

    }

    @Test
    public void xxx() throws Exception{
        SolrInputDocument interview1Doc = new SolrInputDocument();
        interview1Doc.addField("language_keyword", "Czech");
        interview1Doc.addField("language_keyword", "English");

        SolrInputDocument interview2Doc = new SolrInputDocument();
        interview1Doc.addField("language_keyword", "Dumpu");
        interview1Doc.addField("language_keyword", "Slovak");

        PowerMockito.when(mySolrService, "getDocument").thenReturn(interview1Doc, interview2Doc);

        //XXX add metadata to item
        SolrInputDocument doc = new SolrInputDocument();
        plugin.additionalIndex(null, narrator, doc);
        assertEquals(doc.getFieldNames().size(), interview1Doc.getFieldNames().size());
        assertEquals(4, doc.getFieldValues("language_keyword").size());
        // XXX add fields to doc
    }


    @After
    public void destroy(){
        ;//cleanupContext(context);
    }

    protected void cleanupContext(Context c)
    {
        // If context still valid, abort it
        if(c!=null && c.isValid())
            c.abort();

        // Cleanup Context object by setting it to null
        if(c!=null)
            c = null;
    }
}
