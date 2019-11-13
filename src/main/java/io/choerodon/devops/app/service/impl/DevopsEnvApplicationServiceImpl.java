package io.choerodon.devops.app.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.kubernetes.client.JSON;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1ContainerPort;
import io.kubernetes.client.models.V1beta2Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.vo.*;
import io.choerodon.devops.api.vo.iam.DevopsEnvMessageVO;
import io.choerodon.devops.app.service.AppServiceService;
import io.choerodon.devops.app.service.DevopsEnvApplicationService;
import io.choerodon.devops.infra.dto.DevopsEnvAppServiceDTO;
import io.choerodon.devops.infra.mapper.DevopsEnvAppServiceMapper;
import io.choerodon.devops.infra.util.ConvertUtils;

/**
 * @author lizongwei
 * @date 2019/7/1
 */
@Service
public class DevopsEnvApplicationServiceImpl implements DevopsEnvApplicationService {

    private JSON json = new JSON();

    @Autowired
    private AppServiceService applicationService;
    @Autowired
    private DevopsEnvAppServiceMapper devopsEnvAppServiceMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<DevopsEnvApplicationVO> batchCreate(DevopsEnvAppServiceVO devopsEnvAppServiceVO) {
        return Stream.of(devopsEnvAppServiceVO.getAppServiceIds())
                .map(appServiceId -> new DevopsEnvAppServiceDTO(appServiceId, devopsEnvAppServiceVO.getEnvId()))
                .peek(e -> devopsEnvAppServiceMapper.insertIgnore(e))
                .map(e -> ConvertUtils.convertObject(e, DevopsEnvApplicationVO.class))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long envId, Long appServiceId) {
        DevopsEnvAppServiceDTO searchCondition = new DevopsEnvAppServiceDTO(appServiceId, envId);
        DevopsEnvAppServiceDTO recordInDb = devopsEnvAppServiceMapper.selectOne(searchCondition);
        if (recordInDb == null) {
            return;
        }

        if (!checkCanDelete(envId, appServiceId)) {
            throw new CommonException("error.delete.env.app-service.relationship");
        }

        devopsEnvAppServiceMapper.delete(searchCondition);
    }

    /**
     * 删除环境和应用服务之间的关联关系时，应用服务在该环境下不能有实例和相关资源
     *
     * @param envId        环境id
     * @param appServiceId 应用服务id
     * @return true 可以删除
     */
    private boolean checkCanDelete(Long envId, Long appServiceId) {
        return devopsEnvAppServiceMapper.countInstances(appServiceId, envId, null) == 0 &&
                devopsEnvAppServiceMapper.countRelatedConfigMap(appServiceId, envId, null) == 0 &&
                devopsEnvAppServiceMapper.countRelatedSecret(appServiceId, envId, null) == 0 &&
                devopsEnvAppServiceMapper.countRelatedService(appServiceId, envId, null) == 0;
    }

    @Override
    public List<AppServiceRepVO> listAppByEnvId(Long envId) {
        List<Long> appServiceIds = baseListAppByEnvId(envId);
        List<AppServiceRepVO> applicationRepVOS = new ArrayList<>();
        appServiceIds.forEach(v ->
                applicationRepVOS.add(ConvertUtils.convertObject(applicationService.baseQuery(v), AppServiceRepVO.class))
        );
        return applicationRepVOS;
    }

    @Override
    public List<Map<String, String>> listLabelByAppAndEnvId(Long envId, Long appServiceId) {
        List<DevopsEnvMessageVO> devopsEnvMessageVOS = baseListResourceByEnvAndApp(envId, appServiceId);
        List<Map<String, String>> listLabel = new ArrayList<>();
        devopsEnvMessageVOS.forEach(devopsEnvMessageVO -> {
            V1beta2Deployment v1beta2Deployment = json.deserialize(
                    devopsEnvMessageVO.getDetail(), V1beta2Deployment.class);
            listLabel.add(v1beta2Deployment.getMetadata().getLabels());
        });
        return listLabel;
    }

    @Override
    public List<DevopsEnvPortVO> listPortByAppAndEnvId(Long envId, Long appServiceId) {
        List<DevopsEnvMessageVO> devopsEnvMessageVOS = baseListResourceByEnvAndApp(envId, appServiceId);
        List<DevopsEnvPortVO> devopsEnvPortVOS = new ArrayList<>();
        devopsEnvMessageVOS.forEach(devopsEnvMessageVO -> {
            V1beta2Deployment v1beta2Deployment = json.deserialize(
                    devopsEnvMessageVO.getDetail(), V1beta2Deployment.class);
            List<V1Container> containers = v1beta2Deployment.getSpec().getTemplate().getSpec().getContainers();
            for (V1Container container : containers) {
                List<V1ContainerPort> ports = container.getPorts();

                Optional.ofNullable(ports).ifPresent(portList -> {
                    for (V1ContainerPort port : portList) {
                        DevopsEnvPortVO devopsEnvPortVO = new DevopsEnvPortVO();
                        devopsEnvPortVO.setResourceName(devopsEnvMessageVO.getResourceName());
                        devopsEnvPortVO.setPortName(port.getName());
                        devopsEnvPortVO.setPortValue(port.getContainerPort());
                        devopsEnvPortVOS.add(devopsEnvPortVO);
                    }
                });
            }
        });
        return devopsEnvPortVOS;
    }

    @Override
    public DevopsEnvAppServiceDTO baseCreate(DevopsEnvAppServiceDTO devopsEnvAppServiceDTO) {
        if (devopsEnvAppServiceMapper.insert(devopsEnvAppServiceDTO) != 1) {
            throw new CommonException("error.insert.env.app");
        }
        return devopsEnvAppServiceDTO;
    }

    @Override
    public List<Long> baseListAppByEnvId(Long envId) {
        return devopsEnvAppServiceMapper.queryAppByEnvId(envId);
    }

    @Override
    public List<DevopsEnvMessageVO> baseListResourceByEnvAndApp(Long envId, Long appServiceId) {
        return devopsEnvAppServiceMapper.listResourceByEnvAndApp(envId, appServiceId);
    }

    @Override
    public List<BaseApplicationServiceVO> listNonRelatedAppService(Long projectId, Long envId) {
        return devopsEnvAppServiceMapper.listNonRelatedApplications(projectId, envId);
    }
}
