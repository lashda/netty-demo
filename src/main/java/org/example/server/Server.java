package org.example.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class Server {
    public static void main(String[] args) throws Exception {
        Server server = new Server(9999);
        new Thread(()-> {
            try {
                server.run();
            } catch (Exception e) {
                System.out.println("server.run error");
            }
        }).start();
    }
    private final int port;
    private Channel bindChannel ;

    public Server(int port){
        this.port = port;
    }
    public void run() throws Exception {
        // 配置nio线程组 创建两个线程组

        // NioEventLoopGroup是用于处理I / O操作的多线程事件循环器，Netty提供了很多不同的EventLoopGroup的
        // 实现处理不同的传输。在这个例子中我们实现了一个服务端的应用，因此会有2第一个经常被称为'boss'，
        // 使用接收进来的连接。第二个经常被称为'worker'，使用处理已经被接收的连接，一旦'boss'接收到连接，
        // 如何知道多少个线程已经被使用，如何映射到已经创建的Channel上都需要依赖于EventLoopGroup的实现，
        // 并且可以通过构造函数来配置他们的关系
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //ServerBootstrap是一个启动NIO服务的辅助启动类。您可以在这个服务中直接使用Channel，但是这会是一个复杂的处理过程，在很多情况下你并不需要这样做。
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)  // server端引导类,管理两个线程组
                    //此处我们指定使用NioServerSocketChannel类来说明一个新的Channel如何接收进来的连接。
                    .channel(NioServerSocketChannel.class) // (3)
                    //此处的事件处理类经常会被使用处理过一个最近的已经接收到的Channel。SimpleChatServerInitializer继承自ChannelInitializer是一个特殊的处理类，
                    // 他的目的是帮助用户配置一个新的Channel。也许你想通过增加一些处理类类SimpleChatServerHandler来配置一个新的Channel或其对应的ChannelPipeline来
                    // 实现你的网络程序。当你的程序变的复杂时，可能你会增加更多的处理类到pipline上，然后提取这些匿名类到最顶层的类上。
                    // 绑定客户端连接时候触发操作
                    .childHandler(new ServerInitializer())  //(4)

                    //option（）是提供给NioServerSocketChannel以接收进来的连接。childOption（）是提供给由父管道ServerChannel接收到的连接，
                    // 在这个例子中也是NioServerSocketChannel。

                    //您可以设置此处指定的Channel实现的配置参数。我们正在编写一个TCP / IP的服务端，因此我们被允许设置socket的参数选项tcpNoDelay和keepAlive。
                    // 请参考ChannelOption和详细的ChannelConfig实现的接口文档最初可以对ChannelOption的有一个大概的认识。

                    // SO_BACKLOG用于构造服务端套接字ServerSocket对象，标识当服务器请求处理线程全满时，用于临时存
                    // 放已完成三次握手的请求的队列的最大长度。如果未设置或所设置的值小于1，Java将使用默认值50。
                    //初始化服务端可连接队列,指定了队列的大小128
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    //保持长连接
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            System.out.println("Server 启动了");

            //剩下的就是绑定端口然后启动服务。这里我们在机器上绑定了机器所有网卡上的8080端口。当然现在你可以多次调用bind（）方法（基于不同绑定地址）
            // 绑定端口，开始接收进来的连接
            ChannelFuture f = b.bind(port).sync(); // (7)
            bindChannel = f.channel();
            // 等待服务器  socket 关闭 。
            // 在这个例子中，这不会发生，但你可以优雅地关闭你的服务器。
            // 最后绑定服务器等待直到绑定完成，调用sync()方法会阻塞直到服务器完成绑定,然后服务器等待通道关闭，因为使用sync()，所以关闭操作也会被阻塞。
            f.channel().closeFuture().sync();
        } finally {
            // 退出，释放线程池资源
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();

            System.out.println("Server 关闭了");
        }
    }

    public void stop(){
        if (this.bindChannel==null) {
            System.out.println("Server is not running");
        }else{
            System.out.println("Server stop");
            this.bindChannel.close();
            this.bindChannel = null;
        }

    }

}
