package cn.wsds.gamemaster.ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.dialog.CommonAlertDialog;
import cn.wsds.gamemaster.dialog.CommonDialog;
import cn.wsds.gamemaster.dialog.ReaccelPrompt;
import cn.wsds.gamemaster.netdelay.NetDelayDataManager;
import cn.wsds.gamemaster.netdelay.NetDelayDataManager.TimePointContainer;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.NetDelayChart.OnUpdateListener;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.accel.AccelOpenWorker;
import cn.wsds.gamemaster.ui.view.ImageViewAlpha;

/** 展示游戏列表的adapter */
@SuppressLint("SimpleDateFormat")
public class AdapterGameList extends BaseExpandableListAdapter {

	private final SimpleDateFormat formatterOfBeginTime = new SimpleDateFormat("dd日HH:mm");
	private final SimpleDateFormat formatterOfEndTime = new SimpleDateFormat("HH:mm");

	private final StartNodeDetectUI.Owner owner;
	private AccelOpenWorker accelOpenWorker;

	/** 游戏列表 */
	private final List<GameInfo> gameList = new ArrayList<GameInfo>();

	private final LayoutInflater inflater;

	private final SparseArray<ViewHolder> viewList = new SparseArray<ViewHolder>();

	private int currentExpandedGroupPosition = -1;

	/** 最后一个处于展开状态的是哪一个游戏 */
	private int lastExpandedGameUID = -1;

	/** 为True时，灰掉除启动按钮以外的所有元素 */
	private boolean grayExceptStartButton;

	public AdapterGameList(StartNodeDetectUI.Owner owner) {
		this.owner = owner;
		this.inflater = LayoutInflater.from(owner.getActivity());
		resetGameList(gameList);
	}

	public void setGameList(List<GameInfo> gameList) {
		if (resetGameList(gameList)) {
			this.notifyDataSetChanged();
		}
	}
	
	public void setAccelOpenWorker(AccelOpenWorker accelOpenWorker) {
		this.accelOpenWorker = accelOpenWorker;
	}

	private boolean resetGameList(List<GameInfo> gameList) {
		if (this.gameList == gameList) {
			return false;
		}
		this.gameList.clear();
		this.notifyDataSetInvalidated();
		this.gameList.addAll(gameList);
		return true;
	}

	@Override
	public void onGroupCollapsed(int groupPosition) {
		if (currentExpandedGroupPosition == groupPosition) {
			currentExpandedGroupPosition = -1;
			lastExpandedGameUID = -1;
		}
		//
		ViewHolder holder = getViewHolder(groupPosition);
		if (holder != null) {
			holder.appArrow.setImageResource(R.drawable.game_detailed);
		}
	}

	@Override
	public void onGroupExpanded(int groupPosition) {
		currentExpandedGroupPosition = groupPosition;
		lastExpandedGameUID = gameList.get(groupPosition).getUid();
		ViewHolder holder = getViewHolder(groupPosition);
		if (holder != null) {
			holder.appArrow.setImageResource(R.drawable.game_detailed_open);
		}
	}

	ViewHolder getViewHolder(int position) {
		return viewList.get(position);
	}

	@Override
	public int getGroupCount() {
		return gameList.size();
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return 1;
	}

