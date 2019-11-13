import React, { useCallback, Fragment, useEffect } from 'react';
import { TabPage, Content, Header, Breadcrumb, Permission } from '@choerodon/boot';
import { Modal, Button } from 'choerodon-ui/pro';
import { FormattedMessage } from 'react-intl';
import { withRouter } from 'react-router-dom';
import { observer } from 'mobx-react-lite';
import { useRepositoryStore } from './stores';
import RepositoryForm from './repository-form';

import './index.less';

const modalKey = Modal.key();
const modalStyle = {
  width: 380,
};

const Repository = withRouter(observer((props) => {
  const {
    intl: { formatMessage },
    AppState: { currentMenuType: { organizationId } },
    intlPrefix,
    prefixCls,
    permissions,
    homeDs,
    detailDs,
    repositoryStore,
  } = useRepositoryStore();

  function refresh() {
    homeDs.query();
  }

  async function openModal() {
    await detailDs.query();
    Modal.open({
      key: modalKey,
      style: modalStyle,
      drawer: true,
      title: formatMessage({ id: intlPrefix }),
      children: <RepositoryForm
        record={detailDs.current}
        dataSet={detailDs}
        store={repositoryStore}
        id={organizationId}
        intlPrefix={intlPrefix}
        prefixCls={prefixCls}
        refresh={refresh}
      />,
      okText: formatMessage({ id: 'save' }),
    });
  }

  return (
    <TabPage
      service={permissions}
    >
      <Header>
        <Permission
          service={['devops-service.devops-organization-config.create']}
        >
          <Button
            icon="mode_edit"
            color="primary"
            funcType="flat"
            onClick={openModal}
          >
            <FormattedMessage id={intlPrefix} />
          </Button>
        </Permission>
      </Header>
      <Breadcrumb />
      <Content className={`${prefixCls}-home`}>
        <div className={`${prefixCls}-home-item`}>
          <span className={`${prefixCls}-home-item-text`}>
            {formatMessage({ id: `${intlPrefix}.harbor` })}
          </span>
          <span>
            {homeDs.current && homeDs.current.get('harborConfigUrl')
              ? homeDs.current.get('harborConfigUrl')
              : formatMessage({ id: `${intlPrefix}.harbor.default` })}
          </span>
        </div>
        <div>
          <span className={`${prefixCls}-home-item-text`}>
            {formatMessage({ id: `${intlPrefix}.chart` })}
          </span>
          <span>
            {homeDs.current && homeDs.current.get('chartConfigUrl')
              ? homeDs.current.get('chartConfigUrl')
              : formatMessage({ id: `${intlPrefix}.chart.default` })}
          </span>
        </div>
      </Content>
    </TabPage>
  );
}));

export default Repository;
