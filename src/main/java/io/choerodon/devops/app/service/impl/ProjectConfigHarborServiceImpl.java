package io.choerodon.devops.app.service.impl;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import retrofit2.Response;
import retrofit2.Retrofit;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.vo.ConfigVO;
import io.choerodon.devops.app.service.ProjectConfigHarborService;
import io.choerodon.devops.infra.config.ConfigurationProperties;
import io.choerodon.devops.infra.dto.harbor.Project;
import io.choerodon.devops.infra.dto.iam.ProjectDTO;
import io.choerodon.devops.infra.dto.iam.Tenant;
import io.choerodon.devops.infra.feign.HarborClient;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;
import io.choerodon.devops.infra.handler.RetrofitHandler;

/**
 * @author zongw.lee@gmail.com
 * @since 2019/03/11
 */
@Component
public class ProjectConfigHarborServiceImpl implements ProjectConfigHarborService {
    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;

    //创建harbor仓库
    @Override
    public void createHarbor(ConfigVO config, Long projectId) {
        try {
            ConfigurationProperties configurationProperties = new ConfigurationProperties(config);
            configurationProperties.setType("harbor");
            Retrofit retrofit = RetrofitHandler.initRetrofit(configurationProperties);
            HarborClient harborClient = retrofit.create(HarborClient.class);
            Response<Void> result;

            ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
            Tenant organizationDTO = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());
            result = harborClient.insertProject(new Project(
                    organizationDTO.getTenantNum() + "-" + projectDTO.getCode(), 1)).execute();

            if (result.raw().code() != 201) {
                throw new CommonException(result.errorBody().string());
            }
        } catch (IOException e) {
            throw new CommonException(e);
        }
    }
}
