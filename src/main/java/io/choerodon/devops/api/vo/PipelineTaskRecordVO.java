package io.choerodon.devops.api.vo;

import java.util.List;

import org.hzero.starter.keyencrypt.core.Encrypt;

import io.choerodon.devops.infra.annotation.WillDeleted;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  11:34 2019/4/14
 * Description:
 */
@WillDeleted
public class PipelineTaskRecordVO {
    @Encrypt
    private Long id;
    private String name;
    private String status;
    private String taskType;
    private Integer isCountersigned;
    private String appServiceName;
    private String envName;
    private Long appServiceId;
    @Encrypt
    private Long envId;
    private String version;
    private String instanceName;
    @Encrypt
    private Long taskId;
    private String instanceStatus;
    @Encrypt
    private Long instanceId;
    private Boolean envPermission;
    private List<PipelineUserVO> userDTOList;

    public Boolean getEnvPermission() {
        return envPermission;
    }

    public void setEnvPermission(Boolean envPermission) {
        this.envPermission = envPermission;
    }

    public Long getAppServiceId() {
        return appServiceId;
    }

    public void setAppServiceId(Long appServiceId) {
        this.appServiceId = appServiceId;
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

    public List<PipelineUserVO> getUserDTOList() {
        return userDTOList;
    }

    public void setUserDTOList(List<PipelineUserVO> userDTOList) {
        this.userDTOList = userDTOList;
    }

    public String getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(String instanceStatus) {
        this.instanceStatus = instanceStatus;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Integer getIsCountersigned() {
        return isCountersigned;
    }

    public void setIsCountersigned(Integer isCountersigned) {
        this.isCountersigned = isCountersigned;
    }

    public String getAppServiceName() {
        return appServiceName;
    }

    public void setAppServiceName(String appServiceName) {
        this.appServiceName = appServiceName;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
}
