package io.choerodon.devops.app.service;


import com.github.pagehelper.PageInfo;

import io.choerodon.devops.api.vo.DeployRecordCountVO;
import org.springframework.data.domain.Pageable;
import io.choerodon.devops.api.vo.DevopsDeployRecordVO;
import io.choerodon.devops.infra.dto.DevopsDeployRecordDTO;

import java.util.Date;

/**
 * Created by Sheep on 2019/7/29.
 */
public interface DevopsDeployRecordService {

    PageInfo<DevopsDeployRecordVO> pageByProjectId(Long projectId, String params, Pageable pageable);


    PageInfo<DevopsDeployRecordDTO> basePageByProjectId(Long projectId, String params, Pageable pageable);

    void baseCreate(DevopsDeployRecordDTO devopsDeployRecordDTO);

    void baseDelete(DevopsDeployRecordDTO devopsDeployRecordDTO);

    /**
     * 通过环境id删除只属于该环境的手动部署纪录
     *
     * @param envId 环境id
     */
    void deleteManualRecordByEnv(Long envId);


    /**
     * 删除手动部署生成的实例相关的部署纪录
     *
     * @param instanceId 实例id
     */
    void deleteRelatedRecordOfInstance(Long instanceId);

    /**
     * 按时间段，统计项目每日的部署次数
     *
     * @param projectId
     * @param startTime
     * @param endTime
     * @return
     */
    DeployRecordCountVO countByDate(Long projectId, Date startTime, Date endTime);
}
