package io.choerodon.devops.app.service;

import java.util.List;

import io.choerodon.devops.api.vo.GitlabGroupMemberVO;
import io.choerodon.devops.infra.dto.DevopsEnvironmentDTO;
import io.choerodon.devops.infra.dto.UserAttrDTO;
import io.choerodon.devops.infra.dto.gitlab.MemberDTO;

/**
 * Created by Zenger on 2018/3/28.
 */
public interface GitlabGroupMemberService {

    void createGitlabGroupMemberRole(List<GitlabGroupMemberVO> gitlabGroupMemberVOList);

    void deleteGitlabGroupMemberRole(List<GitlabGroupMemberVO> gitlabGroupMemberVOList);

    void checkEnvProject(DevopsEnvironmentDTO devopsEnvironmentDTO, UserAttrDTO userAttrDTO);

    MemberDTO queryByUserId(Integer groupId, Integer userId);

    void delete(Integer groupId, Integer userId);

    int create(Integer groupId, MemberDTO memberDTO);

    void update(Integer groupId, MemberDTO memberDTO);
}
