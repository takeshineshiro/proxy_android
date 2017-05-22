package cn.wsds.gamemaster.ui.floatwindow;

import android.content.Context;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindowCommon.MobileNetType;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindowCommon.OutColorType;

import com.subao.common.net.NetTypeDetector;
import com.subao.resutils.WeakReferenceHandler;

public class FloatWindowInGameEar extends FloatWindow {
	
	private static FloatWindowInGameEar instance; 
	
	private RelativeLayout wholeLayout;
	private ImageView floatBox;
//	private ImageView floatSignalBg;
	private ImageView floatSignal;
	private ImageView floatNetType;
	private ImageView floatWifi;
	
	private NetTypeDetector.NetType netType;
	private OutColorType outColorType;
	private MyHandler myHandler;
	private final FloatWindowInGame floatWindowInGame;
	
	public static void createInstance(Context context, int x, int y, NetTypeDetector.NetType netType, OutColorType outColorType){
		if (instance == null){
			instance = new FloatWindowInGameEar(context, netType, outColorType);
			View view = LayoutInflater.from(context).inflate(instance.floatWindowInGame.getConfig().getEarLayoutResource(), null, false);
			instance.addView(FloatWindow.Type.TOAST, view, x, y);
		}
	}

	public static void destoryInstance(){
		if (instance != null){
			instance.destroy();
			instance = null;
		}
	}
	
	public static boolean exists(){
		return instance != null;
	}

	private FloatWindowInGameEar(Context context, NetTypeDetector.NetType netType, OutColorType outColorType) {
		super(context);
		this.netType = netType;
		this.outColorType = outColorType;
		this.myHandler = new MyHandler(this);
		this.floatWindowInGame = FloatWindowInGame.getInstance();
	}
	
	private void setRectColor(OutColorType outColorType){
		floatBox.setImageResource(floatWindowInGame.getConfig().getBoxRectResource(outColorType));
	}
	
	public static void setInstanceRectColor(OutColorType outColorType){
		if (instance != null){
			instance.setRectColor(outColorType);
		}
	}

	@Override
	protected void onViewAdded(View view) {
		wholeLayout = (RelativeLayout) view.findViewById(R.id.float_box_whole_layout);
		floatBox = (ImageView) view.findViewById(R.id.float_box);
		floatSignal = (ImageView) view.findViewById(R.id.float_signal);
		floatNetType = (ImageView) view.findViewById(R.id.float_net_type);
		floatWifi = (ImageView) view.findViewById(R.id.float_wifi);
		
		setRectColor(outColorType);
		setMargin();
		FloatwindowInGameConfig config = floatWindowInGame.getConfig();
		if (netType == NetTypeDetector.NetType.WIFI){
			floatSignal.setVisibility(View.GONE);
			floatNetType.setVisibility(View.GONE);
			floatWifi.setVisibility(View.VISIBLE);
			floatWifi.setImageResource(config.getWifiSignalResource());
		} else {
			floatSignal.setVisibility(View.VISIBLE);
			floatNetType.setVisibility(View.VISIBLE);
			floatWifi.setVisibility(View.GONE);
			MobileNetType mobileNetType;
			switch (netType) {
			case MOBILE_2G:
				mobileNetType = MobileNetType.Type_2G;
				break;
			case MOBILE_3G:
				mobileNetType = MobileNetType.Type_3G;
				break;
			case MOBILE_4G:
				mobileNetType = MobileNetType.Type_4G;
				break;
			default:
				mobileNetType = MobileNetType.Type_2G;
				break;
			}
			floatSignal.setImageResource(config.getBoxSignalResource());
			floatNetType.setImageResource(config.getBoxNetTypeResource(mobileNetType));
		}
		
		adjustPosition();
		
		Animation animationIn = AnimationUtils.loadAnimation(getContext(), R.anim.float_box_scale_in);
		wholeLayout.clearAnimation();
		wholeLayout.startAnimation(animationIn);
		myHandler.sendEmptyMessageDelayed(MyHandler.MSG_SCALE_OUT, MyHandler.DELAY);
	}
	
	private void setMargin(){
//		int dimenSignalLeft, dimenNetTypeLeft, dimenWifiLeft;
//		switch (FloatWindowMeasure.getCurrentType()) {
//		case MINI:
//			dimenSignalLeft = MetricsUtils.dp2px(getContext(), floatWindowInGame.isCurrenVisibleHunterSkin ? 15 :18);
//			dimenNetTypeLeft = MetricsUtils.dp2px(getContext(), 3);
//			dimenWifiLeft = MetricsUtils.dp2px(getContext(), 24);
//			break;
//		default:
//			dimenSignalLeft = MetricsUtils.dp2px(getContext(), floatWindowInGame.isCurrenVisibleHunterSkin ? 20 :24);
//			dimenNetTypeLeft = MetricsUtils.dp2px(getContext(), 4);
//			dimenWifiLeft = MetricsUtils.dp2px(getContext(), 32);
//			break;
//		}
		FloatwindowInGameConfig config = floatWindowInGame.getConfig();
		changeViewMarginLeft(floatSignal,config.getPxEarNetSignalLeft());
		changeViewMarginLeft(floatNetType,config.getPxEarNetTypeLeft());
		changeViewMarginLeft(floatWifi,config.getPxEarWifiLeft());
	}

	private void changeViewMarginLeft(View view,int space) {
		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
		layoutParams.setMargins(space, 0, 0, 0);
		view.setLayoutParams(layoutParams);
	}
	
	private void adjustPosition(){
		int x = floatWindowInGame.getCenterX();
		int y = floatWindowInGame.getY();
		reLayout(x, y, floatWindowInGame.getConfig().getPxEarWidth(),  floatWindowInGame.getConfig().getPxEarHeight());
		floatWindowInGame.savePosition();
		floatWindowInGame.setCenterPosition(getX(), getCenterY());
	}

	@Override
	protected boolean canDrag() {
		return false;
	}
	
	@Override
	protected void destroy() {
		wholeLayout.clearAnimation();
		if (myHandler != null) {
			myHandler.removeCallbacksAndMessages(null);
			myHandler = null;
		}
		if (floatWindowInGame != null) {
			floatWindowInGame.restorePosition();
		}
		super.destroy();
	}
	
	private static class MyHandler extends WeakReferenceHandler<FloatWindowInGameEar>{
		private static final int MSG_SCALE_OUT = 0;
		private static final int DELAY = 3500;

		public MyHandler(FloatWindowInGameEar ref) {
			super(ref);
		}

		@Override
		protected void handleMessage(FloatWindowInGameEar ref, Message msg) {
			switch (msg.what) {
			case MSG_SCALE_OUT:
				Animation animationOut = AnimationUtils.loadAnimation(ref.getContext(), R.anim.float_box_scale_out);
				animationOut.setAnimationListener(new AnimationListener() {
					
					@Override
					public void onAnimationStart(Animation animation) {
					}
					
					@Override
					public void onAnimationRepeat(Animation animation) {
					}
					
					@Override
					public void onAnimationEnd(Animation animation) {
						destoryInstance();
					}
				});
				ref.wholeLayout.clearAnimation();
				ref.wholeLayout.startAnimation(animationOut);
				break;

			default:
				break;
			}
		}
		
	}
	
}
