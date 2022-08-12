package cn.watermelonhit.paymentdemo.controller;

import cn.watermelonhit.paymentdemo.entity.OrderInfo;
import cn.watermelonhit.paymentdemo.enums.OrderStatus;
import cn.watermelonhit.paymentdemo.service.OrderInfoService;
import cn.watermelonhit.paymentdemo.vo.R;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author watermelonhit
 * @DateTime 2022/8/7
 */
@Api(tags = "商品订单管理")
@RestController
@RequestMapping("/api/order-info")
@Slf4j
public class OrderInfoController {

    @Autowired
    private OrderInfoService orderInfoService;

    @GetMapping("/list")
    public R getOrderInfoList() {
        List<OrderInfo> curList = this.orderInfoService.getOrderInfoListByCreateTimeDesc();
        return R.ok().add("list", curList);
    }

    /**
     *  前端定时查询订单支付状态
     * @param orderNo
     * @return
     */
    @GetMapping("/query-order-status/{orderNo}")
    public R queryOrderInfoStatus(@PathVariable String orderNo) {
        String orderStatus = this.orderInfoService.getOrderStatus(orderNo);
        if (OrderStatus.SUCCESS.getType().equals(orderStatus)) {
            log.info("反馈前端支付成功！");
            //支付成功
            return R.ok();
        }
        return R.ok().setCode(101).setMessage("支付中...");
    }
}
