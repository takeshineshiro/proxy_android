package cn.wsds.gamemaster.ui.floatwindow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import cn.wsds.gamemaster.R;

import com.subao.utils.MetricsUtils;

public class CleanMemoryManual extends FloatWindow {
	
	//==== 动画
	private TheDoor openTheDoor;
	private TheDoor closeTheDoor;
	private ApertureRotate apertureRotate;
	private Apsaras apsaras;
	//=========
	private CharSequence showContent;
	private static CleanMemoryManual instance;

	private CleanMemoryManual(Context context) {
		super(context);
	}
	
	public static CleanMemoryManual getInstance() {
		return instance;
	}

	@SuppressLint("InflateParams")
	/**
	 * 创建自动清理实例并将动画显示到屏幕
	 * @param context
	 * @param x 当前悬浮窗 x 坐标
	 * @param y 当前悬浮窗 y 坐标
	 * @return
	 */
	public static CleanMemoryManual createInstance(Context context,int x,int y){
		if (instance == null) {
			instance = new CleanMemoryManual(context);
			View view = LayoutInflater.from(context).inflate(R.layout.floatwindow_memory_clean_manual, null);
			instance.addView(FloatWindow.Type.TOAST, view, x, y);
		}
		return instance;
	}
	
	private static final class ViewHolder{
		private final ImageView imageSaucer;
		private final ImageView imageDoor;
		private final View imageBackground;
		private final View imageWindow;
		private final ImageView imageRing;

		public ViewHolder(View group) {
			this.imageSaucer = (ImageView) group.findViewById(R.id.image_saucer);
			this.imageDoor = (ImageView) group.findViewById(R.id.image_door);
			this.imageRing = (ImageView) group.findViewById(R.id.image_ring);
			this.imageBackground = group.findViewById(R.id.image_background);
			this.imageWindow = group.findViewById(R.id.image_window);
		}
	}
	
	@Override
	protected void onViewAdded(View view) {
		if(!resetLayout()){
			destoryInstance();
			return;
		}
		ViewHolder viewHolder = new ViewHolder(view);
		openTheDoor = new TheDoor(viewHolder,true,null);
		closeTheDoor = new CloseTheDoor(viewHolder,createCloseTheDoorAnimation());
		apsaras = new Apsaras(viewHolder.imageRing, viewHolder.imageSaucer);
		apertureRotate = new ApertureRotate(viewHolder.imageRing, viewHolder.imageSaucer);
		apertureRotate.start();
	}

	private OnAnimationEndListener createCloseTheDoorAnimation() {
		return new OnAnimationEndListener() {
			
			@Override
			public void onAnimationEnd() {
				if (apsaras != null) {
					apsaras.start();
				}
			}
		};
	}
	
	/**
	 * 重置坐标
	 * @return true 重置成功 false 重置失败
	 */
	private boolean resetLayout() {
		Context context = getContext();
		Point screenSize = MetricsUtils.getDevicesSizeByPixels(context);
		int xScreenCenter = screenSize.x >> 1;
		int x = getX();
		int padding = MetricsUtils.dp2px(context, 8);
		int halfWidth = getWidth()>>1;
		FloatWindowInGame boxInstance = FloatWindowInGame.getInstance();
		if(boxInstance==null){
			return false;
		}
		int left = xScreenCenter - halfWidth - padding - boxInstance.getWidth();
		int right = xScreenCenter + halfWidth + padding;
		// 中心点 左侧X坐标
		int centerX = xScreenCenter - halfWidth;
		if(x < right && x > left){
			if(getCenterY() > screenSize.y>>1){
				setPosition(centerX, 0);
			}else{
				setPosition(centerX, screenSize.y - getHeight());
			}
		}else{
			int y = boxInstance.getCenterY() - (getHeight() >> 1);
			y = clamp(y, 0, screenSize.y - getHeight());
			setPosition(centerX, y);
		}
		return true;
	}

	public void openTheDoor() {
		if (apertureRotate != null) {
			apertureRotate.stop();
		}
		if (openTheDoor != null) {
			openTheDoor.start();
		}
	}
	
	public void clean(String showContent) {
		if (openTheDoor != null) {
			openTheDoor.stop();
		}
		this.showContent = showContent;
		FloatWindowInGame.setInstanceVisibility(View.GONE);
		if (apertureRotate != null) {
			apertureRotate.stop();
		}
		if (closeTheDoor != null) {
			closeTheDoor.start();
		}
	}
	
	@Override
	protected boolean canDrag() {
		return false;
	}
	
	private interface OnAnimationEndListener {
		public void onAnimationEnd();
	}
	
