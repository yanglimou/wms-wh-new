package com.tsj.service;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.google.common.collect.Lists;
import com.jfinal.aop.Inject;
import com.jfinal.kit.Kv;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.common.config.CommonConfig;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.constant.SysConstant;
import com.tsj.common.utils.*;
import com.tsj.domain.model.*;
import com.tsj.service.common.MyService;
import com.tsj.service.spdStockTag.SpdStockTagContainer;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @className: ComService
 * @description: 业务数据服务
 * @author: Frank
 * @create: 2020-03-13 15:23
 */
public class ComService extends MyService {

    public static final Log logger = Log.getLog(ComService.class);

    @Inject
    private CacheService cacheService;

    @Inject
    private BaseService baseService;

    @Inject
    private SysService sysService;

    @Inject
    private SpdService spdService;

    /**
     * 标签过滤
     *
     * @param epcArray   标签EPC集合
     * @param stockState 库存状态
     * @param cabinetId  柜体ID
     * @return 符合条件的标签列表
     */
    private List<Tag> filterTag(String[] epcArray, String stockState, String cabinetId) {
        List<Tag> tagList = new ArrayList<>();

        List<String> invalidTagEpcList = new ArrayList<>();//非法标签集合

        //标签集合去重后遍历
        Arrays.stream(epcArray).distinct().forEach(epc -> {

            //过滤非法标签
            Tag tag = cacheService.getTagByEpc(epc);
            if (tag == null) {
                invalidTagEpcList.add(epc);
                return;
            }

            //无需过滤
            if (StockNone.equals(stockState)) {
                tagList.add(tag);
                return;
            }

            StockTag stockTag = StockTag.dao.findById(tag.getSpdCode());

            //过滤本柜外标签
            if (StockOut.equals(stockState) && (stockTag == null || !stockTag.getCabinetId().equals(cabinetId))) {
                invalidTagEpcList.add(epc);
                return;
            }

            //过滤本柜内标签
            if (StockIn.equals(stockState) && (stockTag != null && stockTag.getCabinetId().equals(cabinetId))) {
                invalidTagEpcList.add(epc);
                return;
            }

            tagList.add(tag);
        });

        if (invalidTagEpcList.size() > 0) {
            logger.warn("find invalid tag:{%s}", String.join(",", invalidTagEpcList));
        }

        return tagList;
    }

    /**
     * 保存耗材入库记录
     *
     * @param cabinetId     柜体ID
     * @param userId        操作人ID
     * @param epcArray      标签集合
     * @param time          时间
     * @param exceptionDesc 异常信息
     * @param confirmed     确认方式
     * @return 操作结果
     */
    public R saveTagIn(String cabinetId, String userId, String[] epcArray, String time, String exceptionDesc, String confirmed) {
        Cabinet cabinet = cacheService.getCabinetById(cabinetId);
        if (cabinet == null) {
            return R.error(ResultCode.DATA_NOT_EXISTED, "目标柜不存在");
        }

        List<String> orderCodeList = new ArrayList<>();
        List<RecordIn> recordInList = new ArrayList<>();
        List<StockTag> updateStockTagList = new ArrayList<>();
        List<StockTag> addStockTagList = new ArrayList<>();
        List<Tag> tagList = new ArrayList<>();
        String recordInOutExceptionId = IDGenerator.makeId();

        filterTag(epcArray, StockIn, cabinetId).forEach(tag -> {
            Goods goods = cacheService.getGoodsById(tag.getGoodsId());

            //柜内转移，直接更改库存状态
            StockTag stockTag = StockTag.dao.findById(tag.getSpdCode());
            if (stockTag != null) {
                stockTag.setCabinetId(cabinetId).setDeptId(cabinet.getDeptId()).setCreateDate(time);
                updateStockTagList.add(stockTag);
                return;
            }

            //柜外退回，增加入柜记录，更改库存状态，判断首次入柜
            RecordIn recordIn = new RecordIn().setId(IDGenerator.makeId()).setExceptionDescId(recordInOutExceptionId).setGoodsId(tag.getGoodsId()).setSpdCode(tag.getSpdCode()).setCabinetId(cabinetId).setConfirmed(confirmed).setCreateUserId(userId).setCreateDate(time).setUpload(UploadFailed).setType(tag.getAccept());
            recordInList.add(recordIn);

            stockTag = new StockTag().setSpdCode(tag.getSpdCode()).setGoodsId(tag.getGoodsId()).setBatchNo(tag.getBatchNo()).setExpireDate(tag.getExpireDate()).setCabinetId(cabinetId).setCreateUserId(userId).setCreateDate(time).setDeptId(cabinet.getDeptId()).setManufacturerId(goods.getManufacturerId()).setSupplierId(goods.getSupplierId());
            addStockTagList.add(stockTag);

            //判断入库耗材配送单号
            if (!orderCodeList.contains(tag.getOrderCode())) {
                orderCodeList.add(tag.getOrderCode());
            }

            //判断标签是否第一次入柜
            if (tag.getAccept().equals(TagNo)) {
                tag.setAccept(TagAc);
                tagList.add(tag);

                //删除缓存信息
                cacheService.removeCache(CacheCom, TagByEpc, tag.getEpc());
            }
        });
        RecordInoutException recordInoutException = new RecordInoutException().setId(recordInOutExceptionId).setType(SysConstant.TagIn).setQuantity(recordInList.size()).setExceptionDesc(exceptionDesc).setCabinetId(cabinetId).setCreateUserId(userId).setCreateDate(time);

        //保存入柜记录,更新库存标签
        boolean succeed = Db.tx(() -> {
            //完成配送单
            orderCodeList.forEach(this::runOrder);

            Db.batchUpdate(tagList, batchSize);
            Db.batchUpdate(updateStockTagList, batchSize);
            Db.batchSave(addStockTagList, batchSize);
            Db.batchSave(recordInList, batchSize);
            if (recordInList.size() > 0) {
                recordInoutException.save();
            }
            return true;
        });

        return succeed ? R.ok() : R.error(ResultCode.CREATE_FAIL, "入柜记录保存失败");
    }

    /**
     * 保存耗材出库记录
     *
     * @param cabinetId     柜体ID
     * @param userId        操作人ID
     * @param epcArray      标签集合
     * @param time          时间
     * @param exceptionDesc 异常信息
     * @param confirmed     确认方式
     * @param patientNo     患者编号
     * @return 操作结果
     */
    public R saveTagOut(String cabinetId, String userId, String[] epcArray, String time, String exceptionDesc, String confirmed, String patientNo) {
        Cabinet cabinet = cacheService.getCabinetById(cabinetId);
        if (cabinet == null) {
            return R.error(ResultCode.DATA_NOT_EXISTED, "目标柜不存在");
        }

        List<RecordOut> recordOutList = new ArrayList<>();
        List<String> spdCodeList = new ArrayList<>();
        String recordInOutExceptionId = IDGenerator.makeId();
        filterTag(epcArray, StockOut, cabinetId).forEach(tag -> {
            spdCodeList.add(tag.getSpdCode());

            RecordOut recordOut = new RecordOut().setId(IDGenerator.makeId()).setExceptionDescId(recordInOutExceptionId).setGoodsId(tag.getGoodsId()).setSpdCode(tag.getSpdCode()).setCabinetId(cabinetId).setConfirmed(confirmed).setPatientNo(patientNo).setCreateUserId(userId).setCreateDate(time).setUpload(UploadFailed);
            recordOutList.add(recordOut);
        });

        RecordInoutException recordInoutException = new RecordInoutException().setId(recordInOutExceptionId).setType(SysConstant.TagOut).setQuantity(recordOutList.size()).setExceptionDesc(exceptionDesc).setCabinetId(cabinetId).setCreateUserId(userId).setCreateDate(time);

        //保存出柜记录,更新库存标签
        boolean succeed = Db.tx(() -> {
            Db.batchSave(recordOutList, batchSize);
            if (recordOutList.size() > 0) {
                recordInoutException.save();
            }
            //TODO 批量删除方法不支持，现改为循环删除
            for (String s : spdCodeList) {
                StockTag.dao.deleteById(s);
            }
            return true;
        });

        return succeed ? R.ok() : R.error(ResultCode.CREATE_FAIL, "出柜记录保存失败");
    }

