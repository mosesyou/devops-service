package io.choerodon.devops.app.service;

import com.github.pagehelper.PageInfo;

import io.choerodon.base.domain.PageRequest;
import io.choerodon.devops.api.vo.DevopsDeployValueVO;
import io.choerodon.devops.infra.dto.DevopsDeployValueDTO;

import java.util.List;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  9:57 2019/4/10
 * Description:
 */
public interface DevopsDeployValueService {

    /**
     * 项目下创建流水线配置
     *
     * @param projectId           项目id
     * @param devopsDeployValueVO 配置信息
     * @return 创建后的配置信息
     */
    DevopsDeployValueVO createOrUpdate(Long projectId, DevopsDeployValueVO devopsDeployValueVO);

    /**
     * 项目下删除配置
     *
     * @param projectId 项目id
     * @param valueId   配置id
     */
    void delete(Long projectId, Long valueId);

    /**
     * 项目下获取部署配置
     *
     * @param projectId    项目id
     * @param appServiceId 应用服务id
     * @param envId        环境id
     * @param pageRequest  分页参数
     * @param params       查询参数
     * @return 分页的部署配置
     */
    PageInfo<DevopsDeployValueVO> pageByOptions(Long projectId, Long appServiceId, Long envId, PageRequest pageRequest, String params);

    /**
     * 项目下查询配置详情
     *
     * @param pipelineId 流水线id
     * @param valueId    配置id
     * @return 配置信息
     */
    DevopsDeployValueVO query(Long pipelineId, Long valueId);

    /**
     * 名称校验
     *
     * @param projectId     项目id
     * @param name          待校验的名称
     * @param deployValueId deployValueId
     */
    void checkName(Long projectId, String name, Long deployValueId);

    /**
     * 根据应用服务Id和环境Id获取配置
     *
     * @param projectId    项目id
     * @param appServiceId 应用服务id
     * @param envId        环境id
     * @return 查看环境下服务的配置
     */
    List<DevopsDeployValueVO> listByEnvAndApp(Long projectId, Long appServiceId, Long envId);

    /**
     * 检测能否删除
     *
     * @param projectId 项目id
     * @param valueId   配置id
     * @return true 如果能删除
     */
    Boolean checkDelete(Long projectId, Long valueId);

    PageInfo<DevopsDeployValueDTO> basePageByOptions(Long projectId, Long appServiceId, Long envId, Long userId, PageRequest pageRequest, String params);

    DevopsDeployValueDTO baseCreateOrUpdate(DevopsDeployValueDTO pipelineRecordE);

    void baseDelete(Long valueId);

    DevopsDeployValueDTO baseQueryById(Long valueId);

    void baseCheckName(Long projectId, String name, Long deployValueId);

    List<DevopsDeployValueDTO> baseQueryByAppIdAndEnvId(Long projectId, Long appServiceId, Long envId);
}
