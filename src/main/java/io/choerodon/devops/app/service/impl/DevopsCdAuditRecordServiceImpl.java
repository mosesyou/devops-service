package io.choerodon.devops.app.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hzero.boot.message.entity.Receiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.app.service.DevopsCdAuditRecordService;
import io.choerodon.devops.app.service.SendNotificationService;
import io.choerodon.devops.infra.constant.MessageCodeConstants;
import io.choerodon.devops.infra.constant.PipelineCheckConstant;
import io.choerodon.devops.infra.constant.ResourceCheckConstant;
import io.choerodon.devops.infra.dto.DevopsCdAuditRecordDTO;
import io.choerodon.devops.infra.dto.DevopsCdJobRecordDTO;
import io.choerodon.devops.infra.dto.DevopsCdStageRecordDTO;
import io.choerodon.devops.infra.dto.DevopsPipelineRecordRelDTO;
import io.choerodon.devops.infra.dto.iam.IamUserDTO;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;
import io.choerodon.devops.infra.mapper.DevopsCdAuditRecordMapper;
import io.choerodon.devops.infra.mapper.DevopsCdJobRecordMapper;
import io.choerodon.devops.infra.mapper.DevopsCdStageRecordMapper;
import io.choerodon.devops.infra.mapper.DevopsPipelineRecordRelMapper;
import io.choerodon.devops.infra.util.KeyDecryptHelper;

/**
 * @author scp
 * @date 2020/7/3
 * @description
 */
@Service
public class DevopsCdAuditRecordServiceImpl implements DevopsCdAuditRecordService {

    private static final String STAGE_NAME = "stageName";
    private static final String REL_ID = "pipelineIdRecordId";
    private static final String PIPELINE_ID = "pipelineId";
    private static final String ERROR_SAVE_AUDIT_RECORD = "error.save.audit.record";
    private static final String ERROR_UPDATE_AUDIT_RECORD = "error.update.audit.record";

    @Autowired
    private DevopsCdAuditRecordMapper devopsCdAuditRecordMapper;

    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;
    @Autowired
    private SendNotificationService sendNotificationService;
    @Autowired
    private DevopsPipelineRecordRelMapper devopsPipelineRecordRelMapper;
    @Autowired
    private DevopsCdStageRecordMapper devopsCdStageRecordMapper;
    @Autowired
    private DevopsCdJobRecordMapper devopsCdJobRecordMapper;

    @Override
    public List<DevopsCdAuditRecordDTO> queryByStageRecordId(Long stageRecordId) {
        DevopsCdAuditRecordDTO auditRecordDTO = new DevopsCdAuditRecordDTO();
        auditRecordDTO.setStageRecordId(stageRecordId);
        return devopsCdAuditRecordMapper.select(auditRecordDTO);
    }

    @Override
    public List<DevopsCdAuditRecordDTO> queryByJobRecordId(Long jobRecordId) {
        DevopsCdAuditRecordDTO auditRecordDTO = new DevopsCdAuditRecordDTO();
        auditRecordDTO.setJobRecordId(jobRecordId);
        return devopsCdAuditRecordMapper.select(auditRecordDTO);
    }

    @Override
    public void sendStageAuditMessage(DevopsCdStageRecordDTO devopsCdStageRecord, Long devopsCdPipelineRecordId) {
        // 查询审核人员
        List<DevopsCdAuditRecordDTO> devopsCdAuditRecordDTOS = queryByStageRecordId(devopsCdStageRecord.getId());
        if (CollectionUtils.isEmpty(devopsCdAuditRecordDTOS)) {
            return;
        }
        // 发送审核通知
        List<Receiver> userList = new ArrayList<>();
        List<Long> userIds = devopsCdAuditRecordDTOS.stream().map(DevopsCdAuditRecordDTO::getUserId).collect(Collectors.toList());
        List<IamUserDTO> iamUserDTOS = baseServiceClientOperator.queryUsersByUserIds(userIds);
        Map<Long, IamUserDTO> userDTOMap = iamUserDTOS.stream().collect(Collectors.toMap(IamUserDTO::getId, v -> v));

        userIds.forEach(id -> {
            IamUserDTO iamUserDTO = userDTOMap.get(id);
            if (iamUserDTO != null) {
                Receiver user = new Receiver();
                user.setEmail(iamUserDTO.getEmail());
                user.setUserId(iamUserDTO.getId());
                user.setPhone(iamUserDTO.getPhone());
                user.setTargetUserTenantId(iamUserDTO.getOrganizationId());
                userList.add(user);
            }
        });
        HashMap<String, String> params = new HashMap<>();
        params.put(STAGE_NAME, devopsCdStageRecord.getStageName());
        DevopsPipelineRecordRelDTO recordRelDTO = new DevopsPipelineRecordRelDTO();
        recordRelDTO.setCdPipelineRecordId(devopsCdPipelineRecordId);
        DevopsPipelineRecordRelDTO relDTO = devopsPipelineRecordRelMapper.selectOne(recordRelDTO);
        params.put(REL_ID, relDTO.getId().toString());
        params.put(PIPELINE_ID, KeyDecryptHelper.encryptValueWithoutToken(relDTO.getPipelineId()));
        sendNotificationService.sendCdPipelineNotice(devopsCdStageRecord.getPipelineRecordId(), MessageCodeConstants.PIPELINE_STAGE_AUDIT, userList, params);
    }