    /**
     * 保存耗材盘点记录
     *
     * @param cabinetId 柜体ID
     * @param userId    操作人ID
     * @param epcArray  标签集合
     * @param time      时间
     * @return 操作结果
     */
    public R saveTagInventory(String cabinetId, String userId, String[] epcArray, String time) {
        Cabinet cabinet = cacheService.getCabinetById(cabinetId);
        if (cabinet == null) {
            return R.error(ResultCode.DATA_NOT_EXISTED, "目标柜不存在");
        }

        //分析盘盈、盘亏、正常的标签列表
        Map<String, String> tagMap = new HashMap<>();
        filterTag(epcArray, StockNone, cabinetId).forEach(tag -> tagMap.put(tag.getSpdCode(), InventoryMore));

        List<Record> recordList = getStockTagList(Kv.by("cabinetId", cabinetId));
        recordList.forEach(record -> {
            String spdCode = record.getStr("spdCode");
            if (tagMap.containsKey(spdCode)) {
                tagMap.put(spdCode, InventoryNormal);
            } else {
                tagMap.put(spdCode, InventoryLess);
            }
        });

        List<RecordInventory> recordInventoryList = new ArrayList<>();
        String recordInventoryDifferenceId = IDGenerator.makeId();

        //系统自动识别出入库标签集合
        List<String> epcInList = Lists.newArrayList();
        List<String> epcOutList = Lists.newArrayList();

        tagMap.forEach((spdCode, state) -> {
            //生成盘点记录
            Tag tag = Tag.dao.findById(spdCode);
            RecordInventory recordInventory = new RecordInventory().setId(IDGenerator.makeId()).setInventoryDifferenceId(recordInventoryDifferenceId).setCabinetId(cabinetId).setGoodsId(tag.getGoodsId()).setSpdCode(tag.getSpdCode()).setCreateUserId(userId).setCreateDate(time).setState(state).setUpload(UploadFailed);
            recordInventoryList.add(recordInventory);

            if (state.equals(InventoryMore)) {
                epcInList.add(tag.getEpc());
            } else if (state.equals(InventoryLess)) {
                epcOutList.add(tag.getEpc());
            }
        });

        //根据盘盈盘亏结果自动生成出入库记录
        //saveTagOut(cabinetId, getDefaultUserId(), String.join(",", epcOutList).split(","), time, "盘亏自动出库", "false", "");
        saveTagIn(cabinetId, getDefaultUserId(), String.join(",", epcInList).split(","), time, "盘盈自动入库", "false");

        //保存差异
        RecordInventoryDifference recordInventoryDifference = new RecordInventoryDifference().setId(recordInventoryDifferenceId).setCabinetId(cabinetId).setDeptId(cabinet.getDeptId()).setCreateUserId(userId).setCreateDate(time).setStockActual(recordList.size() + epcInList.size() - epcOutList.size())//实际盘点数
                .setStockTheory(recordList.size())//理论盘点数
                .setStockMore(epcInList.size())//盘盈数
                .setStockLess(epcOutList.size());//盘亏数

        //保存盘点记录
        boolean succeed = Db.tx(() -> {
            Db.batchSave(recordInventoryList, batchSize);
            recordInventoryDifference.save();
            return true;
        });

        return succeed ? R.ok() : R.error(ResultCode.CREATE_FAIL, "盘点记录保存失败");
    }

    /**
     * 保存耗材盘点记录
     *
     * @param cabinetId 柜体ID
     * @param userId    操作人ID
     * @param epcArray  标签集合
     * @param time      时间
     * @return 操作结果
     */
    public R saveTagInventoryNew(String cabinetId, String userId, String[] epcArray, String time) {
        List<RecordInventoryNew> recordInventoryNewList = Arrays.stream(epcArray).map(epc -> new RecordInventoryNew().setCreateDate(time).setCabinetId(cabinetId).setEpc(epc).setCreateUserId(userId).setId(IDGenerator.makeId())).collect(Collectors.toList());
        Db.tx(() -> {
//            Db.delete("delete from com_record_inventory_new");
            Db.batchSave(recordInventoryNewList, 1000);
            return true;
        });

        return R.ok();
    }

    /**
     * 保存耗材标签注册信息
     *
     * @param goodsId    耗材ID
     * @param batchNo    批次号
     * @param expireDate 效期
     * @param epc        标签号
     * @param userId     操作人ID
     * @param deptId     科室ID
     * @param orderCode  制标单号
     * @return 操作结果
     */
    public R saveTag(String goodsId, String batchNo, String expireDate, String epc, String userId, String deptId, String orderCode) {
        String GoodsTagPrefix = CommonConfig.prop.get("GoodsTagPrefix");

        //标签前缀过滤
        if (!epc.startsWith(GoodsTagPrefix)) {
            return R.error(ResultCode.DATA_IS_INVALID);
        }

        if (cacheService.getTagByEpc(epc) != null) {
            return R.error(ResultCode.DATA_ALREADY_EXISTED, "标签已绑定其他耗材，请勿重复绑定");
        }

        Dept dept = cacheService.getDeptById(deptId);
        if (dept == null) {
            return R.error(ResultCode.DATA_NOT_EXISTED, "目标科室不存在");
        }

        String time = DateUtil.now();
        //判断耗材是否过期
        Date startDate = DateUtil.parseDateTime(time);
        Date checkedDate = DateUtil.parseDate(expireDate);
        boolean isExpired = DateUtil.isExpired(startDate, DateField.HOUR, 0, checkedDate);
        if (isExpired) {
            return R.error(ResultCode.DATA_IS_INVALID, "耗材过期，不允许关联");
        }

        //EPC删除前缀标识生成唯一码
        String spdCode = IDGenerator.makeId();
        return new Tag().setSpdCode(spdCode).setDeptId(deptId).setOrderCode(orderCode).setGoodsId(goodsId).setEpc(epc).setBatchNo(batchNo).setExpireDate(expireDate).setCreateUserId(userId).setCreateDate(time).setAccept(TagNo).save() ? R.ok() : R.error(ResultCode.CREATE_FAIL, "标签注册失败");
    }

    /**
     * 保存耗材绑定标签信息
     *
     * @param spdCode 耗材唯一码
     * @param epc     标签号
     * @param userId  操作人ID
     * @return 操作结果
     */
    public R saveTagEpc(String spdCode, String epc, String userId) {

        //标签前缀过滤
        if (!epc.startsWith(CommonConfig.prop.get("GoodsTagPrefix"))) {
            return R.error(ResultCode.DATA_IS_INVALID);
        }

        if (cacheService.getTagByEpc(epc) != null) {
            return R.error(ResultCode.DATA_ALREADY_EXISTED, "标签已绑定其他耗材，请勿重复绑定");
        }

        //判断耗材是否存在
        Tag tag = Tag.dao.findById(spdCode);
        if (tag == null) {
            Material material = cacheService.getMaterialById(spdCode);
            if (material == null) {
                return R.error(ResultCode.DATA_NOT_EXISTED, "耗材不存在，请联系SPD系统进行确认");
            }

            //清除缓存
            cacheService.removeCache(CacheCom, TagByEpc, epc);
            cacheService.removeCache(CacheCom, TagById, spdCode);

            return new Tag().setEpc(epc).setGoodsId(material.getGoodsId()).setDeptId(material.getDeptId()).setOrderCode(material.getOrderCode()).setBatchNo(material.getBatchNo()).setExpireDate(material.getExpireDate()).setSpdCode(material.getSpdCode()).setCreateUserId(userId).setCreateDate(DateUtils.getCurrentTime()).setAccept(TagNo).save() ? R.ok() : R.error(ResultCode.CREATE_FAIL);
        } else {
            if (StringUtils.isNotEmpty(tag.getEpc())) {
                return R.error(ResultCode.DATA_ALREADY_EXISTED, "耗材已绑定其他标签，请勿重复绑定");
            }

            //清除缓存
            cacheService.removeCache(CacheCom, TagByEpc, epc);
            cacheService.removeCache(CacheCom, TagById, spdCode);

            return tag.setEpc(epc).setCreateUserId(userId).setCreateDate(DateUtil.now()).update() ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
        }
    }

