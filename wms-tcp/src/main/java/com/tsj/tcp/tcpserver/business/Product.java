package com.tsj.tcp.tcpserver.business;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.domain.model.Location;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
/**
 * {
 * 	"order": "product",
 * 	"number": "32位字符串",
 * "code": "耗材柜编号",
 * 	"data": [{
 * 		"location": "1",
 * 		"data": ["ABCD1235"]
 *        }, {
 * 		"location": "2",
 * 		"data": ["ABCD1236", "ABCD1237"]
 *    }, {
 * 		"location": "3",
 * 		"data": []
 *    }]
 * }
 */

/**
 * {
 *   "order":"product",
 *   "number":"32 位字符串",
 *   "message":"0",
 *   "data":[
 *     {
 *       "pp":"品牌 ",
 *       "mc":"名称",
 *       "gg":"规格",
 *       "xqpc":"效期批次",
 *       "yxrq":"有效日期",
 *       "syts":"剩余天数",
 *       "location":"所在位置",
 *       "epc":"耗材EPC",
 *       "operation":"存/取"
 *     },
 *     {
 *       "pp":"品牌 ",
 *       "mc":"名称",
 *       "gg":"规格",
 *       "xqpc":"效期批次",
 *       "yxrq":"有效日期",
 *       "syts":"剩余天数",
 *       "location":"所在位置",
 *       "epc":"耗材EPC",
 *       "operation":"存/取"
 *     }
 *   ]
 * }
 */
public class Product {
    public static void run(ChannelHandlerContext channelHandlerContext, JSONObject jsonObject) {
        log.debug("耗材上传");
        String cabinet = jsonObject.getString("code");//耗材柜编号
        JSONObject response = new JSONObject();
        response.put("order", "product");
        response.put("number", jsonObject.getString("number"));
        response.put("message", "0");
        List<JSONObject> dataResponse = new ArrayList();
        jsonObject.getJSONArray("data").stream().map(row -> (JSONObject) row).forEach(row -> {
            String location = row.getString("location");
            //获取数据库集合
            Set<String> epcSetOld = Db.find("select epc from location  WHERE cabinet=? and location=?", cabinet, location).stream().map(record -> record.getStr("epc")).collect(Collectors.toSet());
            //获取当前集合
            Set<String> epcSetNow = row.getJSONArray("data").stream().map(epc -> (String) epc).collect(Collectors.toSet());
            //获取要存的 epcSetNow-epcSetOld
            Collection<String> epcNeedAdd = CollectionUtils.subtract(epcSetNow, epcSetOld);
            handleEpcSet(epcNeedAdd, true, location, cabinet, dataResponse);
            //获取要取的 epcSetOld-epcSetNow
            Collection<String> epcNeedRemove = CollectionUtils.subtract(epcSetOld, epcSetNow);
            handleEpcSet(epcNeedRemove, false, location, cabinet, dataResponse);
        });
        response.put("data", dataResponse);
        String responseString = response.toJSONString();
        log.debug("耗材上报响应报文：{}", responseString);
        channelHandlerContext.writeAndFlush(responseString);
    }

    private static void handleEpcSet(Collection<String> epcSet, boolean isAdd, String location, String cabinet, List<JSONObject> dataResponse) {
        epcSet.forEach(epc -> {
            if (isAdd) {
                Location locationModel = new Location().setEpc(epc).setLocation(location).setCabinet(cabinet);
                locationModel.save();
            } else {
                Db.delete("delete from location where epc=? and location=? and cabinet=?", epc, location, cabinet);
            }
            Record record = Db.findFirst("select a.epc,a.batchNo,a.expireDate,TIMESTAMPDIFF(DAY,NOW(),a.expireDate) as lastDay,b.name,b.spec,c.`name` manufacturerName from com_tag a LEFT JOIN base_goods b on a.goodsId=b.id LEFT JOIN base_manufacturer c on b.manufacturerId=c.id where a.epc= ?", epc);
            log.debug("record:{}", record);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("pp", record == null ? "" : record.getStr("manufacturerName"));
            jsonObject.put("mc", record == null ? "" : record.getStr("name"));
            jsonObject.put("gg", record == null ? "" : record.getStr("spec"));
            jsonObject.put("xqpc", record == null ? "" : record.getStr("batchNo"));
            jsonObject.put("yxrq", record == null ? "" : record.getStr("expireDate"));
            jsonObject.put("syts", record == null ? "" : record.getStr("lastDay"));
            jsonObject.put("location", location);
            jsonObject.put("epc", epc);
            jsonObject.put("operation", isAdd ? "存" : "取");
            dataResponse.add(jsonObject);
        });
    }
}
