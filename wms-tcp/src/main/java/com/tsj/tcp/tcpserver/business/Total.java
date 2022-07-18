package com.tsj.tcp.tcpserver.business;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.domain.model.Location;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class Total {
    /**
     * {
     * 	"order": "total",
     * 	"number": "32位字符串",
     * "code": "耗材柜编号",
     * "operator": "0001",
     * 	"data": [{
     * 		"location": "1",
     * 		"data": ["ABCD1234", "ABCD1235"]
     *        }, {
     * 		"location": "2",
     * 		"data": ["ABCD1236", "ABCD1237"]
     *    }, {
     * 		"location": "3",
     * 		"data": ["ABCD1238", "ABCD1239"]
     *    }, {
     * 		"location": "4",
     * 		"data": ["ABCD1240", "ABCD1241"]
     *    }, {
     * 		"location": "5",
     * 		"data": ["ABCD1242", "ABCD1243"]
     *    }]
     * }
     */

    /**
     * {
     *   "order":"total",
     *   "number":"32 位字符串",
     *   "message":"0",
     *   "data":[
     *     {
     *       "location":"1",
     *       "jxq":[
     *         {
     *           "pp":"品牌 ",
     *           "mc":"名称",
     *           "gg":"规格",
     *           "xqpc":"效期批次",
     *           "yxrq":"有效日期",
     *           "syts":"剩余天数",
     *           "location":"所在位置",
     *           "operation":""
     *         }
     *       ],
     *       "qt":[
     *         {
     *           "pp":"品牌 ",
     *           "mc":"名称",
     *           "gg":"规格",
     *           "xqpc":"效期批次",
     *           "yxrq":"有效日期",
     *           "syts":"剩余天数",
     *           "location":"所在位置",
     *           "operation":""
     *         }
     *       ]
     *     },
     *     {
     *       "location":"2",
     *       "jxq":[
     *         {
     *           "pp":"品牌 ",
     *           "mc":"名称",
     *           "gg":"规格",
     *           "xqpc":"效期批次",
     *           "yxrq":"有效日期",
     *           "syts":"剩余天数",
     *           "location":"所在位置",
     *           "operation":""
     *         }
     *       ],
     *       "qt":[
     *         {
     *           "pp":"品牌 ",
     *           "mc":"名称",
     *           "gg":"规格",
     *           "xqpc":"效期批次",
     *           "yxrq":"有效日期",
     *           "syts":"剩余天数",
     *           "location":"所在位置",
     *           "operation":""
     *         }
     *       ]
     *     }
     *   ]
     * }
     *
     */

    /**
     * 1. 删除耗材柜的所有位置信息
     * 2. 添加耗材柜的所有位置信息
     * 3. 查询耗材相关信息
     * 4. 组织报文返回
     *
     * @param channelHandlerContext
     * @param jsonObject
     */
    public static void run(ChannelHandlerContext channelHandlerContext, JSONObject jsonObject) {
        log.debug("耗材统计");
        String cabinet = jsonObject.getString("code");//耗材柜编号
        // 1. 删除耗材柜的所有位置信息
        Db.delete("delete from com_location WHERE cabinet=?", cabinet);
        JSONObject response = new JSONObject();
        response.put("order", "total");
        response.put("number", jsonObject.getString("number"));
        response.put("message", "0");
        List<JSONObject> dataResponse = new ArrayList();
        Set<String> epcSet = jsonObject.getJSONArray("data").stream().map(row -> (JSONObject) row).flatMap(row -> row.getJSONArray("data").stream().map(epc -> (String) epc)).collect(Collectors.toSet());
        log.debug("epcSet : {}", JSON.toJSONString(epcSet));
        JSONObject rowResponse = new JSONObject();
        rowResponse.put("location", "1");
        List<JSONObject> jxq = new ArrayList<>();
        List<JSONObject> qt = new ArrayList<>();
        List<Location> locationList = new ArrayList<>();
        epcSet.forEach(epc -> {
            // 3. 查询耗材相关信息
            Record record = Db.findFirst("select a.epc,a.batchNo,a.expireDate,TIMESTAMPDIFF(DAY,NOW(),a.expireDate) as lastDay,b.name,b.spec,c.`name` manufacturerName from com_tag a LEFT JOIN base_goods b on a.goodsId=b.id LEFT JOIN base_manufacturer c on b.manufacturerId=c.id where a.epc= ?", epc);
            log.debug("record:{}", record);
            if (record == null) return;
            // 4. 组织报文返回
            JSONObject goodsResponse = new JSONObject();
            goodsResponse.put("pp", record == null ? "" : record.getStr("manufacturerName"));
            goodsResponse.put("mc", record == null ? "" : record.getStr("name"));
            goodsResponse.put("gg", record == null ? "" : record.getStr("spec"));
            goodsResponse.put("xqpc", record == null ? "" : record.getStr("batchNo"));
            goodsResponse.put("yxrq", record == null ? "" : record.getStr("expireDate"));
            goodsResponse.put("syts", record == null ? "" : record.getStr("lastDay"));
            goodsResponse.put("location", "1");
            goodsResponse.put("operation", "");
            if (record != null && record.getInt("lastDay") >= 0 && record.getInt("lastDay") <= 60) {
                jxq.add(goodsResponse);
            } else {
                qt.add(goodsResponse);
            }
            // 保存数据库
            Location locationModel = new Location().setCabinet(cabinet).setEpc(epc);
            locationList.add(locationModel);
        });
        rowResponse.put("jxq", jxq);
        rowResponse.put("qt", qt);
        dataResponse.add(rowResponse);
        response.put("data", dataResponse);
        String responseString = response.toJSONString();
        Db.batchSave(locationList, 5000);
        log.debug("耗材统计响应报文：{}", responseString);
        channelHandlerContext.writeAndFlush(responseString);
    }
}
