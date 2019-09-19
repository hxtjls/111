package com.jn.fep.util;

import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


/**
 * @author hxt
 * @create 2019-08-08 16:58
 * @description 读取配置文件
 * @version 1.0
 */
public class PropertiesUtil {

    private static Logger logger = Logger.getLogger(PropertiesUtil.class);

    public static String ReadPro(String inparam){
        String outparm = null;
        //FileInputStream fileInputStream;
        InputStream inputStream;
        Properties pro =new Properties();
        try {
            inputStream = PropertiesUtil.class.getClassLoader().getResourceAsStream("port.properties");
            //fileInputStream = new FileInputStream("port.properties");
            pro.load(inputStream);
            outparm = pro.getProperty(inparam);
        } catch (IOException e) {
            logger.error("IO异常", e.fillInStackTrace());
        } catch (Exception e){
            logger.error("异常信息", e.fillInStackTrace());
        }
        return outparm;
    }

}
