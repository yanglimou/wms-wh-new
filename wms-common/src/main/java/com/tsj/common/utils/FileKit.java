package com.tsj.common.utils;

import com.jfinal.log.Log;
import com.jfinal.upload.UploadFile;
import com.tsj.common.config.CommonConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Frank
 * @describe 文件操作工具
 * @date 2018年1月11日 下午8:34:00
 */
public class FileKit {
    private static Log logger = Log.getLog(FileKit.class);

    /**
     * 移动目标文件到指定目录
     *
     * @param file     目标文件
     * @param filePath 移动的文件路径
     * @return
     */
    public static String moveFile(File file, String filePath) {
        String fileName = file.getName();
        String fileSuffix = fileName.substring(fileName.lastIndexOf("."), fileName.length());
        String newFileName = IDGenerator.makeId() + fileSuffix;
        String newFilePath = filePath + newFileName;
        boolean result = file.renameTo(new File(newFilePath));
        return result ? newFileName : null;
    }

    public static boolean moveFile(UploadFile uploadFile, String filePath, String fileNewName) {
        // 查看文件夹是否存在,如果不存在则创建
        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = uploadFile.getFile();
        String fileName = uploadFile.getFileName();
        String fileSuffix = fileName.substring(fileName.lastIndexOf("."), fileName.length());
        String newFileName = fileNewName + fileSuffix;
        String newFilePath = filePath + "/" + newFileName;
        return file.renameTo(new File(newFilePath));
    }

    public static List<String> moveFile(UploadFile file, String filePath, String time, String filePrefix) {
        List<UploadFile> fileList = new ArrayList<UploadFile>() {{
            add(file);
        }};
        return moveFiles(fileList, filePath, time, filePrefix);
    }

    /**
     * 移动目标文件到指定目录
     * 若指定目录存在，则覆盖
     *
     * @param fileList   目标文件列表
     * @param filePath   移动的文件根路径
     * @param time       时间
     * @param filePrefix 文件名前缀
     * @return 返回文件路径集合
     */
    public static List<String> moveFiles(List<UploadFile> fileList, String filePath, String time, String filePrefix) {

        //提取日期，将文件按日期分组保存
        String date = time.substring(0, 10);

        List<String> images = new ArrayList<>();
        if (fileList == null || fileList.size() == 0) {
            return images;
        }

        //创建指定日期文件夹
        String destFilePath = filePath + date;
        if (!createDir(destFilePath, false)) {
            return images;
        }

        //移动目标文件到指定路径并修改文件名
        fileList.forEach(uploadFile -> {
            File file = uploadFile.getFile();
            String fileName = uploadFile.getFileName();
            String fileSuffix = fileName.substring(fileName.lastIndexOf("."), fileName.length());
            String newFileName = filePrefix + time.substring(11, 19).replace(":", "") + fileSuffix;
            String newFilePath = destFilePath + "/" + newFileName;
            file.renameTo(new File(newFilePath));

            rotate(newFilePath, fileSuffix.substring(1).toLowerCase(), CommonConfig.prop.getInt("switch.imageRotate", 0));
            images.add(date + "/" + newFileName);
        });
        return images;
    }

    /**
     * 图片旋转
     *
     * @param filePath    图片路径
     * @param fileType    图片类型
     * @param rotateValue 顺时针旋转角度
     */
    private static void rotate(String filePath, String fileType, int rotateValue) {
        if (rotateValue == 0) {
            return;
        }

        if (fileType.equals("png") || fileType.equals("jpg")) {
            BufferedImage src = null;
            try {
                src = ImageIO.read(new File(filePath));
                BufferedImage des1 = RotateImage.Rotate(src, rotateValue);
                ImageIO.write(des1, fileType, new File(filePath));
            } catch (IOException e) {
                logger.error(e.toString());
            }
        }
    }

    /**
     * 创建目标文件夹
     *
     * @param destFilePath 文件夹路径
     * @param isCover      是否覆盖
     * @return
     */
    private static boolean createDir(String destFilePath, boolean isCover) {
        if (new File(destFilePath).exists()) {
            if (isCover) {
                //若目标文件夹存在，则清空文件夹
                Arrays.asList(new File(destFilePath).listFiles()).forEach(file -> file.delete());
                logger.warn(destFilePath + " is cover");
            }
            return true;
        } else {
            boolean bl = new File(destFilePath).mkdirs();
            if (!bl) {
                logger.warn(destFilePath + " mkdir failed");
                return false;
            } else {
                return true;
            }
        }
    }
}