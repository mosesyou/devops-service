import React, { Component, Fragment } from 'react';
import { withRouter } from 'react-router-dom';
import { observer } from 'mobx-react';
import { injectIntl, FormattedMessage } from 'react-intl';
import { Page, Header, Content, stores, Breadcrumb } from '@choerodon/boot';
import { Select, Button, Popover } from 'choerodon-ui';
import _ from 'lodash';
import moment from 'moment';
import ChartSwitch from '../Component/ChartSwitch';
import LineChart from './LineChart';
import CommitHistory from './CommitHistory';
import TimePicker from '../Component/TimePicker';
import NoChart from '../Component/NoChart';
import MaxTagPopover from '../Component/MaxTagPopover';
import './Submission.less';
import '../../main.less';
import LoadingBar from '../../../components/loading';

/**
 * 将数据转为图表可用格式
 * @param data
 * @returns {{total, user: Array}}
 */
function formatData(data) {
  const { totalCommitsDate, commitFormUserVOList } = data;
  const total = {};
  const user = [];
  if (totalCommitsDate && commitFormUserVOList) {
    // total.items = _.countBy(totalCommitsDate, item => item.slice(0, 10));
    total.items = totalCommitsDate.slice();
    total.count = totalCommitsDate.length;
    _.forEach(commitFormUserVOList, (item) => {
      const { name, imgUrl, commitDates, id } = item;
      const userTotal = {
        name,
        avatar: imgUrl,
      };
      userTotal.id = id;
      // userTotal.items = _.countBy(commitDates, cit => cit.slice(0, 10));
      userTotal.items = commitDates.slice();
      userTotal.count = commitDates.length;
      user.push(userTotal);
    });
  }

  return {
    total,
    user,
  };
}

const { AppState } = stores;
const { Option } = Select;

@observer
class Submission extends Component {
  constructor(props) {
    super(props);
    this.state = {
      appId: null,
      page: 1,
      dateType: 'seven',
    };
  }

  componentDidMount() {
    const { ReportsStore } = this.props;
    ReportsStore.changeIsRefresh(true);
    this.loadData();
  }

  componentWillUnmount() {
    const { ReportsStore } = this.props;
    ReportsStore.setAllApps([]);
    ReportsStore.setCommits({});
    ReportsStore.setCommitsRecord([]);
    ReportsStore.setStartTime(moment().subtract(6, 'days'));
    ReportsStore.setEndTime(moment());
    ReportsStore.setStartDate(null);
    ReportsStore.setEndDate(null);
  }

  handleRefresh = () => this.loadData();


  /**
   * 应用选择
   * @param e
   */
  handleSelect = (e) => {
    const {
      ReportsStore: {
        loadCommits,
        loadCommitsRecord,
        getStartTime,
        getEndTime,
      },
    } = this.props;
    const { id: projectId } = AppState.currentMenuType;
    const startTime = getStartTime
      .format()
      .split('T')[0]
      .replace(/-/g, '/');
    const endTime = getEndTime
      .format()
      .split('T')[0]
      .replace(/-/g, '/');
    this.setState({ appId: e });
    loadCommits(projectId, startTime, endTime, e);
    loadCommitsRecord(projectId, startTime, endTime, e, 1);
  };

  handlePageChange = (page) => {
    const {
      ReportsStore: { loadCommitsRecord, getStartTime, getEndTime },
    } = this.props;
    const { appId } = this.state;
    const { id: projectId } = AppState.currentMenuType;
    const startTime = getStartTime
      .format()
      .split('T')[0]
      .replace(/-/g, '/');
    const endTime = getEndTime
      .format()
      .split('T')[0]
      .replace(/-/g, '/');
    this.setState({ page });
    loadCommitsRecord(projectId, startTime, endTime, appId, page);
  };

  loadData = () => {
    const {
      ReportsStore: {
        loadAllApps,
        loadCommits,
        loadCommitsRecord,
        getStartTime,
        getEndTime,
        changeIsRefresh,
      },
      ReportsStore,
      history: {
        location: { state },
      },
    } = this.props;
    let repoAppId = [];
    if (state && state.appId) {
      repoAppId = state.appId;
    }
    const { id: projectId } = AppState.currentMenuType;
    const { page, appId, dateType } = this.state;
    const startTime = getStartTime
      .format()
      .split('T')[0]
      .replace(/-/g, '/');
    const endTime = getEndTime
      .format()
      .split('T')[0]
      .replace(/-/g, '/');
    ReportsStore.changeIsRefresh(true);
    loadAllApps(projectId).then((data) => {
      ReportsStore.changeIsRefresh(false);
      const appData = data;
      if (appData.length) {
        let selectApp = appId || _.map(appData, (item) => item.id);
        if (!appId) {
          if (repoAppId.length) {
            selectApp = repoAppId;
          }
          this.setState({ appId: selectApp });
        }
        loadCommits(projectId, startTime, endTime, selectApp);
        loadCommitsRecord(projectId, startTime, endTime, selectApp, page);
      }
    });
  };

