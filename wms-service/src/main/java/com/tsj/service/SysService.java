package com.tsj.service;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.jfinal.aop.Inject;
import com.jfinal.kit.Kv;
import com.jfinal.kit.StrKit;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.common.config.CommonConfig;
import com.tsj.common.constant.FileConstant;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.utils.*;
import com.tsj.domain.model.*;
import com.tsj.service.common.MyService;
import com.tsj.tcp.tcpserver.business.Person;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

/**
 * @className: SysOperateService
 * @description: 系统数据服务
 * @author: Frank
 * @create: 2020-03-16 13:32
 */
public class SysService extends MyService {

    public static final Log logger = Log.getLog(SysService.class);

    @Inject
    private CacheService cacheService;


    public R saveLogLogin(String[] userIdArray, String cabinetId, String time) {
        List<LogLogin> list = Lists.newArrayList();
        for (String userId : userIdArray) {
            LogLogin log = new LogLogin()
                    .setId(IDGenerator.makeId())
                    .setCabinetId(cabinetId)
                    .setCreateUserId(userId)
                    .setCreateDate(time);
            list.add(log);
        }

        boolean succeed = Db.tx(() -> {
            Db.batchSave(list, batchSize);
            return true;
        });

        return succeed ? R.ok() : R.error(ResultCode.CREATE_FAIL);
    }

    public R saveUserFinger(String fingerUserId, int fingerNo, String fingerValue, String createUserId, String createDate) {
        //判断用户是否存在
        User user = cacheService.getUserById(fingerUserId);
        if (user == null) {
            return R.error(ResultCode.USER_NOT_EXIST);
        }

        //判断指纹是否登记
        UserFinger userFinger = UserFinger.dao.findFirst("SELECT * FROM sys_user_finger WHERE deptId=? and fingerNo=?", user.getDeptId(), fingerNo);
        if (userFinger != null) {
            return R.error(ResultCode.DATA_ALREADY_EXISTED, "目标指纹编号已占用，请申请新的指纹编号");
        }

        UserFinger newserFinger = new UserFinger()
                .setId(IDGenerator.makeId())
                .setDeptId(user.getDeptId())
                .setFingerUserId(fingerUserId)
                .setFingerNo(fingerNo)
                .setFingerValue(fingerValue)
                .setCreateUserId(createUserId)
                .setCreateDate(createDate);

        boolean succeed = Db.tx(newserFinger::save);
        if (succeed) {
            Person.addOne(fingerNo + "", fingerValue);
        }
        return succeed ? R.ok() : R.error(ResultCode.CREATE_FAIL);
    }

    public R deleteUserFinger(String userId, String fingerUserId, String deptId) {
        int rows = 0;
        if (!StringUtils.isEmpty(deptId)) {
            rows = Db.delete("delete from sys_user_finger WHERE deptId=?", deptId);
            if (rows > 0) {
                //删除全部指纹
                Person.deleteAll();
            }
        } else if (!StringUtils.isEmpty(fingerUserId)) {
            List<String> fingerNosByFingerUserId = getFingerNosByFingerUserId(fingerUserId);
            if (CollectionUtils.isNotEmpty(fingerNosByFingerUserId)) {
                rows = Db.delete("delete from sys_user_finger WHERE fingerUserId=?", fingerUserId);
                if (rows > 0) {
                    //删除单个指纹
                    Person.deleteOnePerson(fingerNosByFingerUserId);
                }
            }
        }
        logger.warn("{} deleteUserFinger rows:{}", userId, rows);
        return rows > 0 ? R.ok() : R.error(ResultCode.DELETE_FAIL);
    }

