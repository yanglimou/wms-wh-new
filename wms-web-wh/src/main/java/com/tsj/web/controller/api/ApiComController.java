package com.tsj.web.controller.api;

import com.jfinal.aop.Before;
import com.jfinal.aop.Clear;
import com.jfinal.aop.Inject;
import com.jfinal.ext.interceptor.GET;
import com.jfinal.ext.interceptor.POST;
import com.jfinal.kit.Kv;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.upload.UploadFile;
import com.tsj.common.annotation.IgnoreParameter;
import com.tsj.common.annotation.NotNull;
import com.tsj.common.annotation.OperateLog;
import com.tsj.common.config.CommonConfig;
import com.tsj.common.constant.FileConstant;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.constant.SysConstant;
import com.tsj.common.utils.FileKit;
import com.tsj.common.utils.R;
import com.tsj.domain.model.Cabinet;
import com.tsj.domain.model.Order;
import com.tsj.domain.model.Tag;
import com.tsj.service.CacheService;
import com.tsj.service.ComService;
import com.tsj.service.SpdService;
import com.tsj.service.interceptor.AuthInterceptor;
import com.tsj.web.common.MyController;
import com.tsj.web.hikvision.HikVision;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @className: ApiComController
 * @description: 业务数据控制器
 * @author: Frank
 * @create: 2020-03-24 10:49
 */
@Clear(AuthInterceptor.class)
public class ApiComController extends MyController {

    @Inject
    private CacheService cacheService;

    @Inject
    private ComService comService;

    @Inject
    private SpdService spdService;

