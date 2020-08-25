package io.choerodon.devops.api.vo;

import java.util.Date;
import java.util.List;

import org.hzero.starter.keyencrypt.core.Encrypt;

import io.choerodon.devops.app.eventhandler.payload.AppServiceVersionUploadPayload;


/**
 * Created by ernst on 2018/5/12.
 */
public class AppServiceReleasingVO {
    @Encrypt
    private Long id;
    @Encrypt
    private Long appServiceId;
    private String name;
    private String code;
    private String publishLevel;
    private List<AppServiceVersionUploadPayload> appServiceVersions;
    private String imgUrl;
    private String category;
    private String description;
    private String contributor;
    private String readme;
    private Date lastUpdatedDate;
    private Boolean isDeployed;
    private Boolean isFree;
    private List<ProjectReqVO> projectDTOS;
    private Boolean isSite;

    public Boolean getSite() {
        return isSite;
    }

    public void setSite(Boolean site) {
        isSite = site;
    }

    public Boolean getFree() {
        return isFree;
    }

    public void setFree(Boolean free) {
        isFree = free;
    }

    public List<ProjectReqVO> getProjectDTOS() {
        return projectDTOS;
    }

    public void setProjectDTOS(List<ProjectReqVO> projectDTOS) {
        this.projectDTOS = projectDTOS;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAppServiceId() {
        return appServiceId;
    }

    public void setAppServiceId(Long appServiceId) {
        this.appServiceId = appServiceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPublishLevel() {
        return publishLevel;
    }

    public void setPublishLevel(String publishLevel) {
        this.publishLevel = publishLevel;
    }

    public List<AppServiceVersionUploadPayload> getAppServiceVersions() {
        return appServiceVersions;
    }

    public void setAppServiceVersions(List<AppServiceVersionUploadPayload> appServiceVersions) {
        this.appServiceVersions = appServiceVersions;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContributor() {
        return contributor;
    }

    public void setContributor(String contributor) {
        this.contributor = contributor;
    }

    public String getReadme() {
        return readme;
    }

    public void setReadme(String readme) {
        this.readme = readme;
    }

    public Boolean getDeployed() {
        return isDeployed;
    }

    public void setDeployed(Boolean deployed) {
        isDeployed = deployed;
    }

    public Date getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(Date lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }
}
