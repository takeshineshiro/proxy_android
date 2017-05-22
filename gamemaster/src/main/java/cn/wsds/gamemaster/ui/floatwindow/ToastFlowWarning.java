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
import cn.wsds.gamemaster.statistic.Statistic;

import com.subao.utils.MetricsUtils;

/**
 * 流量警告Toast
 */
@SuppressLint("InflateParams") public class ToastFlowWarning extends FloatWindow {

	private static ToastFlowWarning instance;

	private final View frame, content;
	private final View contentGameSelf, contentOther;
	private final TextView tvAppName;

	private abstract class AnimationListenerBase implements AnimationListener {
		@Override
		public void onAnimationStart(Animation animation) {

		}

		@Override
		public void onAnimationRepeat(Animation animation) {

		}
	}

	/**
	 * 整个Toast出现时的动画监听
	 */
	private class AnimationListener_FrameIn extends AnimationListenerBase {

		@Override
		public void onAnimationEnd(Animation animation) {
			content.setVisibility(View.VISIBLE);
			Animation ani = AnimationUtils.loadAnimation(getContext(), R.anim.toast_flow_warning_text_in); // .makeInChildBottomAnimation(getContext());
			content.startAnimation(ani);
			ani.setAnimationListener(new AnimationListener_ContentIn());
		}
	}

	/**
	 * 内容部分出现的动画监听
	 */
	private class AnimationListener_ContentIn extends AnimationListenerBase {
		@Override
		public void onAnimationEnd(Animation animation) {
			frame.postDelayed(new Runnable() {
				@Override
				public void run() {
					Animation ani = AnimationUtils.loadAnimation(getContext(), R.anim.toast_flow_warning_text_out);
					ani.setAnimationListener(new AnimationListener_ContentOut());
					content.startAnimation(ani);					
				}
			}, 5000);
		}
	}

	/**
	 * 内容部分消失的动画监听
	 */
	private class AnimationListener_ContentOut extends AnimationListenerBase {
		@Override
		public void onAnimationEnd(Animation animation) {
			content.setVisibility(View.INVISIBLE);
			Animation ani = AnimationUtils.loadAnimation(getContext(), R.anim.toast_flow_warning_hide);
			ani.setAnimationListener(new AnimationListener_FrameOut());
			frame.startAnimation(ani);
		}
	}

	private class AnimationListener_FrameOut extends AnimationListenerBase {
		@Override
		public void onAnimationEnd(Animation animation) {
			frame.post(new Runnable() {
				@Override
				public void run() {
					destroy();
				}
			});
		}
	}

	/**
	 * 显示一个实例
	 * 
	 * @param context
	 * @param appName
	 *            消耗最大的应用名称
	 * @param isCurrentGame
	 *            消耗最大的是不是游戏本身？
	 */
	public static void show(Context context, String appName, boolean isCurrentGame) {
		destroyInstance();
		instance = new ToastFlowWarning(context, appName, isCurrentGame);
	}

	/**
	 * 销毁实例
	 */
	public static void destroyInstance() {
		ToastFlowWarning inst = instance;
		if (inst != null) {
			inst.destroy();
			instance = null;
		}
	}

	private ToastFlowWarning(Context context, String appName, boolean isCurrentGame) {
		super(context);
		View view = LayoutInflater.from(context).inflate(R.layout.toast_flow_warning, null);
		frame = view.findViewById(R.id.toast_flow_warning_frame);
		content = view.findViewById(R.id.toast_flow_warning_content);
		content.setVisibility(View.INVISIBLE);
		contentGameSelf = view.findViewById(R.id.toast_flow_warning_content_gameself);
		contentOther = view.findViewById(R.id.toast_flow_warning_content_other);
		tvAppName = (TextView) view.findViewById(R.id.toast_flow_warning_app_name);
		tvAppName.setText(appName);
		//
		if (isCurrentGame) {
			contentOther.setVisibility(View.INVISIBLE);
			contentGameSelf.setVisibility(View.VISIBLE);
		} else {
			contentOther.setVisibility(View.VISIBLE);
			contentGameSelf.setVisibility(View.GONE);
		}
		addView(Type.TOAST, view, 0, 0);
//		StatisticDefault.addEvent(context, StatisticDefault.Event.TOAST_BACKGROUND_TRAFFIC, appName);
		Statistic.addEvent(context, Statistic.Event.FLOATING_WINDOW_TOAST_ABNORMAL_TRAFFIC, isCurrentGame ? "当前游戏流量" : "后台程序流量");
	}

	@Override
	protected void onViewAdded(View view) {
		Context context = this.getContext();
		Point size = MetricsUtils.getDevicesSizeByPixels(context);
		int x = (size.x - view.getMeasuredWidth()) >> 1;
		int y = (size.y << 1) / 3;
		this.setPosition(x, y);
		//
		Animation ani = AnimationUtils.loadAnimation(context, R.anim.toast_flow_warning_show);
		ani.setAnimationListener(new AnimationListener_FrameIn());
		frame.startAnimation(ani);
	}

	@Override
	protected void destroy() {
		super.destroy();
		if (this == instance) {
			instance = null;
		}
	}

	@Override
	protected boolean canDrag() {
		return false;
	}

}
