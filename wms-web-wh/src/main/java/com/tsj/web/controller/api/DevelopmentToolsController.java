package com.tsj.web.controller.api;

import com.jfinal.aop.Before;
import com.jfinal.aop.Clear;
import com.jfinal.ext.interceptor.GET;
import com.jfinal.kit.Kv;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.common.annotation.NotNull;
import com.tsj.common.annotation.OperateLog;
import com.tsj.common.config.CommonConfig;
import com.tsj.common.constant.SpdUrl;
import com.tsj.common.utils.HttpKit;
import com.tsj.common.utils.R;
import com.tsj.service.interceptor.AuthInterceptor;
import com.tsj.service.spdStockTag.SpdStockTagContainer;
import com.tsj.web.common.MyController;

import java.util.List;

@Clear(AuthInterceptor.class)
public class DevelopmentToolsController extends MyController {
    @Before(GET.class)
    @OperateLog("查询全部spd库存")
    public void getAllSpdStock() {
        renderJson(R.ok().putData(SpdStockTagContainer.getConcurrentHashMap()));
    }

    @Before(GET.class)
    @NotNull("deptId")
    @OperateLog("部门查询库存缓存")
    public void getSpdStockCache(String deptId) {
        renderJson(R.ok().putData(SpdStockTagContainer.getByDept(deptId)));
    }

    @Before(GET.class)
    @NotNull("deptId")
    @OperateLog("部门查询库存真实")
    public void getSpdStockTrue(String deptId) {
        List<Record> recordList = HttpKit.postSpdData(CommonConfig.prop.get("SPD_BASE_URL") + SpdUrl.URL_STOCK_TAG.getUrl(), null,
                Kv.by("DeptId", deptId));
        renderJson(R.ok().putData(recordList));
    }
}
