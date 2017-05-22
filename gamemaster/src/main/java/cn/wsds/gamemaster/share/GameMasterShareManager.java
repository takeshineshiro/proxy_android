package cn.wsds.gamemaster.share;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.widget.Toast;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.wxapi.NotInstalledException;

/**
 * 分享管理类
 * 
 * @author Administrator
 * 
 */
public class GameMasterShareManager {

	//微信包名
	public static final String WEIXIN_PACKAGE_NAME = "com.tencent.mm";
	//分享链接地址
	public static final String SHARE_URL = "http://www.xunyou.mobi/?c=download&ver=g_share_";
	//分享图片地址
	private static final String SHARE_IMG_URL = "http://game.wsds.cn/d/256.png";

	private static GameMasterShareManager gameMasterShareManager;

	private GameMasterShareManager() {};

	/**
	 * 获取GameMasterShareManager实例
	 * 
	 * @return
	 */
	public static GameMasterShareManager getInstance() {
		if (gameMasterShareManager == null) {
			gameMasterShareManager = new GameMasterShareManager();
		}
		return gameMasterShareManager;
	}

	/**
	 * 分享类型
	 * 
	 * @author Administrator
	 * 
	 */
	public enum ShareType {
		ShareToSina,     //新浪
		ShareToWeixin,   //微信
		ShareToQQ,       //QQ
		ShareToFriends,  //朋友圈
		ShareToZone,      //QQ空间
	}

	/**
	 * 主界面分享
	 */
	public void mainPageShare(ShareType shareType, Activity context) {
		Resources res = context.getResources();
		String title = null;
		String content = null;
		String shareChannel = null; //统计渠道
		switch (shareType) {
		case ShareToSina:     //新浪
			title = res.getString(R.string.share_title_content);
			content = res.getString(R.string.share_weibo_content);
			shareChannel = "weibo";
			shareToSina(context,
				new SinaShareManager.ShareContentWebpage(title, content, SHARE_URL + shareChannel, R.drawable.ic_launcher));
			break;
		case ShareToWeixin:   //微信
			title = res.getString(R.string.share_title_content);
			content = res.getString(R.string.share_weixin_content);
			shareChannel = "weixin";
			shareToWeixin(context, WeixinShareManager.WEIXIN_SHARE_TYPE_TALK,
				new WeixinShareManager.ShareContentWebpage(title, content, SHARE_URL + shareChannel, R.drawable.ic_launcher));
			break;
		case ShareToQQ:       //QQ
			title = res.getString(R.string.share_title_content);
			content = res.getString(R.string.share_qq_content);
			shareChannel = "qq";
			shareToQQ(context, shareType, new QQShareManager.ShareContentWebpage(title, content, SHARE_URL + shareChannel, SHARE_IMG_URL));
			break;
		case ShareToFriends:  //朋友圈
			//说明：微信6.0分享出去显示的是content内容
			//微信6.0.2分享出去显示的是title内容，所以为了一致两个都设置为content
			title = res.getString(R.string.share_weixin_friends_content);
			content = res.getString(R.string.share_weixin_friends_content);
			shareChannel = "weixin2";
			shareToWeixin(context, WeixinShareManager.WEIXIN_SHARE_TYPE_FRENDS,
				new WeixinShareManager.ShareContentWebpage(title, content, SHARE_URL + shareChannel, R.drawable.ic_launcher));
			break;
		case ShareToZone:      //QQ空间
			title = res.getString(R.string.share_title_content);
			content = res.getString(R.string.share_qzone_content);
			shareChannel = "qzone";
			shareToQQ(context, shareType, new QQShareManager.ShareContentWebpage(title, content, SHARE_URL + shareChannel, SHARE_IMG_URL));
			break;
		default:
			return;
		}
		Statistic.addEvent(context, Statistic.Event.SHARE_HOMEPAGE_CLICK, shareChannel);
	}

	/**
	 * 分享到新浪微博
	 */
	public static void shareToSina(Activity context, SinaShareManager.ShareContent shareContent) {
		SinaShareManager sinaShareManager = new SinaShareManager();
		sinaShareManager.registSina(context);
		sinaShareManager.shareBySina(shareContent, context);
	}

	public static boolean shareToWeixin(Activity context, int shareType, WeixinShareManager.ShareContent shareContent) {
		try {
			WeixinShareManager.shareByWeixin(context, shareContent, shareType);
			return true;
		} catch (NotInstalledException e) {
			UIUtils.showToast(e.getMessage(), Toast.LENGTH_SHORT);
			return false;
		}
	}

	/**
	 * 分享到QQ和空间
	 * 
	 * @param context
	 * @param title
	 * @param content
	 * @param shareType
	 */
	public static void shareToQQ(Activity context, ShareType shareType, QQShareManager.ShareContent shareContent) {
		QQShareManager qqShareManager = new QQShareManager();
		if (ShareType.ShareToQQ == shareType) {
			qqShareManager.shareByQQ(context, shareContent, QQShareManager.QQ_SHARE_TYPE_TALK);
		} else {
			qqShareManager.shareByQQ(context, shareContent, QQShareManager.QQ_SHARE_TYPE_ZONE);
		}
		shareResponedQQ(qqShareManager, context);
	}

