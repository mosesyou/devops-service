import { observable, action, computed } from 'mobx';
import { axios, store, Choerodon } from '@choerodon/boot';
import { handlePromptError } from '../../../../../utils';
import DevPipelineStore from '../../../stores/DevPipelineStore';

@store('AppTagStore')
class AppTagStore {
  @observable tagData = [];

  // 防止初次为false时对页面的判断
  @observable loading = null;

  @observable pageInfo = {
    current: 1,
    total: 0,
    pageSize: 10,
  };

  @observable branchData = [];

  @action setTagData(data) {
    this.tagData = data;
  }

  @computed get getTagData() {
    return this.tagData;
  }

  @action setLoading(flag) {
    this.loading = flag;
  }

  @computed get getLoading() {
    return this.loading;
  }

  @action setPageInfo(pages) {
    this.pageInfo = pages;
  }

  @computed get getPageInfo() {
    return this.pageInfo;
  }

  @action setBranchData(data) {
    this.branchData = data;
  }

  @computed get getBranchData() {
    return this.branchData;
  }

  queryTagData = (projectId, page = 1, sizes = 10, postData = { searchParam: {}, param: '' }) => {
    this.setLoading(true);
    if (DevPipelineStore.selectedApp) {
      axios.post(`/devops/v1/projects/${projectId}/app_service/${DevPipelineStore.selectedApp}/git/page_tags_by_options?page=${page}&size=${sizes}`, JSON.stringify(postData))
        .then((data) => {
          this.setLoading(false);
          if (handlePromptError(data)) {
            const { list, total, pageNum, pageSize } = data;
            this.setTagData(list);
            this.setPageInfo({ current: pageNum, pageSize, total });
          }
        }).catch((err) => {
          Choerodon.handleResponseError(err);
          this.setLoading(false);
        });
    } else {
      // 增加loading效果，如觉不妥，请删除
      setTimeout(() => {
        this.setLoading(false);
      }, 600);
    }
  };

  /**
   * 查询应用下的所有分支
   * @param projectId
   * @param appId
   * @returns {Promise<T>}
   */
  queryBranchData = ({ projectId, sorter = { field: 'createDate', order: 'asc' }, postData = { searchParam: {}, param: '' }, size = 3 }) => {
    axios.post(`/devops/v1/projects/${projectId}/app_service/${DevPipelineStore.selectedApp}/git/page_branch_by_options?page=1&size=${size}`, JSON.stringify(postData)).then((data) => {
      if (handlePromptError(data)) {
        this.setBranchData(data);
      }
    }).catch((err) => Choerodon.handleResponseError(err));
  };

  /**
   * 检查标记名称的唯一性
   * @param projectId
   * @param name
   */
  checkTagName = (projectId, name) => axios.get(`/devops/v1/projects/${projectId}/app_service/${DevPipelineStore.selectedApp}/git/check_tag?tag_name=${name}`);

  /**
   * 创建tag
   * @param projectId
   * @param tag tag名称
   * @param ref 来源分支
   * @param release 发布日志
   */
  createTag = (projectId, tag, ref, release) => axios.post(`/devops/v1/projects/${projectId}/app_service/${DevPipelineStore.selectedApp}/git/tags?tag=${tag}&ref=${ref}`, release);

  /**
   * 编辑发布日志
   * @param projectId
   * @param tag
   * @param release
   * @returns {IDBRequest | Promise<void>}
   */
  editTag = (projectId, tag, release) => axios.put(`/devops/v1/projects/${projectId}/app_service/${DevPipelineStore.selectedApp}/git/tags?tag=${tag}`, release);

  /**
   * 删除标记
   * @param projectId
   * @param tag
   */
  deleteTag = (projectId, tag) => axios.delete(`/devops/v1/projects/${projectId}/app_service/${DevPipelineStore.selectedApp}/git/tags?tag=${tag}`);
}

const appTagStore = new AppTagStore();

export default appTagStore;
