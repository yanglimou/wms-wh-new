package com.tsj.common.constant;

import com.tsj.common.config.CommonConfig;
import com.tsj.common.utils.BaseUtil;

/**
 * 系统内的路径常量
 * @author Frank
 */
public class FileConstant {
    private final static String BASE_PATH = CommonConfig.prop.get("file.path");

    /**
     * 高值柜刷卡拍照上传文件路径
     */
    public final static String CABINET_PATH = BASE_PATH + "cabinet/";

    /**
     * 默认的临时生成文件路径
     */
    public final static String TEMP_PATH = BASE_PATH + "temp/";

    /**
     * 默认的模板文件路径
     */
    public final static String TEMPLATE_PATH = BASE_PATH + "template/";

    /**
     * APP 升级文件路径
     */
    public final static String APP_UPGRADE_PATH = BASE_PATH + "upgrade/";

    /**
     * 备份文件路径
     */
    public final static String BACKUP_PATH = BASE_PATH + "backup/";

    /**
     * 库文件路径
     */
    public final static String LIB_PATH = BASE_PATH + "lib/";

    /**
     * 录像文件路径
     */
    public final static String VIDEO_PATH = BASE_PATH + "video/";

    /**
     * 日志文件路径
     */
    public final static String LOG_PATH = BASE_PATH + "log/";

    /**
     * 海康SDK库文件路径
     */
    public final static String HIKVISION_PATH = BASE_PATH + "hikvision/";

    /**
     * 应用更新路径
     */
    public final static String APPLICATION_PATH = BASE_PATH + "applications/";


}
