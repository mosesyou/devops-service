package io.choerodon.devops.infra.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import io.choerodon.devops.infra.dto.AppServiceShareRuleDTO;
import io.choerodon.mybatis.common.BaseMapper;

/**
 * Created by ernst on 2018/5/12.
 */
public interface AppServiceShareRuleMapper extends BaseMapper<AppServiceShareRuleDTO> {

    List<AppServiceShareRuleDTO> listByOptions(@Param("appServiceId") Long appServiceId,
                                               @Param("searchParam") Map<String, Object> searchParam,
                                               @Param("params") List<String> params);


    void updatePublishLevel();

    void deleteAll();

    int countShareRulesByAppServiceId(@Param("appServiceId") Long appServiceId);
}
