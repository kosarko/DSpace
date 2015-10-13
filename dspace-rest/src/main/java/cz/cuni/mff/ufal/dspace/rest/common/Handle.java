package cz.cuni.mff.ufal.dspace.rest.common;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by okosarko on 13.10.15.
 */
@XmlRootElement(name="handle")
public class Handle {

    @XmlElement
    public String handle;
    @XmlElement
    public String url;

    public Handle(){

    }

    public Handle(String handle, String url) {
        this.handle = handle;
        this.url = url;
    }
}
