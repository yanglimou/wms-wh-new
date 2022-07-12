package com.tsj.service.schedule;

import com.jfinal.log.Log;
import com.jfinal.plugin.cron4j.ITask;
import com.tsj.common.threadpool.ThreadPoolKit;
import com.tsj.service.spdStockTag.SpdStockTagContainer;

public class SpdStockTagScheduler implements ITask {
    private static final Log log = Log.getLog(SpdReissue.class);

    @Override
    public void stop() {
        log.info("SpdStockTagScheduler stop");
    }

    @Override
    public void run() {
        ThreadPoolKit.execute(() -> {
            SpdStockTagContainer.syncSpd();
        });
    }
}
