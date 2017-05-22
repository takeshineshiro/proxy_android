package cn.wsds.gamemaster.ui.floatwindow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import cn.wsds.gamemaster.R;

import com.subao.utils.MetricsUtils;

public class FloatWindowVpnImpowerHelp extends FloatWindow {
	
	private static FloatWindowVpnImpowerHelp instance;
	private View bar;
	
	private class EndAnimationListener implements AnimationListener {

		@Override
		public void onAnimationStart(Animation animation) {}

		@Override
		public void onAnimationEnd(Animation animation) {
			bar.post(new Runnable() {
				@Override
				public void run() {
					FloatWindowVpnImpowerHelp.this.destroy();					
				}
			});
		}

		@Override
		public void onAnimationRepeat(Animation animation) {}
		
	}

	public static void destroyInstance() {
		FloatWindowVpnImpowerHelp inst = instance;
		instance = null;
		if (inst != null) {
			Animation ani = AnimationUtils.loadAnimation(inst.getContext(), R.anim.vpn_impower_toast_fade_out);
			ani.setAnimationListener(inst.new EndAnimationListener());
			inst.bar.startAnimation(ani);
		}
	}

	public static void createInstance(Context context) {
		if (instance == null) {
			instance = new FloatWindowVpnImpowerHelp(context);
			instance.addView(Type.TOAST, createView(context), 0, 0);
		}
	}

	@SuppressLint("InflateParams") private static View createView(Context context) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View layout = inflater.inflate(R.layout.vpn_impower_help, null);
		TextView tv = (TextView) layout.findViewById(R.id.text_vpn_impower_help);
		tv.setText(loadText(context));
		return layout;
	}

	private static CharSequence loadText(Context context) {
		int[] colors = new int[] {
			context.getResources().getColor(R.color.color_game_2),
			context.getResources().getColor(R.color.color_game_11)
		};
		String[] texts = context.getResources().getStringArray(R.array.vpn_impower_help_texts);
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		int idxColor = 0;
		int start = 0;
		for (String s : texts) {
			ssb.append(s);
			int end = ssb.length();
			ssb.setSpan(new ForegroundColorSpan(colors[idxColor]), start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
			start = end;
			++idxColor;
			if (idxColor >= colors.length) {
				idxColor = 0;
			}
		}
		return ssb;
	}

	private FloatWindowVpnImpowerHelp(Context context) {
		super(context);
	}
	
	@Override
	protected void destroy() {
		super.destroy();
		if (this == instance) {
			instance = null;
		}
	}

	@Override
	protected void onViewAdded(View view) {
		bar = view.findViewById(R.id.bar_vpn_impower_help);
		Point sizeScreen = MetricsUtils.getDevicesSizeByPixels(getContext());
		int x = (sizeScreen.x - view.getMeasuredWidth()) >> 1;
		int y = (int)getContext().getResources().getDimension(R.dimen.margin_top_of_vpn_impower_toast);
		this.setPosition(x, y);
		Animation ani = AnimationUtils.loadAnimation(getContext(), R.anim.vpn_impower_toast_fade_in);
		bar.startAnimation(ani);
	}

	@Override
	protected boolean canDrag() {
		return false;
	}

}
