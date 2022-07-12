package com.tsj.common.utils;

/**
 * ID生成器
 *
 * @author Frank
 */
public class IDGenerator {

    /**
     * 生成用户登录TOKEN
     *
     * @return
     */
    public static String makeAccessToken() {
        return SnowFlakeIdGenerator.generate();
    }

    /**
     * 生成对象ID
     *
     * @return
     */
    public static String makeId() {
        return SnowFlakeIdGenerator.generate();
    }

}
