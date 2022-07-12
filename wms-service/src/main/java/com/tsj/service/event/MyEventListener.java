package com.tsj.service.event;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.jfinal.log.Log;
import com.tsj.common.constant.SysConstant;
import com.tsj.common.event.LogEvent;
import com.tsj.common.event.SpdEvent;
import com.tsj.common.utils.IDGenerator;
import com.tsj.common.utils.IpUtil;
import com.tsj.domain.model.LogOperate;
import com.tsj.domain.model.LogSpd;
import net.dreamlu.event.core.EventListener;
import org.apache.commons.lang3.StringUtils;

/**
 * @className: LogEventListener
 * @description: 日志事件监听器
 * @author: Frank
 * @create: 2020-03-13 15:23
 */
public class MyEventListener {
    private Log logger = Log.getLog(MyEventListener.class);

    @EventListener(value = LogEvent.class, async = true)
    public void handleLogEvent(LogEvent event) {

        //查询成功的操作不记录到日志表
        if ("GET".equals(event.getMethod()) && StringUtils.isEmpty(event.getErrorMessage())) {
            return;
        }

        //拼接参数，参数过长，截取首尾信息
        StringBuilder sb = new StringBuilder();
        event.getParaMap().forEach((s, strings) -> sb.append(s).append("=").append(StringUtils.join(strings, ",")).append("  "));
        String parameter = sb.toString();
        int length = parameter.length();
        if (length > 5000) {
            parameter = parameter.substring(0, 2500) + "..." + parameter.substring(length - 2500);
        }

        LogOperate log = new LogOperate()
                .setId(IDGenerator.makeId())
                .setIp(IpUtil.ip2Long(event.getIp()))
                .setModule(event.getModule())
                .setUrl(event.getUrl())
                .setMethod(event.getMethod())
                .setParameter(parameter)
                .setErrorMessage(event.getErrorMessage())
                .setCreateDate(DateUtil.now())
                .setCreateUserId(event.getCreateUserId())
                .setState(StringUtils.isEmpty(event.getErrorMessage()) ? SysConstant.StateSuccess : SysConstant.StateError)
                .setExecuteTime(event.getExecuteTime());
        boolean result = log.save();
        if (!result) {
            logger.warn("saveLogOperate error：{}", JSON.toJSONString(log));
        }
    }

    @EventListener(value = SpdEvent.class, async = true)
    public void handleSpdEvent(SpdEvent event) {
        LogSpd log = new LogSpd()
                .setId(IDGenerator.makeId())
                .setModule(event.getModule())
                .setMethod(event.getMethod())
                .setUrl(event.getUrl())
                .setParameter(event.getJsonObject().toJSONString())
                .setErrorMessage(event.getErrorMessage())
                .setResult(event.getResult())
                .setCreateDate(DateUtil.now())
                .setState(StringUtils.isEmpty(event.getErrorMessage()) ? SysConstant.StateSuccess : SysConstant.StateError)
                .setExecuteTime(event.getExecuteTime());

        //查询成功的操作不记录到日志表
        if (event.getJsonObject().containsKey("spdCodeData") || StringUtils.isNotEmpty(event.getErrorMessage())) {
            boolean result = log.save();
            if (!result) {
                logger.error("saveLogSpd error：{}", JSON.toJSONString(log));
            }
        }
    }
}