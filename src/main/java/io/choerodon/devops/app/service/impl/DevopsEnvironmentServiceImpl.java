package io.choerodon.devops.app.service.impl;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.choerodon.asgard.saga.annotation.Saga;
import io.choerodon.asgard.saga.producer.StartSagaBuilder;
import io.choerodon.asgard.saga.producer.TransactionalProducer;
import io.choerodon.base.domain.PageRequest;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.devops.api.validator.DevopsEnvironmentValidator;
import io.choerodon.devops.api.vo.*;
import io.choerodon.devops.api.vo.iam.UserVO;
import io.choerodon.devops.app.eventhandler.constants.SagaTopicCodeConstants;
import io.choerodon.devops.app.eventhandler.payload.DevopsEnvUserPayload;
import io.choerodon.devops.app.eventhandler.payload.EnvGitlabProjectPayload;
import io.choerodon.devops.app.eventhandler.payload.GitlabProjectPayload;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.infra.dto.*;
import io.choerodon.devops.infra.dto.gitlab.CommitDTO;
import io.choerodon.devops.infra.dto.gitlab.GitlabProjectDTO;
import io.choerodon.devops.infra.dto.gitlab.MemberDTO;
import io.choerodon.devops.infra.dto.gitlab.ProjectHookDTO;
import io.choerodon.devops.infra.dto.iam.IamUserDTO;
import io.choerodon.devops.infra.dto.iam.OrganizationDTO;
import io.choerodon.devops.infra.dto.iam.ProjectDTO;
import io.choerodon.devops.infra.enums.*;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;
import io.choerodon.devops.infra.feign.operator.GitlabServiceClientOperator;
import io.choerodon.devops.infra.handler.ClusterConnectionHandler;
import io.choerodon.devops.infra.mapper.*;
import io.choerodon.devops.infra.util.*;

/**
 * Created by younger on 2018/4/9.
 */
