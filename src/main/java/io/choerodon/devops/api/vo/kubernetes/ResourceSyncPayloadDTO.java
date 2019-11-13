package io.choerodon.devops.api.vo.kubernetes;

public class ResourceSyncPayloadDTO {
    private String resourceType;
    private String[] resources;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String[] getResources() {
        return resources;
    }

    public void setResources(String[] resources) {
        this.resources = resources;
    }
}
