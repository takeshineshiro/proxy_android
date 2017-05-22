package cn.wsds.gamemaster.ui;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import cn.wsds.gamemaster.ErrorReportor;
import cn.wsds.gamemaster.Misc;
import cn.wsds.gamemaster.R;

import com.subao.utils.FileUtils;

/**
 * 错误日志显示界面
 * 
 * @author Administrator
 * 
 */
public class ActivityErrorLog extends ActivityBase {

	private WebView webView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_error_log);

		initShowErrorlog();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Misc.clearWebView(webView);
		webView = null;
	}
	
	private static void encode(String raw, StringBuilder result) {
		result.append("<pre>");
		raw = raw.replaceAll("<", "&lt;");
		raw = raw.replaceAll(">", "&gt;");
		raw = raw.replace("&lt;END&gt;", "<b><i>&lt;END&gt;</i></b>");
		result.append(raw);
		result.append("</pre>");
	}
	

	private void initShowErrorlog() {
		webView = (WebView) findViewById(R.id.error_log_webview);
		byte[] data = FileUtils.read(ErrorReportor.getErrorLogFile());
		StringBuilder errorLog = new StringBuilder();
		if (data == null) {
			errorLog.append("<center><b>当前没有错误日志</b></center>");
		} else {
			encode(new String(data), errorLog);
		}
		webView.loadDataWithBaseURL(null, errorLog.toString(), "text/html", "utf-8", null);
		//
		findViewById(R.id.error_log_close).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ActivityErrorLog.this.finish();
			}
		});
	}
}
