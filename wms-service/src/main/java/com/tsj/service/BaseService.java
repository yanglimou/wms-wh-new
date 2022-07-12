package com.tsj.service;

import cn.hutool.core.date.DateUtil;
import com.jfinal.aop.Inject;
import com.jfinal.kit.Kv;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.constant.SysConstant;
import com.tsj.common.utils.QueryCondition;
import com.tsj.common.utils.QueryConditionBuilder;
import com.tsj.common.utils.R;
import com.tsj.domain.model.*;
import com.tsj.service.common.MyService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @className: BaseService
 * @description: 基础数据服务
 * @author: Frank
 * @create: 2020-03-13 15:23
 */
public class BaseService extends MyService {

    public static final Log logger = Log.getLog(BaseService.class);

    @Inject
    private CacheService cacheService;

    /******************************编辑******************************/

    public R saveOrUpdateCabinet(Cabinet cabinet, String userId) {
        String time = DateUtil.now();
        if (StringUtils.isEmpty(cabinet.getId())) {
            //判断对象是否存在
            if (cacheService.getCabinetById(cabinet.getCode()) != null) {
                return R.error(ResultCode.DATA_ALREADY_EXISTED, "编号不能重复");
            }
            return cabinet.setId(cabinet.getCode())
                    .setEnable(StateEnable)
                    .setCreateUserId(userId)
                    .setModifyUserId(userId)
                    .setCreateDate(time)
                    .setModifyDate(time)
                    .save() ? R.ok() : R.error(ResultCode.CREATE_FAIL);
        } else {
            //判断对象是否存在
            if (cacheService.getCabinetById(cabinet.getId()) == null) {
                return R.error(ResultCode.DATA_NOT_EXISTED, "目标柜不存在");
            }

            //清除缓存
            cacheService.removeCache(CacheBase, CabinetById, cabinet.getCode());

            return cabinet.setModifyUserId(userId)
                    .setModifyDate(time)
                    .update() ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
        }
    }

    public R saveOrUpdateGoods(Goods goods, String userId) {
        String time = DateUtil.now();

        Supplier supplier = cacheService.getSupplierById(goods.getSupplierId());
        Manufacturer manufacturer = cacheService.getManufacturerById(goods.getManufacturerId());

        //判断对象是否存在
        if (supplier == null || manufacturer == null) {
            return R.error(ResultCode.DATA_NOT_EXISTED, "供应商或生产商不存在");
        }

        if (StringUtils.isEmpty(goods.getId())) {
            //判断对象是否存在
            if (cacheService.getGoodsById(goods.getCode()) != null) {
                return R.error(ResultCode.DATA_ALREADY_EXISTED, "编号不能重复");
            }

            return goods.setId(goods.getCode())
                    .setEnable(StateEnable)
                    .setCreateUserId(userId)
                    .setModifyUserId(userId)
                    .setCreateDate(time)
                    .setModifyDate(time)
                    .save() ? R.ok() : R.error(ResultCode.CREATE_FAIL);
        } else {
            //判断对象是否存在
            if (cacheService.getGoodsById(goods.getId()) == null) {
                return R.error(ResultCode.DATA_NOT_EXISTED, "目标耗材不存在");
            }

            //清除缓存
            cacheService.removeCache(CacheBase, GoodsById, goods.getCode());

            return goods.setModifyUserId(userId)
                    .setModifyDate(time)
                    .update() ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
        }
    }

    public R saveOrUpdateDept(Dept dept, String userId) {
        String time = DateUtil.now();

        if (StringUtils.isEmpty(dept.getId())) {
            //判断对象是否存在
            if (cacheService.getDeptById(dept.getCode()) != null) {
                return R.error(ResultCode.DATA_ALREADY_EXISTED, "编号不能重复");
            }

            return dept.setId(dept.getCode())
                    .setEnable(StateEnable)
                    .setCreateUserId(userId)
                    .setModifyUserId(userId)
                    .setCreateDate(time)
                    .setModifyDate(time)
                    .save() ? R.ok() : R.error(ResultCode.CREATE_FAIL);
        } else {
            //判断对象是否存在
            if (cacheService.getDeptById(dept.getId()) == null) {
                return R.error(ResultCode.DATA_NOT_EXISTED, "目标科室不存在");
            }

            //清除缓存
            cacheService.removeCache(CacheBase, DeptById, dept.getCode());
            cacheService.removeCache(CacheBase, DeptByName, dept.getName());

            return dept.setModifyUserId(userId)
                    .setModifyDate(time)
                    .update() ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
        }
    }

