package io.choerodon.devops.api.vo;

import java.util.Date;
import java.util.List;

import org.hzero.starter.keyencrypt.core.Encrypt;

import io.choerodon.devops.infra.annotation.WillDeleted;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  16:14 2019/4/8
 * Description:
 */
@WillDeleted
public class PipelineReqVO {
    @Encrypt
    private Long id;
    private String name;
    private String triggerType;
    @Encrypt
    private List<Long> pipelineUserRels;
    private List<PipelineStageVO> pipelineStageVOs;
    private Long projectId;
    private Long objectVersionNumber;
    private Date lastUpdateDate;
    private Boolean edit;

    public Boolean getEdit() {
        return edit;
    }

    public void setEdit(Boolean edit) {
        this.edit = edit;
    }

    public List<Long> getPipelineUserRels() {
        return pipelineUserRels;
    }

    public void setPipelineUserRels(List<Long> pipelineUserRels) {
        this.pipelineUserRels = pipelineUserRels;
    }

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

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public List<PipelineStageVO> getPipelineStageVOs() {
        return pipelineStageVOs;
    }

    public void setPipelineStageVOs(List<PipelineStageVO> pipelineStageVOs) {
        this.pipelineStageVOs = pipelineStageVOs;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getObjectVersionNumber() {
        return objectVersionNumber;
    }

    public void setObjectVersionNumber(Long objectVersionNumber) {
        this.objectVersionNumber = objectVersionNumber;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(Date lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }
}