    public R deleteUserCode(String userId, String codeUserId, String deptId) {
        int rows = 0;
        if (!StringUtils.isEmpty(deptId)) {
            rows = Db.update("update base_user set code=null,modifyUserId=?,modifyDate=? WHERE deptId=?", userId, DateUtil.now(), deptId);
        } else if (!StringUtils.isEmpty(codeUserId)) {
            rows = Db.update("update base_user set code=null,modifyUserId=?,modifyDate=? WHERE id=?", userId, DateUtil.now(), codeUserId);
        }
        logger.warn("{} updateUserCode rows:{}", userId, rows);
        return rows > 0 ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
    }

    public R updateUserCode(String userId, String codeUserId, String code) {
        User user = cacheService.getUserById(codeUserId);
        if (user == null) {
            return R.error(ResultCode.USER_NOT_EXIST);
        }
        int rows = Db.update("update base_user set code = ?,modifyUserId=?,modifyDate=? WHERE id=?", code, userId, DateUtil.now(), codeUserId);
        logger.warn("{} updateUserCode rows:{}", userId, rows);
        return rows > 0 ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
    }

    public R updateUserEpc(String userId, String epcUserId, String epc) {
        User user = cacheService.getUserById(epcUserId);
        if (user == null) {
            return R.error(ResultCode.USER_NOT_EXIST);
        }

        //标签前缀过滤
        if (!epc.startsWith(CommonConfig.prop.get("UserTagPrefix"))) {
            return R.error(ResultCode.DATA_IS_INVALID);
        }

        User userByEpc = User.dao.findFirst("select * from base_user where epc=?", epc);
        if (userByEpc != null) {
            logger.info(JSON.toJSONString(userByEpc));
            return R.error(ResultCode.DATA_ALREADY_EXISTED, "标签已绑定到，不允许重复关联");
        }

        cacheService.removeCache(CacheBase, UserById, epcUserId);
        int rows = Db.update("update base_user set epc=?,modifyUserId=?,modifyDate=? WHERE id=?", epc, userId, DateUtil.now(), epcUserId);
        logger.debug("{} updateUserEpc rows:{}", userId, rows);
        return rows > 0 ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
    }

    public R reset(String password) {
        if (!password.equals(CommonConfig.prop.get("password"))) {
            return R.error(ResultCode.SAFE_PASSWORD_ERROR);
        }

        boolean succeed = Db.tx(() -> {

            //删除科室、人员、耗材种类、供应商、生产商
            Db.delete("delete from base_dept");
            Db.delete("delete from base_user");
            Db.delete("delete from base_goods");
            Db.delete("delete from base_supplier");
            Db.delete("delete from base_manufacturer");

            //删除制标
            Db.delete("delete from com_tag");

            //删除入柜、出柜、盘点记录
            Db.delete("delete from com_record_in");
            Db.delete("delete from com_record_out");
            Db.delete("delete from com_record_inventory");
            Db.delete("delete from com_record_inventory_difference");
            Db.delete("delete from com_record_inout_exception");

            //删除库存记录
            Db.delete("delete from com_stock_tag");
            Db.delete("delete from com_stock_base");

            //删除日志
            Db.delete("delete from sys_log_login");
            Db.delete("delete from sys_log_operate");
            Db.delete("delete from sys_log_spd");

            //清空缓存
            cacheService.clearCache();
            return true;
        });

        return succeed ? R.ok() : R.error(ResultCode.CREATE_FAIL);
    }

    public R updateConfig(String userId, String type, String name, String value) {
        Config config = getConfig(type, name);
        if (config == null) {
            return R.error(ResultCode.DATA_NOT_EXISTED, "该系统配置不存在");
        }
        return config.setValue(value)
                .setModifyUserId(userId)
                .setModifyDate(DateUtil.now())
                .update() ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
    }

    public Config getConfig(String type, String name) {
        return Config.dao.findFirst("select * from sys_config where type=? and name=?", type, name);
    }

    public List<Config> getConfigList() {
        return Config.dao.findAll();
    }

    public List<Record> getUserFingerList(String deptId) {
        String select = "SELECT * FROM sys_user_finger WHERE deptId=? ORDER BY fingerNo ASC";
        return Db.find(select, deptId);
    }