    public R saveOrUpdateManufacturer(Manufacturer manufacturer, String userId) {
        String time = DateUtil.now();
        if (StringUtils.isEmpty(manufacturer.getId())) {
            //判断对象是否存在
            if (cacheService.getManufacturerById(manufacturer.getCode()) != null) {
                return R.error(ResultCode.DATA_ALREADY_EXISTED, "编号不能重复");
            }
            return manufacturer.setId(manufacturer.getCode())
                    .setEnable(StateEnable)
                    .setCreateUserId(userId)
                    .setModifyUserId(userId)
                    .setCreateDate(time)
                    .setModifyDate(time)
                    .save() ? R.ok() : R.error(ResultCode.CREATE_FAIL);
        } else {
            //判断对象是否存在
            if (cacheService.getManufacturerById(manufacturer.getId()) == null) {
                return R.error(ResultCode.DATA_NOT_EXISTED, "目标生产商不存在");
            }

            //清除缓存
            cacheService.removeCache(CacheBase, ManufacturerById, manufacturer.getCode());
            cacheService.removeCache(CacheBase, ManufacturerByName, manufacturer.getName());

            return manufacturer.setModifyUserId(userId)
                    .setModifyDate(time)
                    .update() ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
        }
    }

    public R saveOrUpdateSupplier(Supplier supplier, String userId) {
        String time = DateUtil.now();
        if (StringUtils.isEmpty(supplier.getId())) {
            //判断对象是否存在
            if (cacheService.getSupplierById(supplier.getCode()) != null) {
                return R.error(ResultCode.DATA_ALREADY_EXISTED, "编号不能重复");
            }
            return supplier.setId(supplier.getCode())
                    .setEnable(StateEnable)
                    .setCreateUserId(userId)
                    .setModifyUserId(userId)
                    .setCreateDate(time)
                    .setModifyDate(time)
                    .save() ? R.ok() : R.error(ResultCode.CREATE_FAIL);
        } else {
            //判断对象是否存在
            if (cacheService.getSupplierById(supplier.getId()) == null) {
                return R.error(ResultCode.DATA_NOT_EXISTED, "目标供应商不存在");
            }

            //清除缓存
            cacheService.removeCache(CacheBase, SupplierById, supplier.getCode());
            cacheService.removeCache(CacheBase, SupplierByName, supplier.getName());

            return supplier.setModifyUserId(userId)
                    .setModifyDate(time)
                    .update() ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
        }
    }

    public R saveOrUpdateUser(User user, String userId) {
        String time = DateUtil.now();
        User oldUser = cacheService.getUserById(user.getId());
        if (oldUser == null) {
            return user.setEnable(StateEnable)
                    .setCreateUserId(userId)
                    .setModifyUserId(userId)
                    .setCreateDate(time)
                    .setModifyDate(time)
                    .save() ? R.ok() : R.error(ResultCode.CREATE_FAIL);
        } else {
            //清除缓存
            cacheService.removeCache(CacheBase, UserById, user.getId());

            return oldUser
                    .setName(user.getName())
                    .setUsername(user.getUsername())
                    .setDeptId(user.getDeptId())
                    .setRoles(user.getRoles())
                    .setModifyUserId(userId)
                    .setModifyDate(time)
                    .update() ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
        }
    }

    /******************************查询******************************/

    public List<Record> getCabinetList(Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_EQUAL, "deptId")
                .put(SQL_PATTERN_LIKE, "name")
                .build();

