package io.choerodon.devops.infra.util;

import static io.choerodon.devops.infra.constant.GitOpsConstants.DEFAULT_PIPELINE_RECORD_SIZE;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import io.choerodon.devops.api.vo.BaseDomain;
import io.choerodon.devops.api.vo.CiCdPipelineRecordVO;;
import io.choerodon.devops.api.vo.DevopsCdPipelineRecordVO;
import io.choerodon.devops.api.vo.DevopsCiPipelineRecordVO;
import io.choerodon.devops.infra.enums.PipelineStatus;

public class CiCdPipelineUtils {


    public static <T extends BaseDomain> void recordListSort(List<T> list) {
        Collections.sort(list, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {
                    if (o1.getCreatedDate().getTime() > o2.getCreatedDate().getTime()) {
                        return -1;
                    } else if (o1.getCreatedDate().getTime() < o2.getCreatedDate().getTime()) {
                        return 1;
                    } else {
                        return 0;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
    }



    public static void calculateStatus(CiCdPipelineRecordVO ciCdPipelineRecordVO, DevopsCiPipelineRecordVO devopsCiPipelineRecordVO, DevopsCdPipelineRecordVO devopsCdPipelineRecordVO) {
        if (!Objects.isNull(devopsCiPipelineRecordVO) && !Objects.isNull(devopsCdPipelineRecordVO)) {
            //计算记录的状态
            if (!PipelineStatus.SUCCESS.toValue().equals(devopsCiPipelineRecordVO.getStatus())) {
                ciCdPipelineRecordVO.setStatus(devopsCiPipelineRecordVO.getStatus());
                return;
            }
            if (PipelineStatus.SUCCESS.toValue().equals(devopsCiPipelineRecordVO.getStatus())
                    && PipelineStatus.SUCCESS.toValue().equals(devopsCdPipelineRecordVO.getStatus())) {
                ciCdPipelineRecordVO.setStatus(PipelineStatus.SUCCESS.toValue());
                return;
            }
            //如果ci状态成功cd是未执行，则状态为执行中
            if (PipelineStatus.SUCCESS.toValue().equals(devopsCiPipelineRecordVO.getStatus()) &&
                    PipelineStatus.CREATED.toValue().equals(devopsCdPipelineRecordVO.getStatus())) {
                ciCdPipelineRecordVO.setStatus(PipelineStatus.RUNNING.toValue());
                return;
            } else {
                ciCdPipelineRecordVO.setStatus(devopsCdPipelineRecordVO.getStatus());
            }
        }
        if (!Objects.isNull(devopsCiPipelineRecordVO) && Objects.isNull(devopsCdPipelineRecordVO)) {
            ciCdPipelineRecordVO.setStatus(devopsCiPipelineRecordVO.getStatus());
        }
        if (Objects.isNull(devopsCiPipelineRecordVO) && !Objects.isNull(devopsCdPipelineRecordVO)) {
            ciCdPipelineRecordVO.setStatus(devopsCdPipelineRecordVO.getStatus());
        }
    }

}
