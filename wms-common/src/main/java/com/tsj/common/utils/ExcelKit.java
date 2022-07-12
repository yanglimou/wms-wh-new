package com.tsj.common.utils;

import com.tsj.common.config.CommonConfig;
import com.tsj.common.constant.FileConstant;
import com.jfinal.log.Log;
import com.jfinal.upload.UploadFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * EXCEL数据报表服务
 *
 * @author Frank
 */
public class ExcelKit {
    private static Log logger = Log.getLog(ExcelKit.class);

    /**
     * 导入Excel
     *
     * @param file
     * @return
     */
    public static List<String[]> getObjectListFromExcel(UploadFile file, int rows, int cellNum, int... notNullCells) throws Exception {
        if (file == null)
            throw new Exception("文件上传失败");

        POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(file.getFile()));
        HSSFWorkbook wb = new HSSFWorkbook(fs);
        HSSFSheet sheet = wb.getSheetAt(0);

        List<String[]> list = new ArrayList<>();
        while (true) {
            String[] valueArray = getCellValueArray(sheet, rows++, cellNum);

            //判断是否读取全部数据
            if (StringUtils.isAllEmpty(valueArray))
                break;

            //判断单元格是否为空
            boolean state = Arrays.stream(notNullCells).noneMatch(index -> StringUtils.isEmpty(valueArray[index]));
            if (state)
                list.add(valueArray);
        }

        //列表去重
        list = list.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(()
                -> new TreeSet<>(Comparator.comparing(member -> member[0] + "-" + member[1] + "-" + member[2]))), ArrayList::new));

        logger.info("getObjectListFromExcel{%d}", list.size());

        wb.close();
        fs.close();
        return list;
    }

    /**
     * 导出Excel
     *
     * @param templateFilePath 模板文件
     * @param rows             数据起始行数
     * @param cells            数据列数
     * @param list             数据内容
     * @param count            数据总计
     * @return
     * @throws Exception
     */
    public static String putObjectListToExcel(String templateFilePath, int rows, int cells, List<String[]> list, int count) throws Exception {

        String filePath = FileConstant.TEMP_PATH + IDGenerator.makeId() + ".xls";
        Files.copy(new File(templateFilePath).toPath(), new File(filePath).toPath());

        POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(filePath));
        HSSFWorkbook wb = new HSSFWorkbook(fs);
        CellStyle cellStyle = getCellStyle(wb);

        HSSFSheet sheet = wb.getSheetAt(0);

        //组装EXCEL标题（医院名称+报表名称+日期）
        String hospitalName= CommonConfig.prop.get("hospitalName");
        String title = sheet.getRow(0).getCell(0).getStringCellValue();
        String dateTime = DateUtils.getCurrentTime();
        sheet.getRow(0).getCell(0).setCellValue(hospitalName + " " + title + " " + dateTime);

        for (String[] stringArray : list) {
            createRow(sheet, rows, cells, cellStyle);

            for (int i = 0; i < stringArray.length; i++) {
                sheet.getRow(rows).getCell(i).setCellValue(stringArray[i]);
            }
            rows++;
        }

        //新增数量总计和导出日期信息
        CellStyle cellStyle2 = getCellStyle(wb);
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 20);
        cellStyle2.setFont(font);

        createRow(sheet, rows, 2, cellStyle2);
        sheet.getRow(rows).getCell(0).setCellValue("数量总计");
        sheet.getRow(rows).getCell(1).setCellValue(count);

        //修改模板内容导出目标路径
        FileOutputStream out = new FileOutputStream(filePath);
        wb.write(out);
        out.close();
        return filePath;
    }


    /******************************************************************************************/

    /**
     * 读取单元格内容
     *
     * @param sheet
     * @param rowNum
     * @param cellNum
     * @return
     */
    private static String getCellValue(HSSFSheet sheet, int rowNum, int cellNum) {
        String value = null;
        try {
            if (sheet.getRow(rowNum).getCell(cellNum) != null) {
                value = sheet.getRow(rowNum).getCell(cellNum).toString().trim();
            }
        } catch (Exception ex) {

        }

        return value;
    }

    /**
     * 读取单元格内容
     *
     * @param sheet
     * @param rowNum
     * @param cellNum
     * @return
     */
    private static String[] getCellValueArray(HSSFSheet sheet, int rowNum, int cellNum) {
        String[] valueArray = new String[cellNum];
        String temp;
        try {
            for (int i = 0; i < cellNum; i++) {
                if (sheet.getRow(rowNum).getCell(i) != null) {
                    temp = sheet.getRow(rowNum).getCell(i).toString().trim();
                    if (temp.endsWith(".0")) {
                        temp = temp.replace(".0", "");
                    }
                    valueArray[i] = temp;
                } else {
                    valueArray[i] = null;
                }
            }

        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }

        return valueArray;
    }

    /**
     * 设置单元格样式
     *
     * @param wb
     * @return
     */
    private static CellStyle getCellStyle(HSSFWorkbook wb) {
        CellStyle cellStyle = wb.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);//左右居中
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);//上下居中
        cellStyle.setBorderBottom(BorderStyle.THIN); // 底部边框
        cellStyle.setBorderLeft(BorderStyle.THIN);  // 左边边框
        cellStyle.setBorderRight(BorderStyle.THIN); // 右边边框
        cellStyle.setBorderTop(BorderStyle.THIN); // 上边边框
        cellStyle.setWrapText(true);
        return cellStyle;
    }

    /**
     * 动态添加行
     *
     * @param sheet
     * @param rowNum    行号
     * @param cells     单元格数
     * @param cellStyle 单元格样式
     */
    private static void createRow(HSSFSheet sheet, int rowNum, int cells, CellStyle cellStyle) {
        HSSFRow row = sheet.createRow(rowNum);
        row.setHeight((short) 512);

        for (int i = 0; i < cells; i++) {
            Cell cell = row.createCell(i);
            cell.setCellStyle(cellStyle);
        }
    }
}
