package cz.cuni.mff.ufal.dspace.rest.suggest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class FacetFields {

    @XmlElement(name="facet_fields")
    public Counts getFacetFields() {
        return facetFields;
    }

    public void setFacetFields(Counts facetFields) {
        this.facetFields = facetFields;
    }

    private Counts facetFields;

    public FacetFields() {
    }
}
