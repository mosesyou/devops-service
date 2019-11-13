package io.choerodon.devops.app.service.impl;


import javax.annotation.Nullable;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.app.eventhandler.payload.GitlabGroupPayload;
import io.choerodon.devops.app.service.DevopsProjectService;
import io.choerodon.devops.app.service.GitlabGroupService;
import io.choerodon.devops.app.service.UserAttrService;
import io.choerodon.devops.infra.dto.DevopsProjectDTO;
import io.choerodon.devops.infra.dto.UserAttrDTO;
import io.choerodon.devops.infra.dto.gitlab.GroupDTO;
import io.choerodon.devops.infra.enums.Visibility;
import io.choerodon.devops.infra.feign.operator.GitlabServiceClientOperator;
import io.choerodon.devops.infra.util.TypeUtil;

@Service
public class GitlabGroupServiceImpl implements GitlabGroupService {
    private static final String GROUP_NAME_FORMAT = "%s-%s%s";
    private static final String ENV_GROUP_SUFFIX = "-gitops";

    @Autowired
    private DevopsProjectService devopsProjectService;
    @Autowired
    private UserAttrService userAttrService;
    @Autowired
    private GitlabServiceClientOperator gitlabServiceClientOperator;

    @Override
    public void createGroups(GitlabGroupPayload gitlabGroupPayload) {
        createGroup(gitlabGroupPayload, ENV_GROUP_SUFFIX);
        createGroup(gitlabGroupPayload, null);
    }

    @Override
    public void updateGroups(GitlabGroupPayload gitlabGroupPayload) {
        updateGroup(gitlabGroupPayload, ENV_GROUP_SUFFIX);
        updateGroup(gitlabGroupPayload, null);
    }

    @Override
    public GroupDTO createSiteAppGroup(Long iamUserId, String groupName) {
        GroupDTO group = new GroupDTO();
        group.setName(groupName);
        group.setPath(groupName);
        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(iamUserId);
        GroupDTO groupDTO = gitlabServiceClientOperator.queryGroupByName(group.getPath(), TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()));
        if (groupDTO == null) {
            group.setVisibility(Visibility.PUBLIC);
            groupDTO = gitlabServiceClientOperator.createGroup(group, TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()));
        }
        return groupDTO;
    }

    private void createGroup(GitlabGroupPayload gitlabGroupPayload, @Nullable final String suffix) {
        final String actualSuffix = suffix == null ? "" : suffix;

        GroupDTO group = new GroupDTO();

        // name: orgName-projectName + suffix
        String name = String.format(GROUP_NAME_FORMAT, gitlabGroupPayload.getOrganizationName(),
                gitlabGroupPayload.getProjectName(), actualSuffix);
        // path: orgName-projectCode + suffix
        String path = String.format(GROUP_NAME_FORMAT, gitlabGroupPayload.getOrganizationCode(),
                gitlabGroupPayload.getProjectCode(), actualSuffix);

        group.setName(name);
        group.setPath(path);

        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(gitlabGroupPayload.getUserId());
        GroupDTO groupDTO = gitlabServiceClientOperator.queryGroupByName(group.getPath(), TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()));
        if (groupDTO == null) {
            groupDTO = gitlabServiceClientOperator.createGroup(group, TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()));
        }

        DevopsProjectDTO devopsProjectDO = new DevopsProjectDTO(gitlabGroupPayload.getProjectId());
        if (ENV_GROUP_SUFFIX.equals(suffix)) {
            devopsProjectDO.setDevopsEnvGroupId(TypeUtil.objToLong(groupDTO.getId()));
        } else {
            devopsProjectDO.setDevopsAppGroupId(TypeUtil.objToLong(groupDTO.getId()));
        }
        devopsProjectService.baseUpdate(devopsProjectDO);
    }

    /**
     * 更新组
     *
     * @param gitlabGroupPayload 项目信息
     * @param suffix             组名后缀，可为 null
     */
    private void updateGroup(GitlabGroupPayload gitlabGroupPayload, @Nullable final String suffix) {
        final String actualSuffix = suffix == null ? "" : suffix;

        GroupDTO group = new GroupDTO();

        // name: orgName-projectName + suffix
        String name = String.format(GROUP_NAME_FORMAT, gitlabGroupPayload.getOrganizationName(),
                gitlabGroupPayload.getProjectName(), actualSuffix);
        // path: orgName-projectCode + suffix
        String path = String.format(GROUP_NAME_FORMAT, gitlabGroupPayload.getOrganizationCode(),
                gitlabGroupPayload.getProjectCode(), actualSuffix);
        group.setName(name);
        group.setPath(path);

        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(gitlabGroupPayload.getUserId());
        DevopsProjectDTO devopsProjectDTO = devopsProjectService.baseQueryByProjectId(gitlabGroupPayload.getProjectId());

        Integer groupId;
        if (ENV_GROUP_SUFFIX.equals(suffix)) {
            groupId = TypeUtil.objToInteger(devopsProjectDTO.getDevopsEnvGroupId());
        } else {
            groupId = TypeUtil.objToInteger(devopsProjectDTO.getDevopsAppGroupId());
        }

        try {
            gitlabServiceClientOperator.updateGroup(groupId, TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()), group);
        } catch (FeignException e) {
            throw new CommonException(e);
        }
    }

}
