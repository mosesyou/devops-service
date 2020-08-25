package io.choerodon.devops.api.controller.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.hzero.core.util.Results;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.choerodon.devops.app.service.DevopsCdPipelineRecordService;
import io.choerodon.devops.app.service.DevopsCdPipelineService;
import io.choerodon.devops.app.service.UserAttrService;
import io.choerodon.devops.infra.util.CustomContextUtil;
import io.choerodon.swagger.annotation.Permission;

/**
 * 〈功能简述〉
 * 〈〉
 *
 * @author wanghao
 * @since 2020/7/3 17:24
 */
@RestController
@RequestMapping("/v1/cd_pipeline")
public class DevopsCdPipelineController {

    @Autowired
    private DevopsCdPipelineService devopsCdPipelineService;
    @Autowired
    private DevopsCdPipelineRecordService devopsCdPipelineRecordService;
    @Autowired
    private UserAttrService userAttrService;

//    /**
//     * 启动cd流水线
//     */
//    @Permission(permissionPublic = true)
//    @ApiOperation(value = "启动cd流水线")
//    @PostMapping("/trigger_cd_pipeline")
//    public ResponseEntity<Void> triggerCdPipeline(@RequestParam(value = "token") String token,
//                                                  @RequestParam(value = "commit") String commit,
//                                                  @RequestParam(value = "ref") String ref,
//                                                  @RequestParam(value = "gitlab_user_id") Long gitlabUserId,
//                                                  @Encrypt
//                                                  @RequestParam(value = "gitlab_pipeline_id") Long gitlabPipelineId) {
//        // 设置用户上下文
//        Long iamUserId = userAttrService.queryUserIdByGitlabUserId(gitlabUserId);
//        CustomContextUtil.setDefaultIfNull(iamUserId);
//
//        devopsCdPipelineService.triggerCdPipeline(token, commit, ref, gitlabPipelineId);
//        return Results.success();
//    }

    /**
     * 主机模式镜像部署接口
     */
    @Permission(permissionWithin = true)
    @ApiOperation(value = "主机模式部署接口")
    @PostMapping(value = "/cd_host_deploy")
    public ResponseEntity<Void> cdHostDeploy(
            @Encrypt
            @RequestParam(value = "pipeline_record_id") Long pipelineRecordId,
            @Encrypt
            @RequestParam(value = "stage_record_id") Long stageRecordId,
            @Encrypt
            @RequestParam(value = "job_record_id") Long jobRecordId) {
        devopsCdPipelineRecordService.cdHostDeploy(pipelineRecordId, stageRecordId, jobRecordId);
        return Results.success();
    }


    /**
     * 触发环境自动部署
     */
    @Permission(permissionWithin = true)
    @ApiOperation(value = "环境部署")
    @PostMapping(value = "/env_auto_deploy")
    public ResponseEntity<Void> envAutoDeploy(
            @Encrypt
            @RequestParam(value = "pipeline_record_id") Long pipelineRecordId,
            @Encrypt
            @RequestParam(value = "stage_record_id") Long stageRecordId,
            @Encrypt
            @RequestParam(value = "job_record_id") Long jobRecordId) {
        devopsCdPipelineService.envAutoDeploy(pipelineRecordId, stageRecordId, jobRecordId);
        return Results.success();
    }

    /**
     * 接收任务状态
     *
     * @param pipelineRecordId 流水线记录Id
     * @param stageRecordId    阶段记录Id
     * @param jobRecordId      任务Id
     */
    @Permission(permissionWithin = true)
    @ApiOperation(value = "接收任务状态")
    @PutMapping("/auto_deploy/status")
    public ResponseEntity<Void> setAppDeployStatus(
            @Encrypt
            @ApiParam(value = "流水线记录Id", required = true)
            @RequestParam(value = "pipeline_record_id") Long pipelineRecordId,
            @Encrypt
            @ApiParam(value = "阶段记录Id", required = true)
            @RequestParam(value = "stage_record_id") Long stageRecordId,
            @Encrypt
            @ApiParam(value = "任务Id", required = true)
            @RequestParam(value = "job_record_id") Long jobRecordId,
            @ApiParam(value = "状态", required = true)
            @RequestParam(value = "status") Boolean status) {
        devopsCdPipelineService.setAppDeployStatus(pipelineRecordId, stageRecordId, jobRecordId, status);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 接收任务状态
     *
     * @param pipelineRecordId 流水线记录Id
     * @param stageRecordId    阶段记录Id
     * @param jobRecordId      任务Id
     */
    @Permission(permissionWithin = true)
    @ApiOperation(value = "查询任务状态")
    @GetMapping("/job/status")
    public ResponseEntity<String> getJobStatus(
            @Encrypt
            @ApiParam(value = "流水线记录Id", required = true)
            @RequestParam(value = "pipeline_record_id") Long pipelineRecordId,
            @Encrypt
            @ApiParam(value = "阶段记录Id", required = true)
            @RequestParam(value = "stage_record_id") Long stageRecordId,
            @Encrypt
            @ApiParam(value = "任务Id", required = true)
            @RequestParam(value = "job_record_id") Long jobRecordId) {
        return Results.success(devopsCdPipelineService.getDeployStatus(pipelineRecordId, stageRecordId, jobRecordId));
    }

}
