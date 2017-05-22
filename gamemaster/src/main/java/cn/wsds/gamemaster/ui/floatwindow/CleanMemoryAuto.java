package cn.wsds.gamemaster.ui.floatwindow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.TextView;
import cn.wsds.gamemaster.R;

public class CleanMemoryAuto extends FloatWindow{

	
	private static CleanMemoryAuto instance;
	private final int cleanSize;
	
	private CleanMemoryAuto(Context context,int size) {
		super(context);
		this.cleanSize = size;
	}
	
	@SuppressLint("InflateParams")
	public static void createInstance(Context context,int x,int y,int size){
		if (instance == null) {
			instance = new CleanMemoryAuto(context,size);
			View view = LayoutInflater.from(context).inflate(R.layout.floatwindow_memory_clean_auto, null);
			instance.addView(FloatWindow.Type.TOAST, view, x, y);
			FloatWindowInGame.setInstanceVisibility(View.GONE);
		}
	}

	@Override
	protected void onViewAdded(View view) {
		reLayout(getX(), getY(), getWidth(), getHeight());
		ImageView robot = (ImageView) view.findViewById(R.id.robot);
		CleanSmoke cleanSmoke = new CleanSmoke(view,robot);
		cleanSmoke.cleanSmokeing();
	}

	@Override
	protected boolean canDrag() {
		return false;
	}
	
    public static void destory(){
    	if(instance!=null){
    		FloatWindowInGame.setInstanceVisibility(View.VISIBLE);
    		instance.destroy();
    		instance=null;
    	}
    }
	
	private static final class CleanSmoke {
		private final View[] smokeViews = new View[5];
		private final AnimationDrawable drawable;
		private final View viewGroup;
		private final ImageView robot;
		private static final int PER_DURATION = 100;
		private long beginTime;
		private final Animation fadeOutAnimation = new AlphaAnimation(1.0f, 0.5f);
		private final Animation fadeInAnimation = new AlphaAnimation(0.5f, 1.0f);
		private Runnable cleanRunnbale = new Runnable() {
			
			@Override
			public void run() {
				cleanSmokeing();
			}
		};
		private CleanSmoke(View viewGroup,final ImageView robot) {
			this.viewGroup = viewGroup;
			this.robot = robot;
			smokeViews[0] = viewGroup.findViewById(R.id.image_smoke_first);
			smokeViews[1] = viewGroup.findViewById(R.id.image_smoke_second);
			smokeViews[2] = viewGroup.findViewById(R.id.image_smoke_third);
			smokeViews[3] = viewGroup.findViewById(R.id.image_smoke_fourth);
			smokeViews[4] = viewGroup.findViewById(R.id.image_smoke_fifth);
			drawable = (AnimationDrawable) robot.getDrawable();
			
			fadeOutAnimation.setDuration(200);
			fadeOutAnimation.setAnimationListener(new AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {}
				
				@Override
				public void onAnimationRepeat(Animation animation) {}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					robot.setImageResource(R.drawable.loating_window_flying_automatic_clean_03);
					robot.setAlpha(0.5f);
					robot.startAnimation(fadeInAnimation);
				}
			});
			fadeInAnimation.setDuration(500);
			fadeInAnimation.setAnimationListener(new AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {}
				
				@Override
				public void onAnimationRepeat(Animation animation) {}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					robot.clearAnimation();
					complete();
				}
			});
		}
		
		public void cleanSmokeing(){
			if(beginTime == 0){
				beginTime = SystemClock.elapsedRealtime();
				drawable.start();
			}
			int step = (int) ((SystemClock.elapsedRealtime() - beginTime)/PER_DURATION);
			int doubleLengh = smokeViews.length * 2;
			if(step < 0 || step >= doubleLengh){ // 这里判断step<0，是因为某个坑爹设备上诡异地出现了负值
				drawable.stop();
				robot.startAnimation(fadeOutAnimation);
				return;
			}
			if(smokeViews.length > step){
				smokeViews[step].setVisibility(View.VISIBLE);
			}else{
				int index = doubleLengh - step - 1;
				smokeViews[index].setVisibility(View.GONE);
			}
			viewGroup.postDelayed(cleanRunnbale, PER_DURATION);
		}

		private void complete() {
			StarFlicker starFlicker = new StarFlicker(viewGroup, robot);
			starFlicker.starRun();
		}

