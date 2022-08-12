package cn.watermelonhit.paymentdemo.controller;

import cn.watermelonhit.paymentdemo.service.WxPayService;
import cn.watermelonhit.paymentdemo.util.HttpUtils;
import cn.watermelonhit.paymentdemo.util.WechatPay2ValidatorForRequest;
import cn.watermelonhit.paymentdemo.vo.R;
import com.google.gson.Gson;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.ValidationException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author watermelonhit
 * @DateTime 2022/8/4
 */
@RestController
@RequestMapping("/api/wx-pay")
@Api(tags = "网站网站支付API")
@Slf4j
public class WxPayController {

    @Autowired
    private WxPayService wxPayService;

    @Autowired
    private Verifier verifier;


    @ApiOperation("调用统一下单API，生成支付二维码")
    @PostMapping("native/{productId}")
    public R nativePay(@PathVariable Long productId) {
        log.info("发起支付请求！");
        // 返回支付二维码链接和订单号
        Map<String, Object> map = null;
        try {
            map = this.wxPayService.nativePay(productId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.ok().setData(map);
    }

    @ApiOperation("接受微信回馈的订单支付通知")
    @PostMapping("/native/notify")
    public String nativeNotify(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        Gson gson = new Gson();
        HashMap<String, String> map = new HashMap<>();
        // 处理通知参数
        String body = HttpUtils.readData(httpServletRequest);
        HashMap<String, Object> bodyMap = gson.fromJson(body, HashMap.class);
        String requestId = (String) bodyMap.get("id");
        log.info("支付通知的id =====>{}", requestId);
        log.info("支付通知的完整数据 =====>{}", body);


        try {
            // todo: 签名的验签
            WechatPay2ValidatorForRequest validator = new WechatPay2ValidatorForRequest(this.verifier, requestId, body);
            if (!validator.validate(httpServletRequest)) {
                log.error("通知验签失败");
                throw new ValidationException("验签异常");
            }
            log.info("微信支付回馈验签成功！");

            // todo:订单结果处理
            this.wxPayService.processOrder(bodyMap);
        } catch (GeneralSecurityException | ValidationException e) {
            //失败应答
            httpServletResponse.setStatus(500);
            map.put("code", "ERROR");
            map.put("message", "微信支付回馈通知验签失败");
            return gson.toJson(map);
        }

        //成功应答：成功应答必须为200或204，否则就是失败应答
        httpServletResponse.setStatus(200);
        map.put("code", "SUCCESS");
        map.put("message", "成功");
        return gson.toJson(map);
    }

    @ApiOperation("用户取消订单")
    @PostMapping("/cancel/{orderNo}")
    public R cancel(@PathVariable String orderNo) throws IOException {
        log.info("用户取消订单");
        this.wxPayService.cancelOrder(orderNo);
        return R.ok().setMessage("订单已取消");
    }

    @ApiOperation("查询订单：测试订单状态用")
    @GetMapping("query/{orderNo}")
    public R queryOrder(@PathVariable String orderNo) throws Exception {
        log.info("查询订单");
        String bodyAsString = this.wxPayService.queryOrder(orderNo);
        return R.ok().setMessage("查询成功").add("bodyAsString", bodyAsString);
    }

    @ApiOperation("申请退款")
    @PostMapping("/refunds/{orderNo}/{reason}")
    public R refunds(@PathVariable String orderNo, @PathVariable String reason) throws Exception {

        log.info("申请退款");
        this.wxPayService.refund(orderNo, reason);
        return R.ok();
    }

    /**
     * 查询退款
     * @param refundNo
     * @return
     * @throws Exception
     */
    @ApiOperation("查询退款：测试用")
    @GetMapping("/query-refund/{refundNo}")
    public R queryRefund(@PathVariable String refundNo) throws Exception {
        log.info("查询退款");
        String result = this.wxPayService.queryRefund(refundNo);
        return R.ok().setMessage("查询成功").add("result", result);
    }


    /**
     * 退款结果通知
     * 退款状态改变后，微信会把相关退款结果发送给商户。
     */
    @ApiOperation("退款结果通知")
    @PostMapping("/refunds/notify")
    public String refundsNotify(HttpServletRequest request, HttpServletResponse response){

        log.info("退款通知执行");
        Gson gson = new Gson();
        Map<String, String> map = new HashMap<>();//应答对象

        try {
            //处理通知参数
            String body = HttpUtils.readData(request);
            Map<String, Object> bodyMap = gson.fromJson(body, HashMap.class);
            String requestId = (String)bodyMap.get("id");
            log.info("支付通知的id ===> {}", requestId);

            //签名的验证
            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest
                    = new WechatPay2ValidatorForRequest(this.verifier, requestId, body);
            if(!wechatPay2ValidatorForRequest.validate(request)){

                log.error("通知验签失败");
                //失败应答
                response.setStatus(500);
                map.put("code", "ERROR");
                map.put("message", "通知验签失败");
                return gson.toJson(map);
            }
            log.info("通知验签成功");

            //处理退款单
            this.wxPayService.processRefund(bodyMap);

            //成功应答
            response.setStatus(200);
            map.put("code", "SUCCESS");
            map.put("message", "成功");
            return gson.toJson(map);

        } catch (Exception e) {
            e.printStackTrace();
            //失败应答
            response.setStatus(500);
            map.put("code", "ERROR");
            map.put("message", "失败");
            return gson.toJson(map);
        }
    }

    @ApiOperation("获取账单url：测试用")
    @GetMapping("/querybill/{billDate}/{type}")
    public R queryTradeBill(
            @PathVariable String billDate,
            @PathVariable String type) throws Exception {

        log.info("获取账单url");

        String downloadUrl = this.wxPayService.queryBill(billDate, type);
        return R.ok().setMessage("获取账单url成功").add("downloadUrl", downloadUrl);
    }

    @ApiOperation("下载账单")
    @GetMapping("/downloadbill/{billDate}/{type}")
    public R downloadBill(
            @PathVariable String billDate,
            @PathVariable String type) throws Exception {

        log.info("下载账单");
        String result = this.wxPayService.downloadBill(billDate, type);

        return R.ok().add("result", result);
    }
}
