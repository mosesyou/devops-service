package io.choerodon.devops.infra.constant;

/**
 * WebSocket相关的一些常量
 *
 * @author zmf
 * @since 20-5-8
 */
public final class DevOpsWebSocketConstants {
    public static final String AGENT = "agent";
    public static final String FRONT_LOG = "front_log";
    public static final String FRONT_EXEC = "front_exec";
    public static final String FRONT_DESCRIBE = "front_describe";
    public static final String AGENT_LOG = "agent_log";
    public static final String AGENT_EXEC = "agent_exec";
    public static final String AGENT_DESCRIBE = "agent_describe";
    public static final String AGENT_POLARIS = "agent_polaris";
    public static final String CLUSTER_ID = "clusterId";
    public static final String COLON = ":";
    public static final String POD_NAME = "podName";
    public static final String CONTAINER_NAME = "containerName";
    public static final String LOG_ID = "logId";
    public static final String ENV = "env";
    public static final String KIND = "kind";
    public static final String NAME = "name";
    public static final String DESCRIBE_Id = "describeId";

    /**
     * 形如:  cluster:123
     */
    public static final String KEY = "key";

    public static final String TOKEN = "token";

    public static final String VERSION = "version";
    /**
     * 从agent的连接的group的前缀
     */
    public static final String FROM_AGENT_GROUP_PREFIX = "from_agent:";
    /**
     * 从前端的连接的group的前缀
     */
    public static final String FROM_FRONT_GROUP_PREFIX = "from_front:";

    public static final String PARAMETER_NULL_TEMPLATE = "The parameter %s is unexpectedly null";

    public static final String KUBERNETES_GET_LOGS = "kubernetes_get_logs";

    public static final String EXEC_COMMAND = "kubernetes_exec";

    private DevOpsWebSocketConstants() {
    }
}
