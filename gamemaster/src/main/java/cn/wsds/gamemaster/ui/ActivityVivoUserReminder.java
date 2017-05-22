package cn.wsds.gamemaster.ui;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TabWidget;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.tools.SystemInfoUtil;

@SuppressLint("InflateParams")
public class ActivityVivoUserReminder extends ActivityBase {

	private final String[] titles = new String[]{"闪退问题","悬浮窗问题"};
	
	private TabWidget tabWidget ;
	 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		 
		super.onCreate(savedInstanceState);
		setDisplayHomeArrow(R.string.vivo_user_read_me);
		
		if(!SystemInfoUtil.isStrictOs()){  //Android 5.0以下不显示悬浮窗提示页
			setContentView(R.layout.fragment_vivo_read_me);
			return ;
		}
		
	    setContentView(R.layout.activity_vivo_read_me);
	    initView();
	}

	@SuppressWarnings("deprecation")
	private void initView(){  //TabWidget 与ViewPager 结合实现滑动的tab控件
		  
        ArrayList <View> viewList = new ArrayList<View>(2); 
        
        //为tabWidget配置内容 
        tabWidget = (TabWidget)findViewById(R.id.tWidget);
        tabWidget.setStripEnabled(false);
        for(int i = 0; i<titles.length ; i++){       	
        	tabWidget.addView(getTabItemView(i));
        	viewList.add(getContentView(i));       	
        }
         
        //为ViewPager配置内容，设置滑动监听事件;	 
        TabPagerAdapter pagerAdapter = new TabPagerAdapter(viewList);
        final ViewPager viewPager = (ViewPager) findViewById(R.id.vPager);  
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOnPageChangeListener(new TabOnPageChangeListener());
        
        //tabWidget设置Click事件;
        int tabCount = tabWidget.getChildCount();
        for(int i = 0 ; i<tabCount ; i++){	
        	final int index =  i ;
        	tabWidget.getChildAt(i).setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					viewPager.setCurrentItem(index);
				}	
        	});
        }
        
        tabWidget.setCurrentTab(0);
	}
	
	private View getTabItemView(int i) {  
        View view = LayoutInflater.from(this).inflate(R.layout.layout_vivo_read_me_tab, null);  
        TextView textView = (TextView) view.findViewById(R.id.text_title);  
        textView.setText(titles[i]);  
        return view;  
    }  
	
	private View getContentView(int index){
		
		if(index>0){
			return LayoutInflater.from(this).inflate(R.layout.fragment_vivo_read_me2,null) ;
			
		} 
		
		return LayoutInflater.from(this).inflate(R.layout.fragment_vivo_read_me,null) ;
	}
	
	private final class TabPagerAdapter extends PagerAdapter{
		
		private final List<View> viewList = new ArrayList<View>(2);
		
		TabPagerAdapter(List<View> viewList){
			this.viewList.addAll(viewList);
		}

		@Override
		public int getCount() {
			return viewList.size();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {		 
			return (arg0 == arg1);
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
			((ViewPager) container).removeView(viewList.get(position));
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			((ViewPager) container).addView(viewList.get(position), 0);  
            return viewList.get(position);  
		}
		
		@Override
		public Parcelable saveState() {
			return null;
		}
		 
	}
	
	private final class TabOnPageChangeListener  implements OnPageChangeListener{

		@Override
		public void onPageScrollStateChanged(int arg0) {
			
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
			 
		}

		@Override
		public void onPageSelected(int arg0) {
			tabWidget.setCurrentTab(arg0);
		}
		
	}
	
}
