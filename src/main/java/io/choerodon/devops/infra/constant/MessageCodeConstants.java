package io.choerodon.devops.infra.constant;

/**
 * 消息通知的code
 *
 * @author zmf
 * @since 12/6/19
 */
public class MessageCodeConstants {
    private MessageCodeConstants() {
    }

    public static final String APP_SERVICE_CREATION_FAILED = "APPSERVICECREATIONFAILURE";
    public static final String APP_SERVICE_ENABLED = "ENABLEAPPSERVICE";
    public static final String APP_SERVICE_DISABLE = "DISABLEAPPSERVICE";
    public static final String AUDIT_MERGE_REQUEST = "AUDITMERGEREQUEST";
    public static final String CERTIFICATION_CREATION_FAILURE = "CERTIFICATIONFAILURE";
    public static final String GITLAB_CONTINUOUS_DELIVERY_FAILURE = "GITLABCONTINUOUSDELIVERYFAILURE";
    public static final String INGRESS_CREATION_FAILURE = "INGRESSFAILURE";
    public static final String INSTANCE_CREATION_FAILURE = "INSTANCEFAILURE";
    public static final String MERGE_REQUEST_CLOSED = "MERGEREQUESTCLOSED";
    public static final String MERGE_REQUEST_PASSED = "MERGEREQUESTPASSED";
    public static final String SERVICE_CREATION_FAILURE = "SERVICEFAILURE";
    public static final String RESOURCE_DELETE_CONFIRMATION = "RESOURCEDELETECONFIRMATION";
    public static final String GITLAB_PWD = "GITLABPASSWORD";
    public static final String INVITE_USER = "INVITEUSER";
    public static final String DELETE_APP_SERVICE = "DELETEAPPSERVICE";
    public static final String PIPELINE_FAILED = "PIPELINEFAILED";
    public static final String PIPELINE_SUCCESS = "PIPELINESUCCESS";
    public static final String PIPELINE_AUDIT = "PIPELINEAUDIT";
    public static final String PIPELINE_STOP = "PIPELINESTOP";
    public static final String PIPELINE_PASS = "PIPELINEPASS";
    public static final String PIPELINE_STAGE_AUDIT = "PIPELINESTAGEAUDIT";
}
