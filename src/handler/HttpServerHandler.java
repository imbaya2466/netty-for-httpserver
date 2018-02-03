package handler;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import tool.RequestParser;

public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private final String url;


	public HttpServerHandler(String url)
	{

		this.url = url;

	}	


	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		/*如果无法解码400*/
	
		
		
	//	System.out.println(request);
		
		if (!request.decoderResult().isSuccess()) 
		{
			//decoderResult解析该对象的结果
			sendError(ctx, BAD_REQUEST);
   
			return;

		}

		/*只支持GET方法*/

		if (request.method() == HttpMethod.GET)
		{
    
			GetMethod(ctx,request);

		}
		else if(request.method()==HttpMethod.POST)
		{
			
			PostMethod(ctx,request);
		}
		else
		{
			sendError(ctx, METHOD_NOT_ALLOWED);
		    
			return;
		}





	}



////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx)
	{
		System.out.println("取消注册");
	}
	@Override
	public void channelInactive(ChannelHandlerContext ctx)
	{
		System.out.println("断开连接");
	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)throws Exception {

		cause.printStackTrace();

		if (ctx.channel().isActive()) {
  
			sendError(ctx, INTERNAL_SERVER_ERROR);

		}

	}


	private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

//解析uri为本地文件路径
	private String sanitizeUri(String uri) {//    /bin/    

		try {
    
			uri = URLDecoder.decode(uri, "UTF-8");

		} catch (UnsupportedEncodingException e) {
    
			try {
        
				uri = URLDecoder.decode(uri, "ISO-8859-1");
    
			} catch (UnsupportedEncodingException e1) {
        
				throw new Error();
    
			}

		}
	//	System.out.println(uri);//   /bin/
		
		if(uri.compareTo("/")==0)
		{
			uri=url+"/main.html";
		}
		
		

		if (!uri.startsWith(url)) {
    
			uri=url+uri;

		}

		if (!uri.startsWith("/")) {
    
			return null;

		}

		uri = uri.replace('/', File.separatorChar);
		//    \bin\httpbynetty\1\

		if (uri.contains(File.separator + '.')//含"/."   "./"   开始为"."  结束为.    
				|| uri.contains('.' + File.separator) || uri.startsWith(".")
				|| uri.endsWith(".") || INSECURE_URI.matcher(uri).matches()) {
			//matches()对整个字符串进行匹配,只有整个字符串都匹配了才返回true 
			//此处为含<>&\之一就错
    
			return null;

		}
		
		
		

		return System.getProperty("user.dir")  + uri;//当前目录，+get的请求
		//      D:\javawsp\jav\netty实现http服务器\bin\httpbynetty\1\
	}

//发送重定向
	private static void sendRedirect(ChannelHandlerContext ctx, String newUri) {

		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);//302重定向

		response.headers().set(HttpHeaderNames.LOCATION, newUri);//重定向到带/结尾的请求

		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

	}

//返回错误
	private static void sendError(ChannelHandlerContext ctx,HttpResponseStatus status) 

	{

		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
        
				status, Unpooled.copiedBuffer("Failure: " + status.toString()
        
				+ "\r\n", CharsetUtil.UTF_8));

		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

	}
