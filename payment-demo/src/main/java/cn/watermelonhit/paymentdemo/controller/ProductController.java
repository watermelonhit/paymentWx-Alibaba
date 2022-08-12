package cn.watermelonhit.paymentdemo.controller;

import cn.watermelonhit.paymentdemo.entity.Product;
import cn.watermelonhit.paymentdemo.service.ProductService;
import cn.watermelonhit.paymentdemo.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * @Author watermelonhit
 * @DateTime 2022/8/2
 */
@Api(tags="商品管理")
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @ApiOperation("测试接口")
    @GetMapping("/test")
    public R test(){
        return  R.ok().add("time", new Date()).add("des", "test");
    }

    @ApiOperation("商品列表接口")
    @GetMapping("/list")
    public R getList(){
        List<Product> list = this.productService.list();
        return R.ok().add("productList",list);
    }
}
