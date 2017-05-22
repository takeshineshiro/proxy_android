package cn.wsds.gamemaster.debugger.fragment;

import java.io.IOException;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.debugger.logpack.RuntimeLogManager;
import cn.wsds.gamemaster.ui.UIUtils;

/**
 * 日志打包上传
 */
public class FragmentDebugLogPackUpload extends Fragment {

	private Button buttonRuntimeDump;
	private TextView textTime;
	private final RuntimeLogManager.Observer runtimeLogManagerObserver= new RuntimeLogManager.Observer() {
		
		@Override
		public void uploadCompleted(boolean result) {
			UIUtils.showToast("调试信息" + (result ? "上传成功" : "上传失败"));
			RuntimeLogManager.instance.unregisterObserver(runtimeLogManagerObserver);
			closeStyle();
		}
		
		@Override
		public void progressChanged(int progress) {
			textTime.setText("progress:"+progress);
		}
	};
	
	
	private final class ViewClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.button_runtime_dump:
				onButtonDumpClick();
				break;
			}
		}

		/**
		 * 抓取运行时日志按钮点击
		 */
		private void onButtonDumpClick() {
			if(RuntimeLogManager.isCatching()){//抓取中则关闭
				closeStyle();
				RuntimeLogManager.instance.closeCatch();
				RuntimeLogManager.instance.unregisterObserver(runtimeLogManagerObserver);
			}else{//没有抓取则关闭
				start();
			}
		}
	};
	
	
	
	/**
	 * 响应开始动作
	 */
	private void start() {
		try {
			RuntimeLogManager.instance.registerObservers(runtimeLogManagerObserver);
			RuntimeLogManager.instance.startCatch(null);
			stratStyle();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		RuntimeLogManager.instance.unregisterObserver(runtimeLogManagerObserver);
	}
	/**
	 * 开启状态样式
	 * 
	 */
	private void stratStyle() {
		buttonRuntimeDump.setText("关闭");
		textTime.setText("progress:0");
		textTime.setVisibility(View.VISIBLE);
	}
	/**
	 * 关闭状态样式
	 */
	private void closeStyle() {
		buttonRuntimeDump.setText("开始");
		textTime.setText("progress:0");
		textTime.setVisibility(View.GONE);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_debugger_log_dump, container, false);
		initView(view);
		return view;
	}

	/**
	 * 初始化view
	 * @param view
	 */
	private void initView(View view) {
		buttonRuntimeDump = (Button) view.findViewById(R.id.button_runtime_dump);
		buttonRuntimeDump.setOnClickListener(new ViewClickListener());
		textTime = (TextView) view.findViewById(R.id.text_time);  
		initStyle();
	}
	/**
	 * 初始化页面样式
	 */
	private void initStyle() {
		if(RuntimeLogManager.isCatching()){
			stratStyle();
			RuntimeLogManager.instance.registerObservers(runtimeLogManagerObserver);
		}else{
			closeStyle();	
		}
	}
}

