package io.choerodon.devops.infra.dataobject;

import javax.persistence.*;

import io.choerodon.mybatis.entity.BaseDTO;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  19:36 2019/4/3
 * Description:
 */
@Table(name = "devops_pipeline_task_record")
public class PipelineTaskRecordDO extends BaseDTO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long stageRecordId;
    private String taskType;
    private String status;
    private String triggerVersion;
    private Long applicationId;
    private Long envId;
    private Long instanceId;
    private Long versionId;
    private Long projectId;
    private Long appDeployId;
    private Integer isCountersigned;
    private String value;
    private Long taskId;
    private String name;
    private String instanceName;
    private String auditUser;
    private Long valueId;

    @Transient
    private String envName;
    @Transient
    private String appName;
    @Transient
    private String version;
    @Transient
    private String instanceStatus;

    public Long getValueId() {
        return valueId;
    }

    public void setValueId(Long valueId) {
        this.valueId = valueId;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }

    public String getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(String instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStageRecordId() {
        return stageRecordId;
    }

    public void setStageRecordId(Long stageRecordId) {
        this.stageRecordId = stageRecordId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTriggerVersion() {
        return triggerVersion;
    }

    public void setTriggerVersion(String triggerVersion) {
        this.triggerVersion = triggerVersion;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getEnvId() {
        return envId;
    }

    public void setEnvId(Long envId) {
        this.envId = envId;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getAppDeployId() {
        return appDeployId;
    }

    public void setAppDeployId(Long appDeployId) {
        this.appDeployId = appDeployId;
    }

    public Integer getIsCountersigned() {
        return isCountersigned;
    }

    public void setIsCountersigned(Integer isCountersigned) {
        this.isCountersigned = isCountersigned;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
