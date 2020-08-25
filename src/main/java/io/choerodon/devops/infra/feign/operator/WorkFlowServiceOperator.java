package io.choerodon.devops.infra.feign.operator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.infra.dto.workflow.DevopsPipelineDTO;
import io.choerodon.devops.infra.feign.WorkFlowServiceClient;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  17:11 2019/7/19
 * Description:
 */
@Component
public class WorkFlowServiceOperator {
    @Autowired
    private WorkFlowServiceClient workFlowServiceClient;

    public String create(Long projectId, DevopsPipelineDTO devopsPipelineDTO) {
        ResponseEntity<String> responseEntity = workFlowServiceClient.create(projectId, devopsPipelineDTO);
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new CommonException("error.workflow.create");
        }
        return responseEntity.getBody();
    }

    public Boolean approveUserTask(Long projectId, String businessKey) {
        ResponseEntity<Boolean> responseEntity = workFlowServiceClient.approveUserTask(projectId, businessKey);
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new CommonException("error.workflow.approve");
        }
        return responseEntity.getBody();
    }

    public void stopInstance(Long projectId, String businessKey) {
        ResponseEntity responseEntity = workFlowServiceClient.stopInstance(projectId, businessKey);
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new CommonException("error.workflow.stop");
        }
    }

    public String createCiCdPipeline(Long projectId, DevopsPipelineDTO devopsPipelineDTO) {
        ResponseEntity<String> responseEntity = workFlowServiceClient.createCiCdPipeline(projectId, devopsPipelineDTO);
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new CommonException("error.workflow.create");
        }
        return responseEntity.getBody();
    }
}
