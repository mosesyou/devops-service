package io.choerodon.devops.app.eventhandler.payload;

import java.util.List;

import io.swagger.annotations.ApiModelProperty;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  13:48 2019/8/2
 * Description:
 */
public class AppServiceDownloadPayload {
    @ApiModelProperty("应用服务Id，非必传")
    private Long appId;

    @ApiModelProperty("应用服务Name")
    private String appServiceName;

    @ApiModelProperty("应用服务Code")
    private String appServiceCode;

    @ApiModelProperty("应用服务Type")
    private String appServiceType;

    @ApiModelProperty("应用服务版本")
    private List<AppServiceVersionDownloadPayload> appServiceVersionDownloadPayloads;

    public String getAppServiceName() {
        return appServiceName;
    }

    public void setAppServiceName(String appServiceName) {
        this.appServiceName = appServiceName;
    }

    public String getAppServiceCode() {
        return appServiceCode;
    }

    public void setAppServiceCode(String appServiceCode) {
        this.appServiceCode = appServiceCode;
    }

    public String getAppServiceType() {
        return appServiceType;
    }

    public void setAppServiceType(String appServiceType) {
        this.appServiceType = appServiceType;
    }

    public List<AppServiceVersionDownloadPayload> getAppServiceVersionDownloadPayloads() {
        return appServiceVersionDownloadPayloads;
    }

    public void setAppServiceVersionDownloadPayloads(List<AppServiceVersionDownloadPayload> appServiceVersionDownloadPayloads) {
        this.appServiceVersionDownloadPayloads = appServiceVersionDownloadPayloads;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }
}
