package cn.wsds.gamemaster.debugger;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import cn.wsds.gamemaster.R;

public class ActivityDebugMain extends FragmentActivity {

	private ViewPager viewPager;

	private static class MyAdapter extends FragmentPagerAdapter {

		public MyAdapter(FragmentManager fm) {
			super(fm);
		}

		private static final Page[] PAGE_LIST = new Page[] {
			new Page(FragmentDebug00.class),
			new Page(FragmentDebug01.class),
			new Page(FragmentDebug02.class),
			new Page(FragmentDebug03.class),
			new Page(FragmentDebug04.class, "Qos"),
			new Page(FragmentDebug05.class),
			new Page(FragmentDebug06.class),
			new Page(FragmentDebug07.class),
		};

		private static class Page {
			public final Class<? extends Fragment> cls;
			public final String title;

			public Page(Class<? extends Fragment> cls) {
				this(cls, null);
			}

			public Page(Class<? extends Fragment> cls, String title) {
				this.cls = cls;
				this.title = title;
			}
		}

		@Override
		public Fragment getItem(int position) {
			Page page = PAGE_LIST[position];
			try {
				return page.cls.getConstructor().newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public int getCount() {
			return PAGE_LIST.length;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Page page = PAGE_LIST[position];
			return page.title == null ? String.format("%02d", position + 1) : page.title;
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_debug_main);
		viewPager = (ViewPager) findViewById(R.id.view_pager);
		viewPager.setAdapter(new MyAdapter(this.getSupportFragmentManager()));
	}

}
