package cn.wsds.gamemaster.ui.accel.progress;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ui.UIUtils;

import com.subao.net.NetManager;

public class AccelProgressBox extends RelativeLayout {

	/** Box 类型 */
	public enum Type {
		/** 常规的 */
		Normal,

		/** 用于显示“本地网络检查”的 */
		LocalNetChecker,

		/** 用于显示“加速效果百分比”的 */
		AccelEffectPercent,

		/** 用于显示“加速成功”的 */
		ACCEL_OVER,
	}

	public enum MarkType {
		SUCCEED, WARNING, ERROR
	}

	private final View progressBar;
	private final TextView textMessage;

	private int imgMarkResId = -1;
	private final ImageView imgMark;

	private final ViewStub textSecondLine;

	/** 本控件的宽度 */
	private int width;

	/** 本Box是哪种类型的 */
	private Type type = Type.Normal;

	private AniListener aniListener;

	private boolean abortFlag;

	private abstract class MyAnimatorListener extends AnimatorListenerAdapter {
		@Override
		public void onAnimationEnd(Animator animation) {
			if (!abortFlag) {
				doOnAnimationEnd(animation);
			}
		}

		protected abstract void doOnAnimationEnd(Animator animation);
	}

	public AccelProgressBox(Context context, AttributeSet attrs) {
		super(context, attrs);
		View root = LayoutInflater.from(context).inflate(R.layout.accel_progress_box, this, true);
		progressBar = root.findViewById(R.id.accel_progress_box_progress_bar);
		textMessage = (TextView) root.findViewById(R.id.accel_progress_box_text_1);
		textSecondLine = (ViewStub) root.findViewById(R.id.accel_progress_box_text_2);
		imgMark = (ImageView) root.findViewById(R.id.accel_progress_box_icon);
		//		
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AccelProgressBox);
		try {
			UIUtils.setViewText(textMessage, ta.getString(R.styleable.AccelProgressBox_message));
			switch (ta.getInt(R.styleable.AccelProgressBox_markType, 0)) {
			case 1:
				setImageMark(MarkType.WARNING);
				break;
			case 2:
				setImageMark(MarkType.ERROR);
				break;
			default:
				setImageMark(MarkType.SUCCEED);
				break;
			}
		} finally {
			ta.recycle();
		}
		//
		if (!isInEditMode()) {
			hide();
		}
	}

	/**
	 * 设置本Box的{@link Type}
	 */
	public void setType(Type type) {
		this.type = type;
	}

	private void setImageMark(MarkType mt) {
		int resId;
		switch (mt) {
		case WARNING:
			resId = R.drawable.open_to_accelerate_the_progress_of_warning;
			break;
		case ERROR:
			resId = R.drawable.open_to_accelerate_the_progress_of_wrong;
			break;
		default:
			resId = R.drawable.open_to_accelerate_the_progress_of_right;
			break;
		}
		if (resId != imgMarkResId) {
			imgMarkResId = resId;
			imgMark.setImageResource(resId);
		}
	}

	@Override
	public void setAlpha(float alpha) {
		super.setAlpha(alpha);
		AccelProgressCommon.setChildrenAlpha(this, alpha);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		this.width = getMeasuredWidth();
	}

	private void hide() {
		UIUtils.setViewVisibility(this.progressBar, GONE);
		UIUtils.setViewVisibility(this.textMessage, GONE);
		UIUtils.setViewVisibility(this.imgMark, GONE);
		UIUtils.setViewVisibility(this, INVISIBLE);
	}

	/**
	 * 设置文本
	 * 
	 * @param cs
	 */
	public void setMessage(CharSequence cs) {
		this.textMessage.setText(cs);
	}

	public void startAni(AniListener listener) {
		if (this.getVisibility() == VISIBLE) {
			return;
		}
		this.aniListener = listener;
		showBackground();
	}

	/**
	 * 中断动画
	 */
	public void abortAni() {
		if (!abortFlag) {
			abortFlag = true;
			if (aniListener != null) {
				aniListener.onAniAbort(this);
			}
		}
	}

	private static void doAnimation(View view, String property, long duration, float v1, float v2,
		AnimatorListener listener) {
		view.setVisibility(VISIBLE);
		ObjectAnimator ani = ObjectAnimator.ofFloat(view, property, v1, v2);
		ani.setInterpolator(null);
		ani.setDuration(duration);
		if (listener != null) {
			ani.addListener(listener);
		}
		ani.start();
	}

	private static void doAnimationAlpha(View view, long duration, float v1, float v2, AnimatorListenerAdapter listener) {
		doAnimation(view, "alpha", duration, v1, v2, listener);
	}

	private void showBackground() {
		doAnimationAlpha(this, 100, 0f, 1f, new MyAnimatorListener() {
			@Override
			protected void doOnAnimationEnd(Animator animation) {
				showMessageText();
			}
		});
	}

	private void showMessageText() {
		doAnimationAlpha(textMessage, 100, 0f, 1f, new MyAnimatorListener() {
			@Override
			protected void doOnAnimationEnd(Animator animation) {
				showProgressBar1();
			}
		});
	}

	private void showProgressBar1() {
		progressBar.setPivotX(0f);
		doAnimation(progressBar, "scaleX", 300, 0f, 1f, new MyAnimatorListener() {
			@Override
			protected void doOnAnimationEnd(Animator animation) {
				showProgressBar2();
			}
		});
	}

	private void showProgressBar2() {
		progressBar.setPivotX(width);
		doAnimationAlpha(progressBar, 100, 1f, 0f, new MyAnimatorListener() {
			@Override
			protected void doOnAnimationEnd(Animator animation) {
				aniEnd();
			}
		});
	}

	private void aniEnd() {
		UIUtils.setViewVisibility(progressBar, GONE);
		switch (this.type) {
		case LocalNetChecker:
			doLocalNetCheck();
			break;
		case AccelEffectPercent:
			calcAccelEffectPercent();
			break;
		default:
			break;
		}
		//
		UIUtils.setViewVisibility(imgMark, VISIBLE);
		if (this.type == Type.ACCEL_OVER) {
			if (NetManager.getInstance().isDisconnected()) {
				textSecondLine.inflate();
				setImageMark(MarkType.WARNING);
			}
		}
		//
		if (aniListener != null) {
			aniListener.onAniEnd(AccelProgressBox.this);
			aniListener = null;
		}
	}

	private void doLocalNetCheck() {
		if (NetManager.getInstance().isDisconnected()) {
			setMessage("我的网络已断开");
			setImageMark(MarkType.ERROR);
		}
	}

	private void calcAccelEffectPercent() {
		int percent = 51 + (int) (SystemClock.elapsedRealtime() % 25);
		setMessage(String.format("预计降低延迟%d%%", percent));
	}

}
