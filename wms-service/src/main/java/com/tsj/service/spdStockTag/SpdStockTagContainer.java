package com.tsj.service.spdStockTag;

import com.jfinal.kit.Kv;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.common.config.CommonConfig;
import com.tsj.common.constant.SpdUrl;
import com.tsj.common.utils.HttpKit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SpdStockTagContainer {

    public static final Log logger = Log.getLog(SpdStockTagContainer.class);
    private static volatile ConcurrentHashMap<String, Set<String>> concurrentHashMap = new ConcurrentHashMap<>();
    /**
     * SPD数据同步标识，防止重复操作
     */
    private static volatile boolean isRunning = false;

    public static void syncSpd() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        try {
            if (!CommonConfig.prop.getBoolean("spd")) {
                return;
            }
            List<Record> recordList = Db.find("select deptId,count(deptId) num from com_tag group by deptId");
            ConcurrentHashMap<String, Set<String>> temp = new ConcurrentHashMap<>();
            recordList.forEach(record -> {
                String deptId = record.getStr("deptId");
                int num = record.getInt("num");
                Set<String> spdSetFromSpd = HttpKit.postSpdData(CommonConfig.prop.get("SPD_BASE_URL") + SpdUrl.URL_STOCK_TAG.getUrl(), null,
                        Kv.by("DeptId", deptId)).stream().map(record1 -> record1.getStr("CASE_NBR")).collect(Collectors.toSet());
                logger.info("syncSpd deptId:{%s},com_tag:{%d},spd:{%d}", deptId, num, spdSetFromSpd.size());
                temp.put(deptId, spdSetFromSpd);
            });
            concurrentHashMap = temp;
        } catch (Exception ignored) {
            logger.error("错误", ignored);
        }
        isRunning = false;
        return;
    }

    public static ConcurrentHashMap<String, Set<String>> getConcurrentHashMap() {
        return concurrentHashMap;
    }

    public static Set<String> getByDept(String deptId) {
        return concurrentHashMap.getOrDefault(deptId, new HashSet<>());
    }

    public static Set<String> getAll() {
        Set<String> resultSet = new HashSet<>();
        concurrentHashMap.values().forEach(set -> {
            resultSet.addAll(set);
        });
        return resultSet;
    }
}
