import { observable, action, computed } from 'mobx';
import { axios, store, stores, Choerodon } from '@choerodon/boot';
import _ from 'lodash';
import { handlePromptError } from '../../../utils';
import DeploymentPipelineStore from './DeploymentPipelineStore';

const { AppState } = stores;

function findDataIndex(collection, value) {
  return collection && value ? collection.findIndex(
    ({ id, projectId }) => id === value.id && (
      (!projectId && !value.projectId)
      || projectId === value.projectId
    ),
  ) : -1;
}

/**
 * 置顶最近使用过的数据项
 * @param collection 所有数据
 * @param value 当前数据项
 * @param number 显示最近使用的条数
 * @returns {*[]}
 */
function saveRecent(collection = [], value, number) {
  const index = findDataIndex(collection, value);
  if (index !== -1) {
    return collection.splice(index, 1).concat(collection.slice());
  } else {
    collection.unshift(value);
    return collection.slice(0, number);
  }
}

@store('DevPipelineStore')
class DevPipelineStore {
  @observable appData = [];

  @observable selectedApp = null;

  @observable defaultAppName = null;

  @observable recentApp = null;

  @observable preProId = AppState.currentMenuType.id;

  @observable loading = false;

  @observable selectAppData = {};

  @computed get getSelectAppData() {
    return this.selectAppData;
  }

  @action setAppData(data) {
    this.appData = data;
  }

  @computed get getAppData() {
    return this.appData.slice();
  }

  @action setSelectApp(app) {
    this.selectedApp = app;
    if (app) {
      this.selectAppData = _.find(this.appData, (item) => item.id === app);
    }
  }

  @action setPreProId(id) {
    this.preProId = id;
  }

  @computed get getSelectApp() {
    return this.selectedApp;
  }

  @action setDefaultAppName(name) {
    this.defaultAppName = name;
  }

  @computed get getDefaultAppName() {
    return this.defaultAppName;
  }

  @action setLoading(value) {
    this.loading = value;
  }

  @computed get getLoading() {
    return this.loading;
  }

  @computed
  get getRecentApp() {
    let recents = [];
    if (this.recentApp) {
      recents = this.recentApp;
    } else if (localStorage.recentApp) {
      recents = JSON.parse(localStorage.recentApp);
    }
    const permissionApp = _.filter(
      this.appData,
      (value) => value.permission === true
    );
    return _.filter(
      permissionApp,
      (value) => findDataIndex(recents, value) !== -1
    );
  }

  @action
  setRecentApp(id) {
    if (id) {
      if (this.appData.length) {
        const recent = this.appData.filter((value) => value.id === id)[0];
        const recentApp = saveRecent(this.getRecentApp, recent, 3);
        localStorage.recentApp = JSON.stringify(recentApp);
        this.recentApp = recentApp;
      } else {
        localStorage.recentApp = JSON.stringify([id]);
        this.recentApp = [id];
      }
    }
  }

  /**
   * 查询该项目下的所有应用
   */
  queryAppData = (projectId = AppState.currentMenuType.id, type, refersh, isReloadApp) => {
    // 已经加载过app数据 只更新对应模块的数据， 除非主动刷新 否则不查询app数据,
    if (!isReloadApp && this.appData.length !== 0 && type !== 'CodeManagerBranch') {
      refersh && refersh();
      return;
    }
    if (Number(this.preProId) !== Number(projectId)) {
      DeploymentPipelineStore.setProRole('app', '');
    }
    this.setAppData([]);
    this.setPreProId(projectId);
    this.setLoading(true);
    axios.get(`/devops/v1/projects/${projectId}/app_service/list_by_active`)
      .then((data) => {
        this.setLoading(false);
        if (handlePromptError(data)) {
          this.setAppData(data);
          if (data && data.length) {
            if (this.selectedApp) {
              if (_.filter(data, ['id', this.selectedApp]).length === 0) {
                this.setSelectApp(data[0].id);
              }
            } else {
              this.setSelectApp(data[0].id);
            }
            refersh && refersh();
          } else {
            this.setSelectApp(null);
          }
        }
      }).catch((err) => Choerodon.handleResponseError(err));
  };
}

const devPipelineStore = new DevPipelineStore();
export default devPipelineStore;
