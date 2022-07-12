package com.tsj.web.controller;

import com.google.common.collect.Lists;
import com.jfinal.aop.Before;
import com.jfinal.aop.Inject;
import com.jfinal.ext.interceptor.GET;
import com.jfinal.kit.Kv;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.common.annotation.OperateLog;
import com.tsj.common.constant.FileConstant;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.utils.DateUtils;
import com.tsj.common.utils.R;
import com.tsj.service.CacheService;
import com.tsj.service.SpdService;
import com.tsj.service.SysService;
import com.tsj.service.schedule.SpdReissue;
import com.tsj.web.common.MyController;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * @className: WebSysController
 * @description: 系统数据
 * @author: Frank
 * @create: 2020-07-06 15:50
 */
@Before(GET.class)
public class SysController extends MyController {

    @Inject
    private SpdService spdService;

    @Inject
    private SysService sysService;

    @Inject
    private CacheService cacheService;

    /******************************查询******************************/

    @OperateLog("查询菜单树")
    public void getMenuTree() {
        String printerExcludeMenu = getAttr("printerExcludeMenu");
        R result = R.ok().putData(sysService.getMenuTree(printerExcludeMenu));
        renderJson(result);
    }

    @OperateLog("查询菜单列表")
    public void getMenuList() {
        R result = R.ok().putData(sysService.getMenuList(getPara("menuType")));
        renderJson(result);
    }

    @OperateLog("查询数据看板")
    public void getDashboardList() {
        Kv kv = Kv.create();
        kv.set("totalStatistics", sysService.getTotalStatistics());
        R result = R.ok().putData(kv);
        renderJson(result);
    }

    @OperateLog("查询数据字典")
    public void getInitData() {
        R result = R.ok().putData(sysService.getDataDicList());
        renderJson(result);
    }

    @OperateLog("查询SPD数据")
    public void getSpdData() {
        boolean ret = spdService.postBasicData("all");
        if (!ret) {
            renderJson(R.error(ResultCode.SYSTEM_SPD_BUSY));
            return;
        }
        renderJson(R.ok());
    }

    @OperateLog("查询系统配置")
    public void getConfigData() {
        R result = R.ok().putData(sysService.getConfigData());
        renderJson(result);
    }

    @OperateLog("查询操作日志列表")
    public void getLogList(String state, String dateRange, String employeeName, String module) {
        Kv cond = Kv.by("state", state).set("dateRange", dateRange).set("employeeName", employeeName).set("module", module);

        Page<Record> pageData = sysService.getRecordOperateByPage(getParaToInt("page"), getParaToInt("limit"), cond);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @OperateLog("查询Webservice日志列表")
    public void getWsdlList(String state, String dateRange, String method) {
        Kv cond = Kv.by("state", state).set("dateRange", dateRange).set("method", method);

        Page<Record> pageData = sysService.getRecordWsdlByPage(getParaToInt("page"), getParaToInt("limit"), cond);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @OperateLog(value = "查询录像文件列表")
    public void getVideoFileList(String cabinetId, String dateRange) {
        if (StringUtils.isEmpty(cabinetId)) {
            renderJson(R.error(ResultCode.PARAM_NOT_COMPLETE));
            return;
        }

        File file = new File(FileConstant.VIDEO_PATH, cabinetId);
        if (file.exists()) {
            List<Kv> kvList = Lists.newArrayList();
            Arrays.stream(file.listFiles())
                    .sorted(Comparator.comparing(File::lastModified).reversed())
                    .forEach(listFile -> {

                        //按照时间范围过滤
                        if (StringUtils.isNoneBlank(dateRange)) {
                            String[] strs = dateRange.split(" - ");
                            String fileDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(listFile.lastModified()));

                            if (DateUtils.compare(strs[0], fileDate) || DateUtils.compare(fileDate, strs[1])) {
                                return;
                            }
                        }

                        String fileName = listFile.getName();
                        String address = null;
                        if (fileName.contains("_")) {
                            address = fileName.split("_")[0];
                        }

                        kvList.add(Kv.by("fileName", fileName)
                                .set("cabinetId", cabinetId)
                                .set("address", address)
                                .set("fileDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(listFile.lastModified())))
                                .set("fileSize", String.format("%.2f", listFile.length() / (1024 * 1024 * 1.0)) + "MB (" + listFile.length() + "字节)"));
                    });
            renderJson(R.ok().putData(kvList));
        } else {
            renderJson(R.error(ResultCode.FILE_NOT_EXISTED));
        }
    }

    @OperateLog(value = "下载录像文件")
    public void getVideoFile(String cabinetId, String fileName) {
        File file = new File(FileConstant.VIDEO_PATH, cabinetId + "/" + fileName);
        if (file.exists()) {
            renderFile(file);
        } else {
            renderJson(R.error(ResultCode.FILE_NOT_EXISTED));
        }
    }

    @OperateLog(value = "查询日志文件列表")
    public void getLogFileList(String cabinetId, String dateRange) {
        if (StringUtils.isEmpty(cabinetId)) {
            renderJson(R.error(ResultCode.PARAM_NOT_COMPLETE));
            return;
        }

        File file = new File(FileConstant.LOG_PATH, cabinetId);
        if (file.exists()) {
            List<Kv> kvList = Lists.newArrayList();
            Arrays.stream(file.listFiles())
                    .sorted(Comparator.comparing(File::lastModified).reversed())
                    .forEach(listFile -> {

                        //按照时间范围过滤
                        if (StringUtils.isNoneBlank(dateRange)) {
                            String[] strs = dateRange.split(" - ");
                            String fileDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(listFile.lastModified()));

                            if (DateUtils.compare(strs[0], fileDate) || DateUtils.compare(fileDate, strs[1])) {
                                return;
                            }
                        }

                        String fileName = listFile.getName();
                        String address = null;
                        if (fileName.contains("_")) {
                            address = fileName.split("_")[0];
                        }

                        kvList.add(Kv.by("fileName", fileName)
                                .set("cabinetId", cabinetId)
                                .set("address", address)
                                .set("fileDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(listFile.lastModified())))
                                .set("fileSize", String.format("%.2f", listFile.length() / (1024 * 1024 * 1.0)) + "MB (" + listFile.length() + "字节)"));
                    });
            renderJson(R.ok().putData(kvList));
        } else {
            renderJson(R.error(ResultCode.FILE_NOT_EXISTED));
        }
    }

    @OperateLog(value = "下载日志文件")
    public void getLogFile(String cabinetId, String fileName) {
        File file = new File(FileConstant.LOG_PATH, cabinetId + "/" + fileName);
        if (file.exists()) {
            renderFile(file);
        } else {
            renderJson(R.error(ResultCode.FILE_NOT_EXISTED));
        }
    }
}