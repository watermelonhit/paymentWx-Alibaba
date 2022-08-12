package cn.watermelonhit.paymentdemo.task;

import cn.watermelonhit.paymentdemo.entity.OrderInfo;
import cn.watermelonhit.paymentdemo.entity.RefundInfo;
import cn.watermelonhit.paymentdemo.enums.PayType;
import cn.watermelonhit.paymentdemo.service.OrderInfoService;
import cn.watermelonhit.paymentdemo.service.RefundInfoService;
import cn.watermelonhit.paymentdemo.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author watermelonhit
 * @DateTime 2022/8/9
 */
@Component
@Slf4j
public class WxPayTask {

    /**
     * 测试
     * (cron="秒 分 时 日 月 周")
     * 每隔一秒执行
     * 0/3：从第0秒开始，每隔3秒执行一次
     * 1-3: 从第1秒开始执行，到第3秒结束执行
     * 1,2,3：第1、2、3秒执行
     * ?：不指定，若指定日期，则不指定周，反之同理
     */
//    @Scheduled(cron = "0/3 * * * * ?")
//    public void task1() {
//        log.info("task1 执行");
//    }

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private WxPayService wxPayService;

    /**
     * 60s定时查询早期订单状态
     * @throws Exception
     */
    @Scheduled(cron = "0/60 * * * * ?")
    public void orderConfirm() throws Exception {
        log.info("orderConfirm 被执行......");
        List<OrderInfo> orderInfoList = this.orderInfoService.getNoPayOrderByDuration(5, PayType.WXPAY.getType());
        for (OrderInfo orderInfo : orderInfoList) {
            String orderNo = orderInfo.getOrderNo();
            log.warn("超时订单 ===> {}", orderNo);
            //核实订单状态：调用微信支付查单接口
            this.wxPayService.checkOrderStatus(orderNo);
        }
    }

    @Resource
    private RefundInfoService refundInfoService;

    /**
     * 从第0秒开始每隔30秒执行1次，查询创建超过5分钟，并且未成功的退款单
     */
    @Scheduled(cron = "0/60 * * * * ?")
    public void refundConfirm() throws Exception {
        log.info("refundConfirm 被执行......");

        //找出申请退款超过5分钟并且未成功的退款单
        List<RefundInfo> refundInfoList = this.refundInfoService.getNoRefundOrderByDuration(1);

        for (RefundInfo refundInfo : refundInfoList) {
            String refundNo = refundInfo.getRefundNo();
            log.warn("超时未退款的退款单号 ===> {}", refundNo);

            //核实订单状态：调用微信支付查询退款接口
            this.wxPayService.checkRefundStatus(refundNo);
        }
    }
}