	@Override
	public Object getGroup(int groupPosition) {
		return gameList.get(groupPosition);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return null;
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_speed_app, parent, false);
			viewHolder = new ViewHolder();
			viewHolder.appIcon = (ImageViewAlpha) convertView.findViewById(R.id.image_icon);
			viewHolder.appLabel = (TextView) convertView.findViewById(R.id.text_app_label);
			viewHolder.appArrow = (ImageView) convertView.findViewById(R.id.game_detail_arrow);
			viewHolder.markOverseasGame = (TextView)convertView.findViewById(R.id.text_overseas_game_label);
			viewHolder.accelModelLabel = (TextView) convertView.findViewById(R.id.text_accel_model_label);
			viewHolder.appStart = (TextView) convertView.findViewById(R.id.button_openapp);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		setItemContent(groupPosition, viewHolder, isExpanded);
		viewHolder.appStart.setTag(groupPosition);
		viewList.put(groupPosition, viewHolder);
		return convertView;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		ChildViewHolder childViewHolder;
		if (convertView == null) {
			childViewHolder = new ChildViewHolder();
			convertView = inflater.inflate(R.layout.expand_list_item, parent, false);
			childViewHolder.flipperOfLoadingAndMain = (ViewFlipper) convertView.findViewById(R.id.view_flipper_in_item);
			childViewHolder.restartAccelLayout = convertView.findViewById(R.id.item_restart_accel);
			childViewHolder.historyLayout = convertView.findViewById(R.id.item_history);
			//隐藏加速详细
			childViewHolder.historyLayout.setVisibility(View.GONE);
			childViewHolder.historyImage = (ImageView) convertView.findViewById(R.id.item_history_image);
			childViewHolder.historyText = (TextView) convertView.findViewById(R.id.item_history_text);
			childViewHolder.recordTime = (TextView) convertView.findViewById(R.id.item_record_time);
			childViewHolder.delayChart = (NetDelayChart) convertView.findViewById(R.id.item_delay_chart);
			childViewHolder.averageDelayValue = (TextView) convertView.findViewById(R.id.item_average_delay_value);
			childViewHolder.timeoutPercent = (TextView) convertView.findViewById(R.id.item_timeout_percent);
			convertView.setTag(childViewHolder);
		} else {
			childViewHolder = (ChildViewHolder) convertView.getTag();
		}

