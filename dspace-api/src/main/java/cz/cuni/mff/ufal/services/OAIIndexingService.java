/* Created for LINDAT/CLARIN */
/* THE LINDAT/CLARIN PROJECT (LM2010013) IS FULLY SUPPORTED BY THE MINISTRY OF EDUCATION, SPORTS 
AND YOUTH OF THE CZECH REPUBLIC UNDER THE PROGRAMME LM OF "LARGE INFRASTRUCTURES".*/
package cz.cuni.mff.ufal.services;

import java.util.Set;

import org.dspace.content.Item;
import org.dspace.core.Context;

public interface OAIIndexingService
{
    public void indexItems(Context context, Set<Item> items) throws Exception;

}