        String select = "select * from base_cabinet";
        return Db.find(select + condition.getSql(), condition.getParas());
    }

    public List<Record> getDeptList(Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_LIKE, "name")
                .build();

        String select = "select * from base_dept";
        return Db.find(select + condition.getSql(), condition.getParas());
    }

    public List<Record> getGoodsList(Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_LIKE, "name")
                .put(SQL_PATTERN_LIKE, "commonName")
                .put(SQL_PATTERN_EQUAL, "manufactureId")
                .order(SQL_SORT_ASC, "id")
                .build();

        String select = "select * from base_goods";
        return Db.find(select + condition.getSql(), condition.getParas());
    }

    public List<Record> getManufacturerList(Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_LIKE, "name")
                .build();

        String select = "select manufacturer.* from base_manufacturer manufacturer";
        return Db.find(select + condition.getSql(), condition.getParas());
    }

    public List<Record> getSupplierList(Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_LIKE, "name")
                .build();

        String select = "select supplier.* from base_supplier supplier";
        return Db.find(select + condition.getSql(), condition.getParas());
    }

    public List<Record> getUserList(Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_EQUAL, "deptId")
                .put(SQL_PATTERN_LIKE, "name")
                .build();

        String select = "select user.* from base_user user";
        return Db.find(select + condition.getSql(), condition.getParas());
    }

    public List<String[]> getGoodsToArrayList(Kv cond) {
        List<String[]> list = new ArrayList<>();
        getGoodsList(cond).forEach(record -> {
            Supplier supplier = cacheService.getSupplierById(record.getStr("supplierId"));
            Manufacturer manufacturer = cacheService.getManufacturerById(record.getStr("manufacturerId"));

            String[] array = new String[11];
            int cells = 0;
            array[cells++] = record.getStr("name");
            array[cells++] = record.getStr("commonName");
            array[cells++] = record.getStr("tradeName");
            array[cells++] = record.getStr("code");
            array[cells++] = record.getStr("spec");
            array[cells++] = record.getStr("price");
            array[cells++] = record.getStr("unit");
            array[cells++] = supplier == null ? "未知" : supplier.getName();
            array[cells++] = manufacturer == null ? "未知" : manufacturer.getName();
            array[cells++] = record.getStr("implant");
            array[cells] = record.getStr("enable");
            list.add(array);
        });
        return list;
    }

    /******************************分页查询******************************/

    public Page<Record> getDeptPage(int pageNumber, int pageSize, Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_LIKE, "name")
                .order(SQL_SORT_ASC, "name")
                .build();

        String select = "select * ";
        String sqlExceptSelect = " from base_dept ";
        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect + condition.getSql(), condition.getParas());
    }

    public Page<Record> getCabinetPage(int pageNumber, int pageSize, Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_EQUAL, "deptId")
                .put(SQL_PATTERN_LIKE, "name")
                .order(SQL_SORT_ASC, "name")
                .build();

        String select = "select * ";
        String sqlExceptSelect = " from base_cabinet ";
        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect + condition.getSql(), condition.getParas());
    }

    public Page<Record> getUserPage(int pageNumber, int pageSize, Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_EQUAL, "deptId")
                .put(SQL_PATTERN_LIKE, "name")
                .put(SQL_PATTERN_LIKE, "epc")
                .build();

        String select = "select * ";
        String sqlExceptSelect = " from base_user ";
        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect + condition.getSql(), condition.getParas());
    }

    public Page<Record> getGoodsPage(int pageNumber, int pageSize, Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_EQUAL, "manufacturerId")
                .put(SQL_PATTERN_EQUAL, "supplierId")
                .put(SQL_PATTERN_LIKE, "name")
                .build();

        String select = "select * ";
        String sqlExceptSelect = " from base_goods";
        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect + condition.getSql(), condition.getParas());
    }

    public Page<Record> getManufacturerPage(int pageNumber, int pageSize, Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_LIKE, "name")
                .order(SQL_SORT_ASC, "name")
                .build();

        String select = "select * ";
        String sqlExceptSelect = " from base_manufacturer ";
        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect + condition.getSql(), condition.getParas());
    }

    public Page<Record> getSupplierPage(int pageNumber, int pageSize, Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_LIKE, "name")
                .order(SQL_SORT_ASC, "name")
                .build();

        String select = "select * ";
        String sqlExceptSelect = " from base_supplier ";
        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect + condition.getSql(), condition.getParas());
    }

    /******************************修改******************************/

    public R updatePassword(String userId, String oldPassword, String newPassword) {
        User user = cacheService.getUserById(userId);
        if (user == null) {
            return R.error(ResultCode.USER_NOT_EXIST);
        }

        if (!user.getPassword().equals(oldPassword)) {
            return R.error(ResultCode.USER_LOGIN_ERROR);
        }

        R result = user.setPassword(newPassword)
                .setModifyUserId(userId)
                .setModifyDate(DateUtil.now())
                .update() ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
        //清除缓存
        if (result.isSuccess()) {
            cacheService.removeCache(CacheBase, UserById, userId);
        }
        return result;
    }

    public R resetPwd(String userId) {
        User user = cacheService.getUserById(userId);
        if (user == null) {
            return R.error(ResultCode.USER_NOT_EXIST);
        }

        //清除缓存
        cacheService.removeCache(CacheBase, UserById, userId);

        user.setPassword(SysConstant.RESET_PASSWORD);
        return user.update() ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
    }

    /******************************END******************************/
}