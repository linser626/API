package com.airelay.payment.service;

import com.airelay.common.BusinessException;
import com.airelay.common.ErrorCode;
import com.airelay.payment.entity.Order;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class AlipayService {

    private final PaymentService paymentService;

    @Value("${alipay.app-id}")
    private String appId;

    @Value("${alipay.private-key}")
    private String privateKey;

    @Value("${alipay.public-key}")
    private String alipayPublicKey;

    @Value("${alipay.gateway}")
    private String gateway;

    @Value("${alipay.notify-url}")
    private String notifyUrl;

    @Value("${alipay.return-url}")
    private String returnUrl;

    @Value("${alipay.sign-type}")
    private String signType;

    @Value("${alipay.charset}")
    private String charset;

    @Value("${alipay.format}")
    private String format;

    private AlipayClient alipayClient;

    public AlipayService(@Lazy PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostConstruct
    public void init() {
        alipayClient = new DefaultAlipayClient(
                gateway,
                appId,
                privateKey,
                format,
                charset,
                alipayPublicKey,
                signType
        );
    }

    public String createPayment(Order order) {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(notifyUrl);
        request.setReturnUrl(returnUrl);

        String bizContent = String.format(
                "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"%s\",\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}",
                order.getOrderNo(),
                order.getAmount().toPlainString(),
                order.getDescription() != null ? order.getDescription() : "AI Relay Platform"
        );
        request.setBizContent(bizContent);

        try {
            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            log.error("创建支付宝PC支付失败: orderNo={}, error={}", order.getOrderNo(), e.getErrMsg(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建支付宝支付失败: " + e.getErrMsg());
        }
    }

    public String createWapPayment(Order order) {
        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        request.setNotifyUrl(notifyUrl);
        request.setReturnUrl(returnUrl);

        String bizContent = String.format(
                "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"%s\",\"product_code\":\"QUICK_WAP_WAY\"}",
                order.getOrderNo(),
                order.getAmount().toPlainString(),
                order.getDescription() != null ? order.getDescription() : "AI Relay Platform"
        );
        request.setBizContent(bizContent);

        try {
            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            log.error("创建支付宝WAP支付失败: orderNo={}, error={}", order.getOrderNo(), e.getErrMsg(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建支付宝WAP支付失败: " + e.getErrMsg());
        }
    }

    public boolean handleNotify(Map<String, String> params) {
        boolean signVerified = verifySign(params);
        if (!signVerified) {
            log.warn("支付宝通知验签失败: params={}", params);
            return false;
        }

        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            log.info("支付宝通知非成功状态: tradeStatus={}", tradeStatus);
            return false;
        }

        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");

        try {
            paymentService.handlePaymentSuccess(outTradeNo, tradeNo, "alipay");
            return true;
        } catch (Exception e) {
            log.error("处理支付宝通知失败: outTradeNo={}, error={}", outTradeNo, e.getMessage(), e);
            return false;
        }
    }

    public boolean verifySign(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(params, alipayPublicKey, charset, signType);
        } catch (AlipayApiException e) {
            log.error("支付宝验签异常: error={}", e.getErrMsg(), e);
            return false;
        }
    }
}
