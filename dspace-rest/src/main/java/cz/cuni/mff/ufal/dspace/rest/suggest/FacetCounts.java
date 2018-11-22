package cz.cuni.mff.ufal.dspace.rest.suggest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class FacetCounts {

    @XmlElement(name="facet_counts")
    public FacetFields getFacetCounts() {
        return facetCounts;
    }

    public void setFacetCounts(FacetFields facetCounts) {
        this.facetCounts = facetCounts;
    }

    public FacetCounts() {
    }

    private FacetFields facetCounts;


}
