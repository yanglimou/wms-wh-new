package com.tsj.web.task;

import com.jfinal.log.Log;
import com.jfinal.plugin.cron4j.ITask;
import com.tsj.web.hikvision.HikVision;

/**
 * 功能描述：若摄像头录像时间超过10分钟，则自动停止录像
 *
 * @Author: aaa
 * @Date: 2021/4/25 10:39
 */
public class AutoCheckHikvision implements ITask {
    private final Log logger = Log.getLog(this.getClass());

    @Override
    public void stop() {
        logger.info("AutoCheckHikvision stop");
    }

    @Override
    public void run() {
        logger.info("=============================AutoCheckHikvision====================================");
        HikVision.checkCamera();
        logger.info("================================End===========================================");
    }
}

