package com.tsj.service;

import com.tsj.domain.model.Printer;
import com.tsj.service.common.MyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;

@Slf4j
public class PrinterService extends MyService {

    public List<Printer> getPrinterList() {
        return Printer.dao.findAll();
    }

    public void savePrinter(Printer printer) {
        if (ObjectUtils.isNotEmpty(printer.getId())) {
            printer.update();
        } else {
            printer.save();
        }
    }

    public void deletePrinter(Integer id) {
        Printer.dao.deleteById(id);
    }
}