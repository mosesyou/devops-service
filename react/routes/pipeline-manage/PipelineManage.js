import React, { Fragment, useRef, useState, Suspense, useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import { Page, Header, Breadcrumb, Content, Permission } from '@choerodon/boot';
import { Button, Modal } from 'choerodon-ui/pro';
import { axios, Choerodon } from '@choerodon/boot';
import { Prompt, Router } from 'react-router-dom';
import { handlePromptError } from '../../utils';
import PipelineTree from './components/PipelineTree';
import PipelineFlow from './components/PipelineFlow';
import DragBar from '../../components/drag-bar';
import PipelineCreate from './components/PipelineCreate';
import RecordDetail from './components/record-detail';
import EmptyPage from '../../components/empty-page';
import { usePipelineManageStore } from './stores';
import HeaderButtons from '../../components/header-buttons';
import VariableSettings from './components/variable-settings';
import AuditModal from './components/audit-modal';
import GitlabRunner from './components/gitlab-runner';

import './index.less';
import MouserOverWrapper from '../../components/MouseOverWrapper';

const recordDetailKey = Modal.key();
const settingsKey = Modal.key();
const auditKey = Modal.key();
const runnerKey = Modal.key();
const modalStyle = {
  width: 380,
};
const settingsModalStyle = {
  width: 740,
};

const PipelineManage = observer((props) => {
  const {
    intl: { formatMessage },
    intlPrefix,
    prefixCls,
    permissions,
    mainStore,
    editBlockStore,
    detailStore,
    detailStore: {
      loadDetailData, getDetailData,
    },
    editBlockStore: {
      getMainData, loadData, getHasModify, setHasModify,
    },
    treeDs,
    projectId,
  } = usePipelineManageStore();

  const handleCreatePipeline = () => {
    Modal.open({
      key: Modal.key(),
      title: '创建流水线',
      style: {
        width: 'calc(100vw - 3.52rem)',
      },
      drawer: true,
      children: <PipelineCreate mathRandom={Math.random()} refreshTree={handleRefresh} editBlockStore={editBlockStore} mainStore={mainStore} />,
      okText: '创建',
    });
  };

  const rootRef = useRef(null);

  const { getSelectedMenu } = mainStore;

  async function handleRefresh() {
    setHasModify(false, false);
    await treeDs.query();
    const { id } = getMainData;
    const { parentId } = getSelectedMenu;
    const { gitlabPipelineId, devopsPipelineRecordRelId } = getDetailData;
    if (!parentId) {
      id && loadData(projectId, id);
    } else {
      devopsPipelineRecordRelId && loadDetailData(projectId, devopsPipelineRecordRelId);
    }
  }

  function openEditModal() {
    Modal.open({
      key: Modal.key(),
      title: '修改流水线',
      style: {
        width: 'calc(100vw - 3.52rem)',
      },
      drawer: true,
      children: <PipelineCreate
        dataSource={editBlockStore.getMainData}
        refreshTree={handleRefresh}
        editBlockStore={editBlockStore}
      />,
      okText: formatMessage({ id: 'save' }),
    });
  }

  function openRecordDetail() {
    const { devopsPipelineRecordRelId } = getSelectedMenu;
    const { devopsPipelineRecordRelId: detailDevopsPipelineRecordRelId } = getDetailData;
    const newDevopsPipelineRecordRelId = devopsPipelineRecordRelId || detailDevopsPipelineRecordRelId;
    Modal.open({
      key: recordDetailKey,
      style: modalStyle,
      title: <span className={`${prefixCls}-detail-modal-title`}>
        流水线记录“
        <MouserOverWrapper width="100px" text={`#${newDevopsPipelineRecordRelId}`}>
          <span>{`#${newDevopsPipelineRecordRelId}`}</span>
        </MouserOverWrapper>
        ”的详情
      </span>,
      children: <RecordDetail
        pipelineRecordId={newDevopsPipelineRecordRelId}
        intlPrefix={intlPrefix}
        refresh={handleRefresh}
        store={mainStore}
      />,
      drawer: true,
      okCancel: false,
      okText: formatMessage({ id: 'close' }),
    });
  }

  async function changeRecordExecute(type) {
    const { gitlabProjectId, gitlabPipelineId, devopsPipelineRecordRelId } = getSelectedMenu;
    const { gitlabProjectId: detailGitlabProjectId, gitlabPipelineId: detailGitlabPipelineId, devopsPipelineRecordRelId: detailDevopsPipelineRecordRelId } = getDetailData;
    const res = await mainStore.changeRecordExecute({
      projectId,
      gitlabProjectId: gitlabProjectId || detailGitlabProjectId,
      recordId: gitlabPipelineId || detailGitlabPipelineId,
      type,
      devopsPipelineRecordRelId: devopsPipelineRecordRelId || detailDevopsPipelineRecordRelId,
    });
    if (res) {
      handleRefresh();
    }
  }

  function openAuditModal() {
    const { devopsCdPipelineDeatilVO, parentId } = getSelectedMenu;
    const { cdRecordId, devopsCdPipelineDeatilVO: detailDevopsCdPipelineDeatilVO, pipelineName } = getDetailData;
    Modal.open({
      key: auditKey,
      title: formatMessage({ id: `${intlPrefix}.execute.audit` }),
      children: <AuditModal
        cdRecordId={cdRecordId}
        name={pipelineName}
        mainStore={mainStore}
        onClose={handleRefresh}
        checkData={devopsCdPipelineDeatilVO || detailDevopsCdPipelineDeatilVO}
      />,
      movable: false,
    });
  }

  function openSettingsModal(type) {
    const { id } = getMainData;
    const { appServiceId, appServiceName } = getSelectedMenu;
    Modal.open({
      key: settingsKey,
      style: settingsModalStyle,
      title: formatMessage({ id: `${intlPrefix}.settings.${type}` }),
      children: <VariableSettings
        intlPrefix={intlPrefix}
        appServiceId={type === 'global' ? null : appServiceId}
        appServiceName={type === 'global' ? null : appServiceName}
        store={mainStore}
        refresh={handleRefresh}
      />,
      drawer: true,
      okText: formatMessage({ id: 'save' }),
    });
  }

  function openRunnerModal() {
    Modal.open({
      key: runnerKey,
      style: settingsModalStyle,
      title: formatMessage({ id: `${intlPrefix}.gitlab.runner` }),
      children: <GitlabRunner />,
      drawer: true,
      okCancel: false,
      okText: formatMessage({ id: 'close' }),
    });
  }

  function getHeaderButtons() {
    const { parentId, status, devopsCdPipelineDeatilVO } = getSelectedMenu;
    const { status: detailStatus, devopsCdPipelineDeatilVO: detailDevopsCdPipelineDeatilVO } = getDetailData;
    const buttons = [{
      permissions: ['choerodon.code.project.develop.ci-pipeline.ps.create'],
      name: formatMessage({ id: `${intlPrefix}.create` }),
      icon: 'playlist_add',
      handler: handleCreatePipeline,
      display: true,
      group: 1,
    }, {
      permissions: ['choerodon.code.project.develop.ci-pipeline.ps.variable.project'],
      name: formatMessage({ id: `${intlPrefix}.settings.global` }),
      icon: 'settings-o',
      handler: () => openSettingsModal('global'),
      display: true,
      group: 1,
    }, {
      permissions: ['choerodon.code.project.develop.ci-pipeline.ps.runner'],
      name: formatMessage({ id: `${intlPrefix}.gitlab.runner` }),
      icon: 'find_in_page-o',
      handler: openRunnerModal,
      display: true,
      group: 1,
    }];
    if (treeDs.length && treeDs.status === 'ready') {
      if (!parentId) {
        buttons.push({
          permissions: ['choerodon.code.project.develop.ci-pipeline.ps.update'],
          name: formatMessage({ id: 'edit' }),
          icon: 'mode_edit',
          handler: openEditModal,
          display: true,
          group: 2,
        }, {
          permissions: ['choerodon.code.project.develop.ci-pipeline.ps.variable.app'],
          name: formatMessage({ id: `${intlPrefix}.settings.local` }),
          icon: 'settings-o',
          handler: () => openSettingsModal('local'),
          display: true,
          group: 2,
        });
      } else {
        const newStatus = status || detailStatus;
        const newDevopsCdPipelineDeatilVO = devopsCdPipelineDeatilVO || detailDevopsCdPipelineDeatilVO;
        buttons.push({
          name: formatMessage({ id: `${intlPrefix}.record.detail` }),
          icon: 'find_in_page-o',
          handler: openRecordDetail,
          display: true,
          group: 2,
        }, {
          permissions: ['choerodon.code.project.develop.ci-pipeline.ps.cancel'],
          name: formatMessage({ id: `${intlPrefix}.execute.cancel` }),
          icon: 'power_settings_new',
          handler: () => changeRecordExecute('cancel'),
          display: newStatus === 'pending' || newStatus === 'running',
          group: 2,
        }, {
          permissions: ['choerodon.code.project.develop.ci-pipeline.ps.retry'],
          name: formatMessage({ id: `${intlPrefix}.execute.retry` }),
          icon: 'refresh',
          handler: () => changeRecordExecute('retry'),
          display: newStatus === 'failed' || newStatus === 'canceled',
          group: 2,
        }, {
          permissions: ['choerodon.code.project.develop.ci-pipeline.ps.audit'],
          name: formatMessage({ id: `${intlPrefix}.execute.audit` }),
          icon: 'authorize',
          handler: openAuditModal,
          display: newStatus === 'not_audit' && newDevopsCdPipelineDeatilVO && newDevopsCdPipelineDeatilVO.execute,
          group: 2,
        });
      }
    }
    buttons.push({
      name: formatMessage({ id: 'refresh' }),
      icon: 'refresh',
      handler: handleRefresh,
      display: true,
      group: 2,
    });
    return buttons;
  }

  return (
    <Page service={permissions} className="pipelineManage_page">
      <Header title="流水线">
        <HeaderButtons items={getHeaderButtons()} showClassName={false} />
      </Header>
      <Breadcrumb />
      <Content className={`${prefixCls}-content`}>
        {!treeDs.length && treeDs.status === 'ready' ? <div className={`${prefixCls}-wrap`}>
          <Suspense fallback={<span />}>
            <EmptyPage
              title={formatMessage({ id: 'empty.title.pipeline' })}
              describe={formatMessage({ id: 'empty.tips.pipeline.owner' })}
              btnText={formatMessage({ id: `${intlPrefix}.create` })}
              onClick={handleCreatePipeline}
              access
            />
          </Suspense>
        </div> : (<div
          ref={rootRef}
          className={`${prefixCls}-wrap`}
        >
          <DragBar
            parentRef={rootRef}
            store={mainStore}
          />
          <PipelineTree handleRefresh={handleRefresh} />
          <div className={`${prefixCls}-main ${prefixCls}-animate`}>
            <PipelineFlow
              stepStore={editBlockStore}
              detailStore={detailStore}
              handleRefresh={handleRefresh}
              treeDs={treeDs}
              mainStore={mainStore}
            />
          </div>
        </div>
        )}
      </Content>
    </Page>
  );
});

export default PipelineManage;
