package cn.watermelonhit.paymentdemo;

import cn.watermelonhit.paymentdemo.config.WxPayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

@SpringBootTest
class PaymentDemoApplicationTests {

    @Resource
    private WxPayConfig wxPayConfig;

    @Test
    void testGetPrivateKey() {

        String privateKeyPath = this.wxPayConfig.getPrivateKeyPath();

        System.out.println(this.wxPayConfig.getPrivateKey(privateKeyPath));
    }



}
