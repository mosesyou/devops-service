package io.choerodon.devops.app.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.app.service.DevopsBranchService;
import io.choerodon.devops.infra.dto.DevopsBranchDTO;
import io.choerodon.devops.infra.mapper.DevopsBranchMapper;
import io.choerodon.devops.infra.util.TypeUtil;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.pagehelper.domain.Sort;


/**
 * Created by Sheep on 2019/7/11.
 */

@Service
public class DevopsBranchServiceImpl implements DevopsBranchService {
    @Autowired
    private DevopsBranchMapper devopsBranchMapper;

    @Override
    public List<DevopsBranchDTO> baseListDevopsBranchesByIssueId(Long issueId) {
        DevopsBranchDTO queryDevopsBranchDTO = new DevopsBranchDTO();
        queryDevopsBranchDTO.setIssueId(issueId);
        return devopsBranchMapper.select(queryDevopsBranchDTO);
    }

    @Override
    public DevopsBranchDTO baseQueryByAppAndBranchName(Long appServiceId, String branchName) {
        return devopsBranchMapper
                .queryByAppAndBranchName(appServiceId, branchName);
    }

    @Override
    public void updateBranchIssue(DevopsBranchDTO devopsBranchDTO) {
        DevopsBranchDTO oldDevopsBranchDTO = devopsBranchMapper
                .queryByAppAndBranchName(devopsBranchDTO.getAppServiceId(), devopsBranchDTO.getBranchName());

        if (oldDevopsBranchDTO == null) {
            throw new CommonException("error.query.branch.by.name");
        }
        oldDevopsBranchDTO.setIssueId(devopsBranchDTO.getIssueId());
        oldDevopsBranchDTO.setObjectVersionNumber(devopsBranchDTO.getObjectVersionNumber());
        devopsBranchMapper.updateBranchById(oldDevopsBranchDTO);
    }

    @Override
    public void baseUpdateBranchLastCommit(DevopsBranchDTO devopsBranchDTO) {
        DevopsBranchDTO oldDevopsBranchDTO = devopsBranchMapper
                .queryByAppAndBranchName(devopsBranchDTO.getAppServiceId(), devopsBranchDTO.getBranchName());
        oldDevopsBranchDTO.setLastCommit(devopsBranchDTO.getLastCommit());
        oldDevopsBranchDTO.setLastCommitDate(devopsBranchDTO.getLastCommitDate());
        oldDevopsBranchDTO.setLastCommitMsg(devopsBranchDTO.getLastCommitMsg());
        oldDevopsBranchDTO.setLastCommitUser(devopsBranchDTO.getLastCommitUser());
        devopsBranchMapper.updateByPrimaryKey(oldDevopsBranchDTO);
    }

    @Override
    public DevopsBranchDTO baseCreate(DevopsBranchDTO devopsBranchDTO) {
        DevopsBranchDTO exist = new DevopsBranchDTO();
        exist.setAppServiceId(devopsBranchDTO.getAppServiceId());
        exist.setBranchName(devopsBranchDTO.getBranchName());
        if (devopsBranchMapper.selectOne(exist) != null) {
            throw new CommonException("error.branch.exist");
        }
        devopsBranchMapper.insert(devopsBranchDTO);
        return devopsBranchDTO;
    }

    @Override
    public DevopsBranchDTO baseQuery(Long devopsBranchId) {
        return devopsBranchMapper.selectByPrimaryKey(devopsBranchId);
    }


    @Override
    public void baseUpdateBranch(DevopsBranchDTO devopsBranchDTO) {
        devopsBranchDTO.setObjectVersionNumber(devopsBranchMapper.selectByPrimaryKey(devopsBranchDTO.getId()).getObjectVersionNumber());
        if (devopsBranchMapper.updateByPrimaryKey(devopsBranchDTO) != 1) {
            throw new CommonException("error.branch.update");
        }
    }


    @Override
    public Page<DevopsBranchDTO> basePageBranch(Long appServiceId, PageRequest pageable, String params) {
        Map<String, Object> maps = TypeUtil.castMapParams(params);
        Sort sort = pageable.getSort();
        String sortResult = "";
        if (sort != null) {
            sortResult = Lists.newArrayList(pageable.getSort().iterator()).stream()
                    .map(t -> {
                        String property = t.getProperty();
                        if ("branchName".equals(property)) {
                            property = "db.branch_name";
                        } else if ("creation_date".equals(property)) {
                            property = "db.creation_date";
                        } else {
                            throw new CommonException("error.field.not.supported.for.sort", t.getProperty());
                        }
                        return property + " " + t.getDirection();
                    })
                    .collect(Collectors.joining(","));
        }
        String sortString = sortResult;
        return PageHelper.doPage(pageable,
                () -> devopsBranchMapper.list(appServiceId,
                        sortString,
                        TypeUtil.cast(maps.get(TypeUtil.SEARCH_PARAM)),
                        TypeUtil.cast(maps.get(TypeUtil.PARAMS))));
    }


    @Override
    public void baseDelete(Long appServiceId, String branchName) {
        DevopsBranchDTO devopsBranchDTO = devopsBranchMapper.queryByAppAndBranchName(appServiceId, branchName);
        if (devopsBranchDTO != null) {
            devopsBranchMapper.delete(devopsBranchDTO);
        }
    }

    @Override
    public void deleteAllBaranch(Long appServiceId) {
        devopsBranchMapper.deleteByAppServiceId(appServiceId);
    }
}
