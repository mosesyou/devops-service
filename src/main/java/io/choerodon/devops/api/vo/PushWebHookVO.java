package io.choerodon.devops.api.vo;

import java.util.List;

import io.swagger.annotations.ApiModelProperty;

/**
 * Creator: Runge
 * Date: 2018/7/9
 * Time: 15:53
 * Description:
 */
public class PushWebHookVO {
    private String objectKind;
    private String eventName;
    private String before;
    private String after;
    private String ref;
    private String checkoutSha;
    private Integer userId;
    private String userUserName;
    private Integer projectId;
    private List<CommitVO> commits;
    private Integer totalCommitsCount;
    private String token;
    @ApiModelProperty("之前的解析是否有错")
    private Boolean hasErrors;

    public String getUserUserName() {
        return userUserName;
    }

    public void setUserUserName(String userUserName) {
        this.userUserName = userUserName;
    }

    public String getObjectKind() {
        return objectKind;
    }

    public void setObjectKind(String objectKind) {
        this.objectKind = objectKind;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getCheckoutSha() {
        return checkoutSha;
    }

    public void setCheckoutSha(String checkoutSha) {
        this.checkoutSha = checkoutSha;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public List<CommitVO> getCommits() {
        return commits;
    }

    public void setCommits(List<CommitVO> commits) {
        this.commits = commits;
    }

    public Integer getTotalCommitsCount() {
        return totalCommitsCount;
    }

    public void setTotalCommitsCount(Integer totalCommitsCount) {
        this.totalCommitsCount = totalCommitsCount;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getHasErrors() {
        return hasErrors;
    }

    public void setHasErrors(Boolean hasErrors) {
        this.hasErrors = hasErrors;
    }

    @Override
    public String toString() {
        return "PushWebHookVO{" +
                "objectKind='" + objectKind + '\'' +
                ", eventName='" + eventName + '\'' +
                ", before='" + before + '\'' +
                ", after='" + after + '\'' +
                ", ref='" + ref + '\'' +
                ", checkoutSha='" + checkoutSha + '\'' +
                ", userId=" + userId +
                ", userUserName='" + userUserName + '\'' +
                ", projectId=" + projectId +
                ", commits=" + commits +
                ", totalCommitsCount=" + totalCommitsCount +
                ", token='" + token + '\'' +
                ", hasErrors=" + hasErrors +
                '}';
    }
}
