package io.choerodon.devops.app.service.impl;

import java.io.IOException;
import java.util.*;

import com.google.gson.Gson;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.devops.api.vo.ConfigVO;
import io.choerodon.devops.api.vo.DefaultConfigVO;
import io.choerodon.devops.api.vo.DevopsConfigRepVO;
import io.choerodon.devops.api.vo.DevopsConfigVO;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.infra.config.ConfigurationProperties;
import io.choerodon.devops.infra.config.HarborConfigurationProperties;
import io.choerodon.devops.infra.constant.MiscConstants;
import io.choerodon.devops.infra.dto.AppServiceDTO;
import io.choerodon.devops.infra.dto.DevopsConfigDTO;
import io.choerodon.devops.infra.dto.DevopsProjectDTO;
import io.choerodon.devops.infra.dto.HarborUserDTO;
import io.choerodon.devops.infra.dto.harbor.*;
import io.choerodon.devops.infra.dto.iam.ProjectDTO;
import io.choerodon.devops.infra.dto.iam.Tenant;
import io.choerodon.devops.infra.feign.HarborClient;
import io.choerodon.devops.infra.feign.RdupmClient;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;
import io.choerodon.devops.infra.handler.RetrofitHandler;
import io.choerodon.devops.infra.mapper.DevopsConfigMapper;
import io.choerodon.devops.infra.mapper.HarborUserMapper;
import io.choerodon.devops.infra.util.CommonExAssertUtil;
import io.choerodon.devops.infra.util.PageRequestUtil;
import io.choerodon.devops.infra.util.TypeUtil;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

/**
 * @author zongw.lee@gmail.com
 * @since 2019/03/11
 */
@Service
public class DevopsConfigServiceImpl implements DevopsConfigService {
    private static final String HARBOR = "harbor";
    private static final String AUTHTYPE_PULL = "pull";
    private static final String AUTHTYPE_PUSH = "push";
    private static final String CHART = "chart";
    private static final Gson gson = new Gson();
    private static final String USER_PREFIX = "pullUser%s%s";
    private static final String ERROR_CREATE_HARBOR_USER = "error.create.harbor.user";

    @Autowired
    private DevopsConfigMapper devopsConfigMapper;

    @Autowired
    private HarborUserMapper harborUserMapper;

    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;

    @Autowired
    private HarborConfigurationProperties harborConfigurationProperties;

    @Autowired
    private DevopsProjectService devopsProjectService;

    @Autowired
    private AppServiceService appServiceService;

    @Autowired
    private AppServiceVersionService appServiceVersionService;

    @Autowired
    private HarborService harborService;

    @Autowired
    private DevopsHarborUserService devopsHarborUserService;

    @Autowired
    @Lazy
    private RdupmClient rdupmClient;

    @Override
    public void operate(Long resourceId, String resourceType, List<DevopsConfigVO> devopsConfigVOS) {
        devopsConfigVOS.forEach(devopsConfigVO -> {
            //根据每个配置的默认还是自定义执行不同逻辑
            DevopsConfigDTO devopsConfigDTO = baseQueryByResourceAndType(resourceId, resourceType, devopsConfigVO.getType());
            if (devopsConfigVO.getCustom()) {
                //根据配置所在的资源层级，查询出数据库中是否存在
                DevopsConfigDTO newDevopsConfigDTO = voToDto(devopsConfigVO);
                if (devopsConfigDTO != null) {
                    // 存在判断是否已经生成服务版本，无服务版本，直接覆盖更新；有服务版本，将原config对应的resourceId设置为null,新建config
                    if (appServiceVersionService.isVersionUseConfig(devopsConfigDTO.getId(), devopsConfigVO.getType())) {
                        updateResourceId(devopsConfigDTO.getId());
                        setResourceId(resourceId, resourceType, newDevopsConfigDTO);
                        newDevopsConfigDTO.setId(null);
                        baseCreate(newDevopsConfigDTO);
                    } else {
                        newDevopsConfigDTO.setId(devopsConfigDTO.getId());
                        setResourceId(resourceId, resourceType, newDevopsConfigDTO);
                        baseUpdate(newDevopsConfigDTO);
                    }
                } else {
                    setResourceId(resourceId, resourceType, newDevopsConfigDTO);
                    newDevopsConfigDTO.setId(null);
                    baseCreate(newDevopsConfigDTO);
                }
            } else {
                //根据配置所在的资源层级，查询出数据库中是否存在，存在则删除
                if (devopsConfigDTO != null) {
                    if (appServiceVersionService.isVersionUseConfig(devopsConfigDTO.getId(), devopsConfigVO.getType())) {
                        updateResourceId(devopsConfigDTO.getId());
                    } else {
                        baseDelete(devopsConfigDTO.getId());
                    }
                }
            }
        });
    }


