package cn.wsds.gamemaster.net.http;

import android.app.Activity;
import android.text.TextUtils;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;

import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.data.UserSession.LogoutReason;
import cn.wsds.gamemaster.dialog.DefaultRequesterDialog;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.UIUtils.IOnReloginConfirmListener;

/**
 * Created by lidahe on 15/12/20.
 */
public abstract class ResponseHandler {

	public static final String DEFAULT_MSG_NET_PROBLEM = "网络故障，请稍后重试";

	/**
	 * 如果不为null，则表示要发起IO请求时要显示一个进度框（该Activity用于显示对话框）
	 */
	private final WeakReference<Activity> activity;

	/**
	 * 如果不为null，则当服务器返回401时（登录失效），回调它
	 */
	protected final OnHttpUnauthorizedCallBack onHttpUnauthorizedCallBack;
	
	/**
	 * 构造。有进度框，无身份失效回调
	 */
	public ResponseHandler(Activity activity) {
		this(activity, null);
	}

	public ResponseHandler(Activity activity, OnHttpUnauthorizedCallBack onHttpUnauthorizedCallBack) {
		this.activity = new WeakReference<Activity>(activity);
		this.onHttpUnauthorizedCallBack = onHttpUnauthorizedCallBack;
	}

	/**
	 * 请求开始时被调用
	 */
	public final void start() {
		DefaultRequesterDialog.incShow(activity.get());
	}

	/**
	 * 由外部调用：通讯结束了。
	 * 
	 * @param response
	 *            ，为null表示通讯错误。成功时为服务器的应答数据
	 */
	public final void finish(Response response) {
		try {
			if (response != null) {
				if (HttpURLConnection.HTTP_UNAUTHORIZED == response.code && onHttpUnauthorizedCallBack != null) {
					onHttpUnauthorizedCallBack.onHttpUnauthorized(response);
					return;
				}
				onSuccess(response);
			} else {
				CharSequence text = getToastText_RequestFail();
				if (!TextUtils.isEmpty(text)) {
					UIUtils.showToast(text);
				}
				onFailure();
			}
		} finally {
			// 如果请求前打开了Loading动效，这里应该关闭它了
			DefaultRequesterDialog.decShow();
		}
	}

	/**
	 * Fired when a request returns successfully, override to handle in your own
	 * code 服务器成功响应，包含(400-500)等异常状态
	 * <p>
	 * <b>如果是401，且指定了onHttpUnauthorizedCallback，则本方法不会被调用</b>
	 * </p>
	 */
	protected abstract void onSuccess(Response response);

	/**
	 * 服务器未成功响应 超时异常等异常情况
	 */
	protected void onFailure() {}

	/**
	 * 当请求失败时应该显示什么样的Toast？
	 * 
	 * @return null表示不需要显示Toast
	 */
	protected CharSequence getToastText_RequestFail() {
		return DEFAULT_MSG_NET_PROBLEM;
	}

	/**
	 * 外部调用：发起请求之前发现网络不可用
	 */
	public void onNetworkUnavailable() {
		showToastWhenNetError();
	}

	/**
	 * 显示一条Toast：网络故障
	 */
	public static void showToastWhenNetError() {
		UIUtils.showToast(DEFAULT_MSG_NET_PROBLEM);
	}

	/**
	 * 当服务器发生401时回调
	 */
	public interface OnHttpUnauthorizedCallBack {
		/**
		 * 服务器发生了401
		 * 
		 * @param response
		 *            服务器响应数据实体
		 */
		public void onHttpUnauthorized(Response response);
	}

	/**
	 * 当发生 401 时 退出登录
	 */
	public static class DefaultOnHttpUnauthorizedCallBack implements OnHttpUnauthorizedCallBack {

		@Override
		public void onHttpUnauthorized(Response response) {
			// 登出
			UserSession.logout(LogoutReason.LOGIN_ON_OTHER_DEVICE);
		}

	}

	/**
	 * 当发生 401 时 退出登录并提示用户登录
	 */
	public static class ReLoginOnHttpUnauthorizedCallBack extends DefaultOnHttpUnauthorizedCallBack {

		private final WeakReference<Activity> weakContext;
		private final IOnReloginConfirmListener listener;
		private final String dialogMess;

		public ReLoginOnHttpUnauthorizedCallBack(Activity context) {
			this(context, "登录已失效，请重新登录。", null);
		}
		
		public ReLoginOnHttpUnauthorizedCallBack(Activity context,String string) {
			this(context, string, null);
		}
		
		public ReLoginOnHttpUnauthorizedCallBack(Activity context,IOnReloginConfirmListener listener) {
			this(context, "登录已失效，请重新登录。", listener);
		}

		public ReLoginOnHttpUnauthorizedCallBack(Activity context,String dialogMess,IOnReloginConfirmListener listener) {
			this.weakContext = new WeakReference<Activity>(context);
			this.listener = listener;
			if(TextUtils.isEmpty(dialogMess)){
				dialogMess = "登录已失效，请重新登录。";
			}
			this.dialogMess = dialogMess;
		}

		@Override
		public void onHttpUnauthorized(Response response) {
			super.onHttpUnauthorized(response);
			Activity activity = weakContext.get();
			if (activity == null) {
				return;
			}
			if (activity.isFinishing()) {
				return;
			}

            if("登录已失效，请重新登录。".equals(dialogMess)) {
                UIUtils.showReloginDialog(activity, listener);
            } else {
                UIUtils.showReloginDialog(activity, dialogMess, listener);
            }
		}

	}
}
