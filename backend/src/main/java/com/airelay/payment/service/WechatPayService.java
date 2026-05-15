package com.airelay.payment.service;

import com.airelay.common.BusinessException;
import com.airelay.common.ErrorCode;
import com.airelay.payment.entity.Order;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class WechatPayService {

    private final PaymentService paymentService;

    @Value("${wechat-pay.app-id}")
    private String appId;

    @Value("${wechat-pay.mch-id}")
    private String mchId;

    @Value("${wechat-pay.api-v3-key}")
    private String apiV3Key;

    @Value("${wechat-pay.private-key-path}")
    private String privateKeyPath;

    @Value("${wechat-pay.cert-serial-no}")
    private String certSerialNo;

    @Value("${wechat-pay.notify-url}")
    private String notifyUrl;

    private Config config;
    private NativePayService nativePayService;
    private JsapiService jsapiService;
    private NotificationParser notificationParser;

    public WechatPayService(@Lazy PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostConstruct
    public void init() {
        try {
            config = new RSAAutoCertificateConfig.Builder()
                    .merchantId(mchId)
                    .privateKeyFromPath(privateKeyPath)
                    .merchantSerialNumber(certSerialNo)
                    .apiV3Key(apiV3Key)
                    .build();

            nativePayService = new NativePayService.Builder().config(config).build();
            jsapiService = new JsapiService.Builder().config(config).build();
            notificationParser = new NotificationParser(config);
        } catch (Exception e) {
            log.error("微信支付初始化失败: {}", e.getMessage(), e);
        }
    }

    public String createNativePayment(Order order) {
        PrepayRequest request = new PrepayRequest();
        com.wechat.pay.java.service.payments.nativepay.model.Order orderRequest =
                new com.wechat.pay.java.service.payments.nativepay.model.Order();
        orderRequest.setAppid(appId);
        orderRequest.setMchid(mchId);
        orderRequest.setDescription(order.getDescription() != null ? order.getDescription() : "AI Relay Platform");
        orderRequest.setOutTradeNo(order.getOrderNo());
        orderRequest.setNotifyUrl(notifyUrl);

        Amount amount = new Amount();
        amount.setTotal(order.getAmount().multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).longValue());
        amount.setCurrency("CNY");
        orderRequest.setAmount(amount);

        request.setOrder(orderRequest);

        try {
            PrepayResponse response = nativePayService.prepay(request);
            return response.getCodeUrl();
        } catch (Exception e) {
            log.error("创建微信Native支付失败: orderNo={}, error={}", order.getOrderNo(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建微信支付失败: " + e.getMessage());
        }
    }

    public Map<String, String> createJsapiPayment(Order order, String openid) {
        com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest request =
                new com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest();
        com.wechat.pay.java.service.payments.jsapi.model.Order orderRequest =
                new com.wechat.pay.java.service.payments.jsapi.model.Order();
        orderRequest.setAppid(appId);
        orderRequest.setMchid(mchId);
        orderRequest.setDescription(order.getDescription() != null ? order.getDescription() : "AI Relay Platform");
        orderRequest.setOutTradeNo(order.getOrderNo());
        orderRequest.setNotifyUrl(notifyUrl);

        com.wechat.pay.java.service.payments.jsapi.model.Amount amount =
                new com.wechat.pay.java.service.payments.jsapi.model.Amount();
        amount.setTotal(order.getAmount().multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).longValue());
        amount.setCurrency("CNY");
        orderRequest.setAmount(amount);

        Payer payer = new Payer();
        payer.setOpenid(openid);
        orderRequest.setPayer(payer);

        request.setOrder(orderRequest);

        try {
            com.wechat.pay.java.service.payments.jsapi.model.PrepayResponse response =
                    jsapiService.prepay(request);
            Map<String, String> result = new HashMap<>();
            result.put("prepayId", response.getPrepayId());
            result.put("appId", appId);
            result.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
            result.put("nonceStr", UUID.randomUUID().toString().replace("-", ""));
            result.put("package", "prepay_id=" + response.getPrepayId());
            result.put("signType", "RSA");
            return result;
        } catch (Exception e) {
            log.error("创建微信JSAPI支付失败: orderNo={}, error={}", order.getOrderNo(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建微信JSAPI支付失败: " + e.getMessage());
        }
    }

    public boolean handleNotify(HttpServletRequest httpRequest) {
        try {
            String timestamp = httpRequest.getHeader("Wechatpay-Timestamp");
            String nonce = httpRequest.getHeader("Wechatpay-Nonce");
            String signature = httpRequest.getHeader("Wechatpay-Signature");
            String serial = httpRequest.getHeader("Wechatpay-Serial");

            StringBuilder bodyBuilder = new StringBuilder();
            try (BufferedReader reader = httpRequest.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    bodyBuilder.append(line);
                }
            }
            String body = bodyBuilder.toString();

            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serial)
                    .nonce(nonce)
                    .timestamp(timestamp)
                    .signature(signature)
                    .body(body)
                    .build();

            Transaction transaction = notificationParser.parse(requestParam,
                    new TypeReference<Transaction>() {});

            if (Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState())) {
                String outTradeNo = transaction.getOutTradeNo();
                String transactionId = transaction.getTransactionId();
                paymentService.handlePaymentSuccess(outTradeNo, transactionId, "wechat");
                return true;
            }

            log.info("微信支付通知非成功状态: tradeState={}", transaction.getTradeState());
            return false;
        } catch (Exception e) {
            log.error("处理微信支付通知失败: error={}", e.getMessage(), e);
            return false;
        }
    }

    public Map<String, Object> queryOrder(String orderNo) {
        try {
            com.wechat.pay.java.service.payments.query.QueryOrderService queryOrderService =
                    new com.wechat.pay.java.service.payments.query.QueryOrderService.Builder().config(config).build();

            com.wechat.pay.java.service.payments.query.model.QueryOrderByOutTradeNoRequest queryRequest =
                    new com.wechat.pay.java.service.payments.query.model.QueryOrderByOutTradeNoRequest();
            queryRequest.setMchid(mchId);
            queryRequest.setOutTradeNo(orderNo);

            Transaction transaction = queryOrderService.queryOrderByOutTradeNo(queryRequest);

            Map<String, Object> result = new HashMap<>();
            result.put("orderNo", orderNo);
            result.put("tradeState", transaction.getTradeState().name());
            result.put("transactionId", transaction.getTransactionId());
            result.put("tradeStateDesc", transaction.getTradeStateDesc());
            return result;
        } catch (Exception e) {
            log.error("查询微信支付订单失败: orderNo={}, error={}", orderNo, e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "查询微信支付订单失败: " + e.getMessage());
        }
    }
}
