package com.tsj.common.constant;

/**
 * API 统一返回状态码
 */
public enum ResultCode {
    /* 成功状态码 */
    SUCCESS(0, "OK"),

    /* 令牌失效 */
    TOKEN_INVALID(401, "令牌失效"),

    SERVER_ERROR(400, "服务器错误"),

    /*参数错误 10001-19999 */
    PARAM_IS_INVALID(10001, "参数无效"),
    PARAM_IS_BLANK(10002, "参数为空"),
    PARAM_TYPE_BIND_ERROR(10003, "参数类型错误"),
    PARAM_NOT_COMPLETE(10004, "参数缺失"),
    PARAM_OUT_LIMIT(10005, "参数取值超过范围"),

    /* 用户错误：20001-29999*/
    USER_NOT_LOGGED_IN(20001, "用户未登录"),
    USER_LOGIN_ERROR(20002, "密码错误"),
    USER_ACCOUNT_FORBIDDEN(20003, "账号未激活"),
    USER_NOT_EXIST(20004, "用户不存在"),
    USER_HAS_EXISTED(20005, "用户已存在"),
    Cert_HAS_EXISTED(20006, "认证已存在"),
    SAFE_PASSWORD_ERROR(20007, "安全密码错误"),
    Captcha_ERROR(20008, "验证码错误"),

    /* 业务错误：30001-39999 */
    CREATE_FAIL(30001, "保存失败"),
    DELETE_FAIL(30002, "删除失败"),
    UPDATE_FAIL(30003, "修改失败"),
    ORDER_NOT_EXIST(30010, "单据不存在"),
    ORDER_CODE_ALREADY_EXIST(30011, "单据号已存在，请重新输入"),
    ORDER_ALREADY_COMPLETE(30012, "单据已完成，请勿修改"),
    TAG_NOT_VALID(30020, "检测到无效标签"),
    STOCK_NOT_EXIST(30030, "库存信息不存在"),
    PRINTER_URL_NOT_EXIST(30031, "当前账户未配置打印机，请切换到其他账户"),
    PRINTER_FAIL(30032, "打印失败"),

    /* 系统错误：40001-49999 */
    SYSTEM_INNER_ERROR(40001, "系统繁忙，请稍后重试"),
    SYSTEM_SPD_BUSY(40002, "系统正在同步SPD数据，请勿重复操作"),

    /* 数据错误：50001-599999 */
    DATA_ALREADY_EXISTED(50001, "数据已存在"),
    DATA_NOT_EXISTED(50002, "数据未找到"),
    DATA_IS_INVALID(50003, "数据不合法"),
    DATA_IS_LOCKED(50004, "数据不支持修改"),
    DATA_IS_BLANK(50005, "有效数据为空"),

    /* 接口错误：60001-69999 */
    INTERFACE_INNER_INVOKE_ERROR(60001, "内部系统接口调用异常"),
    INTERFACE_OUTER_INVOKE_ERROR(60002, "外部系统接口调用异常"),
    INTERFACE_FORBID_VISIT(60003, "该接口禁止访问"),
    INTERFACE_ADDRESS_INVALID(60004, "接口地址无效"),
    INTERFACE_REQUEST_TIMEOUT(60005, "接口请求超时"),
    INTERFACE_EXCEED_LOAD(60006, "接口负载过高"),

    /* 权限错误：70001-79999 */
    PERMISSION_ERROR(70001, "缺少权限"),

    /* 文件错误：80001-899999 */
    FILE_NOT_EXISTED(80001, "文件未找到"),
    FILE_IS_WRONG(80002, "文件有误"),
    FILE_ALREADY_EXISTED(80003, "文件已存在"),
    FILE_UPLOAD_FAIL(80004, "文件上传失败"),
    FILE_CREATE_FAIL(80005, "文件创建失败"),
    RESULT_PRINTER_NONE(80006, "打印机未找到"),

    /* 其他错误：90001-999999 */
    CAMERA_NOT_CONFIG(90001, "摄像头未配置");


    private int code;
    private String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return this.name();
    }

}
