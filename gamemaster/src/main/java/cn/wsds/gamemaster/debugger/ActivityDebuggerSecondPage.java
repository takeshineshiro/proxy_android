package cn.wsds.gamemaster.debugger;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.KeyEvent;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.debugger.fragment.FragmentDebugAllEffect;
import cn.wsds.gamemaster.debugger.fragment.FragmentDebugDelayValue;
import cn.wsds.gamemaster.debugger.fragment.FragmentDebugLogPackUpload;
import cn.wsds.gamemaster.debugger.fragment.FragmentDebugMemoryClean;
import cn.wsds.gamemaster.debugger.fragment.FragmentDebugNetDelayChart;
import cn.wsds.gamemaster.debugger.fragment.FragmentDebugNoticeAccelResult;
import cn.wsds.gamemaster.debugger.fragment.FragmentDebugNoticeGamePlayAchieve;
import cn.wsds.gamemaster.debugger.fragment.FragmentDebugPing;
import cn.wsds.gamemaster.ui.ActivityBase;

public class ActivityDebuggerSecondPage extends ActivityBase {
	
	public static final String KEY_DEBUGGER_SECOND_PAGE = "key_debugger_second_page";
	public static final int VALUE_DEBUG_NOTICE_ACCEL_REUSLT = 0;
	public static final int VALUE_DEBUG_NOTICE_GAME_ACHIEVE = 1;
	public static final int VALUE_DEBUG_LOG_PACK = 2;
	public static final int VALUE_DEBUG_NETDELAY_CHART = 3;
	public static final int VALUE_DEBUG_TOAST_TEST = 4;
	public static final int VALUE_DEBUG_DELAY = 5;
//	public static final int VALUE_DEBUG_NET_DIAGNOSE_RESULT = 6;
	public static final int VALUE_DEBUG_MEMORY_CLEAN = 7;
	public static final int VALUE_DEBUG_PING = 8;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_debugger_secondpage);
		manageFragment();
	}

	private void manageFragment() {
		int value = getIntent().getIntExtra(KEY_DEBUGGER_SECOND_PAGE, -1);
		Fragment fragment;
		switch (value) {
		case VALUE_DEBUG_NOTICE_ACCEL_REUSLT:
			fragment = new FragmentDebugNoticeAccelResult();
			setDisplayHomeArrow("测试游戏加速结果");
			break;
		case VALUE_DEBUG_NOTICE_GAME_ACHIEVE:
			fragment = new FragmentDebugNoticeGamePlayAchieve();
			setDisplayHomeArrow("测试游戏使用成就");
			break;
		case VALUE_DEBUG_LOG_PACK:
			fragment = new FragmentDebugLogPackUpload();
			setDisplayHomeArrow("测试日志打包上传");
			break;
		case VALUE_DEBUG_NETDELAY_CHART:
			fragment = new FragmentDebugNetDelayChart();
			setDisplayHomeArrow("网络延迟折线图");
			break;
		case VALUE_DEBUG_TOAST_TEST:
			fragment = new FragmentDebugAllEffect();
			setDisplayHomeArrow("游戏内Toast测试");
			break;
		case VALUE_DEBUG_DELAY:
			fragment = new FragmentDebugDelayValue();
			setDisplayHomeArrow("延迟值功能测试");
			break;
//		case VALUE_DEBUG_NET_DIAGNOSE_RESULT:
//			fragment = new FragmentDebugNetDiagnoseResult();
//			setDisplayHomeArrow("网络诊断结果展示黑盒测试");
//			break;
		case VALUE_DEBUG_MEMORY_CLEAN:
			fragment = new FragmentDebugMemoryClean();
			setDisplayHomeArrow("内存清理测试");
			break;
		case VALUE_DEBUG_PING:
			fragment = new FragmentDebugPing();
			setDisplayHomeArrow("PING");
			break;
		default:
			return;
		}
		addFragment(fragment);
		
	}

	private void addFragment(Fragment fragment) {
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		// Replace whatever is in the fragment_container view with this fragment,
		// and add the transaction to the back stack
		transaction.replace(R.id.fragment_container, fragment);
//		transaction.addToBackStack(null);

		// Commit the transact	ion
		transaction.commit();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){     
			//FIXME 返回时总是返回到activity界面所以直接finish
			if(getFragmentManager().getBackStackEntryCount() == 1)
				finish();
		}
		return super.onKeyDown(keyCode, event);
	}

}