    @Override
    public void sendJobAuditMessage(Long pipelineRecordId, DevopsCdJobRecordDTO devopsCdJobRecordDTO) {
        // 查询审核人员
        List<DevopsCdAuditRecordDTO> devopsCdAuditRecordDTOS = queryByJobRecordId(devopsCdJobRecordDTO.getId());
        if (CollectionUtils.isEmpty(devopsCdAuditRecordDTOS)) {
            return;
        }
        // 发送审核通知
        List<Receiver> userList = new ArrayList<>();
        List<Long> userIds = devopsCdAuditRecordDTOS.stream().map(DevopsCdAuditRecordDTO::getUserId).collect(Collectors.toList());
        List<IamUserDTO> iamUserDTOS = baseServiceClientOperator.queryUsersByUserIds(userIds);
        Map<Long, IamUserDTO> userDTOMap = iamUserDTOS.stream().collect(Collectors.toMap(IamUserDTO::getId, v -> v));

        userIds.forEach(id -> {
            IamUserDTO iamUserDTO = userDTOMap.get(id);
            if (iamUserDTO != null) {
                Receiver user = new Receiver();
                user.setEmail(iamUserDTO.getEmail());
                user.setUserId(iamUserDTO.getId());
                user.setPhone(iamUserDTO.getPhone());
                user.setTargetUserTenantId(iamUserDTO.getOrganizationId());
                userList.add(user);
            }
        });
        HashMap<String, String> params = new HashMap<>();
        params.put(STAGE_NAME, devopsCdJobRecordDTO.getName());
        DevopsPipelineRecordRelDTO recordRelDTO = new DevopsPipelineRecordRelDTO();
        recordRelDTO.setCdPipelineRecordId(pipelineRecordId);
        DevopsPipelineRecordRelDTO relDTO = devopsPipelineRecordRelMapper.selectOne(recordRelDTO);
        params.put(REL_ID, relDTO.getId().toString());
        params.put(PIPELINE_ID, KeyDecryptHelper.encryptValueWithoutToken(relDTO.getPipelineId()));
        sendNotificationService.sendCdPipelineNotice(pipelineRecordId, MessageCodeConstants.PIPELINE_AUDIT, userList, params);
    }

    @Override
    @Transactional
    public void update(DevopsCdAuditRecordDTO devopsCdAuditRecordDTO) {
        if (devopsCdAuditRecordMapper.updateByPrimaryKeySelective(devopsCdAuditRecordDTO) != 1) {
            throw new CommonException(ERROR_UPDATE_AUDIT_RECORD);
        }
    }

    @Override
    public void save(DevopsCdAuditRecordDTO devopsCdAuditRecordDTO) {
        if (devopsCdAuditRecordMapper.insertSelective(devopsCdAuditRecordDTO) != 1) {
            throw new CommonException(ERROR_SAVE_AUDIT_RECORD);
        }
    }

    @Override
    public DevopsCdAuditRecordDTO queryByStageRecordIdAndUserId(Long stageRecordId, Long userId) {
        Assert.notNull(stageRecordId, PipelineCheckConstant.ERROR_STAGE_RECORD_ID_IS_NULL);
        Assert.notNull(userId, ResourceCheckConstant.ERROR_USER_ID_IS_NULL);

        DevopsCdAuditRecordDTO devopsCdAuditRecordDTO = new DevopsCdAuditRecordDTO();
        devopsCdAuditRecordDTO.setStageRecordId(stageRecordId);
        devopsCdAuditRecordDTO.setUserId(userId);
        return devopsCdAuditRecordMapper.selectOne(devopsCdAuditRecordDTO);
    }

    @Override
    public DevopsCdAuditRecordDTO queryByJobRecordIdAndUserId(Long jobRecordId, Long userId) {
        Assert.notNull(jobRecordId, PipelineCheckConstant.ERROR_JOB_RECORD_ID_IS_NULL);
        Assert.notNull(userId, ResourceCheckConstant.ERROR_USER_ID_IS_NULL);

        DevopsCdAuditRecordDTO devopsCdAuditRecordDTO = new DevopsCdAuditRecordDTO();
        devopsCdAuditRecordDTO.setJobRecordId(jobRecordId);
        devopsCdAuditRecordDTO.setUserId(userId);
        return devopsCdAuditRecordMapper.selectOne(devopsCdAuditRecordDTO);
    }

    @Override
    public void fixProjectId() {
        List<DevopsCdAuditRecordDTO> devopsCdAuditDTOS = devopsCdAuditRecordMapper.selectAll();
        Set<Long> stageRecordIds = devopsCdAuditDTOS.stream().filter(i->i.getStageRecordId()!=null).map(DevopsCdAuditRecordDTO::getStageRecordId).collect(Collectors.toSet());
        Set<Long> jobRecordIds = devopsCdAuditDTOS.stream().filter(i->i.getJobRecordId()!=null).map(DevopsCdAuditRecordDTO::getJobRecordId).collect(Collectors.toSet());

        List<DevopsCdStageRecordDTO> devopsCdStageRecordDTOS = devopsCdStageRecordMapper.selectByIds(StringUtils.join(stageRecordIds, ","));
        List<DevopsCdJobRecordDTO> devopsCdJobRecordDTOS = devopsCdJobRecordMapper.selectByIds(StringUtils.join(jobRecordIds, ","));

        devopsCdStageRecordDTOS.forEach(i -> {
            devopsCdAuditRecordMapper.updateProjectIdByStageRecordId(i.getProjectId(), i.getId());
        });

        devopsCdJobRecordDTOS.forEach(i -> {
            devopsCdAuditRecordMapper.updateProjectIdByJobRecordId(i.getProjectId(), i.getId());
        });
    }
}