	/**
	 * 引导动画
	 */
	private static final class ApertureRotate {
		private final ImageView imageRing;
		private final ImageView imageSaucer;
		private final Animation rotateAnimation = new RotateAnimation(0f, 361f,Animation.RELATIVE_TO_SELF,0.5f,
				Animation.RELATIVE_TO_SELF,0.5f);
		private ApertureRotate(ImageView imageRing,ImageView imageSaucer) {
			this.imageRing = imageRing;
			this.imageSaucer = imageSaucer;
			rotateAnimation.setDuration(500);
			rotateAnimation.setRepeatCount(-1);
		}
		public void start(){
			imageSaucer.setImageResource(R.drawable.loating_window_flying_saucer_normal);
			imageRing.setImageResource(R.drawable.loating_window_flying_saucer_aperture);
			imageRing.setVisibility(View.VISIBLE);
			imageRing.startAnimation(rotateAnimation);
		}
		public void stop(){
			imageRing.clearAnimation();
			imageRing.setVisibility(View.GONE);
		}
	}
	
	private static final class TheDoorAnimationDrawableCreater {
		public static final int FRAME_DURATION = 50;
		@SuppressWarnings("deprecation")
		private static AnimationDrawable createTheDoorAnimationDrawable(Context context,boolean isOpenTheDoor){
			AnimationDrawable animationDrawable = new AnimationDrawable();
			animationDrawable.setOneShot(true);
			Resources resources = context.getResources();
			int[] drawableIds = isOpenTheDoor ? getOpenTheDoorDrawableIds() : getCloseTheDoorDrawableIds();
			for (int id : drawableIds) {
				animationDrawable.addFrame(resources.getDrawable(id), FRAME_DURATION);
			}
			return animationDrawable;
		}
		
		private static int[] getOpenTheDoorDrawableIds(){
			return new int[]{
				R.drawable.loating_window_flying_saucer_door_01,
				R.drawable.loating_window_flying_saucer_door_02,			
				R.drawable.loating_window_flying_saucer_door_03,
				R.drawable.loating_window_flying_saucer_door_04,
				R.drawable.loating_window_flying_saucer_door_05,
				R.drawable.loating_window_flying_saucer_door_06,			
				R.drawable.loating_window_flying_saucer_door_07,
				R.drawable.loating_window_flying_saucer_door_08
			};
		}
		
		private static int[] getCloseTheDoorDrawableIds(){
			return new int[]{
				R.drawable.loating_window_flying_saucer_door_08,
				R.drawable.loating_window_flying_saucer_door_07,
				R.drawable.loating_window_flying_saucer_door_06,			
				R.drawable.loating_window_flying_saucer_door_05,
				R.drawable.loating_window_flying_saucer_door_04,
				R.drawable.loating_window_flying_saucer_door_03,
				R.drawable.loating_window_flying_saucer_door_02,			
				R.drawable.loating_window_flying_saucer_door_01,
			};
		}
	}
	
	/**
	 * 门相关的动画
	 */
	private static class TheDoor {
		private final ImageView imageDoor;
		private final View imageBackground;
		private final AnimationDrawable drawable;
		public final int drawableDurtion;
		private final Runnable action = new Runnable() {
			
			@Override
			public void run() {
				end();
			}
		};
		private OnAnimationEndListener onAnimationListener;
		protected TheDoor(ViewHolder viewHolder,boolean isOpen,OnAnimationEndListener onAnimationEndListener) {
			this.imageDoor = viewHolder.imageDoor;
			this.imageBackground = viewHolder.imageBackground;
			this.drawable = TheDoorAnimationDrawableCreater.createTheDoorAnimationDrawable(imageDoor.getContext(), isOpen);
			this.drawableDurtion = drawable.getNumberOfFrames() * TheDoorAnimationDrawableCreater.FRAME_DURATION;
			this.onAnimationListener = onAnimationEndListener;
		}
		public void start(){
			setVisiable(View.VISIBLE);
			imageDoor.setImageDrawable(drawable);
			drawable.start();
			imageDoor.postDelayed(action,drawableDurtion);
		}
		public void stop(){
			drawable.stop();
			imageDoor.removeCallbacks(action);
			setVisiable(View.GONE);
		}
		
		protected void setVisiable(int visibility){
			imageDoor.setVisibility(visibility);
			imageBackground.setVisibility(visibility);
		}
		public void end(){
			if(onAnimationListener!=null){
				onAnimationListener.onAnimationEnd();
			}
		}
	}
	
	/**
	 * 关门动画
	 */
	private static class CloseTheDoor extends TheDoor {

