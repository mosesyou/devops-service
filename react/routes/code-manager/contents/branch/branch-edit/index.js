import React, { Component } from 'react';
import { withRouter } from 'react-router-dom';
import { observer } from 'mobx-react';
import { Modal, Form, Select, Tooltip } from 'choerodon-ui';
import { stores, Content, Choerodon } from '@choerodon/boot';
import { injectIntl, FormattedMessage } from 'react-intl';
import _ from 'lodash';
import '../../../../main.less';
import '../branch-create/index.less';
import '../index.less';
import MouserOverWrapper from '../../../../../components/MouseOverWrapper';
import DevPipelineStore from '../../../stores/DevPipelineStore';
import InterceptMask from '../../../../../components/intercept-mask';

const { AppState } = stores;
const Sidebar = Modal.Sidebar;
const { Option } = Select;
const FormItem = Form.Item;
const formItemLayout = {
  labelCol: {
    xs: { span: 24 },
    sm: { span: 100 },
  },
  wrapperCol: {
    xs: { span: 24 },
    sm: { span: 26 },
  },
};

@Form.create()
@withRouter
@injectIntl
@observer
class BranchEdit extends Component {
  constructor(props) {
    const menu = AppState.currentMenuType;
    super(props);
    this.state = {
      projectId: menu.id,
      submitting: false,
      searchInput: '',
    };
  }

  /**
   * 获取issue的正文
   * @param s
   * @returns {*}
   */
  getOptionContent =(s) => {
    const { formatMessage } = this.props.intl;
    let mes = '';
    let icon = '';
    let color = '';
    switch (s.typeCode) {
      case 'story':
        mes = formatMessage({ id: 'branch.issue.story' });
        icon = 'agile_story';
        color = '#00bfa5';
        break;
      case 'bug':
        mes = formatMessage({ id: 'branch.issue.bug' });
        icon = 'agile_fault';
        color = '#f44336';
        break;
      case 'issue_epic':
        mes = formatMessage({ id: 'branch.issue.epic' });
        icon = 'agile_epic';
        color = '#743be7';
        break;
      case 'sub_task':
        mes = formatMessage({ id: 'branch.issue.subtask' });
        icon = 'agile_subtask';
        color = '#4d90fe';
        break;
      default:
        mes = formatMessage({ id: 'branch.issue.task' });
        icon = 'agile_task';
        color = '#4d90fe';
    }
    return (<span>
      <div style={{ color, marginRight: 5 }} className="branch-issue"><i className={`icon icon-${icon}`} /></div>
      <span className="branch-issue-content">
        <span style={{ color: 'rgb(0,0,0,0.65)' }}>{s.issueNum}</span>
        <MouserOverWrapper style={{ display: 'inline-block', verticalAlign: 'sub' }} width="350px" text={s.summary}>{s.summary}</MouserOverWrapper>
      </span>
    </span>);
  };

  /**
   * 提交分支数据
   * @param e
   */
  handleOk = (e) => {
    e.preventDefault();
    // const { store, isDevConsole } = this.props;
    const { store } = this.props;
    const appId = DevPipelineStore.selectedApp;
    const { projectId } = this.state;
    let isModify = false;
    const issueId = this.props.form.getFieldValue('issueId');
    if (!store.branch.issueId && (issueId && typeof issueId === 'number')) {
      isModify = true;
    }
    this.props.form.validateFieldsAndScroll((err, data, modify) => {
      if ((!err && modify) || (isModify && !err)) {
        data.branchName = store.branch.branchName;
        data.appServiceId = DevPipelineStore.getSelectApp;
        data.objectVersionNumber = store.branch.objectVersionNumber;
        this.setState({ submitting: true });
        store.updateBranchByName(projectId, appId, data)
          .then(() => {
            store.loadBranchData({ projectId });
            this.props.onClose();
            this.props.form.resetFields();
            this.setState({ submitting: false });
          })
          .catch((error) => {
            Choerodon.handleResponseError(error);
            this.setState({ submitting: false });
          });
      } else if (!modify) {
        this.props.form.resetFields();
        this.setState({ submitting: false });
        this.props.onClose();
      }
    });
  };

  /**
   * 关闭弹框
   */
  handleClose = () => {
    this.props.form.resetFields();
    this.props.onClose(false);
  };

  /**
   * 搜索issue
   * @param input
   * @param options
   */
  searchIssue = (input, options) => {
    const { store } = this.props;
    const { searchInput } = this.state;
    if (input !== '') {
      store.loadIssue(this.state.projectId, input, false);
    } else if (searchInput && input === '') {
      store.loadIssue(this.state.projectId, '', true);
    }
    this.setState({ searchInput: input });
  };


  render() {
    const { visible, store, form: { getFieldDecorator }, name } = this.props;
    const issueInitValue = store.issueInitValue;
    const issue = store.issue.slice();
    const branch = store.branch;
    let issueId = branch && branch.issueId;
    if (branch && !issueId && issueInitValue && issue.length) {
      const issues = _.filter(issue, (i) => i.issueNum === issueInitValue);
      if (issues.length) {
        issueId = issues[0].issueId;
      }
    }
    // 如果 issue列表为空的时候默认显示原始issue
    if (issue.length === 0 && store.currentBranchIssue) {
      issueId = this.getOptionContent(store.currentBranchIssue);
    }
    return (
      <Sidebar
        title={<FormattedMessage id="branch.edit" />}
        visible={visible}
        onOk={this.handleOk}
        onCancel={this.handleClose}
        okText={<FormattedMessage id="save" />}
        cancelText={<FormattedMessage id="cancel" />}
        confirmLoading={this.state.submitting}
        width={740}
      >
        <Content className="sidebar-content c7n-createBranch">
          <Form layout="vertical" onSubmit={this.handleOk} className="c7n-sidebar-form">
            <FormItem
              className="branch-formItem"
              {...formItemLayout}
            >
              {getFieldDecorator('issueId', {
                initialValue: issueId,
              })(
                <Select
                  dropdownClassName="createBranch-dropdown"
                  onFilterChange={this.searchIssue}
                  loading={store.issueLoading}
                  key="service"
                  allowClear
                  label={<FormattedMessage id="branch.issueName" />}
                  filter
                  dropdownMatchSelectWidth
                  onSelect={this.selectTemplate}
                  size="default"
                  optionFilterProp="children"
                  filterOption={false}
                >
                  {issue.map((s) => (
                    <Option
                      key={s.issueId}
                      value={s.issueId}
                      title={s.summary}
                    >
                      {this.getOptionContent(s)}
                    </Option>
                  ))}
                </Select>,
              )}
            </FormItem>
          </Form>
          <InterceptMask visible={this.state.submitting} />
        </Content>
      </Sidebar>
    );
  }
}
export default BranchEdit;
