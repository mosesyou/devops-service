package io.choerodon.devops.infra.mapper;

import org.apache.ibatis.annotations.Param;

import io.choerodon.devops.infra.dto.CertificationFileDTO;
import io.choerodon.mybatis.common.Mapper;

/**
 * Created by n!Ck
 * Date: 2018/8/20
 * Time: 19:57
 * Description:
 */

public interface DevopsCertificationFileMapper extends Mapper<CertificationFileDTO> {
    CertificationFileDTO queryByCertificationId(@Param("certificationId") Long certificationId);
}

