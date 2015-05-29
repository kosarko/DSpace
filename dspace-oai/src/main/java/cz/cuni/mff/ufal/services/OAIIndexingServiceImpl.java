/* Created for LINDAT/CLARIN */
/* THE LINDAT/CLARIN PROJECT (LM2010013) IS FULLY SUPPORTED BY THE MINISTRY OF EDUCATION, SPORTS 
AND YOUTH OF THE CZECH REPUBLIC UNDER THE PROGRAMME LM OF "LARGE INFRASTRUCTURES".*/
package cz.cuni.mff.ufal.services;

import java.util.Set;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.xoai.app.BasicConfiguration;
import org.dspace.xoai.app.XOAI;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class OAIIndexingServiceImpl implements OAIIndexingService
{
    @Override
    public void indexItems(Context context, Set<Item> items) throws Exception
    {
        XOAI indexer = new XOAI(context, false, false, false);
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
                new Class[] { BasicConfiguration.class });
        applicationContext.getAutowireCapableBeanFactory()
                .autowireBean(indexer);
        indexer.indexItems(items);
        applicationContext.close();
    }
}
