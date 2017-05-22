package cn.wsds.gamemaster.ui.market;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewStub;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.Misc;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.SessionInfo;
import cn.wsds.gamemaster.data.UserInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.dialog.CommonAlertDialog;
import cn.wsds.gamemaster.net.http.GMHttpClient;
import cn.wsds.gamemaster.net.http.RequestProperty;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.ActivityBase;
import cn.wsds.gamemaster.ui.ActivityUserAccount;
import cn.wsds.gamemaster.ui.UIUtils;

import com.subao.net.NetManager;

/**
 * Created by lidahe on 15/12/8.
 */
public class ActivityMarketWeb extends ActivityBase {
	//    private static final String TAG = ActivityMarketWeb.class.toString();
	private static final Integer PROGRESS_BAR_START = 10;
	private static final Integer PROGRESS_BAR_ALMOST_END = 90;
	private static final Integer PROGRESS_BAR_END = 100;

	private WebView webView;
//	private String htmlUrl = "http://192.168.1.69:8088/index.html";
    private String htmlUrl = HttpApiService.getCouponHtmlUrl();
	private ProgressBar progressBar;
	private UpdateProgressAsyncTask updateProgressAsyncTask;
	private Strategy strategy;
	private String alertModalLayer;

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setDisplayHomeArrow("流量兑换");
		setContentView(R.layout.activity_market_web);
		//
		webView = (WebView) this.findViewById(R.id.webView);
		progressBar = (ProgressBar) findViewById(R.id.webViewProgressBar);
		try {
			WebSettings settings = webView.getSettings();
			settings.setJavaScriptEnabled(true);
			settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
			webView.setWebViewClient(new MyWebViewClient());
			webView.setWebChromeClient(new WebChromeClient());
			webView.addJavascriptInterface(new JsInteration(), "webapp");
		} catch (RuntimeException e) {
			// 在某个坑爹的手机上有可能会抛NullPointer异常，与TTS有关
			showAlertBox();
			return;
		}

