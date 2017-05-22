package cn.wsds.gamemaster.ui.accel.animation;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.accel.progress.AccelProgress;
import cn.wsds.gamemaster.ui.accel.progress.AniListener;
import cn.wsds.gamemaster.ui.mainfragment.PieceAccelOff;

import com.subao.common.net.NetTypeDetector;
import com.subao.common.utils.CalendarUtils;
import com.subao.net.NetManager;

/**
 * 主界面开启动画
 */
public class AnimationAccelOn {

	private boolean animationRunning = false;
	private AccelAnimationListener accelAnimationListener;

	private final Controller controller;

	// 三段动画
	private final AccelProgressAnimation accelProgressAnimation;
	private final FadeInAnimation fadeInAnimation;

	public interface Controller {
		public View getPieceAccelOn();

		public PieceAccelOff getPieceAccelOff();

		public void switchPiece(boolean accelOn);

		public boolean isCurrentPieceAccelOn();
	}

	public AnimationAccelOn(Controller controller) {
		View accelOnContent = controller.getPieceAccelOn();
		accelProgressAnimation = new AccelProgressAnimation(accelOnContent);
		fadeInAnimation = new FadeInAnimation(accelOnContent);
		this.controller = controller;
	}

	/**
	 * 开启动画
	 * 
	 * @param cleanSize
	 *            清理后台进程数量
	 */
	public void start(int cleanSize) {
		this.animationRunning = true;
		if (accelAnimationListener != null) {
			accelAnimationListener.onAnimationStart();
		}
		PieceAccelOff pieceAccelOff = controller.getPieceAccelOff();
		if (pieceAccelOff != null) {
			pieceAccelOff.startLeaveAnimation(new PieceAccelOff.LeaveAnimationListener() {

				@Override
				public void onLeaveAnimationEnd(PieceAccelOff who) {
					if (!AccelOpenManager.isStarted()) {
						return;
					}
					controller.switchPiece(true);
					if (doesAccelProgressAnimationNeed()) {
						accelProgressAnimation.start(AppMain.getContext());
					} else {
						fadeInAnimation.start();
					}
				}
			});
		}
		fadeInAnimation.setCleanSize(cleanSize);
	}

	private void onAccelOnAnimationFinished() {
		this.animationRunning = false;
		if (accelAnimationListener != null) {
			accelAnimationListener.onAnimationEnd();
		}
	}

	/**
	 * 判断是否需要展示加速流程动画
	 * 
	 * @return true表示需要做，false表示不需要
	 */
	private boolean doesAccelProgressAnimationNeed() {
		// 2016.10.28 需求更改为一生只做一次动画了
//		int today = CalendarUtils.todayLocal();
//		return (today != ConfigManager.getInstance().getDayOfAccelProgressAni());
		return ConfigManager.getInstance().getDayOfAccelProgressAni() == 0;
	}

	/**
	 * 动画是否在运行
	 */
	public boolean isRunning() {
		return animationRunning;
	}

	/**
	 * 设置加速动画监听
	 * 
	 * @param listener
	 */
	public void setAccelAnimationListener(AccelAnimationListener listener) {
		this.accelAnimationListener = listener;
	}

	/**
	 * 加速流程动画（第二段动画）
	 * 
	 * @param context
	 */
	private final class AccelProgressAnimation {
		private AccelProgressAniListener accelProgressAniListener;

		private final View viewContent;

		public AccelProgressAnimation(View viewContent) {
			this.viewContent = viewContent;
		}

		private void start(Context context) {
			// 如果已经有Listener了，说明动画已经开始了
			if (accelProgressAniListener != null || !AccelOpenManager.isStarted()) {
				return;
			}

			// 开始做动画		
			AccelProgress accelProgress = new AccelProgress(context);
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			accelProgress.setLayoutParams(lp);
			ViewParent parent = viewContent.getParent().getParent();
			if (parent == null) {
				return;
			}
			viewContent.setVisibility(View.GONE);
			ViewGroup container = (ViewGroup) parent;
			container.addView(accelProgress);

			accelProgressAniListener = new AccelProgressAniListener(container, accelProgress);
			accelProgress.startAni(accelProgressAniListener);

			ConfigManager.getInstance().setDayOfAccelProgressAni(CalendarUtils.todayLocal());
		}

		private class AccelProgressAniListener implements AniListener {

