package com.tsj.service;

import com.google.common.collect.Lists;
import com.jfinal.aop.Inject;
import com.jfinal.kit.Kv;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.common.constant.FileConstant;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.constant.SysConstant;
import com.tsj.common.utils.*;
import com.tsj.domain.model.*;
import com.tsj.service.common.MyService;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @className: FileService
 * @description: 文件导入导出数据服务
 * @author: Frank
 * @create: 2020-04-16 14:42
 */
public class FileService extends MyService {

    public static final Log logger = Log.getLog(FileService.class);

    @Inject
    private CacheService cacheService;

    @Inject
    private BaseService baseService;

    @Inject
    private SysService sysService;

    /******************************数据导入******************************/

    public R saveDeptList(String userId, String time, List<String[]> list) {
        List<Dept> deptList = Lists.newArrayList();

        list.forEach(array -> {
            int cellNum = 0;
            String name = array[cellNum++];
            String code = array[cellNum++];
            String pinyin = array[cellNum++];
            String caption = array[cellNum];

            //对象若存在，不重复添加
            if (cacheService.getDeptById(code) != null)
                return;

            Dept dept = new Dept()
                    .setId(code)
                    .setName(name)
                    .setCode(code)
                    .setPinyin(pinyin)
                    .setCaption(caption)
                    .setEnable(StateEnable)
                    .setCreateUserId(userId)
                    .setCreateDate(time)
                    .setModifyUserId(userId)
                    .setModifyDate(time);
            deptList.add(dept);
        });

        if (deptList.isEmpty()) {
            return R.error(ResultCode.DATA_IS_BLANK);
        }

        boolean succeed = Db.tx(() -> {
            Db.batchSave(deptList, batchSize);
            return true;
        });

        return succeed ? R.ok().putData(Kv.by("size", deptList.size())) : R.error(ResultCode.CREATE_FAIL);
    }

    public R saveUserList(String userId, String time, List<String[]> list) {
        List<User> userList = Lists.newArrayList();

        list.forEach(array -> {
            int cellNum = 0;
            String deptName = array[cellNum++];
            String name = array[cellNum++];
            String code = array[cellNum++];
            String username = array[cellNum];

            //对象若存在，不重复添加
            if (cacheService.getUserById(code) != null)
                return;

            User user = new User()
                    .setId(code)
                    .setName(name)
                    .setCode(code)
                    .setUsername(username)
                    .setEnable(StateEnable)
                    .setCreateUserId(userId)
                    .setCreateDate(time)
                    .setModifyUserId(userId)
                    .setModifyDate(time);

            //判断科室是否存在
            Dept dept = cacheService.getDeptByName(deptName);
            if (dept != null) {
                user.setDeptId(dept.getId());
            }

            userList.add(user);
        });

        if (userList.isEmpty()) {
            return R.error(ResultCode.DATA_IS_BLANK);
        }

        boolean succeed = Db.tx(() -> {
            Db.batchSave(userList, batchSize);
            return true;
        });

        return succeed ? R.ok().putData(Kv.by("size", userList.size())) : R.error(ResultCode.CREATE_FAIL);
    }

    public R saveCabinetList(String userId, String time, List<String[]> list) {
        List<Cabinet> cabinetList = Lists.newArrayList();

        list.forEach(array -> {
            //解析数组内容到对象属性
            int cellNum = 0;
            String deptName = array[cellNum++];
            String name = array[cellNum++];
            String code = array[cellNum++];
            String ip = array[cellNum++];
            String type = array[cellNum];

            //对象若存在，不重复添加
            if (cacheService.getUserById(code) != null)
                return;

            Cabinet cabinet = new Cabinet()
                    .setId(code)
                    .setName(name)
                    .setCode(code)
                    .setIp(ip)
                    .setType(type)
                    .setEnable(StateEnable)
                    .setCreateUserId(userId)
                    .setCreateDate(time)
                    .setModifyUserId(userId)
                    .setModifyDate(time);

            //判断科室是否存在
            Dept dept = cacheService.getDeptByName(deptName);
            if (dept != null) {
                cabinet.setDeptId(dept.getId());
            }

            cabinetList.add(cabinet);
        });

        if (cabinetList.isEmpty()) {
            return R.error(ResultCode.DATA_IS_BLANK);
        }

        boolean succeed = Db.tx(() -> {
            Db.batchSave(cabinetList, batchSize);
            return true;
        });

        return succeed ? R.ok().putData(Kv.by("size", cabinetList.size())) : R.error(ResultCode.CREATE_FAIL);
    }

