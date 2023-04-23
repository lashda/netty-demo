package org.example.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class ClientInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        //ByteToMessageDecoder
        pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        //MessageToMessageDecoder
        pipeline.addLast("decoder", new StringDecoder());//解码
        //MessageToMessageEncoder
        pipeline.addLast("encoder", new StringEncoder());//编码
        //ChannelHandlerAdapter
        pipeline.addLast("handler", new ClientHandler());
    }
}