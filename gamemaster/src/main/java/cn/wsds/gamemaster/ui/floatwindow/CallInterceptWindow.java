package cn.wsds.gamemaster.ui.floatwindow;

import android.content.Context;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import cn.wsds.gamemaster.PhoneCtrl;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.statistic.Statistic.Event;
import cn.wsds.gamemaster.ui.UIUtils;

import com.subao.resutils.WeakReferenceHandler;

public class CallInterceptWindow extends FloatWindow {
	
	private static CallInterceptWindow instance;
	private TextView contact;
	private ImageView hangup, ignore, accept;
	private String phoneNumber;
	private int taskId = -1;
//	private int marginTop, marginRight;
	
	public static CallInterceptWindow createInstance(Context context, String phoneNumber, int taskId){
		if (instance == null){
			int viewId = 0;
			int heightId = 0;
			int marginTopId = 0;
			int marginRightId = 0;
			if (isDisplayPortrait(context)) {
				viewId = R.layout.phone_intercept_portrait;
				heightId = R.dimen.space_size_129;
				marginTopId = R.dimen.space_size_124;
				marginRightId = R.dimen.space_size_16;
			} else {
				viewId = R.layout.phone_intercept_landscape;
				heightId = R.dimen.space_size_79;
				marginTopId = R.dimen.space_size_74;
				marginRightId = R.dimen.space_size_90;
			}
			View view = LayoutInflater.from(context).inflate(viewId, null);
			int height = (int)context.getResources().getDimension(heightId);
			int marginTop = (int)context.getResources().getDimension(marginTopId);
			int marginRight = (int)context.getResources().getDimension(marginRightId);
			instance = new CallInterceptWindow(context, phoneNumber, taskId, marginTop, marginRight);
			instance.addView(Type.DIALOG, view, 0, 0,LayoutParams.MATCH_PARENT, height);
			instance.animation(true);
		}
		return instance;
	}
	
	private static boolean isDisplayPortrait(Context context){
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels > dm.widthPixels;
	}
	
	public static void destoryInstance(){
		if (instance != null){
			CallInterceptWindow temp = instance;
			temp.destroy();
			instance = null;
		}
	}
	
	@Override
	protected void destroy() {
		UIUtils.setForground(taskId);
		myHandler.removeCallbacksAndMessages(null);
		if (!hasHandledCall){
			Statistic.addEvent(getContext(), Event.FLOATING_WINDOW_CALL_IN, "无操作");
		}
		taskId = -1;
		hasHandledCall = false;
//		PhoneInterceptPrompt.destroyInstance();
		super.destroy();
		myHandler.onIntercpteWindowexit();
	}
	
	public static boolean exist(){
		return instance != null;
	}
	
	private CallInterceptWindow(Context context, String phoneNumber, int taskId, int marginTop, int marginRight) {
		super(context);
		this.phoneNumber = phoneNumber;
		this.taskId = taskId;
//		this.enterTime = SystemClock.elapsedRealtime();
//		this.marginTop = marginTop;
//		this.marginRight = marginRight;
	}
	
	private boolean hasHandledCall = false;	
	
	@Override
	protected void onViewAdded(View view) {
		setPosition(0, -getHeight(), true);
		hangup = (ImageView) view.findViewById(R.id.call_hangup);
		ignore = (ImageView) view.findViewById(R.id.call_ignore);
		accept = (ImageView) view.findViewById(R.id.call_accept);
		contact = (TextView) view.findViewById(R.id.call_number);
		contact.setText(phoneNumber);
		hangup.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendStatisticAndSetHandleCall("挂断");
				PhoneCtrl.rejectCall(getContext());
			}
		});
		ignore.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendStatisticAndSetHandleCall("忽略");
				GameForgroundDetect.instance.setIgnore(true);
				animation(false);
			}
		});
		accept.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendStatisticAndSetHandleCall("接听");
				PhoneCtrl.acceptCall(getContext());
				GameForgroundDetect.instance.setHasCalled();
				myHandler.removeCallbacksAndMessages(null);
				CallUtils.changeToDefaultAnswer();
				destoryInstance();
			}
		});
		
	}
	
	private void sendStatisticAndSetHandleCall(String info){
		Statistic.addEvent(getContext(), Event.FLOATING_WINDOW_CALL_IN, info);
		hasHandledCall = true;
	}
	
	
	
	@Override
	protected boolean canDrag() {
		return false;
	}
	
	private void animation(boolean in){
		myHandler.removeCallbacksAndMessages(null);
		if (in){
			myHandler.onPhoneIncoming();
		}
		int what = in ? MyHandler.MSG_ANIMATION_IN : MyHandler.MSG_ANIMATION_OUT;
		Message msg = myHandler.obtainMessage(what, 0);
		myHandler.sendMessage(msg);
	}
	
	public static void hide(){
		if(instance != null){
			instance.animation(false);
		}
	}
	
	private final MyHandler myHandler = new MyHandler(this);
	private static class MyHandler extends WeakReferenceHandler<CallInterceptWindow>{
		private static final int MSG_ANIMATION_IN = 0;
		private static final int MSG_ANIMATION_OUT = 1;
		private static final int MSG_SET_FOREGROUND = 2;
		
		private static final int STEPS = 20;
		private static final int TIME_PER_STEP = 10;
		
		private long stopTime;

		private int taskId;
		private long delayMillis;
		
		public MyHandler(CallInterceptWindow ref) {
			super(ref);
		}

		@Override
		protected void handleMessage(CallInterceptWindow ref, Message msg) {
			switch (msg.what) {
			case MSG_ANIMATION_IN:
				int height = (Integer) msg.obj;
				if (height < ref.getHeight()){
					ref.setPosition(0, height - ref.getHeight(), true);
					height += ref.getHeight() / STEPS;
					msg = obtainMessage(MyHandler.MSG_ANIMATION_IN, height);
					sendMessageDelayed(msg, TIME_PER_STEP);
				} else {
					ref.setPosition(0, 0);
//					PhoneInterceptPrompt.createInstance(ref.getContext(), ref.marginTop, ref.marginRight);
				}
				break;
			case MSG_ANIMATION_OUT:
				int height2 = (Integer) msg.obj;
				if (height2 < ref.getHeight()){
					ref.setPosition(0, -height2, true);
					height2 += ref.getHeight() / STEPS;
					msg = obtainMessage(MyHandler.MSG_ANIMATION_OUT, height2);
					sendMessageDelayed(msg, TIME_PER_STEP);
				} else {
					ref.setPosition(0, -ref.getHeight());
					ref.destroy();
					instance = null;
				}
				break;
			case MSG_SET_FOREGROUND:
				if (SystemClock.elapsedRealtime() >= stopTime) {
					removeMessages(MSG_SET_FOREGROUND);
					return;
				}
				if(this.taskId <= 0){
					this.taskId = ref.taskId;
				}
				UIUtils.setForground(taskId);
				sendEmptyMessageDelayed(MSG_SET_FOREGROUND, delayMillis);
				break;
			default:
				break;
			}
		}

		public void onPhoneIncoming() {
			stopTime = SystemClock.elapsedRealtime() + 5000;
			this.delayMillis = 50;
			sendEmptyMessageDelayed(MSG_SET_FOREGROUND, this.delayMillis );
		}
		
		public void onIntercpteWindowexit() {
			stopTime = SystemClock.elapsedRealtime() + 1000;
			this.delayMillis = 20;
			sendEmptyMessage(MSG_SET_FOREGROUND);
		}
	}

}