    public String getUserFingerLastUpdateTime(String deptId) {
        String select = "SELECT MAX(createDate) as lastUpdateTime, count(id) as total from sys_user_finger where deptId=?";
        Record record = Db.findFirst(select, deptId);
        return record == null ? null : record.getStr("lastUpdateTime") + "_" + record.getInt("total");
    }

    public String getTagStockLastUpdateTime(String cabinetId) {
        String select = "SELECT MAX(createDate) as lastUpdateTime, count(spdCode) as total from com_stock_tag where cabinetId=?";
        Record record = Db.findFirst(select, cabinetId);
        return record == null ? null : record.getStr("lastUpdateTime") + "_" + record.getInt("total");
    }


    public List<String> getFingerNosByFingerUserId(String fingerUserId) {
        String select = "SELECT fingerNo FROM sys_user_finger WHERE fingerUserId=?";
        return Db.find(select, fingerUserId).stream().map(record -> record.getStr("fingerNo")).collect(Collectors.toList());
    }

    public String getLastUpdateTime(String table) {
        String select = "SELECT MAX(modifyDate) as lastUpdateTime, count(id) as total from " + table;
        Record record = Db.findFirst(select);
        return record == null ? null : record.getStr("lastUpdateTime") + "_" + record.getInt("total");
    }

    public String getCabinetPhoto(String cabinetId, String time) {
        return FileConstant.CABINET_PATH
                + time.substring(0, 10) + "/"
                + cabinetId + time.substring(11, 19).replace(":", "") + ".jpg";
    }

    public R getUser(String username, String password) {
        User user = User.dao.findFirst("select * from base_user where username=?", username);
        if (user == null || !user.getPassword().equals(password)) {
            return R.error(ResultCode.USER_LOGIN_ERROR);
        }

        if (user.getEnable().equals("false")) {
            return R.error(ResultCode.USER_ACCOUNT_FORBIDDEN);
        }

        return R.ok().putData(user);
    }

    public int getFreeUserFingerNo(String deptId) {
        List<Record> recordList = Db.find("SELECT fingerNo FROM sys_user_finger WHERE deptId=?", deptId);
        if (recordList == null || recordList.isEmpty()) {
            return 0;
        }

        int maxElement = 0;
        int[] arr = recordList.stream().mapToInt(p -> p.getInt("fingerNo")).toArray();
        OptionalInt optionalInt = Arrays.stream(arr).max();
        if (optionalInt.isPresent()) {
            maxElement = optionalInt.getAsInt();
        }
        return MyKit.getMissingNumber(arr, maxElement);
    }

    public Kv getMenuTree(String excludeMenuIds) {
        List<String> idList = new ArrayList<>();
        if (StringUtils.isNotEmpty(excludeMenuIds)) {
            idList = Arrays.asList(excludeMenuIds.split(","));
        }

        Kv kv = Kv.create();
        String[] menuTypes = {"levelOne", "levelTwo", "sys"};
        for (String menuType : menuTypes) {
            List<Menu> newMenuList = new ArrayList<>();
            List<Menu> menuList = Menu.dao.find("select * from sys_menu where menuType = ? order by sort asc", menuType);
            for (Menu menu : menuList) {
                if (!idList.contains(menu.getId().toString())) {
                    newMenuList.add(menu);
                }
            }

            kv.put(menuType, listMenuToTree(newMenuList));//调用TreeTest工具类方法生成树形结构的List集合
        }
        return kv;
    }

    public List<Menu> getMenuList(String menuType) {
        if (StrKit.isBlank(menuType)) {
            return Menu.dao.find("select * from sys_menu order by sort asc");
        } else {
            return Menu.dao.find("select * from sys_menu where menuType = '?' order by sort asc", menuType);
        }
    }

