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
		/*����޷�����400*/
	
		
		
	//	System.out.println(request);
		
		if (!request.decoderResult().isSuccess()) 
		{
			//decoderResult�����ö���Ľ��
			sendError(ctx, BAD_REQUEST);
   
			return;

		}

		/*ֻ֧��GET����*/

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
		System.out.println("ȡ��ע��");
	}
	@Override
	public void channelInactive(ChannelHandlerContext ctx)
	{
		System.out.println("�Ͽ�����");
	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)throws Exception {

		cause.printStackTrace();

		if (ctx.channel().isActive()) {
  
			sendError(ctx, INTERNAL_SERVER_ERROR);

		}

	}


	private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

//����uriΪ�����ļ�·��
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

		if (uri.contains(File.separator + '.')//��"/."   "./"   ��ʼΪ"."  ����Ϊ.    
				|| uri.contains('.' + File.separator) || uri.startsWith(".")
				|| uri.endsWith(".") || INSECURE_URI.matcher(uri).matches()) {
			//matches()�������ַ�������ƥ��,ֻ�������ַ�����ƥ���˲ŷ���true 
			//�˴�Ϊ��<>&\֮һ�ʹ�
    
			return null;

		}
		
		
		

		return System.getProperty("user.dir")  + uri;//��ǰĿ¼��+get������
		//      D:\javawsp\jav\nettyʵ��http������\bin\httpbynetty\1\
	}

//�����ض���
	private static void sendRedirect(ChannelHandlerContext ctx, String newUri) {

		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);//302�ض���

		response.headers().set(HttpHeaderNames.LOCATION, newUri);//�ض��򵽴�/��β������

		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

	}

//���ش���
	private static void sendError(ChannelHandlerContext ctx,HttpResponseStatus status) 

	{

		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
        
				status, Unpooled.copiedBuffer("Failure: " + status.toString()
        
				+ "\r\n", CharsetUtil.UTF_8));

		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

	}
//���÷�������
	private static void setContentTypeHeader(HttpResponse response, File file) {
		
	
		MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
		//���ļ�·����Ϊmime���͡���MIME (Multipurpose Internet Mail Extensions) ��������Ϣ�������͵���������׼��
		//java�Դ���������ܷǳ���....css���ж����ˣ����黻�������Ŀ�
		
		
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
	
	
	
	
	
	
	
	
	
	
	//GET���������������������������̬������
	private void GetMethod(ChannelHandlerContext ctx, FullHttpRequest request) throws IOException
	{
		

		
		QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
		final String uri = decoder.path();
		/*��ʽ��URL�����һ�ȡ·��*/
		System.out.println(uri);//http://127.0.0.1:8080   ��ָhost
		//����.uri�õ�����http�����е�uri     /bin/   GET /bin/ HTTP/1.1�м���Ǹ�������host��
		final String path = sanitizeUri(uri);//����uri
		
		System.out.println(path);
		if (path == null)
		{
    
			sendError(ctx, FORBIDDEN);
   
			return;

		}

		File file = new File(path);//file����ʱ���һ����/ʱ�ᱻ���Ե�/
	//	System.out.println(path);
		/*����ļ����ɷ��ʻ����ļ�������*/

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
   
			randomAccessFile = new RandomAccessFile(file, "r");// ��ֻ���ķ�ʽ���ļ�

		} catch (FileNotFoundException fnfe) {
    
			sendError(ctx, NOT_FOUND);
    
			return;

		}

		long fileLength = randomAccessFile.length();

		//����һ��Ĭ�ϵ�HTTP��Ӧ

		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);

		//����Content Length

		HttpUtil.setContentLength(response, fileLength);

		//����Content Type  

		setContentTypeHeader(response, file);//��������
		

		//���request����KEEP ALIVE��Ϣ

		if (HttpUtil.isKeepAlive(request)) {
    
			response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

		}

		ctx.write(response);//���ͻش�ͷ
		
		
		
		
		
		
		
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
		//���ͻش���
		
		//ͨ��Netty��ChunkedFile����ֱ�ӽ��ļ�д�뷢�͵���������
		//ChunkedFile�ǵڶ��֣��ֿ�ķ�ʽ�����õ���chunkedinput����������ļ��ж�ȡ
		sendFileFuture = ctx.write(new ChunkedFile(randomAccessFile, 0,
        fileLength, 8192), ctx.newProgressivePromise());
		
		//������ͷ�ʽ�����⣬�������0��������������netty�Ĵ��ļ��������������

		sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
    
			@Override
    
			public void operationProgressed(ChannelProgressiveFuture future,long progress, long total) {
        //һ��EventListener��������һ����δ����صķ������񱻴��ͣ��������á� ��ʾ�Ѿ����˶���(�ۼ�)   ��ʾ������δ֪Ϊ-1��
				
				if (total < 0) { // �ܴ�СΪֹ
           
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
		
		System.out.println("�������һ���ش���");
		//�������һ���ش���
		ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		//���ұ������ؾ�û��Transfer-Encoding: chunked....
		//����������һ��channel ������д�����һ���̴߳�������һ���ᰴ˳����ļ������ٷ����
*
*
*
*/
		//�����֧��keep-Alive���������������ر�����

		if (!HttpUtil.isKeepAlive(request)) {
    
			lastContentFuture.addListener(ChannelFutureListener.CLOSE);

		}
	}
	
	
	
	
	
	
	
	
    private static final AsciiString CONTENT_TYPE = AsciiString.cached("Content-Type");
    private static final AsciiString CONTENT_LENGTH = AsciiString.cached("Content-Length");
    private static final AsciiString CONNECTION = AsciiString.cached("Connection");
    private static final AsciiString KEEP_ALIVE = AsciiString.cached("keep-alive");
	//Post�����������ڲ���
	private void PostMethod(ChannelHandlerContext ctx, FullHttpRequest request) throws IOException {
		 Map<String, String> parmMap = new RequestParser(request).parse();
		//parmeMap����name=value��
		 
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

