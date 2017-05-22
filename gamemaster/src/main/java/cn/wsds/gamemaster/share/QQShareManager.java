package cn.wsds.gamemaster.share;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;
import cn.wsds.gamemaster.share.GameMasterShareManager.ShareType;
import cn.wsds.gamemaster.social.AppId;

import com.tencent.connect.share.QQShare;
import com.tencent.connect.share.QzoneShare;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

public class QQShareManager {
	/**
	 * 链接
	 */
	public static final int QQ_SHARE_WAY_WEBPAGE = 3;
	/**
	 * QQ
	 */
	public static final int QQ_SHARE_TYPE_TALK = 1;  
	/**
	 * QQ空间
	 */
	public static final int QQ_SHARE_TYPE_ZONE = 2;
	
	private QQShareResponse qqShareResponse;
	private int currentType;
	
	/**
	 * 分享qq和空间
	 * @param shareContent 分享内容
	 * @param shareType  选择类型（qq、空间）
	 */
	public void shareByQQ(Activity activity, ShareContent shareContent, int shareType){
		shareWebPage(activity, shareType, shareContent);
	}
	
	private void shareWebPage(Activity activity, int shareType, ShareContent shareContent){
		Bundle params = new Bundle();
		if(shareType == QQ_SHARE_TYPE_ZONE){
			shareWebPageQzone(activity, shareContent, params);
		}else{
			shareWebPageQQ(activity, shareContent, params);
		}
	}

	/**
	 * 分享到QQ(链接分享)参数配置
	 * @param activity
	 * @param shareContent
	 * @param params
	 */
	private void shareWebPageQQ(Activity activity, ShareContent shareContent, Bundle params) {
		params.putString(QQShare.SHARE_TO_QQ_TITLE, shareContent.getTitle());
		params.putString(QQShare.SHARE_TO_QQ_SUMMARY, shareContent.getContent());
		params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE,
				QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
		params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, shareContent.getURL());
		params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, shareContent.getPicUrl());
		
		currentType = QQ_SHARE_TYPE_TALK;
		Tencent tencent = Tencent.createInstance(AppId.QQ_APP_ID, activity.getApplicationContext());
		tencent.shareToQQ(activity, params, new ShareCallback(ShareType.ShareToQQ));
	}

	/**
	 * 分享到QQ空间(链接分享)参数配置
	 * @param activity
	 * @param shareContent
	 * @param params
	 */
	private void shareWebPageQzone(Activity activity, ShareContent shareContent, Bundle params) {
		params.putString(QzoneShare.SHARE_TO_QQ_TITLE, shareContent.getTitle());
		params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, shareContent.getContent());
		params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE, 
				QzoneShare.SHARE_TO_QZONE_TYPE_IMAGE_TEXT);
		params.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL, shareContent.getURL());
		ArrayList<String> imageUrls = new ArrayList<String>();
		imageUrls.add(shareContent.getPicUrl());
		params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, imageUrls);
		//params.putString(QzoneShare.SHARE_TO_QQ_IMAGE_URL, shareContent.getPicUrl());
		currentType = QQ_SHARE_TYPE_ZONE;
		Tencent tencent = Tencent.createInstance(AppId.QQ_APP_ID, activity.getApplicationContext());
		tencent.shareToQzone(activity, params, new ShareCallback(ShareType.ShareToZone));
	}
	
	
	/**
	 * QQ分享监听
	 */
	private final class ShareCallback implements IUiListener {
		
		public ShareCallback(ShareType type) {
		}

		@Override
		public void onCancel() {
			sendRespCode(ShareObserver.CALLBACK_CODE_CANCEL);
		}

		@Override
		public void onComplete(Object response) {
			sendRespCode(ShareObserver.CALLBACK_CODE_SUCCESS);
		}


		@Override
		public void onError(UiError e) {
			cn.wsds.gamemaster.ui.UIUtils.showToast(e.errorMessage, Toast.LENGTH_SHORT);
			sendRespCode(ShareObserver.CALLBACK_CODE_DENY);
		}
		
		private void sendRespCode(int code) {
			if(qqShareResponse != null){
				qqShareResponse.respCode(currentType, code);
			}
		}
	};
	
	/**
	 * QQ分享结果反馈
	 * @author Administrator
	 *
	 */
	public interface QQShareResponse{
		/**
		 * 分享结果
		 * @param code 结果码
		 */
		public void respCode(int currentType, int code);
	}
	
	/**
	 * 注册结果回馈
	 * @param qqShareResponse
	 */
	public void setOnQQShareResponse(QQShareResponse qqShareResponse){
		this.qqShareResponse = qqShareResponse;
	}
	
	/**
	 * 分享传递参数的抽象类
	 * @author Administrator
	 *
	 */
	public static abstract class ShareContent{
		protected abstract int getShareWay();
		protected abstract String getContent();
		protected abstract String getTitle();
		protected abstract String getURL();
		protected abstract String getPicUrl();
	}
	
	/**
	 * 设置分享链接的内容
	 */
	public static class ShareContentWebpage extends ShareContent{
		private String title;
		private String content;
		private String url;
		private String picUrl;
		public ShareContentWebpage(String title, String content, 
				String url, String picUrl){
			this.title = title;
			this.content = content;
			this.url = url;
			this.picUrl = picUrl;
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
		protected int getShareWay() {
			return QQ_SHARE_WAY_WEBPAGE;
		}

		@Override
		protected String getPicUrl() {
			return picUrl;
		}
	}
	
	
	public static Tencent createTencent(Context context){
		return Tencent.createInstance(AppId.QQ_APP_ID, context.getApplicationContext());
	}
	
	public static void shareLocalImgToQQ(Activity activity,Tencent tencent,String path, IUiListener iUiListener){
		Bundle params = new Bundle();
		params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL,path);
		params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE);
		QQShare qqShare = new QQShare(activity, tencent.getQQToken());
		qqShare.shareToQQ(activity, params, iUiListener);
	}
	public static void shareLocalImgToQzone(Activity activity,Tencent tencent,String path, IUiListener iUiListener){
		Bundle params = new Bundle();
		params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL,path);
		params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, QQShare.SHARE_TO_QQ_FLAG_QZONE_AUTO_OPEN);
		params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE);
		params.putString(QQShare.SHARE_TO_QQ_APP_NAME, "");
		QQShare qqShare = new QQShare(activity, tencent.getQQToken());
		qqShare.shareToQQ(activity, params, iUiListener);
	}
	
}
