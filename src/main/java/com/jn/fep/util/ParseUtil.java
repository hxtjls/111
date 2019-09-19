package com.jn.fep.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hxt
 * @create 2019-07-16 9:50
 * @description 字节数组与16进制字符串互转
 * @version 1.0
 */
public class ParseUtil {

    /**
     * 字节数组转换成16进制字符串
     * @param bArray
     * @return HexString
     */
    public static String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 16进制的字符串转字节再转成二进制字节数组
     * @param b
     * @return 转换后的字节数组
     **/
    public static byte[] hex2byte(byte[] b) {
        if ((b.length % 2) != 0) {
            throw new IllegalArgumentException("长度不是偶数");
        }
        byte[] b2 = new byte[b.length / 2];
        for (int n = 0; n < b.length; n += 2) {
            String item = new String(b, n, 2);
            // 两位一组，表示一个字节,把这样表示的16进制字符串，还原成一个进制字节
            b2[n / 2] = (byte) Integer.parseInt(item, 16);
        }
        b = null;
        return b2;
    }

    /**
     * 10进制转2进制再补位
     * @param num
     * @return 补位后的字符串
     * */
    public static String toFullBinaryString(int num) {
        char[] chs = new char[Integer.SIZE];
        for(int i = 0; i < Integer.SIZE; i++) {
            chs[Integer.SIZE - 1 - i] = (char)((num >> i & 1) + '0');
        }
        return new String(chs);
    }

    /**
     * 字符串隔两位加空格
     * @param waitString
     * @return 加空格后的字符串
     * */
    public static String regexString (String waitString){
        String regex = "(.{2})";
        String ExString = waitString.replaceAll (regex, "$1 ");
        return ExString;
    }

    /**
     * AFN Map维护
     * @param key
     * @return 应用层功能码
     * */
    public static String getMap(String key){

        Map<String, String> appmap = new HashMap<>();
        appmap.put("00", "确认/否认");
        appmap.put("01", "复位命令");
        appmap.put("02", "链路接口检测");
        appmap.put("03", "中继站命令");
        appmap.put("04", "设置参数");
        appmap.put("05", "控制命令");
        appmap.put("06", "身份认证及密钥协商");
        appmap.put("08", "请求被级联终端主动上报");
        appmap.put("09", "请求终端配置及信息");
        appmap.put("0A", "查询参数");
        appmap.put("0B", "请求任务数据");
        appmap.put("0C", "请求1类数据(实时数据)");
        appmap.put("0D", "请求2类数据(历史数据)");
        appmap.put("0E", "请求3类数据(任务数据)");
        appmap.put("0F", "文件传输");
        appmap.put("10", "数据转发");

        return appmap.get(key);
    }
}