		strategy = new StrategyInternet(htmlUrl);
		strategy.refresh();

	}

	private void showAlertBox() {
		CommonAlertDialog dlg = new CommonAlertDialog(this);
		dlg.setMessage("由于您的系统内置网页插件不兼容，无法进入本页面。");
		dlg.setCancelable(true);
		dlg.setPositiveButton("关闭", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				Activity a = ActivityMarketWeb.this;
				if (!a.isFinishing()) {
					a.finish();
				}
			}
		});
		dlg.show();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Misc.clearWebView(webView);
		webView = null;
	}

	@Override
	protected void onResume() {
		super.onResume();
		CallViewMethod.init(webView);
	}

	private final class MyWebViewClient extends WebViewClient {
		private Boolean error;

		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}

		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			error = false;
			if (url.startsWith("http")) {
				strategy.onPageStarted();
			}
			super.onPageStarted(view, url, favicon);
		}

		public void onPageFinished(WebView view, String url) {
			if (url.startsWith("http")) {
				strategy.onPageFinished(error);
			}
			super.onPageFinished(view, url);
		}

		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			error = true;
			strategy.showErrorUI();
			super.onReceivedError(view, errorCode, description, failingUrl);
		}
	}

	private abstract class Strategy {
		public final void refresh() {
			UIUtils.setViewVisibility(webView, View.INVISIBLE);
			progressBar.setProgress(PROGRESS_BAR_START);
			UIUtils.setViewVisibility(progressBar, View.VISIBLE);
			doRefresh();
		}

		protected abstract void doRefresh();

		public abstract void showErrorUI();

		public abstract void onPageStarted();

		public abstract void onPageFinished(Boolean error);
	}

	private class StrategyInternet extends Strategy {
		private final String url;
		private View errorLayout;
		private View buttonRetry;

		public StrategyInternet(String url) {
			this.url = url;
		}

		@Override
		public void doRefresh() {
			webView.loadUrl(url);
		}

		@Override
		public void showErrorUI() {
			if (errorLayout == null) {
				errorLayout = ((ViewStub) findViewById(R.id.stub_exception)).inflate();
				buttonRetry = errorLayout.findViewById(R.id.button_retry);
				buttonRetry.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (NetManager.getInstance().isDisconnected()) {
							UIUtils.showToast("本地网络已断开");
							//                            StatisticDefault.addEvent(ActivityMarketWeb.this, StatisticDefault.Event.USER_EXCHANGE_RESULT, "fail");
							//                            StatisticDefault.addEvent(ActivityMarketWeb.this, StatisticDefault.Event.USER_EXCHANGE_FAIL_REASON, "network");
							return;
						}
						UIUtils.setViewVisibility(errorLayout, View.INVISIBLE);
						UIUtils.setViewVisibility(webView, View.VISIBLE);
						refresh();
					}
				});
			}
			webView.loadUrl("about:blank");
			UIUtils.setViewVisibility(webView, View.INVISIBLE);
			UIUtils.setViewVisibility(errorLayout, View.VISIBLE);
			//Statistic.addEvent(ActivityMarketWeb.this, Statistic.Event.USER_EXCHANGE_CENTRE_CLICK, "网络异常");
			//            StatisticDefault.addEvent(ActivityMarketWeb.this, StatisticDefault.Event.USER_EXCHANGE_RESULT, "fail");
			//            StatisticDefault.addEvent(ActivityMarketWeb.this, StatisticDefault.Event.USER_EXCHANGE_FAIL_REASON, "network");
		}

		@Override
		public void onPageStarted() {
			progressBar.setProgress(PROGRESS_BAR_START);
			updateProgressAsyncTask = new UpdateProgressAsyncTask();
			updateProgressAsyncTask.execute(100);
		}

		@Override
		public void onPageFinished(Boolean error) {
			if (updateProgressAsyncTask != null) {
				updateProgressAsyncTask.cancel(true);
			}
			progressBar.setProgress(PROGRESS_BAR_END);
			new Handler().postDelayed(new Runnable() {
				public void run() {
					UIUtils.setViewVisibility(progressBar, View.INVISIBLE);
				}
			}, 300);
			if (error) {
				UIUtils.setViewVisibility(webView, View.INVISIBLE);
			} else {
				UIUtils.setViewVisibility(webView, View.VISIBLE);
				CallViewMethod.init(webView);
			}
		}
	}

	private static class CallViewMethod {

		static void init(WebView webView) {
			UserSession.UserType type = UserSession.userType();
			int score = 0, preCheck = 1;
			String userId = "", token = "", authorization = "", x_wsse = "";

			if (type == UserSession.UserType.LOGIN || type == UserSession.UserType.BIND_PHONE) {
				UserSession userSession = UserSession.getInstance();
				SessionInfo sessionInfo = userSession.getSessionInfo();
				userId = sessionInfo.getUserId();
				token = sessionInfo.getAccessToken();
				UserInfo userInfo = userSession.getUserInfo();
				score = userInfo.getScore();

			}

			List<RequestProperty> headers = GMHttpClient.XwsseRequestPropertiesCreater.create();
			for (RequestProperty property : headers) {
				if (property.field.equals("Authorization")) {
					authorization = property.newValue;
				} else if (property.field.equals("X-WSSE")) {
					x_wsse = property.newValue;
				}
			}

			//            type = UserSession.UserType.BIND_PHONE;
			//            userId = "AA0E8400-E29B-11D4-A716-446655440055";
			//            token = "550E8400-E29B-11D4-A716-446655440000";
			//            score = 171;

            String call = String.format("javascript:initUser('%s', '%s', '%s', '%s', '%s', '%s')", type.getValue(), userId, token, score, authorization, x_wsse);
            webView.loadUrl(call);

			if (!GlobalDefines.CLIENT_PRE_CHECK) {
				preCheck = 0;
			}
			call = String.format("javascript:init('%s', '%s')", HttpApiService.getFlowCouponApiUrl(), preCheck);
			webView.loadUrl(call);
		}

		static void close(WebView webView) {
			String call = String.format("javascript:closeModal()");
			webView.loadUrl(call);
		}
	}

	public class JsInteration {

		@JavascriptInterface
		public void updateScore(final int score, final String id) {
			MainHandler.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    UserSession.getInstance().updateSorce(score);
                    ConfigManager.getInstance().addOrderId(id);
                }
            });
