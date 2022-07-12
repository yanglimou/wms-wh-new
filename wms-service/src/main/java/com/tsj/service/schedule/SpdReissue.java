package com.tsj.service.schedule;

import com.jfinal.aop.Aop;
import com.jfinal.log.Log;
import com.jfinal.plugin.cron4j.ITask;
import com.tsj.common.threadpool.ThreadPoolKit;
import com.tsj.service.SpdService;

/**
 * @className: SpdReissue
 * @description: SPD系统业务数据定时补发
 * @author: Frank
 * @create: 2020-03-16 13:30
 */
public class SpdReissue implements ITask {
    private static final Log logger = Log.getLog(SpdReissue.class);

    @Override
    public void stop() {
        logger.info("SpdReissue stop");
    }

    @Override
    public void run() {
        SpdService spdService = Aop.get(SpdService.class);
        ThreadPoolKit.execute(() -> {
            spdService.postBasicData("all");
            spdService.postSpdTag("all");
        });
    }
}
