package com.jn.fep.fepwork;

import com.jn.fep.parse.MessageParse;
import com.jn.fep.service.FEPMapService;
import com.jn.fep.util.ParseUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import org.apache.log4j.Logger;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author hxt
 * @create 2019-08-27 15:01
 * @description 终端服务handler
 * @version 1.0
 */

public class FEPZDServerHandler extends ChannelInboundHandlerAdapter{

    private Logger logger = Logger.getLogger(FEPZDServerHandler.class);

    private static String clientZDAddress;

    public static AtomicInteger ZDatomicInteger = new AtomicInteger(0); //终端数量计数

    /**
     * 收到客户端消息，自动触发
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)  {
        try {
            /**
             * 将 msg 转为 netty 的 ByteBuf 对象，类似 JDK 中的 java.nio.ByteBuffer，不过 ButeBuf 功能更强，更灵活
             */
            ByteBuf buf = (ByteBuf) msg;

            /**readableBytes：获取缓冲区可读字节数,然后创建字节数组
             * 从而避免了像 java.nio.ByteBuffer 时，只能盲目的创建特定大小的字节数组，比如 1024
             * */
            byte[] reg = new byte[buf.readableBytes()];  //创建字节数组
            /**readBytes：将缓冲区字节数组复制到新建的 byte 数组中
             * 然后将字节数组转为字符串
             * */
            buf.readBytes(reg);

            synchronized (FEPZDServerHandler.class) {

                String receviceMsg = MessageParse.Parse(reg, ctx);

                /**回复消息
                 * copiedBuffer：创建一个新的缓冲区，内容为里面的参数
                 * 通过 ChannelHandlerContext 的 write 方法将消息异步发送给客户端
                 * */
                if (receviceMsg != null) {
                    if (FEPMapService.getZDChannel(MessageParse.logicAddrtips) != null && MessageParse.forwardFlag == 0) {
                        ByteBuf respByteBuf = Unpooled.copiedBuffer(ParseUtil.hex2byte(receviceMsg.getBytes()));
                        ctx.writeAndFlush(respByteBuf);  //前置机回复给终端
                    } else if (FEPMSServerHandler.MSAddr != null && FEPMapService.getMSChannel(MessageParse.MSnetworkAddr) != null && MessageParse.forwardFlag == 1) {
                        ByteBuf respByteBuf = Unpooled.copiedBuffer(ParseUtil.hex2byte(receviceMsg.getBytes()));
                        SocketChannel sc = FEPMapService.getMSChannel(MessageParse.MSnetworkAddr);
                        sc.writeAndFlush(respByteBuf);  //终端被动发送给指定主站
                    } else if (FEPMSServerHandler.MSAddr != null && FEPMapService.getMSMap().size() > 0 && MessageParse.forwardFlag == 2) {
                        ByteBuf respByteBuf = Unpooled.copiedBuffer(ParseUtil.hex2byte(receviceMsg.getBytes()));
                        for (Map.Entry<String, SocketChannel> entry : FEPMapService.getMSMap().entrySet()) {
                            entry.getValue().writeAndFlush(respByteBuf);  //终端主动发送给所有主站
                        }
                    } else if (FEPMSServerHandler.MSAddr == null && FEPMapService.getMSMap().size() == 0) {
                        logger.warn("[" + FEPMSServerHandler.MSatomicInteger + "/" + ZDatomicInteger + "][(" + MessageParse.logicAddrtips + ")尚未有主站连接前置机，无法上报此终端的上行报文 ]" + "\n");
                    }
                }
            }
            buf.release();
        }catch (Exception e){
            logger.error("异常信息", e.fillInStackTrace());
        }

    }


    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        //注册连接
        InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
        String ZDclientIP = insocket.getAddress().getHostAddress();
        int ZDclientPort = insocket.getPort();
        clientZDAddress = ZDclientIP + ":"+ ZDclientPort;

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //激活连接
      ctx.fireChannelActive();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        /**flush：将消息发送队列中的消息写入到 SocketChannel 中发送给对方，为了频繁的唤醒 Selector 进行消息发送
         * fepwork 的 write 方法并不直接将消息写入 SocketChannel 中，调用 write 只是把待发送的消息放到发送缓存数组中，再通过调用 flush
         * 方法，将发送缓冲区的消息全部写入到 SocketChannel 中
         * */
        ctx.flush();
        /*for (Map.Entry<String, SocketChannel> entry : FEPMapService.getZDMap().entrySet()) {
            System.out.println("key = " + entry.getKey() + ", value = " + entry.getValue());
        }*/
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception{
        /**当发生异常时，关闭 ChannelHandlerContext，释放和它相关联的句柄等资源 */
        ctx.channel().close();
        logger.error("异常信息", cause.fillInStackTrace());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception{
        //断开连接
        InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
        clientZDAddress = insocket.getAddress().getHostAddress() + ":" +insocket.getPort();
        ctx.fireChannelInactive();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        //注销连接
        try {
            InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
            String clientZDAddress = insocket.getAddress().getHostAddress() + ":" + insocket.getPort();
            String logicAddr = MessageParse.ZDlaipMap.get(clientZDAddress);
            ctx.fireChannelUnregistered();
            if (MessageParse.ZDofflineFlag != -1) {
                ctx.channel().close();
                if (FEPMapService.getZDChannel(logicAddr) != null) {
                    FEPMapService.removeZDMapChannel(logicAddr);
                    ZDatomicInteger.decrementAndGet();
                    logger.warn("[" + FEPMSServerHandler.MSatomicInteger + "/" + ZDatomicInteger + "][(" + clientZDAddress + ")终端已经离线(" + logicAddr + ")]" + "\n");
                } else {
                    logger.warn("[" + FEPMSServerHandler.MSatomicInteger + "/" + ZDatomicInteger + "][(" + clientZDAddress + ")终端已经离线,且此终端尚未发送登录帧...]" + "\n");
                }
            }
        }catch (Exception e){
            logger.error("异常信息", e.fillInStackTrace());
        }
    }

}
