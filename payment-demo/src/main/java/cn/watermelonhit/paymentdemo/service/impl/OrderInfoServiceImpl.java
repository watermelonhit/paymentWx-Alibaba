package cn.watermelonhit.paymentdemo.service.impl;


import cn.watermelonhit.paymentdemo.entity.OrderInfo;
import cn.watermelonhit.paymentdemo.entity.PaymentInfo;
import cn.watermelonhit.paymentdemo.entity.Product;
import cn.watermelonhit.paymentdemo.enums.OrderStatus;
import cn.watermelonhit.paymentdemo.mapper.OrderInfoMapper;
import cn.watermelonhit.paymentdemo.mapper.ProductMapper;
import cn.watermelonhit.paymentdemo.service.OrderInfoService;
import cn.watermelonhit.paymentdemo.util.OrderNoUtils;
import cn.watermelonhit.paymentdemo.util.TimeUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Override
    public OrderInfo createOrderInfoByProductId(Long productId,String paymentType) {
        Product product = this.productMapper.selectById(productId);
        // 查询是否已经提交但是未支付的订单
        OrderInfo orderInfo = this.getNoPayOrderByProductId(productId,paymentType);
        if (orderInfo != null && TimeUtils.timeIsValid(new Date(System.currentTimeMillis()), orderInfo.getCreateTime(), 2)) {
            return orderInfo;
        }
        // 生成订单
        orderInfo = new OrderInfo();
        orderInfo.setTitle(product.getTitle())
                .setOrderNo(OrderNoUtils.getOrderNo())
                .setProductId(productId)
                .setTotalFee(product.getPrice()) // 价格，单位是分
                .setOrderStatus(OrderStatus.NOTPAY.getType())
                .setPaymentType(paymentType);
        // 插入数据库
        this.orderInfoMapper.insert(orderInfo);
        return orderInfo;
    }

    /**
     * 将二维码保存到订单数据库中
     *
     * @param orderNo
     * @param codeUrl
     */
    @Override
    public void saveCodeUrl(String orderNo, String codeUrl) {
        UpdateWrapper<OrderInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCodeUrl(codeUrl);
        this.orderInfoMapper.update(orderInfo, updateWrapper);
    }

    @Override
    public List<OrderInfo> getOrderInfoListByCreateTimeDesc() {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<OrderInfo>().orderByDesc("create_time");
        return this.orderInfoMapper.selectList(queryWrapper);
    }

    /**
     * 用户成功支付后，修改订单支付状态
     *
     * @param orderNo
     * @param orderStatus
     */
    @Override
    public void updateStatusByOrderNo(String orderNo, OrderStatus orderStatus) {
        UpdateWrapper<OrderInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderStatus(orderStatus.getType());
        this.orderInfoMapper.update(orderInfo, updateWrapper);
    }

    @Override
    public String getOrderStatus(String orderNo) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);
        OrderInfo orderInfo = this.baseMapper.selectOne(queryWrapper);
        //防止被删除的订单的回调通知的调用
        if (orderInfo == null) {
            return null;
        }
        return orderInfo.getOrderStatus();
    }

    @Override
    public List<OrderInfo> getNoPayOrderByDuration(int minutes,String paymentType) {
        //minutes分钟之前的时间
        LocalDateTime preDateTime = LocalDateTime.now().minusMinutes(minutes);
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_status", OrderStatus.NOTPAY.getType())
                        .eq("payment_type",paymentType);
        queryWrapper.le("create_time", preDateTime);
        List<OrderInfo> orderInfoList = this.baseMapper.selectList(queryWrapper);
        return orderInfoList;
    }

    @Override
    public OrderInfo getOrderByOrderNo(String orderNo) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no",orderNo);
        return this.orderInfoMapper.selectOne(queryWrapper);
    }

    /**
     * 防止由于用户重复提交点击支付而生成未支付订单
     *
     * @param productId
     * @return
     */
    private OrderInfo getNoPayOrderByProductId(Long productId,String paymentType) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId)
                .eq("order_status", OrderStatus.NOTPAY.getType())
                .eq("payment_Type",paymentType);
        return this.orderInfoMapper.selectOne(queryWrapper);
    }

}
