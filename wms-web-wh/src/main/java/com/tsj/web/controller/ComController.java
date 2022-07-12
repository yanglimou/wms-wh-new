package com.tsj.web.controller;

import cn.hutool.core.date.DateUtil;
import com.alibaba.druid.util.StringUtils;
import com.jfinal.aop.Before;
import com.jfinal.aop.Inject;
import com.jfinal.ext.interceptor.GET;
import com.jfinal.ext.interceptor.POST;
import com.jfinal.kit.Kv;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.common.annotation.NotNull;
import com.tsj.common.annotation.OperateLog;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.constant.SysConstant;
import com.tsj.common.utils.*;
import com.tsj.domain.model.StockTag;
import com.tsj.domain.model.Tag;
import com.tsj.service.ComService;
import com.tsj.common.config.CommonConfig;
import com.tsj.web.common.MyController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @className: ComController
 * @description: 业务数据
 * @author: Frank
 * @create: 2020-07-06 11:14
 */
public class ComController extends MyController {

    @Inject
    private ComService comService;

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("查询标签分页")
    public void getTagList(String accept, String epc, String deptId, String spdCode, String goodsId, String dateRange, String orderCode, int page, int limit) {
        Kv cond = Kv.create();
        cond.set("accept", accept);
        cond.set("epc", epc);
        cond.set("orderCode", orderCode);
        cond.set("deptId", deptId);
        cond.set("spdCode", spdCode);
        cond.set("goodsId", goodsId);
        cond.set("dateRange", dateRange);

        Page<Record> pageData = comService.getTagPage(page, limit, cond);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("查询库存数量分页")
    public void getTagStockQuantityList(String deptId, String goodsName, String cabinetName, int page, int limit) {
        Kv cond = Kv.by("a.deptId", deptId).set("c.name", goodsName).set("b.name", cabinetName);
        renderJson(comService.getStockTagQuantityPage(page, limit, cond));
    }

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("查询库存标签分页")
    public void getTagStockList(String deptId, String goodsId, String cabinetName, String type, String spdCode, int page, int limit) {
        Kv cond = Kv.by("deptId", deptId).set("goodsId", goodsId).set("cabinetName", cabinetName).set("type", type).set("spdCode", spdCode);

        Page<Record> pageData = comService.getStockTagPage(page, limit, cond);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @Before(GET.class)
    @NotNull({"deptId", "page", "limit"})
    @OperateLog("查询SPD库存标签分页")
    public void getSpdTagStockList(String deptId, String goodsId, String type, String spdCode, int page, int limit) {
        Kv cond = Kv.by("deptId", deptId).set("goodsId", goodsId).set("type", type).set("spdCode", spdCode);

        Page<Tag> pageData = comService.getSpdStockTagPage(page, limit, cond);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("查询出入柜分页")
    public void getTagRecordQuantityList(String deptId, String dateRange, String type, int page, int limit) {
        if (StringUtils.isEmpty(type)) {
            type = SysConstant.TagOut;
        }

        Kv cond = Kv.by("deptId", deptId).set("dateRange", dateRange).set("type", type);

        Page<Record> pageData = comService.getTagRecordQuantityPage(page, limit, cond);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("查询出入柜明细分页")
    public void getTagRecordList(String deptId, String goodsId, String dateRange, String type, String spdCode, int page, int limit) {
        if (StringUtils.isEmpty(type)) {
            type = "tagOut";
        }
        Kv cond = Kv.by("deptId", deptId).set("goodsId", goodsId).set("dateRange", dateRange).set("spdCode", spdCode);
        Page<Record> pageData = comService.getTagRecordPage(page, limit, cond, type);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("查询出入柜明细分页")
    public void getTagRecordListByExceptionDescId(String exceptionDescId, String type, int page, int limit) {
        Page<Record> pageData = comService.getTagRecordPage(page, limit, exceptionDescId, type);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @Before(GET.class)
    @OperateLog("查询出入库统计")
    public void getTagRecordChartsData(String type) {
        String dateStr;

        if (StringUtils.isEmpty(type) || type.equals("currentMonth")) {
            dateStr = DateUtils.getCurrentDate();
        } else if (type.equals("lastMonth")) {
            dateStr = DateUtils.addMonth(DateUtils.getCurrentDate(), -1);
        } else {
            renderJson(R.error(ResultCode.PARAM_IS_INVALID));
            return;
        }

        R result = comService.getTagRecordChartData(dateStr);
        renderJson(result);
    }

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("查询盘点统计分页")
    public void getTagInventoryQuantityList(String deptId, String dateRange, String type, int page, int limit) {
        Kv cond = Kv.by("deptId", deptId).set("dateRange", dateRange).set("type", type);

        Page<Record> pageData = comService.getTagInventoryRecordDifferencePage(page, limit, cond);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("无库存查询盘点统计分页")
    public void getTagInventoryQuantityListNoStock(int page, int limit) {
        Page<Record> pageData = comService.getTagInventoryNoStock(page, limit);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("查询盘点明细分页")
    public void getTagInventoryListByInventoryDifferenceId(String inventoryDifferenceId, int page, int limit) {
        Page<Record> pageData = comService.getTagInventoryListPage(page, limit, inventoryDifferenceId);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @Before(POST.class)
    @NotNull({"index", "count"})
    @OperateLog("一键测试")
    public void generateTestData(String deptId, Integer index, Integer count, String batchNo) {
        if (!CommonConfig.prop.getBoolean("jfinal.generateTestData", false)) {
            renderJson(R.error(ResultCode.INTERFACE_FORBID_VISIT));
            return;
        }

        R result = comService.generateTestData(index, count, batchNo, deptId, getLoginUserId());
        renderJson(result);
    }

    @Before(GET.class)
    @NotNull({"deptId", "dateRange", "type"})
    @OperateLog("查询出柜未归还记录列表")
    public void getTagOutRecordList(String deptId, String dateRange, String cabinetName, String userName, Integer type) {
        Kv cond = Kv.by("deptId", deptId)
                .set("dateRange", dateRange)
                .set("cabinetName", cabinetName)
                .set("userName", userName);

        List<Kv> kvList = comService.getTagOutWithoutReturnRecordListPage(cond, type);
        renderJson(R.ok().putData(kvList).put("count", kvList.size()));
    }

    @Before(GET.class)
    @NotNull({"spdCode"})
    @OperateLog("查询耗材历史记录")
    public void getTagHistory(String spdCode) {
        R result = comService.getTagHistory(spdCode);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"orderCode"})
    @OperateLog("取消配送单")
    public void cancelOrder(String orderCode) {
        if (StringUtils.isEmpty(orderCode)) {
            renderJson(R.error(ResultCode.PARAM_IS_BLANK));
            return;
        }

        R result = comService.cancelOrder(orderCode);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"orderCode"})
    @OperateLog("复核配送单")
    public void checkOrder(String orderCode) {
        R result = comService.confirmOrder(orderCode);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"spdCode"})
    @OperateLog("异常库存耗材手动出库")
    public void removeStockTag(String spdCode) {
        R result = comService.removeStockTag(spdCode);
        renderJson(result);
    }
}