package com.tsj.common.constant;

/**
 * @className: SpdUrl
 * @description: SPD系统访问路径
 * @author: Frank
 * @create: 2020-07-14 10:43
 */
public enum SpdUrl {
    URL_IN_OUT("/HcgTtagstkAdd", "上传出入库记录"),
    URL_Inventory("/HcgTtagstkInventory", "上传盘点记录"),
    URL_DEPARTMENT("/Department", "同步科室信息"),
    URL_USER("/Users", "同步人员信息"),
    URL_GOODS("/Goods", "同步耗材信息"),
    URL_TAG("/TTag", "同步制标信息"),
    URL_STOCK_TAG("/StockTag", "同步库存信息"),
    URL_PRINT("/TTagIns", "同步打印信息"),
    ;

    private String url;
    private String module;

    SpdUrl(String url, String module) {
        this.url = url;
        this.module = module;
    }

    public String getModule() {
        return module;
    }

    public String getUrl() {
        return url;
    }

    /**
     * 枚举反向查找
     *
     * @param url
     * @return
     */
    public static SpdUrl fromUrl(String url) {
        for (SpdUrl spdUrl : SpdUrl.values()) {
            if (url.contains(spdUrl.getUrl())) {
                return spdUrl;
            }
        }
        return null;
    }
}
