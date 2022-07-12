package com.tsj.web.controller.api;

import cn.hutool.core.date.DateUtil;
import com.jfinal.aop.Before;
import com.jfinal.aop.Clear;
import com.jfinal.aop.Inject;
import com.jfinal.ext.interceptor.GET;
import com.jfinal.ext.interceptor.POST;
import com.jfinal.kit.Kv;
import com.jfinal.upload.UploadFile;
import com.tsj.common.annotation.IgnoreParameter;
import com.tsj.common.annotation.NotNull;
import com.tsj.common.annotation.OperateLog;
import com.tsj.common.constant.FileConstant;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.utils.FileKit;
import com.tsj.common.utils.R;
import com.tsj.domain.model.Cabinet;
import com.tsj.domain.model.Config;
import com.tsj.domain.model.Dept;
import com.tsj.service.BaseService;
import com.tsj.service.CacheService;
import com.tsj.service.SysService;
import com.tsj.service.interceptor.AuthInterceptor;
import com.tsj.web.common.MyController;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @className: ApiSysController
 * @description: 系统数据控制器
 * @author: Frank
 * @create: 2020-03-24 10:49
 */
@Clear(AuthInterceptor.class)
public class ApiSysController extends MyController {

    @Inject
    private SysService sysService;

    @Inject
    private BaseService baseService;

    @Inject
    private CacheService cacheService;

