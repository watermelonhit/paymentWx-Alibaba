package cn.watermelonhit.paymentdemo.service.impl;

import cn.watermelonhit.paymentdemo.entity.OrderInfo;
import cn.watermelonhit.paymentdemo.entity.RefundInfo;
import cn.watermelonhit.paymentdemo.enums.AliPayTradeState;
import cn.watermelonhit.paymentdemo.enums.OrderStatus;
import cn.watermelonhit.paymentdemo.enums.PayType;
import cn.watermelonhit.paymentdemo.service.AliPayService;
import cn.watermelonhit.paymentdemo.service.OrderInfoService;
import cn.watermelonhit.paymentdemo.service.PaymentInfoService;
import cn.watermelonhit.paymentdemo.service.RefundInfoService;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author watermelonhit
 * @DateTime 2022/8/9
 */
@Service
@Slf4j
public class AliPayServiceImpl implements AliPayService {
    @Resource
    private OrderInfoService orderInfoService;
    @Resource(name = "alipayClient")
    private AlipayClient alipayClient;

    @Resource
    private Environment config;

    @Resource
    private PaymentInfoService paymentInfoService;

    @Override
    @Transactional
    public String tradeCreate(Long productId) {
        try {
            log.info("生成订单！");
            OrderInfo orderInfo = this.orderInfoService.createOrderInfoByProductId(productId, PayType.ALIPAY.getType());
            //调用支付宝接口
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            //配置需要的公共请求参数
            //支付完成后，支付宝向谷粒学院发起异步通知的地址
            request.setNotifyUrl(this.config.getProperty("alipay.notify-url"));
            request.setReturnUrl(this.config.getProperty("alipay.return-url"));
            //组装当前业务方法的请求参数
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderInfo.getOrderNo());
            BigDecimal total = new BigDecimal(orderInfo.getTotalFee().toString()).divide(new BigDecimal("100"));
            bizContent.put("total_amount", total);
            bizContent.put("subject", orderInfo.getTitle());
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
            request.setBizContent(bizContent.toString());
            log.info("订单请求参数=====>{}", bizContent.toString());
            //执行请求，调用支付宝接口
            AlipayTradePagePayResponse response = this.alipayClient.pageExecute(request);
            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> " + response.getBody());
                return response.getBody();
            } else {
                log.info("调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());
                throw new RuntimeException("创建支付交易失败");
            }
        } catch (AlipayApiException | JSONException e) {
            e.printStackTrace();
            throw new RuntimeException("创建支付交易失败");
        }
    }

    private ReentrantLock lock;

    @Override
    public void processOrder(Map<String, String> params) {
        log.info("处理订单");
        if (this.lock.tryLock()) {
            try {
                //获取订单号
                String orderNo = params.get("out_trade_no");
                //接口调用的幂等性：无论接口被调用多少次，以下业务执行一次
                String orderStatus = this.orderInfoService.getOrderStatus(orderNo);
                if (!OrderStatus.NOTPAY.getType().equals(orderStatus)) {
                    return;
                }
                //更新订单状态
                this.orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
                //记录支付日志
                this.paymentInfoService.createPaymentInfoForAliPay(params);
            } finally {
                this.lock.unlock();
            }
        }
    }

    @Override
    public void cancelOrder(String orderNo) {
        //调用支付宝提供的统一收单交易关闭接口
        this.closeOrder(orderNo);
        //更新用户订单状态
        this.orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CANCEL);
    }

    /**
     * @param orderNo
     * @return 返回订单查询结果，如果返回null则表示支付宝端尚未创建订单
     */
    @Override
    public String queryOrder(String orderNo) {
        try {
            log.info("查单接口调用 ===> {}", orderNo);
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderNo);
            request.setBizContent(bizContent.toString());
            AlipayTradeQueryResponse response = this.alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> " + response.getBody());
                return response.getBody();
            } else {
                log.info("调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());
                return null;
            }
        } catch (AlipayApiException | JSONException e) {
            e.printStackTrace();
            throw new RuntimeException("支付宝查单接口的调用失败");
        }
    }

    @Override
    public void checkOrderStatus(String orderNo) {
        String queryOrder = this.queryOrder(orderNo);
        // 未支付且支付宝平台未创建订单
        if (queryOrder == null) {
            log.warn("核实订单未创建 ===> {}", orderNo);
            //更新本地订单状态
            this.orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
            return;
        }
        Gson gson = new Gson();
        HashMap<String, LinkedTreeMap> resultMap = gson.fromJson(queryOrder, HashMap.class);
        LinkedTreeMap response = resultMap.get("alipay_trade_query_response");
        String tradeStatus = (String) response.get("trade_status");
        if (AliPayTradeState.NOTPAY.getType().equals(tradeStatus)) {
            log.warn("核实订单未支付(支付宝)=====>{}", orderNo);
            // 调用关单接口
            this.closeOrder(orderNo);
            // 更新订单状态
            this.orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
            return;
        }
        if (AliPayTradeState.SUCCESS.getType().equals(tradeStatus)) {
            log.warn("核实订单未支付(支付宝)=====>{}", orderNo);
            // 更新订单状态
            this.orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
            // 插入日志
            this.paymentInfoService.createPaymentInfoForAliPay(response);
        }


    }

    @Resource
    private RefundInfoService refundsInfoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(String orderNo, String reason) {
        try {
            log.info("调用退款API");
            //创建退款单
            RefundInfo refundInfo = this.refundsInfoService.createRefundByOrderNoForAliPay(orderNo, reason);
            //调用统一收单交易退款接口
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            //组装当前业务方法的请求参数
            JSONObject bizContent = new JSONObject();
            //订单编号
            bizContent.put("out_trade_no", orderNo);
            BigDecimal refund = new BigDecimal(refundInfo.getRefund().toString()).divide(new BigDecimal("100"));
            bizContent.put("refund_amount", refund);//退款金额：不能大于支付金额
            bizContent.put("refund_reason", reason);//退款原因(可选)
            request.setBizContent(bizContent.toString());
            //执行请求，调用支付宝接口
            AlipayTradeRefundResponse response = this.alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> " + response.getBody());
                //更新订单状态
                this.orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_SUCCESS);
                //更新退款单
                this.refundsInfoService.updateRefundForAliPay(refundInfo.getRefundNo(), response.getBody(), AliPayTradeState.REFUND_SUCCESS.getType()); //退款成功
            } else {
                log.info("调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());
                //更新订单状态
                this.orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_ABNORMAL);
                //更新退款单
                this.refundsInfoService.updateRefundForAliPay(refundInfo.getRefundNo(), response.getBody(), AliPayTradeState.REFUND_ERROR.getType()); //退款失败
            }
        } catch (AlipayApiException | JSONException e) {
            e.printStackTrace();
            throw new RuntimeException("创建退款申请失败");
        }
    }

    @Override
    public String queryRefund(String orderNo) {
        log.info("查询退款接口调用 ===> {}", orderNo);
        try {
            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderNo);
            bizContent.put("out_request_no", orderNo);
            request.setBizContent(bizContent.toString());
            AlipayTradeFastpayRefundQueryResponse response = this.alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> " + response.getBody());
                return response.getBody();
            } else {
                log.info("调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());
                //throw new RuntimeException("查单接口的调用失败");
                return null;//订单不存在
            }
        } catch (JSONException | AlipayApiException e) {
            e.printStackTrace();
            throw new RuntimeException("查单接口的调用失败");
        }
    }

    @Override
    public String queryBill(String billDate, String type) {
        try {
            AlipayDataDataserviceBillDownloadurlQueryRequest request = new AlipayDataDataserviceBillDownloadurlQueryRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("bill_type", type);
            bizContent.put("bill_date", billDate);
            request.setBizContent(bizContent.toString());
            AlipayDataDataserviceBillDownloadurlQueryResponse response = this.alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> " + response.getBody());
                //获取账单下载地址
                Gson gson = new Gson();
                HashMap<String, LinkedTreeMap> resultMap = gson.fromJson(response.getBody(), HashMap.class);
                LinkedTreeMap billDownloadurlResponse = resultMap.get("alipay_data_dataservice_bill_downloadurl_query_response");
                String billDownloadUrl = (String) billDownloadurlResponse.get("bill_download_url");
                return billDownloadUrl;
            } else {
                log.info("调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());
                throw new RuntimeException("申请账单失败");
            }
        } catch (AlipayApiException | JSONException e) {
            e.printStackTrace();
            throw new RuntimeException("申请账单失败");
        }
    }

    private void closeOrder(String orderNo) {
        try {
            log.info("关单接口的调用，订单号 ===> {}", orderNo);
            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderNo);
            request.setBizContent(bizContent.toString());
            AlipayTradeCloseResponse response = this.alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> " + response.getBody());
            } else {
                log.info("调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());
            }
        } catch (AlipayApiException | JSONException e) {
            e.printStackTrace();
            throw new RuntimeException("关单接口的调用失败");
        }
    }

}
