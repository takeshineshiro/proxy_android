package cn.wsds.gamemaster.ui.mainfragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.TextView;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.Misc;
import cn.wsds.gamemaster.NetDelayDetector;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ResUsageChecker.ResUsage;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.IntentExtraName;
import cn.wsds.gamemaster.data.ProcessCleanRecords;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.ActivityMain;
import cn.wsds.gamemaster.ui.ActivityMemoryClean;
import cn.wsds.gamemaster.ui.AdapterGameList;
import cn.wsds.gamemaster.ui.NewGameSubmit;
import cn.wsds.gamemaster.ui.StartNodeDetectUI;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.accel.AccelOpenWorker;
import cn.wsds.gamemaster.ui.floatwindow.DelayTextSpannable;

import com.subao.net.NetManager;

/**
 * 主界面 （加速开启后 游戏列表相关部分）
 *  添加游戏、加速游戏列表、内存清理
 */
public class FragmentList extends Fragment{

	private final List<GameInfo> gameInfos = new ArrayList<GameInfo>();
	private static long memoryCleanTimeMillis;
	private int lastExpandPosition = -1;

	private MemoryCleanPrompt memoryCleanPormpt;
	private View launchGamePorpmt;
	private AdapterGameList adapter;
	private ExpandableListView accelListView;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_list, container, false);
		initView(view);
		refreshGameInfos();
		return view;
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		memoryCleanPormpt = null;
	}
	
	private void initView(View view){
		launchGamePorpmt = view.findViewById(R.id.launch_game_porpmt);
		view.findViewById(R.id.add_game_button).setOnClickListener(getAddGameListener());
		
        Activity activity = getActivity();
        if(activity==null){
        	return;
        }
        memoryCleanPormpt = new MemoryCleanPrompt(activity);
        
        accelListView = (ExpandableListView) view.findViewById(R.id.accel_list);
        //=== api 要求必须这么干，比较坑
        accelListView.addFooterView(memoryCleanPormpt.root);
        accelListView.removeFooterView(memoryCleanPormpt.root);
        //===
		accelListView.setEmptyView(view.findViewById(R.id.empty_view));
		accelListView.setOnGroupExpandListener(createOnGroupExpandListener());
		accelListView.setOnGroupCollapseListener(createOnGroupCollapseListener());
		accelListView.setOnGroupClickListener(createOnGroupClickListener());
		initAdapter();
	}

	private OnGroupClickListener createOnGroupClickListener() {
		return new OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				GameInfo gameInfo = gameInfos.get(groupPosition);
				return (gameInfo == null);
			}
		};
	}
	
	private OnGroupCollapseListener createOnGroupCollapseListener() {
		return new OnGroupCollapseListener() {
			
			@Override
			public void onGroupCollapse(int groupPosition) {
				if(lastExpandPosition == groupPosition){
					Statistic.addEvent(AppMain.getContext(), Statistic.Event.INTERACTIVE_CLICK_GAME_LIST,"收起");
				}
			}
		};
	}

	private OnGroupExpandListener createOnGroupExpandListener() {
		return new OnGroupExpandListener() {
			@Override
			public void onGroupExpand(int groupPosition) {
				lastExpandPosition = groupPosition;
				Statistic.addEvent(AppMain.getContext(), Statistic.Event.INTERACTIVE_CLICK_GAME_LIST,"展开");
				for (int i = 0; i < adapter.getGroupCount(); i++) {
					if (i != groupPosition && accelListView.isGroupExpanded(i)) {
						accelListView.collapseGroup(i);
					}
				}
			}
		};
	}
	
	private OnClickListener getAddGameListener() {
		return  new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Activity activity = getActivity();
				new NewGameSubmit(activity).execute();
			}
		};
	}
	
	
	
	public void refreshGameList() {
		refreshGameInfos();
		if(adapter==null){
			boolean init = initAdapter();
			if(!init){
				return;
			}
		}
		adapter.setGameList(gameInfos);
		setLaunchGamePormptVisible();
	}

	private boolean initAdapter() {
		Activity activity = getActivity();
		if(activity==null){
			return false;
		}
		//
		StartNodeDetectUI.Owner owner = new StartNodeDetectUI.Owner() {

			@Override
			public Activity getActivity() {
				return FragmentList.this.getActivity();
			}

			@Override
			public boolean isCurrentNetworkOk() {
				Activity activity = this.getActivity();
				if (activity instanceof ActivityMain) {
					int delay = NetDelayDetector.getDelayValue(ActivityMain.NET_DELAY_DETECT_TYPE);
					return delay >= 0 && delay < GlobalDefines.NET_DELAY_TIMEOUT;
				}
				return NetManager.getInstance().isConnected();
			}
			
		};
		
		
		adapter = new AdapterGameList(owner);
		if(accelListView!=null){
			accelListView.setAdapter(adapter);
			return true;
		}
		return false; 
	}

	public void setLaunchGamePormptVisible() {
		if(launchGamePorpmt!=null){
			launchGamePorpmt.setVisibility(gameInfos.isEmpty() ? View.GONE : View.VISIBLE);
		}
	}
	
	public void setAccelOpenWorker(AccelOpenWorker accelOpenWorker){
		Activity activity = getActivity();
		if(activity!=null){
			adapter.setAccelOpenWorker(accelOpenWorker);
		}
	}

	/** 刷新游戏列表内容 */
	private void refreshGameInfos() {
		List<GameInfo> supportedAndReallyInstalledGames = GameManager.getInstance().getSupportedAndReallyInstalledGames();
		GameInfoComparator comparator = new GameInfoComparator();
		Collections.sort(supportedAndReallyInstalledGames, comparator);
		gameInfos.clear();
		gameInfos.addAll(supportedAndReallyInstalledGames);
	}

	/** 列表排序方式的接口，先按照启动时间，再按照名称排序 */
	private class GameInfoComparator implements Comparator<GameInfo> {
		@Override
		public int compare(GameInfo arg0, GameInfo arg1) {
			int r = (int) (arg1.getLaunchedTime() - arg0.getLaunchedTime());
			if (r == 0) {
				r = arg0.getAppLabel().compareTo(arg1.getAppLabel());
			}
			return r;
		}
	}

	/**
	 * 当前系统占用资源发生变化
	 * @param data 资源占用情况
	 */
	public void onResUsageAccelOn(ResUsage data) {
		if (memoryCleanPormpt == null) {
			return;
		}
		Activity activity = getActivity();
		if (activity == null || activity.isFinishing()) {
			return;
		}
		if (!isTimeInMinCleanInterval() && Misc.isResUsageOverflow(data)) {
			int runningAppCount = data.runningAppList == null ? 0
					: ProcessCleanRecords.getInstance()
							.getCleanRecord(data.runningAppList).size();
			if (runningAppCount > 0) {
				memoryCleanPormpt.setVisibility(true, runningAppCount);
				return;
			}
		}
		memoryCleanPormpt.setVisibility(false, 0);
	}

	/** ListView的Footer 内存清理提示条 */
	private class MemoryCleanPrompt {

		private final View root;
		private final TextView cleanMemoryText;
		private final View button;
		private boolean alreadyAddToListView;

		MemoryCleanPrompt(Activity activity) {
			root = View.inflate(activity, R.layout.main_clear_mem, null);
			View.OnClickListener listener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Activity activity = getActivity();
					if (activity == null) {
						return;
					}
					// 打开 内存清理动画并执行内存清理
					String strParam = AccelOpenManager.isStarted() ? "加速后进入内存清理页"
							: "加速前进入内存清理页";
					Statistic.addEvent(activity,
							Statistic.Event.INTERACTIVE_CLEARRAM_IN,
							strParam);
					Intent intent = new Intent(activity,
							ActivityMemoryClean.class);
					Object tag = v.getTag();
					if (tag != null && (tag instanceof Integer)) {
						intent.putExtra(
								IntentExtraName.INTENT_EXTRANAME_APP_CLEAN_COUNT,
								(Integer) v.getTag());
					}
					startActivityForResult(
							intent,
							GlobalDefines.START_ACTIVITY_REQUEST_CODE_MEMORY_CLEAN);
				}
			};
			this.button = root.findViewById(R.id.button_clean_memory);
			this.button.setOnClickListener(listener);
			this.cleanMemoryText = (TextView) root
					.findViewById(R.id.clean_memory_text);
		}

		void setVisibility(boolean visibility, int runningAppCount) {
			Activity activity = getActivity();
			if (activity == null || activity.isFinishing()) {
				return;
			}
			if (visibility) {
				cleanMemoryText.setText(DelayTextSpannable.getSpecialInMid("有",
						String.valueOf(runningAppCount), "个应用偷偷运行，影响游戏速度",
						getResources().getColor(R.color.color_game_16)));
				button.setTag(runningAppCount);
				if (!alreadyAddToListView) {
					accelListView.addFooterView(root);
					alreadyAddToListView = true;
					// 下面这个如果不加，在某些机型（红米和一个三星手机上面）就无法显示
					// footer，而且最后一项展不开
					if (adapter != null) {
						accelListView.setAdapter(adapter);
					}
				}
				
			} else {
				if (alreadyAddToListView) {
					accelListView.removeFooterView(root);
					alreadyAddToListView = false;
				}
			}

		}

		void getLocationOnScreen(int[] location) {
			root.getLocationOnScreen(location);
		}
	}
	
	private static long getMemoryCleanTimeMillis() {
		return memoryCleanTimeMillis;
	}

	public static void setMemoryCleanTimeMillis() {
		memoryCleanTimeMillis = SystemClock.elapsedRealtime();
	}

	/**
	 * 两次清理最小间隔 (如果当前)
	 */
	private static final int MIN_CLEAN_TIME_MILLIS_INTERVAL = 300000;

	public static boolean isTimeInMinCleanInterval() {
		if (getMemoryCleanTimeMillis() == 0) {
			return false;
		}
		if (SystemClock.elapsedRealtime() - getMemoryCleanTimeMillis() <= MIN_CLEAN_TIME_MILLIS_INTERVAL) {
			return true;
		}
		return false;
	}

	public boolean collapseAll() {
		if(adapter==null ||accelListView==null){
			return false;
		}
		return adapter.collapseAll(accelListView);
	}
	
	public View getLaunchGamePorpmt() {
		return this.launchGamePorpmt;
	}
	

	public List<GameInfo> getGameInfos() {
		return this.gameInfos;
	}
	
	public AdapterGameList getListAdapter() {
		return adapter;
	}

	public ExpandableListView getAccelListView() {
		return this.accelListView;
	}

	public void setMemoryCleanPromptVisible(boolean visibility, int runningAppCount) {
		if (memoryCleanPormpt != null) {
			memoryCleanPormpt.setVisibility(visibility, runningAppCount);
		}
	}

	public boolean getMemoryCleanPromptLocationOnScreen(int[] location) {
		if (memoryCleanPormpt != null) {
			memoryCleanPormpt.getLocationOnScreen(location);
			return true;
		}
		return false;
	}
}
