package main;

//�÷�����Ϊ���ط�����������������
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
	                                    new HttpRequestDecoder()); // ������Ϣ������
	                            ch.pipeline().addLast("http-encoder",
	                                    new HttpResponseEncoder());//��Ӧ������
	                            ch.pipeline().addLast("http-aggregator",
	                                    new HttpObjectAggregator(65536));// Ŀ���ǽ������Ϣת��Ϊ��һ��request����response����
	  
	                            ch.pipeline().addLast("http-chunked",
	                                    new ChunkedWriteHandler());//Ŀ����֧���첽���ļ����䣨��
	                            ch.pipeline().addLast("fileServerHandler",
	                                    new HttpServerHandler(url));// ҵ���߼�
	                        }
	                    });
	            ChannelFuture future = b.bind(port).sync();
	            System.out.println("HTTP��̬��������������ַ�� : " +  InetAddress.getLocalHost()+":"
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
	        System.out.println("������ʼĿ¼��Ը÷�����Ŀ¼:  /...����ҳλ�ڸ�Ŀ¼������Ϊmain.html");
	        url=in.next();
	        System.out.println("����˿ں�");
	        port=in.nextInt();
	        new httpserver().run(port, url);
	    }
}