package io.choerodon.devops.infra.dto;

import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.annotation.VersionAudit;
import io.choerodon.mybatis.domain.AuditDomain;

import javax.persistence.*;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  19:29 2019/4/3
 * Description:
 */
@ModifyAudit
@VersionAudit
@Table(name = "devops_pipeline_stage_record")
public class PipelineStageRecordDTO extends AuditDomain {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long pipelineRecordId;
    private String status;
    private String triggerType;
    private Integer isParallel;
    private String executionTime;
    private Long projectId;
    private Long stageId;
    private String stageName;
    private String auditUser;

    @Transient
    private String pipelineName;

    public PipelineStageRecordDTO(Long projectId, Long pipelineRecordId) {
        this.pipelineRecordId = pipelineRecordId;
        this.projectId = projectId;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }

    public PipelineStageRecordDTO() {
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(String executionTime) {
        this.executionTime = executionTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPipelineRecordId() {
        return pipelineRecordId;
    }

    public void setPipelineRecordId(Long pipelineRecordId) {
        this.pipelineRecordId = pipelineRecordId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public Integer getIsParallel() {
        return isParallel;
    }

    public void setIsParallel(Integer isParallel) {
        this.isParallel = isParallel;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public PipelineStageRecordDTO setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
        return this;
    }
}
