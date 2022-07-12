package com.tsj.web.controller;

import com.jfinal.aop.Before;
import com.jfinal.aop.Inject;
import com.jfinal.ext.interceptor.GET;
import com.jfinal.ext.interceptor.POST;
import com.jfinal.kit.Kv;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.common.annotation.NotNull;
import com.tsj.common.annotation.OperateLog;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.constant.SysConstant;
import com.tsj.common.utils.R;
import com.tsj.domain.model.*;
import com.tsj.service.BaseService;
import com.tsj.service.CacheService;
import com.tsj.service.ComService;
import com.tsj.web.common.MyController;

/**
 * @className: BaseController
 * @description: 基础信息录入
 * @author: Frank
 * @create: 2020-07-06 10:52
 */
public class BaseController extends MyController {

    @Inject
    private BaseService baseService;

    @Inject
    private ComService comService;

    @Inject
    private CacheService cacheService;

    /******************************查询******************************/

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("查询科室分页")
    public void getDeptList(String name, int page, int limit) {
        Kv cond = Kv.by("name", name);

        Page<Record> pageData = baseService.getDeptPage(page, limit, cond);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("查询柜体分页")
    public void getCabinetList(String deptId, String name, int page, int limit) {
        Kv cond = Kv.by("deptId", deptId).set("name", name);

        Page<Record> pageData = baseService.getCabinetPage(page, limit, cond);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("查询人员分页")
    public void getUserList(String deptId, String name, String epc, int page, int limit) {
        Kv cond = Kv.by("deptId", deptId).set("name", name).set("epc", epc);

        Page<Record> pageData = baseService.getUserPage(page, limit, cond);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("查询耗材分页")
    public void getGoodsList(String name, String supplierId, String manufacturerId, int page, int limit) {
        Kv cond = Kv.by("name", name).set("supplierId", supplierId).set("manufacturerId", manufacturerId);

        Page<Record> pageData = baseService.getGoodsPage(page, limit, cond);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("查询生产商分页")
    public void getManufacturerList(String name, int page, int limit) {
        Kv cond = Kv.by("name", name);

        Page<Record> pageData = baseService.getManufacturerPage(page, limit, cond);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    @Before(GET.class)
    @NotNull({"page", "limit"})
    @OperateLog("查询供应商分页")
    public void getSupplierList(String name, int page, int limit) {
        Kv cond = Kv.by("name", name);

        Page<Record> pageData = baseService.getSupplierPage(page, limit, cond);
        renderJson(R.ok().putData(pageData.getList()).put("count", pageData.getTotalRow()));
    }

    /******************************编辑******************************/

    @Before(POST.class)
    @NotNull({"name", "code"})
    @OperateLog("保存科室")
    public void saveDept(String id, String name, String code, String pinyin, String caption) {
        Dept dept = new Dept()
                .setId(id)
                .setName(name)
                .setCode(code)
                .setPinyin(pinyin)
                .setCaption(caption);

        R result = baseService.saveOrUpdateDept(dept, getLoginUserId());
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"deptId", "name", "code", "type"})
    @OperateLog("保存高值柜")
    public void saveCabinet(String id, String deptId, String code, String name, String ip, String type) {
        Cabinet cabinet = new Cabinet()
                .setId(id)
                .setDeptId(deptId)
                .setCode(code)
                .setName(name)
                .setIp(ip)
                .setType(type);

        R result = baseService.saveOrUpdateCabinet(cabinet, getLoginUserId());
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"name", "code", "spec", "price", "unit", "supplierId", "manufacturerId", "implant"})
    @OperateLog("保存耗材")
    public void saveGoods(String id, String name, String commonName, String tradeName, String code, String spec, Double price,
                          String unit, String supplierId, String manufacturerId, String implant) {
        Goods goods = new Goods()
                .setId(id)
                .setName(name)
                .setCommonName(commonName)
                .setTradeName(tradeName)
                .setCode(code)
                .setSpec(spec)
                .setPrice(price)
                .setUnit(unit)
                .setSupplierId(supplierId)
                .setManufacturerId(manufacturerId)
                .setImplant(implant);

        R result = baseService.saveOrUpdateGoods(goods, getLoginUserId());
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"code", "name"})
    @OperateLog("保存生产商")
    public void saveManufacturer(String id, String code, String name, String pinyin, String linkman, String mobile, String location) {
        Manufacturer manufacturer = new Manufacturer()
                .setId(id)
                .setCode(code)
                .setName(name)
                .setPinyin(pinyin)
                .setLinkman(linkman)
                .setMobile(mobile)
                .setLocation(location);

        R result = baseService.saveOrUpdateManufacturer(manufacturer, getLoginUserId());
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"code", "name"})
    @OperateLog("保存供应商")
    public void saveSupplier(String id, String code, String name, String pinyin, String linkman, String mobile, String location) {
        Supplier supplier = new Supplier()
                .setId(id)
                .setCode(code)
                .setName(name)
                .setPinyin(pinyin)
                .setLinkman(linkman)
                .setMobile(mobile)
                .setLocation(location);

        R result = baseService.saveOrUpdateSupplier(supplier, getLoginUserId());
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"id","name"})
    @OperateLog("保存人员")
    public void saveUser(String id, String name, String username, String deptId, String roles) {
        User user = new User()
                .setId(id)
                .setName(name)
                .setUsername(username)
                .setDeptId(deptId)
                .setRoles(roles);

        R result = baseService.saveOrUpdateUser(user, getLoginUserId());
        renderJson(result);
    }

