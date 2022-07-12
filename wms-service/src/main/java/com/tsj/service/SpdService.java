package com.tsj.service;

import cn.hutool.core.date.DateUtil;
import com.jfinal.aop.Inject;
import com.jfinal.kit.Kv;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.common.config.CommonConfig;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.constant.SpdUrl;
import com.tsj.common.constant.SysConstant;
import com.tsj.common.utils.DateUtils;
import com.tsj.common.utils.HttpKit;
import com.tsj.common.utils.IDGenerator;
import com.tsj.common.utils.R;
import com.tsj.domain.model.*;
import com.tsj.service.common.MyService;
import com.tsj.service.spdStockTag.SpdStockTagContainer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @className: SpdService
 * @description: SPD数据服务
 * @author: Frank
 * @create: 2020-03-23 14:12
 */
public class SpdService extends MyService {

    public static final Log logger = Log.getLog(SpdService.class);

    public static final String SPD_BASE_URL = CommonConfig.prop.get("SPD_BASE_URL");

    @Inject
    private CacheService cacheService;

    public R postStockTagList(String deptId) {
        if (!CommonConfig.prop.getBoolean("spd")) {
            return R.error(ResultCode.INTERFACE_FORBID_VISIT);
        }
        try {
            List<Tag> tagList = new ArrayList<>();
            Set<String> spdCodeSetFromSpd = SpdStockTagContainer.getByDept(deptId);
//            List<Record> recordList = HttpKit.postSpdData(SPD_BASE_URL + SpdUrl.URL_STOCK_TAG.getUrl(), null,
//                    Kv.by("DeptId", deptId));

            //查询该科室的标签唯一码集合
            Set<String> spdCodeSetFromComTag = Db.find("select spdCode from com_tag where deptId=?", deptId).stream().map(record -> record.getStr("spdCode")).collect(Collectors.toSet());

            spdCodeSetFromSpd.forEach(spdCode -> {
//                String CASE_NBR = record.getStr("CASE_NBR");
                if (!spdCodeSetFromComTag.contains(spdCode))
                    return;

                Tag tag = cacheService.getTagById(spdCode);
                if (tag != null) {
                    tagList.add(tag);
                }
            });

            logger.info("查询SPD库存记录总数：%d（条），可识别的标签记录数：%d（条） ", spdCodeSetFromSpd.size(), tagList.size());

            return R.ok().putData(tagList);
        } catch (Exception ex) {
            return R.error(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
    }

    public void postSpdTag(String type) {
        if (!CommonConfig.prop.getBoolean("spd")) {
            return;
        }

        boolean isTagIn = type.equals("tagIn") || type.equals("all");
        boolean isTagReturn = type.equals("tagReturn") || type.equals("all");
        boolean isTagOut = type.equals("tagOut") || type.equals("all");
        boolean isTagInventory = type.equals("tagInventory") || type.equals("all");

        //补发入柜记录（首次收货）
        if (isTagIn) {
            synchronized (this) {
                List<Record> recordList = Db.find("select cabinetId,createDate,createUserId from com_record_in where type = ? and upload = ? group by cabinetId,createDate", TagNo, UploadFailed);
                recordList.forEach(record -> {
                    String cabinetId = record.getStr("cabinetId");
                    String deptId = cacheService.getCabinetById(cabinetId).getDeptId();
                    String userId = record.getStr("createUserId");
                    String time = record.getStr("createDate");

                    List<RecordIn> recordInList = RecordIn.dao.find("select * from com_record_in where upload=? and type=? and cabinetId=? and createDate=? ", UploadFailed, TagNo, cabinetId, time);
                    List<String> spdCodeList = recordInList.stream().map(RecordIn::getSpdCode).collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(spdCodeList)) {
                        R result = HttpKit.postSpdTag(SPD_BASE_URL + SpdUrl.URL_IN_OUT.getUrl(), deptId, cabinetId,
                                cacheService.getCabinetById(cabinetId).getName(), spdCodeList, userId, time, "tagIn");
                        if (result.isSuccess()) {
                            recordInList.forEach(recordIn -> recordIn.setUpload(UploadSuccess));
                            Db.batchUpdate(recordInList, batchSize);
                        }
                    }
                });
            }
        }

        //补发入柜记录（取出放回）
        if (isTagReturn) {
            synchronized (this) {
                List<Record> recordList = Db.find("select cabinetId,createDate,createUserId as quantity from com_record_in where type = ? and upload = ? group by cabinetId,createDate", TagAc, UploadFailed);
                recordList.forEach(record -> {
                    String cabinetId = record.getStr("cabinetId");
                    String deptId = cacheService.getCabinetById(cabinetId).getDeptId();
                    String userId = record.getStr("createUserId");
                    String time = record.getStr("createDate");

                    List<RecordIn> recordInList = RecordIn.dao.find("select * from com_record_in where upload=? and type=?  and cabinetId=? and createDate=? ", UploadFailed, TagAc, cabinetId, time);
                    List<String> spdCodeList = recordInList.stream().map(RecordIn::getSpdCode).collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(spdCodeList)) {
                        R result = HttpKit.postSpdTag(SPD_BASE_URL + SpdUrl.URL_IN_OUT.getUrl(), deptId, cabinetId,
                                cacheService.getCabinetById(cabinetId).getName(), spdCodeList, userId, time, "tagReturn");
                        if (result.isSuccess()) {
                            recordInList.forEach(recordIn -> recordIn.setUpload(UploadSuccess));
                            Db.batchUpdate(recordInList, batchSize);
                        }
                    }
                });
            }
        }

        //补发出柜记录
        if (isTagOut) {
            synchronized (this) {
                List<Record> recordList = Db.find("select cabinetId,createDate,createUserId as quantity from com_record_out where upload=? group by cabinetId,createDate", UploadFailed);
                recordList.forEach(record -> {
                    String cabinetId = record.getStr("cabinetId");
                    String deptId = cacheService.getCabinetById(cabinetId).getDeptId();
                    String userId = record.getStr("createUserId");
                    String time = record.getStr("createDate");

                    List<RecordOut> recordOutList = RecordOut.dao.find("select * from com_record_out where upload=?  and cabinetId=? and createDate=? ", UploadFailed, cabinetId, time);
                    List<String> spdCodeList = recordOutList.stream().map(RecordOut::getSpdCode).collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(spdCodeList)) {
                        R result = HttpKit.postSpdTag(SPD_BASE_URL + SpdUrl.URL_IN_OUT.getUrl(), deptId, cabinetId,
                                cacheService.getCabinetById(cabinetId).getName(), spdCodeList, userId, time, "tagOut");
                        if (result.isSuccess()) {
                            recordOutList.forEach(recordIn -> recordIn.setUpload(UploadSuccess));
                            Db.batchUpdate(recordOutList, batchSize);
                        }
                    }
                });
            }
        }

