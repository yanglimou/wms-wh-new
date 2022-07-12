package com.tsj.api.vanx.vo.main;

import lombok.Data;

/**
 * 接口[BD201]出参
 *
 * @author honesty
 * @date 2021/10/2214:03
 */
@Data
public class PageResponseMain {
    private String SUCCEED;
    private String MESSAGE;
    private String CURRENT_PAGE_NUMBER;
    private String PAGE_DATA_COUNT;
    private String TOTAL_PAGE_COUNT;
    private String TOTAL_DATA_COUNT;
    private String TOTAL_RECORDS;
}