		private final View window;
		private final Animation windowFadeOutAnimation = new AlphaAnimation(1.0f,0.0f);
		protected CloseTheDoor(ViewHolder viewHolder,OnAnimationEndListener onAnimationEndListener) {
			super(viewHolder, false, onAnimationEndListener);
			this.window = viewHolder.imageWindow;
			windowFadeOutAnimation.setDuration(drawableDurtion);
		}
		@Override
		public void start() {
			super.start();
			window.startAnimation(windowFadeOutAnimation);
		}
		
		@Override
		public void end() {
			super.end();
			setVisiable(View.GONE);
			window.clearAnimation();
		}
		
		@Override
		protected void setVisiable(int visibility) {
			super.setVisiable(visibility);
			window.setVisibility(visibility);
		}
	}
	
	/**
	 * 飞天动画
	 */
	private static final class Apsaras {

		private final ImageView imageRing;
		private final ImageView imageSaucer;
		private final Animation ringApsaras = createRingApsarasAnimatonSet();
		private final Animation fadeOutAnimation = new AlphaAnimation(1f,0f);
		private AnimationDrawable saucerDrawable;
		private final Runnable ringApsarasAction = new Runnable() {
			
			@Override
			public void run() {
				stopApsaras();
				imageSaucer.startAnimation(fadeOutAnimation);
			}
		};
		private Apsaras(final ImageView imageRing,final ImageView imageSaucer) {
			this.imageRing = imageRing;
			this.imageSaucer = imageSaucer;
			fadeOutAnimation.setDuration(1000);
			fadeOutAnimation.setAnimationListener(new AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {}
				
				@Override
				public void onAnimationRepeat(Animation animation) {}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					if(CleanMemoryManual.instance!=null){
						CleanMemoryManual.instance.cleanComplete();
					}
				}
			});
		}
		
		private Animation createRingApsarasAnimatonSet(){
			AnimationSet animationSet = new AnimationSet(false);
			Animation alphaAnimation = new AlphaAnimation(1.0f, 0.4f);
			alphaAnimation.setRepeatCount(Animation.INFINITE);
			alphaAnimation.setRepeatMode(Animation.REVERSE);
			alphaAnimation.setDuration(100);
			Animation translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0f,
					Animation.RELATIVE_TO_SELF,0f,
					Animation.RELATIVE_TO_SELF,0f,
					Animation.RELATIVE_TO_SELF,-0.2f);
			translateAnimation.setDuration(400);
			animationSet.addAnimation(alphaAnimation);
			animationSet.addAnimation(translateAnimation);
			
			return animationSet;
		}
		public void start(){
			imageSaucer.setImageResource(R.drawable.saucer_rotate);
			saucerDrawable = (AnimationDrawable) imageSaucer.getDrawable();
			saucerDrawable.start();
			imageRing.setImageResource(R.drawable.loating_window_flying_saucer_wind);
			imageRing.setVisibility(View.VISIBLE);
			imageRing.startAnimation(ringApsaras);
			imageRing.postDelayed(ringApsarasAction, 400);
		}
//		public void stop(){
//			stopApsaras();
//		}

		private void stopApsaras() {
			imageRing.clearAnimation();
			imageRing.setVisibility(View.GONE);
			imageSaucer.setImageResource(R.drawable.loating_window_flying_saucer_normal);
			if(saucerDrawable!=null){
				saucerDrawable.stop();
			}
		}
	
	}
	
	private void cleanComplete() {
		if(showContent!=null){
			ToastMemoryClean.show(getContext(), showContent);
		}
		CleanMemoryManual.destoryInstance();
		FloatWindowInGame floatwindowInGame = FloatWindowInGame.getInstance();
		if(floatwindowInGame == null){
			return;
		}
		floatwindowInGame.restorePosition();
		FloatWindowInGame.setInstanceVisibility(View.VISIBLE);
	}
	
	/**
	 * 飞碟是否在前台
	 * @return
	 */
	public static boolean isSaucerInForeground(){
		return instance!=null;
	}

	/**
	 * 是否可以清理
	 *  依据 x,y是否在飞碟上
	 * @param x 小悬浮窗中心点 x 坐标
	 * @param y 小悬浮窗中心点 y 坐标
	 * @return
	 */
	public boolean canClean(int x,int y) {
		int left = getX();
		int right = getY();
		Rect rect = new Rect(left, right,left + getWidth(),right +getHeight());
		return rect.contains(x, y);
	}
	
	public static void destoryInstance(){
		if(instance!=null){
			instance.destroy();
			instance = null;
		}
	}
	
}
