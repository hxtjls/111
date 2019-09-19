package com.jn.fep.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Calendar;

/**
 * @author hxt
 * @create  2019-07-15 15:56
 * @description 计算检验
 * @version 1.0
 */
public class CheckUtil {

    /**
    * 计算16进制校验和
    * */
    public static String makeChecksum(String data) {

        if (StringUtils.isEmpty(data))
        {
            return "";
        }

        int iTotal = 0;
        int iLen = data.length();
        int iNum = 0;

        while (iNum < iLen)
        {
            String s = data.substring(iNum, iNum + 2);
            //System.out.println(s);
            iTotal += Integer.parseInt(s, 16);
            iNum = iNum + 2;
        }

        /**
         * 用256求余最大是255，即16进制的FF
         */
        int iMod = iTotal % 256;
        String sHex = Integer.toHexString(iMod);
        iLen = sHex.length();
        //如果不够校验位的长度，补0,这里用的是两位校验
        if (iLen < 2)
        {
            sHex = "0" + sHex;
        }
        return sHex.toUpperCase();
    }

    /**
     * 判断是周几，返回十六进制字符串
     * */
    public  static String judgeWeek() {

        Calendar c = Calendar.getInstance();
        int weekday = c.get(Calendar.DAY_OF_WEEK)-1;
        String weekHex =null;
        switch (weekday){
            case 0:
                weekHex = "E";
                break;
            case 1:
                weekHex = "2";
                break;
            case 2:
                weekHex = "4";
                break;
            case 3:
                weekHex = "6";
                break;
            case 4:
                weekHex = "8";
                break;
            case 5:
                weekHex = "A";
                break;
            case 6:
                weekHex = "C";
                break;
        }
        return  weekHex;
    }

    /**
     * 计算报文字节数(长度)
     **/
    public static int calcLength(String Message){
        String frameBegin = Message.substring(0, 12);
        String frameLength = frameBegin.substring(2, 6); //报文头16进制长度
        String turnFrameLength = frameLength.substring(2, 4) + frameLength.substring(0, 2);
        String binaryFrame =  Integer.toBinaryString(Integer.parseInt(turnFrameLength, 16));
        String parseBinaryFrame = binaryFrame.substring(0, binaryFrame.length() - 2);
        int finalFrameLength =Integer.parseInt(parseBinaryFrame, 2);
        return finalFrameLength + 8;
    }

    /**
     *判断报文头(68-68)
     * */
    public static Boolean judgeBegin(String Message){
        Boolean beginBoolen = false;
        if(Message.length() >= 12) {
            String frameBegin = Message.substring(0, 12);
            if (frameBegin.startsWith("68") && frameBegin.endsWith("68")) {
               beginBoolen = true;
            }
        }else{
            beginBoolen =false;
        }
        return beginBoolen;
    }

}
