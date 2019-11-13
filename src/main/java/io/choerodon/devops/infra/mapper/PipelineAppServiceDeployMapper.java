package io.choerodon.devops.infra.mapper;

import java.util.List;

import io.choerodon.devops.infra.dto.PipelineAppServiceDeployDTO;
import io.choerodon.mybatis.common.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  10:37 2019/4/4
 * Description:
 */
public interface PipelineAppServiceDeployMapper extends Mapper<PipelineAppServiceDeployDTO> {
    PipelineAppServiceDeployDTO queryById(@Param("appServiceDeployId") Long appServiceDeployId);

    void updateInstanceId(@Param("instanceId") Long instanceId);

    Boolean checkNameExist(@Param("name") String name, @Param("envIds") List<Long> envIds);

}
