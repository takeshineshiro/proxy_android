package cn.wsds.gamemaster.share;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.wxapi.NotInstalledException;
import cn.wsds.gamemaster.wxapi.WeixinUtils;

import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXImageObject;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXTextObject;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;

/**
 * 实现微信分享功能的核心类
 */
public class WeixinShareManager{
	
	/**
	 * 文字
	 */
	public static final int WEIXIN_SHARE_WAY_TEXT = 1;
	/**
	 * 图片
	 */
	public static final int WEIXIN_SHARE_WAY_PIC = 2;
	/**
	 * 链接
	 */
	public static final int WEIXIN_SHARE_WAY_WEBPAGE = 3;
	/**
	 * 会话
	 */
	public static final int WEIXIN_SHARE_TYPE_TALK = SendMessageToWX.Req.WXSceneSession;  
	/**
	 * 朋友圈
	 */
	public static final int WEIXIN_SHARE_TYPE_FRENDS = SendMessageToWX.Req.WXSceneTimeline;
	
	/**
	 * 通过微信分享
	 * @param shareWay 分享的方式（文本、图片、链接）
	 * @param shareType 分享的类型（朋友圈，会话）
	 * @throws NotInstalledException 
	 */
	public static void shareByWeixin(Context context, ShareContent shareContent, int shareType) throws NotInstalledException {
		IWXAPI wxApi = WeixinUtils.createWXApi(context);
		switch (shareContent.getShareWay()) {
		case WEIXIN_SHARE_WAY_TEXT:
			shareText(wxApi, shareType, shareContent);
			break;
		case WEIXIN_SHARE_WAY_PIC:
			sharePicture(wxApi, context, shareType, shareContent);
			break;
		case WEIXIN_SHARE_WAY_WEBPAGE:
			shareWebPage(wxApi, context, shareType, shareContent);
			break;
		}
	}
	
	public static abstract class ShareContent{
		protected abstract int getShareWay();
		protected abstract String getContent();
		protected abstract String getTitle();
		protected abstract String getURL();
		protected abstract int getPicResource();
		
	}
	
	/**
	 * 设置分享文字的内容
	 * @author Administrator
	 *
	 */
	public static class ShareContentText extends ShareContent{
		private String content;
		
		/**
		 * 构造分享文字类
		 * @param text 分享的文字内容
		 */
		public ShareContentText(String content){
			this.content = content;
		}

		@Override
		protected String getContent() {

			return content;
		}

		@Override
		protected String getTitle() {
			return null;
		}

		@Override
		protected String getURL() {
			return null;
		}

		@Override
		protected int getPicResource() {
			return -1;
		}

		@Override
		protected int getShareWay() {
			return WEIXIN_SHARE_WAY_PIC;
		}
		
	}
	
	/**
	 * 设置分享图片的内容
	 * @author Administrator
	 *
	 */
	public static class ShareContentPic extends ShareContent{
		private int picResource;
		public ShareContentPic(int picResource){
			this.picResource = picResource;
		}
		
		@Override
		protected String getContent() {
			return null;
		}

		@Override
		protected String getTitle() {
			return null;
		}

		@Override
		protected String getURL() {
			return null;
		}

		@Override
		protected int getPicResource() {
			return picResource;
		}

		@Override
		protected int getShareWay() {
			return WEIXIN_SHARE_WAY_PIC;
		}
	}
	
	/**
	 * 设置分享链接的内容
	 * @author Administrator
	 *
	 */
	public static class ShareContentWebpage extends ShareContent{
		private String title;
		private String content;
		private String url;
		private int picResource;
		public ShareContentWebpage(String title, String content, 
				String url, int picResource){
			this.title = title;
			this.content = content;
			this.url = url;
			this.picResource = picResource;
		}

		@Override
		protected String getContent() {
			return content;
		}

		@Override
		protected String getTitle() {
			return title;
		}

		@Override
		protected String getURL() {
			return url;
		}

		@Override
		protected int getPicResource() {
			return picResource;
		}

		@Override
		protected int getShareWay() {
			return WEIXIN_SHARE_WAY_WEBPAGE;
		}
		
	}
	
	/*
	 * 分享文字
	 */
	private static void shareText(IWXAPI wxApi, int shareType, ShareContent shareContent) {
		String text = shareContent.getContent();
		//初始化一个WXTextObject对象
		WXTextObject textObj = new WXTextObject();
		textObj.text = text;
		//用WXTextObject对象初始化一个WXMediaMessage对象
		WXMediaMessage msg = new WXMediaMessage();
		msg.mediaObject = textObj;
		msg.description = text;
		//构造一个Req
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		//transaction字段用于唯一标识一个请求
		req.transaction = buildTransaction("textshare");
		req.message = msg;
		//发送的目标场景， 可以选择发送到会话 WXSceneSession 或者朋友圈 WXSceneTimeline。 默认发送到会话。
		req.scene = shareType;
		wxApi.sendReq(req);
	}

	/*
	 * 分享图片
	 */
	private static void sharePicture(IWXAPI wxApi, Context context, int shareType, ShareContent shareContent) {
		WXMediaMessage msg = new WXMediaMessage();
		Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), shareContent.getPicResource());
		msg.mediaObject = new WXImageObject(bmp);
		if (msg.thumbData == null) {
			msg.setThumbImage(bmp);
		}
		
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.transaction = buildTransaction("imgshareappdata");
		req.message = msg;
		req.scene = shareType;
		wxApi.sendReq(req);
	}
	
	/**
	 * 分享本地图片
	 */
	public static boolean shareLocalPicture(IWXAPI wxApi, Context context, int shareScene, String path){
		WXImageObject imgObj = new WXImageObject();
		imgObj.setImagePath(path);
		WXMediaMessage msg = new WXMediaMessage();
		msg.mediaObject = imgObj;
		
		Bitmap bmp = BitmapFactory.decodeFile(path);
		if (bmp.getHeight() > 320) {
			bmp = UIUtils.bmpScaled(bmp, 320 * 100 / bmp.getHeight());
		}
		msg.thumbData = UIUtils.bmpCompressToByteArray(bmp,32*1024, true);
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.message = msg;
		req.transaction = buildTransaction("img");
		req.scene =  shareScene;
		if(!req.checkArgs()){
			return false;
		}
		return wxApi.sendReq(req);
	}
	


	/*
	 * 分享链接
	 */
	private static void shareWebPage(IWXAPI wxApi, Context context, int shareType, ShareContent shareContent) {
		WXWebpageObject webpage = new WXWebpageObject();
		webpage.webpageUrl = shareContent.getURL();
		WXMediaMessage msg = new WXMediaMessage(webpage);
		msg.title = shareContent.getTitle();
		msg.description = shareContent.getContent();
		
		Bitmap thumb = BitmapFactory.decodeResource(context.getResources(), shareContent.getPicResource());
		if(thumb == null){
			UIUtils.showToast("图片不能为空", Toast.LENGTH_SHORT);
			return;
		}else{
			msg.setThumbImage(thumb);
		}
		
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.transaction = shareType + "_" + buildTransaction("webpage");
		req.message = msg;
		req.scene = shareType;
		wxApi.sendReq(req);
	}
	
	private static String buildTransaction(final String type) {
		return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
	}
	
}
