package com.tsj.web.controller;

import cn.hutool.core.date.DateUtil;
import com.google.common.collect.Lists;
import com.jfinal.aop.Before;
import com.jfinal.aop.Inject;
import com.jfinal.ext.interceptor.GET;
import com.jfinal.ext.interceptor.POST;
import com.jfinal.kit.Kv;
import com.jfinal.upload.UploadFile;
import com.tsj.common.annotation.NotNull;
import com.tsj.common.annotation.OperateLog;
import com.tsj.common.constant.FileConstant;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.constant.SysConstant;
import com.tsj.common.utils.ExcelKit;
import com.tsj.common.utils.R;
import com.tsj.service.BaseService;
import com.tsj.service.CacheService;
import com.tsj.service.ComService;
import com.tsj.service.FileService;
import com.tsj.web.common.MyController;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * @className: ApiFileController
 * @description: 文件上传下载控制器
 * @author: Frank
 * @create: 2020-03-24 10:49
 */
public class FileController extends MyController {

    @Inject
    private BaseService baseService;

    @Inject
    private FileService fileService;

    @Inject
    private ComService comService;

    @Inject
    private CacheService cacheService;

    /******************************数据导入******************************/

    @Before(POST.class)
    @OperateLog("导入科室列表")
    public void uploadDeptList(UploadFile file) throws Exception {
        List<String[]> list = ExcelKit.getObjectListFromExcel(file, 1, 4, 0, 1);
        R result = fileService.saveDeptList(getLoginUserId(), DateUtil.now(), list);
        renderJson(result);
    }

    @Before(POST.class)
    @OperateLog("导入用户列表")
    public void uploadUserList(UploadFile file) throws Exception {
        List<String[]> list = ExcelKit.getObjectListFromExcel(file, 1, 4, 0, 1);
        R result = fileService.saveUserList(getLoginUserId(), DateUtil.now(), list);
        renderJson(result);
    }

    @Before(POST.class)
    @OperateLog("导入高值柜列表")
    public void uploadCabinetList(UploadFile file) throws Exception {
        List<String[]> list = ExcelKit.getObjectListFromExcel(file, 1, 5, 0, 1, 2, 3);
        R result = fileService.saveCabinetList(getLoginUserId(), DateUtil.now(), list);
        renderJson(result);
    }

    @Before(POST.class)
    @OperateLog("导入生产商列表")
    public void uploadManufacturerList(UploadFile file) throws Exception {
        List<String[]> list = ExcelKit.getObjectListFromExcel(file, 1, 6, 0, 1);
        R result = fileService.saveManufacturerList(getLoginUserId(), DateUtil.now(), list);
        renderJson(result);
    }

