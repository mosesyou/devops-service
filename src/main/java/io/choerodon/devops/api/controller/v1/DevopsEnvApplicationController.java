package io.choerodon.devops.api.controller.v1;

import io.choerodon.base.annotation.Permission;
import io.choerodon.base.enums.ResourceType;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.InitRoleCode;
import io.choerodon.devops.api.dto.ApplicationRepDTO;
import io.choerodon.devops.api.dto.DevopsEnvApplicationDTO;
import io.choerodon.devops.app.service.DevopsEnvApplicationService;
import io.choerodon.devops.domain.application.entity.DevopsEnvApplicationE;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * @author lizongwei
 * @date 2019/7/1
 */
@RestController
@RequestMapping(value = "/v1/projects/{project_id}/apps")
public class DevopsEnvApplicationController {

    @Autowired
    DevopsEnvApplicationService devopsEnvApplicationService;

    /**
     * 创建环境下的应用关联
     *
     * @param devopsEnvApplicationDTO 应用信息
     * @return ApplicationRepDTO
     */
    @Permission(type= ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER,InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "创建环境下的应用关联")
    @PostMapping
    public ResponseEntity<DevopsEnvApplicationDTO> create(
            @ApiParam(value = "关联信息", required = true)
            @RequestBody DevopsEnvApplicationDTO devopsEnvApplicationDTO) {
        return Optional.ofNullable(devopsEnvApplicationService.create(devopsEnvApplicationDTO))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.env.app.create"));
    }

    /**
     * 查询环境下的所有应用
     *
     * @param envId 环境id
     * @return List
     */
    @Permission(type= ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER,InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "查询环境下的所有应用")
    @PostMapping
    public ResponseEntity<List<ApplicationRepDTO>> queryAppByEnvId(
            @ApiParam(value = "环境id", required = true)
            @RequestParam(value = "env_id") Long envId) {
        return Optional.ofNullable(devopsEnvApplicationService.queryAppByEnvId(envId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.env.app.query"));
    }

}
