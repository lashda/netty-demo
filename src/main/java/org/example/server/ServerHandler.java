package org.example.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerHandler extends SimpleChannelInboundHandler<String> {
    public static Map<Channel,String> channelNameMap = new ConcurrentHashMap<>();
    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    //覆盖了 handlerAdded() 事件处理方法。每当从服务端收到新的客户端连接时，客户端的 Channel 存入ChannelGroup列表中，
    // 并通知列表中的其他客户端 Channel
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {  // (2)
        Channel incoming = ctx.channel();
        /*for (Channel channel : channels) {
            //writeAndFlush()方法分为两步, 先 write 再 flush
            channel.writeAndFlush("[Client] - " + incoming.remoteAddress() + " 加入\n");
        }*/
        channels.add(incoming);
    }
    //覆盖了 handlerRemoved() 事件处理方法。每当从服务端收到客户端断开时，客户端的 Channel 移除 ChannelGroup 列表中，
    // 并通知列表中的其他客户端 Channel
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {  // (3)
        Channel incoming = ctx.channel();
        String name = channelNameMap.get(incoming);
        for (Channel channel : channels) {
            channel.writeAndFlush("[Client] - " + name + " 离开\n");
        }
        channels.remove(incoming);
        channelNameMap.remove(incoming);
    }

    //覆盖了 channelRead0() 事件处理方法。每当从服务端读到客户端写入信息时，将信息转发给其他客户端的 Channel。
    // 其中如果你使用的是 Netty 5.x 版本时，需要把 channelRead0() 重命名为messageReceived()
    @Override
    protected void messageReceived(ChannelHandlerContext ctx, String s) throws Exception { // (4)
        Channel incoming = ctx.channel();
        String nickName ;
        if(s.startsWith("nickName:")){
            String newNickName = s.replace("nickName:", "").trim();
            String oldNickName = channelNameMap.get(incoming);
            if(oldNickName==null||oldNickName.length()==0){
                nickName = newNickName;
                s =  "加入\n";
            }else{
                nickName = oldNickName;
                s =  "修改昵称："+ "[" + newNickName + "]\n";
            }
            channelNameMap.put(incoming,newNickName);
        }else{
            nickName = channelNameMap.get(incoming);
        }

        for (Channel channel : channels) {
            if (channel != incoming){
                channel.writeAndFlush("[" + nickName + "]" + s + "\n");
            } else {
                channel.writeAndFlush("[me]" + s + "\n");
            }
        }
    }

    //覆盖了 channelActive() 事件处理方法。服务端监听到客户端活动
    //channelActive TODO 什么时候触发
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception { // (5)
        Channel incoming = ctx.channel();
        //System.out.println("Client:"+incoming.remoteAddress()+"在线");
    }

    //覆盖了 channelInactive() 事件处理方法。服务端监听到客户端不活动
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception { // (6)
        Channel incoming = ctx.channel();
        System.out.println("Client:"+channelNameMap.get(incoming)+"掉线");
    }

    //exceptionCaught() 事件处理方法是当出现 Throwable 对象才会被调用，即当 Netty 由于 IO 错误或者处理器在
    // 处理事件时抛出的异常时。在大部分情况下，捕获的异常应该被记录下来并且把关联的 channel 给关闭掉。然而
    // 这个方法的处理方式会在遇到不同异常的情况下有不同的实现，比如你可能想在关闭连接之前发送一个错误码的响应消息。
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (7)
        Channel incoming = ctx.channel();
        System.out.println("Client:"+channelNameMap.get(incoming)+"异常");
        // 当出现异常就关闭连接
        cause.printStackTrace();
        ctx.close();
    }
}
