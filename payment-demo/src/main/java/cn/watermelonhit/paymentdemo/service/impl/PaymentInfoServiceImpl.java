package cn.watermelonhit.paymentdemo.service.impl;


import cn.watermelonhit.paymentdemo.entity.PaymentInfo;
import cn.watermelonhit.paymentdemo.enums.PayType;
import cn.watermelonhit.paymentdemo.mapper.PaymentInfoMapper;
import cn.watermelonhit.paymentdemo.service.PaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    @Override
    public void createPaymentInfo(String plainText) {
        Gson gson = new Gson();
        Map<String, Object> plainTextMap = gson.fromJson(plainText, HashMap.class);
        String orderNo = (String) plainTextMap.get("out_trade_no");
        String transactionId = (String) plainTextMap.get("transaction_id");
        String tradeType = (String) plainTextMap.get("trade_type");
        String tradeState = (String) plainTextMap.get("trade_state");
        Map<String, Object> amount = (Map) plainTextMap.get("amount");
        Integer payerTotal = ((Double) amount.get("payer_total")).intValue();
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPaymentType(PayType.WXPAY.getType());
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setTradeType(tradeType);
        paymentInfo.setTradeState(tradeState);
        paymentInfo.setPayerTotal(payerTotal);
        paymentInfo.setContent(plainText);
        // 将数据插入数据库中
        this.baseMapper.insert(paymentInfo);
    }

    @Override
    public void createPaymentInfoForAliPay(Map<String, String> params) {
        log.info("记录支付日志");
        //获取订单号
        String orderNo = params.get("out_trade_no");
        //业务编号
        String transactionId = params.get("trade_no");
        //交易状态
        String tradeStatus = params.get("trade_status");
        // 交易金额
        String totalAmount = params.get("total_amount");
        int totalAmountInt = new BigDecimal(totalAmount).multiply(new BigDecimal("100")).intValue();
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPaymentType(PayType.ALIPAY.getType());
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setTradeType("电脑网站支付");
        paymentInfo.setTradeState(tradeStatus);
        paymentInfo.setPayerTotal(totalAmountInt);
        Gson gson = new Gson();
        String json = gson.toJson(params, HashMap.class);
        paymentInfo.setContent(json);
        this.baseMapper.insert(paymentInfo);
    }
}
