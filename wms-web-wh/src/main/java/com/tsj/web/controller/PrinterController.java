package com.tsj.web.controller;

import com.alibaba.fastjson.JSON;
import com.jfinal.aop.Before;
import com.jfinal.aop.Inject;
import com.jfinal.ext.interceptor.GET;
import com.jfinal.ext.interceptor.POST;
import com.tsj.common.annotation.NotNull;
import com.tsj.common.annotation.OperateLog;
import com.tsj.common.utils.R;
import com.tsj.domain.model.Printer;
import com.tsj.service.PrinterService;
import com.tsj.web.common.MyController;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrinterController extends MyController {

    @Inject
    private PrinterService printerService;

    @Before(GET.class)
    @OperateLog("查询打印机")
    public void getPrinterList() {
        renderJson(R.ok().putData(printerService.getPrinterList()));
    }

    @Before(POST.class)
    @OperateLog("保存打印机")
    public void savePrinter() {
        String rawData = getRawData();
        log.debug("savePrinter request : {}",rawData);
        Printer printer = JSON.parseObject(rawData, Printer.class);
        printerService.savePrinter(printer);
        renderJson(R.ok().putData("success"));
    }

    @NotNull({"id"})
    @OperateLog("删除打印机")
    public void deletePrinter(Integer id) {
        printerService.deletePrinter(id);
        renderJson(R.ok().putData("success"));
    }
}
