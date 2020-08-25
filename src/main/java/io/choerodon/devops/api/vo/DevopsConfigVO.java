package io.choerodon.devops.api.vo;

import org.hzero.starter.keyencrypt.core.Encrypt;

/**
 * @author zongw.lee@gmail.com
 * @since 2019/03/11
 */
public class DevopsConfigVO {
    @Encrypt
    private Long id;

    private String name;

    private ConfigVO config;

    private Long projectId;

    private Long organizationId;

    @Encrypt
    private Long appServiceId;

    private Boolean harborPrivate;

    private String type;

    private Boolean custom;

    private Long objectVersionNumber;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConfigVO getConfig() {
        return config;
    }

    public void setConfig(ConfigVO config) {
        this.config = config;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getObjectVersionNumber() {
        return objectVersionNumber;
    }

    public void setObjectVersionNumber(Long objectVersionNumber) {
        this.objectVersionNumber = objectVersionNumber;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public Long getAppServiceId() {
        return appServiceId;
    }

    public void setAppServiceId(Long appServiceId) {
        this.appServiceId = appServiceId;
    }

    public Boolean getCustom() {
        return custom;
    }

    public void setCustom(Boolean custom) {
        this.custom = custom;
    }

    public Boolean getHarborPrivate() {
        return harborPrivate;
    }

    public void setHarborPrivate(Boolean harborPrivate) {
        this.harborPrivate = harborPrivate;
    }
}