    /**
     * 耗材标签批量解除绑定
     *
     * @param orderCode 耗材出库单号
     * @param userId    操作人ID
     * @return 操作结果
     */
    public R deleteTagEpcByOrder(String orderCode, String userId) {
        //判断出库单是否存在
        List<Tag> tagList = Tag.dao.find("select * from com_tag where orderCode=?", orderCode);
        if (tagList.isEmpty()) {
            return R.error(ResultCode.DATA_NOT_EXISTED, "出库单不存在");
        }

        //判断是否存在可解绑的标签
        List<Tag> updateList = tagList.stream().filter(tag -> StringUtils.isNotEmpty(tag.getEpc())).collect(Collectors.toList());
        if (updateList.isEmpty()) {
            return R.error(ResultCode.DATA_IS_BLANK, "出库单耗材未绑定，无需解绑");
        }

        //清除缓存
        updateList.forEach(tag -> cacheService.removeCache(CacheCom, TagByEpc, tag.getEpc()));

        String createDate = DateUtils.getCurrentTime();
        boolean succeed = Db.tx(() -> {
            //批量更新
            Db.update("update com_tag set epc=null,createDate=?,createUserId=? where orderCode=?", createDate, userId, orderCode);
            return true;
        });
        return succeed ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
    }

    /**
     * 耗材标签解除绑定
     *
     * @param epc    标签号
     * @param userId 操作人ID
     * @return 操作结果
     */
    public R deleteTagEpc(String epc, String userId) {
        //标签前缀过滤
        if (!epc.startsWith(CommonConfig.prop.get("GoodsTagPrefix"))) {
            return R.error(ResultCode.DATA_IS_INVALID);
        }

        //判断标签EPC是否已存在
        Tag tag = cacheService.getTagByEpc(epc);
        if (tag == null) {
            return R.error(ResultCode.DATA_NOT_EXISTED, "标签不存在");
        }

        //清除缓存
        cacheService.removeCache(CacheCom, TagByEpc, epc);
        cacheService.removeCache(CacheCom, TagById, tag.getSpdCode());

        String createDate = DateUtils.getCurrentTime();
        return tag.setEpc(null).setCreateUserId(userId).setCreateDate(createDate).update() ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
    }

    /**
     * 查询库存数量列表
     *
     * @param cond 条件键值对
     * @return 查询列表
     */
    public List<Record> getStockQuantityList(Kv cond) {
        String select = "SELECT goodsId,count(spdCode) as quantity from com_stock_tag WHERE cabinetId=? GROUP BY goodsId";
        return Db.find(select, cond.getStr("cabinetId"));
    }

    /**
     * 查询库存标签列表
     *
     * @param cond 条件键值对
     * @return 查询列表
     */
    public List<Record> getStockTagList(Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true).put(SQL_PATTERN_EQUAL, "cabinetId", "stock.cabinetId").put(SQL_PATTERN_EQUAL, "deptId", "stock.deptId").order(SQL_SORT_ASC, "stock.expireDate,goodsName").build();

