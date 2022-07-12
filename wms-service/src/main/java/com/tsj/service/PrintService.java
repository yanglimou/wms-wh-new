package com.tsj.service;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.tsj.domain.model.Printer;
import com.tsj.service.common.MyService;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.TcpConnection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class PrintService extends MyService {

    public Page<Record> getPrintPage(int pageNumber, int pageSize, String spdCode, String insNo) {
        String condition = "";
        if (StringUtils.isNotEmpty(spdCode)) {
            condition += " and a.spdCode='" + spdCode + "'";
        }
        if (StringUtils.isNotEmpty(insNo)) {
            condition += " and a.insNo='" + insNo + "'";
        }
        String select = "select a.insNo,a.spdCode,a.epc,a.userId,a.comGoodsId,a.lotNo,substring(a.expireDate,1,10) expireDate,a.printFlag,a.shelfCode,a.hvFlag,b.name,b.spec,b.unit,c.`name` manufacturerName ";
        String sqlExceptSelect = "  from com_print a LEFT JOIN base_goods b on a.comGoodsId=b.id left JOIN base_manufacturer c on b.manufacturerId=c.id where 1=1 " + condition + " order by a.printFlag,a.spdCode ";
        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect);
    }

    public List<Record> findByInsNo(String insNo) {
        return Db.find("select a.spdCode,a.epc,a.comGoodsId,a.lotNo,substring(a.expireDate,1,10) expireDate,a.shelfCode,a.hvFlag,b.name,b.spec,b.unit,c.`name` manufacturerName from com_print a LEFT JOIN base_goods b on a.comGoodsId=b.id left JOIN base_manufacturer c on b.manufacturerId=c.id where a.insNo=?", insNo);
    }

    public Page<Record> getPrintPage2(int pageNumber, int pageSize) {
        String select = "select a.* ";
        String sqlExceptSelect = " from (select insNo,count(insNo) num,sum(printFlag=1) printNum,sum(printFlag=0) unPrintNum from com_print group by insNo ORDER BY insNo desc) as a";
        return Db.paginate(pageNumber, pageSize, select, sqlExceptSelect);
    }

    public List<Printer> getPrinter() {
        return Printer.dao.findAll();
//        return Kv.create()
//                .set("high", Arrays.asList(Kv.by("clientId", "2").set("highFlag", "1")))
//                .set("low", Arrays.asList());
    }

    public boolean print(String printId, String zpl) {
        Printer printer = Printer.dao.findById(printId);
        if (Objects.isNull(printer)) return false;
        String ip = printer.getIp();
        String port = printer.getPort();
        log.debug("printId:{},zpl:{},ip:{},port:{}", printId, zpl, ip, port);
        try {
            Connection printerConnection = new TcpConnection(ip, Integer.parseInt(port));
            printerConnection.open();
            printerConnection.write(zpl.getBytes());
            printerConnection.close();
            return true;
        } catch (Exception e) {
            log.error("print error", e);
        }
        return false;
    }

    public String getZpl(String name, String spec, String manufacturerName, String lotNo, String expireDate, String spdCode, String epc) {
        String[] nameTowLine = getTowLine(name);
        String[] specTowLine = getTowLine(spec);
        String[] manufacturerNameTowLine = getTowLine(manufacturerName);
        String template = "^XA\n" +
                "^CI28\n" +
                "^CWA,E:SIMSUN.FNT\n" +
                "^FO100,250^AAN,50,28^FD" + nameTowLine[0] + "^FS\n" +
                "^FO100,300^AAN,50,28^FD" + nameTowLine[1] + "^FS\n" +
                "^FO100,350^AAN,50,28^FD" + specTowLine[0] + "^FS\n" +
                "^FO100,400^AAN,50,28^FD" + specTowLine[1] + "^FS\n" +
                "^FO100,450^AAN,50,28^FD" + spdCode + "^FS\n" +
                "^FO100,500^AAN,50,28^FD" + lotNo + "^FS\n" +
                "^FO100,550^AAN,50,28^FD" + expireDate + "^FS\n" +
                "^FO100,600^AAN,50,28^FD" + manufacturerNameTowLine[0] + "^FS\n" +
                "^FO100,650^AAN,50,28^FD" + manufacturerNameTowLine[1] + "^FS\n" +
                "^BY2,2,100\n" +
                "^FO100,700^BC^FD" + spdCode + "^FS\n" +
                "^RFW,H,2,12,1^FD" + epc + "^FS" +
                "^XZ";
        return template;
    }

    //分割字符成两行
    private String[] getTowLine(String name) {
        if (StringUtils.isBlank(name)) return new String[]{"", ""};
        int max = 30;
        List<String> list = new ArrayList();
        char[] chars = name.toCharArray();
        int count = 0;
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : chars) {
            int size = c >= '\u4e00' && c <= '\u9fa5' ? 2 : 1;
            if (count + size > max) {
                //超了
                list.add(stringBuilder.toString());
                if (list.size() == 2) return new String[]{list.get(0), list.get(1)};
                stringBuilder = new StringBuilder();
                count = 0;
            }
            count += size;
            stringBuilder.append(c);
        }
        if (stringBuilder.length() > 0) {
            list.add(stringBuilder.toString());
        }
        return new String[]{list.get(0), list.size() > 1 ? list.get(1) : ""};
    }
}