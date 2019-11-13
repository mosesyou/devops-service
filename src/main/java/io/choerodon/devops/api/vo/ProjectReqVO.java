package io.choerodon.devops.api.vo;

public class ProjectReqVO {

    private Long id;
    private String name;
    private String code;
    private Boolean permission;


    public ProjectReqVO(Long id, String name, String code, Boolean permission) {
        this(id, name, code);
        this.permission = permission;
    }

    public ProjectReqVO(Long id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
    }

    public ProjectReqVO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getPermission() {
        return permission;
    }

    public void setPermission(Boolean permission) {
        this.permission = permission;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

}
