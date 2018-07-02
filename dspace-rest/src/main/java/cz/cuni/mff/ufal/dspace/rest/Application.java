package cz.cuni.mff.ufal.dspace.rest;

import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class Application extends ResourceConfig{
    public Application(){
        register(MoxyJsonFeature.class);
        register(MoxyXmlFeature.class);
        packages("org.dspace.rest","cz.cuni.mff.ufal.dspace.rest");
    }
}
