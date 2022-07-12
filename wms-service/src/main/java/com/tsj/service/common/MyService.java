package com.tsj.service.common;

import com.tsj.common.config.CommonConfig;
import com.tsj.common.constant.SysConstant;
import org.apache.commons.lang3.StringUtils;

/**
 * 功能描述：
 *
 * @Author: aaa
 * @Date: 2021/8/21 19:42
 */
public class MyService extends SysConstant {

    /**
     * 获取默认用户ID
     * @return
     */
    protected  String getDefaultUserId()
    {
        String defaultUserId = CommonConfig.prop.get("spd.defaultUserId");
        if(StringUtils.isEmpty(defaultUserId))
            return "1";

        return defaultUserId;
    }
}
