package io.choerodon.devops.infra.mapper;

import java.util.List;
import java.util.Map;

import io.choerodon.devops.infra.dto.DevopsDeployRecordDTO;
import io.choerodon.mybatis.common.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Created by Sheep on 2019/7/29.
 */

public interface DevopsDeployRecordMapper extends Mapper<DevopsDeployRecordDTO> {

    List<DevopsDeployRecordDTO> listByProjectId(@Param("projectId") Long projectId,
                                                @Param("params") List<String> params,
                                                @Param("searchParam") Map<String, Object> searchParam);

    void deleteRelatedRecordOfInstance(@Param("instanceId") Long instanceId);

    void batchInsertSelective(@Param("records") List<DevopsDeployRecordDTO> records);
}