        //补发盘点
        if (isTagInventory) {
            synchronized (this) {
                List<Record> recordList = Db.find("select cabinetId,createDate,createUserId as quantity from com_record_inventory where upload=? and ( state = ? or state = ? ) group by cabinetId,createDate", UploadFailed, InventoryNormal, InventoryMore);
                recordList.forEach(record -> {
                    String cabinetId = record.getStr("cabinetId");
                    String deptId = cacheService.getCabinetById(cabinetId).getDeptId();
                    String userId = record.getStr("createUserId");
                    String time = record.getStr("createDate");
                    String inventoryNo = RecordInventoryDifference.dao.findFirst("select * from com_record_inventory_difference where cabinetId=? and createDate=?",
                            record.getStr("cabinetId"), record.getStr("createDate")).getId();

                    List<RecordInventory> recordInventoryList = RecordInventory.dao.find("select * from com_record_inventory where upload = ? and ( state = ? or state = ? )  and cabinetId=? and createDate=? ", UploadFailed, InventoryNormal, InventoryMore, cabinetId, time);
                    List<String> spdCodeList = recordInventoryList.stream().map(RecordInventory::getSpdCode).collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(spdCodeList)) {
                        R result = HttpKit.postSpdTagInventory(SPD_BASE_URL + SpdUrl.URL_Inventory.getUrl(), deptId, cabinetId,
                                cacheService.getCabinetById(cabinetId).getName(), spdCodeList, userId, time, inventoryNo, "tagInventory");
                        if (result.isSuccess()) {
                            recordInventoryList.forEach(recordIn -> recordIn.setUpload(UploadSuccess));
                            Db.batchUpdate(recordInventoryList, batchSize);
                        }
                    }
                });
            }
        }

        logger.debug("postSpdTag");
    }

    /**
     * SPD数据同步标识，防止重复操作
     */
    private static volatile boolean isRunning = false;

    public boolean postBasicData(String type) {
        if (isRunning) {
            return false;
        }
        isRunning = true;
        try {
            doPostBasicData(type);
        } catch (Exception ignored) {
            logger.error("错误", ignored);
        }
        isRunning = false;
        return true;
    }

    public void doPostBasicData(String type) {
        if (!CommonConfig.prop.getBoolean("spd")) {
            return;
        }

        boolean isDept = type.equals("dept") || type.equals("all");
        boolean isUser = type.equals("user") || type.equals("all");
        boolean isGoods = type.equals("goods") || type.equals("all");
        boolean isMaterial = type.equals("material") || type.equals("all");
        boolean isPrint = type.equals("print") || type.equals("all");
        logger.info("同步科室");
        //同步科室
        if (isDept) {
            List<Dept> saveList = new ArrayList<>();
            List<Dept> updateList = new ArrayList<>();
            List<Record> recordList = HttpKit.postSpdData(SPD_BASE_URL + SpdUrl.URL_DEPARTMENT.getUrl(), "MODIFYDATE",
                    Kv.create());
            logger.info("同步科室数量:" + recordList.size());

            recordList.forEach(record -> {
                String COM_DEPOT_ID = record.getStr("COM_DEPOT_ID");
                Dept dept = cacheService.getDeptById(COM_DEPOT_ID);
                if (dept == null) {
                    dept = new Dept()
                            .setId(COM_DEPOT_ID)
                            .setName(record.getStr("DEPOT_NAME"))
                            .setEnable(StateEnable)
                            .setCreateDate(record.getStr("MODIFYDATE"))
                            .setModifyDate(DateUtil.now());
                    saveList.add(dept);
                } else if (!dept.getCreateDate().equals(record.getStr("MODIFYDATE"))) {
                    dept.setName(record.getStr("DEPOT_NAME"))
                            .setCreateDate(record.getStr("MODIFYDATE"))
                            .setModifyDate(DateUtil.now());
                    updateList.add(dept);

                    cacheService.removeCache(CacheBase, DeptById, COM_DEPOT_ID);
                }
            });
            // 批量新增和修改信息
            if (!saveList.isEmpty()) {
                Db.batchSave(saveList, batchSize);
            }
            if (!updateList.isEmpty()) {
                Db.batchUpdate(updateList, batchSize);
            }
        }
        logger.info("同步人员");
        //同步人员
        if (isUser) {
            List<User> saveList = new ArrayList<>();
            List<User> updateList = new ArrayList<>();
            List<Record> recordList = HttpKit.postSpdData(SPD_BASE_URL + SpdUrl.URL_USER.getUrl(), "MODIFYDATE",
                    Kv.create());
            logger.info("同步人员数量:" + recordList.size());

            recordList.forEach(record -> {
                String COM_PARTY_ID = record.getStr("COM_PARTY_ID");
                User user = cacheService.getUserById(COM_PARTY_ID);
                if (user == null) {
                    user = new User()
                            .setId(COM_PARTY_ID)
                            .setName(record.getStr("PARTY_NAM"))
                            .setDeptId(record.getStr("COM_DEPOT_ID"))
                            .setEnable(StateEnable)
                            .setCreateDate(record.getStr("MODIFYDATE"))
                            .setModifyDate(DateUtil.now());
                    saveList.add(user);
                } else if (!user.getCreateDate().equals(record.getStr("MODIFYDATE"))) {
                    user.setName(record.getStr("PARTY_NAM"))
                            .setDeptId(record.getStr("COM_DEPOT_ID"))
                            .setCreateDate(record.getStr("MODIFYDATE"))
                            .setModifyDate(DateUtil.now());
                    updateList.add(user);

                    cacheService.removeCache(CacheBase, UserById, COM_PARTY_ID);
                }
            });
            // 批量新增和修改信息
            if (!saveList.isEmpty()) {
                Db.batchSave(saveList, batchSize);
            }
            if (!updateList.isEmpty()) {
                Db.batchUpdate(updateList, batchSize);
            }
        }
        logger.info("同步耗材");
        //同步耗材
        if (isGoods) {
            List<Goods> saveList = new ArrayList<>();
            List<Goods> updateList = new ArrayList<>();
            List<Record> recordList = HttpKit.postSpdData(SPD_BASE_URL + SpdUrl.URL_GOODS.getUrl(), "MODIFYDATE",
                    Kv.create());
            logger.info("同步耗材数量:" + recordList.size());

            recordList.forEach(record -> {

                //新增三个参数
                String GOODS_TYPE = record.getStr("GOODS_TYPE");
                String MANUFACTURER_NAME = record.getStr("MANUFACTURER_NAME");
                String SUPPLIER_NAME = record.getStr("SUPPLIER_NAME");

                //TODO 耗材规格=SPD耗材描述+SPD耗材型号
                String GOODS_DESC = record.getStr("GOODS_DESC");
                if (StringUtils.isNotEmpty(GOODS_TYPE)) {
                    GOODS_DESC += "(" + GOODS_TYPE + ")";
                }

                //自动新增生产商
                String manufacturerId = null;
                if (StringUtils.isNotEmpty(MANUFACTURER_NAME)) {
                    Manufacturer manufacturer = cacheService.getManufacturerByName(MANUFACTURER_NAME);
                    if (manufacturer == null) {
                        manufacturerId = IDGenerator.makeId();
                        new Manufacturer().setId(manufacturerId).setName(MANUFACTURER_NAME).save();
                    } else {
                        manufacturerId = manufacturer.getId();
                    }
                }

                //自动新增供应商
                String supplierId = null;
                if (StringUtils.isNotEmpty(SUPPLIER_NAME)) {
                    Supplier supplier = cacheService.getSupplierByName(SUPPLIER_NAME);
                    if (supplier == null) {
                        supplierId = IDGenerator.makeId();
                        new Supplier().setId(supplierId).setName(SUPPLIER_NAME).save();
                    } else {
                        supplierId = supplier.getId();
                    }
                }

                String COM_GOODS_ID = record.getStr("COM_GOODS_ID");
                Goods goods = cacheService.getGoodsById(COM_GOODS_ID);
                if (goods == null) {
                    goods = new Goods()
                            .setId(COM_GOODS_ID)
                            .setName(record.getStr("GOODS_NAME"))
                            .setCode(record.getStr("GOODS_OPCODE"))
                            .setSpec(GOODS_DESC)
                            .setManufacturerId(manufacturerId)
                            .setSupplierId(supplierId)
                            .setUnit(record.getStr("UNIT_NAME"))
                            .setPrice(record.getDouble("RETAIL_PRICE"))
                            .setEnable(StateEnable)
                            .setCreateDate(record.getStr("MODIFYDATE"))
                            .setModifyDate(DateUtil.now());
                    saveList.add(goods);
                } else if (!goods.getCreateDate().equals(record.getStr("MODIFYDATE"))) {
                    goods.setName(record.getStr("GOODS_NAME"))
                            .setCode(record.getStr("GOODS_OPCODE"))
                            .setSpec(GOODS_DESC)
                            .setManufacturerId(manufacturerId)
                            .setSupplierId(supplierId)
                            .setUnit(record.getStr("UNIT_NAME"))
                            .setPrice(record.getDouble("RETAIL_PRICE"))
                            .setCreateDate(record.getStr("MODIFYDATE"))
                            .setModifyDate(DateUtil.now());

                    updateList.add(goods);

                    cacheService.removeCache(CacheBase, GoodsById, COM_GOODS_ID);
                }
            });
            // 批量新增和修改信息
            if (!saveList.isEmpty()) {
                Db.batchSave(saveList, batchSize);
            }
            if (!updateList.isEmpty()) {
                Db.batchUpdate(updateList, batchSize);
            }
        }

        logger.info("同步打印");
        //同步打印
        if (isPrint) {

            int lastDays = CommonConfig.prop.getInt("spd.lastDays", 7);
            String QueryEndDate = DateUtils.getCurrentTime();
            String QueryBeginDate = DateUtils.addDay(QueryEndDate, -1 * lastDays);
            List<Print> saveList = new ArrayList<>();
            List<Record> recordList = HttpKit.postSpdData(SPD_BASE_URL + SpdUrl.URL_PRINT.getUrl(), null,
                    Kv.by("QueryBeginDate", QueryBeginDate).set("QueryEndDate", QueryEndDate));
            logger.info("同步打印数量:" + recordList.size());
            recordList.forEach(record -> {
                //新增三个参数
                String SPD_CODE = record.getStr("CASE_NBR");
                String ORDER_CODE = record.getStr("ORDER_CODE");
                String COM_GOODS_ID = record.getStr("COM_GOODS_ID");
                String LOT_NO = record.getStr("LOT_NO");
                String EXPIRE_DATE = record.getStr("EXPIRE_DATE");
                String DEPOT_ID = record.getStr("DEPOT_ID");
                String SHELF_NAME = record.getStr("SHELF_NAME");
                String SHELF_CODE = record.getStr("SHELF_CODE");
                String ISHV = record.getStr("ISHV");
                String CREATEDATE = record.getStr("CREATEDATE");
                String INSNO = record.getStr("INSNO");
                Print printCache = cacheService.getPrintById(SPD_CODE);
                if (printCache == null) {
                    Print print = new Print().setSpdCode(SPD_CODE)
//                            .setEpc("474B4854" + ("是".equals(ISHV) ? "0" : "1") + "0" + CASE_NBR)
                            .setEpc("474B485400" + SPD_CODE)
                            .setOrderCode(ORDER_CODE)
                            .setComGoodsId(COM_GOODS_ID)
                            .setLotNo(LOT_NO)
                            .setExpireDate(EXPIRE_DATE)
                            .setDepotId(DEPOT_ID)
                            .setShelfName(SHELF_NAME)
                            .setShelfCode(SHELF_CODE)
                            .setHvFlag(ISHV)
                            .setCreateDate(CREATEDATE)
                            .setPrintFlag("0")
                            .setInsNo(INSNO);
                    saveList.add(print);
                }
            });
            // 批量新增
            if (!saveList.isEmpty()) {
                Db.batchSave(saveList, batchSize);
            }
        }

        logger.info("同步制标");

        //同步制标{退中心库的耗材唯一码会被重新使用，本地需要判断后覆盖}
//        String deptNames = CommonConfig.prop.get("spd.deptName");
        if (isMaterial) {
            int lastDays = CommonConfig.prop.getInt("spd.lastDays", 7);
            String QueryEndDate = DateUtils.getCurrentTime();
            String QueryBeginDate = DateUtils.addDay(QueryEndDate, -1 * lastDays);

//            for (String deptName : deptNames.split(",")) {
//                Dept dept = cacheService.getDeptByName(deptName);
//                if (dept == null) {
//                    continue;
//                }
//                logger.info("同步科室：" + deptName);

            List<Material> saveList = new ArrayList<>();
            List<Material> updateList = new ArrayList<>();
            List<Record> recordList = HttpKit.postSpdData(SPD_BASE_URL + SpdUrl.URL_TAG.getUrl(), "CREATEDATE",
//                        Kv.by("QueryBeginDate", QueryBeginDate).set("QueryEndDate", QueryEndDate).set("DeptId", dept.getId()));
                    Kv.by("QueryBeginDate", QueryBeginDate).set("QueryEndDate", QueryEndDate));
            logger.info("同步制标数量:" + recordList.size());

            recordList.forEach(record -> {
                String CASE_NBR = record.getStr("CASE_NBR");
                Material material = cacheService.getMaterialById(CASE_NBR);
                if (material == null) {
                    material = new Material()
                            .setSpdCode(CASE_NBR)
                            .setDeptId(record.getStr("DEPOT_ID"))
                            .setOrderCode(record.getStr("ORDER_CODE"))
                            .setGoodsId(record.getStr("COM_GOODS_ID"))
                            .setBatchNo(record.getStr("LOT_NO"))
                            .setExpireDate(record.getStr("EXPIRE_DATE").replace("T", " "))
                            .setCreateDate(record.getStr("CREATEDATE"));
                    saveList.add(material);
                } else if (record.getStr("CREATEDATE").compareTo(material.getCreateDate()) > 0) {
                    material.setDeptId(record.getStr("DEPOT_ID"))
                            .setOrderCode(record.getStr("ORDER_CODE"))
                            .setGoodsId(record.getStr("COM_GOODS_ID"))
                            .setBatchNo(record.getStr("LOT_NO"))
                            .setExpireDate(record.getStr("EXPIRE_DATE").replace("T", " "))
                            .setCreateDate(record.getStr("CREATEDATE"));
                    updateList.add(material);
                    cacheService.removeCache(CacheBase, MaterialById, CASE_NBR);
                }
            });
            // 批量新增和修改信息
            if (!saveList.isEmpty()) {
                Db.batchSave(saveList, batchSize);
            }
            if (!updateList.isEmpty()) {
                Db.batchUpdate(updateList, batchSize);
            }

            List<Material> materialList = new ArrayList<>();
            materialList.addAll(saveList);
            materialList.addAll(updateList);


            //TODO 自动创建配送单
            materialList.forEach(material -> {
                Order order = cacheService.getOrderByCode(material.getOrderCode());
                if (order == null) {
                    Order newOrder = new Order();
                    newOrder.setId(IDGenerator.makeId());
                    newOrder.setDeptId(material.getDeptId());
                    newOrder.setCode(material.getOrderCode());
                    newOrder.setCreateDate(DateUtil.now());
                    //初始状态为待复核
                    newOrder.setState(SysConstant.StateNeedConfirm);
                    newOrder.save();
                }
            });


            //TODO 自动创建标签记录
            if (true) {
                List<Tag> tagSaveList = new ArrayList<>();
                List<Tag> tagUpdateList = new ArrayList<>();
                materialList.forEach(material -> {
                    Tag tag = cacheService.getTagById(material.getSpdCode());
                    if (tag == null) {
                        tag = new Tag()
                                .setSpdCode(material.getSpdCode())
                                .setEpc(null)
                                .setAccept(TagNo)

                                .setGoodsId(material.getGoodsId())
                                .setDeptId(material.getDeptId())
                                .setOrderCode(material.getOrderCode())
                                .setBatchNo(material.getBatchNo())
                                .setExpireDate(material.getExpireDate())
                                .setCreateUserId("1")
                                .setCreateDate(DateUtils.getCurrentTime());
                        tagSaveList.add(tag);
                    } else {
                        tag
                                .setGoodsId(material.getGoodsId())
                                .setDeptId(material.getDeptId())
                                .setOrderCode(material.getOrderCode())
                                .setBatchNo(material.getBatchNo())
                                .setExpireDate(material.getExpireDate())
                                .setCreateUserId("1")
                                .setCreateDate(DateUtils.getCurrentTime());
                        tagUpdateList.add(tag);
                    }

                    //删除缓存信息
                    cacheService.removeCache(CacheCom, TagById, material.getSpdCode());
                });

                // 批量新增和修改信息
                if (!tagSaveList.isEmpty()) {
                    Db.batchSave(tagSaveList, batchSize);
                }
                if (!tagUpdateList.isEmpty()) {
                    Db.batchUpdate(tagUpdateList, batchSize);
                }
            }


//                //TODO 自动绑定标签
//                List<String> orderCodeList = new ArrayList<>();
//                ComService comService = Aop.get(ComService.class);
//                List<Material> materialList = new ArrayList<>();
//                materialList.addAll(saveList);
//                materialList.addAll(updateList);
//                materialList.forEach(material -> {
//
//                    //过滤相同的配送单号
//                    String orderCode = material.getOrderCode();
//                    if (orderCodeList.contains(orderCode)) {
//                        return;
//                    }
//                    orderCodeList.add(orderCode);
//
//                    //调用标签绑定
//                    List<Record> spdCodeList = Db.find("select spdCode from base_material where orderCode=?", orderCode);
//                    spdCodeList.forEach(item -> {
//                        String spdCode = item.get("spdCode");
//
//                        //TODO 表名替换为标签打印表
//                        logger.info("spdCode:"+spdCode);
//                        Record record = Db.findFirst("select epc,userId from print where caseNbr=?", spdCode);
//                        if(record!=null)
//                            comService.saveTagEpc(spdCode, record.getStr("epc"), record.getStr("userId"));
//                    });
//                });
//            }
        }

        logger.debug("postBasicData");
    }
}