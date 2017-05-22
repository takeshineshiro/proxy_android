package cn.wsds.gamemaster.ui;

import java.io.IOException;
import java.lang.ref.WeakReference;

import com.subao.common.utils.InfoUtils;
import com.subao.net.NetManager;
import com.subao.upgrade.PortalUpgradeConfig;
import com.subao.upgrade.Upgrader;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewFlipper;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.SelfUpgrade;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.debugger.ActivityDebugMain;
import cn.wsds.gamemaster.dialog.CommonAlertDialog;
import cn.wsds.gamemaster.dialog.CommonDialog;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.service.VPNGlobalDefines.CloseReason;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.tools.FaultProcessor;
import cn.wsds.gamemaster.tools.FaultProcessor.Observer;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.view.SubaoProgressBar;
import cn.wsds.gamemaster.ui.view.Switch;
import cn.wsds.gamemaster.ui.view.Switch.OnBeforeCheckChangeListener;
import cn.wsds.gamemaster.ui.view.Switch.OnChangedListener;

@SuppressLint("InflateParams") public class ActivitySetting extends ActivityBase implements View.OnClickListener, Switch.OnChangedListener {

	private Switch checkCallRemind; // 防流量偷跑开关
	private Switch checkBootAutoaccel;//开机直接加速
	private final NetworkAnomalyAnalysisClickListener networkAnomalyAnalysisClickListener = new NetworkAnomalyAnalysisClickListener(this);
	
