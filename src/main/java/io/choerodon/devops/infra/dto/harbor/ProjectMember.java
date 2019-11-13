package io.choerodon.devops.infra.dto.harbor;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Sheep on 2019/4/29.
 */
public class ProjectMember {

    private Integer id;

    @SerializedName("role_id")
    private Integer roleId;

    @SerializedName("member_user")
    private MemberUser memberUser;

    public ProjectMember() {
        this.roleId = 1;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public MemberUser getMemberUser() {
        return memberUser;
    }

    public void setMemberUser(MemberUser memberUser) {
        this.memberUser = memberUser;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