//		public void interrupt() {
//			viewGroup.removeCallbacks(cleanRunnbale);
//			robot.clearAnimation();
//		}
	}
	
	
	private static final class StarFlicker {
		private final View firstStar;
		private final View sencondStar;
		private final View starGroup;
		private final ImageView robot;
		private static final int DELTA = 200;
		private final Runnable runnable = new Runnable() {
			
			@Override
			public void run() {
				starRun();
			}
		};
		
		private long startTime;
		public StarFlicker(View starGroup,ImageView robot) {
			this.starGroup = starGroup;
			this.firstStar = starGroup.findViewById(R.id.image_star_first);
			this.sencondStar = starGroup.findViewById(R.id.image_star_second);
			this.robot = robot;
		}
		
		private void starRun() {
			if(startTime == 0){
				startTime = SystemClock.elapsedRealtime();
				init();
			}
			long elapsed = SystemClock.elapsedRealtime() - startTime;
			int step = elapsed == 0 ? 0 : (int) elapsed/ DELTA;
			switch (step) {
			case 0:
			case 2:
				firstStar.setAlpha(1.0f);
				sencondStar.setAlpha(0.5f);
				break;
			case 1:
			case 3:
				firstStar.setAlpha(0.5f);
				sencondStar.setAlpha(1.0f);
				break;
			case 4:
				sencondStar.setAlpha(0.5f);
				break;
			case 5:
			default:
				end();
				return;
			}
			starGroup.postDelayed(runnable, DELTA);
		}

		private void init() {
			firstStar.setVisibility(View.VISIBLE);
			sencondStar.setVisibility(View.VISIBLE);
			robot.setAlpha(1.0f);
		}

		private void end() {
			firstStar.setVisibility(View.GONE);
			sencondStar.setVisibility(View.GONE);
			robot.setAlpha(0.5f);
			interrupt();
			if(instance != null){
				Complete complete = new Complete(starGroup, robot,instance.cleanSize);
				complete.showResult();
			}
		}
		
		public void interrupt() {
			starGroup.removeCallbacks(runnable);
		}
	}
	
	private static final class Complete {
		private final View viewGroup;
		private final TextView textResult;
		private final TextView textFloatBox;
		private final View robot;
		private final Animation robotAnimation = new AlphaAnimation(0.5f, 0.0f);
		private final Animation resultFadeInAnimation = new AlphaAnimation(0.5f, 1.0f);
		private final Animation resultFadeOutAnimation = new AlphaAnimation(1.0f, 0f);
		
		private final Runnable runnable = new Runnable() {
			
			@Override
			public void run() {
				textResult.startAnimation(resultFadeOutAnimation);
				floatBox.startAnimation(resultFadeOutAnimation);			
			}
		};
		private View floatBox;

		private Complete(final View viewGroup,final View robot,int size) {
			this.viewGroup = viewGroup;
			this.robot = robot;
			textResult = (TextView) viewGroup.findViewById(R.id.text_result);
			textFloatBox = (TextView) this.viewGroup.findViewById(R.id.float_box);
			setTextValue(size);
			
			floatBox = viewGroup.findViewById(R.id.float_box);
			robotAnimation.setDuration(500);
			robotAnimation.setAnimationListener(new AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {}
				
				@Override
				public void onAnimationRepeat(Animation animation) {}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					robot.setVisibility(View.GONE);
					textResult.setVisibility(View.VISIBLE);
					floatBox.setVisibility(View.VISIBLE);
					textResult.startAnimation(resultFadeInAnimation);
					floatBox.startAnimation(resultFadeInAnimation);
				}
			});
			resultFadeInAnimation.setDuration(1000);
			resultFadeInAnimation.setAnimationListener(new AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {}
				
				@Override
				public void onAnimationRepeat(Animation animation) {}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					viewGroup.removeCallbacks(runnable);
					viewGroup.postDelayed(runnable, 2000);
				}
			});
			resultFadeOutAnimation.setDuration(1000);
			resultFadeOutAnimation.setAnimationListener(new AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {}
				
				@Override
				public void onAnimationRepeat(Animation animation) {}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					textResult.setVisibility(View.GONE);
					floatBox.setVisibility(View.GONE);
					end();
				}
			});
		}

		private void setTextValue(int size) {
			if(size == 0){
				textResult.setText("自动\n清理");
				textFloatBox.setText("已完成");
			}else{
				textResult.setText(getSpannable(size));
				textFloatBox.setText("已清理");
			}
		}
		
		protected void end() {
			CleanMemoryAuto.destory();
		}

		public void showResult(){
			robot.startAnimation(robotAnimation);
		}

		private CharSequence getSpannable(int size) {
			SpannableStringBuilder spannable = new SpannableStringBuilder();
			String sizeStr = String.valueOf(size);
			String unit = "个";
			spannable.append(sizeStr);
			spannable.append(unit);
			spannable.append("\n应用");
			Resources resources = viewGroup.getResources();
			spannable.setSpan(new ForegroundColorSpan(resources.getColor(R.color.color_game_8)), 0, sizeStr.length() + unit.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			spannable.setSpan(new AbsoluteSizeSpan(13,true), 0, sizeStr.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			return spannable;
		}
	}
}
