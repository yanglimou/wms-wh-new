package com.tsj.tcp.tcpserver.business;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.domain.model.Location;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
/**
 *
 * {
 * 	"order": " patientproduct ",
 * 	"number": "32 位字符串",
 * 	"code": "耗材柜编号",
 * 	"data": {
 * 		"result": "0",
 * 		"operator": "0001",
 * 		"product": [
 *                        {
 * 				"epc": "123",
 * 				"location": "1",
 * 				"operation": "存/取",
 * 				"patient": "手术ID"
 *            },
 *            {
 * 				"epc": "123",
 * 				"location": "1",
 * 				" operation ": "存/取",
 * 				"patient": "手术ID"
 *            }
 * 		]
 *    }
 * }
 *
 */

/**
 *
 * {
 *   "order":"patientproduct",
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
 */ public class PatientProduct {
    public static void run(ChannelHandlerContext channelHandlerContext, JSONObject jsonObject) {
        log.debug("耗材确认");
        String cabinet = jsonObject.getString("code");//耗材柜编号
        JSONObject response = new JSONObject();
        response.put("order", "patientproduct");
        response.put("number", jsonObject.getString("number"));
        response.put("message", "0");
        List<JSONObject> dataResponse = new ArrayList();
        JSONObject rowResponse = new JSONObject();
        rowResponse.put("location", "1");
        List<JSONObject> jxq = new ArrayList<>();
        List<JSONObject> qt = new ArrayList<>();
        jsonObject.getJSONObject("data").getJSONArray("product").stream().map(row -> (JSONObject) row).forEach(row -> {
            String epc = row.getString("epc");
            String operation = row.getString("operation");
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
            if ("存".equals(operation)) {
                Location locationModel = new Location().setEpc(epc).setCabinet(cabinet);
                locationModel.save();
            } else if ("取".equals(operation)) {
                Db.delete("delete from com_location where epc=? and cabinet=?", epc, cabinet);
            }
        });
        rowResponse.put("jxq", jxq);
        rowResponse.put("qt", qt);
        dataResponse.add(rowResponse);
        response.put("data", dataResponse);
        String responseString = response.toJSONString();
        log.debug("耗材确认响应报文：{}", responseString);
        channelHandlerContext.writeAndFlush(responseString);
    }
}
