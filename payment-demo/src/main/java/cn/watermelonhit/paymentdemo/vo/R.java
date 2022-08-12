package cn.watermelonhit.paymentdemo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author watermelonhit
 * @DateTime 2022/8/2
 */
@Data
@ToString
@Accessors(chain = true) // set方法返回结果为对象本身
public class R {

    private Integer code;
    private String message;
    private Map<String,Object> data=new HashMap<>();

    public static R ok(){
        R r = new R();
        r.code=200;
        r.message="ok";
        return r;
    }

    public static R error(){
        R r = new R();
        r.code=500;
        r.message="error";
        return r;
    }

    /**
     *  为对象填充数据
     * @param msg
     * @param data
     * @return
     */
    public R add(String msg,Object data){
        this.data.put(msg,data);
        return this;
    }

}
