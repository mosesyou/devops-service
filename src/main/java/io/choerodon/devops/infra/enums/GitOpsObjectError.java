package io.choerodon.devops.infra.enums;

public enum GitOpsObjectError {
    RELEASE_APP_SERVICE_ID_MISMATCH("release.app.service.id.not.matched"),
    RELEASE_META_DATA_NOT_FOUND("release.meta.data.not.found"),
    RELEASE_NAME_NOT_FOUND("release.name.not.found"),
    RELEASE_SPEC_NOT_FOUND("release.spec.not.found"),
    RELEASE_CHART_NAME_NOT_FOUND("release.chart.name.not.found"),
    RELEASE_CHART_VERSION_NOT_FOUND("release.chart.version.not.found"),
    RELEASE_REPO_URL_NOT_FOUND("release.repo.url.not.found"),
    RELEASE_API_VERSION_NOT_FOUND("release.api.version.not.found"),
    SERVICE_METADATA_NOT_FOUND("service.metadata.not.found"),
    SERVICE_NAME_NOT_FOUND("service.name.not.found"),
    SERVICE_SPEC_NOT_FOUND("service.spec.not.found"),
    SERVICE_PORTS_NOT_FOUND("service.ports.not.found"),
    SERVICE_PORTS_NAME_NOT_FOUND("service.ports.name.not.found"),
    SERVICE_PORTS_PORT_NOT_FOUND("service.ports.port.not.found"),
    SERVICE_PORTS_TARGET_PORT("service.ports.target.port"),
    SERVICE_TYPE_NOT_FOUND("service.type.not.found"),
    SERVICE_API_VERSION_NOT_FOUND("service.api.version.not.found"),
    SERVICE_RELATED_INGRESS_NOT_FOUND("service.related.ingress.not.found"),
    SERVICE_ANNOTATED_NOT_SUPPORTED_ANY_MORE("service.not.supported.any.more"),
    INGRESS_META_DATA_NOT_FOUND("ingress.meta.data.not.found"),
    INGRESS_NAME_NOT_FOUND("ingress.name.not.found"),
    INGRESS_SPEC_NOT_FOUND("ingress.spec.not.found"),
    INGRESS_RULES_NOT_FOUND("ingress.rules.not.found"),
    INGRESS_RULE_HOST_NOT_FOUND("ingress.rule.host.not.found"),
    INGRESS_RULE_HTTP_NOT_FOUND("ingress.rule.http.not.found"),
    INGRESS_BACKEND_SERVICE_NAME_NOT_FOUND("ingress.backend.service.name.not.found"),
    INGRESS_BACKEND_SERVICE_PORT_NOT_FOUND("ingress.backend.service.port.not.found"),
    INGRESS_API_VERSION_NOT_FOUND("ingress.api.version.not.found"),
    INGRESS_DOMAIN_PATH_IS_EXIST("ingress.domain.path.is.exist"),
    INGRESS_PATHS_NOT_FOUND("ingress.paths.not.found"),
    INGRESS_PATHS_PATH_NOT_FOUND("ingress.paths.path.not.found"),
    INGRESS_PATHS_BACKEND_NOT_FOUND("ingress.paths.backend.not.found"),
    INGRESS_PATH_DUPLICATED("ingress.path.duplicated"),
    INGRESS_PATH_IS_EMPTY("ingress.path.is.empty"),
    INGRESS_PATH_PORT_NOT_BELONG_TO_SERVICE("ingress.path.port.not.belong.to.service"),
    CERT_ACME_OR_EXIST_CERT_NOT_FOUND("cert.acme.or.exist.cert.not.found"),
    CERT_DOMAINS_ILLEGAL("cert.domains.illegal"),
    CERT_NAMESPACE_NOT_FOUND("cert.namespace.not.found"),
    CERT_API_VERSION_NOT_FOUND("cert.api.version.not.found"),
    CERT_META_DATA_NOT_FOUND("cert.meta.data.not.found"),
    CERT_NAME_NOT_FOUND("cert.name.not.found"),
    CERT_SPEC_NOT_FOUND("cert.spec.not.found"),
    CERT_COMMON_NAME_NOT_FOUND("cert.common.name.not.found"),
    CERT_CRT_NOT_FOUND("cert.crt.not.found"),
    CERT_KEY_NOT_FOUND("cert.key.not.found"),
    CERT_ACME_CONFIG_NOT_FOUND("cert.acme.config.not.found"),
    CERT_HTTP_NOT_FOUND("cert.http.not.found"),
    CERT_INGRESS_CLASS_ERROR("cert.ingress.class.error"),
    CERT_DOMAINS_NOT_FOUND("cert.domains.not.found"),
    CERT_CHANGED("cert.changed"),
    SECRET_API_VERSION_NOT_FOUND("secret.api.version.not.found"),
    SECRET_NAME_NOT_FOUND("secret.name.not.found"),
    SECRET_TYPE_NOT_FOUND("secret.type.not.found"),
    SECRET_STRING_DATA_NOT_FOUND("secret.stringData.not.found"),
    SECRET_DATA_NOT_FOUND("secret.data.not.found"),
    INSTANCE_APP_ID_NOT_SAME("instance.app.id.not.same"),
    INSTANCE_RELATED_SERVICE_NOT_FOUND("instance.related.service.not.found"),
    CONFIG_MAP_DATA_NOT_FOUND("configMap.data.not.found"),
    CONFIG_MAP_NAME_NOT_FOUND("configMap.name.not.found"),
    CONFIG_MAP_METADATA_NOT_FOUND("configMap.metadata.not.found"),
    END_POINT_NAME_NOT_FOUND("endPoints.name.not.found"),
    END_POINT_METADATA_NOT_FOUND("endPoints.metadata.not.found"),
    END_POINT_SUBSETS_NOT_FOUND("endPoints.subsets.not.found"),
    END_POINT_ADDRESS_NOT_FOUND("endPoints.address.not.found"),
    END_POINT_PORTS_NOT_FOUND("endPoints.ports.not.found"),
    END_POINT_ADDRESS_IP_NOT_FOUND("endPoints.address.ip.not.found"),
    END_POINT_PORTS_PORT_NOT_FOUND("endPoints.ports.port.not.found"),
    CUSTOM_RESOURCE_NAME_NOT_FOUND("custom.resource.name.not.found"),
    CUSTOM_RESOURCE_KIND_NOT_FOUND("custom.resource.kind.not.found"),
    CUSTOM_RESOURCE_METADATA_NOT_FOUND("custom.resource.metadata.not.found"),
    PERSISTENT_VOLUME_CLAIM_METADATA_NOT_FOUND("pvc.metadata.not.found"),
    PERSISTENT_VOLUME_CLAIM_NAME_NOT_FOUND("pvc.name.not.found"),
    PERSISTENT_VOLUME_CLAIM_ACCESS_MODE_NOT_FOUND("pvc.access.mode.not.found"),
    PERSISTENT_VOLUME_CLAIM_ACCESS_MODE_SIZE_NOT_SUPPORTED("pvc.access.mode.size.not.supported"),
    PERSISTENT_VOLUME_CLAIM_RESOURCE_REQUEST_NOT_FOUND("pvc.resource.request.not.found"),
    PERSISTENT_VOLUME_CLAIM_UNMODIFIED("pvc.is.unmodified"),
    PERSISTENT_VOLUME_METADATA_NOT_FOUND("pv.metadata.not.found"),
    PERSISTENT_VOLUME_NAME_NOT_FOUND("pv.name.not.found"),
    PERSISTENT_VOLUME_CAPACITY_NOT_FOUND("pv.capacity.not.found"),
    PERSISTENT_VOLUME_STORAGE_NOT_FOUND("pv.storage.not.found"),
    PERSISTENT_VOLUME_ACCESS_MODE_NOT_FOUND("pv.access.mode.not.found"),
    PERSISTENT_VOLUME_ACCESS_MODE_SIZE_NOT_SUPPORTED("pv.access.mode.size.not.supported"),
    PERSISTENT_VOLUME_TYPE_NOT_FOUND("pv.type.not.found"),
    PERSISTENT_VOLUME_TYPE_MULTI("multi.pv.type.not.valid"),
    PERSISTENT_VOLUME_NFS_PATH_NOT_FOUND("pv.nfs.path.not.found"),
    PERSISTENT_VOLUME_NFS_SERVER_NOT_FOUND("pv.nfs.server.not.found"),
    PERSISTENT_VOLUME_NFS_SERVER_NOT_IP("pv.nfs.server.not.ip"),
    PERSISTENT_VOLUME_HOSTPATH_PATH_NOT_FOUND("pv.hostpath.path.not.found"),
    PERSISTENT_VOLUME_LOCAL_PATH_NOT_FOUND("pv.local.path.not.found"),
    PERSISTENT_VOLUME_LOCAL_PATH_NODE_AFFINITY_NOT_FOUND("pv.local.nodeName.not.found"),
    PERSISTENT_VOLUME_LOCAL_PATH_NODE_NAME_MORE_THAN_ONE("pv.local.nodeName.count.more.than.one"),
    PERSISTENT_VOLUME_UNMODIFIED("pv.is.unmodified"),
    OBJECT_EXIST("object.exist"),
    FILE_NOT_YAML("error.file.not.yaml.format"),
    DUPLICATED_CLUSTER_COMPONENT("duplicated.cluster.component"),
    RESOURCE_TYPE_NOT_SUPPORTED("resource.type.not.supported");

    private String error;

    GitOpsObjectError(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

}