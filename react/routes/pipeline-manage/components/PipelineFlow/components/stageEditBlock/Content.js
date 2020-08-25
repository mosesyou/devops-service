import React, { useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import { Spin } from 'choerodon-ui';
import EditColumn from '../eidtColumn';
import { usePipelineStageEditStore } from './stores';
import Loading from '../../../../../../components/loading';

const defaultData = [
  {
    name: '构建',
    sequence: 1,
    jobList: [],
    type: 'CI',
    parallel: 1,
    triggerType: '',
  }, {
    name: '部署',
    sequence: 2,
    jobList: [],
    type: 'CD',
    parallel: 0,
    triggerType: 'auto',
  },
];

export default observer(() => {
  const {
    projectId,
    pipelineId,
    editBlockStore,
    stepStore,
    edit,
    appServiceId,
    appServiceName,
    appServiceCode,
    appServiceType,
    image,
    dataSource: propsDataSource,
  } = usePipelineStageEditStore();

  const {
    setStepData,
    getStepData,
    getStepData2,
    getLoading,
  } = editBlockStore || stepStore;

  useEffect(() => {
    let stageList = [];
    if (appServiceId && appServiceType === 'test') {
      stageList = [...defaultData.slice(0, 1)];
    } else {
      stageList = [...defaultData];
    }
    if (propsDataSource) {
      stageList = [...propsDataSource.stageList];
    }
    setStepData(stageList, edit);
  }, [appServiceId]);

  function renderColumn() {
    const dataSource = edit ? getStepData2 : getStepData;
    if (dataSource && dataSource.length > 0) {
      return dataSource.map((item, index) => {
        const nextStageType = dataSource[index + 1]?.type && dataSource[index + 1]?.type.toUpperCase();
        return (<EditColumn
          {...item}
          columnIndex={index}
          key={item.id}
          isLast={String(index) === String(dataSource.length - 1)}
          isFirst={index === 0}
          nextStageType={nextStageType}
          edit={edit}
          pipelineId={pipelineId}
          appServiceId={appServiceId}
          appServiceName={appServiceName}
          appServiceCode={appServiceCode}
          appServiceType={appServiceType}
          image={image}
        />);
      });
    }
  }

  function renderBlock() {
    if (edit) {
      return (
        <div className="c7n-piplineManage-edit">
          {renderColumn()}
        </div>
      );
    } else {
      return (
        !getLoading && !edit ? <div className="c7n-piplineManage-edit">
          {renderColumn()}
        </div> : <Loading display={getLoading} />
      );
    }
  }

  return renderBlock();
});
