package com.tsj.service.schedule;

import com.alibaba.fastjson.JSON;
import com.jfinal.aop.Aop;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.cron4j.ITask;
import com.tsj.common.config.CommonConfig;
import com.tsj.service.CacheService;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static com.tsj.common.constant.SysConstant.*;
/**
 * @className: com_tag绑定print的epc
 * @description: com_tag绑定print的epc
 * @author: yanglimou
 * @create: 2022-06-04 14:17
 */
public class BindEpcSchdeuler implements ITask {
    private static final Log logger = Log.getLog(BindEpcSchdeuler.class);

    @Override
    public void stop() {
        logger.info("BindEpcSchdeuler stop");
    }

    @Override
    public void run() {
        if (!CommonConfig.prop.getBoolean("spd")) {
            return;
        }
        CacheService cacheService = Aop.get(CacheService.class);
        String sql = "select spdCode from com_tag where epc is null";
        List<String> spdCodeList = Db.query(sql);
        if (spdCodeList.size() == 0) {
            logger.info("没有需要绑定epc的标签");
            return;
        }
        logger.info("epc为空的标签有" + spdCodeList.size() + "条，具体标签值如下：" + JSON.toJSONString(spdCodeList));
        spdCodeList.forEach(spdCode -> {
            Record printRecord = Db.findFirst("select * from com_print where spdCode=?", spdCode);
            if (printRecord == null) {
                logger.error(spdCode + "标签print数据没有推送，联系spd推送数据");
            } else {
                String epc = printRecord.getStr("epc");
                String userId = printRecord.getStr("userId");
                if (StringUtils.isEmpty(epc)) {
                    logger.error(spdCode + "标签print数据epc值为空");
                } else if (StringUtils.isEmpty(userId)) {
                    logger.error(spdCode + "标签print数据未打印，请先打印");
                } else {
                    Db.update("update com_tag set epc=?,createUserId=?,createDate=now() where spdCode=?", epc, userId, spdCode);
                    //清除缓存
                    cacheService.removeCache(CacheCom, TagByEpc, epc);
                    cacheService.removeCache(CacheCom, TagById, spdCode);
                }
            }
        });
    }
}