  handleRefreshChartByTimePicker() {
    const {
      ReportsStore: {
        loadCommits,
        loadCommitsRecord,
        getStartTime,
        getEndTime,
      },
    } = this.props;
    const { id: projectId } = AppState.currentMenuType;
    const startTime = getStartTime
      .format()
      .split('T')[0]
      .replace(/-/g, '/');
    const endTime = getEndTime
      .format()
      .split('T')[0]
      .replace(/-/g, '/');
    loadCommits(projectId, startTime, endTime, this.state.appId);
    loadCommitsRecord(projectId, startTime, endTime, this.state.appId, this.state.index);
  }


  /**
   * 选择今天、近7天和近30天的选项，当使用DatePick的时候清空type
   * @param type 时间范围类型
   */
  handleDateChoose = (type) => this.setState({ dateType: type });

  maxTagNode = (data, value) => (
    <MaxTagPopover dataSource={data} value={value} />
  );

  render() {
    const {
      intl: { formatMessage },
      history,
      ReportsStore,
    } = this.props;
    const {
      location: { state, search },
    } = history;
    const backPath = state && state.backPath;
    const {
      getCommits,
      getStartTime,
      getEndTime,
      getAllApps,
      getCommitsRecord,
      getIsRefresh,
      getCommitLoading,
      getStartDate,
      getEndDate,
      getHistoryLoad,
    } = ReportsStore;
    const { id, name, type, organizationId } = AppState.currentMenuType;
    const { appId, dateType } = this.state;
    const { total, user } = formatData(getCommits);
    const options = _.map(getAllApps, (item) => (
      <Option key={item.id} value={item.id}>
        {item.name}
      </Option>
    ));
    const personChart = _.map(user, (item) => (
      <div key={item.id} className="c7n-report-submission-item">
        <LineChart
          languageType="report"
          loading={getCommitLoading}
          name={item.name || 'Unknown'}
          color="#ff9915"
          style={{ width: '100%', height: 176 }}
          data={item}
          start={getStartTime}
          end={getEndTime}
          hasAvatar
        />
      </div>
    ));

    const content = getAllApps.length ? (
      <Fragment>
        <div className="c7n-report-control c7n-report-select">
          <Select
            className=" c7n-report-control-select"
            mode="multiple"
            label={formatMessage({ id: 'chooseApp' })}
            placeholder={formatMessage({ id: 'report.app.noselect' })}
            maxTagCount={3}
            value={appId || []}
            maxTagPlaceholder={this.maxTagNode.bind(this, getAllApps)}
            onChange={this.handleSelect}
            optionFilterProp="children"
            filter
          >
            {options}
          </Select>
          <TimePicker
            unlimit
            startTime={getStartDate}
            endTime={getEndDate}
            func={this.handleRefreshChartByTimePicker}
            store={ReportsStore}
            type={dateType}
            onChange={this.handleDateChoose}
          />
        </div>

        <div className="c7n-report-submission clearfix">
          <div className="c7n-report-submission-overview">
            <LineChart
              languageType="report"
              loading={getCommitLoading}
              name="提交情况"
              color="#4677dd"
              style={{ width: '100%', height: 276 }}
              data={total}
              hasAvatar={false}
              start={getStartTime}
              end={getEndTime}
            />
          </div>
          <div className="c7n-report-submission-history">
            <CommitHistory
              loading={getHistoryLoad}
              onPageChange={this.handlePageChange}
              dataSource={getCommitsRecord}
            />
          </div>
        </div>
        <div className="c7n-report-submission-wrap clearfix">{personChart}</div>
      </Fragment>
    ) : (
      <NoChart type="app" />
    );

    return (
      <Page
        className="c7n-region"
        service={[
          'devops-service.application.listByActive',
          'devops-service.devops-gitlab-commit.getCommits',
          'devops-service.devops-gitlab-commit.getRecordCommits',
        ]}
      >
        <Header
          title={formatMessage({ id: 'report.submission.head' })}
          backPath={
            backPath
            || `/charts${search}`
          }
        >
          <ChartSwitch history={history} current="submission" />
          <Button icon="refresh" onClick={this.handleRefresh}>
            <FormattedMessage id="refresh" />
          </Button>
        </Header>
        <Breadcrumb title={formatMessage({ id: 'report.submission.head' })} />
        <Content>
          {getIsRefresh ? <LoadingBar display="getIsRefresh" /> : content}
        </Content>
      </Page>
    );
  }
}

export default withRouter(injectIntl(Submission));
