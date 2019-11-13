package io.choerodon.devops.app.service;

import java.util.List;

import com.github.pagehelper.PageInfo;
import io.choerodon.base.domain.PageRequest;
import io.choerodon.devops.api.vo.*;
import io.choerodon.devops.infra.dto.DevopsClusterDTO;
import io.choerodon.devops.infra.dto.DevopsEnvPodDTO;

public interface DevopsClusterService {

    /**
     * 创建集群
     *
     * @param projectId          项目id
     * @param devopsClusterReqVO 集群信息
     * @return
     */
    String createCluster(Long projectId, DevopsClusterReqVO devopsClusterReqVO);

    /**
     * 更新集群
     *
     * @param clusterId             集群id
     * @param devopsClusterUpdateVO 集群信息
     */
    void updateCluster(Long clusterId, DevopsClusterUpdateVO devopsClusterUpdateVO);

    /**
     * 校验集群名唯一性
     *
     * @param projectId 项目id
     * @param name      集群名称
     */
    void checkName(Long projectId, String name);


    String queryShell(Long clusterId);

    /**
     * 校验集群编码唯一性
     *
     * @param projectId 项目id
     * @param code      集群code
     */
    void checkCode(Long projectId, String code);


    /**
     * 集群列表查询
     *
     * @param projectId   项目id
     * @param doPage      是否分页
     * @param pageRequest 分页参数
     * @param params      查询参数
     * @return 集群列表
     */
    PageInfo<ClusterWithNodesVO> pageClusters(Long projectId, Boolean doPage, PageRequest pageRequest, String params);


    /**
     * 列出组织下所有项目中在数据库中没有权限关联关系的项目(不论当前数据库中是否跳过权限检查)
     *
     * @param projectId 项目ID
     * @param clusterId 集群ID
     * @param params    搜索参数
     * @return 组织下所有项目中在数据库中没有权限关联关系的项目
     */
    List<ProjectReqVO> listNonRelatedProjects(Long projectId, Long clusterId, String params);

    /**
     * 分配权限
     *
     * @param devopsClusterPermissionUpdateVO 集群权限信息
     */
    void assignPermission(DevopsClusterPermissionUpdateVO devopsClusterPermissionUpdateVO);

    /**
     * 删除该项目对该集群的权限
     *
     * @param clusterId 集群id
     * @param relatedProjectId 项目id
     */
    void deletePermissionOfProject(Long clusterId, Long relatedProjectId);

    /**
     * 查询项目下的集群以及所有节点信息
     *
     * @param projectId 项目id
     * @return 集群列表
     */
    List<DevopsClusterBasicInfoVO> queryClustersAndNodes(Long projectId);

    /**
     * 分页查询组织下在数据库中已有关联关系项目列表
     *
     * @param projectId   项目id
     * @param clusterId   集群id
     * @param pageRequest 分页参数
     * @param params      查询参数
     * @return List
     */
    PageInfo<ProjectReqVO> pageRelatedProjects(Long projectId, Long clusterId, PageRequest pageRequest, String params);

    /**
     * 删除集群
     *
     * @param clusterId 集群id
     */
    void deleteCluster(Long clusterId);

    /**
     * 查询单个集群信息
     *
     * @param clusterId 集群id
     */
    DevopsClusterRepVO query(Long clusterId);

    /**
     * 查询集群下是否关联已连接环境
     *
     * @param clusterId 集群id
     * @return
     */
    Boolean checkConnectEnvs(Long clusterId);

    /**
     * 分页查询节点下的Pod
     *
     * @param clusterId   集群id
     * @param nodeName    节点名称
     * @param pageRequest 分页参数
     * @param searchParam 查询参数
     * @return pods
     */
    PageInfo<DevopsEnvPodVO> pagePodsByNodeName(Long clusterId, String nodeName, PageRequest pageRequest, String searchParam);


    /**
     * 根据组织Id和集群编码查询集群信息
     *
     * @param projectId project id
     * @param code      the cluster code
     * @return the node information
     */
    DevopsClusterRepVO queryByCode(Long projectId, String code);


    DevopsClusterDTO baseCreateCluster(DevopsClusterDTO devopsClusterDTO);

    void baseCheckName(DevopsClusterDTO devopsClusterDTO);

    void baseCheckCode(DevopsClusterDTO devopsClusterDTO);

    List<DevopsClusterDTO> baseListByProjectId(Long projectId, Long organizationId);

    DevopsClusterDTO baseQuery(Long clusterId);

    void baseUpdate(DevopsClusterDTO devopsClusterDTO);

    PageInfo<DevopsClusterDTO> basePageClustersByOptions(Long organizationId, Boolean doPage, PageRequest pageRequest, String params);

    void baseDelete(Long clusterId);

    DevopsClusterDTO baseQueryByToken(String token);

    List<DevopsClusterDTO> baseList();

    /**
     * 分页查询节点下的Pod
     *
     * @param clusterId   集群id
     * @param nodeName    节点名称
     * @param pageRequest 分页参数
     * @param searchParam 查询参数
     * @return pods
     */
    PageInfo<DevopsEnvPodDTO> basePageQueryPodsByNodeName(Long clusterId, String nodeName, PageRequest pageRequest, String searchParam);

    DevopsClusterDTO baseQueryByCode(Long organizationId, String code);

    void baseUpdateProjectId(Long orgId, Long proId);
}