//          v2.1.2 deprecated
//			StatisticDefault.addEvent(ActivityMarketWeb.this, StatisticDefault.Event.USER_EXCHANGE_RESULT, "success");
		}

		@JavascriptInterface
		public void jumpToLogin(int type) {
			UserSession.UserType userType = UserSession.UserType.fromValue(type);
			if (userType == UserSession.UserType.LOGIN) {
//              v2.1.2 deprecated
//				StatisticDefault.addEvent(ActivityMarketWeb.this, StatisticDefault.Event.USER_EXCHANGE_RESULT, "fail");
				Statistic.addEvent(ActivityMarketWeb.this, Statistic.Event.USER_EXCHANGE_FAIL_REASON, "phonenumber");
				ActivityUserAccount.open(ActivityMarketWeb.this, ActivityUserAccount.FRAGMENT_TYPE_BIND_PHONE);
				return;
			}
//			v2.1.2 deprecated
//			StatisticDefault.addEvent(ActivityMarketWeb.this, StatisticDefault.Event.USER_EXCHANGE_RESULT, "fail");
			Statistic.addEvent(ActivityMarketWeb.this, Statistic.Event.USER_EXCHANGE_FAIL_REASON, "unknown");
			ActivityUserAccount.open(ActivityMarketWeb.this, ActivityUserAccount.FRAGMENT_TYPE_LOGIN);
		}

		@JavascriptInterface
		public void modalLayer(String type) {
			alertModalLayer = type;
		}

		@JavascriptInterface
		public void statistics(String param) {
//			v2.1.2 deprecated
//            StatisticDefault.addEvent(ActivityMarketWeb.this, StatisticDefault.Event.USER_EXCHANGE_RESULT, "fail");
			Statistic.addEvent(ActivityMarketWeb.this, Statistic.Event.USER_EXCHANGE_FAIL_REASON, param);
		}

		@JavascriptInterface
		public void onExchangeButtonClicked(String flowType) {
            if(!TextUtils.isEmpty(flowType)) {
                Statistic.addEvent(ActivityMarketWeb.this, Statistic.Event.USER_EXCHANGE_CENTRE_CLICK_CHANGE, flowType);
            }
		}

		@JavascriptInterface
		public void onPageOpen() {
			//Statistic.addEvent(ActivityMarketWeb.this, Statistic.Event.USER_EXCHANGE_CENTRE_CLICK, "正常");
		}

        @JavascriptInterface
        public void onExchangeResult(String result) {
            if(!TextUtils.isEmpty(result)) {
                Statistic.addEvent(ActivityMarketWeb.this, Statistic.Event.USER_EXCHANGE_FLOW_RESULT, result);
            }
        }
	}

	class UpdateProgressAsyncTask extends AsyncTask<Integer, Integer, String> {
		protected String doInBackground(Integer... params) {
			for (int i = PROGRESS_BAR_START; i <= PROGRESS_BAR_ALMOST_END; i = i + 3) {
				if (isCancelled()) {
					break;
				}
				publishProgress(i);
				try {
					Thread.sleep(params[0]);
				} catch (InterruptedException e) {
					//                    e.printStackTrace();
				}
			}
			return null;
		}

		protected void onProgressUpdate(Integer... progress) {
			progressBar.setProgress(progress[0]);
		}

        protected void onPostExecute(String result) {
        }

		protected void onCancelled() {
			progressBar.setProgress(PROGRESS_BAR_END);
		}
	}

	@Override
	public void onBackPressed() {
		if (alertModalLayer != null && alertModalLayer.equals("open")) {
			CallViewMethod.close(webView);
			return;
		}
		super.onBackPressed();
	}
}
