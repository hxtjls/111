package com.jn.fep;

import com.jn.fep.fepwork.FEPMSServer;
import com.jn.fep.fepwork.FEPZDServer;
import com.jn.fep.util.PropertiesUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class JnFepApplication {

    public static void main(String[] args) {
        SpringApplication.run(JnFepApplication.class, args);

        FEPZDServer fepzdServer = new FEPZDServer(Integer.parseInt(PropertiesUtil.ReadPro("ZDHostTCP")));
        fepzdServer.start();
        FEPMSServer fepmsServer = new FEPMSServer(Integer.parseInt(PropertiesUtil.ReadPro("MSHostTCP")));
        fepmsServer.start();
    }

}
