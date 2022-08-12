package cn.watermelonhit.paymentdemo.util;

import java.util.Date;

/**
 * @Author watermelonhit
 * @DateTime 2022/8/7
 */
public class TimeUtils {

    /**
     *  判断某资源是否在有效期内
     * @param now
     * @param pre
     * @param hour
     * @return
     */
    public static boolean timeIsValid(Date now,Date pre,Integer hour){
        long t1 = now.getTime();
        long t2 = pre.getTime();
        if(t1-t2>hour*60*60*1000){
            return false;
        }
        return true;
    }
}
