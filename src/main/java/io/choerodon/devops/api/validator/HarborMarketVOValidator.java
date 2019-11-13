package io.choerodon.devops.api.validator;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.vo.HarborMarketVO;

import java.util.regex.Pattern;

/**
 * @author: trump
 * @date: 2019/8/20 15:38
 * @description:
 */
public class HarborMarketVOValidator {
    private static final Pattern EMAIL = Pattern.compile("^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z0-9]{2,6}$");
    private static final Pattern PASSWORD = Pattern.compile("^(?![a-z0-9\\W]+$)(?![A-Za-z\\W]+$)(?![A-Z0-9\\W]+$)[a-zA-Z0-9\\W]{8,}$");

    private HarborMarketVOValidator() {
    }

    /**
     * 检查应用的name和code是否符合标准
     */
    public static void checkEmailAndPassword(HarborMarketVO harborMarketVO) {
        if (!EMAIL.matcher(harborMarketVO.getUser().getEmail()).matches()) {
            throw new CommonException("error.harbor.user.email.notMatch");
        }
        if (!PASSWORD.matcher(harborMarketVO.getUser().getPassword()).matches()) {
            throw new CommonException("error.harbor.user.password.notMatch");
        }
    }

}