    public R saveManufacturerList(String userId, String time, List<String[]> list) {
        List<Manufacturer> manufacturerList = new ArrayList<>();

        list.forEach(array -> {
            //解析数组内容到对象属性
            int cellNum = 0;
            String name = array[cellNum++];
            String code = array[cellNum++];
            String pinyin = array[cellNum++];
            String linkman = array[cellNum++];
            String mobile = array[cellNum++];
            String location = array[cellNum];

            //对象若存在，不重复添加
            if (cacheService.getManufacturerById(code) != null)
                return;

            Manufacturer manufacturer = new Manufacturer()
                    .setId(code)
                    .setName(name)
                    .setCode(code)
                    .setPinyin(pinyin)
                    .setLinkman(linkman)
                    .setMobile(mobile)
                    .setLocation(location)
                    .setEnable(StateEnable)
                    .setCreateUserId(userId)
                    .setCreateDate(time)
                    .setModifyUserId(userId)
                    .setModifyDate(time);
            manufacturerList.add(manufacturer);
        });

        if (manufacturerList.isEmpty()) {
            return R.error(ResultCode.DATA_IS_BLANK);
        }

        boolean succeed = Db.tx(() -> {
            Db.batchSave(manufacturerList, batchSize);
            return true;
        });

        return succeed ? R.ok().putData(Kv.by("size", manufacturerList.size())) : R.error(ResultCode.CREATE_FAIL);
    }

    public R saveSupplierList(String userId, String time, List<String[]> list) {
        List<Supplier> supplierList = new ArrayList<>();

        list.forEach(array -> {
            //解析数组内容到对象属性
            int cellNum = 0;
            String name = array[cellNum++];
            String code = array[cellNum++];
            String pinyin = array[cellNum++];
            String linkman = array[cellNum++];
            String mobile = array[cellNum++];
            String location = array[cellNum];

            //对象若存在，不重复添加
            if (cacheService.getSupplierById(code) != null)
                return;

            Supplier supplier = new Supplier()
                    .setId(code)
                    .setName(name)
                    .setCode(code)
                    .setPinyin(pinyin)
                    .setLinkman(linkman)
                    .setMobile(mobile)
                    .setLocation(location)
                    .setEnable(StateEnable)
                    .setCreateUserId(userId)
                    .setCreateDate(time)
                    .setModifyUserId(userId)
                    .setModifyDate(time);
            supplierList.add(supplier);
        });

        if (supplierList.isEmpty()) {
            return R.error(ResultCode.DATA_IS_BLANK);
        }

        boolean succeed = Db.tx(() -> {
            Db.batchSave(supplierList, batchSize);
            return true;
        });

        return succeed ? R.ok().putData(Kv.by("size", supplierList.size())) : R.error(ResultCode.CREATE_FAIL);
    }

    public R saveGoodsList(String userId, String time, List<String[]> list) {
        List<Goods> goodsList = new ArrayList<>();

        list.forEach(array -> {
            //解析数组内容到对象属性
            int cellNum = 0;
            String name = array[cellNum++];
            String commonName = array[cellNum++];
            String tradeName = array[cellNum++];
            String code = array[cellNum++];
            String spec = array[cellNum++];
            double price = Double.parseDouble(array[cellNum++]);
            String unit = array[cellNum++];
            String manufacturerName = array[cellNum++];
            String supplierName = array[cellNum++];
            String implant = array[cellNum];

            //对象若存在，不重复添加
            if (cacheService.getGoodsById(code) != null)
                return;

            Goods goods = new Goods()
                    .setId(IDGenerator.makeId())
                    .setName(name)
                    .setCommonName(commonName)
                    .setTradeName(tradeName)
                    .setCode(code)
                    .setSpec(spec)
                    .setPrice(price)
                    .setUnit(unit)
                    .setImplant(implant)
                    .setEnable(StateEnable)
                    .setCreateUserId(userId)
                    .setCreateDate(time)
                    .setModifyUserId(userId)
                    .setModifyDate(time);

            Manufacturer manufacturer = cacheService.getManufacturerByName(manufacturerName);
            if (manufacturer != null) {
                goods.setManufacturerId(manufacturer.getId());
            }

            Supplier supplier = cacheService.getSupplierByName(supplierName);
            if (supplier != null) {
                goods.setSupplierId(supplier.getId());
            }

            goodsList.add(goods);
        });

        if (goodsList.isEmpty()) {
            return R.error(ResultCode.DATA_IS_BLANK);
        }

        boolean succeed = Db.tx(() -> {
            Db.batchSave(goodsList, batchSize);
            return true;
        });

        return succeed ? R.ok().putData(Kv.by("size", goodsList.size())) : R.error(ResultCode.CREATE_FAIL);
    }

