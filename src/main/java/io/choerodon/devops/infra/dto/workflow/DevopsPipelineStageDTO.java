package io.choerodon.devops.infra.dto.workflow;

import java.util.List;

/**
 * Created by Sheep on 2019/4/2.
 */
public class DevopsPipelineStageDTO {

    private Long stageRecordId;
    private List<DevopsPipelineTaskDTO> tasks;
    private Boolean parallel;
    private List<String> usernames;
    private String nextStageTriggerType;
    private Boolean multiAssign;

    public Long getStageRecordId() {
        return stageRecordId;
    }

    public void setStageRecordId(Long stageRecordId) {
        this.stageRecordId = stageRecordId;
    }

    public List<DevopsPipelineTaskDTO> getTasks() {
        return tasks;
    }

    public void setTasks(List<DevopsPipelineTaskDTO> tasks) {
        this.tasks = tasks;
    }

    public String getNextStageTriggerType() {
        return nextStageTriggerType;
    }

    public void setNextStageTriggerType(String nextStageTriggerType) {
        this.nextStageTriggerType = nextStageTriggerType;
    }

    public Boolean getParallel() {
        return parallel;
    }

    public void setParallel(Boolean parallel) {
        this.parallel = parallel;
    }

    public List<String> getUsernames() {
        return usernames;
    }

    public void setUsernames(List<String> usernames) {
        this.usernames = usernames;
    }

    public Boolean getMultiAssign() {
        return multiAssign;
    }

    public void setMultiAssign(Boolean multiAssign) {
        this.multiAssign = multiAssign;
    }
}
