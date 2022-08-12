package cn.watermelonhit.paymentdemo.task;

import cn.watermelonhit.paymentdemo.entity.OrderInfo;
import cn.watermelonhit.paymentdemo.enums.PayType;
import cn.watermelonhit.paymentdemo.service.AliPayService;
import cn.watermelonhit.paymentdemo.service.OrderInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author watermelonhit
 * @DateTime 2022/8/11
 */
@Component
@Slf4j
public class AliPayTask {

    @Resource
    private OrderInfoService orderInfoService;
    @Resource
    private AliPayService aliPayService;

    /**
     * 60s定时查询早期订单状态
     * @throws Exception
     */
    @Scheduled(cron = "0/60 * * * * ?")
    public void orderConfirm() throws Exception {
        log.info("orderConfirm(支付宝) 被执行......");
        List<OrderInfo> orderInfoList = this.orderInfoService.getNoPayOrderByDuration(5, PayType.ALIPAY.getType());
        for (OrderInfo orderInfo : orderInfoList) {
            String orderNo = orderInfo.getOrderNo();
            log.warn("超时订单 ===> {}", orderNo);
            //核实订单状态：调用微信支付查单接口
            this.aliPayService.checkOrderStatus(orderNo);
        }
    }
}
