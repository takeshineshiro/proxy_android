package cn.wsds.gamemaster.ui;

import com.subao.net.NetManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewStub;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import cn.wsds.gamemaster.Misc;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.message.MessageManager;
import cn.wsds.gamemaster.message.MessageManager.Record;
import cn.wsds.gamemaster.statistic.Statistic;

public class ActivityMessageView extends ActivityBase {

	private static final String NAME_TITLE = "cn.wsds.gamemaster.message.title";
	private static final String NAME_CONTENT = "cn.wsds.gamemaster.message.content";
	private static final String NAME_TYPE = "cn.wsds.gamemaster.message.type";

	private ProgressBar progress;
	private WebView webView;

	private Strategy strategy;

	private abstract class Strategy {
		public final void refresh() {
			UIUtils.setViewVisibility(webView, View.INVISIBLE);
			UIUtils.setViewVisibility(progress, View.VISIBLE);
			doRefresh();
		}

		protected abstract void doRefresh();

		public abstract void showErrorUI();
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
							return;
						}
						UIUtils.setViewVisibility(errorLayout, View.INVISIBLE);
						UIUtils.setViewVisibility(webView, View.VISIBLE);
						refresh();
					}
				});
			}
			UIUtils.setViewVisibility(webView, View.INVISIBLE);
			UIUtils.setViewVisibility(errorLayout, View.VISIBLE);
		}
	}

	private class StrategyLocal extends Strategy {
		private final String content;

		public StrategyLocal(String content) {
			this.content = content;
		}

		@Override
		public void doRefresh() {
			webView.loadDataWithBaseURL(null, content, null, "utf-8", null);
		}

		@Override
		public void showErrorUI() {
			// do nothing
		}
	}

	private class StrategyNull extends Strategy {

		@Override
		public void doRefresh() {
			webView.loadUrl("about:blank");
		}

		@Override
		public void showErrorUI() {
			// do nothing
		}

	}

	public static void show(Context context, MessageManager.Record messageRecord,boolean callFromJpush) {
		Statistic.addEvent(context, Statistic.Event.INTERACTIVE_MESSAGE_WEBPAGE_OPEN,callFromJpush);
		
		Intent intent = new Intent(context, ActivityMessageView.class);
		if (!TextUtils.isEmpty(messageRecord.title)) {
			intent.putExtra(NAME_TITLE, messageRecord.title);
		}
		if (!TextUtils.isEmpty(messageRecord.content)) {
			intent.putExtra(NAME_CONTENT, messageRecord.content);
		}
		intent.putExtra(NAME_TYPE, messageRecord.type);
		if(Record.TYPE_JPUSH_NOTIFY_URL==messageRecord.type){
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP );
		}
		context.startActivity(intent);
	}
	 
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_message_view);
		this.progress = (ProgressBar) findViewById(R.id.progress);
		this.webView = (WebView) findViewById(R.id.webView);
		webView.setWebViewClient(new WebViewClient() {

			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				UIUtils.setViewVisibility(progress, View.INVISIBLE);
				strategy.showErrorUI();
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				if (UIUtils.setViewVisibility(progress, View.INVISIBLE)) {
					UIUtils.setViewVisibility(webView, View.VISIBLE);
				}
			}
			
		});
		WebSettings settings = webView.getSettings();
		if(settings!=null){
			settings.setJavaScriptEnabled(true);
		}
		//
		Intent intent = this.getIntent();
		String title = intent.getStringExtra(NAME_TITLE);
		if (TextUtils.isEmpty(title)) {
			title = "消息查看";
		}
		this.setDisplayHomeArrow(title);
		//
		int messageType = intent.getIntExtra(NAME_TYPE, MessageManager.Record.TYPE_HTML);
		String content = intent.getStringExtra(NAME_CONTENT);
		if (TextUtils.isEmpty(content)) {
			strategy = new StrategyNull();
		} else {
			switch (messageType) {
			case MessageManager.Record.TYPE_URL:
			case MessageManager.Record.TYPE_JPUSH_NOTIFY_URL:
				strategy = new StrategyInternet(content);
				break;
			default:
				strategy = new StrategyLocal(content);
				break;
			}
		}
		strategy.refresh();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Misc.clearWebView(webView);
		webView = null;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (webView != null && webView.canGoBack()) {
				webView.goBack();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected ActivityType getPreActivityType() {
		return ActivityType.MESSAGE_CENTER;
	}
}