@Service
public class DevopsEnvironmentServiceImpl implements DevopsEnvironmentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DevopsEnvironmentServiceImpl.class);

    private static final Gson gson = new Gson();
    private static final String MEMBER = "member";
    private static final String OWNER = "owner";
    private static final String MASTER = "master";
    private static final String README = "README.md";
    private static final String README_CONTENT =
            "# This is gitops env repository!";
    private static final String ENV = "ENV";
    private static final String PROJECT_OWNER = "role/project/default/project-owner";
    private static final String PROJECT_MEMBER = "role/project/default/project-member";
    private static final String ERROR_CODE_EXIST = "error.code.exist";
    private static final String ERROR_GITLAB_USER_SYNC_FAILED = "error.gitlab.user.sync.failed";
    private static final String LOGIN_NAME = "loginName";
    private static final String REAL_NAME = "realName";
    private static final Pattern CODE = Pattern.compile("[a-z]([-a-z0-9]*[a-z0-9])?");


    @Value("${services.gateway.url}")
    private String gatewayUrl;

    @Value("${services.gitlab.url}")
    private String gitlabUrl;

    @Autowired
    private DevopsServiceService devopsServiceService;
    @Autowired
    private DevopsEnvironmentValidator devopsEnvironmentValidator;
    @Autowired
    private AgentCommandService agentCommandService;
    @Autowired
    private DevopsEnvironmentMapper devopsEnvironmentMapper;
    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;
    @Autowired
    private UserAttrService userAttrService;
    @Autowired
    private DevopsProjectService devopsProjectService;
    @Autowired
    private DevopsEnvGroupService devopsEnvGroupService;
    @Autowired
    private DevopsClusterService devopsClusterService;
    @Autowired
    private DevopsEnvUserPermissionService devopsEnvUserPermissionService;
    @Autowired
    private DevopsEnvUserPermissionMapper devopsEnvUserPermissionMapper;
    @Autowired
    private GitlabServiceClientOperator gitlabServiceClientOperator;
    @Autowired
    private DevopsEnvCommitService devopsEnvCommitService;
    @Autowired
    private AppServiceInstanceService appServiceInstanceService;
    @Autowired
    private DevopsEnvFileErrorMapper devopsEnvFileErrorMapper;
    @Autowired
    private DevopsEnvCommandService devopsEnvCommandService;
    @Autowired
    private DevopsIngressService devopsIngressService;
    @Autowired
    private TransactionalProducer producer;
    @Autowired
    private GitlabGroupMemberService gitlabGroupMemberService;
    @Autowired
    private DevopsGitService devopsGitService;
    @Autowired
    private ClusterConnectionHandler clusterConnectionHandler;
    @Autowired
    private PipelineAppDeployService pipelineAppDeployService;
    @Autowired
    private AppServiceInstanceMapper appServiceInstanceMapper;
    @Autowired
    private DevopsServiceMapper devopsServiceMapper;
    @Autowired
    private DevopsIngressMapper devopsIngressMapper;
    @Autowired
    private DevopsCertificationMapper devopsCertificationMapper;
    @Autowired
    private DevopsConfigMapMapper devopsConfigMapMapper;
    @Autowired
    private DevopsSecretMapper devopsSecretMapper;
    @Autowired
    private DevopsCustomizeResourceMapper devopsCustomizeResourceMapper;
    @Autowired
    private DevopsEnvAppServiceMapper devopsEnvAppServiceMapper;
    @Autowired
    private DevopsEnvGroupMapper devopsEnvGroupMapper;
    @Autowired
    private DevopsDeployRecordService devopsDeployRecordService;
    @Autowired
    private AppServiceService appServiceService;

    @Override
    @Saga(code = SagaTopicCodeConstants.DEVOPS_CREATE_ENV, description = "创建环境", inputSchema = "{}")
    @Transactional(rollbackFor = Exception.class)
    public void create(Long projectId, DevopsEnvironmentReqVO devopsEnvironmentReqVO) {
        DevopsEnvironmentDTO devopsEnvironmentDTO = ConvertUtils.convertObject(devopsEnvironmentReqVO, DevopsEnvironmentDTO.class);
        // 创建环境时默认跳过权限校验
        devopsEnvironmentDTO.setSkipCheckPermission(Boolean.TRUE);
        devopsEnvironmentDTO.setProjectId(projectId);

        checkCode(projectId, devopsEnvironmentReqVO.getClusterId(), devopsEnvironmentReqVO.getCode());
        devopsEnvGroupService.checkGroupIdInProject(devopsEnvironmentDTO.getDevopsEnvGroupId(), projectId);

        devopsEnvironmentDTO.setActive(true);
        devopsEnvironmentDTO.setConnected(false);
        devopsEnvironmentDTO.setSynchro(false);
        devopsEnvironmentDTO.setFailed(false);
        devopsEnvironmentDTO.setClusterId(devopsEnvironmentReqVO.getClusterId());
        devopsEnvironmentDTO.setToken(GenerateUUID.generateUUID());
        devopsEnvironmentDTO.setProjectId(projectId);
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
        OrganizationDTO organizationDTO = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());

        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));
        if (userAttrDTO == null) {
            throw new CommonException(ERROR_GITLAB_USER_SYNC_FAILED);
        }

        // 查询创建应用所在的gitlab应用组
        DevopsProjectDTO devopsProjectDTO = devopsProjectService.baseQueryByProjectId(projectId);
        MemberDTO memberDTO = gitlabServiceClientOperator.queryGroupMember(
                TypeUtil.objToInteger(devopsProjectDTO.getDevopsEnvGroupId()),
                TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()));
        if (memberDTO == null || !memberDTO.getAccessLevel().equals(AccessLevel.OWNER.toValue())) {
            throw new CommonException("error.user.not.owner");
        }

        List<String> sshKeys = FileUtil.getSshKey(
                organizationDTO.getCode() + "/" + projectDTO.getCode() + "/" + devopsEnvironmentReqVO.getCode());
        devopsEnvironmentDTO.setEnvIdRsa(sshKeys.get(0));
        devopsEnvironmentDTO.setEnvIdRsaPub(sshKeys.get(1));
        Long envId = baseCreate(devopsEnvironmentDTO).getId();
        devopsEnvironmentDTO.setId(envId);

        EnvGitlabProjectPayload gitlabProjectPayload = new EnvGitlabProjectPayload();
        gitlabProjectPayload.setGroupId(TypeUtil.objToInteger(devopsProjectDTO.getDevopsEnvGroupId()));
        gitlabProjectPayload.setUserId(TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()));
        gitlabProjectPayload.setPath(devopsEnvironmentReqVO.getCode());
        gitlabProjectPayload.setOrganizationId(null);
        gitlabProjectPayload.setType(ENV);
        IamUserDTO iamUserDTO = baseServiceClientOperator.queryUserByUserId(userAttrDTO.getIamUserId());
        gitlabProjectPayload.setLoginName(iamUserDTO.getLoginName());
        gitlabProjectPayload.setRealName(iamUserDTO.getRealName());
        gitlabProjectPayload.setClusterId(devopsEnvironmentReqVO.getClusterId());
        gitlabProjectPayload.setIamProjectId(projectId);
        gitlabProjectPayload.setSkipCheckPermission(devopsEnvironmentDTO.getSkipCheckPermission());

        producer.apply(
                StartSagaBuilder
                        .newBuilder()
                        .withLevel(ResourceLevel.PROJECT)
                        .withSagaCode(SagaTopicCodeConstants.DEVOPS_CREATE_ENV)
                        .withPayloadAndSerialize(gitlabProjectPayload)
                        .withRefId("")
                        .withRefType("")
                        .withSourceId(projectId),
                builder -> {
                }
        );
        agentCommandService.initEnv(devopsEnvironmentDTO, devopsEnvironmentReqVO.getClusterId());
    }


    @Override
    public List<DevopsEnvGroupEnvsVO> listDevopsEnvGroupEnvs(Long projectId, Boolean active) {
        List<DevopsEnvGroupEnvsVO> devopsEnvGroupEnvsDTOS = new ArrayList<>();
        List<Long> upgradeClusterList = clusterConnectionHandler.getUpdatedClusterList();
        List<DevopsEnvironmentDTO> devopsEnvironmentDTOS = baseListByProjectIdAndActive(projectId, active).stream().peek(t ->
                setEnvStatus(upgradeClusterList, t)
        )
                .collect(Collectors.toList());
        List<DevopsEnvironmentRepVO> devopsEnviromentRepDTOS = ConvertUtils.convertList(devopsEnvironmentDTOS, DevopsEnvironmentRepVO.class);
        if (!active) {
            DevopsEnvGroupEnvsVO devopsEnvGroupEnvsDTO = new DevopsEnvGroupEnvsVO();
            devopsEnvGroupEnvsDTO.setDevopsEnvironmentRepDTOs(devopsEnviromentRepDTOS);
            devopsEnvGroupEnvsDTOS.add(devopsEnvGroupEnvsDTO);
            return devopsEnvGroupEnvsDTOS;
        }
        List<DevopsEnvGroupDTO> devopsEnvGroupES = devopsEnvGroupService.baseListByProjectId(projectId);
        devopsEnviromentRepDTOS.forEach(devopsEnviromentRepDTO -> {
            DevopsClusterDTO devopsClusterDTO = devopsClusterService.baseQuery(devopsEnviromentRepDTO.getClusterId());
            devopsEnviromentRepDTO.setClusterName(devopsClusterDTO == null ? null : devopsClusterDTO.getName());
            if (devopsEnviromentRepDTO.getDevopsEnvGroupId() == null) {
                devopsEnviromentRepDTO.setDevopsEnvGroupId(0L);
            }
        });
        //按照环境组分组查询，有环境的环境组放前面，没环境的环境组放后面
        Map<Long, List<DevopsEnvironmentRepVO>> resultMaps = devopsEnviromentRepDTOS.stream()
                .collect(Collectors.groupingBy(DevopsEnvironmentRepVO::getDevopsEnvGroupId));

        List<Long> envGroupIds = new ArrayList<>();
        resultMaps.forEach((key, value) -> {
            envGroupIds.add(key);
            DevopsEnvGroupEnvsVO devopsEnvGroupEnvsDTO = new DevopsEnvGroupEnvsVO();
            DevopsEnvGroupDTO devopsEnvGroupDTO = new DevopsEnvGroupDTO();
            if (key != 0) {
                devopsEnvGroupDTO = devopsEnvGroupService.baseQuery(key);
            }
            devopsEnvGroupEnvsDTO.setDevopsEnvGroupId(devopsEnvGroupDTO.getId());
            devopsEnvGroupEnvsDTO.setDevopsEnvGroupName(devopsEnvGroupDTO.getName());
            devopsEnvGroupEnvsDTO.setDevopsEnvironmentRepDTOs(value);
            devopsEnvGroupEnvsDTOS.add(devopsEnvGroupEnvsDTO);
        });
        devopsEnvGroupES.forEach(devopsEnvGroupE -> {
            if (!envGroupIds.contains(devopsEnvGroupE.getId())) {
                DevopsEnvGroupEnvsVO devopsEnvGroupEnvsDTO = new DevopsEnvGroupEnvsVO();
                devopsEnvGroupEnvsDTO.setDevopsEnvGroupId(devopsEnvGroupE.getId());
                devopsEnvGroupEnvsDTO.setDevopsEnvGroupName(devopsEnvGroupE.getName());
                devopsEnvGroupEnvsDTOS.add(devopsEnvGroupEnvsDTO);
            }
        });
        return devopsEnvGroupEnvsDTOS;
    }

    @Override
    public List<DevopsEnvGroupEnvsVO> listEnvTreeMenu(Long projectId) {
        List<DevopsEnvGroupEnvsVO> devopsEnvGroupEnvsDTOS = new ArrayList<>();
        // 获得环境列表(包含激活与不激活)
        List<Long> upgradeClusterList = clusterConnectionHandler.getUpdatedClusterList();

        List<DevopsEnvironmentDTO> devopsEnvironmentList = devopsEnvironmentMapper.listByProjectId(projectId)
                .stream()
                .peek(t -> setEnvStatus(upgradeClusterList, t))
                .collect(Collectors.toList());

        // 没有环境列表则返回空列表
        if (devopsEnvironmentList.isEmpty()) {
            devopsEnvGroupEnvsDTOS.add(new DevopsEnvGroupEnvsVO());
            return devopsEnvGroupEnvsDTOS;
        }
        List<DevopsEnvironmentDTO> devopsEnvironmentDTOS = devopsEnvironmentList.stream()
                .peek(t -> setEnvStatus(upgradeClusterList, t))
                .collect(Collectors.toList());

        List<DevopsEnvironmentRepVO> devopsEnvironmentRepDTOS = ConvertUtils.convertList(devopsEnvironmentDTOS, DevopsEnvironmentRepVO.class);

        List<DevopsEnvGroupDTO> devopsEnvGroupES = devopsEnvGroupService.baseListByProjectId(projectId);

        Map<Long, List<DevopsEnvironmentRepVO>> resultMaps = sort(devopsEnvironmentRepDTOS);

        List<Long> groupIds = new ArrayList<>(resultMaps.keySet());
        Map<Long, DevopsEnvGroupDTO> devopsEnvGroupDTOMap = new HashMap<>();
        devopsEnvGroupMapper.listByIdList(groupIds)
                .stream()
                .forEach(i -> devopsEnvGroupDTOMap.put(i.getId(), i));

        List<Long> envGroupIds = new ArrayList<>();

        //有环境的分组
        resultMaps.forEach((key, value) -> {
            envGroupIds.add(key);
            DevopsEnvGroupEnvsVO devopsEnvGroupEnvsDTO1 = new DevopsEnvGroupEnvsVO();
            DevopsEnvGroupDTO devopsEnvGroupDTO = new DevopsEnvGroupDTO();
            if (key != 0) {
                devopsEnvGroupDTO = Optional.ofNullable(devopsEnvGroupDTOMap.get(key)).
                        orElseThrow(() -> new CommonException("error.env.group.not.exist"));
            }
            devopsEnvGroupEnvsDTO1.setDevopsEnvGroupId(devopsEnvGroupDTO.getId());
            devopsEnvGroupEnvsDTO1.setDevopsEnvGroupName(devopsEnvGroupDTO.getName());
            devopsEnvGroupEnvsDTO1.setDevopsEnvironmentRepDTOs(value);
            devopsEnvGroupEnvsDTOS.add(devopsEnvGroupEnvsDTO1);
        });
        //没有环境的分组
        devopsEnvGroupES.forEach(devopsEnvGroupE -> {
            if (!envGroupIds.contains(devopsEnvGroupE.getId())) {
                DevopsEnvGroupEnvsVO devopsEnvGroupEnvsDTO2 = new DevopsEnvGroupEnvsVO();
                devopsEnvGroupEnvsDTO2.setDevopsEnvGroupId(devopsEnvGroupE.getId());
                devopsEnvGroupEnvsDTO2.setDevopsEnvGroupName(devopsEnvGroupE.getName());
                devopsEnvGroupEnvsDTOS.add(devopsEnvGroupEnvsDTO2);
            }
        });
        if (!resultMaps.containsKey(0L)) {
            devopsEnvGroupEnvsDTOS.add(new DevopsEnvGroupEnvsVO());
        }
        return devopsEnvGroupEnvsDTOS;
    }

    @Override
    public List<DevopsEnvironmentRepVO> listByGroup(Long projectId, Long groupId) {
        List<Long> upgradeClusterList = clusterConnectionHandler.getUpdatedClusterList();
        List<DevopsEnvironmentDTO> devopsEnvironmentDTOS = devopsEnvironmentMapper.listByProjectIdAndGroupId(projectId, groupId)
                .stream()
                .peek(t -> setEnvStatus(upgradeClusterList, t))
                .collect(Collectors.toList());
        if (devopsEnvironmentDTOS.isEmpty()) {
            return new ArrayList<>();
        }

        // 如果没有传入groupId，则赋值0
        if (groupId == null) {
            groupId = 0L;
        }
        return sort(ConvertUtils.convertList(devopsEnvironmentDTOS, DevopsEnvironmentRepVO.class)).get(groupId);
    }

    private Map<Long, List<DevopsEnvironmentRepVO>> sort(List<DevopsEnvironmentRepVO> devopsEnvironmentRepDTOS) {

        devopsEnvironmentRepDTOS.forEach(devopsEnvironmentRepDTO -> {
            if (devopsEnvironmentRepDTO.getDevopsEnvGroupId() == null) {
                devopsEnvironmentRepDTO.setDevopsEnvGroupId(0L);
            }
        });

        // 按分组选出处理中以及失败的环境
        Map<Long, List<DevopsEnvironmentRepVO>> synchroAndFailResultMaps = devopsEnvironmentRepDTOS
                .stream()
                .filter(i -> (!i.getSynchro()) || (i.getSynchro() && i.getFailed()))
                .sorted(Comparator.comparing(DevopsEnvironmentRepVO::getId).reversed())
                .collect(Collectors.groupingBy(DevopsEnvironmentRepVO::getDevopsEnvGroupId));
        // 按分组选出创建成功、没有停用的环境列表环境列表
        Map<Long, List<DevopsEnvironmentRepVO>> resultMaps = devopsEnvironmentRepDTOS
                .stream()
                .filter(i -> i.getSynchro() && !i.getFailed() && i.getActive())
                .sorted(
                        Comparator.comparing(DevopsEnvironmentRepVO::getConnected)
                                .thenComparing(DevopsEnvironmentRepVO::getId)
                                .reversed()
                )
                .collect(Collectors.groupingBy(DevopsEnvironmentRepVO::getDevopsEnvGroupId));
        // 按分组选出未激活环境
        Map<Long, List<DevopsEnvironmentRepVO>> notActiveResultMaps = devopsEnvironmentRepDTOS
                .stream()
                .filter(i -> i.getSynchro() && !i.getFailed() && !i.getActive())
                .collect(Collectors.groupingBy(DevopsEnvironmentRepVO::getDevopsEnvGroupId));

        synchroAndFailResultMaps.forEach((key, value) -> {
            List<DevopsEnvironmentRepVO> devopsEnvironmentRepVOList = resultMaps.get(key);
            if (devopsEnvironmentRepVOList != null) {
                devopsEnvironmentRepVOList.addAll(value);
                resultMaps.put(key, devopsEnvironmentRepVOList);
            } else {
                resultMaps.put(key, value);
            }
        });

        // 把按分组选出的停用环境加入到激活环境分组中
        notActiveResultMaps.forEach((key, value) -> {
            List<DevopsEnvironmentRepVO> devopsEnvironmentRepVOList = resultMaps.get(key);
            if (devopsEnvironmentRepVOList != null) {
                devopsEnvironmentRepVOList.addAll(value);
                resultMaps.put(key, devopsEnvironmentRepVOList);
            } else {
                resultMaps.put(key, value);
            }
        });

        return resultMaps;
    }

    @Override
    public List<DevopsEnvironmentRepVO> listByProjectIdAndActive(Long projectId, Boolean active) {

        // 查询当前用户的环境权限
        List<Long> permissionEnvIds = devopsEnvUserPermissionService
                .listByUserId(TypeUtil.objToLong(GitUserNameUtil.getUserId())).stream()
                .filter(DevopsEnvUserPermissionDTO::getPermitted)
                .map(DevopsEnvUserPermissionDTO::getEnvId).collect(Collectors.toList());
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
        // 查询当前用户是否为项目所有者
        Boolean isProjectOwner = baseServiceClientOperator
                .isProjectOwner(TypeUtil.objToLong(GitUserNameUtil.getUserId()), projectDTO);

        List<Long> upgradeClusterList = clusterConnectionHandler.getUpdatedClusterList();
        List<DevopsEnvironmentDTO> devopsEnvironmentDTOS = baseListByProjectIdAndActive(projectId, active).stream()
                .filter(devopsEnvironmentE -> !devopsEnvironmentE.getFailed()).peek(t -> {
                    setEnvStatus(upgradeClusterList, t);
                    // 项目成员返回拥有对应权限的环境，项目所有者返回所有环境
                    setPermission(t, permissionEnvIds, isProjectOwner);
                })
                .collect(Collectors.toList());
        return ConvertUtils.convertList(devopsEnvironmentDTOS, DevopsEnvironmentRepVO.class);
    }

    @Override
    public List<DevopsEnvironmentViewVO> listInstanceEnvTree(Long projectId) {
        List<Long> upgradeClusterList = clusterConnectionHandler.getUpdatedClusterList();

        List<DevopsEnvironmentViewVO> connectedEnvs = new ArrayList<>();
        List<DevopsEnvironmentViewVO> unConnectedEnvs = new ArrayList<>();

        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
        boolean isOwner = baseServiceClientOperator.isProjectOwner(DetailsHelper.getUserDetails().getUserId(), projectDTO);

        List<DevopsEnvironmentViewDTO> views;
        if (isOwner) {
            views = devopsEnvironmentMapper.listAllInstanceEnvTree(projectId);
        } else {
            views = devopsEnvironmentMapper.listMemberInstanceEnvTree(projectId, DetailsHelper.getUserDetails().getUserId());
        }

        views.forEach(e -> {
            // 将DTO层对象转为VO
            DevopsEnvironmentViewVO vo = new DevopsEnvironmentViewVO();
            BeanUtils.copyProperties(e, vo, "apps");
            boolean connected = upgradeClusterList.contains(e.getClusterId());
            vo.setConnect(connected);
            vo.setApps(e.getApps().stream().map(app -> {

                DevopsAppServiceViewVO appVO = new DevopsAppServiceViewVO();
                BeanUtils.copyProperties(app, appVO, "instances");
                AppServiceDTO appServiceDTO = appServiceService.baseQuery(app.getId());
                appVO.setType(appServiceService.checkAppServiceType(projectId,appServiceDTO));
                appVO.setInstances(app.getInstances().stream().map(ins -> {
                    DevopsAppServiceInstanceViewVO insVO = new DevopsAppServiceInstanceViewVO();
                    BeanUtils.copyProperties(ins, insVO);
                    return insVO;
                }).collect(Collectors.toList()));

                return appVO;
            }).collect(Collectors.toList()));

            if (connected) {
                connectedEnvs.add(vo);
            } else {
                unConnectedEnvs.add(vo);
            }
        });

        // 为了将环境按照状态排序: 连接（运行中） > 未连接
        connectedEnvs.addAll(unConnectedEnvs);
        return connectedEnvs;
    }

    @Override
    public List<DevopsResourceEnvOverviewVO> listResourceEnvTree(Long projectId) {
        List<Long> upgradeClusterList = clusterConnectionHandler.getUpdatedClusterList();

        List<DevopsResourceEnvOverviewVO> connectedEnvs = new ArrayList<>();
        List<DevopsResourceEnvOverviewVO> unConnectedEnvs = new ArrayList<>();

        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
        boolean isOwner = baseServiceClientOperator.isProjectOwner(DetailsHelper.getUserDetails().getUserId(), projectDTO);

        List<DevopsResourceEnvOverviewDTO> views;
        if (isOwner) {
            views = devopsEnvironmentMapper.listAllResourceEnvTree(projectId);
        } else {
            views = devopsEnvironmentMapper.listMemberResourceEnvTree(projectId, DetailsHelper.getUserDetails().getUserId());
        }

        views.forEach(e -> {
            // 将DTO层对象转为VO
            DevopsResourceEnvOverviewVO vo = new DevopsResourceEnvOverviewVO();
            BeanUtils.copyProperties(e, vo);
            boolean connected = upgradeClusterList.contains(e.getClusterId());
            vo.setConnect(connected);

            if (connected) {
                connectedEnvs.add(vo);
            } else {
                unConnectedEnvs.add(vo);
            }
        });

        // 为了将环境按照状态排序: 连接（运行中） > 未连接
        connectedEnvs.addAll(unConnectedEnvs);
        return connectedEnvs;
    }

    @Override
    public Boolean updateActive(Long projectId, Long environmentId, Boolean active) {
        if (active == null) {
            active = Boolean.TRUE;
        }

        DevopsEnvironmentDTO devopsEnvironmentDTO = baseQueryById(environmentId);
        List<Long> updatedClusterList = clusterConnectionHandler.getUpdatedClusterList();

        // 要停用环境时，对环境进行校验
        if (!active) {
            // 如果已连接
            if (updatedClusterList.contains(devopsEnvironmentDTO.getClusterId())) {
                devopsEnvironmentValidator.checkEnvCanDisabled(environmentId);
            } else {
                if (!pipelineAppDeployService.baseQueryByEnvId(environmentId).isEmpty()) {
                    throw new CommonException("error.env.stop.pipeline.app.deploy.exist");
                }
            }
        }

        devopsEnvironmentDTO.setActive(active);
        baseUpdate(devopsEnvironmentDTO);
        return true;
    }

    @Override
    public DevopsEnvironmentUpdateVO query(Long environmentId) {
        return ConvertUtils.convertObject(baseQueryById(environmentId), DevopsEnvironmentUpdateVO.class);
    }

    @Override
    public DevopsEnvironmentInfoVO queryInfoById(Long projectId, Long environmentId) {
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
        OrganizationDTO organizationDTO = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());
        DevopsEnvironmentInfoDTO envInfo = devopsEnvironmentMapper.queryInfoById(environmentId);
        if (envInfo == null) {
            return null;
        }

        DevopsEnvironmentInfoVO vo = new DevopsEnvironmentInfoVO();
        BeanUtils.copyProperties(envInfo, vo);

        List<Long> upgradeClusterList = clusterConnectionHandler.getUpdatedClusterList();
        vo.setConnect(upgradeClusterList.contains(envInfo.getClusterId()));


        if (envInfo.getSagaSyncCommit() == null) {
            if (devopsEnvFileErrorMapper.queryErrorFileCountByEnvId(environmentId) > 0) {
                vo.setGitopsStatus(EnvironmentGitopsStatus.FAILED.getValue());
            } else {
                vo.setGitopsStatus(EnvironmentGitopsStatus.PROCESSING.getValue());
            }
            return vo;
        }

        if (envInfo.getSagaSyncCommit().equals(envInfo.getAgentSyncCommit()) &&
                envInfo.getSagaSyncCommit().equals(envInfo.getDevopsSyncCommit())) {
            vo.setGitopsStatus(EnvironmentGitopsStatus.FINISHED.getValue());
        } else {
            if (devopsEnvFileErrorMapper.queryErrorFileCountByEnvId(environmentId) > 0) {
                vo.setGitopsStatus(EnvironmentGitopsStatus.FAILED.getValue());
            } else {
                vo.setGitopsStatus(EnvironmentGitopsStatus.PROCESSING.getValue());
            }
        }

        gitlabUrl = gitlabUrl.endsWith("/") ? gitlabUrl.substring(0, gitlabUrl.length() - 1):gitlabUrl;
        vo.setGitlabUrl(String.format("%s/%s-%s-gitops/%s/",gitlabUrl,organizationDTO.getCode(),projectDTO.getCode(),envInfo.getCode()));
        return vo;
    }

    @Override
    public DevopsEnvResourceCountVO queryEnvResourceCount(Long environmentId) {
        return devopsEnvironmentMapper.queryEnvResourceCount(environmentId);
    }


    @Override
    public void checkEnv(DevopsEnvironmentDTO devopsEnvironmentDTO, UserAttrDTO userAttrDTO) {
        //校验用户是否有环境的权限
        devopsEnvUserPermissionService.checkEnvDeployPermission(TypeUtil.objToLong(GitUserNameUtil.getUserId()), devopsEnvironmentDTO.getId());

        //校验环境是否连接
        clusterConnectionHandler.checkEnvConnection(devopsEnvironmentDTO.getClusterId());


        //检验gitops库是否存在，校验操作人是否是有gitops库的权限
        gitlabGroupMemberService.checkEnvProject(devopsEnvironmentDTO, userAttrDTO);
    }


    @Override
    public DevopsEnvironmentUpdateVO update(DevopsEnvironmentUpdateVO devopsEnvironmentUpdateDTO, Long projectId) {
        DevopsEnvironmentDTO toUpdate = new DevopsEnvironmentDTO();
        toUpdate.setId(devopsEnvironmentUpdateDTO.getId());
        toUpdate.setName(devopsEnvironmentUpdateDTO.getName());
        toUpdate.setDescription(devopsEnvironmentUpdateDTO.getDescription());
        toUpdate.setObjectVersionNumber(devopsEnvironmentUpdateDTO.getObjectVersionNumber());

        devopsEnvGroupService.checkGroupIdInProject(devopsEnvironmentUpdateDTO.getDevopsEnvGroupId(), projectId);

        toUpdate.setDevopsEnvGroupId(devopsEnvironmentUpdateDTO.getDevopsEnvGroupId());
        return ConvertUtils.convertObject(baseUpdate(toUpdate), DevopsEnvironmentUpdateVO.class);
    }

    private void setEnvStatus(List<Long> upgradeEnvList, DevopsEnvironmentDTO t) {
        t.setConnected(upgradeEnvList.contains(t.getClusterId()));
    }


    @Override
    public void retryGitOps(Long envId) {
        DevopsEnvironmentDTO devopsEnvironmentDTO = baseQueryById(envId);
        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(GitUserNameUtil.getUserId().longValue());
        if (userAttrDTO == null) {
            throw new CommonException(ERROR_GITLAB_USER_SYNC_FAILED);
        }
        CommitDTO commitDO = gitlabServiceClientOperator.listCommits(devopsEnvironmentDTO.getGitlabEnvProjectId().intValue(), userAttrDTO.getGitlabUserId().intValue(), 1, 1).get(0);
        PushWebHookVO pushWebHookVO = new PushWebHookVO();
        pushWebHookVO.setCheckoutSha(commitDO.getId());
        pushWebHookVO.setUserId(userAttrDTO.getGitlabUserId().intValue());
        pushWebHookVO.setProjectId(devopsEnvironmentDTO.getGitlabEnvProjectId().intValue());
        CommitVO commitDTO = new CommitVO();
        commitDTO.setId(commitDO.getId());
        commitDTO.setTimestamp(commitDO.getTimestamp());
        pushWebHookVO.setCommits(Arrays.asList(commitDTO));

        //当环境总览第一阶段为空，第一阶段的commit不是最新commit, 第一阶段和第二阶段commit不一致时，可以重新触发gitOps
        if (devopsEnvironmentDTO.getSagaSyncCommit() == null) {
            devopsGitService.fileResourceSyncSaga(pushWebHookVO, devopsEnvironmentDTO.getToken());
        } else {
            DevopsEnvCommitDTO sagaSyncCommit = devopsEnvCommitService.baseQuery(devopsEnvironmentDTO.getSagaSyncCommit());
            if (!devopsEnvironmentDTO.getSagaSyncCommit().equals(devopsEnvironmentDTO.getDevopsSyncCommit()) || !sagaSyncCommit.getCommitSha().equals(commitDO.getId())) {
                devopsGitService.fileResourceSyncSaga(pushWebHookVO, devopsEnvironmentDTO.getToken());
            }
        }
    }

    @Override
    public void checkCode(Long projectId, Long clusterId, String code) {
        if (!CODE.matcher(code).matches()) {
            throw new CommonException("error.env.code.notMatch");
        }
        DevopsEnvironmentDTO devopsEnvironmentDTO = new DevopsEnvironmentDTO();
        DevopsClusterDTO devopsClusterDTO = devopsClusterService.baseQuery(clusterId);
        devopsEnvironmentDTO.setProjectId(projectId);
        devopsEnvironmentDTO.setClusterId(clusterId);
        devopsEnvironmentDTO.setCode(code);
        if (devopsClusterDTO.getNamespaces() != null) {
            JSONArray.parseArray(devopsClusterDTO.getNamespaces(), String.class).forEach(namespace -> {
                if (namespace.equals(code)) {
                    throw new CommonException(ERROR_CODE_EXIST);
                }
            });
        }
        baseCheckCode(devopsEnvironmentDTO);
    }

    @Override
    public List<DevopsEnvironmentRepVO> listByProjectId(Long projectId, Long appServiceId) {
        List<DevopsEnvironmentRepVO> devopsEnviromentRepDTOList = listByProjectIdAndActive(projectId, true);

        if (appServiceId == null) {
            return devopsEnviromentRepDTOList.stream().filter(t ->
                    appServiceInstanceService.baseListByEnvId(t.getId()).stream()
                            .anyMatch(applicationInstanceDTO ->
                                    applicationInstanceDTO.getStatus().equals(InstanceStatus.RUNNING.getStatus())))
                    .collect(Collectors.toList());
        } else {
            return devopsEnviromentRepDTOList.stream().filter(t ->
                    appServiceInstanceService.baseListByEnvId(t.getId()).stream()
                            .anyMatch(applicationInstanceDTO ->
                                    applicationInstanceDTO.getStatus().equals(InstanceStatus.RUNNING.getStatus())
                                            && applicationInstanceDTO.getAppServiceId().equals(appServiceId)))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void handleCreateEnvSaga(EnvGitlabProjectPayload gitlabProjectPayload) {
        DevopsProjectDTO gitlabGroupE = devopsProjectService.baseQueryByGitlabEnvGroupId(
                TypeUtil.objToInteger(gitlabProjectPayload.getGroupId()));
        DevopsEnvironmentDTO devopsEnvironmentDTO = baseQueryByClusterIdAndCode(gitlabProjectPayload.getClusterId(), gitlabProjectPayload.getPath());
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(gitlabGroupE.getIamProjectId());
        OrganizationDTO organizationDTO = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());

        GitlabProjectDTO gitlabProjectDO = gitlabServiceClientOperator.queryProjectByName(organizationDTO.getCode()
                + "-" + projectDTO.getCode() + "-gitops", devopsEnvironmentDTO.getCode(), gitlabProjectPayload.getUserId());
        if (gitlabProjectDO == null || gitlabProjectDO.getId() == null) {
            gitlabProjectDO = gitlabServiceClientOperator.createProject(
                    gitlabProjectPayload.getGroupId(),
                    gitlabProjectPayload.getPath(),
                    gitlabProjectPayload.getUserId(),
                    false);
        }
        devopsEnvironmentDTO.setGitlabEnvProjectId(TypeUtil.objToLong(gitlabProjectDO.getId()));
        if (gitlabServiceClientOperator.listDeployKey(gitlabProjectDO.getId(), gitlabProjectPayload.getUserId()).isEmpty()) {
            gitlabServiceClientOperator.createDeployKey(
                    gitlabProjectDO.getId(),
                    gitlabProjectPayload.getPath(),
                    devopsEnvironmentDTO.getEnvIdRsaPub(),
                    true,
                    gitlabProjectPayload.getUserId());
        }
        ProjectHookDTO projectHookDTO = ProjectHookDTO.allHook();
        projectHookDTO.setEnableSslVerification(true);
        projectHookDTO.setProjectId(gitlabProjectDO.getId());
        projectHookDTO.setToken(devopsEnvironmentDTO.getToken());
        String uri = !gatewayUrl.endsWith("/") ? gatewayUrl + "/" : gatewayUrl;
        uri += "devops/webhook/git_ops";
        projectHookDTO.setUrl(uri);
        List<ProjectHookDTO> projectHookDTOS = gitlabServiceClientOperator.listProjectHook(gitlabProjectDO.getId(),
                gitlabProjectPayload.getUserId());
        if (projectHookDTOS == null || projectHookDTOS.isEmpty()) {
            devopsEnvironmentDTO.setHookId(TypeUtil.objToLong(gitlabServiceClientOperator.createWebHook(
                    gitlabProjectDO.getId(), gitlabProjectPayload.getUserId(), projectHookDTO).getId()));
        } else {
            devopsEnvironmentDTO.setHookId(TypeUtil.objToLong(projectHookDTOS.get(0).getId()));
        }
        if (!gitlabServiceClientOperator.getFile(gitlabProjectDO.getId(), MASTER, README)) {
            gitlabServiceClientOperator.createFile(gitlabProjectDO.getId(),
                    README, README_CONTENT, "ADD README", gitlabProjectPayload.getUserId());
        }

        // 创建环境时初始化用户权限，分为gitlab权限和devops环境用户表权限
        gitlabProjectPayload.setGitlabProjectId(gitlabProjectDO.getId());
        initUserPermissionWhenCreatingEnv(gitlabProjectPayload, devopsEnvironmentDTO.getId(), projectDTO.getId());

        devopsEnvironmentDTO.setSynchro(true);
        baseUpdate(devopsEnvironmentDTO);
    }

    private void initUserPermissionWhenCreatingEnv(EnvGitlabProjectPayload gitlabProjectPayload, Long envId, Long
            projectId) {

        // 跳过权限检查，项目下所有成员自动分配权限
        if (Boolean.TRUE.equals(gitlabProjectPayload.getSkipCheckPermission())) {
            List<Long> iamUserIds = baseServiceClientOperator.getAllMemberIdsWithoutOwner(gitlabProjectPayload.getIamProjectId());
            userAttrService.baseListByUserIds(iamUserIds)
                    .stream()
                    .map(UserAttrDTO::getGitlabUserId)
                    .map(TypeUtil::objToInteger)
                    .forEach(userId -> updateGitlabMemberPermission(
                            gitlabProjectPayload.getGroupId(),
                            gitlabProjectPayload.getGitlabProjectId(),
                            userId)
                    );
            return;
        }

        // 需要分配权限的用户id
        List<Long> userIds = gitlabProjectPayload.getUserIds();
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        // 获取项目下所有项目成员
        PageInfo<UserVO> allProjectMemberPage = getMembersFromProject(new PageRequest(0, 0), projectId, "");

        // 所有项目成员中有权限的
        allProjectMemberPage.getList().stream().filter(e -> userIds.contains(e.getId())).forEach(e -> {
            Long userId = e.getId();
            String loginName = e.getLoginName();
            String realName = e.getRealName();
            UserAttrDTO userAttrDTO = userAttrService.baseQueryById(userId);

            updateGitlabMemberPermission(
                    gitlabProjectPayload.getGroupId(),
                    gitlabProjectPayload.getGitlabProjectId(),
                    userAttrDTO.getGitlabUserId().intValue());
            // 添加devops数据库记录
            devopsEnvUserPermissionService.baseCreate(new DevopsEnvUserPermissionDTO(loginName, userId, realName, envId, true));
        });
    }

    /**
     * update gitlab project member permission
     *
     * @param gitlabGroupId   gitlab group id
     * @param gitlabProjectId gitlab project id
     * @param gitlabUserId    gitlab user id
     */
    private void updateGitlabMemberPermission(Integer gitlabGroupId, Integer gitlabProjectId, Integer gitlabUserId) {
        // 删除组和用户之间的关系，如果存在
        MemberDTO memberDTO = gitlabServiceClientOperator.queryGroupMember(gitlabGroupId, TypeUtil.objToInteger(gitlabUserId));
        if (memberDTO != null) {
            gitlabServiceClientOperator.deleteGroupMember(gitlabGroupId, TypeUtil.objToInteger(gitlabUserId));
            UserAttrDTO userAttrDTO = userAttrService.baseQueryByGitlabUserId(TypeUtil.objToLong(gitlabUserId));
            List<Long> gitlabProjectIds = devopsEnvironmentMapper.listGitlabProjectIdByEnvPermission(TypeUtil.objToLong(gitlabGroupId), userAttrDTO.getIamUserId());
            if (gitlabProjectIds != null && !gitlabProjectIds.isEmpty()) {
                gitlabProjectIds.forEach(aLong -> {
                    MemberDTO gitlabMemberDTO = gitlabServiceClientOperator.getProjectMember(gitlabProjectId, TypeUtil.objToInteger(aLong));
                    if (gitlabMemberDTO == null || gitlabMemberDTO.getId() == null) {
                        gitlabServiceClientOperator.createProjectMember(gitlabProjectId, new MemberDTO(TypeUtil.objToInteger(aLong), 40, ""));
                    }
                });
            }
        }
        // 当项目不存在用户权限纪录时(防止失败重试时报成员已存在异常)，添加gitlab用户权限
        MemberDTO gitlabMemberDTO = gitlabServiceClientOperator.getProjectMember(gitlabProjectId, TypeUtil.objToInteger(gitlabUserId));
        if (gitlabMemberDTO == null || gitlabMemberDTO.getId() == null) {
            gitlabServiceClientOperator.createProjectMember(gitlabProjectId, new MemberDTO(TypeUtil.objToInteger(gitlabUserId), 40, ""));
        }
    }

    @Override
    public EnvSyncStatusVO queryEnvSyncStatus(Long projectId, Long envId) {
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
        OrganizationDTO organizationDTO = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());
        DevopsEnvironmentDTO devopsEnvironmentDTO = baseQueryById(envId);
        if (devopsEnvironmentDTO == null) {
            return null;
        }
        EnvSyncStatusVO envSyncStatusDTO = new EnvSyncStatusVO();
        if (devopsEnvironmentDTO.getAgentSyncCommit() != null) {
            envSyncStatusDTO.setAgentSyncCommit(devopsEnvCommitService
                    .baseQuery(devopsEnvironmentDTO.getAgentSyncCommit()).getCommitSha());
        }
        if (devopsEnvironmentDTO.getDevopsSyncCommit() != null) {
            envSyncStatusDTO.setDevopsSyncCommit(devopsEnvCommitService
                    .baseQuery(devopsEnvironmentDTO.getDevopsSyncCommit())
                    .getCommitSha());
        }
        if (devopsEnvironmentDTO.getSagaSyncCommit() != null) {
            envSyncStatusDTO.setSagaSyncCommit(devopsEnvCommitService
                    .baseQuery(devopsEnvironmentDTO.getSagaSyncCommit()).getCommitSha());
        }

        gitlabUrl = gitlabUrl.endsWith("/") ? gitlabUrl.substring(0, gitlabUrl.length() - 1) : gitlabUrl;
        envSyncStatusDTO.setCommitUrl(String.format("%s/%s-%s-gitops/%s/commit/",
                gitlabUrl, organizationDTO.getCode(), projectDTO.getCode(),
                devopsEnvironmentDTO.getCode()));
        return envSyncStatusDTO;
    }

    @Override
    public PageInfo<DevopsUserPermissionVO> pageUserPermissionByEnvId(Long projectId, PageRequest
            pageRequest, String params, Long envId) {
        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentMapper.selectByPrimaryKey(envId);

        RoleAssignmentSearchVO roleAssignmentSearchVO = new RoleAssignmentSearchVO();
        roleAssignmentSearchVO.setEnabled(true);
        Map<String, Object> searchParamMap = null;
        List<String> paramList = null;
        // 处理搜索参数
        if (!StringUtils.isEmpty(params)) {
            Map maps = gson.fromJson(params, Map.class);
            searchParamMap = TypeUtil.cast(maps.get(TypeUtil.SEARCH_PARAM));
            paramList = TypeUtil.cast(maps.get(TypeUtil.PARAMS));
            roleAssignmentSearchVO.setParam(paramList == null ? null : paramList.toArray(new String[0]));
            if (searchParamMap != null) {
                if (searchParamMap.get(LOGIN_NAME) != null) {
                    String loginName = TypeUtil.objToString(searchParamMap.get(LOGIN_NAME));
                    roleAssignmentSearchVO.setLoginName(loginName);
                }
                if (searchParamMap.get(REAL_NAME) != null) {
                    String realName = TypeUtil.objToString(searchParamMap.get(REAL_NAME));
                    roleAssignmentSearchVO.setRealName(realName);
                }
            }
        }

        // 根据搜索参数查询所有的项目所有者
        Long ownerId = baseServiceClientOperator.queryRoleIdByCode(PROJECT_OWNER);
        PageInfo<IamUserDTO> projectOwners = baseServiceClientOperator.pagingQueryUsersByRoleIdOnProjectLevel(
                new PageRequest(0, 0), roleAssignmentSearchVO, ownerId, projectId, false);

        List<Long> ownerIds = projectOwners
                .getList()
                .stream()
                .map(IamUserDTO::getId)
                .collect(Collectors.toList());
        List<DevopsUserPermissionVO> members;
        if (!devopsEnvironmentDTO.getSkipCheckPermission()) {
            // 根据搜索参数查询数据库中所有的环境权限分配数据
            List<DevopsEnvUserPermissionDTO> permissions = devopsEnvUserPermissionMapper.listUserEnvPermissionByOption(envId, searchParamMap, paramList);
            members = permissions
                    .stream()
                    .filter(p -> !ownerIds.contains(p.getIamUserId()))
                    .map(p -> {
                        DevopsUserPermissionVO permissionVO = new DevopsUserPermissionVO();
                        BeanUtils.copyProperties(p,permissionVO);
                        IamUserDTO iamUserDTO = baseServiceClientOperator.queryUserByUserId(p.getIamUserId());
                        if(!iamUserDTO.getLdap()){
                            permissionVO.setLoginName(iamUserDTO.getEmail());
                        }
                        return permissionVO;
                    })
                    .peek(p -> p.setRole(MEMBER))
                    .sorted(Comparator.comparing(DevopsUserPermissionVO::getCreationDate).reversed())
                    .collect(Collectors.toList());
        } else {
            // 搜索所有的项目成员，并过滤其中的项目所有者
            Long memberRoleId = baseServiceClientOperator.queryRoleIdByCode(PROJECT_MEMBER);

            members = baseServiceClientOperator.pagingQueryUsersByRoleIdOnProjectLevel(
                    new PageRequest(0, 0), roleAssignmentSearchVO, memberRoleId, projectId, false)
                    .getList()
                    .stream()
                    .filter(u -> !ownerIds.contains(u.getId()))
                    .map(iamUser -> new DevopsUserPermissionVO(iamUser.getId(), iamUser.getLdap()?iamUser.getLoginName():iamUser.getEmail(), iamUser.getRealName(), devopsEnvironmentDTO.getCreationDate()))
                    .peek(p -> p.setRole(MEMBER))
                    .collect(Collectors.toList());
        }

        // 项目成员加上项目所有者
        List<DevopsUserPermissionVO> owners = projectOwners.getList()
                .stream().map(iamUser -> {
                    DevopsUserPermissionVO devopsUserPermissionVO = new DevopsUserPermissionVO();
                    if (iamUser.getLdap()) {
                        devopsUserPermissionVO = new DevopsUserPermissionVO(iamUser.getId(), iamUser.getLoginName(), iamUser.getRealName(), devopsEnvironmentDTO.getCreationDate());
                    } else {
                        devopsUserPermissionVO = new DevopsUserPermissionVO(iamUser.getId(), iamUser.getEmail(), iamUser.getRealName(), devopsEnvironmentDTO.getCreationDate());
                    }
                    return devopsUserPermissionVO;
                })
                .peek(p -> p.setRole(OWNER))
                .collect(Collectors.toList());
        members.addAll(owners);

        // 根据结果手动设置page的相关属性
        return PageInfoUtil.createPageFromList(members, pageRequest);
    }

    @Override
    public List<DevopsEnvUserVO> listNonRelatedMembers(Long projectId, Long envId, String params) {
        RoleAssignmentSearchVO roleAssignmentSearchVO = new RoleAssignmentSearchVO();
        roleAssignmentSearchVO.setEnabled(true);
        // 处理搜索参数
        if (!StringUtils.isEmpty(params)) {
            Map maps = gson.fromJson(params, Map.class);
            Map<String, Object> searchParamMap = TypeUtil.cast(maps.get(TypeUtil.SEARCH_PARAM));
            List<String> paramList = TypeUtil.cast(maps.get(TypeUtil.PARAMS));

            roleAssignmentSearchVO.setParam(paramList == null ? null : paramList.toArray(new String[0]));
            if (searchParamMap.get(LOGIN_NAME) != null) {
                String loginName = TypeUtil.objToString(searchParamMap.get(LOGIN_NAME));
                roleAssignmentSearchVO.setLoginName(loginName);
            }

            if (searchParamMap.get(REAL_NAME) != null) {
                String realName = TypeUtil.objToString(searchParamMap.get(REAL_NAME));
                roleAssignmentSearchVO.setRealName(realName);
            }
        }

        // 根据参数搜索所有的项目成员
        Long memberRoleId = baseServiceClientOperator.queryRoleIdByCode(PROJECT_MEMBER);
        PageInfo<IamUserDTO> allProjectMembers = baseServiceClientOperator.pagingQueryUsersByRoleIdOnProjectLevel(
                new PageRequest(0, 0), roleAssignmentSearchVO, memberRoleId, projectId, false);
        if (allProjectMembers.getList().isEmpty()) {
            return Collections.emptyList();
        }

        // 获取项目下所有的项目所有者（带上搜索参数搜索可以获得更精确的结果）
        Long ownerId = baseServiceClientOperator.queryRoleIdByCode(PROJECT_OWNER);
        List<Long> allProjectOwnerIds = baseServiceClientOperator.pagingQueryUsersByRoleIdOnProjectLevel(
                new PageRequest(0, 0), roleAssignmentSearchVO, ownerId, projectId, false)
                .getList()
                .stream()
                .map(IamUserDTO::getId)
                .collect(Collectors.toList());

        // 数据库中已被分配权限的
        List<Long> assigned = devopsEnvUserPermissionMapper.listUserIdsByEnvId(envId);

        // 过滤项目成员中的项目所有者和已被分配权限的
        List<IamUserDTO> members = allProjectMembers.getList()
                .stream()
                .filter(member -> !allProjectOwnerIds.contains(member.getId()))
                .filter(member -> !assigned.contains(member.getId()))
                .collect(Collectors.toList());

        return ConvertUtils.convertList(members,
                iamUserDTO -> new DevopsEnvUserVO(iamUserDTO.getId(), iamUserDTO.getLdap()?iamUserDTO.getLoginName():iamUserDTO.getEmail(), iamUserDTO.getRealName()));
    }

    @Override
    public void deletePermissionOfUser(Long projectId, Long envId, Long userId) {
        if (envId == null || userId == null) {
            return;
        }

        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(userId);

        if (userAttrDTO == null) {
            return;
        }

        DevopsEnvironmentDTO environmentDTO = devopsEnvironmentMapper.selectByPrimaryKey(envId);

        if (environmentDTO == null) {
            return;
        }

        if (userAttrDTO.getGitlabUserId() == null) {
            throw new CommonException(ERROR_GITLAB_USER_SYNC_FAILED);
        }

        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);

        if (baseServiceClientOperator.isProjectOwner(userAttrDTO.getIamUserId(), projectDTO)) {
            throw new CommonException("error.delete.permission.of.project.owner");
        }

        // 删除数据库中的纪录
        DevopsEnvUserPermissionDTO devopsEnvUserPermissionDTO = new DevopsEnvUserPermissionDTO();
        devopsEnvUserPermissionDTO.setEnvId(envId);
        devopsEnvUserPermissionDTO.setIamUserId(userId);
        devopsEnvUserPermissionMapper.delete(devopsEnvUserPermissionDTO);


        // 删除GitLab对应的项目的权限
        DevopsEnvUserPayload userPayload = new DevopsEnvUserPayload();
        userPayload.setIamProjectId(projectId);
        userPayload.setGitlabProjectId(environmentDTO.getGitlabEnvProjectId().intValue());
        userPayload.setEnvId(envId);
        userPayload.setAddGitlabUserIds(Collections.emptyList());
        // 待删除的用户
        userPayload.setDeleteGitlabUserIds(Collections.singletonList(userAttrDTO.getGitlabUserId().intValue()));
        userPayload.setOption(3);

        // 发送saga进行相应的gitlab侧的数据处理
        producer.apply(
                StartSagaBuilder.newBuilder()
                        .withSourceId(projectId)
                        .withLevel(ResourceLevel.PROJECT)
                        .withRefType("env")
                        .withRefId(String.valueOf(envId))
                        .withJson(JSONObject.toJSONString(userPayload))
                        .withSagaCode(SagaTopicCodeConstants.DEVOPS_UPDATE_ENV_PERMISSION),
                builder -> {
                });
    }

    @Override
    public List<DevopsEnvUserVO> listAllUserPermission(Long envId) {
        return ConvertUtils.convertList(devopsEnvUserPermissionService.baseListByEnvId(envId), DevopsEnvUserVO.class);
    }


    @Saga(code = SagaTopicCodeConstants.DEVOPS_UPDATE_ENV_PERMISSION, description = "更新环境的权限")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateEnvUserPermission(DevopsEnvPermissionUpdateVO devopsEnvPermissionUpdateVO) {
        DevopsEnvironmentDTO preEnvironmentDTO = devopsEnvironmentMapper.selectByPrimaryKey(devopsEnvPermissionUpdateVO.getEnvId());

        DevopsEnvUserPayload userPayload = new DevopsEnvUserPayload();
        userPayload.setEnvId(preEnvironmentDTO.getId());
        userPayload.setGitlabProjectId(preEnvironmentDTO.getGitlabEnvProjectId().intValue());
        userPayload.setIamProjectId(preEnvironmentDTO.getProjectId());
        userPayload.setIamUserIds(devopsEnvPermissionUpdateVO.getUserIds());

        // 判断更新的情况
        if (preEnvironmentDTO.getSkipCheckPermission()) {
            if (devopsEnvPermissionUpdateVO.getSkipCheckPermission()) {
                return;
            } else {
                // 添加权限
                List<IamUserDTO> addIamUsers = baseServiceClientOperator.listUsersByIds(devopsEnvPermissionUpdateVO.getUserIds());
                addIamUsers.forEach(e -> devopsEnvUserPermissionService.baseCreate(new DevopsEnvUserPermissionDTO(e.getLoginName(), e.getId(), e.getRealName(), preEnvironmentDTO.getId(), true)));

                userPayload.setOption(1);

                // 更新字段
                preEnvironmentDTO.setSkipCheckPermission(devopsEnvPermissionUpdateVO.getSkipCheckPermission());
                preEnvironmentDTO.setObjectVersionNumber(devopsEnvPermissionUpdateVO.getObjectVersionNumber());
                devopsEnvironmentMapper.updateByPrimaryKeySelective(preEnvironmentDTO);
            }
        } else {
            if (devopsEnvPermissionUpdateVO.getSkipCheckPermission()) {
                // 删除原先所有的分配情况
                devopsEnvUserPermissionService.deleteByEnvId(preEnvironmentDTO.getId());
                userPayload.setOption(2);

                // 更新字段
                preEnvironmentDTO.setSkipCheckPermission(devopsEnvPermissionUpdateVO.getSkipCheckPermission());
                preEnvironmentDTO.setObjectVersionNumber(devopsEnvPermissionUpdateVO.getObjectVersionNumber());
                devopsEnvironmentMapper.updateByPrimaryKeySelective(preEnvironmentDTO);
            } else {
                // 待添加的用户
                List<Long> addIamUserIds = devopsEnvPermissionUpdateVO.getUserIds();

                List<Integer> addGitlabUserIds = userAttrService.baseListByUserIds(addIamUserIds)
                        .stream()
                        .map(UserAttrDTO::getGitlabUserId)
                        .map(TypeUtil::objToInteger)
                        .collect(Collectors.toList());
                userPayload.setAddGitlabUserIds(addGitlabUserIds);
                userPayload.setDeleteGitlabUserIds(Collections.emptyList());

                devopsEnvUserPermissionService.baseUpdate(devopsEnvPermissionUpdateVO.getEnvId(), addIamUserIds, Collections.emptyList());

                userPayload.setOption(3);
            }
        }

        // 发送saga进行相应的gitlab侧的数据处理
        producer.apply(
                StartSagaBuilder.newBuilder()
                        .withSourceId(preEnvironmentDTO.getProjectId())
                        .withLevel(ResourceLevel.PROJECT)
                        .withPayloadAndSerialize(userPayload)
                        .withSagaCode(SagaTopicCodeConstants.DEVOPS_UPDATE_ENV_PERMISSION),
                builder -> {
                });
    }


    private PageInfo<UserVO> getMembersFromProject(PageRequest pageRequest, Long projectId, String searchParams) {
        RoleAssignmentSearchVO roleAssignmentSearchVO = new RoleAssignmentSearchVO();
        if (!StringUtils.isEmpty(searchParams)) {
            Map maps = gson.fromJson(searchParams, Map.class);
            Map<String, Object> searchParamMap = TypeUtil.cast(maps.get(TypeUtil.SEARCH_PARAM));
            List<String> param = TypeUtil.cast(maps.get(TypeUtil.PARAMS));
            roleAssignmentSearchVO.setParam(param == null ? null : param.toArray(new String[0]));
            if (searchParamMap.get(LOGIN_NAME) != null) {
                String loginName = TypeUtil.objToString(searchParamMap.get(LOGIN_NAME));
                String subLogin = loginName.substring(1, loginName.length() - 1);
                roleAssignmentSearchVO.setLoginName(subLogin);
            }
            if (searchParamMap.get(REAL_NAME) != null) {
                String realName = TypeUtil.objToString(searchParamMap.get(REAL_NAME));
                String subReal = realName.substring(1, realName.length() - 1);
                roleAssignmentSearchVO.setRealName(subReal);
            }
        }
        // 获取项目所有者角色id和数量
        Long ownerId = baseServiceClientOperator.queryRoleIdByCode(PROJECT_OWNER);
        // 获取项目成员id
        Long memberId = baseServiceClientOperator.queryRoleIdByCode(PROJECT_MEMBER);
        // 所有项目成员，可能还带有项目所有者的角色
        PageInfo<IamUserDTO> allMemberWithOtherUsersPage = baseServiceClientOperator
                .pagingQueryUsersByRoleIdOnProjectLevel(new PageRequest(0, 0), roleAssignmentSearchVO,
                        memberId, projectId, false);
        // 所有项目所有者
        PageInfo<IamUserDTO> allOwnerUsersPage = baseServiceClientOperator
                .pagingQueryUsersByRoleIdOnProjectLevel(new PageRequest(0, 0), roleAssignmentSearchVO,
                        ownerId, projectId, false);
        //合并项目所有者和项目成员
        Set<IamUserDTO> iamUserDTOS = new HashSet<>(allMemberWithOtherUsersPage.getList());
        iamUserDTOS.addAll(allOwnerUsersPage.getList());
        List<IamUserDTO> returnUserDTOList;

        //没有项目所有者
        if (allOwnerUsersPage.getList().isEmpty()) {
            return ConvertUtils.convertPage(allMemberWithOtherUsersPage, UserVO.class);
        } else {
            returnUserDTOList = iamUserDTOS.stream()
                    .peek(e -> {
                        if (!allOwnerUsersPage.getList().contains(e)) {
                            e.setProjectOwner(false);
                        } else {
                            e.setProjectOwner(true);
                        }
                    }).collect(Collectors.toList());
        }
        allMemberWithOtherUsersPage.setPageSize(pageRequest.getSize());
        allMemberWithOtherUsersPage.setTotal(returnUserDTOList.size());
        allMemberWithOtherUsersPage.setPageNum(pageRequest.getPage());
        if (returnUserDTOList.size() < pageRequest.getSize() * pageRequest.getPage()) {
            allMemberWithOtherUsersPage.setSize(TypeUtil.objToInt(returnUserDTOList.size()) - (pageRequest.getSize() * (pageRequest.getPage() - 1)));
            allMemberWithOtherUsersPage.setList(returnUserDTOList);
        } else {
            allMemberWithOtherUsersPage.setSize(pageRequest.getSize());
            int fromIndex = pageRequest.getSize() * (pageRequest.getPage() - 1);
            int toIndex = (pageRequest.getSize() * pageRequest.getPage()) > returnUserDTOList.size() ? returnUserDTOList.size() : pageRequest.getSize() * pageRequest.getPage();
            allMemberWithOtherUsersPage.setList(returnUserDTOList.subList(fromIndex, toIndex));
        }

        return ConvertUtils.convertPage(allMemberWithOtherUsersPage, UserVO.class);
    }

    private void setPermission(DevopsEnvironmentDTO devopsEnvironmentDTO, List<Long> permissionEnvIds,
                               Boolean isProjectOwner) {
        if (devopsEnvironmentDTO.getSkipCheckPermission()) {
            devopsEnvironmentDTO.setPermission(true);
        } else if (permissionEnvIds.contains(devopsEnvironmentDTO.getId()) || isProjectOwner) {
            devopsEnvironmentDTO.setPermission(true);
        } else {
            devopsEnvironmentDTO.setPermission(false);
        }
    }
    @Saga(code = SagaTopicCodeConstants.DEVOPS_DELETE_ENV,
            description = "devops删除停用和失败的环境")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDeactivatedOrFailedEnvironment(Long projectId,Long envId) {
        DevopsEnvironmentDTO devopsEnvironmentDTO = baseQueryById(envId);

        if (devopsEnvironmentDTO == null) {
            return;
        }

        if (!Boolean.FALSE.equals(devopsEnvironmentDTO.getActive()) && !Boolean.TRUE.equals(devopsEnvironmentDTO.getFailed())) {
            throw new CommonException("error.env.delete");
        }
        devopsEnvironmentDTO.setSynchro(Boolean.FALSE);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("envId",envId);
        devopsEnvironmentMapper.updateByPrimaryKeySelective(devopsEnvironmentDTO);
        producer.apply(
                StartSagaBuilder
                        .newBuilder()
                        .withLevel(ResourceLevel.PROJECT)
                        .withRefType("")
                        .withJson(jsonObject.toString())
                        .withSourceId(projectId)
                        .withSagaCode(SagaTopicCodeConstants.DEVOPS_DELETE_ENV),
                builder -> {});

    }
    @Override
    public void deleteEnvSaga(Long envId){
        DevopsEnvironmentDTO devopsEnvironmentDTO = baseQueryById(envId);
        // 删除对应的环境-应用服务关联关系
        DevopsEnvAppServiceDTO deleteCondition = new DevopsEnvAppServiceDTO();
        deleteCondition.setEnvId(envId);
        devopsEnvAppServiceMapper.delete(deleteCondition);

        // 删除环境对应的实例
        appServiceInstanceService.baseListByEnvId(envId).forEach(instanceE ->
                devopsEnvCommandService.baseListByObject(HelmObjectKind.INSTANCE.toValue(), instanceE.getId()).forEach(t -> devopsEnvCommandService.baseDeleteByEnvCommandId(t)));
        appServiceInstanceService.deleteByEnvId(envId);
        // 删除环境对应的域名、域名路径
        devopsIngressService.baseListByEnvId(envId).forEach(ingressE ->
                devopsEnvCommandService.baseListByObject(HelmObjectKind.INGRESS.toValue(), ingressE.getId()).forEach(t -> devopsEnvCommandService.baseDeleteByEnvCommandId(t)));
        devopsIngressService.deleteIngressAndIngressPathByEnvId(envId);
        // 删除环境对应的网络和网络实例
        devopsServiceService.baseListByEnvId(envId).forEach(serviceE ->
                devopsEnvCommandService.baseListByObject(HelmObjectKind.SERVICE.toValue(), serviceE.getId()).forEach(t -> devopsEnvCommandService.baseDeleteByEnvCommandId(t)));
        devopsServiceService.baseDeleteServiceAndInstanceByEnvId(envId);
        // 删除实例对应的部署纪录
        devopsDeployRecordService.deleteManualRecordByEnv(envId);

        // 删除环境
        baseDeleteById(envId);
        // 删除gitlab库, 删除之前查询是否存在
        if (devopsEnvironmentDTO.getGitlabEnvProjectId() != null) {
            Integer gitlabProjectId = TypeUtil.objToInt(devopsEnvironmentDTO.getGitlabEnvProjectId());
            GitlabProjectDTO gitlabProjectDO = gitlabServiceClientOperator.queryProjectById(gitlabProjectId);
            if (gitlabProjectDO != null && gitlabProjectDO.getId() != null) {
                UserAttrDTO userAttrDTO = userAttrService.baseQueryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));
                Integer gitlabUserId = TypeUtil.objToInt(userAttrDTO.getGitlabUserId());
                gitlabServiceClientOperator.deleteProjectById(gitlabProjectId, gitlabUserId);
            }
        }

        //更新集群关联的namespaces数据
        DevopsClusterDTO devopsClusterDTO = devopsClusterService.baseQuery(devopsEnvironmentDTO.getClusterId());
        if (devopsClusterDTO.getNamespaces() != null) {
            List<String> namespaces = JSONArray.parseArray(devopsClusterDTO.getNamespaces(), String.class);
            namespaces.remove(devopsEnvironmentDTO.getCode());
            devopsClusterDTO.setNamespaces((JSONArray.toJSONString(namespaces)));
            devopsClusterService.baseUpdate(devopsClusterDTO);
        }


        // 删除环境命名空间
        if (devopsEnvironmentDTO.getClusterId() != null) {
            agentCommandService.deleteEnv(envId, devopsEnvironmentDTO.getCode(), devopsEnvironmentDTO.getClusterId());
        }
    }
    @Override
    public List<DevopsClusterRepVO> listDevopsCluster(Long projectId) {
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
        List<DevopsClusterRepVO> devopsClusterRepVOS = ConvertUtils.convertList(devopsClusterService.baseListByProjectId(projectId, projectDTO.getOrganizationId()), DevopsClusterRepVO.class);
        List<Long> connectedClusterList = clusterConnectionHandler.getConnectedClusterList();
        List<Long> upgradeClusterList = clusterConnectionHandler.getUpdatedClusterList();
        devopsClusterRepVOS.forEach(t -> {
            if (connectedClusterList.contains(t.getId()) && upgradeClusterList.contains(t.getId())) {
                t.setConnect(true);
            } else {
                t.setConnect(false);
            }
        });
        return devopsClusterRepVOS;
    }

    @Override
    @Saga(code = SagaTopicCodeConstants.DEVOPS_SET_ENV_ERR,
            description = "devops创建环境失败(devops set env status create err)", inputSchemaClass = GitlabProjectPayload.class)
    public void setEnvErrStatus(String data, Long projectId) {
        producer.applyAndReturn(
                StartSagaBuilder
                        .newBuilder()
                        .withLevel(ResourceLevel.PROJECT)
                        .withRefType("")
                        .withSagaCode(SagaTopicCodeConstants.DEVOPS_SET_ENV_ERR),
                builder -> builder
                        .withJson(data)
                        .withRefId("")
                        .withSourceId(projectId));

    }

    @Override
    public DevopsEnvironmentRepVO queryByCode(Long clusterId, String code) {
        return ConvertUtils.convertObject(baseQueryByProjectIdAndCode(clusterId, code), DevopsEnvironmentRepVO.class);
    }


    @Override
    public DevopsEnvironmentDTO baseCreate(DevopsEnvironmentDTO devopsEnvironmentDTO) {
        if (devopsEnvironmentMapper.insert(devopsEnvironmentDTO) != 1) {
            throw new CommonException("error.environment.create");
        }
        return devopsEnvironmentDTO;
    }

    @Override
    public DevopsEnvironmentDTO baseQueryById(Long id) {
        return devopsEnvironmentMapper.selectByPrimaryKey(id);
    }

    @Override
    public DevopsEnvironmentDTO baseUpdate(DevopsEnvironmentDTO devopsEnvironmentDTO) {
        if (devopsEnvironmentDTO.getDevopsEnvGroupId() == null) {
            devopsEnvironmentMapper.updateDevopsEnvGroupId(devopsEnvironmentDTO.getId());
        }
        devopsEnvironmentDTO.setObjectVersionNumber(devopsEnvironmentMapper.selectByPrimaryKey(
                devopsEnvironmentDTO.getId()).getObjectVersionNumber());
        if (devopsEnvironmentMapper.updateByPrimaryKeySelective(devopsEnvironmentDTO) != 1) {
            throw new CommonException("error.environment.update");
        }
        return devopsEnvironmentDTO;
    }

    @Override
    public void baseCheckCode(DevopsEnvironmentDTO devopsEnvironmentDTO) {
        DevopsEnvironmentDTO environmentDTO = new DevopsEnvironmentDTO();
        environmentDTO.setClusterId(devopsEnvironmentDTO.getClusterId());
        environmentDTO.setCode(devopsEnvironmentDTO.getCode());
        if (!devopsEnvironmentMapper.select(environmentDTO).isEmpty()) {
            throw new CommonException(ERROR_CODE_EXIST);
        }
        environmentDTO.setClusterId(null);
        environmentDTO.setProjectId(devopsEnvironmentDTO.getProjectId());
        if (!devopsEnvironmentMapper.select(environmentDTO).isEmpty()) {
            throw new CommonException(ERROR_CODE_EXIST);
        }
    }

    @Override
    public List<DevopsEnvironmentDTO> baseListByProjectId(Long projectId) {
        DevopsEnvironmentDTO environmentDTO = new DevopsEnvironmentDTO();
        environmentDTO.setProjectId(projectId);
        return devopsEnvironmentMapper.select(environmentDTO);
    }

    @Override
    public List<DevopsEnvironmentDTO> baseListByProjectIdAndActive(Long projectId, Boolean active) {
        DevopsEnvironmentDTO devopsEnvironmentDTO = new DevopsEnvironmentDTO();
        devopsEnvironmentDTO.setProjectId(projectId);
        devopsEnvironmentDTO.setActive(active);
        return devopsEnvironmentMapper.select(devopsEnvironmentDTO);
    }

    @Override
    public DevopsEnvironmentDTO baseQueryByClusterIdAndCode(Long clusterId, String code) {
        DevopsEnvironmentDTO devopsEnvironmentDTO = new DevopsEnvironmentDTO();
        devopsEnvironmentDTO.setClusterId(clusterId);
        devopsEnvironmentDTO.setCode(code);
        return devopsEnvironmentMapper.selectOne(devopsEnvironmentDTO);
    }

    @Override
    public DevopsEnvironmentDTO baseQueryByProjectIdAndCode(Long projectId, String code) {
        DevopsEnvironmentDTO devopsEnvironmentDTO = new DevopsEnvironmentDTO();
        devopsEnvironmentDTO.setProjectId(projectId);
        devopsEnvironmentDTO.setCode(code);
        return devopsEnvironmentMapper.selectOne(devopsEnvironmentDTO);
    }

    @Override
    public DevopsEnvironmentDTO baseQueryByToken(String token) {
        return devopsEnvironmentMapper.queryByToken(token);
    }

    @Override
    public void baseUpdateSagaSyncEnvCommit(DevopsEnvironmentDTO devopsEnvironmentDTO) {
        devopsEnvironmentMapper.updateSagaSyncEnvCommit(devopsEnvironmentDTO.getId(),
                devopsEnvironmentDTO.getSagaSyncCommit());
    }

    @Override
    public void baseUpdateDevopsSyncEnvCommit(DevopsEnvironmentDTO devopsEnvironmentDTO) {
        devopsEnvironmentMapper.updateDevopsSyncEnvCommit(devopsEnvironmentDTO.getId(),
                devopsEnvironmentDTO.getDevopsSyncCommit());
    }

    @Override
    public void baseUpdateAgentSyncEnvCommit(DevopsEnvironmentDTO devopsEnvironmentDTO) {
        devopsEnvironmentMapper.updateAgentSyncEnvCommit(devopsEnvironmentDTO.getId(),
                devopsEnvironmentDTO.getAgentSyncCommit());
    }


    @Override
    public void baseDeleteById(Long id) {
        devopsEnvironmentMapper.deleteByPrimaryKey(id);
    }

    @Override
    public List<DevopsEnvironmentDTO> baseListByClusterId(Long clusterId) {
        DevopsEnvironmentDTO devopsEnvironmentDO = new DevopsEnvironmentDTO();
        devopsEnvironmentDO.setClusterId(clusterId);
        return devopsEnvironmentMapper.select(devopsEnvironmentDO);
    }

    @Override
    public List<DevopsEnvironmentDTO> baseListByIds(List<Long> envIds) {
        return devopsEnvironmentMapper.listByIds(envIds);
    }

    @Override
    public Boolean deleteCheck(Long projectId, Long envId) {
        //pipeLineAppDeploy为空
        boolean pipeLineAppDeployEmpty = pipelineAppDeployService.baseQueryByEnvId(envId).isEmpty();

        DevopsEnvResourceCountVO devopsEnvResourceCountVO = devopsEnvironmentMapper.queryEnvResourceCount(envId);

        return devopsEnvResourceCountVO.getRunningInstanceCount() == 0
                && devopsEnvResourceCountVO.getServiceCount() == 0
                && devopsEnvResourceCountVO.getIngressCount() == 0
                && devopsEnvResourceCountVO.getCertificationCount() == 0
                && devopsEnvResourceCountVO.getSecretCount() == 0
                && devopsEnvResourceCountVO.getConfigMapCount() == 0
                && pipeLineAppDeployEmpty;
    }

    @Override
    public Boolean checkExist(Long projectId, Long envId, Long objectId, String type) {
        // type为null表示查询环境是否存在
        if (type == null) {
            return devopsEnvironmentMapper.selectByPrimaryKey(envId) != null;
        }
        // type为app表示查询应用服务是否存在
        if ("app".equals(type)) {
            DevopsEnvAppServiceDTO devopsEnvAppServiceDTO = new DevopsEnvAppServiceDTO();
            devopsEnvAppServiceDTO.setEnvId(envId);
            devopsEnvAppServiceDTO.setAppServiceId(objectId);
            return devopsEnvAppServiceMapper.selectOne(devopsEnvAppServiceDTO) != null;
        }
        boolean check = false;
        ObjectType objectType = ObjectType.valueOf(type.toUpperCase());
        switch (objectType) {
            case INSTANCE:
                if (appServiceInstanceMapper.countNonDeletedInstancesWithEnv(envId, objectId) == 1) {
                    check = true;
                }
                break;
            case SERVICE:
                //状态不为删除时返回true,即选出为删除状态的实例数量为0返回true
                if (devopsServiceMapper.countNonDeletedServiceWithEnv(envId, objectId) == 1) {
                    check = true;
                }
                break;
            case CONFIGMAP:
                DevopsConfigMapDTO devopsConfigMapDTO = new DevopsConfigMapDTO();
                devopsConfigMapDTO.setEnvId(envId);
                devopsConfigMapDTO.setId(objectId);
                if (devopsConfigMapMapper.selectCount(devopsConfigMapDTO) == 1) {
                    check = true;
                }
                break;
            case INGRESS:
                DevopsIngressDTO devopsIngressDTO = new DevopsIngressDTO();
                devopsIngressDTO.setEnvId(envId);
                devopsIngressDTO.setId(objectId);
                if (devopsIngressMapper.selectCount(devopsIngressDTO) == 1) {
                    check = true;
                }
                break;
            case CERTIFICATE:
                CertificationDTO certificationDTO = new CertificationDTO();
                certificationDTO.setEnvId(envId);
                certificationDTO.setId(objectId);
                if (devopsCertificationMapper.selectCount(certificationDTO) == 1) {
                    check = true;
                }
                break;
            case SECRET:
                DevopsSecretDTO devopsSecretDTO = new DevopsSecretDTO();
                devopsSecretDTO.setEnvId(envId);
                devopsSecretDTO.setId(objectId);
                if (devopsSecretMapper.selectCount(devopsSecretDTO) == 1) {
                    check = true;
                }
                break;
            case CUSTOM:
                DevopsCustomizeResourceDTO devopsCustomizeResourceDTO = new DevopsCustomizeResourceDTO();
                devopsCustomizeResourceDTO.setEnvId(envId);
                devopsCustomizeResourceDTO.setId(objectId);
                if (devopsCustomizeResourceMapper.selectCount(devopsCustomizeResourceDTO) == 1) {
                    check = true;
                }
                break;
            default:
                break;
        }
        return check;
    }
}