    @Before(POST.class)
    @NotNull({"cabinetId", "userId", "time"})
    @OperateLog("保存登录记录")
    public void saveAccess(String cabinetId, String userId, String time,
                           @IgnoreParameter UploadFile file) {
        if (file != null) {
            List<String> imageList = FileKit.moveFile(file, FileConstant.CABINET_PATH, time, cabinetId);
            if (imageList.size() == 0) {
                renderJson(R.error(ResultCode.FILE_CREATE_FAIL));
                return;
            }
        }

        R result = sysService.saveLogLogin(userId.split(","), cabinetId, time);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"password"})
    @OperateLog("重置系统，数据不可恢复")
    public void reset(String password,
                      @IgnoreParameter UploadFile file) {
        R result = sysService.reset(password);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"username", "password"})
    @OperateLog("用户登录")
    public void login(String username, String password,
                      @IgnoreParameter UploadFile file) {
        R result = sysService.getUser(username, password);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"userId", "fingerUserId", "fingerNo", "fingerValue"})
    @OperateLog("保存用户指纹")
    public void saveUserFinger(String userId, String fingerUserId, Integer fingerNo, String fingerValue,
                               @IgnoreParameter UploadFile file) {
        if (fingerNo < 1 || fingerNo > 710) {
            renderJson(R.error(ResultCode.DATA_IS_INVALID, "指纹编号只能是1-710"));
            return;
        }
        fingerValue = fingerValue.substring(0, 386);
        R result = sysService.saveUserFinger(fingerUserId, fingerNo, fingerValue, userId, DateUtil.now());
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"userId", "fingerUserId"})
    @OperateLog("删除指纹信息")
    public void deleteUserFinger(String userId, String fingerUserId, String deptId,
                                 @IgnoreParameter UploadFile file) {
        R result = sysService.deleteUserFinger(userId, fingerUserId, deptId);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"userId"})
    @OperateLog("删除卡号信息")
    public void deleteUserCode(String userId, String codeUserId, String deptId,
                               @IgnoreParameter UploadFile file) {
        R result = sysService.deleteUserCode(userId, codeUserId, deptId);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"userId", "code"})
    @OperateLog("更新卡号信息")
    public void updateUserCode(String userId, String codeUserId, String code,
                               @IgnoreParameter UploadFile file) {
        R result = sysService.updateUserCode(userId, codeUserId, code);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"userId", "epcUserId", "epc"})
    @OperateLog("用户卡号绑定EPC")
    public void updateUserEpc(String userId, String epcUserId, String epc,
                              @IgnoreParameter UploadFile file) {
        R result = sysService.updateUserEpc(userId, epcUserId, epc);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"userId", "type", "name", "value"})
    @OperateLog("更新系统配置")
    public void updateConfig(String userId, String type, String name, String value,
                             @IgnoreParameter UploadFile file) {
        R result = sysService.updateConfig(userId, type, name, value);
        renderJson(result);
    }

    @Before(POST.class)
    @NotNull({"userId", "oldPassword", "newPassword"})
    @OperateLog("更新用户密码")
    public void updateUserPassword(String userId, String oldPassword, String newPassword,
                                   @IgnoreParameter UploadFile file) {
        R result = baseService.updatePassword(userId, oldPassword, newPassword);
        renderJson(result);
    }

    @Before(GET.class)
    @NotNull({"deptId"})
    @OperateLog("查询空闲指纹编号")
    public void getFreeUserFingerNo(String deptId) {
        int freeUserFingerNo = sysService.getFreeUserFingerNo(deptId);
        renderJson(R.ok().putData(Kv.by("freeUserFingerNo", freeUserFingerNo)));
    }

    @Before(GET.class)
    @OperateLog("查询系统配置列表")
    public void getConfigList() {
        List<Config> recordList = sysService.getConfigList();
        renderJson(R.ok().putData(recordList));
    }

    @Before(GET.class)
    @NotNull({"deptId"})
    @OperateLog("查询用户指纹列表")
    public void getUserFingerList(String deptId) {
        Kv kv = Kv.by("userFingerLastUpdateTime", sysService.getUserFingerLastUpdateTime(deptId))
                .set("userFingerList", sysService.getUserFingerList(deptId));
        renderJson(R.ok().putData(kv));
    }

    @Before(GET.class)
    @NotNull({"deptId"})
    @OperateLog("查询科室用户指纹列表")
    public void getUserFingerListByDeptId(String deptId) {
        renderJson(R.ok().putData(sysService.getUserFingerList(deptId)));
    }

    @Before(GET.class)
    @NotNull({"cabinetId", "time"})
    @OperateLog("查询高值柜拍照图片")
    public void getCabinetPhoto(String cabinetId, String time) {
        String filePath = sysService.getCabinetPhoto(cabinetId, time);
        File file = new File(filePath);
        if (file.exists()) {
            renderFile(file);
        } else {
            renderJson(R.error(ResultCode.FILE_NOT_EXISTED));
        }
    }

    @Before(GET.class)
    @NotNull({"deptName", "cabinetName"})
    @OperateLog("请求服务器更新信息")
    public void getServerInfo(String deptName, String cabinetName) {
        Dept dept = cacheService.getDeptByName(deptName);
        Cabinet cabinet = cacheService.getCabinetByName(cabinetName);
        if (dept == null || cabinet == null) {
            renderJson(R.error(ResultCode.DATA_NOT_EXISTED));
            return;
        }

        Kv kv = Kv.create();
        kv.set("configTime", sysService.getLastUpdateTime("sys_config"));
        kv.set("userTime", sysService.getLastUpdateTime("base_user"));
        kv.set("deptTime", sysService.getLastUpdateTime("base_dept"));
        kv.set("cabinetTime", sysService.getLastUpdateTime("base_cabinet"));
        kv.set("goodsTime", sysService.getLastUpdateTime("base_goods"));
        kv.set("supplierTime", sysService.getLastUpdateTime("base_supplier"));
        kv.set("manufacturerTime", sysService.getLastUpdateTime("base_manufacturer"));
        kv.set("userFingerTime", sysService.getUserFingerLastUpdateTime(dept.getId()));
        kv.set("tagStockTime", sysService.getTagStockLastUpdateTime(cabinet.getId()));

        renderJson(R.ok().putData(kv));
    }


    @Before(GET.class)
    @NotNull({"ServerDirectoryPath"})
    @OperateLog("请求服务器更新信息")
    public void getServerVersion(String ServerDirectoryPath) {
        File file = new File(FileConstant.APP_UPGRADE_PATH + ServerDirectoryPath + "/");
        if (!file.exists() || !file.isDirectory()) {
            renderJson(R.error(ResultCode.FILE_NOT_EXISTED));
        }

        List<Kv> kvList = new ArrayList<>();

        String[] tempList = file.list();
        if (tempList != null && tempList.length > 0) {
            for (String path : tempList) {
                File temp = new File(FileConstant.APP_UPGRADE_PATH + ServerDirectoryPath + "/" + path);
                kvList.add(Kv.by(temp.getName(),
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(temp.lastModified()))));
            }
        }

        renderJson(R.ok().putData(kvList));
    }

    @Before(GET.class)
    @NotNull({"ServerDirectoryPath", "name"})
    @OperateLog("请求服务器文件")
    public void getServerFile(String ServerDirectoryPath, String name) {
        File file = new File(FileConstant.APP_UPGRADE_PATH + ServerDirectoryPath + "/" + name);
        if (!file.exists()) {
            renderJson(R.error(ResultCode.FILE_NOT_EXISTED));
        }
        renderFile(file);
    }
}