//设置返回类型
	private static void setContentTypeHeader(HttpResponse response, File file) {
		
	
		MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
		//把文件路径化为mime类型——MIME (Multipurpose Internet Mail Extensions) 是描述消息内容类型的因特网标准。
		//java自带的这个功能非常烂....css都判定不了，建议换用其它的库
		
		
		response.headers().set(HttpHeaderNames.CONTENT_TYPE,mimeTypesMap.getContentType(file.getPath()));
		if(file.getPath().endsWith("css"))
		{
			response.headers().set(HttpHeaderNames.CONTENT_TYPE,"text/css");
		}
		if(file.getPath().endsWith("js"))
		{
			response.headers().set(HttpHeaderNames.CONTENT_TYPE,"application/x-javascript");
		}


	}
	
	
	
	
	
	
	
	
	
	
	//GET方法解析，不解析参数，解决静态服务器
	private void GetMethod(ChannelHandlerContext ctx, FullHttpRequest request) throws IOException
	{
		

		
		QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
		final String uri = decoder.path();
		/*格式化URL，并且获取路径*/
		System.out.println(uri);//http://127.0.0.1:8080   是指host
		//这里.uri得到的是http请求中的uri     /bin/   GET /bin/ HTTP/1.1中间的那个（不含host）
		final String path = sanitizeUri(uri);//解析uri
		
		System.out.println(path);
		if (path == null)
		{
    
			sendError(ctx, FORBIDDEN);
   
			return;

		}

		File file = new File(path);//file创建时最后一个有/时会被忽略掉/
	//	System.out.println(path);
		/*如果文件不可访问或者文件不存在*/

		if (file.isHidden() || !file.exists()) {
    
			sendError(ctx, NOT_FOUND);
  
			return;

		}


		if (!file.isFile()) {
    
			sendError(ctx, FORBIDDEN);
    
			return;

		}

		RandomAccessFile randomAccessFile = null;

		try {
   
			randomAccessFile = new RandomAccessFile(file, "r");// 以只读的方式打开文件

		} catch (FileNotFoundException fnfe) {
    
			sendError(ctx, NOT_FOUND);
    
			return;

		}

		long fileLength = randomAccessFile.length();

		//创建一个默认的HTTP响应

		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);

		//设置Content Length

		HttpUtil.setContentLength(response, fileLength);

		//设置Content Type  

		setContentTypeHeader(response, file);//设置种类
		

		//如果request中有KEEP ALIVE信息

		if (HttpUtil.isKeepAlive(request)) {
    
			response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

		}

		ctx.write(response);//发送回答头
		
		
		
		
		
		
		
        // Write the content.
        ChannelFuture sendFileFuture;
        ChannelFuture lastContentFuture;
        
        
        
        
        
        
        if (ctx.pipeline().get(SslHandler.class) == null) {
            sendFileFuture =
                    ctx.write(new DefaultFileRegion(randomAccessFile.getChannel(), 0, fileLength), ctx.newProgressivePromise());
            // Write the end marker.
            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        } else {
            sendFileFuture =
                    ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(randomAccessFile, 0, fileLength, 8192)),
                            ctx.newProgressivePromise());
            // HttpChunkedInput will write the end marker (LastHttpContent) for us.
            lastContentFuture = sendFileFuture;
        }
        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                if (total < 0) { // total unknown
                    System.err.println(future.channel() + " Transfer progress: " + progress);
                } else {
                    System.err.println(future.channel() + " Transfer progress: " + progress + " / " + total);
                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) {
                System.err.println(future.channel() + " Transfer complete.");
            }
        });

        
    /*    
        
		ChannelFuture sendFileFuture;
		//发送回答体
		
		//通过Netty的ChunkedFile对象直接将文件写入发送到缓冲区中
		//ChunkedFile是第二种，分块的方式，采用的是chunkedinput里的阻塞从文件中读取
		sendFileFuture = ctx.write(new ChunkedFile(randomAccessFile, 0,
        fileLength, 8192), ctx.newProgressivePromise());
		
		//这个发送方式有问题，建议采用0拷贝或者其它，netty的大文件传输理解有问题

		sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
    
			@Override
    
			public void operationProgressed(ChannelProgressiveFuture future,long progress, long total) {
        //一个EventListener侦听器，一旦与未来相关的发送任务被传送，将被调用。 表示已经发了多少(累计)   表示总量（未知为-1）
				
				if (total < 0) { // 总大小为止
           
					System.err.println("Transfer progress: " + progress);
        
				} 
				else {
            
					System.err.println("Transfer progress: " + progress + " / "+ total);
        }
    }

    
			@Override
    
			public void operationComplete(ChannelProgressiveFuture future)throws Exception {
        
				System.out.println("Transfer complete.");
    
			}

		});
		
		System.out.println("发送最后一个回答体");
		//发送最后一个回答体
		ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		//况且本来返回就没用Transfer-Encoding: chunked....
		//在这里由于一个channel 的所有写入均由一个线程处理，所以一定会按顺序等文件发完再发这个
*
*
*
*/
		//如果不支持keep-Alive，服务器端主动关闭请求

		if (!HttpUtil.isKeepAlive(request)) {
    
			lastContentFuture.addListener(ChannelFutureListener.CLOSE);

		}
	}
	
	
	
	
	
	
	
	
    private static final AsciiString CONTENT_TYPE = AsciiString.cached("Content-Type");
    private static final AsciiString CONTENT_LENGTH = AsciiString.cached("Content-Length");
    private static final AsciiString CONNECTION = AsciiString.cached("Connection");
    private static final AsciiString KEEP_ALIVE = AsciiString.cached("keep-alive");
	//Post请求处理，着重于参数
	private void PostMethod(ChannelHandlerContext ctx, FullHttpRequest request) throws IOException {
		 Map<String, String> parmMap = new RequestParser(request).parse();
		//parmeMap含有name=value对
		 
		 String fanhuiceshi=request.uri()+"\n"+parmMap.toString();
		 System.out.println(fanhuiceshi);
		 
         FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(fanhuiceshi.getBytes()));
         response.headers().set(CONTENT_TYPE, "text/plain");
         response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
		 
         if (!HttpUtil.isKeepAlive(request)) {
             ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
         } else {
             response.headers().set(CONNECTION, KEEP_ALIVE);
             ctx.writeAndFlush(response);
         }
	}

}

