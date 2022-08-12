package cn.watermelonhit.paymentdemo.service;

import cn.watermelonhit.paymentdemo.entity.OrderInfo;
import cn.watermelonhit.paymentdemo.enums.OrderStatus;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface OrderInfoService extends IService<OrderInfo> {

    public OrderInfo createOrderInfoByProductId(Long productId,String paymentType);

    void saveCodeUrl(String orderNo, String codeUrl);

    List<OrderInfo> getOrderInfoListByCreateTimeDesc();

    void updateStatusByOrderNo(String orderNo, OrderStatus orderStatus);

    String getOrderStatus(String orderNo);

    List<OrderInfo> getNoPayOrderByDuration(int minutes,String paymentType);

    OrderInfo getOrderByOrderNo(String orderNo);
}