    @Before(POST.class)
    @OperateLog("导入供应商列表")
    public void uploadSupplierList(UploadFile file) throws Exception {
        List<String[]> list = ExcelKit.getObjectListFromExcel(file, 1, 6, 0, 1);
        R result = fileService.saveSupplierList(getLoginUserId(), DateUtil.now(), list);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"userId"})
    @OperateLog("导入耗材列表")
    public void uploadGoodsList(UploadFile file) throws Exception {
        List<String[]> list = ExcelKit.getObjectListFromExcel(file, 1, 10, 0, 3, 4);
        R result = fileService.saveGoodsList(getLoginUserId(), DateUtil.now(), list);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"userId"})
    @OperateLog("导入库存基数列表")
    public void uploadStockBaseList(UploadFile file) throws Exception {
        List<String[]> list = ExcelKit.getObjectListFromExcel(file, 1, 4);
        R result = fileService.saveStockBaseList(getLoginUserId(), DateUtil.now(), list);
        renderJson(result);
    }

    @Before(GET.class)
    @NotNull("temp")
    @OperateLog(value = "下载导入模板")
    public void downloadTemplate(String temp) {
        String fileName = null;
        switch (temp) {
            case "dept":
                fileName = SysConstant.TEMPLATE_Dept;
                break;

            case "user":
                fileName = SysConstant.TEMPLATE_User;
                break;

            case "cabinet":
                fileName = SysConstant.TEMPLATE_Cabinet;
                break;

            case "manufacturer":
                fileName = SysConstant.TEMPLATE_Manufacturer;
                break;

            case "supplier":
                fileName = SysConstant.TEMPLATE_Supplier;
                break;

            case "goods":
                fileName = SysConstant.TEMPLATE_Goods;
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + temp);
        }
        renderFile(new File(FileConstant.TEMPLATE_PATH + fileName));
    }

    /******************************客户端在线升级******************************/

    @Before(POST.class)
    @NotNull("ServerDirectoryPath")
    @OperateLog("导入升级文件")
    public void uploadFile(String ServerDirectoryPath, UploadFile file) {
        if (file == null) {
            renderJson(R.error(ResultCode.FILE_UPLOAD_FAIL));
            return;
        }

        File newFilePath = new File(FileConstant.APP_UPGRADE_PATH, ServerDirectoryPath + "/" + file.getOriginalFileName());
        if (newFilePath.exists()) {
            newFilePath.delete();
        }

        R result = file.getFile().renameTo(newFilePath) ? R.ok() : R.error(ResultCode.FILE_UPLOAD_FAIL);
        renderJson(result);
    }

    @Before(GET.class)
    @NotNull("ServerDirectoryPath")
    @OperateLog("查询升级文件列表")
    public void getFileList(String ServerDirectoryPath) {
        File file = new File(FileConstant.APP_UPGRADE_PATH, ServerDirectoryPath);
        if (file.exists()) {
            List<Kv> kvList = Lists.newArrayList();
            Arrays.stream(file.listFiles())
                    .sorted(Comparator.comparing(File::lastModified).reversed())
                    .forEach(listFile -> {
                        kvList.add(Kv.by("fileName", listFile.getName())
                                .set("fileDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(listFile.lastModified())))
                                .set("fileSize", String.format("%.2f", listFile.length() / (1024 * 1024 * 1.0)) + "MB (" + listFile.length() + "字节)"));
                    });
            renderJson(R.ok().putData(kvList));
        } else {
            renderJson(R.error(ResultCode.FILE_NOT_EXISTED));
        }
    }

    @Before(POST.class)
    @NotNull({"ServerDirectoryPath", "fileName"})
    @OperateLog("删除升级文件")
    public void deleteFile(String ServerDirectoryPath, String fileName) {
        File file = new File(FileConstant.APP_UPGRADE_PATH, ServerDirectoryPath + "/" + fileName);
        if (file.exists()) {
            file.delete();
            renderJson(R.ok());
        } else {
            renderJson(R.error(ResultCode.FILE_NOT_EXISTED));
        }
    }

    @Before(GET.class)
    @NotNull({"ServerDirectoryPath", "fileName"})
    @OperateLog("下载升级文件")
    public void downloadFile(String ServerDirectoryPath, String fileName) {
        File file = new File(FileConstant.APP_UPGRADE_PATH, ServerDirectoryPath + "/" + fileName);
        if (file.exists()) {
            renderFile(file);
        } else {
            renderJson(R.error(ResultCode.FILE_NOT_EXISTED));
        }
    }

    @Before(GET.class)
    @NotNull({"cabinetId", "createDate"})
    @OperateLog("查询高值柜操作图片")
    public void getRecordImage(String cabinetId, String createDate) {
        String imagePath = FileConstant.CABINET_PATH + createDate.substring(0, 10)
                + "/" + cabinetId + createDate.substring(11).replace(":", "") + ".png";
        if (!new File(imagePath).exists()) {
            renderJson(R.error(ResultCode.FILE_NOT_EXISTED));
        } else {
            renderFile(new File(imagePath));
        }
    }

    /******************************数据导出******************************/

    @Before(GET.class)
    @NotNull({"deptId"})
    @OperateLog("导出制标记录")
    public void downloadTagList(String accept, String deptId, String goodsId, String dateRange, String epc, String spdCode) throws Exception {
        Kv cond = Kv.create();
        cond.set("accept", accept);
        cond.set("epc", epc);
        cond.set("deptId", deptId);
        cond.set("spdCode", spdCode);
        cond.set("goodsId", goodsId);
        cond.set("dateRange", dateRange);

        File file = fileService.getTagListToFile(cond);
        if (!file.exists()) {
            logger.error("导出制标记录失败");
            renderJson(R.error(ResultCode.FILE_CREATE_FAIL));
        } else {
            renderFile(file);
        }
    }

    @Before(GET.class)
    @NotNull({"deptId", "type"})
    @OperateLog("导出出入柜记录")
    public void downloadTagInOutList(String deptId, String dateRange, String type) throws Exception {
        Kv cond = Kv.create();
        cond.set("deptId", deptId);
        cond.set("dateRange", dateRange);
        cond.set("type", type);

        File file = fileService.getTagInOutToFile(cond);
        if (!file.exists()) {
            logger.error("导出出入柜记录失败，高值柜ID为:%s", deptId);
            renderJson(R.error(ResultCode.FILE_CREATE_FAIL));
        } else {
            renderFile(file);
        }
    }

    @Before(GET.class)
    @NotNull({"deptId", "type"})
    @OperateLog("导出出入柜明细记录")
    public void downloadTagInOutRecordList(String deptId, String goodsId, String dateRange, String type, String spdCode) throws Exception {
        if (StringUtils.isEmpty(type)) {
            type = "tagOut";
        }

        Kv cond = Kv.by("deptId", deptId).set("goodsId", goodsId).set("dateRange", dateRange).set("spdCode", spdCode);
        File file = fileService.getTagInOutRecordToFile(cond, type);
        if (!file.exists()) {
            logger.error("导出出入柜记录失败，高值柜ID为:%s", deptId);
            renderJson(R.error(ResultCode.FILE_CREATE_FAIL));
        } else {
            renderFile(file);
        }
    }

    @Before(GET.class)
    @NotNull({"deptId"})
    @OperateLog("导出库存记录")
    public void downloadStockTagList(String deptId, String goodsName, String cabinetName) throws Exception {
        Kv cond = Kv.by("a.deptId", deptId).set("c.name", goodsName).set("b.name", cabinetName);
        File file = fileService.getTagStockToFile(cond);
        if (!file.exists()) {
            logger.error("导出库存记录失败，高值柜ID为:%s", deptId);
            renderJson(R.error(ResultCode.FILE_CREATE_FAIL));
        } else {
            renderFile(file);
        }
    }

    @Before(GET.class)
    @NotNull({"inventoryDifferenceId"})
    @OperateLog("导出盘点记录")
    public void downloadInventory(String inventoryDifferenceId) throws Exception {
        File file = fileService.downloadInventory(inventoryDifferenceId);
        if (!file.exists()) {
            logger.error("导出库存记录失败");
            renderJson(R.error(ResultCode.FILE_CREATE_FAIL));
        } else {
            renderFile(file);
        }
    }

    @Before(GET.class)
    @NotNull({"createDate"})
    @OperateLog("导出无库存盘点记录")
    public void downloadRecordInventoryNewList(String createDate) throws Exception {
        File file = fileService.getRecordInventoryNewFile(createDate);
        if (!file.exists()) {
            logger.error("导出库存记录失败");
            renderJson(R.error(ResultCode.FILE_CREATE_FAIL));
        } else {
            renderFile(file);
        }
    }


    @Before(GET.class)
    @NotNull({"deptId"})
    @OperateLog("导出库存明细记录")
    public void downloadStockTagRecordList(String deptId, String goodsId, String cabinetName, String type, String spdCode) throws Exception {
        Kv cond = Kv.by("deptId", deptId).set("goodsId", goodsId).set("cabinetName", cabinetName).set("type", type).set("spdCode", spdCode);

        File file = fileService.getTagStockRecordToFile(cond);
        if (!file.exists()) {
            logger.error("导出库存记录失败，高值柜ID为:%s", deptId);
            renderJson(R.error(ResultCode.FILE_CREATE_FAIL));
        } else {
            renderFile(file);
        }
    }
}