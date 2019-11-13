package io.choerodon.devops.infra.mapper;

import java.util.List;

import io.choerodon.mybatis.common.Mapper;
import org.apache.ibatis.annotations.Param;

import io.choerodon.devops.infra.dto.DevopsIngressPathDTO;

/**
 * Creator: Runge
 * Date: 2018/4/20
 * Time: 14:30
 * Description:
 */
public interface DevopsIngressPathMapper extends Mapper<DevopsIngressPathDTO> {

    List<DevopsIngressPathDTO> listPathByEnvIdAndServiceName(@Param("envId") Long envId,
                                                             @Param("serviceName") String serviceName);

    List<DevopsIngressPathDTO> listPathByEnvIdAndServiceId(@Param("envId") Long envId,
                                                           @Param("serviceId") Long serviceId);

    boolean checkDomainAndPath(@Param("envId") Long envId, @Param("domain") String domain,
                               @Param("path") String path, @Param("ingressId") Long ingressId);

    void deleteByIngressIds(@Param("ingressIds") List<Long> ingressIds);
}
