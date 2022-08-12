package cn.watermelonhit.paymentdemo.controller;

import cn.watermelonhit.paymentdemo.config.WxPayConfig;
import cn.watermelonhit.paymentdemo.vo.R;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author watermelonhit
 * @DateTime 2022/8/3
 */
@Api(tags = "测试控制器")
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Resource
    WxPayConfig wxPayConfig;

    @GetMapping("/getId")
    public R getData(){
        return R.ok().add("id", this.wxPayConfig.getAppid());
    }
}
