package main;

//该服务器为下载服务器！！！！！！
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import handler.HttpServerHandler;
import java.net.InetAddress;
import java.util.Scanner;

public class httpserver {
	 private static final String DEFAULT_URL = "/";

	    public void run(final int port, final String url) throws Exception {
	        EventLoopGroup bossGroup = new NioEventLoopGroup();
	        EventLoopGroup workerGroup = new NioEventLoopGroup();
	        try {
	            ServerBootstrap b = new ServerBootstrap();
	            b.group(bossGroup, workerGroup)
	                    .channel(NioServerSocketChannel.class)
	                    .childHandler(new ChannelInitializer<SocketChannel>() {
	                        @Override
	                        protected void initChannel(SocketChannel ch)
	                                throws Exception {
	                            ch.pipeline().addLast("http-decoder",
	                                    new HttpRequestDecoder()); // 请求消息解码器
	                            ch.pipeline().addLast("http-encoder",
	                                    new HttpResponseEncoder());//响应解码器
	                            ch.pipeline().addLast("http-aggregator",
	                                    new HttpObjectAggregator(65536));// 目的是将多个消息转换为单一的request或者response对象
	  
	                            ch.pipeline().addLast("http-chunked",
	                                    new ChunkedWriteHandler());//目的是支持异步大文件传输（）
	                            ch.pipeline().addLast("fileServerHandler",
	                                    new HttpServerHandler(url));// 业务逻辑
	                        }
	                    });
	            ChannelFuture future = b.bind(port).sync();
	            System.out.println("HTTP静态服务器启动，网址是 : " +  InetAddress.getLocalHost()+":"
	                    + port);
	            future.channel().closeFuture().sync();
	        } catch (Exception e) {
	            e.printStackTrace();
	        } finally {
	            bossGroup.shutdownGracefully();
	            workerGroup.shutdownGracefully();
	        }
	    }

	    public static void main(String[] args) throws Exception {
	        int port = 8080;  
	        String url = DEFAULT_URL;
	        Scanner in=new Scanner(System.in);
	        System.out.println("输入起始目录相对该服务器目录:  /...，首页位于该目录下起名为main.html");
	        url=in.next();
	        System.out.println("输入端口号");
	        port=in.nextInt();
	        new httpserver().run(port, url);
	    }
}