package cn.watermelonhit.paymentdemo.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @Author watermelonhit
 * @DateTime 2022/8/3
 */
@Configuration
@EnableTransactionManagement
@MapperScan("cn.watermelonhit.paymentdemo.mapper")
public class MyBatisPlusConfig {
}
