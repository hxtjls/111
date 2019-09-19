package com.jn.fep.fepwork;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.log4j.Logger;


/**
 * @author hxt
 * @create 2019-08-28 13:21
 * @description 监听主站服务端
 * @version 1.0
 */
public class FEPMSServer extends Thread{

    private Logger logger =Logger.getLogger(FEPMSServer.class);

    private int port;

    public FEPMSServer(int port){
        this.port = port;
    }

    public void run() {
        /**
         * interface EventLoopGroup extends EventExecutorGroup extends ScheduledExecutorService extends ExecutorService
         * 配置服务端的 NIO 线程池,用于网络事件处理，实质上他们就是 Reactor 线程组
         * bossGroup 用于服务端接受客户端连接，workerGroup 用于进行 SocketChannel 网络I/O读写
         * */
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            /** ServerBootstrap 是 Netty 用于启动 NIO 服务端的辅助启动类，用于降低开发难度
             * */
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)              //设置通道
                    .option(ChannelOption.SO_BACKLOG, 1024)      //设置客户端
                    .childOption(ChannelOption.TCP_NODELAY,true) //设置服务端
                    .childHandler(new ChildMSChannelHandler());

            /**服务器启动辅助类配置完成后，调用 bind 方法绑定监听端口，调用 sync 方法同步等待绑定操作完成*/
            ChannelFuture f = b.bind(port).sync();

            logger.info("[前置机监听主站连接服务已开启...]" + "\n");
            logger.info("[等待终端远程连接，端口号为：" + port + "...]" + "\n");
            /**下面会进行阻塞，等待服务器连接关闭之后 main 方法退出，程序结束
             *
             * */
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            /**优雅退出，释放线程池资源*/
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private class ChildMSChannelHandler extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel arg0) throws Exception {
            arg0.pipeline().addLast(new FEPMSServerHandler()); //自定义编码解码handler
        }
    }
}