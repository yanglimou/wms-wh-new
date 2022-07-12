package com.tsj.api.vanx.vo.main;

import lombok.Data;

/**
 * 接口[BD201]出参
 *
 * @author honesty
 * @date 2021/10/2214:03
 */
@Data
public class PageRequestMain {
    private String BEGIN_TIME;
    private String END_TIME;
    private String CURRENT_PAGE_NUMBER;
    private String PAGE_DATA_COUNT;
}
