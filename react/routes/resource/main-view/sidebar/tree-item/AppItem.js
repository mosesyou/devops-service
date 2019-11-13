import React, { Fragment } from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { injectIntl } from 'react-intl';
import { Action, Choerodon } from '@choerodon/boot';
import { Icon, Modal } from 'choerodon-ui/pro';
import { handlePromptError } from '../../../../../utils';
import eventStopProp from '../../../../../utils/eventStopProp';
import { useResourceStore } from '../../../stores';
import { useTreeItemStore } from './stores';

const modalKey = Modal.key();

function AppItem({ name, record, intl: { formatMessage }, intlPrefix }) {
  const {
    treeDs,
    AppState: { currentMenuType: { id } },
  } = useResourceStore();
  const { treeItemStore } = useTreeItemStore();

  const type = record.get('type');
  async function handleClick() {
    if (!record) return;

    const envId = record.get('parentId');
    const appId = record.get('id');
    try {
      const result = await treeItemStore.removeService(id, envId, [appId]);
      if (handlePromptError(result, false)) {
        treeDs.query();
      }
    } catch (error) {
      Choerodon.handleResponseError(error);
    }
  }

  function openModal() {
    Modal.open({
      movable: false,
      closable: false,
      key: modalKey,
      title: formatMessage({ id: `${intlPrefix}.modal.service.delete` }),
      children: formatMessage({ id: `${intlPrefix}.modal.service.delete.desc` }),
      okText: formatMessage({ id: 'delete' }),
      onOk: handleClick,
    });
  }

  function getSuffix() {
    const actionData = [{
      service: ['devops-service.devops-env-app-service.delete'],
      text: formatMessage({ id: `${intlPrefix}.modal.service.delete` }),
      action: openModal,
    }];
    return <Action placement="bottomRight" data={actionData} onClick={eventStopProp} />;
  }
  function renderIcon(appType) {
    if (appType === 'normal_server') {
      return <Icon type="widgets" />;
    } else if (appType === 'share_service') {
      return <Icon type="share" />;
    } else {
      return <Icon type="application_market" />;
    }
  }
  return <Fragment>
    {renderIcon(type)}
    {name}
    {getSuffix()}
  </Fragment>;
}

AppItem.propTypes = {
  name: PropTypes.any,
};

export default injectIntl(observer(AppItem));