    /******************************修改******************************/

    @NotNull("userId")
    @OperateLog(value = "重置密码")
    public void resetPwd(String userId) {
        R result = baseService.resetPwd(userId);
        renderJson(result);
    }

    @NotNull({"userId", "oldPassword", "newPassword"})
    @OperateLog(value = "修改密码")
    public void changePwd(String userId, String oldPassword, String newPassword) {
        R result = baseService.updatePassword(userId, oldPassword, newPassword);
        renderJson(result);
    }

    @NotNull({"type", "id", "enable"})
    @OperateLog(value = "修改数据状态")
    public void updateState(String type, String id, String enable) {
        boolean state = false;
        switch (type) {
            case "user":
                User user = cacheService.getUserById(id);
                user.setEnable(enable);
                state = user.update();
                cacheService.removeCache(SysConstant.CacheBase, SysConstant.UserById, user.getId());
                break;

            case "dept":
                Dept dept = cacheService.getDeptById(id);
                dept.setEnable(enable);
                state = dept.update();
                cacheService.removeCache(SysConstant.CacheBase, SysConstant.DeptById, dept.getId());
                break;

            case "cabinet":
                Cabinet cabinet = cacheService.getCabinetById(id);
                cabinet.setEnable(enable);
                state = cabinet.update();
                cacheService.removeCache(SysConstant.CacheBase, SysConstant.CabinetById, cabinet.getId());
                break;

            case "goods":
                Goods goods = cacheService.getGoodsById(id);
                goods.setEnable(enable);
                state = goods.update();
                cacheService.removeCache(SysConstant.CacheBase, SysConstant.GoodsById, goods.getId());
                break;

            case "manufacturer":
                Manufacturer manufacturer = cacheService.getManufacturerById(id);
                manufacturer.setEnable(enable);
                state = manufacturer.update();
                cacheService.removeCache(SysConstant.CacheBase, SysConstant.ManufacturerById, manufacturer.getId());
                break;

            case "supplier":
                Supplier supplier = cacheService.getSupplierById(id);
                supplier.setEnable(enable);
                state = supplier.update();
                cacheService.removeCache(SysConstant.CacheBase, SysConstant.SupplierById, supplier.getId());
                break;

        }

        renderJson(state ? R.ok() : R.error(ResultCode.UPDATE_FAIL));
    }

    @NotNull({"epc"})
    @OperateLog("现有标签解绑EPC")
    public void deleteTagEpc(String epc) {
        R result = comService.deleteTagEpc(epc, getLoginUserId());
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"spdCode", "epc"})
    @OperateLog("现有标签绑定EPC")
    public void saveTagEpc(String spdCode, String epc) {
        R result = comService.saveTagEpc(spdCode, epc, getLoginUserId());
        renderJson(result);
    }
}