    public R saveStockBaseList(String userId, String time, List<String[]> list) {
        List<StockBase> modifyStockBaseList = new ArrayList<>();
        List<StockBase> createStockBaseList = new ArrayList<>();

        //列表去重
        list = list.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(()
                -> new TreeSet<>(Comparator.comparing(member -> member[0] + "-" + member[1]))), ArrayList::new));

        list.forEach(array -> {
            //解析数组内容到对象属性
            int cellNum = 0;
            String deptName = array[cellNum++];
            String goodsCode = array[cellNum++];
            int max = Integer.parseInt(array[cellNum++]);
            int min = Integer.parseInt(array[cellNum]);

            Dept dept = cacheService.getDeptByName(deptName);
            Goods goods = cacheService.getGoodsById(goodsCode);

            if (dept != null && goods != null) {
                StockBase stockBase = cacheService.getStockBase(dept.getId(), goods.getId());
                if (stockBase != null) {
                    stockBase.setMax(max)
                            .setMin(min)
                            .setModifyUserId(userId)
                            .setModifyDate(time);
                    modifyStockBaseList.add(stockBase);
                } else {
                    stockBase = new StockBase()
                            .setId(IDGenerator.makeId())
                            .setGoodsId(goods.getId())
                            .setDeptId(dept.getId())
                            .setMax(max)
                            .setMin(min)
                            .setCreateUserId(userId)
                            .setCreateDate(time)
                            .setModifyUserId(userId)
                            .setModifyDate(time);
                    createStockBaseList.add(stockBase);
                }
            }
        });

        boolean succeed = Db.tx(() -> {
            Db.batchSave(createStockBaseList, batchSize);
            Db.batchUpdate(modifyStockBaseList, batchSize);
            return true;
        });

        R result = succeed ? R.ok().putData(Kv.by("size", createStockBaseList.size() + modifyStockBaseList.size())) : R.error(ResultCode.CREATE_FAIL);
        //删除缓存
        if (result.isSuccess()) {
            cacheService.removeCache(CacheCom, StockBaseByDeptIdAndGoodsId, null);
        }
        return result;
    }

    /******************************数据导出******************************/

    public File getTagListToFile(Kv cond) throws Exception {

        //查询制标记录
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_EQUAL, "accept")
                .put(SQL_PATTERN_EQUAL, "goodsId")
                .put(SQL_PATTERN_EQUAL, "deptId")
                .put(SQL_PATTERN_EQUAL, "spdCode")
                .put(SQL_PATTERN_LIKE, "epc")
                .put(SQL_PATTERN_BETWEEN, "dateRange", "createDate")
                .order(SQL_SORT_DESC, "createDate")
                .build();

        String select = "select * ";
        String sqlExceptSelect = " from com_tag ";
        List<Record> recordList = Db.find(select + sqlExceptSelect + condition.getSql(), condition.getParas());

        //转换导出格式
        List<Integer> countList = Lists.newArrayList();
        List<String[]> list = new ArrayList<>();
        recordList.forEach(record -> {
            Dept dept = cacheService.getDeptById(record.getStr("deptId"));
            Goods goods = cacheService.getGoodsById(record.getStr("goodsId"));
            if (dept == null || goods == null)
                return;

            Manufacturer manufacturer = cacheService.getManufacturerById(goods.getManufacturerId());

            String[] array = new String[11];
            int cells = 0;
            array[cells++] = record.getStr("orderCode");
            array[cells++] = record.getStr("epc");
            array[cells++] = record.getStr("spdCode");
            array[cells++] = record.getStr("expireDate");
            array[cells++] = record.getStr("batchNo");
            array[cells++] = dept.getName();
            array[cells++] = goods.getName();
            array[cells++] = goods.getSpec();
            array[cells++] = goods.getUnit();
            array[cells++] = manufacturer == null ? "未知" : manufacturer.getName();
            array[cells] = record.getStr("createDate");
            list.add(array);

            countList.add(1);
        });

        int count = countList.stream().mapToInt(item -> item).sum();
        String filePath = ExcelKit.putObjectListToExcel(FileConstant.TEMPLATE_PATH + SysConstant.TEMPLATE_TAG, 2, 11, list, count);
        return new File(filePath);
    }

    public File getTagInOutToFile(Kv cond) throws Exception {
        Dept dept = cacheService.getDeptById(cond.getStr("deptId"));
        if (dept == null)
            throw new RuntimeException("未找到任何有效数据");

        List<String> cabinetIdList = baseService.getCabinetList(Kv.by("deptId", dept.getId()))
                .stream().map(e -> e.getStr("id")).collect(Collectors.toList());
        cond.set("cabinetIds", String.join(",", cabinetIdList));

        //科室下没有柜体，返回空集合
        if (cabinetIdList.isEmpty()) {
            throw new RuntimeException("未找到任何有效数据");
        }

        //查询出入柜记录
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_BETWEEN, "dateRange", "rd.createDate")
                .put(SQL_PATTERN_EQUAL, "type", "rd.type")
                .put(SQL_PATTERN_IN, "cabinetIds", "rd.cabinetId")
                .order(SQL_SORT_DESC, "rd.createDate")
                .build();

        String select = "SELECT rd.* FROM com_record_inout_exception rd";
        List<Record> recordList = Db.find(select + condition.getSql(), condition.getParas());

        //转换导出格式
        List<Integer> countList = Lists.newArrayList();
        List<String[]> list = new ArrayList<>();
        recordList.forEach(record -> {
            String createDate = record.getStr("createDate");
            User user = cacheService.getUserById(record.getStr("createUserId"));
            Cabinet cabinet = cacheService.getCabinetById(record.getStr("cabinetId"));
            if (cabinet == null)
                return;

            List<Record> records = Db.find("select goodsId, COUNT(*) quantity from "
                    + (cond.getStr("type").equals("in") ? "com_record_in" : "com_record_out")
                    + " where exceptionDescId=? group by goodsId", record.getStr("id"));
            records.forEach(record1 -> {
                Goods goods = cacheService.getGoodsById(record1.getStr("goodsId"));
                if (goods == null)
                    return;

                Manufacturer manufacturer = cacheService.getManufacturerById(goods.getManufacturerId());

                String[] array = new String[10];
                int cells = 0;
                array[cells++] = cond.getStr("type").equals("in") ? "入柜" : "出柜";
                array[cells++] = dept.getName();
                array[cells++] = cabinet.getName();
                array[cells++] = goods.getName();
                array[cells++] = goods.getSpec();
                array[cells++] = goods.getUnit();
                array[cells++] = manufacturer == null ? "未知" : manufacturer.getName();
                array[cells++] = record1.getStr("quantity");
                array[cells++] = user == null ? "未知" : user.getName();
                array[cells] = createDate;
                list.add(array);

                countList.add(Integer.valueOf(record1.getStr("quantity")));
            });
        });

        int count = countList.stream().mapToInt(item -> item).sum();
        String filePath = ExcelKit.putObjectListToExcel(FileConstant.TEMPLATE_PATH + SysConstant.TEMPLATE_TAG_INOUT, 2, 10, list, count);
        return new File(filePath);
    }

    public File getTagInOutRecordToFile(Kv cond, String type) throws Exception {
        Dept dept = cacheService.getDeptById(cond.getStr("deptId"));
        if (dept == null)
            throw new RuntimeException("未找到任何有效数据");

        List<String> cabinetIdList = baseService.getCabinetList(Kv.by("deptId", cond.getStr("deptId")))
                .stream().map(e -> e.getStr("id")).collect(Collectors.toList());
        cond.set("cabinetIds", String.join(",", cabinetIdList));

        //科室下没有柜体，返回空集合
        if (cabinetIdList.isEmpty()) {
            throw new RuntimeException("未找到任何有效数据");
        }

        QueryCondition condition = QueryConditionBuilder.by(cond, false)
                .put(SQL_PATTERN_EQUAL, "goodsId", "tag.goodsId")
                .put(SQL_PATTERN_BETWEEN, "dateRange", "rd.createDate")
                .put(SQL_PATTERN_LIKE, "spdCode", "rd.spdCode")
                .put(SQL_PATTERN_IN, "cabinetIds", "rd.cabinetId")
                .order(SQL_SORT_DESC, "rd.createDate")
                .build();

        String select = null;
        String sqlExceptSelect = null;
        switch (type) {
            case "tagIn":
                select = "SELECT rd.*,tag.batchNo,tag.expireDate,'tagIn' as recordType";
                sqlExceptSelect = "FROM com_record_in rd LEFT JOIN com_tag tag ON rd.spdCode = tag.spdCode " +
                        "WHERE rd.type='no'";
                break;

            case "tagReturn":
                select = "SELECT rd.*,tag.batchNo,tag.expireDate,'tagReturn' as recordType";
                sqlExceptSelect = "FROM com_record_in rd LEFT JOIN com_tag tag ON rd.spdCode = tag.spdCode " +
                        "WHERE rd.type='ac'";
                break;

            case "tagOut":
                select = "SELECT rd.*,tag.batchNo,tag.expireDate,'tagOut' as recordType";
                sqlExceptSelect = "FROM com_record_out rd LEFT JOIN com_tag tag ON rd.spdCode = tag.spdCode " +
                        "WHERE 1=1 ";
                break;

        }
        List<Record> recordList = Db.find(select + " " + sqlExceptSelect + condition.getSql(), condition.getParas());

        //转换导出格式
        List<Integer> countList = Lists.newArrayList();
        List<String[]> list = new ArrayList<>();
        recordList.forEach(record -> {
            String createDate = record.getStr("createDate");
            User user = cacheService.getUserById(record.getStr("createUserId"));
            Cabinet cabinet = cacheService.getCabinetById(record.getStr("cabinetId"));
            Goods goods = cacheService.getGoodsById(record.getStr("goodsId"));

            if (cabinet == null || goods == null)
                return;

            Manufacturer manufacturer = cacheService.getManufacturerById(goods.getManufacturerId());

            String[] array = new String[14];
            int cells = 0;
            array[cells++] = type.equals("tagIn") || type.equals("tagReturn") ? "入柜" : "出柜";
            array[cells++] = dept.getName();
            array[cells++] = cabinet.getName();
            array[cells++] = goods.getName();
            array[cells++] = goods.getSpec();
            array[cells++] = goods.getUnit();
            array[cells++] = manufacturer == null ? "未知" : manufacturer.getName();
            array[cells++] = goods.getCode();
            array[cells++] = record.getStr("spdCode");
            array[cells++] = record.getStr("expireDate");
            array[cells++] = record.getStr("batchNo");
            array[cells++] = record.getStr("confirmed").equals("true") ? "手动" : "自动";
            array[cells++] = user == null ? "未知" : user.getName();
            array[cells] = createDate;
            list.add(array);

            countList.add(1);
        });

        int count = countList.stream().mapToInt(item -> item).sum();
        String filePath = ExcelKit.putObjectListToExcel(FileConstant.TEMPLATE_PATH + SysConstant.TEMPLATE_TAG_INOUT_RECORD, 2, 14, list, count);
        return new File(filePath);
    }

    public File getTagStockToFile(Kv cond) throws Exception {
        String expireBeforeMonth = sysService.getConfig("cabinet", "expireBeforeMonth").getValue();
        String date = DateUtils.addMonth(DateUtils.getCurrentDate(), Integer.parseInt(expireBeforeMonth));

//        if (StringUtils.isNotEmpty(cond.getStr("cabinetName"))) {
//            List<Cabinet> ids = Cabinet.dao.find("select id from base_cabinet where name like concat('%', ?, '%') ", cond.getStr("cabinetName"));
//            cond.set("cabinetId", ids.stream().map(Cabinet::getId).collect(Collectors.joining(",")));
//        }

//        QueryCondition condition = QueryConditionBuilder.by(cond, true)
//                .put(SQL_PATTERN_EQUAL, "deptId")
//                .put(SQL_PATTERN_EQUAL, "goodsId")
//                .put(SQL_PATTERN_IN, "cabinetId")
//                .build();
        QueryCondition condition = QueryConditionBuilder.by(cond, true).put(SQL_PATTERN_EQUAL, "a.deptId").put(SQL_PATTERN_LIKE, "c.name").put(SQL_PATTERN_LIKE, "b.name").build();
        String select = "SELECT a.deptId, a.cabinetId, a.goodsId, COUNT(*) totalQuantity, COUNT( CASE WHEN a.expireDate < '" + date + "' THEN 1 END ) expireQuantity FROM com_stock_tag a LEFT JOIN base_cabinet b on a.cabinetId=b.id LEFT JOIN base_goods c on a.goodsId=c.id";
        List<Record> recordList = Db.find(select + condition.getSql() + " GROUP BY a.cabinetId, a.goodsId ", condition.getParas());

        List<Integer> countList = Lists.newArrayList();
        List<String[]> list = new ArrayList<>();
        recordList.forEach(record -> {
            Dept dept = cacheService.getDeptById(record.getStr("deptId"));
            Cabinet cabinet = cacheService.getCabinetById(record.getStr("cabinetId"));
            Goods goods = cacheService.getGoodsById(record.getStr("goodsId"));
            if (dept == null || goods == null)
                return;

            Manufacturer manufacturer = cacheService.getManufacturerById(goods.getManufacturerId());

            String[] array = new String[8];
            int cells = 0;
            array[cells++] = dept.getName();
            array[cells++] = cabinet.getName();
            array[cells++] = goods.getName();
            array[cells++] = goods.getSpec();
            array[cells++] = goods.getUnit();
            array[cells++] = manufacturer == null ? "未知" : manufacturer.getName();
            array[cells++] = record.getStr("totalQuantity");
            array[cells] = record.getStr("expireQuantity");
            list.add(array);

            countList.add(Integer.valueOf(record.getStr("totalQuantity")));
        });

        int count = countList.stream().mapToInt(item -> item).sum();
        String filePath = ExcelKit.putObjectListToExcel(FileConstant.TEMPLATE_PATH + SysConstant.TEMPLATE_TAG_STOCK, 2, 8, list, count);
        return new File(filePath);
    }

    public File getRecordInventoryNewFile(String createDate) throws Exception {
        String select = "SELECT\n" +
                "\tc.`name` AS deptName,\n" +
                "\td.`name` AS cabinetName,\n" +
                "\te.`name` AS goodsName,\n" +
                "\te.spec,\n" +
                "\te.unit,\n" +
                "\tf.`name` AS manufacturerName,\n" +
                "\ta.epc,\n" +
                "\tb.spdCode,\n" +
                "\tb.expireDate,\n" +
                "\tb.batchNo,\n" +
                "\ta.createDate\n" +
                "FROM\n" +
                "\tcom_record_inventory_new a\n" +
                "LEFT JOIN com_tag b ON a.epc = b.epc\n" +
                "LEFT JOIN base_dept c ON b.deptId = c.id\n" +
                "LEFT JOIN base_cabinet d ON a.cabinetId = d.id\n" +
                "LEFT JOIN base_goods e ON b.goodsId = e.id\n" +
                "LEFT JOIN base_manufacturer f ON e.manufacturerId = f.id\n" +
                "where a.createDate='" + createDate + "' and b.epc is not null order by b.goodsId";
        List<Record> recordList = Db.find(select);

        List<String[]> list = new ArrayList<>();
        recordList.forEach(record -> {
            String[] array = new String[11];
            int cells = 0;
            array[cells++] = record.getStr("deptName");
            array[cells++] = record.getStr("cabinetName");
            array[cells++] = record.getStr("goodsName");
            array[cells++] = record.getStr("spec");
            array[cells++] = record.getStr("unit");
            array[cells++] = record.getStr("manufacturerName");
            array[cells++] = record.getStr("spdCode");
            array[cells++] = record.getStr("epc");
            array[cells++] = record.getStr("expireDate");
            array[cells++] = record.getStr("batchNo");
            array[cells++] = record.getStr("createDate");
            list.add(array);
        });
        String filePath = ExcelKit.putObjectListToExcel(FileConstant.TEMPLATE_PATH + SysConstant.TEMPLATE_RECORD_INVENTORY_NEW, 2, 11, list, list.size());
        return new File(filePath);
    }

    public File getTagStockRecordToFile(Kv cond) throws Exception {
        String expireBeforeMonth = sysService.getConfig("cabinet", "expireBeforeMonth").getValue();
        String date = DateUtils.addMonth(DateUtils.getCurrentDate(), Integer.parseInt(expireBeforeMonth));

        if ("expireDate".equals(cond.getStr("type"))) {
            cond.set("state2", "1");
        }

        if (StringUtils.isNotEmpty(cond.getStr("cabinetName"))) {
            List<Cabinet> ids = Cabinet.dao.find("select id from base_cabinet where deptId=? and name like concat('%', ?, '%') ", cond.getStr("deptId"), cond.getStr("cabinetName"));
            cond.set("cabinetId", ids.stream().map(Cabinet::getId).collect(Collectors.joining(",")));
        }

        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_EQUAL, "deptId", "tag.deptId")
                .put(SQL_PATTERN_EQUAL, "goodsId", "tag.goodsId")
                .put(SQL_PATTERN_EQUAL, "state2", "(tag.expireDate<'" + date + "')")
                .put(SQL_PATTERN_IN, "cabinetId", "tag.cabinetId")
                .put(SQL_PATTERN_LIKE, "spdCode", "tag.spdCode")
                .order(SQL_SORT_ASC, "tag.deptId,tag.goodsId")
                .build();

        String select = "select tt.epc,tag.*,tag.expireDate<'" + DateUtils.getCurrentDate() + "' state1,tag.expireDate<'" + date + "' state2";
        String sqlExceptSelect = " from com_stock_tag tag left join com_tag tt on tag.spdCode=tt.spdCode";

        List<Record> recordList = Db.find(select + sqlExceptSelect + condition.getSql(), condition.getParas());

        List<Integer> countList = Lists.newArrayList();
        List<String[]> list = new ArrayList<>();
        recordList.forEach(record -> {
            Dept dept = cacheService.getDeptById(record.getStr("deptId"));
            Cabinet cabinet = cacheService.getCabinetById(record.getStr("cabinetId"));
            Goods goods = cacheService.getGoodsById(record.getStr("goodsId"));
            if (dept == null || goods == null)
                return;

            Manufacturer manufacturer = cacheService.getManufacturerById(goods.getManufacturerId());

            String[] array = new String[10];
            int cells = 0;
            array[cells++] = dept.getName();
            array[cells++] = cabinet.getName();
            array[cells++] = goods.getName();
            array[cells++] = goods.getSpec();
            array[cells++] = goods.getUnit();
            array[cells++] = manufacturer == null ? "未知" : manufacturer.getName();
            array[cells++] = record.getStr("spdCode");
            array[cells++] = record.getStr("expireDate");
            array[cells++] = record.getStr("batchNo");
            array[cells] = record.getStr("createDate");
            list.add(array);

            countList.add(1);
        });

        int count = countList.stream().mapToInt(item -> item).sum();
        String filePath = ExcelKit.putObjectListToExcel(FileConstant.TEMPLATE_PATH + SysConstant.TEMPLATE_TAG_STOCK_RECORD, 2, 10, list, count);
        return new File(filePath);
    }

    public File downloadInventory(String inventoryDifferenceId) throws Exception {
        String sql = "SELECT goodsId, count(state) total, sum(state = 'normal') normal, sum(state = 'more') more, sum(state = 'less') less, b.NAME goodsName, b.spec, b.unit, c.NAME manufacturerName FROM com_record_inventory a LEFT JOIN base_goods b ON a.goodsId = b.id LEFT JOIN base_manufacturer c ON b.manufacturerId = c.id WHERE inventoryDifferenceId = ? GROUP BY goodsId";
        List<Record> recordList = Db.find(sql, inventoryDifferenceId);
        List<String[]> list = new ArrayList<>();
        AtomicInteger sum = new AtomicInteger();
        recordList.forEach(record -> {
            int total = record.getInt("total");
            int normal = record.getInt("normal");
            int more = record.getInt("more");
            int less = record.getInt("less");
            String goodsName = record.getStr("goodsName");
            String spec = record.getStr("spec");
            String unit = record.getStr("unit");
            String manufacturerName = record.getStr("manufacturerName");

            String[] array = new String[8];
            int cells = 0;
            array[cells++] = goodsName;
            array[cells++] = spec;
            array[cells++] = unit;
            array[cells++] = manufacturerName;
            array[cells++] = total + "";
            array[cells++] = normal + "";
            array[cells++] = more + "";
            array[cells++] = less + "";
            list.add(array);
            sum.addAndGet(total);
        });
        String filePath = ExcelKit.putObjectListToExcel(FileConstant.TEMPLATE_PATH + SysConstant.TEMPLATE_INVENTORY, 2, 8, list, sum.get());
        return new File(filePath);
    }
}