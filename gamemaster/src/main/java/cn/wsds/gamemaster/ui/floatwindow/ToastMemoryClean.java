package cn.wsds.gamemaster.ui.floatwindow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import cn.wsds.gamemaster.R;

import com.subao.utils.MetricsUtils;

public class ToastMemoryClean extends FloatWindow {
	
	private final Animation toastIn = AnimationUtils.loadAnimation(getContext(), R.anim.toast_in);
	private final Animation toastOut = AnimationUtils.loadAnimation(getContext(), R.anim.toast_out);
	@SuppressLint("InflateParams") public static ToastMemoryClean show(Context context,CharSequence text){
		ToastMemoryClean inst = new ToastMemoryClean(context,text);
		View view = LayoutInflater.from(context).inflate(R.layout.layout_toast_memory_clean, null);
		inst.addView(FloatWindow.Type.TOAST, view, 0, 0);
		return inst;
	}

	private CharSequence text;
	private TextView contentTextView;

	private ToastMemoryClean(Context context, CharSequence text) {
		super(context);
		this.text = text;
	}

	@Override
	protected void onViewAdded(View view) {
		contentTextView = (TextView) view.findViewById(R.id.clean_toast_content);
		contentTextView.setText(text);
		Point screenSize = MetricsUtils.getDevicesSizeByPixels(getContext());
		int xCenter = screenSize.x / 2;
		int yCenter = screenSize.y - getHeight() / 2;
		setCenterPosition(xCenter, yCenter);
		toastIn.setAnimationListener(getToastInListener());
		toastOut.setAnimationListener(getToastOutListener());
		toastOut.setStartOffset(1000);
		contentTextView.startAnimation(toastIn);
	}

	private AnimationListener getToastInListener() {
		return new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				contentTextView.startAnimation(toastOut);
			}
		};
	}

	private AnimationListener getToastOutListener() {
		return new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				contentTextView.clearAnimation();
				destroy();
			}
		};
	}

	@Override
	protected boolean canDrag() {
		return false;
	}
	


}
