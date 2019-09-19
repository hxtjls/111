package com.jn.fep.service;

import com.jn.fep.fepwork.FEPMSServerHandler;
import com.jn.fep.fepwork.FEPZDServerHandler;
import com.jn.fep.parse.MessageParse;
import io.netty.channel.ChannelHandlerContext;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;

/**
 * @author hxt
 * @create 2019-09-02 10:12
 * @description 心跳检测handler
 * @version 1.0
 */
public class ServerHeartbeatHandler extends CustomHeartbeatHandler {

    private  Logger logger = Logger.getLogger(ServerHeartbeatHandler.class);

    public ServerHeartbeatHandler() {
        super("server");
    }


    @Override
    protected void handleReaderIdle(ChannelHandlerContext ctx) {
        super.handleReaderIdle(ctx);
        InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
        String clientZDAddress = insocket.getAddress().getHostAddress() + ":" + insocket.getPort();
        String logicAddr = MessageParse.ZDlaipMap.get(clientZDAddress);
        ctx.channel().close();
        FEPMapService.removeZDMapChannel(logicAddr);
        logger.warn( "[" + FEPMSServerHandler.MSatomicInteger  + "/" + FEPZDServerHandler.ZDatomicInteger + "][("+ clientZDAddress +")终端没有在15分钟内发送任何报文帧，断开此终端连接(" + logicAddr +")]" + "\n");

    }
}
