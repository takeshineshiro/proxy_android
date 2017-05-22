package cn.wsds.gamemaster.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.webkit.WebView;
import cn.wsds.gamemaster.Misc;
import cn.wsds.gamemaster.R;

/**
 * 服务协议
 * @author Administrator
 *
 */
public class ActivityLicence extends ActivityBase{
	private WebView webView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_licence);
		setDisplayHomeArrow("服务协议");
		initWebView();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Misc.clearWebView(webView);
		webView = null;
	}
	
	private boolean initWebView(){
		webView = (WebView) findViewById(R.id.licence_webview);
		webView.setBackgroundColor(getResources().getColor(R.color.color_game_1));
		AssetManager am = getResources().getAssets();
		if(am == null){
			return false;
		}
		InputStream input = null;
		InputStreamReader reader = null;
		try {
			input = am.open("licence.html");
			if(input == null){
				return false;
			}
			reader = new InputStreamReader(input);
			input = null;
			CharBuffer buffer = CharBuffer.allocate(12 * 1024);
			reader.read(buffer);
			String content = new String(buffer.array(), 0, buffer.position());
			webView.loadDataWithBaseURL(null, content, "text/html", "utf-8", null);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
		}
		return false;
	}
}
