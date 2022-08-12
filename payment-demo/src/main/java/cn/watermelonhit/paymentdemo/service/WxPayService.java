package cn.watermelonhit.paymentdemo.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author watermelonhit
 * @DateTime 2022/8/4
 */
public interface WxPayService {
    Map<String, Object> nativePay(Long productId) throws Exception;

    void processOrder(HashMap<String, Object> bodyMap) throws GeneralSecurityException;

    void cancelOrder(String orderNo) throws IOException;

    String queryOrder(String orderNo) throws IOException;

    void checkOrderStatus(String orderNo) throws IOException;

    void refund(String orderNo, String reason) throws Exception;

    String queryRefund(String refundNo) throws Exception;

    void processRefund(Map<String, Object> bodyMap) throws Exception;

    void checkRefundStatus(String refundNo)  throws Exception;

    String queryBill(String billDate, String type) throws Exception;

    String downloadBill(String billDate, String type) throws Exception;

}