			private AccelProgress accelProgress;
			private final ViewGroup container;

			public AccelProgressAniListener(ViewGroup container, AccelProgress accelProgress) {
				this.container = container;
				this.accelProgress = accelProgress;
			}

			@Override
			public void onAniAbort(Object sender) {}

			@Override
			public void onAniEnd(Object sender) {
				if (accelProgress == null) {
					return;
				}
				Animation ani = AnimationUtils.loadAnimation(AppMain.getContext(), R.anim.accel_progress_out);
				ani.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {}

					@Override
					public void onAnimationRepeat(Animation animation) {}

					@Override
					public void onAnimationEnd(Animation animation) {
						abort();
						fadeInAnimation.start();
					}

				});
				ani.setStartOffset(1000);
				accelProgress.setAnimation(ani);
				ani.start();
			}

			public void abort() {
				if (accelProgress != null) {
					accelProgress.abort();
					container.removeView(accelProgress);
					viewContent.setVisibility(View.VISIBLE);
					accelProgress = null;
				}
				if (accelProgressAniListener == this) {
					accelProgressAniListener = null;
				}
			}

		}

		public void abort() {
			if (accelProgressAniListener != null) {
				accelProgressAniListener.abort();
			}
		}
	}

	/**
	 * 最后一段动画
	 */
	private final class FadeInAnimation {
		private final ViewFlipper contentFlipper;
		private final View listGroup;
		private final AnimationSet flipperFadeInAnimation = new AnimationSet(true);
		private final Animation listTranslateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
			Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_PARENT, 0f);
		private final Runnable action = new Runnable() {

			@Override
			public void run() {
				contentFlipper.showNext();
			}
		};
		private int cleanSize;
		private final TextView netResult;
		private final TextView memoryResult;

		public FadeInAnimation(View viewContent) {
			contentFlipper = (ViewFlipper) viewContent.findViewById(R.id.accelOnContentFlipper);
			netResult = (TextView) contentFlipper.findViewById(R.id.net_result);
			memoryResult = (TextView) contentFlipper.findViewById(R.id.memory_result);
			listGroup = viewContent.findViewById(R.id.fragment_list);
			initAnimation();
		}

		public void setCleanSize(int cleanSize) {
			this.cleanSize = cleanSize;
		}

		private void initFlipperAnimation() {
			Animation fadeInAnimation = new AlphaAnimation(0f, 1f);
			fadeInAnimation.setDuration(250);
			TranslateAnimation translateInAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_PARENT, 0f,
				Animation.RELATIVE_TO_PARENT, 0.3f, Animation.RELATIVE_TO_PARENT, 0f);
			translateInAnimation.setDuration(500);
			AnimationSet inAnimation = new AnimationSet(true);
			inAnimation.addAnimation(fadeInAnimation);
			inAnimation.addAnimation(translateInAnimation);
			contentFlipper.setInAnimation(inAnimation);

			Animation fadeOutAnimation = new AlphaAnimation(1f, 0f);
			fadeOutAnimation.setDuration(500);
			TranslateAnimation translateOutAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_PARENT, 0f,
				Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_PARENT, -0.3f);
			translateOutAnimation.setDuration(500);
			AnimationSet outAnimation = new AnimationSet(true);
			outAnimation.addAnimation(fadeOutAnimation);
			outAnimation.addAnimation(translateOutAnimation);
			contentFlipper.setOutAnimation(outAnimation);
			outAnimation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {}

				@Override
				public void onAnimationRepeat(Animation animation) {}

				@Override
				public void onAnimationEnd(Animation animation) {
					if (contentFlipper.getDisplayedChild() < contentFlipper.getChildCount() - 1) {
						contentFlipper.postDelayed(action, 1000);
					} else {
						contentFlipper.postDelayed(new Runnable() {

							@Override
							public void run() {
								if (isRunning()) {
									abort();
									onAccelOnAnimationFinished();
								}
							}
						}, 1000);
					}
				}
			});
		}

		private void initAnimation() {
			Animation fadeInAnimation = new AlphaAnimation(0f, 1f);
			fadeInAnimation.setDuration(250);
			fadeInAnimation.setFillAfter(true);
			TranslateAnimation headTranslateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_PARENT, 0f,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_PARENT, 0f);
			headTranslateAnimation.setFillAfter(true);
			headTranslateAnimation.setDuration(500);
			flipperFadeInAnimation.addAnimation(fadeInAnimation);
			flipperFadeInAnimation.addAnimation(headTranslateAnimation);

			flipperFadeInAnimation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {}

				@Override
				public void onAnimationRepeat(Animation animation) {}

				@Override
				public void onAnimationEnd(Animation animation) {
					initFlipperAnimation();
					contentFlipper.postDelayed(action, 1000);
				}
			});

			listTranslateAnimation.setDuration(500);
			listTranslateAnimation.setFillAfter(true);
		}

		public void start() {
			if (AccelOpenManager.isStarted()) {
				controller.switchPiece(true);
				setNetResult();
				setMemoryResult();
				contentFlipper.setDisplayedChild(0);
				contentFlipper.startAnimation(flipperFadeInAnimation);
				listGroup.startAnimation(listTranslateAnimation);
			}
		}

		private void setMemoryResult() {
			int color = memoryResult.getResources().getColor(R.color.color_game_11);
			int percent = UIUtils.getAccelPercent(cleanSize + 3);
			memoryResult.setText(getTextSpannable(percent, "内存提速", color));
		}

		private void setNetResult() {
			NetTypeDetector.NetType currentNetworkType = NetManager.getInstance().getCurrentNetworkType();
			if (NetTypeDetector.NetType.MOBILE_2G != currentNetworkType &&
				NetTypeDetector.NetType.DISCONNECT != currentNetworkType) {
				int index = 0;
				if (netResult.getId() != contentFlipper.getChildAt(index).getId()) {
					contentFlipper.addView(netResult, index);
				}
				int percent = Long.valueOf(System.currentTimeMillis() % 51).intValue() + 20;
				int color = netResult.getResources().getColor(R.color.color_game_10);
				netResult.setText(getTextSpannable(percent, "网络提速", color));
			} else {
				contentFlipper.removeView(netResult);
			}				
		}

		private CharSequence getTextSpannable(int percent, String prefix, int color) {
			String perfixSymbol = "+";
			String unit = "%";
			SpannableStringBuilder specialBuilder = new SpannableStringBuilder();
			specialBuilder.append(perfixSymbol);
			String percentStr = String.valueOf(percent);
			specialBuilder.append(percentStr);
			specialBuilder.append(unit);

			int symbolSize = 12;
			specialBuilder.setSpan(new ForegroundColorSpan(color), 0, perfixSymbol.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			specialBuilder.setSpan(new AbsoluteSizeSpan(symbolSize, true), 0, perfixSymbol.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

			int unitStart = specialBuilder.length() - unit.length();
			specialBuilder.setSpan(new ForegroundColorSpan(color), unitStart, specialBuilder.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			specialBuilder.setSpan(new AbsoluteSizeSpan(symbolSize, true), unitStart, specialBuilder.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

			int percentSize = 24;
			int percentEnd = perfixSymbol.length() + percentStr.length();
			specialBuilder.setSpan(new ForegroundColorSpan(color), perfixSymbol.length(), percentEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			specialBuilder.setSpan(new AbsoluteSizeSpan(percentSize, true), perfixSymbol.length(), percentEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

			SpannableStringBuilder builder = new SpannableStringBuilder();
			String space = "    ";
			builder.append(prefix);
			builder.append(space);
			builder.append(specialBuilder);
			return builder;
		}

		public void abort() {
			contentFlipper.clearAnimation();
			contentFlipper.removeCallbacks(action);
			Animation inAnimation = contentFlipper.getInAnimation();
			if (inAnimation != null) {
				inAnimation.cancel();
			}
			Animation outAnimation = contentFlipper.getOutAnimation();
			if (outAnimation != null) {
				outAnimation.cancel();
			}
			contentFlipper.setInAnimation(null);
			contentFlipper.setOutAnimation(null);
			int end = contentFlipper.getChildCount() - 1;
			if (contentFlipper.getDisplayedChild() < end) {
				contentFlipper.setDisplayedChild(end);
			}
			listGroup.clearAnimation();
			flipperFadeInAnimation.cancel();
			listTranslateAnimation.cancel();
		}

	}

	public void abort() {
		PieceAccelOff piece = controller.getPieceAccelOff();
		if (piece != null) {
			piece.abortLeaveAnimation();
		}
		accelProgressAnimation.abort();
		fadeInAnimation.abort();
		this.animationRunning = false;
	}
	
	
}
