package cn.wsds.gamemaster.ui.accel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.netcheck.NetworkCheckManager;
import cn.wsds.gamemaster.service.VPNGlobalDefines.CloseReason;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.statistic.Statistic.Event;
import cn.wsds.gamemaster.ui.ActivityBootPrompt;
import cn.wsds.gamemaster.ui.accel.failprocessor.FailProcesser;
import cn.wsds.gamemaster.ui.accel.failprocessor.FailType;
import cn.wsds.gamemaster.ui.floatwindow.BoxInGame;

/**
 * 执行加速开启操作的抽象类
 */
public abstract class AccelOpener {

	/**
	 * 关闭VPN的原因
	 *//*
	public enum CloseReason {
		*//** 用户点击主菜单里的“关闭” *//*
		SETTING_CLOSE("主页关闭加速"),
		*//** 用户在桌面快捷方式里关闭 *//*
		DESKTOP_SHORTCUT_CLOSE("快捷方式关闭加速"),
		*//** Proxy层通知暂停 *//*
		BY_PROXY("后台通知"),
		*//** 程序正常退出 *//*
		APP_EXIT("主页退出"),
		*//** VpnService destroy *//*
		VPN_SERVICE_DESTROY("服务终止"),
		*//** VPN授权被吊销 *//*
		VPN_REVOKE("小钥匙处断开"),
		*//** 测试关闭 *//*
		DEBUG("测试");
		
		public final String desc;
		
		CloseReason(String desc) {
			this.desc = desc;
		}
	}*/
	
	/**
	 * 从哪里开启加速
	 */
	public enum OpenSource {
		/** 主界面 */
		Main("Z"),
		/** 开机启动 */
		Boot("J"),
		/** 快捷方式 */
		Shortcut("K"),
		/** 悬浮窗 */
		Floatwindow("X");
		public final String desc;
		OpenSource(String desc) {
			this.desc = desc;
		}
		
	}

	/**
	 * 开启结果监听接口
	 */
	public interface Listener {
		void onStartSucceed();
		void onStartFail(AccelOpener accelOpener,FailType type);
	}
	
    /**
     * 持用对listener的引用，避免外部频繁判断listener是否为空
     */
	protected static class ListenerRef implements Listener {
		private final Listener ref;
		private final boolean isRootModel;
		private final OpenSource openSource;
		private final Tester tester;
		
		public ListenerRef(Listener l, OpenSource openSource, boolean isRootModel, Tester tester) {
			this.ref = l;	
			this.openSource = openSource;
			this.isRootModel = isRootModel;
			this.tester = tester;
		}
		
		@Override
		public void onStartSucceed() {
			statisticOpenResult(true);
			if (ref != null) {
				ref.onStartSucceed();
			}
			TriggerManager.getInstance().raiseAccelSwitchChanged(true);
			GameManager.getInstance().clearAllAccelTime();
		}
		
		@Override
		public void onStartFail(AccelOpener accelOpener,FailType type) {
			statisticOpenResult(false);
			if (ref != null) {
				ref.onStartFail(accelOpener, type);
			}
		}
		
		public Tester.FakeOpenAccelResult fakeOpenAccel() {
			return this.tester == null ? Tester.FakeOpenAccelResult.NO_FAKE_FUNCTION : this.tester.fakeOpenAccel(this.isRootModel);
		}
		
		/**
		 * 统计事件
		 * @param result
		 */
		private void statisticOpenResult(boolean result){
			String statisticParam;
			Event eventId;
			if(result){
				statisticParam = getStatisticParam(isRootModel,openSource) + "S";
				eventId = Statistic.Event.ACC_ALL_START_SUCCESS_NEW;
			}else{
				statisticParam = getStatisticParam(isRootModel,openSource) + "F";
				eventId = Statistic.Event.ACC_ALL_START_FAIL_NEW;
			}
			Statistic.addEvent(AppMain.getContext(), eventId,statisticParam);
		}
		
	}
	
	/**
	 * 根据 加速模式 开启加速来源 返回开启加速统计信息
	 * @param isRootModel
	 * @param openSource
	 * @return
	 * 格式  加速 来源 + 加速模式 
	 */
	private static String getStatisticParam(boolean isRootModel,OpenSource openSource){
		String accelModel = isRootModel ?  "R" : "V"; 
		return openSource.desc + accelModel;
	}

