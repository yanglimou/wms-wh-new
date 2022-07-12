package com.tsj.service.schedule;

import com.google.common.collect.Lists;
import com.jfinal.kit.Kv;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.cron4j.ITask;
import com.tsj.common.constant.FileConstant;
import com.tsj.common.utils.DateUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @className: RemoveTemp
 * @description: 本地系统定时删除临时数据和临时文件
 * @author: Frank
 * @create: 2020-03-16 13:30
 */
public class RemoveTemp implements ITask {
    private static final Log logger = Log.getLog(RemoveTemp.class);

    @Override
    public void stop() {
        logger.info("RemoveTemp stop");
    }

    @Override
    public void run() {
        //删除30天之前的临时文件
        removeTempFile(FileConstant.TEMP_PATH, 30);

        //删除30天之前备份文件
        removeTempFile(FileConstant.BACKUP_PATH, 30);

        //删除10天之前录像文件
        removeVideoFile(FileConstant.VIDEO_PATH, 10);

        //删除60天之前的高值柜照片
//        removeTempFile(FileConstant.CABINET_PATH, 90);

        //删除2年之前历史数据
//        removeHistoryRecord(36);
    }

    /**
     * 删除录像文件
     *
     * @param directoryPath 文件路径
     * @param days          天数
     */
    public void removeVideoFile(String directoryPath, int days) {
        File root = new File(directoryPath);
        if (root.exists()) {
            Arrays.stream(root.listFiles()).forEach(file -> {
                removeTempFile(file.getAbsolutePath(), days);
            });
        }
    }

    /**
     * 删除临时文件
     *
     * @param directoryPath 文件路径
     * @param days          过期天数
     */
    public void removeTempFile(String directoryPath, int days) {
        Calendar rightNow = Calendar.getInstance();
        rightNow.add(Calendar.DAY_OF_YEAR, days * -1);
        Date dt = rightNow.getTime();

        File file = new File(directoryPath);
        if (!file.exists() || !file.isDirectory()) {
            return;
        }

        String[] tempList = file.list();
        if (tempList != null && tempList.length > 0) {
            for (String path : tempList) {
                File temp = new File(directoryPath +"/"+ path);
                int res = new Date(temp.lastModified()).compareTo(dt);
                if (res < 0) {
                    boolean state = temp.delete();
                    logger.debug(directoryPath + path + " DELETE " + (state ? "OK" : "FAIL"));
                }
            }
        }
    }

    /**
     * 删除历史数据
     *
     * @param months 历史天数
     */
    public void removeHistoryRecord(int months) {
        String date = DateUtils.addMonth(DateUtils.getCurrentDate(), -months);

        //删除出库单历史记录
        Db.delete("delete from base_material where createDate < ?", date);

        //删除出入库历史记录
        Db.delete("delete from com_record_inout_exception where createDate < ?", date);
        Db.delete("delete from com_record_in where createDate < ?", date);
        Db.delete("delete from com_record_out where createDate < ?", date);

        //删除盘点历史记录
        Db.delete("delete from com_record_inventory_difference where createDate < ?", date);
        Db.delete("delete from com_record_inventory where createDate < ?", date);

        //删除日志历史记录
        Db.delete("delete from sys_log_login where createDate < ?", date);
        Db.delete("delete from sys_log_operate where createDate < ?", date);
        Db.delete("delete from sys_log_spd where createDate < ?", date);
    }
}
