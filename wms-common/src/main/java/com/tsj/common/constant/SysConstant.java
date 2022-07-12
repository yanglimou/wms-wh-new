package com.tsj.common.constant;

/**
 * Global constants definition
 */
public abstract class SysConstant {

    /**
     * SQL查询条件拼接类型
     */
    public static final String SQL_PATTERN_EQUAL = "equal";
    public static final String SQL_PATTERN_NOT_EQUAL = "not_equal";
    public static final String SQL_PATTERN_LESS = "less";
    public static final String SQL_PATTERN_MORE = "more";
    public static final String SQL_PATTERN_BETWEEN = "between";
    public static final String SQL_PATTERN_LIKE = "like";
    public static final String SQL_PATTERN_IN = "in";

    /**
     * SQL查询结果排序规则
     */
    public static final String SQL_SORT_ASC = "ASC";
    public static final String SQL_SORT_DESC = "DESC";

    /**
     * 系统状态定义
     */
    public static final String StateSuccess = "success";
    public static final String StateError = "error";

    public static final String StateEnable = "true";
    public static final String StateUnable = "false";

    public static final String InventoryMore = "more";
    public static final String InventoryLess = "less";
    public static final String InventoryNormal = "normal";

    public static final String UploadSuccess = "success";
    public static final String UploadFailed = "failed";

    public static final String TagNo = "no";
    public static final String TagAc = "ac";

    public static final String TagIn = "in";
    public static final String TagOut = "out";

    public static final String StateComplete = "Complete";
    public static final String StateUncompleted = "Uncompleted";
    public static final String StateNeedConfirm = "NeedConfirm";
    public static final String StateCancel = "Cancel";

    /**
     * 数据库批量处理大小
     */
    public static final int batchSize = 1000;

    /**
     * CACHE NAME
     */
    public static final String CacheBase = "CacheBase";
    public static final String CacheCom = "CacheCom";
    public static final String CacheSys = "CacheSys";

    /**
     * CACHE KEY NAME
     */
    public static final String StockBaseByDeptIdAndGoodsId = "StockBaseByDeptIdAndGoodsId@";
    public static final String UserByToken = "UserByToken@";

    public static final String ManufacturerById = "ManufacturerById@";
    public static final String SupplierById = "SupplierById@";
    public static final String GoodsById = "GoodsById@";
    public static final String PrintById = "PrintById@";
    public static final String UserById = "UserById@";
    public static final String DeptById = "DeptById@";
    public static final String CabinetById = "CabinetById@";
    public static final String MaterialById = "MaterialById@";
    public static final String OrderByCode = "OrderByCode@";

    public static final String TagById = "TagById@";
    public static final String TagByEpc = "TagByEpc@";
    public static final String InventoryDeptById = "InventoryDeptById@";

    public static final String DeptByName = "DeptByName@";
    public static final String CabinetByName = "CabinetByName@";
    public static final String ManufacturerByName = "ManufacturerByName@";
    public static final String SupplierByName = "SupplierByName@";

    /**
     * 定义导入导出文件模板
     */
    public static final String TEMPLATE_Dept = "tempDeptList.xls";
    public static final String TEMPLATE_Cabinet = "tempCabinetList.xls";
    public static final String TEMPLATE_Goods = "tempGoodsList.xls";
    public static final String TEMPLATE_Manufacturer = "tempManufacturerList.xls";
    public static final String TEMPLATE_Supplier = "tempSupplierList.xls";
    public static final String TEMPLATE_User = "tempUserList.xls";

    public static final String TEMPLATE_TAG = "tempTagList.xls";
    public static final String TEMPLATE_TAG_INOUT = "tempTagInOutList.xls";
    public static final String TEMPLATE_TAG_INOUT_RECORD = "tempTagInOutRecordList.xls";
    public static final String TEMPLATE_TAG_STOCK = "tempTagStockList.xls";
    public static final String TEMPLATE_INVENTORY = "tempInventory.xls";
    public static final String TEMPLATE_RECORD_INVENTORY_NEW = "tempRecordInventoryNewList.xls";
    public static final String TEMPLATE_TAG_STOCK_RECORD = "tempTagStockRecordList.xls";

    /**
     * Stock State
     */
    public static final String StockIn = "in";
    public static final String StockOut = "out";
    public static final String StockNone = "none";

    /**
     * 安全密码
     */
    public final static String SAFE_PASSWORD = "BJT880803";

    /**
     * 默认登录密码
     */
    public final static String RESET_PASSWORD = "123456";

}
