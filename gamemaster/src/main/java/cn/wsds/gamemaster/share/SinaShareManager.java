package cn.wsds.gamemaster.share;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;
import cn.wsds.gamemaster.social.AppId;
import cn.wsds.gamemaster.ui.UIUtils;

import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WebpageObject;
import com.sina.weibo.sdk.api.WeiboMessage;
import com.sina.weibo.sdk.api.share.IWeiboDownloadListener;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.utils.Utility;

public class SinaShareManager{
	/**
	 * 文字
	 */
	public static final int SINA_SHARE_WAY_TEXT = 1;
	/**
	 * 图片
	 */
	public static final int SINA_SHARE_WAY_PIC = 2;
	/**
	 * 链接
	 */	
	public static final int SINA_SHARE_WAY_WEBPAGE = 3;
	
	/** 
	 *  分享本地图片
	 */
	public static final int SINA_SHARE_WAY_LOCAL_PIC = 4;
	
//	public static final String SCOPE = 
//	            "email,direct_messages_read,direct_messages_write,"
//	            + "friendships_groups_read,friendships_groups_write,statuses_to_me_read,"
//	            + "follow_app_official_microblog," + "invitation_write";
    /** 微博分享的接口实例 */
    private IWeiboShareAPI sinaAPI;
    
    public void registSina(Activity context){
    	sinaAPI = getSinaApi(context);
    }

	/**
	 * 新浪微博分享方法
	 * @param shareContent 分享的内容
	 */
	public void shareBySina(ShareContent shareContent, Context context){
		if(sinaAPI == null) return;
		switch (shareContent.getShareWay()) {
		case SINA_SHARE_WAY_TEXT:
			shareText(shareContent);
			break;
		case SINA_SHARE_WAY_PIC:
			sharePicture(shareContent, context);
			break;
		case SINA_SHARE_WAY_WEBPAGE:
			shareWebPage(shareContent, context);
			break;
		}
	}
	
	/*
	 * 分享文字
	 */
	private void shareText(ShareContent shareContent){
		//初始化微博的分享消息
		WeiboMessage weiboMessage = new WeiboMessage();
		weiboMessage.mediaObject = getTextObj(shareContent.getContent());
		//初始化从第三方到微博的消息请求
		SendMessageToWeiboRequest request = new SendMessageToWeiboRequest();
		request.transaction = buildTransaction("sinatext");
		request.message = weiboMessage;
		//发送请求信息到微博，唤起微博分享界面
		sinaAPI.sendRequest(request);
	}
	
	/*
	 * 分享图片
	 */
	private void sharePicture(ShareContent shareContent, Context context){
		WeiboMessage weiboMessage = new WeiboMessage();
		weiboMessage.mediaObject = getImageObj(shareContent.getPicResource(), context);
		//初始化从第三方到微博的消息请求
		SendMessageToWeiboRequest request = new SendMessageToWeiboRequest();
		request.transaction = buildTransaction("sinatext");
		request.message = weiboMessage;
		//发送请求信息到微博，唤起微博分享界面
		sinaAPI.sendRequest(request);
	}
	
	private void shareWebPage(ShareContent shareContent, Context context){
		WeiboMessage weiboMessage = new WeiboMessage();
		weiboMessage.mediaObject = getWebpageObj(shareContent, context);
		//初始化从第三方到微博的消息请求
		SendMessageToWeiboRequest request = new SendMessageToWeiboRequest();
		request.transaction = buildTransaction("sinatext");
		request.message = weiboMessage;
		//发送请求信息到微博，唤起微博分享界面
		sinaAPI.sendRequest(request);
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
			return SINA_SHARE_WAY_TEXT;
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
			return SINA_SHARE_WAY_PIC;
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
			return SINA_SHARE_WAY_WEBPAGE;
		}
		
	}
	
    /**
     * 创建文本消息对象。
     * 
     * @return 文本消息对象。
     */
    private TextObject getTextObj(String text) {
        TextObject textObject = new TextObject();
        textObject.text = text;
        return textObject;
    }
    
    private ImageObject getImageObj(int picResource, Context context){
    	 ImageObject imageObject = new ImageObject();
    	 Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), picResource);
         imageObject.setImageObject(bmp);
         return imageObject;
    }
    
    private WebpageObject getWebpageObj(ShareContent shareContent, Context context){
    	WebpageObject mediaObject = new WebpageObject();
        mediaObject.identify = Utility.generateGUID();
        mediaObject.title = shareContent.getTitle();
        mediaObject.description = shareContent.getContent();
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), shareContent.getPicResource());
		mediaObject.thumbData = UIUtils.bmpCompressToByteArray(bmp, 32 * 1024, true);
        mediaObject.actionUrl = shareContent.getURL();
        mediaObject.defaultText = shareContent.getContent();
        return mediaObject;
    }
	
	private static IWeiboShareAPI getSinaApi(Activity context){
		// 创建微博 SDK 接口实例
		IWeiboShareAPI sinaAPI = WeiboShareSDK.createWeiboAPI(context, AppId.SINA_APP_ID);
		checkSinaVersin(context, sinaAPI);
        //检查版本支持情况
        boolean result = sinaAPI.registerApp();
        if(result){
        	return sinaAPI;
        }else{
        	return null;
        }
	}

	private static void checkSinaVersin(final Context context, IWeiboShareAPI sinaAPI) {
		// 获取微博客户端相关信息，如是否安装、支持 SDK 的版本
        boolean isInstalledWeibo = sinaAPI.isWeiboAppInstalled();
        //int supportApiLevel = sinaAPI.getWeiboAppSupportAPI(); 
        
        // 如果未安装微博客户端，设置下载微博对应的回调
        if (!isInstalledWeibo) {
           sinaAPI.registerWeiboDownloadListener(new IWeiboDownloadListener() {
                @Override
                public void onCancel() {
                    cn.wsds.gamemaster.ui.UIUtils.showToast("取消下载", Toast.LENGTH_SHORT);
                }
            });
        }
	}
	
	private static String buildTransaction(final String type) {
		return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
	}

	public static boolean shareLocalPic(Activity context,String path) {
		IWeiboShareAPI sinaApi = getSinaApi(context);
		if(sinaApi==null){
			return false;
		}
		WeiboMessage weiboMessage = new WeiboMessage();
		weiboMessage.mediaObject = getLocalImageObj(path);
		// 初始化从第三方到微博的消息请求
		SendMessageToWeiboRequest request = new SendMessageToWeiboRequest();
		request.transaction = buildTransaction("sinatext");
		request.message = weiboMessage;
		if(!weiboMessage.checkArgs()){
			return false;
		}
		// 发送请求信息到微博，唤起微博分享界面
		return sinaApi.sendRequest(request);
	}
	
	private static ImageObject getLocalImageObj(String path){
   	 	ImageObject imageObject = new ImageObject();
   	 	Bitmap bmp = BitmapFactory.decodeFile(path);
        imageObject.setImageObject(bmp);
        return imageObject;
   }
}
