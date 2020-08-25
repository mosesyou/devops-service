package io.choerodon.devops.api.vo;

import java.util.List;

import io.swagger.annotations.ApiModelProperty;

public class GitConfigVO {
    private String gitHost;
    private List<GitEnvConfigVO> envs;
    @ApiModelProperty("Agent的Helm Release code")
    private String agentName;

    public String getGitHost() {
        return gitHost;
    }

    public void setGitHost(String gitHost) {
        this.gitHost = gitHost;
    }

    public List<GitEnvConfigVO> getEnvs() {
        return envs;
    }

    public void setEnvs(List<GitEnvConfigVO> envs) {
        this.envs = envs;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }
}
