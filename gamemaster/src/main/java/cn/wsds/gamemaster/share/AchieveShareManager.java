package cn.wsds.gamemaster.share;

import android.app.Activity;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.share.GameMasterShareManager.ShareType;

public class AchieveShareManager {

	private static final String SHARE_CONTENT = "迅游手游加速器，降低延迟可达80%，为上万款国内外主流手游任意加速。http://t.cn/R7MMmcC。";

	private static final String[] shareTitle = new String[] {
		"用迅游手游玩游戏，PK、团战更畅游。",
		"怎样游戏更省力，迅游手游就是给力！",
		"玩游戏卡顿吗？迅游手游助您速战速决！"
	};

	private static final Integer[] ICON_RES_ID_LIST = new Integer[] {
		R.drawable.achievement_to_share_spread_96_1,
		R.drawable.achievement_to_share_spread_96_2,
		R.drawable.achievement_to_share_spread_96_3
	};

	private static final String[] ICON_URL_LIST = new String[] {
		"http://game.wsds.cn/d/achievement_to_share_spread_256_1.png",
		"http://game.wsds.cn/d/achievement_to_share_spread_256_2.png",
		"http://game.wsds.cn/d/achievement_to_share_spread_256_3.png"
	};

	/**
	 * 推送使用成就通知时，页面增加分享
	 * 
	 * @param shareType
	 * @param context
	 * @param shareObserver
	 */
	public static void share(ShareType shareType, Activity context, ShareObserver shareObserver) {
		ShareCallBackObservable.getInstance().registShareObserver(shareObserver);
		switch (shareType) {
		case ShareToSina: // 新浪
			GameMasterShareManager.shareToSina(context,
				new SinaShareManager.ShareContentWebpage(getValueRandom(shareTitle), SHARE_CONTENT,
					GameMasterShareManager.SHARE_URL + "weibo", getValueRandom(ICON_RES_ID_LIST)));
			break;
		case ShareToWeixin: // 微信
			GameMasterShareManager.shareToWeixin(context,
				WeixinShareManager.WEIXIN_SHARE_TYPE_TALK,
				new WeixinShareManager.ShareContentWebpage(getValueRandom(shareTitle), SHARE_CONTENT,
					GameMasterShareManager.SHARE_URL + "weixin", getValueRandom(ICON_RES_ID_LIST)));
			break;
		case ShareToFriends: // 朋友圈
			// 说明：微信6.0分享出去显示的是content内容
			// 微信6.0.2分享出去显示的是title内容，所以为了一致两个都设置为content
			GameMasterShareManager.shareToWeixin(context,
				WeixinShareManager.WEIXIN_SHARE_TYPE_FRENDS,
				new WeixinShareManager.ShareContentWebpage(SHARE_CONTENT,
					SHARE_CONTENT, GameMasterShareManager.SHARE_URL + "weixin2", getValueRandom(ICON_RES_ID_LIST)));
			break;
		case ShareToQQ:
			shareToQQ(shareType, context, "qq");
			break;
		case ShareToZone:
			shareToQQ(shareType, context, "qzone");
			break;
		default:
			break;
		}
	}

	private static void shareToQQ(ShareType shareType, Activity context, String sign) {
		GameMasterShareManager.shareToQQ(context, shareType,
			new QQShareManager.ShareContentWebpage(
				getValueRandom(shareTitle),
				SHARE_CONTENT,
				GameMasterShareManager.SHARE_URL + sign,
				getValueRandom(ICON_URL_LIST)));
	}

	private static <T> T getValueRandom(T[] values) {
		int idx = (int) (Math.abs(System.currentTimeMillis()) % values.length);
		return values[idx];
	}

}
