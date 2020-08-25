package io.choerodon.devops.app.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.vo.*;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.infra.constant.MiscConstants;
import io.choerodon.devops.infra.dto.*;
import io.choerodon.devops.infra.dto.iam.IamUserDTO;
import io.choerodon.devops.infra.dto.iam.ProjectDTO;
import io.choerodon.devops.infra.enums.PolarisScopeType;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;
import io.choerodon.devops.infra.handler.ClusterConnectionHandler;
import io.choerodon.devops.infra.mapper.DevopsClusterMapper;
import io.choerodon.devops.infra.mapper.DevopsPvProPermissionMapper;
import io.choerodon.devops.infra.util.*;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

@Service
public class DevopsClusterServiceImpl implements DevopsClusterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DevopsClusterServiceImpl.class);
    private static final String CLUSTER_ACTIVATE_COMMAND_TEMPLATE;

    /**
     * 存储集群基本信息的key: cluster-{clusterId}-info
     * 存储的结构为 {@link ClusterSummaryInfoVO}
     */
    private static final String CLUSTER_INFO_KEY_TEMPLATE = "cluster-%s-info";

    private static final String ERROR_CLUSTER_NOT_EXIST = "error.cluster.not.exist";
    private static final String PROJECT_OWNER = "role/project/default/project-owner";
    private static final String ERROR_ORGANIZATION_CLUSTER_NUM_MAX = "error.organization.cluster.num.max";
    @Value("${agent.version}")
    private String agentExpectVersion;
    @Value("${agent.serviceUrl}")
    private String agentServiceUrl;
    @Value("${agent.repoUrl}")
    private String agentRepoUrl;
    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;
    @Autowired
    private ClusterConnectionHandler clusterConnectionHandler;
    @Autowired
    private ClusterNodeInfoService clusterNodeInfoService;
    @Autowired
    private DevopsEnvPodService devopsEnvPodService;
    @Autowired
    private DevopsClusterMapper devopsClusterMapper;
    @Autowired
    private DevopsClusterProPermissionService devopsClusterProPermissionService;
    @Autowired
    private DevopsEnvironmentService devopsEnvironmentService;
    @Autowired
    private DevopsPvService devopsPvService;
    @Autowired
    private DevopsPvProPermissionMapper devopsPvProPermissionMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private PolarisScanningService polarisScanningService;
    @Autowired
    private PermissionHelper permissionHelper;
    @Autowired
    @Lazy
    private SendNotificationService sendNotificationService;

    static {
        InputStream inputStream = DevopsClusterServiceImpl.class.getResourceAsStream("/shell/cluster.sh");
        try {
            CLUSTER_ACTIVATE_COMMAND_TEMPLATE = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CommonException("error.load.cluster.sh");
        }
    }


    @Override
    public void saveClusterSummaryInfo(Long clusterId, ClusterSummaryInfoVO clusterSummaryInfoVO) {
        if (clusterSummaryInfoVO == null || clusterSummaryInfoVO.getVersion() == null) {
            LOGGER.warn("Abandon Bad cluster info: {}", clusterSummaryInfoVO);
            return;
        }
        String redisKey = renderClusterInfoRedisKey(clusterId);
        stringRedisTemplate.opsForValue().set(redisKey, JSONObject.toJSONString(clusterSummaryInfoVO));
        LOGGER.info("Finish saving info about cluster with id {}. The redisKey is {}. the info object is: {} ", clusterId, redisKey, clusterSummaryInfoVO);
    }

    @Override
    public ClusterSummaryInfoVO queryClusterSummaryInfo(Long clusterId) {
        String redisKey = renderClusterInfoRedisKey(clusterId);
        String json = stringRedisTemplate.opsForValue().get(redisKey);
        return StringUtils.isEmpty(json) ? null : JSONObject.parseObject(json, ClusterSummaryInfoVO.class);
    }

    /**
     * 获取存储集群信息到redis的key
     *
     * @param clusterId 集群id
     * @return key
     */
    private String renderClusterInfoRedisKey(Long clusterId) {
        return String.format(CLUSTER_INFO_KEY_TEMPLATE, Objects.requireNonNull(clusterId));
    }

    @Override
    @Transactional
    public String createCluster(Long projectId, DevopsClusterReqVO devopsClusterReqVO) {
        // 判断组织下是否还能创建集群
        checkEnableCreateClusterOrThrowE(projectId);
        ProjectDTO iamProject = null;
        DevopsClusterDTO devopsClusterDTO = null;
        Map<String, String> params = new HashMap<>();
        try {
            iamProject = baseServiceClientOperator.queryIamProjectById(projectId);
            // 插入记录
            devopsClusterDTO = ConvertUtils.convertObject(devopsClusterReqVO, DevopsClusterDTO.class);
            devopsClusterDTO.setToken(GenerateUUID.generateUUID());
            devopsClusterDTO.setProjectId(projectId);
            devopsClusterDTO.setOrganizationId(iamProject.getOrganizationId());
            devopsClusterDTO.setSkipCheckProjectPermission(true);
            devopsClusterDTO = baseCreateCluster(devopsClusterDTO);


            IamUserDTO iamUserDTO = baseServiceClientOperator.queryUserByUserId(GitUserNameUtil.getUserId());

            // 渲染激活环境的命令参数
            params.put("{VERSION}", agentExpectVersion);
            params.put("{NAME}", "choerodon-cluster-agent-" + devopsClusterDTO.getCode());
            params.put("{SERVICEURL}", agentServiceUrl);
            params.put("{TOKEN}", devopsClusterDTO.getToken());
            params.put("{EMAIL}", iamUserDTO == null ? "" : iamUserDTO.getEmail());
            params.put("{CHOERODONID}", devopsClusterDTO.getChoerodonId());
            params.put("{REPOURL}", agentRepoUrl);
            params.put("{CLUSTERID}", devopsClusterDTO
                    .getId().toString());
        } catch (Exception e) {
            //创建集群失败发送webhook json
            sendNotificationService.sendWhenCreateClusterFail(devopsClusterDTO, iamProject, e.getMessage());
            throw e;
        }

        //创建集群成功发送web_hook
        sendNotificationService.sendWhenCreateCluster(devopsClusterDTO, iamProject);
        return FileUtil.replaceReturnString(CLUSTER_ACTIVATE_COMMAND_TEMPLATE, params);
    }

    private void checkEnableCreateClusterOrThrowE(Long projectId) {
        if (Boolean.FALSE.equals(checkEnableCreateCluster(projectId))) {
            throw new CommonException(ERROR_ORGANIZATION_CLUSTER_NUM_MAX);
        }
    }

    @Override
    public Boolean checkEnableCreateCluster(Long projectId) {
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
        Long organizationId = projectDTO.getOrganizationId();
        if (baseServiceClientOperator.checkOrganizationIsRegistered(organizationId)) {
            ResourceLimitVO resourceLimitVO = baseServiceClientOperator.queryResourceLimit();
            DevopsClusterDTO example = new DevopsClusterDTO();
            example.setOrganizationId(organizationId);
            int num = devopsClusterMapper.selectCount(example);
            return num < resourceLimitVO.getClusterMaxNumber();
        }
        return true;
    }

    @Override
    @Transactional
    public void updateCluster(Long projectId, Long clusterId, DevopsClusterUpdateVO devopsClusterUpdateVO) {
        if (StringUtils.isEmpty(devopsClusterUpdateVO.getName())) {
            devopsClusterUpdateVO.setName(null);
        }
        baseUpdate(projectId, ConvertUtils.convertObject(devopsClusterUpdateVO, DevopsClusterDTO.class));
    }

    @Override
    public boolean isNameUnique(Long projectId, String name) {
        DevopsClusterDTO devopsClusterDTO = new DevopsClusterDTO();
        devopsClusterDTO.setProjectId(Objects.requireNonNull(projectId));
        devopsClusterDTO.setName(Objects.requireNonNull(name));
        return devopsClusterMapper.selectCount(devopsClusterDTO) == 0;
    }

    @Override
    public String queryShell(Long clusterId) {
        DevopsClusterRepVO devopsClusterRepVO = getDevopsClusterStatus(clusterId);
        InputStream inputStream = this.getClass().getResourceAsStream("/shell/cluster.sh");

        //初始化渲染脚本
        IamUserDTO iamUserDTO = baseServiceClientOperator.queryUserByUserId(devopsClusterRepVO.getCreatedBy());
        Map<String, String> params = new HashMap<>();
        params.put("{VERSION}", agentExpectVersion);
        params.put("{NAME}", "choerodon-cluster-agent-" + devopsClusterRepVO.getCode());
        params.put("{SERVICEURL}", agentServiceUrl);
        params.put("{TOKEN}", devopsClusterRepVO.getToken());
        params.put("{EMAIL}", iamUserDTO == null ? "" : iamUserDTO.getEmail());
        params.put("{REPOURL}", agentRepoUrl);
        params.put("{CHOERODONID}", devopsClusterRepVO.getChoerodonId());
        params.put("{CLUSTERID}", devopsClusterRepVO
                .getId().toString());
        return FileUtil.replaceReturnString(inputStream, params);
    }

    @Override
    public boolean isCodeUnique(Long projectId, String code) {
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
        DevopsClusterDTO devopsClusterDTO = new DevopsClusterDTO();
        devopsClusterDTO.setOrganizationId(projectDTO.getOrganizationId());
        devopsClusterDTO.setCode(code);
        return devopsClusterMapper.selectCount(devopsClusterDTO) == 0;
    }

    @Override
    public Page<ClusterWithNodesVO> pageClusters(Long projectId, Boolean doPage, PageRequest pageable, String params) {
        Page<DevopsClusterRepVO> devopsClusterRepVOPageInfo = ConvertUtils.convertPage(basePageClustersByOptions(projectId, doPage, pageable, params), DevopsClusterRepVO.class);
        Page<ClusterWithNodesVO> devopsClusterRepDTOPage = ConvertUtils.convertPage(devopsClusterRepVOPageInfo, ClusterWithNodesVO.class);

        List<Long> updatedEnvList = clusterConnectionHandler.getUpdatedClusterList();
        devopsClusterRepVOPageInfo.getContent().forEach(devopsClusterRepVO -> devopsClusterRepVO.setConnect(updatedEnvList.contains(devopsClusterRepVO.getId())));

        devopsClusterRepDTOPage.setContent(fromClusterE2ClusterWithNodesDTO(devopsClusterRepVOPageInfo.getContent(), projectId));
        return devopsClusterRepDTOPage;
    }

    @Override
    public Page<ProjectReqVO> listNonRelatedProjects(Long projectId, Long clusterId, Long selectedProjectId, PageRequest pageable, String params) {
        DevopsClusterDTO devopsClusterDTO = devopsClusterMapper.selectByPrimaryKey(clusterId);
        if (devopsClusterDTO == null) {
            throw new CommonException(ERROR_CLUSTER_NOT_EXIST, clusterId);
        }

        Map<String, String> searchParamMap = new HashMap<>();
        List<String> paramList = new ArrayList<>();
        if (!StringUtils.isEmpty(params)) {
            Map<String, Object> maps = TypeUtil.castMapParams(params);
            searchParamMap = org.apache.commons.lang3.ObjectUtils.defaultIfNull(TypeUtil.cast(maps.get(TypeUtil.SEARCH_PARAM)), Collections.emptyMap());
            paramList = org.apache.commons.lang3.ObjectUtils.defaultIfNull(TypeUtil.cast(maps.get(TypeUtil.PARAMS)), Collections.emptyList());
        }

        ProjectDTO iamProjectDTO = baseServiceClientOperator.queryIamProjectById(projectId);

        // 查出组织下所有符合条件的项目
        List<ProjectDTO> filteredProjects = baseServiceClientOperator.listIamProjectByOrgId(
                iamProjectDTO.getOrganizationId(),
                searchParamMap.get("name"),
                searchParamMap.get("code"),
                CollectionUtils.isEmpty(paramList) ? null : paramList.get(0));

        // 查出数据库中已经分配权限的项目
        List<Long> permitted = devopsClusterProPermissionService.baseListByClusterId(clusterId)
                .stream()
                .map(DevopsClusterProPermissionDTO::getProjectId)
                .collect(Collectors.toList());

        // 将已经分配权限的项目过滤
        List<ProjectReqVO> projectReqVOS = filteredProjects
                .stream()
                .filter(p -> !permitted.contains(p.getId()))
                .map(p -> new ProjectReqVO(p.getId(), p.getName(), p.getCode()))
                .collect(Collectors.toList());

        if (selectedProjectId != null) {
            ProjectDTO selectedProjectDTO = baseServiceClientOperator.queryIamProjectById(selectedProjectId);
            ProjectReqVO projectReqVO = new ProjectReqVO(selectedProjectDTO.getId(), selectedProjectDTO.getName(), selectedProjectDTO.getCode());
            if (!projectReqVOS.isEmpty()) {
                projectReqVOS.remove(projectReqVO);
                projectReqVOS.add(0, projectReqVO);
            } else {
                projectReqVOS.add(projectReqVO);
            }
        }
        return PageInfoUtil.createPageFromList(projectReqVOS, pageable);
    }

    @Transactional
    @Override
    public void assignPermission(Long projectId, DevopsClusterPermissionUpdateVO update) {
        DevopsClusterDTO devopsClusterDTO = devopsClusterMapper.selectByPrimaryKey(update.getClusterId());
        if (devopsClusterDTO == null) {
            throw new CommonException(ERROR_CLUSTER_NOT_EXIST, update.getClusterId());
        }
        CommonExAssertUtil.assertTrue(projectId.equals(devopsClusterDTO.getProjectId()), MiscConstants.ERROR_OPERATING_RESOURCE_IN_OTHER_PROJECT);

        if (devopsClusterDTO.getSkipCheckProjectPermission()) {
            // 原来跳过，现在也跳过，不处理

            if (!update.getSkipCheckProjectPermission()) {
                // 原来跳过，现在不跳过，先更新字段，然后插入关联关系
                updateSkipPermissionCheck(
                        update.getClusterId(),
                        update.getSkipCheckProjectPermission(),
                        update.getObjectVersionNumber());

                devopsClusterProPermissionService.batchInsertIgnore(
                        update.getClusterId(),
                        update.getProjectIds());

                //如果在PV里面有未非配权限的项目，则删除
                List<DevopsPvProPermissionDTO> devopsPvProPermissionDTOList = devopsPvProPermissionMapper.listByClusterId(update.getClusterId());
                if (!devopsPvProPermissionDTOList.isEmpty()) {
                    List<DevopsPvProPermissionDTO> devopsPvProPermissionDTOToDeleteList = devopsPvProPermissionDTOList.stream()
                            .filter(e -> !update.getProjectIds().contains(e.getProjectId()))
                            .collect(Collectors.toList());
                    if (!devopsPvProPermissionDTOToDeleteList.isEmpty()) {
                        devopsPvProPermissionMapper.batchDelete(devopsPvProPermissionDTOToDeleteList);
                    }
                }

            }
        } else {
            // 原来不跳过，现在跳过，更新集群权限字段，再删除所有数据库中与该集群有关的关联关系
            if (update.getSkipCheckProjectPermission()) {
                updateSkipPermissionCheck(
                        update.getClusterId(),
                        update.getSkipCheckProjectPermission(),
                        update.getObjectVersionNumber());

                devopsClusterProPermissionService.baseDeleteByClusterId(update.getClusterId());
            } else {
                // 原来不跳过，现在也不跳过，批量添加权限
                devopsClusterProPermissionService.batchInsertIgnore(
                        update.getClusterId(),
                        update.getProjectIds());
            }
        }
    }

    /**
     * 更新集群的权限校验字段
     *
     * @param clusterId           集群id
     * @param skipCheckPermission 是否跳过权限校验
     * @param objectVersionNumber 版本号
     */
    private void updateSkipPermissionCheck(Long clusterId, Boolean skipCheckPermission, Long objectVersionNumber) {
        DevopsClusterDTO toUpdate = new DevopsClusterDTO();
        toUpdate.setId(clusterId);
        toUpdate.setObjectVersionNumber(objectVersionNumber);
        toUpdate.setSkipCheckProjectPermission(skipCheckPermission);
        devopsClusterMapper.updateByPrimaryKeySelective(toUpdate);
    }

    @Override
    public void deletePermissionOfProject(Long projectId, Long clusterId, Long relatedProjectId) {
        DevopsClusterDTO devopsClusterDTO = devopsClusterMapper.selectByPrimaryKey(clusterId);
        CommonExAssertUtil.assertTrue(projectId.equals(devopsClusterDTO.getProjectId()), MiscConstants.ERROR_OPERATING_RESOURCE_IN_OTHER_PROJECT);
        if (clusterId == null || relatedProjectId == null) {
            return;
        }
        //查出该集群关联的所有PV，删除与relatedProjectId的关联信息
        List<Long> pvIds = devopsPvService.queryByClusterId(clusterId).stream()
                .map(DevopsPvDTO::getId)
                .collect(Collectors.toList());
        if (!pvIds.isEmpty()) {
            devopsPvProPermissionMapper.batchDeleteByPvIdsAndProjectId(pvIds, relatedProjectId);
        }

        devopsClusterProPermissionService.baseDeletePermissionByClusterIdAndProjectId(clusterId, relatedProjectId);
    }

    @Override
    public List<DevopsClusterBasicInfoVO> queryClustersAndNodes(Long projectId) {
        DevopsClusterDTO devopsClusterDTO = new DevopsClusterDTO();
        devopsClusterDTO.setProjectId(projectId);
        List<DevopsClusterDTO> devopsClusterDTOList = devopsClusterMapper.select(devopsClusterDTO);
        List<DevopsClusterBasicInfoVO> devopsClusterBasicInfoVOList = ConvertUtils.convertList(devopsClusterDTOList, DevopsClusterBasicInfoVO.class);
        List<Long> updatedEnvList = clusterConnectionHandler.getUpdatedClusterList();

        // 连接的集群
        List<DevopsClusterBasicInfoVO> connectedClusters = new ArrayList<>();
        // 未连接的集群
        List<DevopsClusterBasicInfoVO> unconnectedClusters = new ArrayList<>();
        devopsClusterBasicInfoVOList.forEach(devopsClusterBasicInfoVO -> {
            boolean connect = updatedEnvList.contains(devopsClusterBasicInfoVO.getId());
            devopsClusterBasicInfoVO.setConnect(connect);
            if (connect) {
                connectedClusters.add(devopsClusterBasicInfoVO);
            } else {
                unconnectedClusters.add(devopsClusterBasicInfoVO);
            }
        });

        // 将连接的集群放置在未连接的集群前
        connectedClusters.addAll(unconnectedClusters);
        connectedClusters.forEach(devopsClusterBasicInfoVO ->
                devopsClusterBasicInfoVO.setNodes(clusterNodeInfoService.queryNodeName(projectId, devopsClusterBasicInfoVO.getId())));

        return connectedClusters;
    }

    @Override
    public Page<ProjectReqVO> pageRelatedProjects(Long projectId, Long clusterId, PageRequest pageable, String params) {
        DevopsClusterDTO devopsClusterDTO = devopsClusterMapper.selectByPrimaryKey(clusterId);
        if (devopsClusterDTO == null) {
            throw new CommonException(ERROR_CLUSTER_NOT_EXIST, clusterId);
        }

        Map<String, Object> map = TypeUtil.castMapParams(params);
        Map<String, Object> searchParamsMap = TypeUtil.cast(map.get(TypeUtil.SEARCH_PARAM));
        String name = null;
        String code = null;
        if (!CollectionUtils.isEmpty(searchParamsMap)) {
            name = TypeUtil.cast(searchParamsMap.get("name"));
            code = TypeUtil.cast(searchParamsMap.get("code"));
        }
        List<String> paramList = TypeUtil.cast(map.get(TypeUtil.PARAMS));
        if (CollectionUtils.isEmpty(paramList)) {
            //如果不分页
            if (pageable.getSize() == 0) {
                Set<Long> devopsProjectIds = devopsClusterProPermissionService.baseListByClusterId(clusterId).stream()
                        .map(DevopsClusterProPermissionDTO::getProjectId)
                        .collect(Collectors.toSet());
                List<ProjectReqVO> projectReqVOList = baseServiceClientOperator.queryProjectsByIds(devopsProjectIds).stream()
                        .map(i -> new ProjectReqVO(i.getId(), i.getName(), i.getCode()))
                        .collect(Collectors.toList());
                return PageInfoUtil.createPageFromList(projectReqVOList, pageable);
            } else {
                // 如果不搜索
                Page<DevopsClusterProPermissionDTO> relationPage = PageHelper.doPage(pageable, () -> devopsClusterProPermissionService.baseListByClusterId(clusterId));
                return ConvertUtils.convertPage(relationPage, permission -> {
                    if (permission.getProjectId() == null) {
                        return null;
                    }
                    ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(permission.getProjectId());
                    return new ProjectReqVO(permission.getProjectId(), projectDTO.getName(), projectDTO.getCode());
                });
            }
        } else {
            // 如果要搜索，需要手动在程序内分页
            ProjectDTO iamProjectDTO = baseServiceClientOperator.queryIamProjectById(projectId);

            // 手动查出所有组织下的项目
            List<ProjectDTO> filteredProjects = baseServiceClientOperator.listIamProjectByOrgId(
                    iamProjectDTO.getOrganizationId(),
                    name, code,
                    paramList.get(0));

            // 数据库中的有权限的项目
            List<Long> permissions = devopsClusterProPermissionService.baseListByClusterId(clusterId)
                    .stream()
                    .map(DevopsClusterProPermissionDTO::getProjectId)
                    .collect(Collectors.toList());

            // 过滤出在数据库中有权限的项目信息
            List<ProjectReqVO> allMatched = filteredProjects
                    .stream()
                    .filter(p -> permissions.contains(p.getId()))
                    .map(p -> ConvertUtils.convertObject(p, ProjectReqVO.class))
                    .collect(Collectors.toList());

            return PageInfoUtil.createPageFromList(allMatched, pageable);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void deleteCluster(Long projectId, Long clusterId) {
        DevopsClusterDTO devopsClusterDTO = devopsClusterMapper.selectByPrimaryKey(clusterId);
        if (devopsClusterDTO == null) {
            return;
        }
        CommonExAssertUtil.assertTrue(projectId.equals(devopsClusterDTO.getProjectId()), MiscConstants.ERROR_OPERATING_RESOURCE_IN_OTHER_PROJECT);

        // 校验集群是否能够删除
        checkConnectEnvsAndPV(clusterId);

        if (!ObjectUtils.isEmpty(devopsClusterDTO.getClientId())) {
            baseServiceClientOperator.deleteClient(devopsClusterDTO.getOrganizationId(), devopsClusterDTO.getClientId());
        }
        devopsEnvironmentService.deleteSystemEnv(devopsClusterDTO.getProjectId(), devopsClusterDTO.getId(), devopsClusterDTO.getCode(), devopsClusterDTO.getSystemEnvId());

        polarisScanningService.deleteAllByScopeAndScopeId(PolarisScopeType.CLUSTER, clusterId);

        baseDelete(clusterId);
        //删除集群后发送webhook
        sendNotificationService.sendWhenDeleteCluster(devopsClusterDTO);
    }

    @Override
    public ClusterMsgVO checkConnectEnvsAndPV(Long clusterId) {
        ClusterMsgVO clusterMsgVO = new ClusterMsgVO(false, false);
        List<Long> connectedEnvList = clusterConnectionHandler.getUpdatedClusterList();
        List<DevopsEnvironmentDTO> devopsEnvironmentDTOS = devopsEnvironmentService.baseListUserEnvByClusterId(clusterId);

        if (connectedEnvList.contains(clusterId)) {
            clusterMsgVO.setCheckEnv(true);
        }
        if (!devopsEnvironmentDTOS.isEmpty()) {
            clusterMsgVO.setCheckEnv(true);
        }
        //集群是否存在PV
        List<DevopsPvDTO> clusterDTOList = devopsPvService.queryByClusterId(clusterId);
        if (!Objects.isNull(clusterDTOList) && !clusterDTOList.isEmpty()) {
            clusterMsgVO.setCheckPV(true);
        }
        return clusterMsgVO;
    }


    @Override
    public DevopsClusterRepVO query(Long clusterId) {
        DevopsClusterRepVO result = ConvertUtils.convertObject(baseQuery(clusterId), DevopsClusterRepVO.class);
        if (result == null) {
            return null;
        }
        List<Long> upToDateList = clusterConnectionHandler.getUpdatedClusterList();
        result.setConnect(upToDateList.contains(clusterId));
        return result;
    }

    @Override
    public Page<DevopsEnvPodVO> pagePodsByNodeName(Long clusterId, String nodeName, PageRequest pageable, String searchParam) {
        Page<DevopsEnvPodDTO> devopsEnvPodDTOPageInfo = basePageQueryPodsByNodeName(clusterId, nodeName, pageable, searchParam);
        Page<DevopsEnvPodVO> envPodVOPageInfo = ConvertUtils.convertPage(devopsEnvPodDTOPageInfo, DevopsEnvPodVO.class);

        envPodVOPageInfo.setContent(devopsEnvPodDTOPageInfo.getContent().stream().map(this::podDTO2VO).collect(Collectors.toList()));
        return envPodVOPageInfo;
    }

    @Override
    public DevopsClusterRepVO queryByCode(Long projectId, String code) {
        return ConvertUtils.convertObject(baseQueryByCode(projectId, code), DevopsClusterRepVO.class);
    }


    @Override
    public DevopsClusterDTO baseCreateCluster(DevopsClusterDTO devopsClusterDTO) {
        List<DevopsClusterDTO> devopsClusterDTOS = devopsClusterMapper.selectAll();
        if (!devopsClusterDTOS.isEmpty()) {
            // 如果数据库有集群数据，就使用第一个集群的choerodonId作为新的集群的choerodonId
            devopsClusterDTO.setChoerodonId(devopsClusterDTOS.get(0).getChoerodonId());
        } else {
            // 加上a前缀(前缀是字母即可)是为了解决随机UUID生成纯数字字符串的问题, 这样会导致agent的安装失败，
            // 因为传入的参数会变为科学计数法，而这个值(转为科学计数法的值)又被用于chart中一个configMap的名称
            // 就会因为configMap的名称不规范导致agent安装失败
            String choerodonId = "a" + GenerateUUID.generateUUID().split("-")[0];
            devopsClusterDTO.setChoerodonId(choerodonId);
        }
        if (devopsClusterMapper.insert(devopsClusterDTO) != 1) {
            throw new CommonException("error.devops.cluster.insert");
        }
        return devopsClusterDTO;
    }

    @Override
    public List<DevopsClusterDTO> baseListByProjectId(Long projectId, Long organizationId) {
        return devopsClusterMapper.listByProjectId(projectId, organizationId);
    }

    @Override
    public DevopsClusterDTO baseQuery(Long clusterId) {
        return devopsClusterMapper.selectByPrimaryKey(clusterId);
    }

    @Override
    public void baseUpdate(Long projectId, DevopsClusterDTO inputClusterDTO) {
        DevopsClusterDTO devopsClusterDTO = devopsClusterMapper.selectByPrimaryKey(inputClusterDTO.getId());
        // 内部调用不需要校验
        if (projectId != null) {
            CommonExAssertUtil.assertTrue(projectId.equals(devopsClusterDTO.getProjectId()), MiscConstants.ERROR_OPERATING_RESOURCE_IN_OTHER_PROJECT);
        }
        inputClusterDTO.setObjectVersionNumber(devopsClusterDTO.getObjectVersionNumber());
        devopsClusterMapper.updateByPrimaryKeySelective(inputClusterDTO);
    }

    @Override
    public Page<DevopsClusterDTO> basePageClustersByOptions(Long projectId, Boolean doPage, PageRequest pageable, String params) {
        Map<String, Object> searchParamMap = TypeUtil.castMapParams(params);
        return PageHelper.doPageAndSort(PageRequestUtil.simpleConvertSortForPage(pageable),
                () -> devopsClusterMapper.listClusters(
                        projectId,
                        TypeUtil.cast(searchParamMap.get(TypeUtil.SEARCH_PARAM)),
                        TypeUtil.cast(searchParamMap.get(TypeUtil.PARAMS))));
    }

    @Override
    public void baseDelete(Long clusterId) {
        devopsClusterMapper.deleteByPrimaryKey(clusterId);
    }

    @Override
    public DevopsClusterDTO baseQueryByToken(String token) {
        DevopsClusterDTO devopsClusterDTO = new DevopsClusterDTO();
        devopsClusterDTO.setToken(token);
        return devopsClusterMapper.selectOne(devopsClusterDTO);
    }

    @Override
    public List<DevopsClusterDTO> baseList() {
        return devopsClusterMapper.selectAll();
    }

    @Override
    public Page<DevopsEnvPodDTO> basePageQueryPodsByNodeName(Long clusterId, String nodeName, PageRequest pageable, String searchParam) {
        Map<String, Object> paramMap = TypeUtil.castMapParams(searchParam);
        return PageHelper.doPageAndSort(PageRequestUtil.simpleConvertSortForPage(pageable), () -> devopsClusterMapper.pageQueryPodsByNodeName(
                clusterId, nodeName,
                TypeUtil.cast(paramMap.get(TypeUtil.SEARCH_PARAM)),
                TypeUtil.cast(paramMap.get(TypeUtil.PARAMS))));
    }

    @Override
    public DevopsClusterDTO baseQueryByCode(Long projectId, String code) {
        DevopsClusterDTO devopsClusterDTO = new DevopsClusterDTO();
        devopsClusterDTO.setProjectId(projectId);
        devopsClusterDTO.setCode(code);
        return devopsClusterMapper.selectOne(devopsClusterDTO);
    }

    @Override
    public void baseUpdateProjectId(Long orgId, Long proId) {
        devopsClusterMapper.updateProjectId(orgId, proId);
    }

    @Override
    public Boolean checkUserClusterPermission(Long clusterId, Long userId) {
        DevopsClusterDTO devopsClusterDTO = devopsClusterMapper.selectByPrimaryKey(clusterId);
        if (ObjectUtils.isEmpty(devopsClusterDTO)) {
            throw new CommonException("error.devops.cluster.is.not.exist");
        }
        if (Boolean.TRUE.equals(permissionHelper.isRoot(userId)) || Boolean.TRUE.equals(permissionHelper.isOrganizationRoot(userId, devopsClusterDTO.getOrganizationId()))) {
            return true;
        }
        if (Boolean.TRUE.equals(permissionHelper.isGitlabProjectOwner(userId, devopsClusterDTO.getProjectId()))) {
            return true;
        }
        // 获取集群和集群分配的项目Ids
        List<DevopsClusterProPermissionDTO> devopsClusterProPermissionDTOS = devopsClusterProPermissionService.baseListByClusterId(clusterId);

        return devopsClusterProPermissionDTOS.stream()
                .anyMatch(devopsClusterProPermissionDTO ->
                        permissionHelper.isGitlabProjectOwner(userId, devopsClusterProPermissionDTO.getProjectId()));
    }

    @Override
    public ClusterOverViewVO getOrganizationClusterOverview(Long organizationId) {
        List<Long> updatedClusterList = clusterConnectionHandler.getUpdatedClusterList();
        List<DevopsClusterDTO> clusterDTOList = devopsClusterMapper.listByOrganizationId(organizationId);
        if (CollectionUtils.isEmpty(clusterDTOList)) {

            return new ClusterOverViewVO(0, 0);
        }
        if (CollectionUtils.isEmpty(updatedClusterList)) {
            return new ClusterOverViewVO(0, updatedClusterList.size());
        }
        int connectedCount = 0;
        for (DevopsClusterDTO v : clusterDTOList) {
            if (updatedClusterList.contains(v.getId())) {
                connectedCount++;
            }
        }
        return new ClusterOverViewVO(connectedCount, clusterDTOList.size() - connectedCount);
    }

    @Override
    public ClusterOverViewVO getSiteClusterOverview() {
        int allCount = devopsClusterMapper.countByOptions(null, null);
        int updatedCount = clusterConnectionHandler.getUpdatedClusterList().size();
        return new ClusterOverViewVO(updatedCount, allCount - updatedCount);
    }

    /**
     * pod dto to cluster pod vo
     *
     * @param devopsEnvPodDTO pod dto
     * @return the cluster pod vo
     */
    private DevopsEnvPodVO podDTO2VO(final DevopsEnvPodDTO devopsEnvPodDTO) {
        DevopsEnvPodVO devopsEnvPodVO = ConvertUtils.convertObject(devopsEnvPodDTO, DevopsEnvPodVO.class);
        devopsEnvPodService.fillContainers(devopsEnvPodVO);
        return devopsEnvPodVO;
    }

    /**
     * convert cluster entity to instances of {@link ClusterWithNodesVO}
     *
     * @param devopsClusterRepVOS the cluster entities
     * @param projectId           the project id
     * @return the instances of the return type
     */
    private List<ClusterWithNodesVO> fromClusterE2ClusterWithNodesDTO(List<DevopsClusterRepVO> devopsClusterRepVOS, Long projectId) {
        // default three records of nodes in the instance
        PageRequest pageable = new PageRequest(1, 3);

        return devopsClusterRepVOS.stream().map(cluster -> {
            ClusterWithNodesVO clusterWithNodesDTO = new ClusterWithNodesVO();
            BeanUtils.copyProperties(cluster, clusterWithNodesDTO);
            if (Boolean.TRUE.equals(clusterWithNodesDTO.getConnect())) {
                clusterWithNodesDTO.setNodes(clusterNodeInfoService.pageClusterNodeInfo(cluster.getId(), projectId, pageable));
            }
            return clusterWithNodesDTO;
        }).collect(Collectors.toList());
    }

    private DevopsClusterRepVO getDevopsClusterStatus(Long clusterId) {
        DevopsClusterRepVO devopsClusterRepVO = ConvertUtils.convertObject(baseQuery(clusterId), DevopsClusterRepVO.class);
        List<Long> updatedEnvList = clusterConnectionHandler.getUpdatedClusterList();

        devopsClusterRepVO.setConnect(updatedEnvList.contains(devopsClusterRepVO.getId()));
        return devopsClusterRepVO;
    }


}