    public List<Menu> listMenuToTree(List<Menu> list) {
        //用递归找子。
        List<Menu> treeList = new ArrayList<Menu>();
        list.forEach(tree -> {
            if (tree.getPid() == -1) {
                treeList.add(findChildren(tree, list));
            }
        });
        return treeList;
    }

    private Menu findChildren(Menu tree, List<Menu> list) {
        list.forEach(node -> {
            if (node.getPid().equals(tree.getId())) {
                if (tree.getChildren() == null) {
                    tree.setChildren(new ArrayList<Menu>());
                }
                tree.getChildren().add(findChildren(node, list));
            }
        });
        return tree;
    }

    public Kv getDataDicList() {
        Kv kv = Kv.create();

        //TODO 减少sessionStorage的存储空间，查询部分字段
        kv.set("deptList", Dept.dao.find("select id,name from base_dept order by name asc"));
        kv.set("cabinetList", Cabinet.dao.find("select id,name,deptId from base_cabinet order by name asc"));
        kv.set("userList", User.dao.find("select id,name,deptId from base_user order by name asc"));
        kv.set("manufacturerList", Manufacturer.dao.find("select id,name from base_manufacturer order by name asc"));
        kv.set("supplierList", Supplier.dao.find("select id,name from base_supplier order by name asc"));
        kv.set("goodsList", Db.find("select id,name,spec,unit,manufacturerId,supplierId from base_goods order by name asc"));
        return kv;
    }

    public Kv getConfigData() {
        Kv kv = Kv.create();
        Config.dao.findAll().forEach(config -> {
            kv.set(config.getType() + config.getName(), config.getValue());
        });
        return kv;
    }

    public User getUser(Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_EQUAL, "name")
                .put(SQL_PATTERN_EQUAL, "username")
                .put(SQL_PATTERN_EQUAL, "password")
                .put(SQL_PATTERN_EQUAL, "code")
                .put(SQL_PATTERN_EQUAL, "duty")
                .put(SQL_PATTERN_EQUAL, "deptId")
                .build();

        String select = "select * from base_user";
        return User.dao.findFirst(select + condition.getSql(), condition.getParas());
    }

    public Kv getTotalStatistics() {
        Kv kv = Kv.create();
        kv.set("deptQuantity", Db.findFirst("select count(id) as quantity from base_dept").getInt("quantity"));
        kv.set("userQuantity", Db.findFirst("select count(id) as quantity from base_user").getInt("quantity"));
        kv.set("cabinetQuantity", Db.findFirst("select count(id) as quantity from base_cabinet").getInt("quantity"));
        kv.set("manufacturerQuantity", Db.findFirst("select count(id) as quantity from base_manufacturer").getInt("quantity"));
        kv.set("supplierQuantity", Db.findFirst("select count(id) as quantity from base_supplier").getInt("quantity"));
        kv.set("goodsQuantity", Db.findFirst("select count(id) as quantity from base_goods").getInt("quantity"));
        return kv;
    }

    public Page<Record> getRecordOperateByPage(int pageNumber, int pageSize, Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_EQUAL, "state")
                .put(SQL_PATTERN_LIKE, "module")
                .put(SQL_PATTERN_BETWEEN, "dateRange", "createDate")
                .order(SQL_SORT_DESC, "createDate")
                .build();

        String select = "select id,INET_NTOA(ip) as ip,module,url,method,parameter,errorMessage,createDate,executeTime,createUserId,state";
        String sqlExceptSelect = "from sys_log_operate";
        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect + condition.getSql(), condition.getParas());
    }

    public Page<Record> getRecordWsdlByPage(int pageNumber, int pageSize, Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_EQUAL, "state")
                .put(SQL_PATTERN_LIKE, "method")
                .put(SQL_PATTERN_BETWEEN, "dateRange", "createDate")
                .order(SQL_SORT_DESC, "createDate")
                .build();

        String select = "select *";
        String sqlExceptSelect = "from sys_log_spd";
        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect + condition.getSql(), condition.getParas());
    }

}