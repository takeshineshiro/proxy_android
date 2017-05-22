package cn.wsds.gamemaster.ui.accel.progress;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import cn.wsds.gamemaster.R;

import com.subao.utils.Misc;

@SuppressLint("RtlHardcoded")
public class AccelProgressGroup extends RelativeLayout {

	private final TextView label;
	private final AccelProgressIcon icon;
	private final AccelProgressBox box1, box2;
	private final AccelProgressLine line;

	private AccelProgressIcon.IconAniListener aniListener;

	private boolean abortFlag;

	public AccelProgressGroup(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AccelProgressGroup);
		label = createLabel(context, ta.getString(R.styleable.AccelProgressGroup_group_label));
		icon = createIcon(context, ta.getDrawable(R.styleable.AccelProgressGroup_group_icon));
		box1 = createBox(context, ta.getString(R.styleable.AccelProgressGroup_group_message));
		box2 = createBox(context, ta.getString(R.styleable.AccelProgressGroup_group_second_message));
		line = createLine(context, ta.getDimensionPixelSize(R.styleable.AccelProgressGroup_group_line_size, -1));
		adjustLabel();
		ta.recycle();
		//
		if (!isInEditMode()) {
			this.hide();
		}
	}

	private TextView createLabel(Context context, String text) {
		Resources res = context.getResources();
		TextView tv = new TextView(context);
		tv.setId(Misc.generateViewId());
		tv.setText(text);
		tv.setSingleLine(true);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.text_size_10));
		tv.setTextColor(res.getColor(R.color.color_game_7));
		tv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		LayoutParams lp = new LayoutParams(res.getDimensionPixelSize(R.dimen.space_size_48), LayoutParams.WRAP_CONTENT);
		tv.setLayoutParams(lp);
		addView(tv);
		return tv;
	}

	private AccelProgressIcon createIcon(Context context, Drawable d) {
		AccelProgressIcon icon = new AccelProgressIcon(context, null);
		icon.setId(Misc.generateViewId());
		icon.setIcon(d);
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		int margin = context.getResources().getDimensionPixelSize(R.dimen.space_size_6);
		lp.setMargins(margin, 0, margin, 0);
		lp.addRule(RelativeLayout.RIGHT_OF, label.getId());
		icon.setLayoutParams(lp);
		addView(icon);
		return icon;
	}

	private AccelProgressBox createBox(Context context, String message) {
		if (TextUtils.isEmpty(message)) {
			return null;
		}
		AccelProgressBox box = new AccelProgressBox(context, null);
		box.setId(Misc.generateViewId());
		box.setMessage(message);
		//
		Resources res = context.getResources();
		LayoutParams lp = new LayoutParams(res.getDimensionPixelSize(R.dimen.accel_progress_box_width),
			res.getDimensionPixelSize(R.dimen.accel_progress_box_height));
		if (box1 != null) {
			lp.addRule(RelativeLayout.ALIGN_LEFT, box1.getId());
			lp.addRule(RelativeLayout.BELOW, box1.getId());
			lp.setMargins(0, res.getDimensionPixelSize(R.dimen.space_size_8), 0, 0);
		} else {
			lp.addRule(RelativeLayout.RIGHT_OF, icon.getId());
		}
		box.setLayoutParams(lp);
		addView(box);
		return box;
	}

	private AccelProgressLine createLine(Context context, int dimensionPixelSize) {
		if (dimensionPixelSize <= 0) {
			return null;
		}
		AccelProgressLine line = new AccelProgressLine(context, null);
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, dimensionPixelSize);
		lp.addRule(RelativeLayout.ALIGN_LEFT, icon.getId());
		lp.addRule(RelativeLayout.ALIGN_RIGHT, icon.getId());
		lp.addRule(RelativeLayout.BELOW, icon.getId());
		line.setLayoutParams(lp);
		addView(line);
		return line;
	}

	private void adjustLabel() {
		LayoutParams lp = new LayoutParams(label.getLayoutParams());
		lp.addRule(RelativeLayout.ALIGN_TOP, icon.getId());
		lp.addRule(RelativeLayout.ALIGN_BOTTOM, icon.getId());
		label.setLayoutParams(lp);
	}

	private void hide() {
		label.setVisibility(INVISIBLE);
		icon.setVisibility(INVISIBLE);
		if (box1 != null) {
			box1.setVisibility(INVISIBLE);
		}
		if (box2 != null) {
			box2.setVisibility(INVISIBLE);
		}
		if (line != null) {
			line.setVisibility(INVISIBLE);
		}
		this.setVisibility(INVISIBLE);
	}

	private void fadeIn(View view, AnimatorListener listener) {
		if (abortFlag) { return; }
		view.setVisibility(VISIBLE);
		ObjectAnimator oa = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
		oa.setDuration(300).setInterpolator(null);
		if (null != listener) {
			oa.addListener(listener);
		}
		oa.start();
	}

	/**
	 * 开始执行动动画
	 */
	public void startAni(AccelProgressIcon.IconAniListener listener) {
		if (getVisibility() == VISIBLE) {
			return;
		}
		setVisibility(VISIBLE);
		this.aniListener = listener;
		fadeIn(icon, new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				if (!abortFlag) {
					showLabel();
				}
			}
		});
	}

	private void showLabel() {
		fadeIn(label, new AnimatorListenerAdapter() {
			public void onAnimationEnd(Animator animation) {
				startIconAni();
			};
		});
	}

	private void startIconAni() {
		icon.startAni(new IconAniListener());
	}

	private void showBox1() {
		if (box1 != null) {
			box1.startAni(new AniListener() {
				@Override
				public void onAniEnd(Object sender) {
					showBox2();
				}
				@Override
				public void onAniAbort(Object sender) {}
			});
		} else {
			showBox2();
		}
	}

	private void showBox2() {
		if (box2 != null) {
			box2.startAni(new AniListener() {
				@Override
				public void onAniEnd(Object sender) {
					showLine();
				}
				@Override
				public void onAniAbort(Object sender) {}
			});
		} else {
			showLine();
		}
	}

	private void showLine() {
		if (line != null) {
			line.startAni(new AniListener() {
				@Override
				public void onAniEnd(Object sender) {
					notifyAniListener();
				}
				@Override
				public void onAniAbort(Object sender) {}
			});
		} else {
			notifyAniListener();
		}
	}

	private void notifyAniListener() {
		if (aniListener != null) {
			aniListener.onAniEnd(AccelProgressGroup.this);
			aniListener = null;
		}
	}

	private class IconAniListener implements AccelProgressIcon.IconAniListener {

		@Override
		public void onAniZoom(AccelProgressIcon sender, float scale) {
			if (aniListener != null) {
				aniListener.onAniZoom(sender, scale);
			}
		}

		@Override
		public void onAniEnd(Object sender) {
			showBox1();
		}
		
		@Override
		public void onAniAbort(Object sender) {
			if (aniListener != null) {
				aniListener.onAniAbort(sender);
			}
		}

	}

	/**
	 * 向上收缩线条的下端
	 * 
	 * @param percent
	 *            百分比
	 */
	public void deflateBottom(float percent) {
		if (line != null) {
			line.deflateBottom(percent);
		}
	}

	/**
	 * See {@link AccelProgressBox#setType}
	 */
	public void setBoxType(AccelProgressBox.Type type) {
		AccelProgressBox box;
		switch (type) {
		case AccelEffectPercent:
			box = box2;
			break;
		default:
			box = box1;
			break;
		}
		if (box != null) {
			box.setType(type);
		}
	}

	@Override
	public void setAlpha(float alpha) {
		super.setAlpha(alpha);
		AccelProgressCommon.setChildrenAlpha(this, alpha);
	}

	public void abort() {
		abortFlag = true;
	}
}
