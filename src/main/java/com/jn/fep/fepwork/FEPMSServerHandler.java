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
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author hxt
 * @create 2019-08-28 10:25
 * @description 主站服务handler
 * @version 1.0
 */
public class FEPMSServerHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = Logger.getLogger(FEPMSServerHandler.class);

    public static String clientMSAddress ; //主站远程地址

    public static String MSAddr; //主站IP

    public static AtomicInteger MSatomicInteger = new AtomicInteger(0); //主站数量计数

    /**
     * 收到客户端消息，自动触发
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            /**
             * 将 msg 转为 fepwork 的 ByteBuf 对象，类似 JDK 中的 java.nio.ByteBuffer，不过 ButeBuf 功能更强，更灵活
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

            String receviceMsg = MessageParse.Parse(reg, ctx); //解析报文

            /**回复消息
             * copiedBuffer：创建一个新的缓冲区，内容为里面的参数
             * 通过 ChannelHandlerContext 的 write 方法将消息异步发送给客户端
             * */

            if (FEPMapService.getZDMap().get(MessageParse.logicAddrtips) != null && MessageParse.forwardFlag == 0) {
                ByteBuf respByteBuf = Unpooled.copiedBuffer(ParseUtil.hex2byte(receviceMsg.getBytes()));
                SocketChannel sc = FEPMapService.getZDMap().get(MessageParse.logicAddrtips);
                sc.writeAndFlush(respByteBuf); //主站发送给指定终端

            } else if (FEPMapService.getZDMap().get(MessageParse.logicAddrtips) == null && MessageParse.forwardFlag == 0) {
                String denialFrame = MessageParse.denybf.toString();
                ByteBuf respByteBuf = Unpooled.copiedBuffer(ParseUtil.hex2byte(denialFrame.getBytes()));
                SocketChannel sc = FEPMapService.getMSMap().get(MessageParse.MSnetworkAddr);
                sc.writeAndFlush(respByteBuf);
                logger.info("[" + MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + MessageParse.MSnetworkAddr + ")前置机回复主站否认帧成功(" + MessageParse.logicAddrtips + ")] : [" + ParseUtil.regexString(denialFrame) + "]" + "\n");
            }
            buf.release();
        }catch (Exception e){
            logger.error("异常信息", e.fillInStackTrace());
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        //注册连接
        ctx.fireChannelRegistered();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //激活连接
        InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
        String MSclientIP = insocket.getAddress().getHostAddress();
        MSAddr = MSclientIP;
        int MSclientPort = insocket.getPort();
        clientMSAddress = MSclientIP + ":"+ MSclientPort;
        if (MSclientIP != null && FEPMapService.getMSChannel(clientMSAddress) == null) {
            FEPMapService.addMSMapChannel(clientMSAddress, (SocketChannel) ctx.channel());
            MSatomicInteger.incrementAndGet();
            logger.info("[" + MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientMSAddress + ")主站连接前置机成功...]" + "\n");
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        /**flush：将消息发送队列中的消息写入到 SocketChannel 中发送给对方，为了频繁的唤醒 Selector 进行消息发送
         * fepwork 的 write 方法并不直接将消息写入 SocketChannel 中，调用 write 只是把待发送的消息放到发送缓存数组中，再通过调用 flush
         * 方法，将发送缓冲区的消息全部写入到 SocketChannel 中
         * */
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)  {
        /**当发生异常时，关闭 ChannelHandlerContext，释放和它相关联的句柄等资源 */
        ctx.channel().close();
        logger.error("异常信息", cause.fillInStackTrace());
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //检测连接断开
        ctx.fireChannelInactive();
        MSatomicInteger.decrementAndGet();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        //注销连接
        InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
        String clientMSAddress = insocket.getAddress().getHostAddress() + ":" +insocket.getPort();
        ctx.fireChannelInactive();
        FEPMapService.removeMSMapChannel(clientMSAddress);
        logger.warn("[" + MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientMSAddress +  ")主站连接已经断开...]" +"\n");
    }
}
