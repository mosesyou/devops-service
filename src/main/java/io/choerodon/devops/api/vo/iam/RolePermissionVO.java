package io.choerodon.devops.api.vo.iam;

import javax.validation.constraints.NotEmpty;

import io.swagger.annotations.ApiModelProperty;
import org.hzero.starter.keyencrypt.core.Encrypt;

/**
 * Created by n!Ck
 * Date: 2018/10/26
 * Time: 13:24
 * Description:
 */
public class RolePermissionVO {
    @Encrypt
    @ApiModelProperty(value = "主键ID")
    private Long id;
    @Encrypt
    @ApiModelProperty(value = "角色ID/必填")
    private Long roleId;
    @Encrypt
    @ApiModelProperty(value = "权限ID/必填")
    @NotEmpty(message = "errpr.rolePermission.permissionId.empty")
    private Long permissionId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }
}
