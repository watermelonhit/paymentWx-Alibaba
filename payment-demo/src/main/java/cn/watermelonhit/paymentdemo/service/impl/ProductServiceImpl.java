package cn.watermelonhit.paymentdemo.service.impl;

import cn.watermelonhit.paymentdemo.entity.Product;
import cn.watermelonhit.paymentdemo.mapper.ProductMapper;
import cn.watermelonhit.paymentdemo.service.ProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

}