    @Override
    public List<DevopsConfigVO> queryByResourceId(Long resourceId, String resourceType) {

        List<DevopsConfigVO> devopsConfigVOS = new ArrayList<>();

        List<DevopsConfigDTO> devopsConfigDTOS = baseListByResource(resourceId, resourceType);
        devopsConfigDTOS.forEach(devopsConfigDTO -> {
            DevopsConfigVO devopsConfigVO = dtoToVo(devopsConfigDTO);
            //如果是项目层级下的harbor类型，需返回是否私有
            if (devopsConfigVO.getProjectId() != null && devopsConfigVO.getType().equals(HARBOR)) {
                DevopsProjectDTO devopsProjectDTO = devopsProjectService.baseQueryByProjectId(devopsConfigVO.getProjectId());
                devopsConfigVO.setHarborPrivate(devopsProjectDTO.getHarborProjectIsPrivate());
            }
            devopsConfigVOS.add(devopsConfigVO);
        });
        return devopsConfigVOS;
    }


    private void operateHarborProject(Long projectId, Boolean harborPrivate) {
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
        DevopsProjectDTO devopsProjectDTO = devopsProjectService.baseQueryByProjectId(projectId);
        Tenant organizationDTO = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());
        ConfigurationProperties configurationProperties = new ConfigurationProperties(harborConfigurationProperties);
        configurationProperties.setType(HARBOR);
        Retrofit retrofit = RetrofitHandler.initRetrofit(configurationProperties);
        HarborClient harborClient = retrofit.create(HarborClient.class);
        if (harborPrivate) {
            //设置为私有后将harbor项目设置为私有
            //创建harbor用户，push用户用于ci推送，pull用户用于部署拉取
            //1.创建用户并更新项目
            // 1.1 push用户
            if (devopsProjectDTO.getHarborUserId() != null) {
                HarborUserDTO oldHarborUser = harborUserMapper.selectByPrimaryKey(devopsProjectDTO.getHarborUserId());
                User user = new User(oldHarborUser.getHarborProjectUserName(),
                        oldHarborUser.getHarborProjectUserEmail(),
                        oldHarborUser.getHarborProjectUserPassword(),
                        oldHarborUser.getHarborProjectUserName());
                updateHarborProjectAndProjectMember(harborClient, user, Arrays.asList(1), organizationDTO, projectDTO);
            } else {
                User user = harborService.convertHarborUser(projectDTO, true, null);
                HarborUserDTO harborUser = new HarborUserDTO(user.getUsername(), null, user.getEmail(), true);
                createHarborUser(harborClient, user);
                harborUser.setHarborProjectUserPassword(user.getPassword());
                updateHarborProjectAndProjectMember(harborClient, user, Arrays.asList(1), organizationDTO, projectDTO);
                devopsHarborUserService.baseCreate(harborUser);
                devopsProjectDTO.setHarborUserId(harborUser.getId());
            }

            //1.2pull用户
            User pullUser = harborService.convertHarborUser(projectDTO, false, null);
            HarborUserDTO pullHarborUser = new HarborUserDTO(pullUser.getUsername(), pullUser.getPassword(), pullUser.getEmail(), false);
            createHarborUser(harborClient, pullUser);
            updateHarborProjectAndProjectMember(harborClient, pullUser, Arrays.asList(3), organizationDTO, projectDTO);
            devopsHarborUserService.baseCreateOrUpdate(pullHarborUser);
            devopsProjectDTO.setHarborPullUserId(pullHarborUser.getId());

            //2.项目设置为私有
            devopsProjectDTO.setHarborProjectIsPrivate(true);
            devopsProjectService.baseUpdate(devopsProjectDTO);
        } else {
            //设置为公有后将harbor项目设置为公有,删除pull成员角色
            try {
                Response<List<ProjectDetail>> projects = harborClient.listProject(organizationDTO.getTenantNum() + "-" + projectDTO.getCode()).execute();
                if (!CollectionUtils.isEmpty(projects.body())) {
                    Integer harborProjectId = getHarborProjectId(projects.body(), organizationDTO.getTenantNum() + "-" + projectDTO.getCode());

                    //1.更新harbor项目为公开
                    ProjectDetail projectDetail = new ProjectDetail();
                    Metadata metadata = new Metadata();
                    metadata.setHarborPublic("true");
                    projectDetail.setMetadata(metadata);
                    Response<Void> result = harborClient.updateProject(harborProjectId, projectDetail).execute();
                    if (result.raw().code() != 200) {
                        throw new CommonException("error.update.harbor.project");
                    }

                    //2.根据harbor版本 移除pull项目成员
                    Response<SystemInfo> systemInfoResponse = harborClient.getSystemInfo().execute();
                    if (systemInfoResponse.raw().code() != 200 || systemInfoResponse.body() == null) {
                        throw new CommonException("error.get.harbor.info");
                    }
                    if (systemInfoResponse.body().getHarborVersion().equals("v1.4.0")) {
                        Response<List<User>> users = harborClient.listUser(String.format(USER_PREFIX, organizationDTO.getTenantId(), projectId)).execute();
                        if (users.raw().code() != 200) {
                            throw new CommonException("error.list.harbor.project.member");
                        }
                        if (!ObjectUtils.isEmpty(users.body())) {
                            if (devopsProjectDTO.getHarborPullUserId() != null) {
                                for (User user : users.body()) {
                                    try {
                                        harborClient.deleteLowVersionMember(harborProjectId, user.getUserId().intValue()).execute();
                                    } catch (IOException e) {
                                        throw new CommonException("error.delete.harbor.member");
                                    }
                                }
                            }
                        }
                    } else {
                        Response<List<ProjectMember>> projectMembers = harborClient.getProjectMembers(harborProjectId, String.format(USER_PREFIX, organizationDTO.getTenantId(), projectId)).execute();
                        if (projectMembers.raw().code() != 200) {
                            throw new CommonException("error.list.harbor.project.member");
                        }
                        if (!ObjectUtils.isEmpty(projectMembers.body())) {
                            if (devopsProjectDTO.getHarborPullUserId() != null) {
                                for (ProjectMember projectMember : projectMembers.body()) {
                                    try {
                                        harborClient.deleteMember(harborProjectId, projectMember.getId()).execute();
                                    } catch (IOException e) {
                                        throw new CommonException("error.delete.harbor.member");
                                    }
                                }
                            }

                        }

                    }

                    //3. 更新devopsProject为非私有
                    devopsHarborUserService.baseDelete(devopsProjectDTO.getHarborPullUserId());
                    devopsProjectDTO.setHarborProjectIsPrivate(false);
                    devopsProjectDTO.setHarborPullUserId(null);
                    devopsProjectService.baseUpdateByPrimaryKey(devopsProjectDTO);
                } else {
                    throw new CommonException("error.harbor.get.project");
                }
            } catch (Exception e) {
                throw new CommonException(e.getMessage(), e);
            }
        }

    }

    @Override
    public DefaultConfigVO queryDefaultConfig(Long resourceId, String resourceType) {
        DefaultConfigVO defaultConfigVO = new DefaultConfigVO();

        //查询当前资源层级数据库中是否有对应的组件设置，有则返回url,无返回空，代表使用默认
        DevopsConfigDTO harborConfig = baseQueryByResourceAndType(resourceId, resourceType, HARBOR);
        if (harborConfig != null) {
            defaultConfigVO.setHarborConfigUrl(gson.fromJson(harborConfig.getConfig(), ConfigVO.class).getUrl());
        }
        DevopsConfigDTO chartConfig = baseQueryByResourceAndType(resourceId, resourceType, CHART);
        if (chartConfig != null) {
            defaultConfigVO.setChartConfigUrl(gson.fromJson(chartConfig.getConfig(), ConfigVO.class).getUrl());
        }
        return defaultConfigVO;
    }

    @Override
    public DevopsConfigDTO queryRealConfig(Long resourceId, String resourceType, String configType, String operateType) {
        //应用服务层次，先找应用配置，在找项目配置,最后找组织配置,项目和组织层次同理
        DevopsConfigDTO defaultConfig = baseQueryDefaultConfig(configType);
        if (resourceType.equals(MiscConstants.APP_SERVICE)) {
            DevopsConfigDTO appServiceConfig = baseQueryByResourceAndType(resourceId, resourceType, configType);
            if (appServiceConfig != null) {
                return appServiceConfig;
            }
            AppServiceDTO appServiceDTO = appServiceService.baseQuery(resourceId);
            if (appServiceDTO.getProjectId() == null) {
                return defaultConfig;
            }
            DevopsConfigDTO projectConfig = baseQueryByResourceAndType(appServiceDTO.getProjectId(), ResourceLevel.PROJECT.value(), configType);
            if (projectConfig != null) {
                return projectConfig;
            }
            ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(appServiceDTO.getProjectId());
            Tenant organizationDTO = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());
            DevopsConfigDTO organizationConfig = baseQueryByResourceAndType(organizationDTO.getTenantId(), ResourceLevel.ORGANIZATION.value(), configType);
            if (organizationConfig != null) {
                return organizationConfig;
            }
            return defaultConfig;
        } else if (resourceType.equals(ResourceLevel.PROJECT.value())) {
            DevopsConfigDTO projectConfig = baseQueryByResourceAndType(resourceId, ResourceLevel.PROJECT.value(), configType);
            if (projectConfig != null) {
                return projectConfig;
            }
            ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(resourceId);
            Tenant organizationDTO = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());
            DevopsConfigDTO organizationConfig = baseQueryByResourceAndType(organizationDTO.getTenantId(), ResourceLevel.ORGANIZATION.value(), configType);
            if (organizationConfig != null) {
                return organizationConfig;
            }
            return defaultConfig;
        } else {
            DevopsConfigDTO organizationConfig = baseQueryByResourceAndType(resourceId, ResourceLevel.ORGANIZATION.value(), configType);
            if (organizationConfig != null) {
                return organizationConfig;
            }
            return defaultConfig;
        }
    }

    @Override
    public DevopsConfigVO queryRealConfigVO(Long resourceId, String resourceType, String configType) {
        return dtoToVo(queryRealConfig(resourceId, resourceType, configType, AUTHTYPE_PULL));
    }

    @Override
    public DevopsConfigDTO baseCreate(DevopsConfigDTO devopsConfigDTO) {
        if (devopsConfigMapper.insert(devopsConfigDTO) != 1) {
            throw new CommonException("error.devops.project.config.create");
        }
        return devopsConfigDTO;
    }

    @Override
    public DevopsConfigDTO baseUpdate(DevopsConfigDTO devopsConfigDTO) {
        if (devopsConfigMapper.updateByPrimaryKeySelective(devopsConfigDTO) != 1) {
            throw new CommonException("error.devops.project.config.update");
        }
        return devopsConfigMapper.selectByPrimaryKey(devopsConfigDTO);
    }

    @Override
    public void updateResourceId(Long configId) {
        devopsConfigMapper.updateResourceId(configId);
    }

    @Override
    public DevopsConfigDTO baseQuery(Long id) {
        return devopsConfigMapper.selectByPrimaryKey(id);
    }

    @Override
    public DevopsConfigDTO baseQueryByName(Long projectId, String name) {
        DevopsConfigDTO paramDO = new DevopsConfigDTO();
        paramDO.setProjectId(projectId);
        paramDO.setName(name);
        return devopsConfigMapper.selectOne(paramDO);
    }

    @Override
    public DevopsConfigDTO baseCheckByName(String name) {
        return devopsConfigMapper.queryByNameWithNoProject(name);
    }

    @Override
    public Page<DevopsConfigDTO> basePageByOptions(Long projectId, PageRequest pageable, String params) {
        Map<String, Object> mapParams = TypeUtil.castMapParams(params);

        return PageHelper.doPageAndSort(PageRequestUtil.simpleConvertSortForPage(pageable),
                () -> devopsConfigMapper.listByOptions(projectId,
                        TypeUtil.cast(mapParams.get(TypeUtil.SEARCH_PARAM)),
                        TypeUtil.cast(mapParams.get(TypeUtil.PARAMS)), PageRequestUtil.checkSortIsEmpty(pageable)));
    }

    @Override
    public void baseDelete(Long id) {
        if (devopsConfigMapper.deleteByPrimaryKey(id) != 1) {
            throw new CommonException("error.devops.project.config.delete");
        }
    }

    @Override
    public DevopsConfigDTO baseQueryByResourceAndType(Long resourceId, String resourceType, String type) {
        DevopsConfigDTO devopsConfigDTO = new DevopsConfigDTO();
        setResourceId(resourceId, resourceType, devopsConfigDTO);
        devopsConfigDTO.setType(type);
        return devopsConfigMapper.selectOne(devopsConfigDTO);
    }


    private DevopsConfigDTO baseQueryDefaultConfig(String type) {
        DevopsConfigDTO devopsConfigDTO = new DevopsConfigDTO();
        if (type.equals(HARBOR)) {
            devopsConfigDTO.setName(MiscConstants.DEFAULT_HARBOR_NAME);
        } else {
            devopsConfigDTO.setName(MiscConstants.DEFAULT_CHART_NAME);
        }
        return devopsConfigMapper.selectOne(devopsConfigDTO);
    }

    private void setResourceId(Long resourceId, String resourceType, DevopsConfigDTO devopsConfigDTO) {
        if (ResourceLevel.ORGANIZATION.value().equals(resourceType)) {
            devopsConfigDTO.setOrganizationId(resourceId);
        } else if (ResourceLevel.PROJECT.value().equals(resourceType)) {
            devopsConfigDTO.setProjectId(resourceId);
        } else {
            devopsConfigDTO.setAppServiceId(resourceId);
        }
    }

    private List<DevopsConfigDTO> baseListByResource(Long resourceId, String resourceType) {
        DevopsConfigDTO devopsConfigDTO = new DevopsConfigDTO();
        setResourceId(resourceId, resourceType, devopsConfigDTO);
        return devopsConfigMapper.select(devopsConfigDTO);
    }

    @Override
    public DevopsConfigVO dtoToVo(DevopsConfigDTO devopsConfigDTO) {
        DevopsConfigVO devopsConfigVO = new DevopsConfigVO();
        BeanUtils.copyProperties(devopsConfigDTO, devopsConfigVO);
        ConfigVO configVO = gson.fromJson(devopsConfigDTO.getConfig(), ConfigVO.class);
        devopsConfigVO.setConfig(configVO);
        return devopsConfigVO;
    }

    @Override
    public DevopsConfigDTO voToDto(DevopsConfigVO devopsConfigVO) {
        DevopsConfigDTO devopsConfigDTO = new DevopsConfigDTO();
        BeanUtils.copyProperties(devopsConfigVO, devopsConfigDTO);
        String configJson = gson.toJson(devopsConfigVO.getConfig());
        devopsConfigDTO.setConfig(configJson);
        return devopsConfigDTO;
    }

    private void configVOInToRepVO(DevopsConfigRepVO devopsConfigRepVO, DevopsConfigVO devopsConfigVO) {
        if (devopsConfigVO.getType().equals(HARBOR)) {
            devopsConfigRepVO.setHarbor(devopsConfigVO);
            devopsConfigRepVO.setHarborPrivate(devopsConfigVO.getHarborPrivate());
        } else if (devopsConfigVO.getType().equals(CHART)) {
            devopsConfigRepVO.setChart(devopsConfigVO);
        }
    }

    @Override
    public DevopsConfigRepVO queryConfig(Long resourceId, String resourceType) {
        DevopsConfigRepVO devopsConfigRepVO = new DevopsConfigRepVO();
        List<DevopsConfigVO> configVOS = queryByResourceId(resourceId, resourceType);
        configVOS.forEach(devopsConfigVO -> configVOInToRepVO(devopsConfigRepVO, devopsConfigVO));
        if (resourceType.equals(ResourceLevel.PROJECT.value())) {
            DevopsProjectDTO devopsProjectDTO = devopsProjectService.baseQueryByProjectId(resourceId);
            devopsConfigRepVO.setHarborPrivate(devopsProjectDTO.getHarborProjectIsPrivate());
        }
        return devopsConfigRepVO;
    }

    @Override
    public void operateConfig(Long resourceId, String resourceType, DevopsConfigRepVO devopsConfigRepVO) {
        List<DevopsConfigVO> configVOS = new ArrayList<>();
        DevopsConfigVO chart;
        if (ObjectUtils.isEmpty(devopsConfigRepVO.getChart())) {
            chart = new DevopsConfigVO();
            chart.setCustom(false);
        } else {
            chart = devopsConfigRepVO.getChart();
            chart.setCustom(Boolean.TRUE);
            ConfigVO configVO = chart.getConfig();
            CommonExAssertUtil.assertNotNull(configVO, "error.chart.config.null");
            boolean usernameEmpty = StringUtils.isEmpty(configVO.getUserName());
            boolean passwordEmpty = StringUtils.isEmpty(configVO.getPassword());
            if (!usernameEmpty && !passwordEmpty) {
                configVO.setUserName(configVO.getUserName());
                configVO.setPassword(configVO.getPassword());
                configVO.setPrivate(Boolean.TRUE);
            }

            // 用户名和密码要么都为空, 要么都有值
            CommonExAssertUtil.assertTrue(((usernameEmpty && passwordEmpty) || (!usernameEmpty && !passwordEmpty)), "error.chart.auth.invalid");
        }
        chart.setType(CHART);
        configVOS.add(chart);
        operate(resourceId, resourceType, configVOS);
    }

    @Override
    public void deleteByConfigIds(Set<Long> configIds) {
        List<DevopsConfigDTO> devopsConfigDTOS = devopsConfigMapper.listByConfigs(configIds);
        devopsConfigDTOS.stream().filter(devopsConfigDTO -> devopsConfigDTO.getAppServiceId() != null)
                .forEach(devopsConfigDTO -> devopsConfigMapper.deleteByPrimaryKey(devopsConfigDTO.getId()));
    }

    private void checkRegistryProjectIsPrivate(DevopsConfigVO devopsConfigVO) {
        ConfigurationProperties configurationProperties = new ConfigurationProperties();
        configurationProperties.setBaseUrl(devopsConfigVO.getConfig().getUrl());
        configurationProperties.setUsername(devopsConfigVO.getConfig().getUserName());
        configurationProperties.setPassword(devopsConfigVO.getConfig().getPassword());
        configurationProperties.setInsecureSkipTlsVerify(false);
        configurationProperties.setProject(devopsConfigVO.getConfig().getProject());
        configurationProperties.setType(HARBOR);
        Retrofit retrofit = RetrofitHandler.initRetrofit(configurationProperties);
        HarborClient harborClient = retrofit.create(HarborClient.class);
        Call<List<ProjectDetail>> listProject = harborClient.listProject(devopsConfigVO.getConfig().getProject());
        Response<List<ProjectDetail>> projectResponse;
        try {
            projectResponse = listProject.execute();
            if (projectResponse != null && projectResponse.body() != null) {
                Optional<ProjectDetail> optional = projectResponse.body().stream().filter(t -> t.getName().equals(devopsConfigVO.getConfig().getProject())).findFirst();
                if (Objects.isNull(optional) || !optional.isPresent()) {
                    throw new CommonException("error.harbor.get.project");
                }
                if ("false".equals(optional.get().getMetadata().getHarborPublic())) {
                    devopsConfigVO.getConfig().setPrivate(true);
                } else {
                    devopsConfigVO.getConfig().setPrivate(false);
                }
            }
        } catch (IOException e) {
            throw new CommonException(e);
        }
    }

    private void createHarborUser(HarborClient harborClient, User user) {
        Response<Void> result = null;
        try {
            Response<List<User>> users = harborClient.listUser(user.getUsername()).execute();
            if (users.raw().code() != 200) {
                throw new CommonException(ERROR_CREATE_HARBOR_USER);
            }
            if (users.body() == null || users.body().isEmpty()) {
                result = harborClient.insertUser(user).execute();
                if (result.raw().code() != 201 && result.raw().code() != 409) {
                    throw new CommonException(ERROR_CREATE_HARBOR_USER);
                }
            } else {
                Boolean exist = users.body().stream().anyMatch(user1 -> user1.getUsername().equals(user.getUsername()));
                if (!exist) {
                    result = harborClient.insertUser(user).execute();
                    if (result.raw().code() != 201 && result.raw().code() != 409) {
                        throw new CommonException(ERROR_CREATE_HARBOR_USER);
                    }
                }
            }
        } catch (IOException e) {
            throw new CommonException(e);
        }
    }

    private void updateHarborProjectAndProjectMember(HarborClient harborClient, User user, List<Integer> roles, Tenant organizationDTO, ProjectDTO projectDTO) {
        Response<Void> result = null;

        //给项目绑定角色
        Response<List<ProjectDetail>> projects = null;
        try {
            projects = harborClient.listProject(organizationDTO.getTenantNum() + "-" + projectDTO.getCode()).execute();

            if (projects.body() != null && !projects.body().isEmpty()) {
                Integer harborProjectId = getHarborProjectId(projects.body(), organizationDTO.getTenantNum() + "-" + projectDTO.getCode());

                ProjectDetail projectDetail = new ProjectDetail();
                Metadata metadata = new Metadata();
                metadata.setHarborPublic("false");
                projectDetail.setMetadata(metadata);
                result = harborClient.updateProject(harborProjectId, projectDetail).execute();
                if (result.raw().code() != 200) {
                    throw new CommonException("error.update.harbor.project");
                }
                Response<SystemInfo> systemInfoResponse = harborClient.getSystemInfo().execute();
                if (systemInfoResponse.raw().code() != 200) {
                    throw new CommonException("error.get.harbor.info");
                }

                if (systemInfoResponse.body().getHarborVersion().equals("v1.4.0")) {
                    Role role = new Role();
                    role.setUsername(user.getUsername());
                    role.setRoles(roles);
                    result = harborClient.setProjectMember(harborProjectId, role).execute();
                } else {
                    ProjectMember projectMember = new ProjectMember();
                    MemberUser memberUser = new MemberUser();
                    projectMember.setRoleId(roles.get(0));
                    memberUser.setUsername(user.getUsername());
                    projectMember.setMemberUser(memberUser);
                    result = harborClient.setProjectMember(harborProjectId, projectMember).execute();
                }
                if (result.raw().code() != 201 && result.raw().code() != 200 && result.raw().code() != 409) {
                    throw new CommonException("error.create.harbor.project.member");
                }
            }
        } catch (Exception e) {
            throw new CommonException(e);
        }
    }

    @Override
    public Integer getHarborProjectId(List<ProjectDetail> projectDetailList, String harborProjectName) {
        Optional<ProjectDetail> optional = projectDetailList.stream().filter(t -> t.getName().equals(harborProjectName)).findFirst();
        if (Objects.isNull(optional) || !optional.isPresent()) {
            throw new CommonException("error.harbor.get.project");
        }
        return optional.get().getProjectId();
    }
}


