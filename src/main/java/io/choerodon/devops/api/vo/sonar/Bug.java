package io.choerodon.devops.api.vo.sonar;

import java.util.List;

/**
 * Created by Sheep on 2019/5/6.
 */
public class Bug {


    private List<Facet> facets;


    public List<Facet> getFacets() {
        return facets;
    }

    public void setFacets(List<Facet> facets) {
        this.facets = facets;
    }
}
