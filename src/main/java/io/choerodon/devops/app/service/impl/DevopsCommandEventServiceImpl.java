package io.choerodon.devops.app.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import io.choerodon.devops.app.service.DevopsCommandEventService;
import io.choerodon.devops.infra.dto.DevopsCommandEventDTO;
import io.choerodon.devops.infra.mapper.DevopsCommandEventMapper;

/**
 * @author zmf
 */
@Service
public class DevopsCommandEventServiceImpl implements DevopsCommandEventService {

    private static final String ERROR_COMMAND_ID_IS_NULL = "error.command.id.is.null";
    @Autowired
    private DevopsCommandEventMapper devopsCommandEventMapper;

    @Override
    public void baseCreate(DevopsCommandEventDTO devopsCommandEventDTO) {
        devopsCommandEventMapper.insert(devopsCommandEventDTO);
    }

    @Override
    public List<DevopsCommandEventDTO> baseListByCommandIdAndType(Long commandId, String type) {
        DevopsCommandEventDTO devopsCommandEventDTO = new DevopsCommandEventDTO();
        devopsCommandEventDTO.setCommandId(commandId);
        devopsCommandEventDTO.setType(type);
        return devopsCommandEventMapper.select(devopsCommandEventDTO);
    }

    @Override
    public void baseDeletePreInstanceCommandEvent(Long instanceId) {
        devopsCommandEventMapper.deletePreInstanceCommandEvent(instanceId);
    }

    @Override
    public void baseDeleteByCommandId(Long commandId) {
        DevopsCommandEventDTO devopsCommandEventDTO = new DevopsCommandEventDTO();
        devopsCommandEventDTO.setCommandId(commandId);
        devopsCommandEventMapper.delete(devopsCommandEventDTO);
    }

    @Override
    public List<DevopsCommandEventDTO> listByCommandIdsAndType(Set<Long> commandIds, String type) {
        return commandIds.isEmpty() ? new ArrayList<>() : devopsCommandEventMapper.listByCommandIdsAndType(commandIds, type);
    }

    @Override
    public List<DevopsCommandEventDTO> listByCommandId(Long commandId) {
        Assert.notNull(commandId, ERROR_COMMAND_ID_IS_NULL);
        DevopsCommandEventDTO devopsCommandEventDTO = new DevopsCommandEventDTO();
        devopsCommandEventDTO.setCommandId(commandId);
        return devopsCommandEventMapper.select(devopsCommandEventDTO);
    }
}