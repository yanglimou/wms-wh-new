package com.tsj.service.schedule;

import com.jfinal.log.Log;
import com.jfinal.plugin.cron4j.ITask;
import com.tsj.common.config.CommonConfig;
import com.tsj.common.constant.FileConstant;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @className: DbBackup
 * @description: 数据自动备份
 * @author: Frank
 * @create: 2020-06-29 10:22
 */
public class DbBackup implements ITask {

    private static final Log logger = Log.getLog(DbBackup.class);

    @Override
    public void stop() {
        logger.info("DbBackup stop");
    }

    @Override
    public void run() {
        try {
            String backName = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".sql";
            dbBackUp(CommonConfig.prop.get("wms.user"), CommonConfig.prop.get("wms.password"), CommonConfig.prop.get("wms.db"), FileConstant.BACKUP_PATH, backName);
            logger.info("DbBackup OK:" + backName);
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    /**
     * 备份数据库db
     *
     * @param root
     * @param pwd
     * @param dbName
     * @param backPath
     * @param backName
     */
    public void dbBackUp(String root, String pwd, String dbName, String backPath, String backName) throws Exception {
        String pathSql = backPath + backName;
        File fileSql = new File(pathSql);
        //创建备份sql文件
        if (!fileSql.exists()) {
            fileSql.createNewFile();
        }
        //mysqldump -hlocalhost -uroot -p123456 db > /home/back.sql
        StringBuffer sb = new StringBuffer();
        sb.append("mysqldump");
        sb.append(" -h127.0.0.1");
        sb.append(" -u" + root);
        sb.append(" -p" + pwd);
        sb.append(" " + dbName + " >");
        sb.append(pathSql);
        logger.debug("cmd命令为：" + sb.toString());
        Runtime runtime = Runtime.getRuntime();
        logger.debug("开始备份：" + dbName);
        Process process = runtime.exec("cmd /c" + sb.toString());
        logger.debug("备份成功!");
    }

    /**
     * 恢复数据库
     *
     * @param root
     * @param pwd
     * @param dbName
     * @param filePath mysql -hlocalhost -uroot -p123456 db < /home/back.sql
     */
    public void dbRestore(String root, String pwd, String dbName, String filePath) {
        StringBuilder sb = new StringBuilder();
        sb.append("mysql");
        sb.append(" -h127.0.0.1");
        sb.append(" -u" + root);
        sb.append(" -p" + pwd);
        sb.append(" " + dbName + " <");
        sb.append(filePath);
        logger.debug("cmd命令为：" + sb.toString());
        Runtime runtime = Runtime.getRuntime();
        logger.debug("开始还原数据");
        try {
            Process process = runtime.exec("cmd /c" + sb.toString());
            InputStream is = process.getInputStream();
            BufferedReader bf = new BufferedReader(new InputStreamReader(is, "utf8"));
            String line = null;
            while ((line = bf.readLine()) != null) {
                System.out.println(line);
            }
            is.close();
            bf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug("还原成功！");
    }
}