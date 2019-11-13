package io.choerodon.devops.api.vo.kubernetes;

import java.util.List;

public class GitOpsMetaData {
    private String commit;
    private List<Error> errors;
    private List<FileCommit> filesCommit;
    private List<ResourceCommitVO> resourceCommits;

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public List<Error> getErrors() {
        return errors;
    }

    public void setErrors(List<Error> errors) {
        this.errors = errors;
    }

    public List<FileCommit> getFilesCommit() {
        return filesCommit;
    }

    public void setFilesCommit(List<FileCommit> filesCommit) {
        this.filesCommit = filesCommit;
    }

    public List<ResourceCommitVO> getResourceCommits() {
        return resourceCommits;
    }

    public void setResourceCommits(List<ResourceCommitVO> resourceCommits) {
        this.resourceCommits = resourceCommits;
    }
}
