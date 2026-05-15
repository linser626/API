package com.airelay.payment.controller;

import com.airelay.common.Constants;
import com.airelay.common.Result;
import com.airelay.payment.dto.OrderVO;
import com.airelay.payment.dto.PayRequest;
import com.airelay.payment.entity.Order;
import com.airelay.payment.service.AlipayService;
import com.airelay.payment.service.PaymentService;
import com.airelay.payment.service.WechatPayService;
import com.airelay.security.SecurityUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "支付管理")
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final AlipayService alipayService;
    private final WechatPayService wechatPayService;

    @Operation(summary = "发起支付")
    @PostMapping("/api/payment/pay")
    public Result<Map<String, String>> pay(@Valid @RequestBody PayRequest request) {
        Order order = paymentService.getOrderByNo(request.getOrderNo());

        if (!order.getUserId().equals(SecurityUtils.getCurrentUserId())) {
            return Result.fail(ErrorCode.FORBIDDEN);
        }

        if (!"pending".equals(order.getPaymentStatus())) {
            return Result.fail(ErrorCode.PARAM_ERROR, "订单状态不正确");
        }

        Map<String, String> result = new HashMap<>();

        if ("alipay".equals(request.getPaymentMethod())) {
            String form = alipayService.createPayment(order);
            result.put("type", "form");
            result.put("content", form);
        } else if ("wechat".equals(request.getPaymentMethod())) {
            String codeUrl = wechatPayService.createNativePayment(order);
            result.put("type", "qrcode");
            result.put("content", codeUrl);
        } else {
            return Result.fail(ErrorCode.PARAM_ERROR, "不支持的支付方式");
        }

        return Result.ok(result);
    }

    @Operation(summary = "支付宝异步通知")
    @PostMapping("/api/payment/alipay/notify")
    public String handleAlipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });

        boolean success = alipayService.handleNotify(params);
        return success ? "success" : "fail";
    }

    @Operation(summary = "微信支付异步通知")
    @PostMapping("/api/payment/wechat/notify")
    public Map<String, String> handleWechatNotify(HttpServletRequest request) {
        boolean success = wechatPayService.handleNotify(request);

        Map<String, String> result = new HashMap<>();
        if (success) {
            result.put("code", "SUCCESS");
            result.put("message", "成功");
        } else {
            result.put("code", "FAIL");
            result.put("message", "失败");
        }
        return result;
    }

    @Operation(summary = "我的订单列表")
    @GetMapping("/api/payment/orders")
    public Result<IPage<OrderVO>> getUserOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "" + Constants.DEFAULT_PAGE_SIZE) int size) {
        if (size > Constants.MAX_PAGE_SIZE) {
            size = Constants.MAX_PAGE_SIZE;
        }
        return Result.ok(paymentService.getUserOrders(SecurityUtils.getCurrentUserId(), page, size));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/api/payment/orders/{orderNo}")
    public Result<OrderVO> getOrderDetail(@PathVariable String orderNo) {
        Order order = paymentService.getOrderByNo(orderNo);
        if (!order.getUserId().equals(SecurityUtils.getCurrentUserId())) {
            return Result.fail(ErrorCode.FORBIDDEN);
        }

        OrderVO orderVO = OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .type(order.getType())
                .amount(order.getAmount())
                .originalAmount(order.getOriginalAmount())
                .discountAmount(order.getDiscountAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .description(order.getDescription())
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .build();
        return Result.ok(orderVO);
    }

    @Operation(summary = "退款(管理员)")
    @PostMapping("/api/admin/payment/refund/{orderNo}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> refundOrder(@PathVariable String orderNo) {
        paymentService.refundOrder(orderNo);
        return Result.ok();
    }
}
