import React, { Fragment, useMemo, useCallback, useEffect, useState } from 'react';
import { observer } from 'mobx-react-lite';
import { Modal } from 'choerodon-ui/pro';
import { Button } from 'choerodon-ui';
import { FormattedMessage } from 'react-intl';
import HeaderButtons from '../../../../../../components/header-buttons';
import { useResourceStore } from '../../../../stores';
import { useModalStore } from './stores';
import { useCertDetailStore } from '../stores';
import Detail from './certificate-detail';

const modalStyle = {
  width: 380,
};

const modalKey1 = Modal.key();

const CustomModals = observer(() => {
  const {
    intlPrefix,
    prefixCls,
    intl: { formatMessage },
    treeDs,
  } = useResourceStore();
  const {
    detailDs,
  } = useCertDetailStore();
  const {
    permissions,
  } = useModalStore();

  function refresh() {
    treeDs.query();
    detailDs.query();
  }

  function openDetail() {
    Modal.open({
      key: modalKey1,
      title: formatMessage({ id: 'ctf.detail' }),
      children: <Detail record={detailDs.current} intlPrefix={intlPrefix} prefixCls={prefixCls} formatMessage={formatMessage} />,
      drawer: true,
      style: modalStyle,
      okText: formatMessage({ id: 'close' }),
      okCancel: false,
    });
  }

  const buttons = useMemo(() => ([{
    name: formatMessage({ id: 'ctf.detail' }),
    icon: 'find_in_page',
    handler: openDetail,
    display: true,
    group: 1,
    service: permissions,
  }, {
    name: formatMessage({ id: 'refresh' }),
    icon: 'refresh',
    handler: refresh,
    display: true,
    group: 1,
  }]), [formatMessage, intlPrefix, permissions, refresh]);

  return <HeaderButtons items={buttons} />;
});

export default CustomModals;