	//	/**
	//	 * 基础分享模版
	//	 * @param context
	//	 * @param shareType
	//	 * @param title
	//	 * @param content
	//	 */
	//	private void baseShare(Activity context, 
	//			ShareType shareType, String title, String content){
	//		if(shareType == ShareType.UnDefined) return;
	//		if(title == null || content == null) return;
	//		switch (shareType) {
	//		case ShareToSina:     //新浪
	//			shareToSina(context, title, content);
	//			break;
	//		case ShareToWeixin:   //微信
	//			shareToWeixin(context, title, content,
	//					WeixinShareManager.WEIXIN_SHARE_TYPE_TALK);
	//			break;
	//		case ShareToQQ:       //QQ
	//			shareToQQ(context, title, content,
	//					QQShareManager.QQ_SHARE_TYPE_TALK);
	//			break;
	//		case ShareToFriends:  //朋友圈
	//			shareToWeixin(context, title, content,
	//					WeixinShareManager.WEIXIN_SHARE_TYPE_FRENDS);
	//			break;
	//		case ShareToZone:      //QQ空间
	//			shareToQQ(context, title, content, 
	//					QQShareManager.QQ_SHARE_TYPE_ZONE);
	//			break;
	//		default:
	//			break;
	//		}
	//	}
	//
	//	/**
	//	 * 分享到新浪微博
	//	 * @param context
	//	 * @param title
	//	 * @param content
	//	 */
	//	private void shareToSina(Activity context, 
	//			String title, String content) {
	//		SinaShareManager sinaShareManager = new SinaShareManager();
	//		sinaShareManager.registSina(context);
	//		SinaShareManager.ShareContentWebpage sinaShareContent = 
	//				new SinaShareManager.ShareContentWebpage(title, content, SHARE_URL, R.drawable.ic_launcher);
	//		sinaShareManager.shareBySina(sinaShareContent, context);
	//	}
	//
	//	/**
	//	 * 分享到QQ和空间
	//	 * @param context
	//	 * @param title
	//	 * @param content
	//	 * @param shareType
	//	 */
	//	private void shareToQQ(Activity context,
	//			String title, String content, int shareType) {
	//		QQShareManager qqShareManager;
	//		qqShareManager = new QQShareManager();
	//		ShareContentWebpage qqShareContentWebPage;
	//		qqShareManager.registShare(context);
	//		qqShareContentWebPage = new ShareContentWebpage(title, content, SHARE_URL, SHARE_IMG_URL);
	//		qqShareManager.shareByQQ(context, qqShareContentWebPage, shareType);
	//		shareResponedQQ(qqShareManager, context);
	//	}
	//	
	//
	//	/**
	//	 * 分享到微信及朋友圈
	//	 * @param context
	//	 * @param title
	//	 * @param content
	//	 * @param shareType
	//	 */
	//	private void shareToWeixin(Activity context, 
	//			String title, String content, int shareType) {
	//		shareToWeixin(context, title, content, shareType, SHARE_URL, R.drawable.ic_launcher);
	//	}
	//	
	//	private void shareToWeixin(Activity context, String title, String content, 
	//			int shareType, String shareUrl, int iconId){
	//		if(Misc.isAppInstalled(context, WEIXIN_PACKAGE_NAME)){
	//			WeixinShareManager weixinShareManager = new WeixinShareManager();
	//			weixinShareManager.registWeixin(context);
	//			WeixinShareManager.ShareContentWebpage weixinShareContentWebpage = new WeixinShareManager.ShareContentWebpage(
	//					title, content, shareUrl, iconId);
	//			weixinShareManager.shareByWeixin(context, weixinShareContentWebpage, shareType);
	//		}else{
	//			UIUtils.showToast("请先安装微信客户端", Toast.LENGTH_SHORT);
	//		}
	//	}

	/**
	 * QQ和空间分享回调
	 * 
	 * @param qqShareManager
	 * @param context
	 */
	private static void shareResponedQQ(QQShareManager
		qqShareManager, Context context) {
		qqShareManager.setOnQQShareResponse(new QQShareManager.QQShareResponse() {
			@Override
			public void respCode(int currentType, int code) {

				ShareType shareType;
				if (currentType == QQShareManager.QQ_SHARE_TYPE_TALK) {
					shareType = ShareType.ShareToQQ;
				} else if (currentType == QQShareManager.QQ_SHARE_TYPE_ZONE) {
					shareType = ShareType.ShareToZone;
				} else {
					return;
				}
				ShareCallBackObservable.getInstance().callbackShareResult(shareType, code);
			}
		});
	}
}
