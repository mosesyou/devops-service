package io.choerodon.devops.api.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

public class DevopsEnvPodInfoVO {

    @ApiModelProperty("pod名称")
    private String name;
    @ApiModelProperty("pod状态")
    private String status;
    @ApiModelProperty("实例名称")
    private String instanceName;
    @ApiModelProperty("内存消耗")
    private String memoryUsed;
    @ApiModelProperty("cpu消耗")
    private String cpuUsed;
    @ApiModelProperty("ip地址")
    private String podIp;
    private Date creationDate;
    @ApiModelProperty(value = "namespace", hidden = true)
    @JsonIgnore
    private String namespace;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(String memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public String getCpuUsed() {
        return cpuUsed;
    }

    public void setCpuUsed(String cpuUsed) {
        this.cpuUsed = cpuUsed;
    }

    public String getPodIp() {
        return podIp;
    }

    public void setPodIp(String podIp) {
        this.podIp = podIp;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
