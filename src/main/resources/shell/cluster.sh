helm upgrade --install --create-namespace \
    {NAME} \
    --repo={REPOURL} \
    --namespace=choerodon \
    --version={VERSION} \
    --set config.connect={SERVICEURL} \
    --set config.token={TOKEN} \
    --set config.email={EMAIL} \
    --set-string config.clusterId={CLUSTERID} \
    --set config.choerodonId={CHOERODONID} \
    --set rbac.create=true \
    choerodon-cluster-agent