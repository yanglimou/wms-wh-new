package com.tsj.web.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.aop.Inject;
import com.jfinal.ext.interceptor.GET;
import com.jfinal.ext.interceptor.POST;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.common.annotation.NotNull;
import com.tsj.common.annotation.OperateLog;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.utils.R;
import com.tsj.domain.model.Print;
import com.tsj.service.PrintService;
import com.tsj.web.common.MyController;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PrintController extends MyController {

    @Inject
    private PrintService printService;

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("查询打印分页")
    public void getPrintList(int page, int limit, String spdCode, String insNo) {
        Page<Record> pageData = printService.getPrintPage(page, limit, spdCode, insNo);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("查询打印分页2")
    public void getPrintList2(int page, int limit) {
        Page<Record> pageData = printService.getPrintPage2(page, limit);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @Before(GET.class)
    @OperateLog("查询在线打印机")
    public void getPrinter() {
        renderJson(R.ok().putData(printService.getPrinter()));
//        renderJson(R.ok().putData(MyWebSocket.getPrinter()));
    }

    @Before(POST.class)
    @NotNull({"insNo", "printId"})
    @OperateLog("执行打印操作")
    public void doPrint2(String insNo, String printId) {
        List<Record> recordList = printService.findByInsNo(insNo);
        StringBuilder highRequests = new StringBuilder();
        recordList.forEach(record -> {
            String name = record.getStr("name");
            String spec = "规格：" + record.getStr("spec");
            String manufacturerName = "厂家：" + record.getStr("manufacturerName");
            String lotNo = "批号：" + record.getStr("lotNo");
            String expireDate = "有效期：" + record.getStr("expireDate").substring(0, 10);
            String spdCode = "标签码：" + record.getStr("spdCode");
            String epc = record.getStr("epc");
            String unit = record.getStr("unit");
            String zpl = printService.getZpl(name, spec, manufacturerName, lotNo, expireDate, spdCode, epc);
            highRequests.append(zpl);
        });
        if (printService.print(printId, highRequests.toString())) {
            Db.update("update com_print set printFlag=1 , userId=? where insNo=?", getLoginUserId(), insNo);
            renderJson(R.ok().putData("success"));
        } else {
            renderJson(R.error(ResultCode.PRINTER_FAIL));
        }
    }

    @Before(POST.class)
    @OperateLog("执行打印操作")
    public void doPrint() {
        String rawData = getRawData();
        JSONObject requestObject = JSON.parseObject(rawData);
        if (handle(requestObject)) {
            renderJson(R.ok().putData("success"));
        } else {
            renderJson(R.error(ResultCode.PRINTER_FAIL));
        }
    }

    private boolean handle(JSONObject high) {
        String printId = high.getString("printId");
        StringBuilder printRequests = new StringBuilder();
        List list = new ArrayList();
        high.getJSONArray("data").stream().map(o -> (JSONObject) o).forEach(jsonObject -> {
            String name = jsonObject.getString("name");
            String spec = "规格：" + jsonObject.getString("spec");
            String manufacturerName = "厂家：" + jsonObject.getString("manufacturerName");
            String lotNo = "批号：" + jsonObject.getString("lotNo");
            String expireDate = "有效期：" + jsonObject.getString("expireDate").substring(0, 10);
            String spdCode = "标签码：" + jsonObject.getString("spdCode");
            String epc = jsonObject.getString("epc");
            String unit = jsonObject.getString("unit");
            String zpl = printService.getZpl(name, spec, manufacturerName, lotNo, expireDate, spdCode, epc);
            printRequests.append(zpl);

            Print print = Print.dao.findById(jsonObject.getString("spdCode"));
            print.setPrintFlag("1");
            print.setUserId(getLoginUserId());
            list.add(print);
        });
        if (printService.print(printId, printRequests.toString())) {
            Db.batchSave(list, 5000);
            return true;
        }
        return false;
    }
}
