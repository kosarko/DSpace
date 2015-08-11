package cz.cuni.mff.ufal.dspace.b2safe;

import fr.cines.eudat.repopack.b2safe_rp_core.DataObject;
import fr.cines.eudat.repopack.b2safe_rp_core.DataSet;

import java.util.List;
import java.util.Properties;

public class HackedDataSet extends DataSet {
    public HackedDataSet(Properties properties){
        super(properties);
    }

    @Override
    public List<DataObject> listDOFromDirectory(String remoteDirectoryAbsolutePath){
        List<DataObject> dos = super.listDOFromDirectory(remoteDirectoryAbsolutePath);
        for(DataObject dobj : dos){
            String remotePath = dobj.getRemoteDirPath();
            int lastSlash = remotePath.lastIndexOf("/");
            String fileName = remotePath.substring(lastSlash+1);
            dobj.setFileName(fileName);
        }
        return dos;
    }
}