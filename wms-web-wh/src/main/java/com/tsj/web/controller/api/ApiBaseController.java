package com.tsj.web.controller.api;

import com.jfinal.aop.Before;
import com.jfinal.aop.Clear;
import com.jfinal.aop.Inject;
import com.jfinal.ext.interceptor.GET;
import com.jfinal.kit.Kv;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.common.annotation.OperateLog;
import com.tsj.common.utils.R;
import com.tsj.service.BaseService;
import com.tsj.service.interceptor.AuthInterceptor;
import com.tsj.web.common.MyController;

import java.util.List;

/**
 * @className: ApiBaseController
 * @description: 基础数据控制器
 * @author: Frank
 * @create: 2020-03-24 10:49
 */
@Clear(AuthInterceptor.class)
public class ApiBaseController extends MyController {

    @Inject
    private BaseService baseService;

    @Before(GET.class)
    @OperateLog("查询科室列表")
    public void getDeptList(String name) {
        Kv cond = Kv.by("name", name);

        List<Record> recordList = baseService.getDeptList(cond);
        renderJson(R.ok().putData(recordList));
    }

    @Before(GET.class)
    @OperateLog("查询高值柜列表")
    public void getCabinetList(String name) {
        Kv cond = Kv.by("name", name);

        List<Record> recordList = baseService.getCabinetList(cond);
        renderJson(R.ok().putData(recordList));
    }

    @Before(GET.class)
    @OperateLog("查询生产商列表")
    public void getManufacturerList(String name) {
        Kv cond = Kv.by("name", name);

        List<Record> recordList = baseService.getManufacturerList(cond);
        renderJson(R.ok().putData(recordList));
    }

    @Before(GET.class)
    @OperateLog("查询供应商列表")
    public void getSupplierList(String name) {
        Kv cond = Kv.by("name", name);

        List<Record> recordList = baseService.getSupplierList(cond);
        renderJson(R.ok().putData(recordList));
    }

    @Before(GET.class)
    @OperateLog("查询耗材列表")
    public void getGoodsList(String name) {
        Kv cond = Kv.by("name", name);

        List<Record> recordList = baseService.getGoodsList(cond);
        renderJson(R.ok().putData(recordList));
    }

    @Before(GET.class)
    @OperateLog("查询用户列表")
    public void getUserList(String name) {
        Kv cond = Kv.by("name", name);

        List<Record> recordList = baseService.getUserList(cond);
        renderJson(R.ok().putData(recordList));
    }
}