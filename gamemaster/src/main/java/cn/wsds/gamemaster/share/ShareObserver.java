package cn.wsds.gamemaster.share;

import cn.wsds.gamemaster.share.GameMasterShareManager.ShareType;

/**
 * 关于分享的观察者接口
 * @author Administrator
 *
 */
public interface ShareObserver {
	/**
	 * 分享成功
	 */
	public static final int CALLBACK_CODE_SUCCESS = 0;
	/**
	 * 取消分享
	 */
	public static final int CALLBACK_CODE_CANCEL = 1;
	/**
	 * 拒绝访问
	 */
	public static final int CALLBACK_CODE_DENY = 2;
	/**
	 * 未知
	 */
	public static final int CALLBACK_CODE_UNKNOWN = 3;
	
	/**
	 * 分享反馈结果
	 * @param shareType
	 * @param resultCode
	 */
	public void callbackResult(ShareType shareType, int resultCode);
}
