package cz.cuni.mff.ufal.dspace.rest.suggest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class Counts {

    @XmlElement(name="xxx")
    public List<String> getCounts() {
        return counts;
    }

    public void setFacetCounts(List<String> counts) {
        this.counts = counts;
    }

    private List<String> counts;

    public Counts(){

    }
}
