package com.tsj.web.hikvision;

import com.jfinal.log.Log;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;
import com.tsj.common.utils.DateUtils;
import com.tsj.service.BaseService;

import java.io.File;
import java.util.*;

/**
 * @author Frank
 */
public class HikVision {
    public static final Log logger = Log.getLog(HikVision.class);

    private static final Map<String, CameraInfo> cameraInfoMap = new HashMap<>();
    private static final Map<String, String> cameraRecordTime = new HashMap<>();

    /**
     * 录像最大时长，10分钟
     */
    private static final int delay = 600;
    private static final HCNetSDK sdk = HCNetSDK.INSTANCE;

    /**
     * 开启摄像头预览
     *
     * @param address  IP地址
     * @param port     端口号
     * @param userName 用户名
     * @param password 密码
     * @return
     */
    public static boolean RealPlay(String address, short port, String userName, String password) {
        // 判断摄像头是否重复
        if (cameraInfoMap.containsKey(address)) {
            return false;
        }

        CameraInfo cameraInfo = new CameraInfo();
        cameraInfo.setAddress(address);
        cameraInfo.setPort(port);
        cameraInfo.setUserName(userName);
        cameraInfo.setPwd(password);

        CameraInfo parameter = getParameter(cameraInfo);

        // 判断是否注册成功
        if (parameter.getUserId().intValue() < 0) {
            logger.info(address + "注册设备失败 错误码为: " + sdk.NET_DVR_GetLastError());
        } else {
            logger.info(address + "注册成功  Id为:      " + cameraInfo.getUserId().intValue());
        }

        // 判断是否获取到设备能力
        HCNetSDK.NET_DVR_WORKSTATE_V30 devWork = new HCNetSDK.NET_DVR_WORKSTATE_V30();
        if (!sdk.NET_DVR_GetDVRWorkState_V30(cameraInfo.getUserId(), devWork)) {
            logger.info("获取设备能力集失败,返回设备状态失败...............");
            return false;
        }

        // 录制前雨刷器先刷几圈
        boolean result = sdk.NET_DVR_PTZControl_Other(cameraInfo.getUserId(), cameraInfo.getChannel(), HCNetSDK.WIPER_PWRON, 0);
        boolean result1 = sdk.NET_DVR_PTZControl_Other(cameraInfo.getUserId(), cameraInfo.getChannel(), HCNetSDK.WIPER_PWRON, 0);
        boolean result2 = sdk.NET_DVR_PTZControl_Other(cameraInfo.getUserId(), cameraInfo.getChannel(), HCNetSDK.WIPER_PWRON, 1);

        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 启动实时预览功能  创建clientInfo对象赋值预览参数
        HCNetSDK.NET_DVR_CLIENTINFO clientInfo = new HCNetSDK.NET_DVR_CLIENTINFO();

        // 设置通道号
        clientInfo.lChannel = cameraInfo.getChannel();
        // TCP取流
        clientInfo.lLinkMode = new NativeLong(0);
        // 不启动多播模式
        clientInfo.sMultiCastIP = null;

        // 创建窗口句柄
        clientInfo.hPlayWnd = null;

        FRealDataCallBack fRealDataCallBack = new FRealDataCallBack();
        // 开启实时预览
//        NativeLong key = sdk.NET_DVR_RealPlay_V30(cameraInfo.getUserId(), clientInfo, fRealDataCallBack, null, false);
        NativeLong key = sdk.NET_DVR_RealPlay(cameraInfo.getUserId(), clientInfo);
        cameraInfo.setKey(key);
        // 判断是否预览成功
        if (key.intValue() == -1) {
            logger.info("预览失败   错误代码为:  " + sdk.NET_DVR_GetLastError());
            sdk.NET_DVR_Logout(cameraInfo.getUserId());
            sdk.NET_DVR_Cleanup();
            return false;
        }
        logger.info("摄像头开始预览....");
        cameraInfoMap.put(address, cameraInfo);
        return true;
    }

    /**
     * 停止摄像头预览
     */
    public static void StopRealPlay() {
        for (String address : cameraInfoMap.keySet()) {
            sdk.NET_DVR_StopRealPlay(cameraInfoMap.get(address).getKey());
            sdk.NET_DVR_Logout(cameraInfoMap.get(address).getUserId());
        }

        sdk.NET_DVR_Cleanup();
        logger.info("摄像头停止预览....");
    }

    /**
     * 开始录制
     *
     * @param address           IP地址
     * @param localSaveFilePath 文件夹路径
     */
    public static void SaveRealDataformDir(String address, String localSaveFilePath) {
        if (!cameraInfoMap.containsKey(address)) {
            return;
        }
        CameraInfo cameraInfo = cameraInfoMap.get(address);

        // 查看文件夹是否存在,如果不存在则创建
        File file = new File(localSaveFilePath);
        if (!file.exists()) {
            file.mkdirs();
        }

        // 预览成功后 调用接口使视频资源保存到文件中 这是完整过程视频,如果需要打开注释即可
        if (!sdk.NET_DVR_SaveRealData(cameraInfo.getKey(), file.getPath() + "/" + cameraInfo.getAddress() + "_" + System.currentTimeMillis() + ".mp4")) {
            logger.info("保存到文件失败 错误码为:  " + sdk.NET_DVR_GetLastError());
            sdk.NET_DVR_StopRealPlay(cameraInfo.getKey());
            sdk.NET_DVR_Logout(cameraInfo.getUserId());
            sdk.NET_DVR_Cleanup();
            return;
        }
        logger.info(address + "开始录制");
        cameraRecordTime.put(address, DateUtils.getCurrentTime());
    }

