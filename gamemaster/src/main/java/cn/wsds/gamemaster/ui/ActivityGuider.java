package cn.wsds.gamemaster.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import cn.wsds.gamemaster.R;

import com.subao.utils.MetricsUtils;

/**
 * 第一次安装引导用户的Activity
 * 
 * @author lixq
 * 
 */
public class ActivityGuider extends Activity {

	/** 启动引导页的时候，一个Boolean值指明是否只显示第3页 */
	private static final String INTENT_EXTRANAME_GUIDE_NEW_PAGE = "cn.wsds.gamemaster.new_page";
	
	public static void show(Context context, boolean showExchageUpgrade) {
		Intent intent = new Intent(context, ActivityGuider.class);
		if (showExchageUpgrade) {
			intent.putExtra(INTENT_EXTRANAME_GUIDE_NEW_PAGE, showExchageUpgrade);
		}
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//
		this.setContentView(R.layout.activity_guide);
		ViewGroup root = (ViewGroup) findViewById(R.id.root);
		LayoutInflater inflater = getLayoutInflater();
		boolean showExcahngeUpgrade = getIntent().getBooleanExtra(INTENT_EXTRANAME_GUIDE_NEW_PAGE, false);
		Theme theme;
		if (showExcahngeUpgrade) {
			theme = new ThemeExchangeUpgrade();
		} else {
			theme = new ThemeNormal();
		}
		theme.init(root, inflater);
	}

	/**
	 * 包含“开始”按钮的页面（第三页）
	 */
	private class PageOfStartButton {

		private final ViewGroup layout;

		/** 开始（立即体验）按钮 */
		private final Button buttonStart;

		public PageOfStartButton(ViewGroup parent, LayoutInflater inflater) {
			layout = (ViewGroup) inflater.inflate(R.layout.guide_item3, parent, false);
			buttonStart = (Button) layout.findViewById(R.id.button_start);
			OnClickListener onClickListener = new OnClickListener() {
				@Override
				public void onClick(View v) {
					switch (v.getId()) {
					case R.id.text_licence:
						UIUtils.turnActivity(ActivityGuider.this, ActivityLicence.class);
						break;
					case R.id.button_start:
						UIUtils.turnActivity(ActivityGuider.this, ActivityMain.class);
						ActivityGuider.this.finish();
						break;
					}
				}
			};
			buttonStart.setOnClickListener(onClickListener);
			// “已阅读许可协议”勾选框
			CheckBox checkbox = (CheckBox) layout.findViewById(R.id.check_licence_already_read);
			checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					licenceCheckStyle(isChecked);
				}
			});
			licenceCheckStyle(true);
			// 勾选框的文字
			TextView textLicence = (TextView) layout.findViewById(R.id.text_licence);
			textLicence.setText(Html.fromHtml("<u>我已阅读并同意相关协议</u>"));
			textLicence.setOnClickListener(onClickListener);
		}

		private void licenceCheckStyle(boolean isChecked) {
			float alpha = isChecked ? 1.0f : 0.5f;
			buttonStart.setAlpha(alpha);
			buttonStart.setEnabled(isChecked);
		}

		public View getView() {
			return layout;
		}
	}

	private abstract class Theme {
		private boolean initialized;
		
		public void init(ViewGroup root, LayoutInflater inflater) {
			if (initialized) {
				throw new IllegalStateException();
			}
			initialized = true;
			doInit(root, inflater);
		}
		
		protected abstract void doInit(ViewGroup root, LayoutInflater inflater);

	}
	
	private class ThemeExchangeUpgrade extends Theme {

		@Override
		protected void doInit(ViewGroup root, LayoutInflater inflater) {
			View layout = inflater.inflate(R.layout.activity_guide_exchange_upgrade, root, false);
			layout.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					UIUtils.turnActivity(ActivityGuider.this, ActivityMain.class);
					ActivityGuider.this.finish();
				}
			});
			root.addView(layout);		
		}
		
	}
	
//	@Deprecated
//	private class ThemeSinglePage extends Theme {
//
//		@Override
//		protected void doInit(ViewGroup root, LayoutInflater inflater) {
//			ViewGroup layout = (ViewGroup)inflater.inflate(R.layout.activity_guide_single_page, root, false);
//			layout.addView(new PageOfStartButton(root, inflater).getView());
//			root.addView(layout);			
//		}
//	}

	private class ThemeNormal extends Theme {

		private static final int PAGE_COUNT = 3;

		/** 三页 */
		private final View[] pageViews = new View[PAGE_COUNT];

		/** 三个小圆点 */
		private final ImageView[] dotList = new ImageView[PAGE_COUNT];

		@Override
		protected void doInit(ViewGroup root, LayoutInflater inflater) {
			ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.activity_guide_normal, root, false);
			root.addView(layout);
			//
			ViewPager viewPager = (ViewPager) layout.findViewById(R.id.guide_viewpager);
			initDotList( (ViewGroup) layout.findViewById(R.id.guide_point_ll) );
			//
			pageViews[0] = inflater.inflate(R.layout.guide_item1, viewPager, false);
			pageViews[1] = inflater.inflate(R.layout.guide_item2, viewPager, false);
			pageViews[2] = new PageOfStartButton(viewPager, inflater).getView();
			//
			viewPager.setAdapter(new GuidePageAdapter());
			viewPager.addOnPageChangeListener(new GuidePageChangeListener());
		}

		private void initDotList(ViewGroup containerOfDotList) {
			Context context = containerOfDotList.getContext();
			for (int i = 0; i < dotList.length; i++) {
				ImageView dot = new ImageView(context);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
				params.setMargins(MetricsUtils.dp2px(context, 14), 0, MetricsUtils.dp2px(context, 14), 0);
				dot.setLayoutParams(params);
				dot.setBackgroundResource(
					(i == 0)
						? R.drawable.the_introductory_pages_carousel_now
						: R.drawable.the_introductory_pages_carousel_before);
				containerOfDotList.addView(dot);
				dotList[i] = dot;
			}
		}

		private class GuidePageAdapter extends PagerAdapter {

			@Override
			public int getCount() {
				return pageViews.length;
			}

			@Override
			public boolean isViewFromObject(View view, Object object) {
				return view == object;
			}

			@Override
			public int getItemPosition(Object object) {
				return super.getItemPosition(object);
			}

			@Override
			public void destroyItem(ViewGroup container, int position, Object object) {
				container.removeView(pageViews[position]);
			}

			@Override
			public Object instantiateItem(View container, int position) {
				((ViewGroup) container).addView(pageViews[position]);
				return pageViews[position];
			}

			@Override
			public void restoreState(Parcelable state, ClassLoader loader) {

			}

			@Override
			public Parcelable saveState() {
				return null;
			}

			@Override
			public void startUpdate(View view) {

			}

			@Override
			public void finishUpdate(View view) {

			}
		}

		// 指引页面更改事件监听器,左右滑动图片时候，小圆点变换显示当前图片位置
		private class GuidePageChangeListener implements OnPageChangeListener {

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

			}

			@Override
			public void onPageSelected(int position) {
				for (int i = 0; i < dotList.length; i++) {
					dotList[position].setBackgroundResource(R.drawable.the_introductory_pages_carousel_now);
					if (position != i) {
						dotList[i].setBackgroundResource(R.drawable.the_introductory_pages_carousel_before);
					}
				}
			}

			@Override
			public void onPageScrollStateChanged(int state) {

			}

		}
	}
}
