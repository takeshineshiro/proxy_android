package cn.wsds.gamemaster.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.wsds.gamemaster.R;

public class ActivityInstructions extends ActivityBase {

	private final List<Integer> layoutList = new ArrayList<Integer>(5);
	private final List<String> titleList = new ArrayList<String>(5);

	private TextView textTitle;
	private ViewPager viewPager;
	private Points points;

	public ActivityInstructions() {
		addPage(R.layout.instructions_0, "分段延迟");
		addPage(R.layout.instructions_1, "网络延迟");
		//addPage(R.layout.instructions_2, "断线重连");
//		if (UIUtils.isCallRemindSupportCurrentRom()) {
//			addPage(R.layout.instructions_3, "游戏中来电提醒");
//		}
		addPage(R.layout.instructions_4, "其他功能");
	}

	private void addPage(int layoutId, String title) {
		layoutList.add(layoutId);
		titleList.add(title);
	}

	private int getPageCount() {
		return layoutList.size();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_instructions);
		setDisplayHomeArrow(R.string.title_activity_instructions);
		//
		textTitle = (TextView) findViewById(R.id.text_title);
		textTitle.setText(titleList.get(0));
		//
		points = new Points(this, getPageCount());
		points.activate(0);
		//
		viewPager = (ViewPager) findViewById(R.id.viewpager);
		viewPager.setAdapter(new MyPagerAdapter());
		viewPager.addOnPageChangeListener(new MyPageChangeListener());
//		StatisticDefault.addEvent(this, StatisticDefault.Event.MESSAGE_READ_APPDEMO);
	}

	@Override
	public void onBackPressed() {
		int position = points.getActivated();
		if (position < 1) {
			super.onBackPressed();
		} else {
			viewPager.setCurrentItem(position - 1);
		}
	}

	private static boolean isFirstPage(int position) {
		return position == 0;
	}

	/**
	 * UI：代表当前激活页序号的圆点
	 */
	private static class Points {

		private final ImageView[] imgPointList;
		private int activated = -1;

		public Points(Activity owner, int pageCount) {
			this.imgPointList = new ImageView[pageCount];
			LinearLayout pointGroup = (LinearLayout) owner.findViewById(R.id.point_group);
			//
			int marginLeft = owner.getResources().getDimensionPixelSize(R.dimen.space_size_8);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lp.setMargins(marginLeft, 0, 0, 0);
			for (int i = 0; i < pageCount; ++i) {
				ImageView img = new ImageView(owner);
				img.setLayoutParams(lp);
				activate(img, false);
				pointGroup.addView(img);
				imgPointList[i] = img;
			}
		}

		private static void activate(ImageView img, boolean active) {
			img.setImageResource(active ? R.drawable.introdution_suspension_point_sel : R.drawable.introdution_suspension_point_nor);
		}

		/**
		 * 激活指定序号的圆点
		 */
		public void activate(int position) {
			if (position != activated) {
				activate(imgPointList[position], true);
				if (activated >= 0) {
					activate(imgPointList[activated], false);
				}
				activated = position;
			}
		}

		/**
		 * 取当前激活圆点的序号
		 * 
		 * @return 序号。负数表示当前无激活圆点
		 */
		public int getActivated() {
			return this.activated;
		}
	}

	/**
	 * 页面滑动改变的监听器
	 */
	private class MyPageChangeListener implements ViewPager.OnPageChangeListener {

		@Override
		public void onPageScrollStateChanged(int arg0) {

		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {

		}

		@Override
		public void onPageSelected(int position) {
			// 页面改变，激活相应的圆点
			points.activate(position);
			textTitle.setText(titleList.get(position));
		}

	}

	/**
	 * ViewPager的适配器
	 */
	private class MyPagerAdapter extends PagerAdapter {

		private boolean animationPresented;

		@Override
		public int getCount() {
			return getPageCount();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			Context context = ActivityInstructions.this;
			View view = LayoutInflater.from(context).inflate(layoutList.get(position), null);
			container.addView(view);
			if (isFirstPage(position)) {
				firstPageInit(view);
				firstPageAnimation(view);
			}
			return view;
		}

		private void firstPageInit(View view) {
			TextView tv = (TextView) view.findViewById(R.id.text_instruction_0);
			tv.setText(null);
			String[] strings = getResources().getStringArray(R.array.instructions_0);
			int color1 = getResources().getColor(R.color.color_game_46);
			int color2 = getResources().getColor(R.color.color_game_7);
			int color = color1;
			for (String s : strings) {

				SpannableString ss = new SpannableString(s);
				ForegroundColorSpan span = new ForegroundColorSpan(color);
				ss.setSpan(span, 0, ss.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				tv.append(ss);
				if (color == color1) {
					color = color2;
				} else {
					color = color1;
				}
			}

		}

		private void firstPageAnimation(View view) {
			if (!animationPresented) {
				animationPresented = true;
				Animator ani = loadAnimator(view.getContext());
				View hintTop = view.findViewById(R.id.img_hint_top);
				View hintBottom = view.findViewById(R.id.img_hint_bottom);
				ani.addListener(new AniListener(hintBottom));
				ani.setTarget(hintTop);
				ani.start();
			}
		}

		private Animator loadAnimator(Context context) {
			return AnimatorInflater.loadAnimator(context, R.animator.hint_in);
		}

		private class AniListener implements AnimatorListener {

			private final View hint2;

			public AniListener(View hint2) {
				this.hint2 = hint2;
				hint2.setAlpha(0);
			}

			@Override
			public void onAnimationStart(Animator animation) {}

			@Override
			public void onAnimationEnd(Animator animation) {
				Animator ani = loadAnimator(hint2.getContext());
				ani.setTarget(hint2);
				ani.start();
			}

			@Override
			public void onAnimationCancel(Animator animation) {}

			@Override
			public void onAnimationRepeat(Animator animation) {}

		}

	}

}
