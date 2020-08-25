package io.choerodon.devops.infra.feign;

import java.util.List;
import java.util.Set;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.choerodon.devops.api.vo.hrdsCode.MemberPrivilegeViewDTO;
import io.choerodon.devops.api.vo.hrdsCode.RepositoryPrivilegeViewDTO;
import io.choerodon.devops.infra.feign.fallback.HzeroMessageServiceClientFallBack;

/**
 * @author scp
 * @date 2020/6/11
 * @description
 */
@FeignClient(value = "hrds-code-repo", fallback = HzeroMessageServiceClientFallBack.class)
public interface HrdsCodeRepoClient {

    /**
     * 查询当前用户在gitlab下的角色
     *
     * @param organizationId
     * @param projectId
     * @param repositoryIds  应用服务Id
     * @return
     */
    @PostMapping("/v1/organizations/{organizationId}/projects/{projectId}/gitlab/repositories/members/self/privilege")
    ResponseEntity<List<MemberPrivilegeViewDTO>> selfPrivilege(@PathVariable("organizationId") Long organizationId,
                                                               @PathVariable("projectId") Long projectId,
                                                               @RequestBody Set<Long> repositoryIds);

    /**
     * 查询用户在项目下有权限的应用服务
     *
     * @param organizationId
     * @param projectId
     * @param userIds        用户id
     * @return
     */
    @PostMapping("/v1/organizations/{organizationId}/projects/{projectId}/gitlab/members/repositories/within")
    ResponseEntity<List<RepositoryPrivilegeViewDTO>> listRepositoriesByPrivilege(@PathVariable("organizationId") Long organizationId,
                                                                                 @PathVariable("projectId") Long projectId,
                                                                                 @RequestBody Set<Long> userIds);
}