		setChildContent(childViewHolder, groupPosition);
		return convertView;
	}

	/**
	 * 点击“启动”按钮的事件处理
	 */
	private class OnButtonStartClickListener implements View.OnClickListener {

		private final int position;

		public OnButtonStartClickListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			if (position < 0 || position >= gameList.size()) {
				return;
			}

			//统计启动游戏总次数
			int startNum = ConfigManager.getInstance().getStartGameCount();
			startNum = (startNum + 1) % 65535;
			ConfigManager.getInstance().setStartGameCount(startNum);
			// 统计事件
			ConfigManager.getInstance().setClickGameLaunchButton();
			Statistic.addEvent(v.getContext(), Statistic.Event.ACC_HOMEPAGE_CLICK_GAME_START);
			//			
			GameInfo gameInfo = gameList.get(position);
			if (AccelOpenManager.isStarted()) {				
				StartNodeDetectUI.createInstanceIfNotExists(owner, gameInfo, null);				
			} else {
				doWhenAccelStatusOff();
			}
		}

		private void doWhenAccelStatusOff() {
			if (ConfigManager.getInstance().getFirstLaunchGame()) {
				ConfigManager.getInstance().setFirstLaunchGame(false);
				CommonDialog dialog = new CommonAlertDialog(owner.getActivity());
				dialog.setTitle("提示");
				dialog.setMessage("您还没有开启加速服务，将先为您加速。");
				dialog.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						openAccel();
					}
				});
				dialog.show();
			} else {
				openAccel();
			}
		}
	}

	private void openAccel() {
		if(accelOpenWorker!=null){
			accelOpenWorker.openAccel();
		}
	}

	/** 为列表项设置内容 */
	@SuppressWarnings("deprecation")
	private void setItemContent(int position, ViewHolder viewHolder, boolean isExpanded) {
		viewHolder.appStart.setOnClickListener(new OnButtonStartClickListener(position));
		GameInfo info = gameList.get(position);
		viewHolder.appIcon.setImageDrawable(info.getAppIcon(owner.getActivity()));
		viewHolder.appLabel.setText(info.getAppLabel());
		viewHolder.appArrow.setImageResource(isExpanded ? R.drawable.game_detailed_open : R.drawable.game_detailed);
		bindAccelModel(viewHolder, info);

		// 是否要灰掉？
		int alpha = grayExceptStartButton ? 48 : 255;
		float alpha_float = (float) alpha / 255;
		viewHolder.appIcon.setDrawAlpha(alpha);
		viewHolder.appLabel.setAlpha(alpha_float);
		viewHolder.appArrow.setAlpha(alpha); // 兼容到API 14
		initOverseasView(viewHolder, info, alpha_float);

		viewHolder.accelModelLabel.setAlpha(alpha_float);
		// 加速状态不同，启动按钮的颜色也不同
		viewHolder.appStart.setBackgroundResource(AccelOpenManager.isStarted() ? R.drawable.main_list_start_btn_bg
			: R.drawable.main_list_start_btn_bg_translucent);
	}
	
	private void initOverseasView(ViewHolder viewHolder, GameInfo info, float alpha_float) {
		// 海外游戏
		boolean isForeignGame = info.isForeignGame();
		viewHolder.markOverseasGame.setVisibility(isForeignGame ? View.VISIBLE : View.GONE);
		if (isForeignGame) {
			//单独处理虚荣游戏
			if(isVaingloryGame(info)) {
				viewHolder.markOverseasGame.setText("自动识别国服外服");
			}
			viewHolder.markOverseasGame.setAlpha(alpha_float);
		}
	}

	private boolean isVaingloryGame(GameInfo info) {
		String str = info.getAppLabel().toLowerCase();
		return str.contains("vainglory") || str.contains("虚荣");
	}

	private void bindAccelModel(ViewHolder viewHolder, GameInfo info) {
		if(info.isAccelByVPNRecommend()){
			viewHolder.accelModelLabel.setVisibility(View.VISIBLE);
			viewHolder.accelModelLabel.setText("请用VPN");
			return;
		}
		viewHolder.accelModelLabel.setVisibility(View.GONE);
	}

	private void setChildContent(final ChildViewHolder childViewHolder, int groupPosition) {
		final GameInfo gameInfo = gameList.get(groupPosition);
		TimePointContainer timePointContainer = NetDelayDataManager.getInstance().getTimePoints(gameInfo.getPackageName());

		if (timePointContainer == null || timePointContainer.getCount() <= 0) {
			childViewHolder.flipperOfLoadingAndMain.setDisplayedChild(0);
			childViewHolder.historyImage.setImageResource(R.drawable.list_icon_history_grey);
			childViewHolder.historyText.setTextColor(AppMain.getContext().getResources().getColor(R.color.color_game_31));
			childViewHolder.historyLayout.setEnabled(false);
		} else {
			childViewHolder.flipperOfLoadingAndMain.setDisplayedChild(1);
			childViewHolder.historyImage.setImageResource(R.drawable.list_icon_history);
			childViewHolder.historyText.setTextColor(AppMain.getContext().getResources().getColor(R.color.color_game_6));
			childViewHolder.historyLayout.setEnabled(true);
			childViewHolder.delayChart.setDataSeconds(300);
			childViewHolder.delayChart.setDataSource(timePointContainer, false);
			childViewHolder.delayChart.setOnUpdateListener(new OnUpdateListener() {
				@Override
				public void onNetDelayChartUpdate(NetDelayChart sender) {
					NetDelayChart.Info info = childViewHolder.delayChart.getStatistic();
					String period;
					if (info.beginTime < 0 || info.endTime < 0) {
						period = "---";
					} else {
						period = String.format("%s-%s", getFormatTime(info.beginTime, true), getFormatTime(info.endTime, false));
					}
					childViewHolder.recordTime.setText(period);
					childViewHolder.averageDelayValue.setText(info.avgDelay < 0 ? "---" : String.format("%dms", info.avgDelay));
					childViewHolder.timeoutPercent.setText(info.timeoutPercent + "%");
					int colorId = (info.timeoutPercent <= 5) ? R.color.color_game_11 : R.color.color_game_16;
					childViewHolder.timeoutPercent.setTextColor(childViewHolder.timeoutPercent.getContext().getResources().getColor(colorId));
				}
			});

		}

		childViewHolder.restartAccelLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (AccelOpenManager.isStarted()) {
					ReaccelPrompt.execute(owner.getActivity(), gameInfo.getUid());
				} else {
					openAccel();
				}
			}
		});

		childViewHolder.historyLayout.setOnClickListener(new OnButtonHistoryClickListener(gameInfo.getPackageName()));
	}

	private class OnButtonHistoryClickListener implements View.OnClickListener {

		private final String packageName;

		public OnButtonHistoryClickListener(String packageName) {
			this.packageName = packageName;
		}

		@Override
		public void onClick(View v) {
			UIUtils.turnGameAccelDesc(owner.getActivity(), packageName);
		}
	}

	private String getFormatTime(long time, boolean start) {
		SimpleDateFormat formatter = start ? formatterOfBeginTime : formatterOfEndTime;
		Date curDate = new Date(time);
		return formatter.format(curDate);

	}

	private static class ViewHolder {
		TextView markOverseasGame;
		ImageViewAlpha appIcon;
		TextView appLabel;
		ImageView appArrow;
		/** 启动按钮 */
		TextView appStart;
		TextView accelModelLabel;
	}

	private static class ChildViewHolder {
		/** 包含“Loading效果”和“常规曲线图”的Flipper */
		private ViewFlipper flipperOfLoadingAndMain;
		/** 按钮：重新加速 */
		private View restartAccelLayout;
		/** 按钮：历史详情 */
		private View historyLayout;
		/** 历史详情按钮上面的图标 */
		private ImageView historyImage;
		/** 历史详情按钮上面的文字 */
		private TextView historyText;
		private TextView recordTime;
		private NetDelayChart delayChart;
		private TextView averageDelayValue;
		private TextView timeoutPercent;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}

	private int findLastExpandedGamePosition() {
		if (lastExpandedGameUID < 0) {
			return -1;
		}
		for (int i = gameList.size() - 1; i >= 0; --i) {
			if (lastExpandedGameUID == gameList.get(i).getUid()) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 展开最近一次展开的项
	 * 
	 * @param listView
	 *            要操作的{@link ExpandableListView}
	 * @param loadingEffect
	 *            为true表示需要Loading特效
	 * @return true表示有需要展开的项并且展开了，false表示没有
	 */
	public boolean expandLastGroup(ExpandableListView listView, boolean loadingEffect) {
		int pos = findLastExpandedGamePosition();
		//		Log.e("TTT", "expandLastGroup: " + pos);
		if (pos >= 0) {
			listView.expandGroup(pos);
			return true;
		}
		return false;
	}

	/**
	 * 收拢所有条目
	 */
	public boolean collapseAll(ExpandableListView accelListView) {
		if (currentExpandedGroupPosition >= 0) {
			accelListView.collapseGroup(currentExpandedGroupPosition);
			currentExpandedGroupPosition = -1;
			return true;
		}
		return false;
	}

	/**
	 * 设置：是否灰掉除“启动按钮”以外的所有UI元素？
	 * 
	 * @param value
	 *            true表示是，false表示否
	 */
	public void setGrayExceptStartButton(boolean value) {
		if (this.grayExceptStartButton != value) {
			this.grayExceptStartButton = value;
			this.notifyDataSetChanged();
		}
	}

	/**
	 * 返回指定位置的启动按钮
	 * 
	 * @param position
	 *            Item的下标
	 * @return 启动按钮或null
	 */
	public View getStartButton(int position) {
		ViewHolder vh = this.getViewHolder(position);
		if (vh == null) {
			return null;
		}
		return vh.appStart;
	}
}