        String select = "SELECT stock.*,tag.epc,(SELECT name from base_goods WHERE id=stock.goodsId) as goodsName FROM COM_STOCK_TAG stock left join COM_TAG tag on stock.spdCode = tag.spdCode";
        return Db.find(select + condition.getSql(), condition.getParas());
    }

    /**
     * 查询库存基数列表
     *
     * @param cond 条件键值对
     * @return 查询列表
     */
    public List<Record> getStockBaseList(Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true).put(SQL_PATTERN_EQUAL, "deptId", "deptId").put(SQL_PATTERN_EQUAL, "goodsId", "goodsId").build();

        String select = "SELECT * FROM COM_STOCK_BASE";
        return Db.find(select + condition.getSql(), condition.getParas());
    }

    /**
     * 查询库存缺货列表
     *
     * @param cond 条件键值对
     * @return 查询列表
     */
    public List<Record> getShortageList(Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true).put(SQL_PATTERN_EQUAL, "deptId", "base.deptId").put(SQL_PATTERN_EQUAL, "goodsId", "base.goodsId").put(SQL_PATTERN_EQUAL, "manufacturerId", "goods.manufacturerId").put(SQL_PATTERN_EQUAL, "supplierId", "goods.supplierId").build();

        String select = "SELECT base.* from com_stock_base base left join base_goods goods on base.goodsId=goods.id " + condition.getSql();
        List<Record> stockBaseList = Db.find(select, condition.getParas());

        //根据当前库存量和库存基数比较计算补充量
        stockBaseList.forEach(stockBase -> {
            int supplement = stockBase.getInt("max");
            Record record = Db.findFirst("SELECT count(spdCode) as quantity from com_stock_tag where deptId=? and goodsId=? group by deptId,goodsId", stockBase.getStr("deptId"), stockBase.getStr("goodsId"));
            if (record != null) {
                supplement = stockBase.getInt("max") - record.getInt("quantity");
            }

            stockBase.set("supplement", supplement);
        });
        return stockBaseList;
    }

    /**
     * 查询盘点数量列表
     *
     * @param cond 条件键值对
     * @return 查询列表
     */
    public List<Record> getTagInventoryQuantityList(Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true).put(SQL_PATTERN_BETWEEN, "dateRange", "rd.createDate").put(SQL_PATTERN_EQUAL, "cabinetId", "rd.cabinetId").build();

        String select = "SELECT rd.cabinetId,rd.createDate, rd.createUserId" + ",count(case  when rd.state='normal' or rd.state='less' then 1 else null end) AS totalQuantity" + ",count(case  when rd.state='normal' or rd.state='more' then 1 else null end) AS realQuantity)" + ",count(case  when rd.state='less' or rd.state='more' then 1 else null end) AS differenceQuantity)" + " FROM com_record_inventory rd " + condition.getSql() + " GROUP BY rd.cabinetId, rd.createDate";
        return Db.find(select, condition.getParas());
    }

    /**
     * 查询盘点标签列表
     *
     * @param cond 条件键值对
     * @return 查询列表
     */
    public List<Record> getTagInventoryRecordList(Kv cond) {
        String date = cond.getStr("dateRange");
        QueryCondition condition = QueryConditionBuilder.by(cond, false).put(date.contains(" - ") ? SQL_PATTERN_BETWEEN : SQL_PATTERN_EQUAL, "dateRange", "rd.createDate").build();

        String select = "SELECT rd.*,tag.batchNo,tag.expireDate FROM com_record_inventory rd LEFT JOIN com_tag tag ON rd.spdCode = tag.spdCode " + " WHERE rd.cabinetId in (SELECT id from base_cabinet where deptId='" + cond.getStr("deptId") + "')" + condition.getSql();
        return Db.find(select, condition.getParas());
    }

    /**
     * 查询入库数量列表
     *
     * @param cond 条件键值对
     * @return 查询列表
     */
    public List<Record> getTagInQuantityList(Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, false).put(SQL_PATTERN_BETWEEN, "dateRange", "rd.createDate").build();

        String select = "SELECT goods.supplierId, rd.createDate, rd.createUserId, COUNT(rd.spdCode) AS quantity" + " FROM com_record_in rd LEFT JOIN base_goods goods ON rd.goodsId = goods.id " + " WHERE rd.cabinetId in (SELECT id from base_cabinet where deptId='" + cond.getStr("deptId") + "')" + condition.getSql() + " GROUP BY goods.supplierId, rd.createDate";
        return Db.find(select, condition.getParas());
    }

    /**
     * 查询入库标签列表
     *
     * @param cond 条件键值对
     * @return 查询列表
     */
    public List<Record> getTagInRecordList(Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, false).put(SQL_PATTERN_EQUAL, "type", "rd.type").put(SQL_PATTERN_BETWEEN, "dateRange", "rd.createDate").order(SQL_SORT_DESC, "rd.createDate").build();

        String select = "SELECT rd.*,tag.batchNo,tag.expireDate FROM com_record_in rd LEFT JOIN com_tag tag ON rd.spdCode = tag.spdCode " + " WHERE rd.cabinetId in (SELECT id from base_cabinet where deptId='" + cond.getStr("deptId") + "')" + condition.getSql();
        return Db.find(select, condition.getParas());
    }

    /**
     * 查询出库标签列表
     *
     * @param cond 条件键值对
     * @return 查询列表
     */
    public List<Record> getTagOutRecordList(Kv cond) {

        //根据柜体名称模糊查询
        if (StringUtils.isNotEmpty(cond.getStr("cabinetName"))) {
            List<Cabinet> ids = Cabinet.dao.find("select id from base_cabinet where deptId=? and name like concat('%', ?, '%') ", cond.getStr("deptId"), cond.getStr("cabinetName"));
            cond.set("cabinetId", ids.stream().map(Cabinet::getId).collect(Collectors.joining(",")));
        } else {
            List<Cabinet> ids = Cabinet.dao.find("select id from base_cabinet where deptId=?  ", cond.getStr("deptId"));
            cond.set("cabinetId", ids.stream().map(Cabinet::getId).collect(Collectors.joining(",")));
        }

        //根据人员名称模糊查询
        if (StringUtils.isNotEmpty(cond.getStr("userName"))) {
            List<Cabinet> ids = Cabinet.dao.find("select id from base_user where name like concat('%', ?, '%') ", cond.getStr("userName"));
            cond.set("userId", ids.stream().map(Cabinet::getId).collect(Collectors.joining(",")));
        }

        QueryCondition condition = QueryConditionBuilder.by(cond, true).put(SQL_PATTERN_BETWEEN, "dateRange", "rd.createDate").put(SQL_PATTERN_IN, "cabinetId", "rd.cabinetId").put(SQL_PATTERN_IN, "userId", "rd.createUserId").order(SQL_SORT_DESC, "rd.createDate").build();

        String select = "SELECT rd.*,tag.batchNo,tag.expireDate FROM com_record_out rd LEFT JOIN com_tag tag ON rd.spdCode = tag.spdCode " + condition.getSql();
        return Db.find(select, condition.getParas());
    }

    /**
     * 查询出库未归还标签列表
     *
     * @param cond 条件键值对
     * @return 查询列表
     */
    public List<Kv> getTagOutWithoutReturnRecordList(Kv cond) {

        boolean spdFlag = CommonConfig.prop.getBoolean("spd");

        //查询SPD库存
        List<String> spdStockTagList = new ArrayList<>();
        if (spdFlag) {
            R ret = spdService.postStockTagList(cond.getStr("deptId"));
            if (ret.isSuccess()) {
                List<Tag> tagList = (List<Tag>) ret.get("data");
                tagList.forEach(tag -> spdStockTagList.add(tag.getSpdCode()));
            }
        }

        //查询本地库存记录
        List<String> stockTagSpdCodeList = getStockTagList(Kv.by("deptId", cond.getStr("deptId"))).stream().map(record -> record.getStr("spdCode")).collect(Collectors.toList());

        Kv spdCodeKv = Kv.create();

        //过滤已归还记录，排除重复出库记录
        List<Record> recordList = getTagOutRecordList(cond).stream().filter(record -> {
            String spdCode = record.getStr("spdCode");

            //SPD库存找不到该耗材，认为已消耗
            if (spdFlag && !spdStockTagList.contains(spdCode)) return false;

            //本地库存找到该耗材，认为已归还
            if (stockTagSpdCodeList.contains(spdCode)) return false;

            //检测到重复出库记录，进行过滤
            if (spdCodeKv.containsKey(spdCode)) return false;

            spdCodeKv.set(spdCode, null);

            return true;
        }).collect(Collectors.toList());

        //按人员分组
        Map<String, List<Record>> groupByUser = recordList.stream().collect(Collectors.groupingBy(record -> {
            String createUserId = record.getStr("createUserId");
            if (StringUtils.isEmpty(createUserId)) {
                createUserId = getDefaultUserId();
            }
            return createUserId;
        }));

        //结果统计
        List<Kv> kvList = new ArrayList<>();
        groupByUser.forEach((k, list) -> {

            //按近效期排序
            list.sort(Comparator.comparing(a -> a.getStr("expireDate")));

            Kv kv = Kv.create();
            kv.set("createUserId", k);
            kv.set("recordCount", list.size());
            kv.set("recordList", list);
            kvList.add(kv);
        });

        return kvList;
    }

    private List<Kv> getProductQuantityList(List<Record> recordList) {
        Map<String, Integer> quantityMap = new HashMap<>();
        recordList.forEach(record -> {
            String productId = record.getStr("productId");
            if (quantityMap.containsKey(productId)) {
                quantityMap.put(productId, quantityMap.get(productId) + 1);
            } else {
                quantityMap.put(productId, 1);
            }
        });

        List<Kv> materialQuantityList = new ArrayList<>();
        quantityMap.forEach((productId, quantity) -> {
            Kv kv = Kv.create();
            kv.set("productId", productId);
            kv.set("quantity", quantity);
            materialQuantityList.add(kv);
        });

        return materialQuantityList;
    }

    public List<Tag> getTagListByEpc(String[] epcArray) {
        return filterTag(epcArray, StockNone, null);
    }

    public List<Tag> getTagListByEpcFuzzy(String[] epcArray) {
        List<Tag> tagList = new ArrayList<>();

        List<String> tagEpcList = new ArrayList<>();
        Arrays.stream(epcArray).distinct().forEach(epc -> {
            List<Tag> list = Tag.dao.find("select * from com_tag where epc REGEXP ?", epc);
            list.forEach(tag -> {
                if (!tagEpcList.contains(tag.getEpc())) {
                    tagEpcList.add(tag.getEpc());
                    tagList.add(tag);
                }
            });
        });
        return tagList;
    }

    public List<Tag> getTagListBySpdCode(String[] spdCodeArray) {
        List<Tag> tagList = new ArrayList<>();
        for (String spdCode : spdCodeArray) {
            Tag tag = Tag.dao.findById(spdCode);
            if (tag != null) {
                tagList.add(tag);
            } else {
                //TODO 本地标签库未找到，去出库耗材数据库查找
                Material material = cacheService.getMaterialById(spdCode);
                if (material != null) {
                    tag = new Tag().setSpdCode(spdCode).setDeptId(material.getDeptId()).setOrderCode(material.getOrderCode()).setGoodsId(material.getGoodsId()).setBatchNo(material.getBatchNo()).setExpireDate(material.getExpireDate()).setCreateDate(material.getCreateDate()).setAccept(TagNo);
                    tagList.add(tag);
                }
            }
        }
        return tagList;
    }

    public List<Record> getSaveTagList(Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true).put(SQL_PATTERN_EQUAL, "deptId", "deptId").put(SQL_PATTERN_BETWEEN, "dateRange", "createDate").build();

        String select = "SELECT * FROM com_tag " + condition.getSql();
        return Db.find(select, condition.getParas());
    }

    public Record getTagInventoryRecordDifferenceList(Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true).put(SQL_PATTERN_EQUAL, "cabinetId", "cabinetId").order(SQL_SORT_DESC, "createDate").build();

        String select = "SELECT * FROM com_record_inventory_difference" + condition.getSql();
        return Db.findFirst(select, condition.getParas());
    }

    public List<Record> getTagInventoryRecordDifferenceDetail(Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true).put(SQL_PATTERN_EQUAL, "inventoryDifferenceId", "rd.inventoryDifferenceId").build();

        String select = "SELECT rd.*, tag.batchNo,tag.expireDate,tag.epc FROM com_record_inventory rd left join com_tag tag on rd.spdCode=tag.spdCode " + condition.getSql() + "ORDER BY state";
        return Db.find(select, condition.getParas());
    }

    /**
     * 生成测试的数据
     *
     * @param index        起始标签号
     * @param count        标签数量
     * @param batchNo      批次号
     * @param deptId       科室ID
     * @param createUserId 创建人ID
     * @return
     */
    public R generateTestData(int index, int count, String batchNo, String deptId, String createUserId) {
        String orderCode = IDGenerator.makeId();
        String createDate = DateUtils.getCurrentTime();

        //每种耗材50件
        int number = 50;
        List<Goods> goodsList = Goods.dao.findAll();
        String goodsId = goodsList.get(0).getId();

        String expireDate = DateUtils.getCurrentDate();
        List<Tag> tags = new ArrayList<>();
        List<Material> materials = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            if (i % number == 0) {
                if (goodsList.size() > (i / number)) {
                    goodsId = goodsList.get(i / number).getId();
                }
            }

            String epc = "474B4854" + String.format("%16s", (index + i)).replace(" ", "0");
            if (cacheService.getTagByEpc(epc) == null) {

                Material material = new Material();
                material.setSpdCode(IDGenerator.makeId());
                material.setDeptId(deptId);
                material.setOrderCode(orderCode);
                material.setGoodsId(goodsId);
                material.setBatchNo(batchNo);
                material.setExpireDate(DateUtils.addMonth(expireDate, random.nextInt(12)));
                material.setCreateDate(createDate);

                materials.add(material);

                Tag tag = new Tag().setOrderCode(orderCode).setSpdCode(material.getSpdCode()).setDeptId(deptId).setGoodsId(goodsId).setEpc(epc).setBatchNo(batchNo).setExpireDate(material.getExpireDate()).setCreateUserId(createUserId).setCreateDate(createDate).setAccept(TagNo);

                tags.add(tag);
            }
        }

        Order order = new Order();
        order.setId(IDGenerator.makeId());
        order.setCode(orderCode);
        order.setDeptId(deptId);
        order.setCreateDate(createDate);
        order.setState(SysConstant.StateNeedConfirm);

        boolean succeed = Db.tx(() -> {
            order.save();
            Db.batchSave(materials, batchSize);
            Db.batchSave(tags, batchSize);
            return true;
        });

        return succeed ? R.ok().putData(tags) : R.error(ResultCode.CREATE_FAIL, "一键测试数据生成失败");
    }

    public R getTagList(String orderCode) {
        Order order = cacheService.getOrderByCode(orderCode);
        if (order == null) {
            return R.error(ResultCode.ORDER_NOT_EXIST);
        }
        List<Tag> tagList = Tag.dao.find("select * from com_tag where orderCode=?", orderCode);
        return R.ok().putData(tagList);
    }

    public List<Order> getUncompletedOrderList(String deptId) {
        return Order.dao.find("select * from com_order where deptId=? and state=?", deptId, SysConstant.StateUncompleted);
    }

    /**
     * 确认配送单
     *
     * @param orderCode 配送单号
     * @return
     */
    public R confirmOrder(String orderCode) {
        Order order = cacheService.getOrderByCode(orderCode);
        if (order == null) {
            return R.error(ResultCode.ORDER_NOT_EXIST);
        }
        if (!order.getState().equals(SysConstant.StateNeedConfirm)) {
            return R.error(ResultCode.ORDER_ALREADY_COMPLETE);
        }

        //清除缓存
        cacheService.removeCache(CacheCom, OrderByCode, orderCode);

        //开启事务
        boolean succeed = Db.tx(() -> {
            //变更入库单状态为未完成
            order.setState(SysConstant.StateUncompleted);
            return order.update();
        });

        return succeed ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
    }

    /**
     * 取消配送单
     *
     * @param orderCode 配送单号
     * @return
     */
    public R cancelOrder(String orderCode) {
        Order order = cacheService.getOrderByCode(orderCode);
        if (order == null) {
            return R.error(ResultCode.ORDER_NOT_EXIST);
        }
        if (!order.getState().equals(SysConstant.StateNeedConfirm)) {
            return R.error(ResultCode.ORDER_ALREADY_COMPLETE);
        }

        //清除缓存
        cacheService.removeCache(CacheCom, OrderByCode, orderCode);

        //开启事务
        boolean succeed = Db.tx(() -> {
            //变更入库单状态为取消
            order.setState(SysConstant.StateCancel);
            order.setCompleteDate(DateUtil.now());
            return order.update();
        });

        return succeed ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
    }

    /**
     * 完成配送单
     *
     * @param orderCode 配送单号
     * @return
     */
    public R runOrder(String orderCode) {
        Order order = cacheService.getOrderByCode(orderCode);
        if (order == null) {
            return R.error(ResultCode.ORDER_NOT_EXIST);
        }
        if (!order.getState().equals(SysConstant.StateUncompleted)) {
            return R.error(ResultCode.ORDER_ALREADY_COMPLETE);
        }

        //清除缓存
        cacheService.removeCache(CacheCom, OrderByCode, orderCode);

        //开启事务
        boolean succeed = Db.tx(() -> {
            //变更入库单状态为取消
            order.setState(SysConstant.StateComplete);
            order.setCompleteDate(DateUtil.now());
            return order.update();
        });

        return succeed ? R.ok() : R.error(ResultCode.UPDATE_FAIL);
    }


    /////////////////////////////////////////////////////////////////////

    public Page<Record> getTagPage(int pageNumber, int pageSize, Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, true).put(SQL_PATTERN_EQUAL, "accept").put(SQL_PATTERN_EQUAL, "goodsId").put(SQL_PATTERN_EQUAL, "deptId").put(SQL_PATTERN_LIKE, "spdCode").put(SQL_PATTERN_LIKE, "epc").put(SQL_PATTERN_EQUAL, "orderCode").put(SQL_PATTERN_BETWEEN, "dateRange", "createDate").order(SQL_SORT_DESC, "createDate").order(SysConstant.SQL_SORT_DESC, "epc").build();

        String select = "select * ";
        String sqlExceptSelect = " from com_tag ";
        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect + condition.getSql(), condition.getParas());
    }

    public R getStockTagQuantityPage(int pageNumber, int pageSize, Kv cond) {
        String expireBeforeMonth = sysService.getConfig("cabinet", "expireBeforeMonth").getValue();
        String date = DateUtils.addMonth(DateUtils.getCurrentDate(), Integer.parseInt(expireBeforeMonth));

//        String sql="";


//        if (StringUtils.isNotEmpty(cond.getStr("cabinetName"))) {
//            List<Cabinet> ids = Cabinet.dao.find("select id from base_cabinet where name like concat('%', ?, '%') ", cond.getStr("cabinetName"));
//            cond.set("cabinetId", ids.stream().map(Cabinet::getId).collect(Collectors.joining(",")));
//        }

        QueryCondition condition = QueryConditionBuilder.by(cond, true).put(SQL_PATTERN_EQUAL, "a.deptId").put(SQL_PATTERN_LIKE, "c.name").put(SQL_PATTERN_LIKE, "b.name").build();

        String select = "SELECT a.deptId, a.cabinetId, a.goodsId, COUNT(*) totalQuantity, COUNT( CASE WHEN a.expireDate < '" + date + "' THEN 1 END ) expireQuantity";
        String sqlExceptSelect = "FROM com_stock_tag a LEFT JOIN base_cabinet b on a.cabinetId=b.id LEFT JOIN base_goods c on a.goodsId=c.id";
        Page<Record> paginate = Db.paginate(pageNumber, pageSize, select, sqlExceptSelect + condition.getSql() + " GROUP BY a.cabinetId, a.goodsId ", condition.getParas());
        Record record = Db.findFirst("SELECT COUNT(*) totalQuantity, COUNT( CASE WHEN a.expireDate < '2023-01-05' THEN 1 END ) expireQuantity " + sqlExceptSelect + condition.getSql(), condition.getParas());
        Integer totalQuantity = record.getInt("totalQuantity");
        Integer expireQuantity = record.getInt("expireQuantity");
        return R.ok().putData(paginate.getList()).put("count", paginate.getTotalRow()).put("totalQuantity", totalQuantity).put("expireQuantity", expireQuantity);
    }

    /**
     * 查询库存标签分页
     *
     * @param pageNumber
     * @param pageSize
     * @param cond
     * @return
     */
    public Page<Record> getStockTagPage(int pageNumber, int pageSize, Kv cond) {
        String expireBeforeMonth = sysService.getConfig("cabinet", "expireBeforeMonth").getValue();
        String date = DateUtils.addMonth(DateUtils.getCurrentDate(), Integer.parseInt(expireBeforeMonth));

        if ("expireDate".equals(cond.getStr("type"))) {
            cond.set("state2", "1");
        }

        if (StringUtils.isNotEmpty(cond.getStr("cabinetName"))) {
            List<Cabinet> ids = Cabinet.dao.find("select id from base_cabinet where deptId=? and name like concat('%', ?, '%') ", cond.getStr("deptId"), cond.getStr("cabinetName"));
            cond.set("cabinetId", ids.stream().map(Cabinet::getId).collect(Collectors.joining(",")));
        }

        QueryCondition condition = QueryConditionBuilder.by(cond, true).put(SQL_PATTERN_EQUAL, "deptId", "tag.deptId").put(SQL_PATTERN_EQUAL, "goodsId", "tag.goodsId").put(SQL_PATTERN_EQUAL, "state2", "(tag.expireDate<'" + date + "')").put(SQL_PATTERN_IN, "cabinetId", "tag.cabinetId").put(SQL_PATTERN_LIKE, "spdCode", "tag.spdCode").order(SQL_SORT_ASC, "tag.deptId,tag.goodsId,tag.spdCode").build();

        String select = "select tt.epc,tag.*,tag.expireDate<'" + DateUtils.getCurrentDate() + "' state1,tag.expireDate<'" + date + "' state2";
        String sqlExceptSelect = " from com_stock_tag tag left join com_tag tt on tag.spdCode=tt.spdCode";
        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect + condition.getSql(), condition.getParas());
    }

    /**
     * 查询库存标签分页
     *
     * @param pageNumber
     * @param pageSize
     * @param cond
     * @return
     */
    public Page<Tag> getSpdStockTagPage(int pageNumber, int pageSize, Kv cond) {
        boolean spdFlag = CommonConfig.prop.getBoolean("spd");

        //查询SPD库存
        List<Tag> spdStockTagList = new ArrayList<>();
        if (spdFlag) {
            R ret = spdService.postStockTagList(cond.getStr("deptId"));
            if (ret.isSuccess()) {
                List<Tag> tagList = (List<Tag>) ret.get("data");
                spdStockTagList.addAll(tagList);
            }
        }

        String expireBeforeMonth = sysService.getConfig("cabinet", "expireBeforeMonth").getValue();
        String date = DateUtils.addMonth(DateUtils.getCurrentDate(), Integer.parseInt(expireBeforeMonth));

        String goodsId = cond.getStr("goodsId");
        String spdCode = cond.getStr("spdCode");
        boolean expire = "expireDate".equals(cond.getStr("type"));

        //多条件过滤
        Predicate<Tag> goodsIdFilter = tag -> StringUtils.isEmpty(goodsId) || tag.getGoodsId().equals(goodsId);
        Predicate<Tag> spdCodeFilter = tag -> StringUtils.isEmpty(spdCode) || tag.getSpdCode().contains(spdCode);
        Predicate<Tag> expireDateFilter = tag -> !expire || DateUtils.compare(date, tag.getExpireDate());

        List<Tag> recordList = spdStockTagList.stream().filter(goodsIdFilter).filter(spdCodeFilter).filter(expireDateFilter).sorted(Comparator.comparing(Tag::getGoodsId)).collect(Collectors.toList());

        Page<Tag> page = new Page<>();
        page.setPageNumber(pageNumber);
        page.setPageSize(pageSize);
        page.setTotalPage(recordList.size() / pageSize);
        page.setTotalRow(recordList.size());

        if (pageNumber > page.getTotalPage()) {
            page.setList(new ArrayList<>());
        } else {
            int fromIndex = (pageNumber - 1) * pageSize;
            int toIndex = pageNumber * pageSize;
            if (page.getTotalRow() < toIndex) {
                toIndex = page.getTotalRow();
            }
            page.setList(recordList.subList(fromIndex, toIndex));
        }
        return page;
    }

    /**
     * 查询出入柜分页
     *
     * @param pageNumber
     * @param pageSize
     * @param cond
     * @return
     */
    public Page<Record> getTagRecordQuantityPage(int pageNumber, int pageSize, Kv cond) {
        if (StringUtils.isNotEmpty(cond.getStr("deptId"))) {
            List<String> cabinetIdList = baseService.getCabinetList(Kv.by("deptId", cond.getStr("deptId"))).stream().map(e -> e.getStr("id")).collect(Collectors.toList());
            cond.set("cabinetIds", String.join(",", cabinetIdList));

            //科室下没有柜体，返回空集合
            if (cabinetIdList.isEmpty()) {
                Page<Record> page = new Page<>();
                page.setTotalRow(0);
                page.setList(new ArrayList<>());
                return page;
            }
        }

        QueryCondition condition = QueryConditionBuilder.by(cond, true).put(SQL_PATTERN_BETWEEN, "dateRange", "rd.createDate").put(SQL_PATTERN_EQUAL, "type", "rd.type").put(SQL_PATTERN_IN, "cabinetIds", "rd.cabinetId").order(SQL_SORT_DESC, "rd.createDate").build();

        String select = "SELECT rd.*";
        String sqlExceptSelect = "FROM com_record_inout_exception rd";

        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect + condition.getSql(), condition.getParas());
    }

    /**
     * 查询出入柜明细分页
     *
     * @param pageNumber
     * @param pageSize
     * @param cond
     * @param type
     * @return
     */
    public Page<Record> getTagRecordPage(int pageNumber, int pageSize, Kv cond, String type) {
        if (StringUtils.isNotEmpty(cond.getStr("deptId"))) {
            List<String> cabinetIdList = baseService.getCabinetList(Kv.by("deptId", cond.getStr("deptId"))).stream().map(e -> e.getStr("id")).collect(Collectors.toList());
            cond.set("cabinetIds", String.join(",", cabinetIdList));

            //科室下没有柜体，返回空集合
            if (cabinetIdList.isEmpty()) {
                Page<Record> page = new Page<>();
                page.setTotalRow(0);
                page.setList(new ArrayList<>());
                return page;
            }
        }

        QueryCondition condition = QueryConditionBuilder.by(cond, false).put(SQL_PATTERN_EQUAL, "goodsId", "tag.goodsId").put(SQL_PATTERN_BETWEEN, "dateRange", "rd.createDate").put(SQL_PATTERN_LIKE, "spdCode", "rd.spdCode").put(SQL_PATTERN_IN, "cabinetIds", "rd.cabinetId").order(SQL_SORT_DESC, "rd.createDate").build();

        String select = null;
        String sqlExceptSelect = null;
        switch (type) {
            case "tagIn":
                select = "SELECT rd.*,tag.batchNo,tag.expireDate,'tagIn' as recordType";
                sqlExceptSelect = "FROM com_record_in rd LEFT JOIN com_tag tag ON rd.spdCode = tag.spdCode " + "WHERE rd.type='no'";
                break;

            case "tagReturn":
                select = "SELECT rd.*,tag.batchNo,tag.expireDate,'tagReturn' as recordType";
                sqlExceptSelect = "FROM com_record_in rd LEFT JOIN com_tag tag ON rd.spdCode = tag.spdCode " + "WHERE rd.type='ac'";
                break;

            case "tagOut":
                select = "SELECT rd.*,tag.batchNo,tag.expireDate,'tagOut' as recordType";
                sqlExceptSelect = "FROM com_record_out rd LEFT JOIN com_tag tag ON rd.spdCode = tag.spdCode " + "WHERE 1=1 ";
                break;

        }

        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect + condition.getSql(), condition.getParas());
    }

    /**
     * 查询出入柜明细分页
     *
     * @param pageNumber
     * @param pageSize
     * @param exceptionDescId
     * @param type
     * @return
     */
    public Page<Record> getTagRecordPage(int pageNumber, int pageSize, String exceptionDescId, String type) {
        String select = null;
        String sqlExceptSelect = null;
        switch (type) {
            case "in":
                select = "SELECT rd.*,tag.batchNo,tag.expireDate,IF (rd.type='no','tagIn','tagReturn') as recordType";
                sqlExceptSelect = "FROM com_record_in rd LEFT JOIN com_tag tag ON rd.spdCode = tag.spdCode " + "WHERE rd.exceptionDescId=? ";
                break;

            case "out":
                select = "SELECT rd.*,tag.batchNo,tag.expireDate,'tagOut' as recordType";
                sqlExceptSelect = "FROM com_record_out rd LEFT JOIN com_tag tag ON rd.spdCode = tag.spdCode " + "WHERE rd.exceptionDescId=? ";
                break;

        }

        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect, exceptionDescId);
    }

    /**
     * 获取指定年月的统计数据
     *
     * @param dateStr yyyy-MM-dd
     * @return
     */
    public R getTagRecordChartData(String dateStr) {
        int days = DateUtils.getDaysOfMonth(dateStr);

        List<String> xAxisList = new ArrayList<>();
        List<Integer> tagQuantityList = new ArrayList<>();
        List<Integer> tagInRecordQuantityList = new ArrayList<>();
        List<Integer> tagOutRecordQuantityList = new ArrayList<>();
        List<Integer> tagReturnRecordQuantityList = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            xAxisList.add(String.valueOf(i + 1));
            tagQuantityList.add(0);
            tagInRecordQuantityList.add(0);
            tagOutRecordQuantityList.add(0);
            tagReturnRecordQuantityList.add(0);
        }

        String date = dateStr.substring(0, 7);

        String sql = "SELECT count(*) as quantity, DAY(createDate) as cd FROM com_tag t WHERE date_format(createDate,'%Y-%m') = ? GROUP BY DAY(createDate)";
        Db.find(sql, date).forEach(record -> {
            int day = record.getInt("cd") - 1;
            tagQuantityList.set(day, record.getInt("quantity"));
        });

        String sql1 = "SELECT count(*) as quantity, DAY(createDate) as cd FROM com_record_in t WHERE type='no' and date_format(createDate,'%Y-%m') = ? GROUP BY DAY(createDate)";
        Db.find(sql1, date).forEach(record -> {
            int day = record.getInt("cd") - 1;
            tagInRecordQuantityList.set(day, record.getInt("quantity"));
        });

        String sql2 = "SELECT count(*) as quantity, DAY(createDate) as cd FROM com_record_out t WHERE date_format(createDate,'%Y-%m') = ? GROUP BY DAY(createDate)";
        Db.find(sql2, date).forEach(record -> {
            int day = record.getInt("cd") - 1;
            tagOutRecordQuantityList.set(day, record.getInt("quantity"));
        });

        String sql3 = "SELECT count(*) as quantity, DAY(createDate) as cd FROM com_record_in t WHERE type='ac' and date_format(createDate,'%Y-%m') = ? GROUP BY DAY(createDate)";
        Db.find(sql3, date).forEach(record -> {
            int day = record.getInt("cd") - 1;
            tagReturnRecordQuantityList.set(day, record.getInt("quantity"));
        });

        List<Integer> integerList = Lists.newArrayList();
        List<Kv> kvList = new ArrayList<>();
        String sql4 = "SELECT goodsId, count(*) AS quantity FROM com_record_out WHERE date_format(createDate, '%Y-%m') = ? GROUP BY goodsId ORDER BY quantity DESC";
        Db.find(sql4, date).forEach(record -> {

            if (kvList.size() < 10) {
                Goods goods = cacheService.getGoodsById(record.getStr("goodsId"));
                kvList.add(Kv.by("name", goods.getName() + " " + goods.getSpec()).set("value", record.getInt("quantity")));
            } else {
                integerList.add(record.getInt("quantity"));
            }
        });

        if (!integerList.isEmpty()) {
            integerList.stream().reduce(Integer::sum).ifPresent(integer -> kvList.add(Kv.by("name", "其他耗材").set("value", integer)));
        }

        Kv kv = Kv.create();
        kv.set("xAxisList", xAxisList);
        kv.set("tagQuantityList", tagQuantityList);
        kv.set("tagInRecordQuantityList", tagInRecordQuantityList);
        kv.set("tagOutRecordQuantityList", tagOutRecordQuantityList);
        kv.set("tagReturnRecordQuantityList", tagReturnRecordQuantityList);
        kv.set("kvList", kvList);
        kv.set("totalCount", kvList.stream().mapToInt(k -> k.getInt("value")).sum());
        return R.ok().putData(kv);
    }

    /**
     * 查询科室库存列表
     *
     * @param deptId 科室ID
     * @return 操作结果
     */
    public R getStockTagList(String deptId) {
        Kv cond = Kv.by("deptId", deptId);
        QueryCondition condition = QueryConditionBuilder.by(cond, true).put(SQL_PATTERN_EQUAL, "deptId", "stock.deptId").build();

        String select = "SELECT tag.* FROM COM_STOCK_TAG stock left join COM_TAG tag on stock.spdCode = tag.spdCode";
        List<Record> tagList = Db.find(select + condition.getSql(), condition.getParas());
        return R.ok().putData(tagList);
    }

    /**
     * 查询盘点统计分页
     *
     * @param pageNumber
     * @param pageSize
     * @param cond
     * @return
     */
    public Page<Record> getTagInventoryRecordDifferencePage(int pageNumber, int pageSize, Kv cond) {
        QueryCondition condition = QueryConditionBuilder.by(cond, false).put(SQL_PATTERN_EQUAL, "deptId", "deptId").put(SQL_PATTERN_BETWEEN, "dateRange", "createDate").order(SQL_SORT_DESC, "createDate").build();

        String select = "SELECT * ";
        String sqlExceptSelect = "FROM com_record_inventory_difference where 1=1";

        //判断是否查询异常情况
        if ("exception".equals(cond.getStr("type"))) {
            sqlExceptSelect += " and (stockMore>0 or stockLess>0)";
        }
        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect + condition.getSql(), condition.getParas());
    }

    /**
     * 查询盘点明细分页
     *
     * @param pageNumber
     * @param pageSize
     * @param inventoryDifferenceId
     * @return 查询结果
     */
    public Page<Record> getTagInventoryListPage(int pageNumber, int pageSize, String inventoryDifferenceId) {
        String select = "SELECT * ";
        String sqlExceptSelect = "FROM com_record_inventory where inventoryDifferenceId=? order by state,spdCode asc";
        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect, inventoryDifferenceId);
    }

    /**
     * 查询出库未归还标签列表分页
     *
     * @param cond 条件键值对
     * @param type 查询类型
     * @return 查询列表
     */
    public List<Kv> getTagOutWithoutReturnRecordListPage(Kv cond, int type) {

        boolean spdFlag = CommonConfig.prop.getBoolean("spd");

        //查询SPD库存
        List<String> spdStockTagList = new ArrayList<>();
        if (spdFlag) {
            R ret = spdService.postStockTagList(cond.getStr("deptId"));
            if (ret.isSuccess()) {
                List<Tag> tagList = (List<Tag>) ret.get("data");
                tagList.forEach(tag -> spdStockTagList.add(tag.getSpdCode()));
            }
        }

        //查询本地库存记录
        List<String> stockTagSpdCodeList = getStockTagList(Kv.by("deptId", cond.getStr("deptId"))).stream().map(record -> record.getStr("spdCode")).collect(Collectors.toList());

        Kv spdCodeKv = Kv.create();

        //过滤已归还记录，排除重复出库记录
        List<Record> recordList = getTagOutRecordList(cond).stream().filter(record -> {
            String spdCode = record.getStr("spdCode");

            //SPD库存找不到该耗材，认为已消耗
            if (spdFlag && !spdStockTagList.contains(spdCode)) return false;

            //本地库存找到该耗材，认为已归还
            if (stockTagSpdCodeList.contains(spdCode)) return false;

            //检测到重复出库记录，进行过滤
            if (spdCodeKv.containsKey(spdCode)) return false;

            spdCodeKv.set(spdCode, null);

            return true;
        }).collect(Collectors.toList());


        //结果统计
        List<Kv> kvList = new ArrayList<>();

        switch (type) {
            case 1:
                //按科室分组
                Map<String, List<Record>> groupByDept = recordList.stream().collect(Collectors.groupingBy(record -> cacheService.getCabinetById(record.getStr("cabinetId")).getDeptId()));
                groupByDept.forEach((k, list) -> {

                    //按近效期排序
                    list.sort(Comparator.comparing(a -> a.getStr("expireDate")));

                    Kv kv = Kv.create();
                    kv.set("deptId", k);
                    kv.set("recordCount", list.size());
                    kv.set("recordList", list);
                    kvList.add(kv);
                });
                break;
            case 2:
                //按柜体分组
                Map<String, List<Record>> groupByCabinet = recordList.stream().collect(Collectors.groupingBy(record -> record.getStr("cabinetId")));
                groupByCabinet.forEach((k, list) -> {

                    //按近效期排序
                    list.sort(Comparator.comparing(a -> a.getStr("expireDate")));

                    Kv kv = Kv.create();
                    kv.set("cabinetId", k);
                    kv.set("recordCount", list.size());
                    kv.set("recordList", list);
                    kvList.add(kv);
                });
                break;
            case 3:
                //按人员分组
                Map<String, List<Record>> groupByUser = recordList.stream().collect(Collectors.groupingBy(record -> {
                    String createUserId = record.getStr("createUserId");
                    if (StringUtils.isEmpty(createUserId)) {
                        createUserId = getDefaultUserId();
                    }
                    return createUserId;
                }));
                groupByUser.forEach((k, list) -> {

                    //按近效期排序
                    list.sort(Comparator.comparing(a -> a.getStr("expireDate")));

                    Kv kv = Kv.create();
                    kv.set("createUserId", k);
                    kv.set("recordCount", list.size());
                    kv.set("recordList", list);
                    kvList.add(kv);
                });
                break;
        }

        return kvList;
    }

    /**
     * 查询耗材历史记录
     *
     * @param spdCode SPD唯一码
     * @return
     */
    public R getTagHistory(String spdCode) {

        //查询SPD耗材同步记录
        Material material = cacheService.getMaterialById(spdCode);
        if (material == null) return R.error(ResultCode.DATA_NOT_EXISTED);

        List<Kv> kvs = new ArrayList<>();
        kvs.add(Kv.by("module", "SPD同步制标").set("createUserId", "").set("createDate", material.getCreateDate()));

        //查询入柜记录
        List<RecordIn> recordInList = RecordIn.dao.find("select * from com_record_in where spdCode= '" + spdCode + "'");
        recordInList.forEach(record -> {
            Cabinet cabinet = cacheService.getCabinetById(record.getCabinetId());
            kvs.add(Kv.by("module", cabinet.getName() + (record.getType().equals(SysConstant.TagNo) ? "收货" : "入柜")).set("createUserId", record.getCreateUserId()).set("createDate", record.getCreateDate()));
        });

        //查询出柜记录
        List<RecordOut> recordOutList = RecordOut.dao.find("select * from com_record_out where spdCode= '" + spdCode + "'");
        recordOutList.forEach(record -> {
            Cabinet cabinet = cacheService.getCabinetById(record.getCabinetId());
            kvs.add(Kv.by("module", cabinet.getName() + "出柜").set("createUserId", record.getCreateUserId()).set("createDate", record.getCreateDate()));
        });

        //查询标签绑定EPC记录
        List<LogOperate> logOperateList = LogOperate.dao.find("select * from sys_log_operate where module ='现有标签绑定EPC' and state='success' and parameter like '%" + spdCode + "%'");
        logOperateList.forEach(record -> {
            kvs.add(Kv.by("module", "标签绑定EPC").set("createUserId", record.getCreateUserId()).set("createDate", record.getCreateDate()));
        });

        //查询标签解绑EPC记录
        Tag tag = cacheService.getTagById(spdCode);
        if (tag != null && StringUtils.isEmpty(tag.getEpc())) {
            kvs.add(Kv.by("module", "标签解除EPC").set("createUserId", tag.getCreateUserId()).set("createDate", tag.getCreateDate()));
        }

        //日志按时间倒序
        List<Kv> kvList = kvs.stream().sorted(Comparator.comparing((Kv kv) -> kv.getStr("createDate")).reversed()).collect(Collectors.toList());
        return R.ok().putData(kvList);
    }

    /**
     * 异常库存耗材手动出库
     *
     * @param spdCode 耗材唯一码，逗号分隔
     * @return
     */
    public R removeStockTag(String spdCode) {
        QueryCondition condition = QueryConditionBuilder.by(Kv.by("spdCode", spdCode), true).put(SysConstant.SQL_PATTERN_IN, "spdCode", "stock.spdCode").build();

        String select = "select stock.*,tag.epc from com_stock_tag stock left join com_tag tag on stock.spdCode=tag.spdCode ";
        List<Record> stockTagList = Db.find(select + condition.getSql(), condition.getParas());

        List<String> noEpcTagList = new ArrayList<>();

        Map<String, List<String>> tagMap = new HashMap<>();
        stockTagList.forEach(tag -> {
            String id = tag.getStr("spdCode");
            String epc = tag.getStr("epc");
            String cabinetId = tag.getStr("cabinetId");

            //清除缓存
            cacheService.removeCache(CacheCom, TagByEpc, epc);
            cacheService.removeCache(CacheCom, TagById, id);

            if (StringUtils.isEmpty(epc)) {
                noEpcTagList.add(epc);
                return;
            }
            if (!tagMap.containsKey(cabinetId)) {
                tagMap.put(cabinetId, new ArrayList<>());
            }
            tagMap.get(cabinetId).add(epc);
        });

        //未绑定EPC直接删除库存
        noEpcTagList.forEach(StockTag.dao::deleteById);

        //执行出库操作
        tagMap.forEach((cabinetId, epcList) -> {
            saveTagOut(cabinetId, getDefaultUserId(), epcList.toArray(new String[epcList.size()]), DateUtil.now(), "异常库存耗材手动出库", "false", "");
        });

        return R.ok();
    }

    public Page<Record> getTagInventoryNoStock(int pageNumber, int pageSize) {
        String select = "select * ";
        String sqlExceptSelect = "from (select createDate,count(createDate) num from com_record_inventory_new GROUP BY createDate order by createDate desc ) a";
        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect);
    }

    public Map queryNoStock(String epc) {
        String[] epcArray = epc.split(",");
        // 非注册标签过滤
        List<Tag> filterComTagList = filterTag(epcArray, StockNone, null);
        Set<String> spdStockSet = SpdStockTagContainer.getAll();
        // spd库存过滤
        List<Tag> filterSpdList = filterComTagList.stream().filter(tag -> spdStockSet.contains(tag.getSpdCode())).collect(Collectors.toList());
        // goodsId分组
        Map<String, List<Tag>> collect = filterSpdList.stream().collect(Collectors.groupingBy(Tag::getGoodsId));
        // 组装结果
        List<Map> data = new ArrayList<>();
        collect.forEach((goodsId, tagList) -> {
            Goods goods = cacheService.getGoodsById(goodsId);
            Kv row = Kv.by("goodsName", goods.getName()).set("spec", goods.getSpec()).set("num", tagList.size());
            data.add(row);
        });
        return Kv.by("readNum", epcArray.length).set("tagFilterNum", filterComTagList.size()).set("spdFilterNum", filterSpdList.size()).set("data", data);
    }
}