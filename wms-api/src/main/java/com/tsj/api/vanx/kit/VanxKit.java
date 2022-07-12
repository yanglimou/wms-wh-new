package com.tsj.api.vanx.kit;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.kit.Kv;
import com.tsj.api.vanx.vo.detail.*;
import com.tsj.api.vanx.vo.main.ListRequestMain;
import com.tsj.api.vanx.vo.main.ListResponseMain;
import com.tsj.api.vanx.vo.main.PageRequestMain;
import com.tsj.api.vanx.vo.main.PageResponseMain;
import com.tsj.api.vanx.wsdl.SPDWebService;
import com.tsj.api.vanx.wsdl.SendRecv;
import com.tsj.common.config.CommonConfig;
import com.tsj.common.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SPD接口辅助类
 *
 * @author honesty
 * @date 2021/10/2214:56
 */
public class VanxKit {
    private static final String SUCCESS = "1";

    /**
     * 查询科室列表
     */
    public static List<BD201Detail> getDeptList() {
        return getList("BD201", BD201Detail.class);
    }

    /**
     * 查询用户列表
     */
    public static List<BD202Detail> getUserList() {
        return getList("BD202", BD202Detail.class);
    }

    /**
     * 查询生产商列表
     */
    public static List<BD203Detail> getManufactureList() {
        return getList("BD203", BD203Detail.class);
    }

    /**
     * 查询供应商列表
     */
    public static List<BD204Detail> getSupplyList() {
        return getList("BD204", BD204Detail.class);
    }

    /**
     * 查询商品列表
     */
    public static List<BD205Detail> getArticleList() {
        return getList("BD205", BD205Detail.class);
    }

    /**
     * 查询手术排期列表
     */
    @Deprecated
    private static List<IC201Detail> getPatientList() {
        return getList("IC201", IC201Detail.class);
    }

    /**
     * 查询标签打印列表
     */
    @Deprecated
    private static List<IC202Detail> getTagPrintList() {
        return getList("IC202", IC202Detail.class);
    }

    /**
     * 查询标签出库列表
     */
    public static List<IC203Detail> getTagOutList(int days) {
        //根据天数计算起止日期
        String QueryEndDate = DateUtils.getCurrentTime();
        String QueryBeginDate = DateUtils.addDay(QueryEndDate, -1 * days);

        return getList("IC203", IC203Detail.class, QueryBeginDate, QueryEndDate);
    }

    /**
     * 查询标签消耗列表
     */
    public static List<IC204Detail> getTagUseList() {
        return getList("IC204", IC204Detail.class);
    }

    /**
     * 查询标签消耗取消列表
     */
    public static List<IC205Detail> getTagUseCancelList() {
        return getList("IC205", IC205Detail.class);
    }

    /**
     * 查询标签盘点单列表
     */
    public static List<IC206Detail> getTagInventoryOrderList() {
        return getList("IC206", IC206Detail.class);
    }

    /**
     * 提交出入柜数据
     */
    public static boolean postTagInOutList(List<IC101Detail> list) {
        return postList("IC101", list);
    }

    /**
     * 提交盘点数据
     */
    public static boolean postTagInventory(List<IC102Detail> list) {
        return postList("IC102", list);
    }


    /**
     * 查询数据列表
     *
     * @param method 方法名
     * @param cls    列表对象类型
     * @param <T>    泛型类
     * @return 数据列表
     */
    private static <T> List<T> getList(String method, Class<T> cls) {
        return getList(method, cls, "1970-01-01 00:00:00", DateUtils.getCurrentTime());
    }

    /**
     * 查询数据列表
     *
     * @param method    方法名
     * @param cls       列表对象类型
     * @param beginDate 开始日期
     * @param endDate   结束日期
     * @param <T>       泛型类
     * @return 数据列表
     */
    private static <T> List<T> getList(String method, Class<T> cls, String beginDate, String endDate) {
        List<T> recordList = new ArrayList<>();
        int pageIndex = 0;
        int pageSize = 5000;

        while (true) {
            PageRequestMain pageRequestMain = new PageRequestMain();
            pageRequestMain.setBEGIN_TIME(beginDate);
            pageRequestMain.setEND_TIME(endDate);
            pageRequestMain.setCURRENT_PAGE_NUMBER(String.valueOf(pageIndex));
            pageRequestMain.setPAGE_DATA_COUNT(String.valueOf(pageSize));

            // 查询数据
            String result = callSpdMethod(method, Kv.by("MAIN", pageRequestMain));
            JSONObject jsonObject = JSON.parseObject(result);
            PageResponseMain pageResponseMain = jsonObject.getObject("MAIN", PageResponseMain.class);
            if (pageResponseMain == null) {
                break;
            }

            // 判断接口返回成功或失败
            if (SUCCESS.equals(pageResponseMain.getSUCCEED())) {
                break;
            }

            // 采集数据
            JSONArray jsonArray = jsonObject.getJSONArray("DETAIL");
            recordList.addAll(jsonArray.toJavaList(cls));

            if (jsonArray.size() < pageSize) {
                break;
            }

            pageIndex++;
        }
        return recordList;
    }

    /**
     * 提交数据列表
     *
     * @param method 方法名
     * @param list   列表对象
     * @param <T>    泛型类
     */
    private static <T> boolean postList(String method, List<T> list) {
        ListRequestMain listRequestMain = new ListRequestMain();
        listRequestMain.setTOTAL_RECORDS(String.valueOf(list.size()));

        // 查询数据
        String result = callSpdMethod(method, Kv.by("MAIN", listRequestMain).set("DETAIL", list));
        JSONObject jsonObject = JSON.parseObject(result);
        ListResponseMain listResponseMain = jsonObject.getObject("MAIN", ListResponseMain.class);
        if (listResponseMain == null) {
            return false;
        }

        // 判断接口返回成功或失败
        if (SUCCESS.equals(listResponseMain.getSUCCEED())) {
            return false;
        }

        return true;
    }

    /**
     * 调用SPD接口方法
     *
     * @param dataMethod 方法名
     * @param object     参数对象
     * @return 返回结果
     */
    private static String callSpdMethod(String dataMethod, Object object) {
        SendRecv sendRecv = new SendRecv();
        sendRecv.setUserCode("");
        sendRecv.setOrgCode("");
        sendRecv.setSafeCode("");
        sendRecv.setDataNo(UUID.randomUUID().toString());
        sendRecv.setDataType("JSON");
        sendRecv.setDataMethod(dataMethod);
        sendRecv.setInsData(JSON.toJSONString(object));
        sendRecv.setInsDataCheck(null);

        return new SPDWebService().getSPDWebServiceSoap().sendRecv(
                sendRecv.getUserCode(),
                sendRecv.getSafeCode(),
                sendRecv.getOrgCode(),
                sendRecv.getDataNo(),
                sendRecv.getDataMethod(),
                sendRecv.getDataType(),
                sendRecv.getInsData(),
                sendRecv.getInsDataCheck());
    }

}
