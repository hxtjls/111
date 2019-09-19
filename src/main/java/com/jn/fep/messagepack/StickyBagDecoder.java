package com.jn.fep.messagepack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

/**
 * @author hxt
 * @create 2019-09-18 15:28
 * @description
 */
public class StickyBagDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf bufferIn, List<Object> out) throws Exception {

        /*if (bufferIn.readableBytes() < 20) {
            return;
        }*/

        int beginIndex = bufferIn.readerIndex(); //初始读索引
        int length = bufferIn.readableBytes();  //可读字节数


        /**
         * 字节可读长度达到512限制时，把读索引置为0，并且retrun重新读取
         * */
        if (bufferIn.readableBytes() == 512) {
            bufferIn.readerIndex(beginIndex);
            return;
        }
        /**
         * 把读索引设置到最大，使bufferIn.isReadable()返回false，读取完毕
         **/
        bufferIn.readerIndex(beginIndex + length);

        /**
         * 获取bufferIn的子缓冲区与原缓冲区共享包括读写索引，长度为0到可读字节数，并且调用retain方法，对象引用计数器加1，使ByteBuf对象保持不释放
         * */
        ByteBuf otherByteBufRef = bufferIn.slice(beginIndex, length);

        otherByteBufRef.retain();

        out.add(otherByteBufRef); //字节添加到list再输出
    }

}