	protected final ListenerRef listener;
	public final OpenSource openSource;
	private boolean isRootModel;
	private AccelOpenWorker accelOpenWorker;

	/**
	 * 测试接口
	 */
	public interface Tester {
		enum FakeOpenAccelResult {
			NO_FAKE_FUNCTION,
			OPEN_ACCEL_SUCCEED,
			OPEN_ACCEL_FAIL,
		}
		FakeOpenAccelResult fakeOpenAccel(boolean isRootMode);
	}
	
	/**
	 * 构造一个加速开启
	 * @param l 开启结果监听
	 * @param isRootModel 
	 */
	public AccelOpener(Listener l, OpenSource openSource, boolean isRootModel, Tester tester) {
		this.openSource = openSource;
		this.isRootModel = isRootModel;
		this.listener = new ListenerRef(l,openSource,isRootModel, tester);
	}
	

	
	/**
	 * 尝试开启加速。
	 */
	public final void tryOpen(final Activity activity) {

		if (this.isStarted()) {
			this.listener.onStartSucceed();
			return;
		}

		Context context = activity;
		if (context == null) {
			context = AppMain.getContext();
		}

		if (!AccelPromptStrategy.prepare(context)) {
			this.listener.onStartFail(this,FailType.ImpowerCancel);
			return;
		}

		Statistic.addEvent(context, Statistic.Event.ACC_ALL_START_NEW,getStatisticParam(isRootModel, openSource));

		if(!this.hasModel()){
			this.listener.onStartFail(this,FailType.DefectModel);
			return;
		}
		FailType failType = checkFailCondition(context);
		if(failType != null) {
			this.listener.onStartFail(this,failType);
			if(FailType.NetworkCheck.equals(failType)){
				NetworkCheckManager.addNetForbiddedEvent();
			}
			return;
		}
		//
		Tester.FakeOpenAccelResult result = this.listener.fakeOpenAccel();
		switch (result) {
		case NO_FAKE_FUNCTION:
			doOpen(activity, context);
			break;
		case OPEN_ACCEL_SUCCEED:
			this.listener.onStartSucceed();
			break;
		case OPEN_ACCEL_FAIL:
			this.listener.onStartFail(this, FailType.StartError);
			break;
		}

	}

	/**
	 * 由派生类执行具体的开启操作
	 * @param activity 页面，仅用于开启VPN时
	 * @param context 由调用者保证不为null的Context对象
	 */
	protected abstract void doOpen(Activity activity, Context context);

	/** 加速是否已经开启 */
	public abstract boolean isStarted();
	
	/** 关闭加速 */
	public abstract void close(CloseReason reason);
	
	/**
	 * 页面的onActivityResult()方法里调用
	 * @param request
	 * @param result
	 * @param data
	 */
	public abstract void checkResult(int request, int result, Intent data);
	
	/** 是否已经取得授权 */
	public abstract boolean isGotPermission();
	
	/** 预判断开启条件 */
	protected abstract FailType checkFailCondition(Context context);
	
	/** 当前是否是Root模式 */
	public boolean isRootModel(){
		return this.isRootModel;
	}

	public abstract void interrupt();
	
	/** 是否拥有这个模块 */
	protected abstract boolean hasModel();
	
	
	public void setAccelOPenWorker(AccelOpenWorker accelOPenWorker) {
		this.accelOpenWorker = accelOPenWorker;
	}
	
	public AccelOpenWorker getAccelOpenWorker(Context context){
		if(this.accelOpenWorker == null){
			return createAccelOpenWorker(context);
		}
		return this.accelOpenWorker;
	}
	
	private AccelOpenWorker createAccelOpenWorker(final Context context) {
		switch (openSource) {
		case Boot:
			return new AccelOpenWorker() {
				
				@Override
				public void openAccel() {
					ActivityBootPrompt.tryOpenAccel(context);					
				}
			};
		case Floatwindow:
			return new BoxInGame.FloatwindowAccelOpenWorker();
		default:
			return null;
		}
	}

	public abstract FailProcesser getFailProcesser();
}