	/** 开关点击后状态改变之前的监听 */
	private final OnBeforeCheckChangeListener onBeforeCheckChangeListener = new OnBeforeCheckChangeListener() {
		
		@Override
		public boolean onBeforeCheckChange(Switch checkSwitch,boolean expectation) {
			switch (checkSwitch.getId()) {
			case R.id.check_boot_autoaccel:
				if(expectation){
					onOpenBootAutoAccelSwitch();
					return false;
				}else{
					ConfigManager.getInstance().setBootAutoAccel(false);
				}
				break;
			}
			return true;
		}
		
		private void onOpenBootAutoAccelSwitch() {
			CommonDialog dialog = new CommonAlertDialog(ActivitySetting.this);
			dialog.setMessage("迅游手游将在开机后自动开启加速");
			OnClickListener listener = new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(DialogInterface.BUTTON_POSITIVE == which){
						ConfigManager.getInstance().setBootAutoAccel(true);
						checkBootAutoaccel.setChecked(true);
					}
				}
			};
			dialog.setPositiveButton(R.string.confirm, listener);
			dialog.setNegativeButton(R.string.cancel, null);
			dialog.show();
		}
	};
	
	private EventObserver eventObserver = new EventObserver() {
		public void onAccelSwitchChanged(boolean state) {
			buttonCloseAccel.setEnabled(state);
		}

	};
	private View checkRootGroup;
	private View buttonCloseAccel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);
		setDisplayHomeArrow(R.string.setting_accel);
		initView();
		TriggerManager.getInstance().addObserver(eventObserver);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		TriggerManager.getInstance().deleteObserver(eventObserver);
		networkAnomalyAnalysisClickListener.cleanUp();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		checkRootGroup.setVisibility(View.GONE);
	}

	private String getVersionName() {
		String s = InfoUtils.getVersionName(this);
		if (TextUtils.isEmpty(s)) {
			s = getString(R.string.unknown);
		}
		return s;
	}

	private class VersionCtrlListener implements View.OnClickListener{
		
		private long lastTimeOfClick;
		private int clickCount;
		
		@Override
		public void onClick(View v) {
			long now = SystemClock.elapsedRealtime();
			if (now - lastTimeOfClick > 10000) {
				lastTimeOfClick = now;
				clickCount = 1;
			} else if(clickCount >= 9){
				clickCount = 0;
				UIUtils.turnActivity(ActivitySetting.this, ActivityDebugMain.class);
			} else{
				++clickCount;
			}
		}

	}
	
	private void doUpgrade() {
		if (!SelfUpgrade.getInstance().showDialogWhenNeed(this, true, false)) {
			UIUtils.showToast("当前版本已经是最新");
		}
	}
	
	/** 初始化版本样式根据是否有更新判断 */
	private void setVersionByUpdate(final Context context) {
		SelfUpgrade.getInstance().check(new Upgrader.Callback() {
			@Override
			public void onCheckNewVersionResult(boolean errorWhenDownload, PortalUpgradeConfig.Items items, int minVer) {
				boolean hasNewVersion = (items != null);
	        	findViewById(R.id.version_new).setVisibility(hasNewVersion ? View.VISIBLE : View.GONE);
			}
		});
	}

	private void initView() {
		
//		if(UIUtils.isCallRemindSupportCurrentRom()){
//			checkCallRemind = (Switch) findViewById(R.id.check_call_remind);
//			checkCallRemind.setChecked(ConfigManager.getInstance().getCallRemindGamePlaying());
//			checkCallRemind.setOnChangedListener(this);
//		}else{
//			findViewById(R.id.group_call_remind_gameplay).setVisibility(View.GONE);
//		}
		findViewById(R.id.group_call_remind_gameplay).setVisibility(View.GONE);

		checkBootAutoaccel = (Switch) findViewById(R.id.check_boot_autoaccel);
		checkBootAutoaccel.setChecked(ConfigManager.getInstance().getBootAutoAccel());
		checkBootAutoaccel.setOnBeforeCheckChangeListener(onBeforeCheckChangeListener);
		
		checkRootGroup = findViewById(R.id.check_root_group);
        Switch checkRoot = (Switch) findViewById(R.id.check_root);
        checkRoot.setChecked(ConfigManager.getInstance().isRootMode());
        checkRoot.setOnChangedListener(new OnChangedListener() {
			
			@Override
			public void onCheckedChanged(Switch checkSwitch, boolean checked) {
				ConfigManager.getInstance().setRootMode(checked);
				ConfigManager.getInstance().resetAutoChangedAccelModel();
				Statistic.addEvent(getApplicationContext(), Statistic.Event.ACC_ALL_SWITCH_ROOTMODE,checked ? "开" : "关");
			}
		});

		// 一键上传错误日志
		View buttonNetworkAnomalyAnalysis = findViewById(R.id.setting_text_upload_error_log);
		buttonNetworkAnomalyAnalysis.setOnClickListener(networkAnomalyAnalysisClickListener);

		// 版本
		TextView textVersion = (TextView) findViewById(R.id.version);
		textVersion.setText("版本 " + getVersionName());
		setVersionByUpdate(getApplicationContext());
		VersionCtrlListener l = new VersionCtrlListener();
		textVersion.setOnClickListener(l);
		// 退出APP
		findViewById(R.id.exit_app).setOnClickListener(this);

		findViewById(R.id.send_notice).setOnClickListener(this);
		findViewById(R.id.procces_clean).setOnClickListener(this);
		findViewById(R.id.floatwindow).setOnClickListener(this);
		findViewById(R.id.shortcut_create_button).setOnClickListener(this);
		findViewById(R.id.update).setOnClickListener(this);
		buttonCloseAccel = findViewById(R.id.button_close_accel);
		buttonCloseAccel.setOnClickListener(this);
		buttonCloseAccel.setEnabled(AccelOpenManager.isStarted());
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();
		switch (id) {
		case R.id.exit_app: // 点击了“退出”
			showExitConfirmDlg();
			break;
		case R.id.send_notice:
			UIUtils.turnActivity(this, ActivitySettingSendNotice.class);
			break;
		case R.id.floatwindow:
			UIUtils.turnActivity(this, ActivitySettingFloatWindow.class);
			break;
		case R.id.procces_clean:
			turnProcessClean();
			break;
		case R.id.shortcut_create_button:
			doCreateShortcut();
			break;
		case R.id.update:
			doUpgrade();
			break;
		case R.id.button_close_accel:
			if (AccelOpenManager.isStarted()) {
				Statistic.addEvent(AppMain.getContext(), Statistic.Event.ACC_HOMEPAGE_CLICK_STOP,
					AccelOpenManager.isRootModel() ? "ROOT" : "VPN");
				AccelProcessHelper.closeAccel(CloseReason.SETTING_CLOSE,ActivitySetting.this,null);
			}
			
			break;
		}
	}

	private void doCreateShortcut() {
		Statistic.addEvent(this, Statistic.Event.INTERACTIVE_SHORTCUT_CREATE, "设置页面里点击创建");
		ShortcutCreateHelper.Result result = ShortcutCreateHelper.createShortcut(this);
		switch (result) {
		case OK:
			ConfigManager.getInstance().setManuallyCreatedShortcut();
			UIUtils.showToast("桌面快捷启动已创建");
			break;
		case NO_GAME_FOUND:
			CommonAlertDialog dlg = new CommonAlertDialog(this);
			dlg.setCancelable(true);
			dlg.setPositiveButton(R.string.ok, null);
			dlg.setMessage(R.string.cannot_create_shortcut_when_no_games);
			dlg.setDismissWhenButtonClick(true);
			dlg.show();
			break;
		}
	}

	private void turnProcessClean() {
		Intent intent = new Intent(this, ActivityProccesClean.class);
		startActivity(intent);
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
	}

	// 当switch状态修改时，通知监听者
	@Override
	public void onCheckedChanged(Switch checkSwitch, boolean bChecked) {
		switch (checkSwitch.getId()) {
		case R.id.check_call_remind:
			UIUtils.onSwitchCallManagerChanged(bChecked, getApplicationContext());
			break;
		default:
			break;
		}
	}

	// 确认是否退出app
	private void showExitConfirmDlg() {
		CommonDialog dlg = new CommonAlertDialog(this);
		dlg.setTitle("提示");
		dlg.setMessage("是否确定退出？退出后将无法使用游戏加速");
		dlg.setPositiveButton("取消", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		dlg.setNegativeButton("退出", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
//				StatisticDefault.addEvent(ActivitySetting.this, StatisticDefault.Event.CLICK_EXIT);
				AppMain.exit(true);
			}
		});
		dlg.show();
	}
	
	

	private static final class NetworkAnomalyAnalysisClickListener implements View.OnClickListener {
		
		private final WeakReference<ActivitySetting> contextRef;
		
		private  AlertDialog dialog ;		
		private SubaoProgressBar progressbar ;		
		private EditText editText ;
		
		private final Observer observer = new Observer() {
			
			@Override
			public void uploadCompleted(boolean result) {
				if(result){
					UIUtils.showToast("网络异常分析日志已成功上传");
				}else{
					if(NetManager.getInstance().isConnected()){
						UIUtils.showToast("网络日志上传失败，请稍后重试");
					}else{					 
						UIUtils.showToast("网络已断开，网络日志上传失败");						 
					}
				}
				 
				FaultProcessor.getInstance().stop() ;
				FaultProcessor.getInstance().unregisterObserver(observer);
				
				if((dialog!=null)&&(dialog.isShowing())){
					MainHandler.getInstance().postDelayed(new Runnable(){
						@Override
						public void run() {											
							dialog.dismiss();							
						}
                		
                	}, 1000);    
				}								
			}
			
			@Override
			public void progressChanged(int progress) {
				if((dialog!=null)&&(dialog.isShowing())){
					if(progressbar!=null){
						progressbar.setPercent(progress);
					}
				}
			}
		};

		private NetworkAnomalyAnalysisClickListener(ActivitySetting context) {
			this.contextRef = new WeakReference<ActivitySetting>(context);		   
		}
		
		@Override
		public void onClick(View v) {	
			if(NetManager.getInstance().isDisconnected()){
				UIUtils.showToast("网络已断开，无法上传网络日志");
				return ;
			}
			//adt   
			if(FaultProcessor.getInstance().isRunning()){//抓取中
				promptCatching();
			}else{//没有抓取则提示开启
				startCatch();
			}
		}
		
		public void cleanUp(){
			FaultProcessor.getInstance().unregisterObserver(observer);
		}

		private void startCatch() {
			ActivitySetting context = contextRef.get();
			if(context == null)
				return;
			View layout = LayoutInflater.from(context).inflate(R.layout.dialog_contains_edit, null);
			editText = (EditText) layout.findViewById(R.id.edit_desc);
			editText.setHint("简单描述存在的问题和影响的游戏");
			editText.setMinHeight((int) context.getResources().getDimension(R.dimen.space_size_80));
			
			dialog = new AlertDialog.Builder(context) 
			.setView(layout).create();
			
		    final ViewFlipper flipper = (ViewFlipper)layout.findViewById(R.id.fault_process_flipper);
		    flipper.setDisplayedChild(0);
		    
		    progressbar = (SubaoProgressBar)layout.findViewById(R.id.fault_process_progress);
		    
			layout.findViewById(R.id.button_confirm).setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					try {
						String desc = editText.getText().toString();
						start(desc);
						UIUtils.showToast("网络异常分析已开启");
					} catch (IOException e) {
						e.printStackTrace();
					}	
					
					flipper.setDisplayedChild(1);
					editText.setEnabled(false);
					//dialog.dismiss();
				}
			});
			dialog.setCanceledOnTouchOutside(false);
			dialog .show();  
		}

		private void start(String desc) throws IOException {
		 
			FaultProcessor.getInstance().registerObservers(observer);
			FaultProcessor.getInstance().start(desc, contextRef.get() ,null);				 
		}
		
		private void promptCatching() {
			ActivitySetting context = contextRef.get();
			if(context == null)
				return;
			CommonDialog dialog = new CommonAlertDialog(context);
			dialog.setMessage("网络异常分析正在进行。");
			dialog.setPositiveButton("确定", null);
			dialog.show();
		}
		
		private Observer getObserver(){
			return observer ;
		}
	}

	@Override
	public void onBackPressed() {
		if(FaultProcessor.getInstance().isRunning()){			
			FaultProcessor.getInstance().stop();
			FaultProcessor.getInstance().unregisterObserver(networkAnomalyAnalysisClickListener.getObserver());	
			UIUtils.showToast("您已取消上传日志");
		}
		
		super.onBackPressed();
	}
	
	
	
}
