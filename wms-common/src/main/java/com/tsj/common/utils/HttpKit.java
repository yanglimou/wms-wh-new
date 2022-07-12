package com.tsj.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.json.FastJson;
import com.jfinal.kit.Kv;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.event.SpdEvent;
import net.dreamlu.event.EventKit;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @className: HttpService
 * @description: Http数据服务
 * @author: Frank
 * @create: 2020-04-13 10:12
 */
public class HttpKit {
    private static final Log logger = Log.getLog(HttpKit.class);

    public static R postSpdTag(String url, String deptId, String cabinetId, String cabinetName, List<String> spdCodeArray, String userId, String time, String type) {

        //TODO 万序修改
        List<Kv> list = new ArrayList<>();
        for (String spdCode : spdCodeArray) {
            list.add(Kv.by("ttag", spdCode));
        }

        JSONObject jo = new JSONObject();
        jo.put("deptId", deptId);
        jo.put("cabinetId", cabinetId);
        jo.put("cabinetName", cabinetName);
        jo.put("userId", userId);
        jo.put("type", type);
        jo.put("time", time);
        jo.put("spdCodeData", list);

        JSONObject result = JSON.parseObject(HttpPostWithJson(url, jo));
        if ("0".equals(result.getString("code"))) {
            return R.ok();
        } else {
            logger.error(result.getString("msg"));
            return R.error(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
    }

    public static R postSpdTagInventory(String url, String deptId, String cabinetId, String cabinetName, List<String> spdCodeArray,
                                        String userId, String time, String inventoryNo, String type) {

        //TODO 万序修改
        List<Kv> list = new ArrayList<>();
        for (String spdCode : spdCodeArray) {
            list.add(Kv.by("ttag", spdCode));
        }

        JSONObject jo = new JSONObject();
        jo.put("deptId", deptId);
        jo.put("cabinetId", cabinetId);
        jo.put("cabinetName", cabinetName);
        jo.put("userId", userId);
        jo.put("type", type);
        jo.put("time", time);
        jo.put("inventoryNo", inventoryNo);
        jo.put("spdCodeData", list);

        JSONObject result = JSON.parseObject(HttpPostWithJson(url, jo));
        if ("0".equals(result.getString("code"))) {
            return R.ok();
        } else {
            logger.error(result.getString("msg"));
            return R.error(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
    }

    public static List<Record> postSpdData(String url, String dateParameter, Kv kv) {
        List<Record> recordList = new ArrayList<>();
        int pageIndex = 0;
        int pageSize = 5000;

        //TODO 万序修改
        while (true) {
            JSONObject jo = new JSONObject();
            jo.put("pageIndex", String.valueOf(pageIndex));
            jo.put("pageSize", String.valueOf(pageSize));
            kv.forEach((k, v) -> jo.put(k.toString(), v));

            JSONArray jsonArray = postToServer(url, jo);
            jsonArray.forEach(object -> {
                JSONObject jsonObject = (JSONObject) object;
                if (dateParameter != null) {
                    //日期格式替换2019-12-26T17:31:39=》2019-12-26 17:31:39
                    jsonObject.replace(dateParameter, jsonObject.getString(dateParameter).replace("T", " "));
                }
                recordList.add(new Record().setColumns(FastJson.getJson().parse(jsonObject.toJSONString(), Map.class)));
            });

            if (jsonArray.size() < pageSize) {
                break;
            }

            pageIndex++;
        }
        return recordList;
    }

    public static JSONArray postToServer(String Url, JSONObject jsonObject) {
        JSONArray jsonArray = new JSONArray();
        JSONObject result = JSON.parseObject(HttpPostWithJson(Url, jsonObject));
        if ("0".equals(result.getString("code"))) {
            jsonArray = result.getJSONArray("ResonseData");
        } else {
            logger.error(Url + "," + jsonObject.toJSONString() + "," + result.toJSONString());
        }
        return jsonArray;
    }

    public static String HttpPostWithJson(String url, JSONObject jsonObject) {
        String result = null;

        String errorMessage = "";
        Instant startTime = Instant.now();

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-Type", "application/json");
        CloseableHttpResponse httpResponse = null;
        try {
            // 解决中文乱码问题
            StringEntity stringEntity = new StringEntity(jsonObject.toString(), "utf-8");
            stringEntity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            httpPost.setEntity(stringEntity);
            httpResponse = httpClient.execute(httpPost);
            result = EntityUtils.toString(httpResponse.getEntity());
        } catch (IOException e) {
            errorMessage += e.toString();
        } finally {
            try {
                if (httpClient != null) {
                    httpClient.close();
                }
                if (httpResponse != null) {
                    httpResponse.close();
                }
            } catch (IOException e) {
                errorMessage += e.toString();
            }
        }

        if (StringUtils.isNotEmpty(errorMessage)) {
            JSONObject jo = new JSONObject();
            jo.put("code", "404");
            jo.put("msg", "服务请求异常");
            result = jo.toJSONString();
        }
        int millis = (int) Duration.between(startTime, Instant.now()).toMillis();
        EventKit.post(new SpdEvent("POST", url, jsonObject, errorMessage, result, millis));
        return result;
    }
}