    @Before(POST.class)
    @NotNull({"cabinetId", "time", "epc", "confirmed"})
    @OperateLog("保存入柜记录")
    public void saveTagIn(String epc, String cabinetId, String userId, String confirmed, String time, String exceptionDesc, UploadFile file) {
        if (file != null) {
            List<String> images = FileKit.moveFile(file, FileConstant.CABINET_PATH, time, cabinetId);
            if (images.size() == 0) {
                renderJson(R.error(ResultCode.FILE_CREATE_FAIL));
                return;
            }
        }

        R result = comService.saveTagIn(cabinetId, userId, epc.split(","), time, exceptionDesc, confirmed.toLowerCase());
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"cabinetId", "time", "epc", "confirmed"})
    @OperateLog("保存出柜记录")
    public void saveTagOut(String epc, String cabinetId, String userId, String confirmed, String time, String exceptionDesc, String patientNo, UploadFile file) {
        if (file != null) {
            List<String> imageList = FileKit.moveFile(file, FileConstant.CABINET_PATH, time, cabinetId);
            if (imageList.size() == 0) {
                renderJson(R.error(ResultCode.FILE_CREATE_FAIL));
                return;
            }
        }

        R result = comService.saveTagOut(cabinetId, userId, epc.split(","), time, exceptionDesc, confirmed.toLowerCase(), patientNo);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"cabinetId", "time", "userId"})
    @OperateLog("保存盘点记录")
    public void saveTagInventory(String epc, String cabinetId, String userId, String time,
                                 @IgnoreParameter UploadFile file) {

        if (StringUtils.isEmpty(epc))
            epc = "";

        R result = comService.saveTagInventory(cabinetId, userId, epc.split(","), time);
        renderJson(result);
    }


    @Before(POST.class)
    @NotNull({"time", "userId"})
    @OperateLog("保存盘点记录new")
    public void saveTagInventoryNew(String epc, String cabinetId, String userId, String time,
                                    @IgnoreParameter UploadFile file) {
        if (StringUtils.isEmpty(epc))
            epc = "";
        R result = comService.saveTagInventoryNew(cabinetId, userId, epc.split(","), time);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"goodsId", "batchNo", "expireDate", "epc", "userId", "deptId", "orderCode"})
    @OperateLog("保存注册标签")
    public void saveTag(String goodsId, String batchNo, String expireDate, String epc, String userId, String deptId, String orderCode,
                        @IgnoreParameter UploadFile file) {
        R result = comService.saveTag(goodsId, batchNo, expireDate, epc, userId, deptId, orderCode);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"spdCode", "epc", "userId"})
    @OperateLog("现有标签绑定EPC")
    public void saveTagEpc(String spdCode, String epc, String userId,
                           @IgnoreParameter UploadFile file) {
        R result = comService.saveTagEpc(spdCode, epc, userId);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"orderCode", "userId"})
    @OperateLog("现有标签批量解绑EPC")
    public void deleteTagEpcByOrder(String orderCode, String userId,
                                    @IgnoreParameter UploadFile file) {
        R result = comService.deleteTagEpcByOrder(orderCode, userId);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"epc", "userId"})
    @OperateLog("现有标签解绑EPC")
    public void deleteTagEpc(String epc, String userId,
                             @IgnoreParameter UploadFile file) {
        R result = comService.deleteTagEpc(epc, userId);
        renderJson(result);
    }

    @Before(GET.class)
    @NotNull("cabinetId")
    @OperateLog("查询库存数量列表")
    public void getStockQuantityList(String cabinetId) {
        Kv cond = Kv.by("cabinetId", cabinetId);

        List<Record> recordList = comService.getStockQuantityList(cond);
        renderJson(R.ok().putData(recordList));
    }

    @Before(GET.class)
    @NotNull("cabinetId")
    @OperateLog("查询库存标签列表")
    public void getStockTagList(String cabinetId) {
        Kv cond = Kv.by("cabinetId", cabinetId);
        List<Record> recordList = comService.getStockTagList(cond);

        renderJson(R.ok().putData(recordList));
    }

    @Before(GET.class)
    @NotNull("cabinetId")
    @OperateLog("查询SPD库存标签列表")
    public void getStockTagListBySPD(String cabinetId) {
        Cabinet cabinet = cacheService.getCabinetById(cabinetId);
        if (cabinet == null) {
            renderJson(R.error(ResultCode.DATA_NOT_EXISTED));
            return;
        }

        R ret = CommonConfig.prop.getBoolean("spd") ?
                spdService.postStockTagList(cabinet.getDeptId()) : comService.getStockTagList(cabinet.getDeptId());
        renderJson(ret);
    }

    @Before(GET.class)
    @NotNull("deptId")
    @OperateLog("查询科室库存标签列表")
    public void getStockTagListByDept(String deptId) {
        Kv cond = Kv.by("deptId", deptId);
        List<Record> recordList = comService.getStockTagList(cond);

        renderJson(R.ok().putData(recordList));
    }

    @Before(GET.class)
    @NotNull("deptId")
    @OperateLog("查询库存基数列表")
    public void getStockBaseList(String deptId) {
        Kv cond = Kv.by("deptId", deptId);

        List<Record> recordList = comService.getStockBaseList(cond);
        renderJson(R.ok().putData(recordList));
    }

    @Before(GET.class)
    @NotNull("deptId")
    @OperateLog("查询库存缺货列表")
    public void getShortageList(String deptId, String goodsId, String manufacturerId, String supplierId) {
        Kv cond = Kv.by("deptId", deptId)
                .set("goodsId", goodsId)
                .set("manufacturerId", manufacturerId)
                .set("supplierId", supplierId);

        List<Record> recordList = comService.getShortageList(cond);
        renderJson(R.ok().putData(recordList));
    }

    @Before(GET.class)
    @NotNull({"deptId", "dateRange"})
    @OperateLog("查询收货数量列表")
    public void getTagInQuantityList(String deptId, String dateRange) {
        Kv cond = Kv.by("deptId", deptId)
                .set("dateRange", dateRange);

        List<Record> recordList = comService.getTagInQuantityList(cond);
        renderJson(R.ok().putData(recordList));
    }

    @Before(GET.class)
    @NotNull({"deptId", "dateRange"})
    @OperateLog("查询收货记录列表")
    public void getTagInRecordList(String deptId, String dateRange) {
        Kv cond = Kv.by("deptId", deptId)
                .set("dateRange", dateRange)
                .set("type", SysConstant.TagNo);

        List<Record> recordList = comService.getTagInRecordList(cond);
        renderJson(R.ok().putData(recordList));
    }

    @Before(GET.class)
    @NotNull({"deptId", "dateRange"})
    @OperateLog("查询出柜未归还记录列表")
    public void getTagOutRecordList(String deptId, String dateRange) {
        Kv cond = Kv.by("deptId", deptId)
                .set("dateRange", dateRange);

        List<Kv> kvList = comService.getTagOutWithoutReturnRecordList(cond);
        renderJson(R.ok().putData(kvList));
    }

    @Before(GET.class)
    @NotNull({"deptId", "dateRange"})
    @OperateLog("查询盘点数量列表")
    public void getTagInventoryQuantityList(String dateRange, String cabinetId) {
        Kv cond = Kv.by("cabinetId", cabinetId)
                .set("dateRange", dateRange);

        List<Record> recordList = comService.getTagInventoryRecordList(cond);
        renderJson(R.ok().putData(recordList));
    }

    @Before(GET.class)
    @NotNull({"deptId", "dateRange"})
    @OperateLog("查询盘点标签列表")
    public void getTagInventoryList(String deptId, String dateRange) {
        Kv cond = Kv.by("deptId", deptId)
                .set("dateRange", dateRange);

        List<Record> recordList = comService.getTagInventoryRecordList(cond);
        renderJson(R.ok().putData(recordList));
    }


    @Before(GET.class)
    @NotNull("epc")
    @OperateLog("查询标签，根据EPC")
    public void getTag(String epc) {
        String[] epcArray = epc.split(",");
        List<Tag> tagList = null;
        if (epcArray[0].length() == 24) {
            tagList = comService.getTagListByEpc(epcArray);
        } else {
            tagList = comService.getTagListByEpcFuzzy(epcArray);
        }
        renderJson(R.ok().putData(tagList));
    }

    @Before(GET.class)
    @NotNull("epc")
    @OperateLog("无库存盘点查询")
    public void queryNoStock(String epc) {
        Map map = comService.queryNoStock(epc);
        renderJson(R.ok().putData(map));
    }

    @Before(GET.class)
    @NotNull("spdCode")
    @OperateLog("查询标签，根据唯一码")
    public void getTagBySpdCode(String spdCode) {
        List<Tag> tagList = comService.getTagListBySpdCode(spdCode.split(","));
        if (tagList.size() > 0) {
            renderJson(R.ok().putData(tagList));
        } else {
            renderJson(R.error(ResultCode.DATA_NOT_EXISTED, "标签信息未找到，请在SPD系统检查耗材入柜状态，5分钟后重试"));
        }
    }

    @Before(GET.class)
    @NotNull("dateRange")
    @OperateLog("查询注册标签记录")
    public void getSaveTagList(String dateRange, String deptId) {
        Kv cond = Kv.by("dateRange", dateRange)
                .set("deptId", deptId);

        List<Record> tagList = comService.getSaveTagList(cond);
        renderJson(R.ok().putData(tagList));
    }

    @Before(GET.class)
    @NotNull({"cabinetId"})
    @OperateLog("查询最近的盘点记录")
    public void getLastTagInventoryRecord(String cabinetId) {
        Kv cond = Kv.by("cabinetId", cabinetId);

        Record record = comService.getTagInventoryRecordDifferenceList(cond);
        renderJson(R.ok().putData(record));
    }

    @Before(GET.class)
    @NotNull({"inventoryDifferenceId"})
    @OperateLog("查询盘点记录详情")
    public void getTagInventoryDetail(String inventoryDifferenceId) {
        Kv cond = Kv.by("inventoryDifferenceId", inventoryDifferenceId);

        List<Record> recordList = comService.getTagInventoryRecordDifferenceDetail(cond);
        renderJson(R.ok().putData(recordList));
    }

    @Before(GET.class)
    @NotNull({"deptId"})
    @OperateLog(value = "查询配送单")
    public void getOrderList(String deptId) {
        List<Order> orderList = comService.getUncompletedOrderList(deptId);
        renderJson(R.ok().putData(orderList));
    }

    @Before(GET.class)
    @NotNull({"orderCode"})
    @OperateLog("查询配送单明细")
    public void getTagList(String orderCode) {
        R result = comService.getTagList(orderCode);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"orderCode"})
    @OperateLog("复核配送单")
    public void checkOrder(String orderCode,
                           @IgnoreParameter UploadFile file) {
        R result = comService.confirmOrder(orderCode);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"cabinetId"})
    @OperateLog("开始摄像头录像")
    public void startRecord(String cabinetId,
                            @IgnoreParameter UploadFile file) {
        List<String> addressList = CommonConfig.getCabinetCameraAddressList(cabinetId);
        addressList.forEach(address -> HikVision.SaveRealDataformDir(address, FileConstant.VIDEO_PATH + cabinetId));
        renderJson(addressList.size() > 0 ? R.ok() : R.error(ResultCode.CAMERA_NOT_CONFIG));
    }

    @Before(POST.class)
    @NotNull({"cabinetId"})
    @OperateLog("停止摄像头录像")
    public void stopRecord(String cabinetId,
                           @IgnoreParameter UploadFile file) {
        List<String> addressList = CommonConfig.getCabinetCameraAddressList(cabinetId);
        addressList.forEach(HikVision::StopSaveRealData);
        renderJson(R.ok());
    }

    @Before(POST.class)
    @NotNull({"cabinetId", "ip", "time"})
    @OperateLog("上传摄像头录像")
    public void uploadRecord(String cabinetId, String ip, String time, UploadFile file) {
        if (file == null) {
            renderJson(R.error(ResultCode.FILE_NOT_EXISTED));
            return;
        }

        // 判断是否MP4格式
        if (!file.getFileName().toLowerCase().endsWith(".mp4")) {
            renderJson(R.error(ResultCode.FILE_IS_WRONG));
            return;
        }

        boolean res = FileKit.moveFile(file, FileConstant.VIDEO_PATH + cabinetId, ip + "_" + time.replaceAll(":", "-").replaceAll(" ", "-"));
        if (!res) {
            renderJson(R.error(ResultCode.FILE_CREATE_FAIL));
            return;
        }
        renderJson(R.ok());
    }

    @Before(POST.class)
    @NotNull({"cabinetId", "time"})
    @OperateLog("上传日志文件")
    public void uploadLogFile(String cabinetId, String time, UploadFile file) {
        if (file == null) {
            renderJson(R.error(ResultCode.FILE_NOT_EXISTED));
            return;
        }

        // 判断是否LOG格式
        if (!file.getFileName().toLowerCase().endsWith(".log")) {
            renderJson(R.error(ResultCode.FILE_IS_WRONG));
            return;
        }

        boolean res = FileKit.moveFile(file, FileConstant.LOG_PATH + cabinetId, time.replaceAll(":", "-").replaceAll(" ", "-"));
        if (!res) {
            renderJson(R.error(ResultCode.FILE_CREATE_FAIL));
            return;
        }
        renderJson(R.ok());
    }
}