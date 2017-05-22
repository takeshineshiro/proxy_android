package cn.wsds.gamemaster.ui.mainfragment;

import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.tools.MobileSystemTypeUtil;
import cn.wsds.gamemaster.tools.MobileSystemTypeUtil.SystemType;
import cn.wsds.gamemaster.ui.ActivityInstructions;
import cn.wsds.gamemaster.ui.ActivityMiuiUserReminder;
import cn.wsds.gamemaster.ui.ActivityVivoUserReminder;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.accel.AccelOpenWorker;
import cn.wsds.gamemaster.ui.accel.animation.AccelAnimationListener;
import cn.wsds.gamemaster.ui.accel.animation.WideAnimation;
import cn.wsds.gamemaster.ui.view.SubaoPiece;

/**
 * 主界面加速关闭界面
 */
public class PieceAccelOff extends SubaoPiece {

	private final AccelOpenWorker accelOpenWorker;

	private final View startButtonGroup;
	private final View shadeGroup;
	private final View topShade;
	private final View bottomShade;
	private final View topLine;
	private final View bottomLine;

	private final StartButton startButton;

	private LeaveAnimation leaveAnimation;

	private final View pormpt ;

	public PieceAccelOff(AccelOpenWorker accelOpenWorker, ViewGroup container) {
		super(container, R.layout.fragment_accel_off);
		this.accelOpenWorker = accelOpenWorker;
		//
		View rootView = getView();
		this.topShade = rootView.findViewById(R.id.top_shade);
		this.bottomShade = rootView.findViewById(R.id.bottom_shade);
		this.topLine = rootView.findViewById(R.id.top_line);
		this.bottomLine = rootView.findViewById(R.id.bottom_line);
		this.startButtonGroup = rootView.findViewById(R.id.start_button);
		this.shadeGroup = rootView.findViewById(R.id.shade_group);
		this.pormpt=rootView.findViewById(R.id.pormpt);
		//
		startButton = new StartButton(startButtonGroup);
		startButton.changeImageResId(StartButton.ImageResId.createNormalRes());
		startButtonGroup.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (PieceAccelOff.this.accelOpenWorker != null) {
					PieceAccelOff.this.accelOpenWorker.openAccel();
				}
			}
		});
		shadeGroup.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				UIUtils.turnActivity(v.getContext(), ActivityInstructions.class);
			}
		});
				
		showReadMeTextIfNeed(rootView) ;
	}
	
	private void showReadMeTextIfNeed(View rootView){
		if(rootView == null){
			return ;
		}
		
        final String versionProp = MobileSystemTypeUtil.getSystemProp().prop ;
	    if(versionProp==null){
	    	return ;
	    }
	    
	    TextView readMeTextView ;
		if(needShowVivoReadMe(versionProp)){
			readMeTextView = (TextView)rootView.findViewById(R.id.vivo_read_me_text);
		}else if(needShowMiuiReadMe(versionProp)){
			readMeTextView = (TextView)rootView.findViewById(R.id.miui_read_me_text);
		}else{
			return ;
		}		 
		
		View readMeLayout = rootView.findViewById(R.id.read_me_text);	
		readMeLayout.setVisibility(View.VISIBLE);
		readMeTextView.setVisibility(View.VISIBLE);
		readMeTextView.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				if(needShowVivoReadMe(versionProp)){
					turnToVivoReadMe(v.getContext());
				}else{
					turnToMiuiReadMe(v.getContext());
				}
			}			
		});
        
	}
	
	//vivo Rom 2.0 以上（基于Android4.4.2）才支持后台高耗电管理功能
	private static boolean needShowVivoReadMe(String versionProp){		
		if(versionProp==null){
			return false ;
		}
		
		if((Build.MODEL.startsWith("vivo")
				&&("Funtouch OS_2.0".compareToIgnoreCase(versionProp) <= 0))){
			return true ;
		}else{
			return false ;
		}	
	}
	
	//MIUI 6 以上支持神隐模式
	private static boolean needShowMiuiReadMe(String versionProp){
		if(versionProp==null){
			return false ;
		}
		
		if((SystemType.MIUI.equals(MobileSystemTypeUtil.getSystemType()))
				&&("V6".compareToIgnoreCase(versionProp) <= 0)){
			return true;
		}else{
			return false ;
		}		
	}
	
	private static void turnToVivoReadMe(Context context){
		UIUtils.turnActivity(context, ActivityVivoUserReminder.class);
		Statistic.addEvent(context, Statistic.Event.VIVO_README, "vivo readme clicked !");
	}
	
	private static void turnToMiuiReadMe(Context context){
		UIUtils.turnActivity(context, ActivityMiuiUserReminder.class);
		Statistic.addEvent(context, Statistic.Event.XIAOMI_README, "xiaomi readme clicked !");
	}

	public interface LeaveAnimationListener {
		public void onLeaveAnimationEnd(PieceAccelOff who);
	}

	public void startLeaveAnimation(LeaveAnimationListener listener) {
		if (this.leaveAnimation == null) {
			this.leaveAnimation = new LeaveAnimation(listener);
			this.leaveAnimation.start();
		}
	}

	public void abortLeaveAnimation() {
		if (this.leaveAnimation != null) {
			this.leaveAnimation.abort();
			this.leaveAnimation = null;
		}
	}

	private class LeaveAnimation {

		private LeaveAnimationListener listener;
		private final Animation lineScaleAnimation;
		private final Animation buttonFadeOutAnimation;

		private int rectHalfHeight;
		private WideAnimation topAnimation, bottomAnimation;

		public LeaveAnimation(LeaveAnimationListener listener) {
			this.listener = listener;
			this.lineScaleAnimation = new ScaleAnimation(1.0f, 0f, 1f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_PARENT, 0f);
			this.lineScaleAnimation.setDuration(500);
			this.buttonFadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
			this.buttonFadeOutAnimation.setDuration(500);
			this.buttonFadeOutAnimation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {}

				@Override
				public void onAnimationRepeat(Animation animation) {}

				@Override
				public void onAnimationEnd(Animation animation) {
					end();
				}

			});
			setRectHalfHeight(shadeGroup, true);
		}

		public void start() {
			if (AccelOpenManager.isStarted()) {
				topAnimation = createAnimation(topShade, topLine);
				bottomAnimation = createAnimation(bottomShade, bottomLine);
				topAnimation.execute();
				bottomAnimation.execute();
			}
		}

		private void end() {
			this.abort();
			if (listener != null) {
				listener.onLeaveAnimationEnd(PieceAccelOff.this);
			}
			if (leaveAnimation == this) {
				leaveAnimation = null;
			}
		}

		private void setRectHalfHeight(final View rect, boolean isPostOnRectEmpty) {
			if (rect.getHeight() == 0) {
				if (!isPostOnRectEmpty) {
					return;
				}
				rect.post(new Runnable() {

					@Override
					public void run() {
						setRectHalfHeight(rect, false);
					}
				});
				return;
			}
			rectHalfHeight = rect.getHeight() >> 1;
		}

		private WideAnimation createAnimation(View view, final View line) {
			WideAnimation topAnimation = new WideAnimation(800, rectHalfHeight, view);
			topAnimation.setAccelAnimationListener(new AccelAnimationListener() {
				@Override
				public void onAnimationStart() {}

				@Override
				public void onAnimationEnd() {
					line.startAnimation(lineScaleAnimation);
					setPormptTextVisibility(View.INVISIBLE);
					addAnimationToStartButtonGroup(buttonFadeOutAnimation);
				}
			});
			return topAnimation;
		}

		public void abort() {
			if (topAnimation != null) {
				topAnimation.reset();
			}
			if (bottomAnimation != null) {
				bottomAnimation.reset();
			}
			clearAnimations();
			setPormptTextVisibility(View.VISIBLE);
		}
	}

	/**
	 * 开启动画按钮部分
	 */
	private static final class StartButton {

		private final ImageView imageBackground;
		private final ImageView imageText;

		private OnTouchListener onTouchListener = new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_OUTSIDE:
					changeImageResId(ImageResId.createNormalRes());
					break;
				case MotionEvent.ACTION_DOWN:
					changeImageResId(ImageResId.createOntouchRes());
					break;
				}
				return false;
			}

		};

		public StartButton(View viewGroup) {
			viewGroup.setOnTouchListener(onTouchListener);
			imageBackground = (ImageView) viewGroup.findViewById(R.id.start_button_background);
			imageText = (ImageView) viewGroup.findViewById(R.id.start_button_text);
		}

		private static final class ImageResId {
			private final int ringRes;
			private final int textRes;
			private final float ringAlpha;

			private ImageResId(int ringRes, int textRes, float ringAlpha) {
				this.ringRes = ringRes;
				this.textRes = textRes;
				this.ringAlpha = ringAlpha;
			}

			public static ImageResId createNormalRes() {
				return new ImageResId(R.drawable.homepage_button_ring_nor, R.drawable.homepage_button_text_nor, 0.5f);
			}

			public static ImageResId createOntouchRes() {
				return new ImageResId(R.drawable.homepage_button_ring_down, R.drawable.homepage_button_text_down, 1f);
			}
		}

		private void changeImageResId(ImageResId resId) {
			imageBackground.setImageResource(resId.ringRes);
			imageBackground.setAlpha(resId.ringAlpha);
			imageText.setImageResource(resId.textRes);
		}
	}

	private void clearAnimations() {
		topLine.clearAnimation();
		bottomLine.clearAnimation();
		startButtonGroup.clearAnimation();
	}

	private void addAnimationToStartButtonGroup(Animation ani) {
		if (startButtonGroup.getAnimation() == null) {
			startButtonGroup.startAnimation(ani);
		}
	}

	public void setTopBotoomShadeHeight(int value) {
		setViewLayoutHeight(topShade, value);
		setViewLayoutHeight(bottomShade, value);
	}

	private static void setViewLayoutHeight(View v, int height) {
		ViewGroup.LayoutParams lp = v.getLayoutParams();
		lp.height = height;
		v.setLayoutParams(lp);
	}

	public void setPormptTextVisibility(int visibility){
		pormpt.setVisibility(visibility);
	}

}
