package com.airelay.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),
    INSUFFICIENT_BALANCE(1001, "余额不足"),
    API_KEY_INVALID(1002, "API密钥无效"),
    CHANNEL_UNAVAILABLE(1003, "通道不可用"),
    RATE_LIMIT_EXCEEDED(1004, "请求频率超限"),
    SUBSCRIPTION_EXPIRED(1005, "订阅已过期"),
    COUPON_INVALID(1006, "优惠券无效"),
    ORDER_NOT_FOUND(1007, "订单不存在"),
    ORDER_STATUS_ERROR(1008, "订单状态错误"),
    PAYMENT_FAILED(1009, "支付失败");

    private final int code;
    private final String message;
}
