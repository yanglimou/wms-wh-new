package com.tsj.service;

import cn.hutool.core.date.DateUtil;
import com.jfinal.aop.Inject;
import com.jfinal.kit.Kv;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.api.vanx.kit.VanxKit;
import com.tsj.api.vanx.vo.detail.*;
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
import org.apache.commons.lang3.StringUtils;
import com.tsj.api.vanx.wsdl.SPDWebService;
import com.tsj.api.vanx.wsdl.SendRecv;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @className: SpdService
 * @description: SPD数据服务
 * @author: Frank
 * @create: 2020-03-23 14:12
 */
public class Spd2Service extends MyService {

    public static final Log logger = Log.getLog(Spd2Service.class);

    public static final String SPD_BASE_URL = CommonConfig.prop.get("SPD_BASE_URL");

    @Inject
    private CacheService cacheService;

    public R postStockTagList(String deptId) {
        if (!CommonConfig.prop.getBoolean("spd")) {
            return R.error(ResultCode.INTERFACE_FORBID_VISIT);
        }
        try {
            List<Tag> tagList = new ArrayList<>();
            List<Record> recordList = HttpKit.postSpdData(SPD_BASE_URL + SpdUrl.URL_STOCK_TAG.getUrl(), null,
                    Kv.by("DeptId", "10021004"));

            //查询该科室的标签唯一码集合
            Set<String> spdCodeSet = Db.find("select spdCode from com_tag where deptId=?", deptId).stream().map(record -> record.getStr("spdCode")).collect(Collectors.toSet());

            recordList.forEach(record -> {
                String CASE_NBR = record.getStr("CASE_NBR");
                if (!spdCodeSet.contains(CASE_NBR))
                    return;

                Tag tag = cacheService.getTagById(CASE_NBR);
                if (tag != null) {
                    tagList.add(tag);
                }
            });

            logger.info("查询SPD库存记录总数：%d（条），可识别的标签记录数：%d（条） ", recordList.size(), tagList.size());

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
                    List<RecordIn> list = RecordIn.dao.find("select * from com_record_in where upload=? and type=?", UploadFailed, TagNo);
                    if (list != null && list.size() > 0) {
                        List<IC101Detail> ic101DetailList = new ArrayList<>();
                        list.forEach(in -> {
                            Tag tag = cacheService.getTagById(in.getSpdCode());
                            Order order = cacheService.getOrderByCode(tag.getOrderCode());

                            IC101Detail ic101Detail = new IC101Detail();
                            ic101Detail.setINSOUTID(order.getId());
                            ic101Detail.setINSOUTNO(order.getCode());
                            ic101Detail.setTAGCODE(in.getSpdCode());
                            ic101Detail.setACTIONTYPEKEY("SHELFING");
                            ic101Detail.setBILLNO(null);
                            ic101Detail.setBILLID(null);
                            ic101Detail.setUPDUSERID(in.getCreateUserId());
                            ic101Detail.setUPDDATE(in.getCreateDate());
                            ic101DetailList.add(ic101Detail);
                        });
                        boolean result = VanxKit.postTagInOutList(ic101DetailList);
                        if (result) {
                            list.forEach(recordIn -> recordIn.setUpload(UploadSuccess));
                            Db.batchUpdate(list, batchSize);
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
                    List<RecordIn> list = RecordIn.dao.find("select * from com_record_in where upload=? and type=?", UploadFailed, TagAc);
                    if (list != null && list.size() > 0) {
                        List<IC101Detail> ic101DetailList = new ArrayList<>();
                        list.forEach(in -> {
                            Tag tag = cacheService.getTagById(in.getSpdCode());
                            Order order = cacheService.getOrderByCode(tag.getOrderCode());

                            IC101Detail ic101Detail = new IC101Detail();
                            ic101Detail.setINSOUTID(order.getId());
                            ic101Detail.setINSOUTNO(order.getCode());
                            ic101Detail.setTAGCODE(in.getSpdCode());
                            ic101Detail.setACTIONTYPEKEY("GIVING_BACK");
                            ic101Detail.setBILLNO(null);
                            ic101Detail.setBILLID(null);
                            ic101Detail.setUPDUSERID(in.getCreateUserId());
                            ic101Detail.setUPDDATE(in.getCreateDate());
                            ic101DetailList.add(ic101Detail);
                        });
                        boolean result = VanxKit.postTagInOutList(ic101DetailList);
                        if (result) {
                            list.forEach(recordIn -> recordIn.setUpload(UploadSuccess));
                            Db.batchUpdate(list, batchSize);
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
                    List<RecordOut> list = RecordOut.dao.find("select * from com_record_out where upload=?", UploadFailed);
                    if (list != null && list.size() > 0) {
                        List<IC101Detail> ic101DetailList = new ArrayList<>();
                        list.forEach(in -> {
                            Tag tag = cacheService.getTagById(in.getSpdCode());
                            Order order = cacheService.getOrderByCode(tag.getOrderCode());

                            IC101Detail ic101Detail = new IC101Detail();
                            ic101Detail.setINSOUTID(order.getId());
                            ic101Detail.setINSOUTNO(order.getCode());
                            ic101Detail.setTAGCODE(in.getSpdCode());
                            ic101Detail.setACTIONTYPEKEY("TAKING_OUT");
                            ic101Detail.setBILLNO(null);
                            ic101Detail.setBILLID(null);
                            ic101Detail.setUPDUSERID(in.getCreateUserId());
                            ic101Detail.setUPDDATE(in.getCreateDate());
                            ic101DetailList.add(ic101Detail);
                        });
                        boolean result = VanxKit.postTagInOutList(ic101DetailList);
                        if (result) {
                            list.forEach(recordIn -> recordIn.setUpload(UploadSuccess));
                            Db.batchUpdate(list, batchSize);
                        }
                    }
                });
            }
        }

        //补发盘点
        if (isTagInventory) {
            synchronized (this) {
                List<Record> recordList = Db.find("select cabinetId,createDate,createUserId as quantity from com_record_inventory where upload=? group by cabinetId,createDate", UploadFailed);
                recordList.forEach(record -> {
                    List<RecordInventory> list = RecordInventory.dao.find("select * from com_record_inventory where upload = ? and ( state = ? or state = ? )", UploadFailed, InventoryNormal, InventoryMore);
                    if (list != null && list.size() > 0) {
                        List<IC102Detail> ic101DetailList = new ArrayList<>();
                        list.forEach(in -> {
                            Tag tag = cacheService.getTagById(in.getSpdCode());
                            Order order = cacheService.getOrderByCode(tag.getOrderCode());

                            IC102Detail ic102Detail = new IC102Detail();
                            ic102Detail.setINSOUTNO(order.getCode());
                            ic102Detail.setTAGCODE(in.getSpdCode());
                            ic102Detail.setTAGCODESTATUS(in.getState());
                            ic102Detail.setBILLNO(null);
                            ic102Detail.setUPDUSERID(in.getCreateUserId());
                            ic102Detail.setUPDDATE(in.getCreateDate());
                            ic101DetailList.add(ic102Detail);
                        });
                        boolean result = VanxKit.postTagInventory(ic101DetailList);
                        if (result) {
                            list.forEach(recordIn -> recordIn.setUpload(UploadSuccess));
                            Db.batchUpdate(list, batchSize);
                        }
                    }
                });
            }
        }

        logger.debug("postSpdTag");
    }

    public void postBasicData(String type) {
        if (!CommonConfig.prop.getBoolean("spd")) {
            return;
        }

        boolean isDept = type.equals("dept") || type.equals("all");
        boolean isUser = type.equals("user") || type.equals("all");
        boolean isManufacture = type.equals("manufacture") || type.equals("all");
        boolean isSupply = type.equals("supply") || type.equals("all");
        boolean isArticle = type.equals("article") || type.equals("all");
        boolean isMaterial = type.equals("material") || type.equals("all");

        //同步科室
        if (isDept) {
            List<Dept> saveList = new ArrayList<>();
            List<Dept> updateList = new ArrayList<>();
            List<BD201Detail> recordList = VanxKit.getDeptList();
            logger.info("同步科室数量:" + recordList.size());

            recordList.forEach(record -> {
                String COM_DEPOT_ID = record.getDEPTID();
                Dept dept = cacheService.getDeptById(COM_DEPOT_ID);
                if (dept == null) {
                    dept = new Dept()
                            .setId(COM_DEPOT_ID)
                            .setCode(record.getDEPTCODE())
                            .setName(record.getDEPTNAME())
                            .setEnable("0".equals(record.getSTOPFLG()) ? StateEnable : StateUnable)
                            .setCreateDate(record.getUPDDATE())
                            .setModifyDate(DateUtil.now());
                    saveList.add(dept);
                } else if (!dept.getCreateDate().equals(record.getUPDDATE())) {
                    dept.setCode(record.getDEPTCODE())
                            .setName(record.getDEPTNAME())
                            .setEnable("0".equals(record.getSTOPFLG()) ? StateEnable : StateUnable)
                            .setCreateDate(record.getUPDDATE())
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

        //同步人员
        if (isUser) {
            List<User> saveList = new ArrayList<>();
            List<User> updateList = new ArrayList<>();
            List<BD202Detail> recordList = VanxKit.getUserList();
            logger.info("同步人员数量:" + recordList.size());

            recordList.forEach(record -> {
                Dept dept = cacheService.getDeptByName(record.getDEPTNAME());
                if (dept == null) {
                    return;
                }

                String COM_PARTY_ID = record.getUSERID();
                User user = cacheService.getUserById(COM_PARTY_ID);
                if (user == null) {
                    user = new User()
                            .setId(COM_PARTY_ID)
                            .setCode(record.getUSERCODE())
                            .setName(record.getUSERNAME())
                            .setDeptId(dept.getId())
                            .setEnable("0".equals(record.getSTOPFLG()) ? StateEnable : StateUnable)
                            .setCreateDate(record.getUPDDATE())
                            .setModifyDate(DateUtil.now());
                    saveList.add(user);
                } else if (!user.getCreateDate().equals(record.getUPDDATE())) {
                    user.setCode(record.getUSERCODE())
                            .setName(record.getUSERNAME())
                            .setDeptId(dept.getId())
                            .setEnable("0".equals(record.getSTOPFLG()) ? StateEnable : StateUnable)
                            .setCreateDate(record.getUPDDATE())
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

        //同步生产商
        if (isManufacture) {
            List<Manufacturer> saveList = new ArrayList<>();
            List<Manufacturer> updateList = new ArrayList<>();
            List<BD203Detail> recordList = VanxKit.getManufactureList();
            logger.info("同步生产商数量:" + recordList.size());

            recordList.forEach(record -> {
                String COM_MF_ID = record.getMFID();
                Manufacturer manufacturer = cacheService.getManufacturerById(COM_MF_ID);
                if (manufacturer == null) {
                    manufacturer = new Manufacturer()
                            .setId(COM_MF_ID)
                            .setCode(record.getMFCODE())
                            .setName(record.getMFNAME())
                            .setEnable("0".equals(record.getSTOPFLG()) ? StateEnable : StateUnable)
                            .setCreateDate(record.getUPDDATE())
                            .setModifyDate(DateUtil.now());
                    saveList.add(manufacturer);
                } else if (!manufacturer.getCreateDate().equals(record.getUPDDATE())) {
                    manufacturer.setCode(record.getMFCODE())
                            .setName(record.getMFNAME())
                            .setEnable("0".equals(record.getSTOPFLG()) ? StateEnable : StateUnable)
                            .setCreateDate(record.getUPDDATE())
                            .setModifyDate(DateUtil.now());
                    updateList.add(manufacturer);

                    cacheService.removeCache(CacheBase, ManufacturerById, COM_MF_ID);
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

        //同步供应商
        if (isSupply) {
            List<Supplier> saveList = new ArrayList<>();
            List<Supplier> updateList = new ArrayList<>();
            List<BD204Detail> recordList = VanxKit.getSupplyList();
            logger.info("同步供应商数量:" + recordList.size());

            recordList.forEach(record -> {
                String COM_SUPPLY_ID = record.getSUPPLYID();
                Supplier supplier = cacheService.getSupplierById(COM_SUPPLY_ID);
                if (supplier == null) {
                    supplier = new Supplier()
                            .setId(COM_SUPPLY_ID)
                            .setCode(record.getSUPPLYCODE())
                            .setName(record.getSUPPLYNAME())
                            .setEnable("0".equals(record.getSTOPFLG()) ? StateEnable : StateUnable)
                            .setCreateDate(record.getUPDDATE())
                            .setModifyDate(DateUtil.now());
                    saveList.add(supplier);
                } else if (!supplier.getCreateDate().equals(record.getUPDDATE())) {
                    supplier.setCode(record.getSUPPLYCODE())
                            .setName(record.getSUPPLYNAME())
                            .setEnable("0".equals(record.getSTOPFLG()) ? StateEnable : StateUnable)
                            .setCreateDate(record.getUPDDATE())
                            .setModifyDate(DateUtil.now());
                    updateList.add(supplier);

                    cacheService.removeCache(CacheBase, SupplierById, COM_SUPPLY_ID);
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

        //同步耗材
        if (isArticle) {
            List<Goods> saveList = new ArrayList<>();
            List<Goods> updateList = new ArrayList<>();
            List<BD205Detail> recordList = VanxKit.getArticleList();
            logger.info("同步耗材数量:" + recordList.size());

            recordList.forEach(record -> {

                //新增三个参数
                String GOODS_TYPE = record.getMATE_TYPE_NAME();
                Manufacturer manufacturer = cacheService.getManufacturerByName(record.getMFNAME());
                Supplier supplier = cacheService.getSupplierByName(record.getSUPPLYNAME());

                //TODO 耗材规格=SPD耗材描述+SPD耗材型号
                String GOODS_DESC = record.getSPEC();
                if (StringUtils.isNotEmpty(GOODS_TYPE)) {
                    GOODS_DESC += "(" + GOODS_TYPE + ")";
                }

                String COM_GOODS_ID = record.getARTIID();
                Goods goods = cacheService.getGoodsById(COM_GOODS_ID);
                if (goods == null) {
                    goods = new Goods()
                            .setId(COM_GOODS_ID)
                            .setName(record.getARTINAME())
                            .setCode(record.getARTICODE())
                            .setSpec(GOODS_DESC)
                            .setManufacturerId(manufacturer == null ? null : manufacturer.getId())
                            .setSupplierId(supplier == null ? null : supplier.getId())
                            .setUnit(record.getUNIT())
                            .setPrice(Double.parseDouble(record.getINSPRICE()))
                            .setEnable("0".equals(record.getSTOPFLG()) ? StateEnable : StateUnable)
                            .setCreateDate(record.getUPDDATE())
                            .setModifyDate(DateUtil.now());
                    saveList.add(goods);
                } else if (!goods.getCreateDate().equals(record.getUPDDATE())) {
                    goods.setName(record.getARTINAME())
                            .setCode(record.getARTICODE())
                            .setSpec(GOODS_DESC)
                            .setManufacturerId(manufacturer == null ? null : manufacturer.getId())
                            .setSupplierId(supplier == null ? null : supplier.getId())
                            .setUnit(record.getUNIT())
                            .setPrice(Double.parseDouble(record.getINSPRICE()))
                            .setEnable("0".equals(record.getSTOPFLG()) ? StateEnable : StateUnable)
                            .setCreateDate(record.getUPDDATE())
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

        //同步制标{退中心库的耗材唯一码会被重新使用，本地需要判断后覆盖}
        if (isMaterial) {
            int lastDays = CommonConfig.prop.getInt("spd.lastDays", 7);

            Map<String, String> orderMap = new HashMap<>();
            List<Material> saveList = new ArrayList<>();
            List<Material> updateList = new ArrayList<>();
            List<IC203Detail> recordList = VanxKit.getTagOutList(lastDays);
            logger.info("同步制标数量:" + recordList.size());

            recordList.forEach(record -> {
                String orderCode = record.getINSOUTNO();
                String orderId = record.getINSOUTID();
                String deptId = record.getINSDEPTID();
                if (!orderMap.containsKey(orderCode)) {
                    orderMap.put(orderCode, orderId + "," + deptId);
                }

                String CASE_NBR = record.getTAGCODE();
                Material material = cacheService.getMaterialById(CASE_NBR);
                if (material == null) {
                    material = new Material()
                            .setSpdCode(CASE_NBR)
                            .setDeptId(deptId)
                            .setOrderCode(orderCode)
                            .setGoodsId(record.getARTIID())
                            .setBatchNo(record.getLOTNO())
                            .setExpireDate(record.getEXPDATE())
                            .setCreateDate(record.getUPDDATE());
                    saveList.add(material);
                } else if (!material.getCreateDate().equals(record.getUPDDATE())) {
                    material.setDeptId(deptId)
                            .setOrderCode(orderCode)
                            .setGoodsId(record.getARTIID())
                            .setBatchNo(record.getLOTNO())
                            .setExpireDate(record.getEXPDATE())
                            .setCreateDate(record.getUPDDATE());
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

            //TODO 自动创建配送单
            List<Order> orderList = new ArrayList<>();
            orderMap.keySet().forEach(orderCode -> {
                Order order = cacheService.getOrderByCode(orderCode);
                if (order == null) {
                    String[] ss = orderMap.get(orderCode).split(",");
                    Order newOrder = new Order();
                    newOrder.setId(ss[0]);
                    newOrder.setDeptId(ss[1]);
                    newOrder.setCode(orderCode);
                    newOrder.setCreateDate(DateUtil.now());
                    //初始状态为待复核
                    newOrder.setState(SysConstant.StateNeedConfirm);
                    orderList.add(newOrder);
                }
            });
            if (!orderList.isEmpty()) {
                Db.batchUpdate(orderList, batchSize);
            }
        }

        logger.debug("postBasicData");
    }
}