    /**
     * 停止录制
     *
     * @param address IP地址
     */
    public static void StopSaveRealData(String address) {
        if (!cameraInfoMap.containsKey(address)) {
            return;
        }
        CameraInfo cameraInfo = cameraInfoMap.get(address);

        sdk.NET_DVR_StopSaveRealData(cameraInfo.getKey());
        logger.info(address + "停止录制");
        cameraRecordTime.remove(address);
    }

    /**
     * 若摄像头录像时间超过10分钟，则自动停止录像
     */
    public static void checkCamera() {
        String now = DateUtils.getCurrentTime();
        cameraRecordTime.keySet().forEach(address -> {
            if (DateUtils.getSeconds(cameraRecordTime.get(address), now) > delay) {
                StopSaveRealData(address);
            }
        });
    }

    /**
     * 通过云台参数控制摄像机位置
     *
     * @param address
     * @param wPanpos
     * @param wTiltPos
     * @param wZoomPos
     */
    public static boolean PTZ(String address, String wPanpos, String wTiltPos, String wZoomPos) {
        if (!cameraInfoMap.containsKey(address)) {
            return false;
        }

        CameraInfo cameraInfo = cameraInfoMap.get(address);

        /*   获取PTZ快球位置参数
         *    获取水平角度,垂直角度,变倍角度
         * */
        IntByReference intByReference = new IntByReference(0);
        // 创建PTZPOS参数对象
        HCNetSDK.NET_DVR_PTZPOS net_dvr_ptzpos = new HCNetSDK.NET_DVR_PTZPOS();
        Pointer pointer = net_dvr_ptzpos.getPointer();
        // 执行获取参数
        if (!sdk.NET_DVR_GetDVRConfig(cameraInfo.getKey(), HCNetSDK.NET_DVR_GET_PTZPOS, new NativeLong(0), pointer, net_dvr_ptzpos.size(), intByReference)) {
            System.out.println("获取DVR参数PTZ参数失败,错误码为:    " + sdk.NET_DVR_GetLastError());
            sdk.NET_DVR_StopRealPlay(cameraInfo.getKey());
            sdk.NET_DVR_Logout(cameraInfo.getUserId());
            sdk.NET_DVR_Cleanup();
            return false;
        }

        // 把数据写入net_dvr_ptzpos对象中  下面才可以获取到参数
        net_dvr_ptzpos.read();
        /*
         * 设置PTZPOS快球参数
         * 设置垂直角度,水平角度,变倍角度
         * 从配置文件中获取参数 设置到PTZ中
         * */
        try {
            net_dvr_ptzpos.wPanPos = Short.parseShort(wPanpos);                //设置水平角度
            net_dvr_ptzpos.wTiltPos = Short.parseShort(wTiltPos);                   //设置垂直角度
            net_dvr_ptzpos.wZoomPos = Short.parseShort(wZoomPos);                  //设置变倍参数
            net_dvr_ptzpos.write();                     //把数据写入缓冲区
        } catch (Exception e) {
            System.out.println("获取配置文件PTZ参数失败,请检查参数名称是否正确.....");
            sdk.NET_DVR_StopRealPlay(cameraInfo.getKey());
            sdk.NET_DVR_Logout(cameraInfo.getUserId());
            sdk.NET_DVR_Cleanup();
            return false;
        }
        /*
         * 设置PTZ参数 水平参数 垂直参数 变倍参数
         * */
        if (!sdk.NET_DVR_SetDVRConfig(cameraInfo.getUserId(), HCNetSDK.NET_DVR_SET_PTZPOS, new NativeLong(0), pointer, net_dvr_ptzpos.size())) {
            System.out.println("设置PTZ参数失败,错误码为:    " + sdk.NET_DVR_GetLastError());

            sdk.NET_DVR_Logout(cameraInfo.getUserId());
            sdk.NET_DVR_Cleanup();
            return false;
        }

        System.out.println("设置PTZ参数成功");
        return true;
    }

    /*****************************************************************
     * 注册设备并返回参数
     * 目的:   注册设备并返回key    :Nativelong
     * parameters:    cameraInfo
     *return:        cameraInfo
     * ****************************************************************/
    private static CameraInfo getParameter(CameraInfo cameraInfo) {
        //设置设备通道号   查看Demo代码  通道号为1
        NativeLong channel = new NativeLong(1);
        cameraInfo.setChannel(channel);
        if (!sdk.NET_DVR_Init()) {
            System.out.println("初始化失败..................");
        }

        //创建设备
        HCNetSDK.NET_DVR_DEVICEINFO_V30 deInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();

        //注册用户设备
        NativeLong id = sdk.NET_DVR_Login_V30(cameraInfo.getAddress(), cameraInfo.getPort(),
                cameraInfo.getUserName(), cameraInfo.getPwd(), deInfo);
        cameraInfo.setUserId(id);
        return cameraInfo;
    }

    static class FRealDataCallBack implements HCNetSDK.FRealDataCallBack_V30 {
        //预览回调
        public void invoke(NativeLong lRealHandle, int dwDataType, ByteByReference pBuffer, int dwBufSize, Pointer pUser) {

        }
    }
}
