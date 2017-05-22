package cn.wsds.gamemaster.ui;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import cn.wsds.gamemaster.Misc;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.AccelDetails;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.IntentExtraName;
import cn.wsds.gamemaster.ui.view.LoadingRing;
import cn.wsds.gamemaster.ui.view.LoadingRing.OnCompleteListener;

import com.subao.common.utils.CalendarUtils;
import com.subao.resutils.WeakReferenceHandler;
import com.subao.utils.StringUtils;

/**
 * 加速详情界面
 */
public class ActivityGameAccelDesc extends ActivityBase {
	
	private static final int STATUS_ONCREATE = 0;
	private static final int STATUS_ONRESTART = 1;
			
	private ImageView gameIncon;
	private TextView totalAccessSecond;
	private TextView totalShortTime;
	private TextView totalCutlinkCount;
	private LinearLayout totalMessageLayout;
	private LinearLayout totalCutlinkLayout;
	private TextView sorryMessage;
	private ListView accelDetailList;
//	private List<AccelDetails> accelsDetails;
	private ImageView vitrualLine;
	private ImageView topStrokLine;
	private AccelDescListAdapter adapter;
	private LoadingRing loadRing;
	private FrameLayout datalistArea;
	private LinearLayout errorLayout;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setDisplayHomeArrow(R.string.accel_desc);
		setContentView(R.layout.activity_accel_desc);
		initView();
		initData(STATUS_ONCREATE);
	}
	
	
	@Override
	protected void onRestart() {
		super.onRestart();
		initData(STATUS_ONRESTART);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		stopAnimation();
		if(this.isNeedLoad){
			this.isNeedLoad = false;
		}
	}

	private void stopAnimation() {
		Animation animation = loadRing.getAnimation();
		if(animation!=null){
			animation.cancel();
		}
		if(itemElementManager.isRunning()){
			itemElementManager.stop();
			accelDetailList.setOnTouchListener(null);
			adapter.notifyDataSetChanged();
		}
	}
	
	/**
	 * 初始化view
	 */
	private void initView() {
		loadRing = (LoadingRing) findViewById(R.id.accel_list_refresh);
		gameIncon = (ImageView) findViewById(R.id.accel_desc_game_icon);
		vitrualLine = (ImageView) findViewById(R.id.vitrual_line);
		topStrokLine = (ImageView) findViewById(R.id.top_strok_line);
		totalAccessSecond = (TextView) findViewById(R.id.total_access_second);  //累计加速时长
		totalShortTime = (TextView) findViewById(R.id.total_short_time);     //累计减少等待
		totalCutlinkCount = (TextView) findViewById(R.id.total_cut_link_count);  //累计防断线次数
		totalMessageLayout = (LinearLayout) findViewById(R.id.total_message_layout);  //累计 统计信息 区域
		totalCutlinkLayout = (LinearLayout) findViewById(R.id.total_cut_link_layout);  //累计防断线 区域
		sorryMessage = (TextView) findViewById(R.id.accel_desc_sorry_msg);  //中间面板无内容提示
		accelDetailList = (ListView) findViewById(R.id.accel_desc_list);    //中间列表
		datalistArea = (FrameLayout) findViewById(R.id.accel_datalist_area);
		errorLayout = (LinearLayout) findViewById(R.id.errory_layout);
	} 

	/**
	 * 初始化界面数据
	 */
	private void initData(int status) {
		Intent intent = getIntent();
//		statisticEvent(intent);
		GameInfo info = getGameInfo(intent);
		
		if(info == null) { 									//游戏已经卸载	
			totalMessageLayout.setVisibility(View.GONE);
			datalistArea.setVisibility(View.GONE);
			gameIncon.setImageDrawable(UIUtils.loadAppDefaultIcon(ActivityGameAccelDesc.this));
			sorryMessage.setText("游戏已卸载><");
		}else{
			refreshAnimation(status);
			datalistArea.setVisibility(View.VISIBLE);
			totalMessageLayout.setVisibility(View.VISIBLE);
			gameIncon.setImageDrawable(info.getAppIcon(this));
			int totalAccess = info.getAccumulateAccelTimeSecond();
			int reconnectTimes = info.getAccumulateReconnectCount();
			List<AccelDetails> accelsDetails = info.cloneAccelDetails();
			sortListData(accelsDetails);
			if(totalAccess <= 0){  							//加速时长为0,没有加速记录
				totalMessageLayout.setVisibility(View.GONE);
				totalCutlinkLayout.setVisibility(View.VISIBLE);
			}else if(reconnectTimes <= 0){					//累计防断线为0，不予显示该记录
				totalCutlinkLayout.setVisibility(View.GONE);
				totalMessageLayout.setVisibility(View.VISIBLE);
			}
															//列表数据为空
			if(accelsDetails.isEmpty()){
				datalistArea.setVisibility(View.GONE);
				errorLayout.setVisibility(View.VISIBLE);
				//显示sorry图标
				if(totalAccess <= 0){
					vitrualLine.setVisibility(View.VISIBLE);
					topStrokLine.setVisibility(View.VISIBLE);
					sorryMessage.setText("暂无加速记录，快去使用吧~");
				}else{
					vitrualLine.setVisibility(View.INVISIBLE); 
					topStrokLine.setVisibility(View.INVISIBLE);
					sorryMessage.setText("当前没有加速记录哦~");
				}	
			}else{
				datalistArea.setVisibility(View.VISIBLE);
				errorLayout.setVisibility(View.GONE);
			}
			
			//注意，必须获取accelsDetails后才能设置适配器
			adapter = new AccelDescListAdapter(accelsDetails);
			accelDetailList.setAdapter(adapter);
			totalAccessSecond.setText(Misc.formatTime(totalAccess, "时"));
			totalShortTime.setText(UIUtils.formatTotalSparedTime(info.getAccumulateShortenWaitTimeMilliseconds()));
			totalCutlinkCount.setText(reconnectTimes + "次");
		}
	}

	/**
	 * 执行刷新动画
	 * @param status 
	 */
	private void refreshAnimation(final int status) {
		accelDetailList.setVisibility(View.INVISIBLE);
		loadRing.setDuration(1000 + (int)(Math.random() * 500));
		loadRing.start(new OnCompleteListener() {
			
			@Override
			public void onComplete() {
				accelDetailList.setVisibility(View.VISIBLE);
				loadRing.setVisibility(View.INVISIBLE);
				if(status == STATUS_ONCREATE && isNeedLoad){
					initListAnimation();
				}
			}
		});
		loadRing.setVisibility(View.VISIBLE);
	}

//	/**统计通知阅读次数*/
//	private void statisticEvent(Intent intent) {
//		if (intent.getBooleanExtra(IntentExtraName.START_FROM_NOTIFICATION, false)){
//			StatisticDefault.addEvent(this, StatisticDefault.Event.NOTIFICATION_REPORT_GAME_READ);
//		}
//	}

	/**找寻GameInfo */
	private GameInfo getGameInfo(Intent intent) {
		String packageName = intent.getStringExtra(IntentExtraName.INTENT_EXTRANAME_PACKAGE_NAME);
		if(packageName == null) return null;
		GameInfo info = GameManager.getInstance().getInstalledGameInfo(packageName);
		if(info == null || info == GameManager.UNINSTALLED_GAMEINFO) return null;
		return info;
	}
	
	private void sortListData(List<AccelDetails> details){
		Collections.sort(details, new Comparator<AccelDetails>() {
			
			@Override
			public int compare(AccelDetails lhs, AccelDetails rhs) {
				if (rhs.getLaunchedTime() > lhs.getLaunchedTime()) {
					return 1;
				} else if (rhs.getLaunchedTime() < lhs.getLaunchedTime()) {
					return -1;
				} else {
					return 0;
				}
			}
		});
	}
	
	private class AccelDescListAdapter extends BaseAdapter{
		private final LayoutInflater inflater;
		private final SparseArray<ViewHolder> holderList = new SparseArray<ViewHolder>();
		private final List<AccelDetails> data;
		
		public AccelDescListAdapter(List<AccelDetails> accelsDetails){
			inflater = getLayoutInflater();
			this.data = accelsDetails;
		}
		
		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public Object getItem(int position) {
			return data.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		public ViewHolder getViewHolder(int position){
			return holderList.get(position);
		}
		
		private int getDay(int position){
			AccelDetails details = data.get(position);
			Calendar calendar = CalendarUtils.calendarLocal_FromMilliseconds(details.getLaunchedTime());
			return CalendarUtils.dayFrom_CalendarLocal(calendar);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if(convertView == null){
				convertView = inflater.inflate(R.layout.item_accel_details, parent, false);
				holder = new ViewHolder(convertView);
				if(convertView != null){
					convertView.setTag(holder);
				}
				holderList.put(position, holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}

			if(!isNeedLoad){
				itemElementManager.noWaitLoad(holder);
			}
			
			AccelDetails details = data.get(position);
			Calendar calendar = CalendarUtils.calendarLocal_FromMilliseconds(details.getLaunchedTime());
			int currentDay = CalendarUtils.dayFrom_CalendarLocal(calendar);
			if(position==0){
				holder.headGroup.setVisibility(View.VISIBLE);
			}else{
				int previousData = getDay(position-1);
				if(previousData == currentDay){
					holder.headGroup.setTag("gone");
					holder.headGroup.setVisibility(View.GONE);
				}else{
					holder.headGroup.setTag(null);
					if(!isNeedLoad){
						holder.headGroup.setVisibility(View.VISIBLE);
					}
				}
			}
			
			holder.textDate.setText(calendar.get(Calendar.YEAR) + "/" 
					+ (calendar.get(Calendar.MONTH) + 1) + "/" 
					+ (calendar.get(Calendar.DAY_OF_MONTH)));  //设置日期
			holder.textTime.setText(getTimeByCalendar(calendar));  //设置时间
			holder.textDelayProgress.setText(String.valueOf(details.getPercentDelayDecrease()) + "%");  //设置平均降低延迟
			holder.progressDelay.setProgress(details.getPercentDelayDecrease());
			holder.textAccelTime.setText(
					Misc.formatTime((int)(details.getDuration() / 1000), "小时"));        //设置本次加速时长
			if(details.getFlow() <= 0){
				if(isNeedLoad){
					holder.textConsumptionFlow.setTag("gone");
					holder.descConsumptionFlow.setTag("gone");
				}
				holder.textConsumptionFlow.setVisibility(View.GONE);
				holder.descConsumptionFlow.setVisibility(View.GONE);
			}else{
				StringUtils.FlowString flowString = new StringUtils.FlowString(details.getFlow(), 1);
				holder.textConsumptionFlow.setText(flowString.getValue() + flowString.getUnit());  			 //设置流量消耗
				if(!isNeedLoad){
					holder.textConsumptionFlow.setVisibility(View.VISIBLE);
					holder.descConsumptionFlow.setVisibility(View.VISIBLE);
				}
			}
			setNetStateIcon(holder, details);
			if(position == getCount()-1){
				holder.bodyBottomLine.setTag("gone");
				holder.bodyBottomLine.setVisibility(View.GONE);
			}else {
				holder.bodyBottomLine.setTag(null);
				if(!isNeedLoad)
					holder.bodyBottomLine.setVisibility(View.VISIBLE);
			}
			
			return convertView;
		}
		
		private String getTimeByCalendar(Calendar calendar){
			int hour = calendar.get(Calendar.HOUR_OF_DAY);
			int minute = calendar.get(Calendar.MINUTE);
			String shour = String.valueOf(hour).length() == 1? "0" + hour : String.valueOf(hour);
			String sminute = String.valueOf(minute).length() == 1 ? "0" + minute : String.valueOf(minute);
			return shour + ":" + sminute;
		}
		
		
		private void setNetStateIcon(ViewHolder holder, AccelDetails details) {
			switch (details.getNetState()) {
			case BOTH:
				holder.netIconWifi.setVisibility(View.VISIBLE);
				holder.netIconUnknow.setVisibility(View.GONE);
				holder.netIconLine.setVisibility(View.VISIBLE);
				holder.netIconFlow.setVisibility(View.VISIBLE);
				break;
			case MOBILE:
				holder.netIconWifi.setVisibility(View.GONE);
				holder.netIconUnknow.setVisibility(View.GONE);
				holder.netIconLine.setVisibility(View.GONE);
				holder.netIconFlow.setVisibility(View.VISIBLE);
				break;
			case WIFI:
				holder.netIconWifi.setVisibility(View.VISIBLE);
				holder.netIconUnknow.setVisibility(View.GONE);
				holder.netIconLine.setVisibility(View.GONE);
				holder.netIconFlow.setVisibility(View.GONE);
				break;
			default:
				holder.netIconWifi.setVisibility(View.GONE);
				holder.netIconUnknow.setVisibility(View.VISIBLE);
				holder.netIconLine.setVisibility(View.GONE);
				holder.netIconFlow.setVisibility(View.GONE);
				break;
			};
		}
	}
	
	private static final class ViewHolder {
		// 线
		public final View headTopLine, headBottomLine;
		public final View bodyTopLine, bodyBottomLine;
		// 年月日 、时分
		public final TextView textDate, textTime;
		// 点
		public final View point;
		// 网络图标组
		public final View groupNet;
		// 详情信息组
		public final View groupDetail;
		// 详情描述信息
		public final View descDelayRange, descAccelTime, descConsumptionFlow;
		public final TextView textDelayProgress, textAccelTime, textConsumptionFlow;
		public final ProgressBar progressDelay;
		public final View headGroup;
		private final View netIconWifi, netIconUnknow,netIconLine,netIconFlow;

		public ViewHolder(View view) {
			headGroup = view.findViewById(R.id.head_group);
			headTopLine = view.findViewById(R.id.head_top_line);
			headBottomLine = view.findViewById(R.id.head_bottom_line);
			bodyTopLine = view.findViewById(R.id.body_top_line);
			bodyBottomLine = view.findViewById(R.id.body_bottom_line);
			textDate = (TextView) view.findViewById(R.id.text_date);
			textTime = (TextView) view.findViewById(R.id.text_time);
			textDelayProgress = (TextView) view
					.findViewById(R.id.text_delay_progress);
			textAccelTime = (TextView) view.findViewById(R.id.text_accel_time);
			textConsumptionFlow = (TextView) view
					.findViewById(R.id.text_consumption_flow);
			point = view.findViewById(R.id.point);
			descDelayRange = view.findViewById(R.id.desc_delay_range);
			descAccelTime = view.findViewById(R.id.desc_accel_time);
			descConsumptionFlow = view.findViewById(R.id.desc_consumption_flow);
			groupNet = view.findViewById(R.id.layout_net_desc);
			groupDetail = view.findViewById(R.id.details_group);
			progressDelay = (ProgressBar) view
					.findViewById(R.id.progress_delay);
			
			netIconWifi = groupNet.findViewById(R.id.speed_details_icon_wifi);
			netIconUnknow = groupNet.findViewById(R.id.speed_details_icon_unknow);
			netIconLine = groupNet.findViewById(R.id.speed_details_icon_line);
			netIconFlow = groupNet.findViewById(R.id.speed_details_icon_flow);
		}
	}
	
	// 加载动画相关 ==================
	private ItemElementManager itemElementManager = new ItemElementManager();
	private boolean isNeedLoad = true;
	private AnimationHandler handler = new AnimationHandler(this);
	public void initListAnimation(){
		itemElementManager.start();
		accelDetailList.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});
	}
	private abstract class Element{
		protected final ViewHolder viewHolder;
		public final int waitTime;
		
		public Element(ViewHolder holder,int waitTime) {
			this.viewHolder = holder;
			this.waitTime = waitTime;
		}
		
		public abstract void draw();
		
		protected void setVisibility(View view) {
			if("gone".equals(view.getTag())){
				view.setVisibility(View.GONE);
			}else{
				view.setVisibility(View.VISIBLE);
			}
		}

		public void reset() {}
	}
	
	private static final class AnimationHandler extends WeakReferenceHandler<ActivityGameAccelDesc>{
		
		public AnimationHandler(ActivityGameAccelDesc ref) {
			super(ref);
		}

		@Override
		protected void handleMessage(ActivityGameAccelDesc ref, Message arg1) {
			boolean next = ref.itemElementManager.next();
			if(!next){
				ref.isNeedLoad = false;
				ref.accelDetailList.setOnTouchListener(null);
			}
		}
	}
	
	private class ItemElementManager{

		Queue<Element> queue = new LinkedList<Element>();
		private Element currentElement;
//		private int currentCount;
		private void load(ViewHolder holder,int waitTime){
			queue.add(new DrawDate(holder, waitTime));
			queue.add(new Second(holder, waitTime));
			queue.add(new DrawDetailGroupTranslation(holder, waitTime));
			queue.add(new DrawDetailGroupCommon(holder, waitTime));
			queue.add(new DrawDetailGroupNormal(holder, waitTime));
			queue.add(new DrawDetailNetGroupFirst(holder, waitTime));
			queue.add(new DrawDetailNetGroupSecond(holder, waitTime));
			queue.add(new DrawDetailGroupThird(holder, waitTime));
			queue.add(new DrawDetailGroupFour(holder, waitTime));
			queue.add(new DrawDetailGroupFive(holder, waitTime));
			queue.add(new DrawDetailGroupSex(holder, waitTime));
			queue.add(new DrawDetailGroupSeven(holder, waitTime));
			queue.add(new DrawBottomLine(holder, waitTime));
		}
		
		public boolean next(){
			currentElement = queue.poll();
			if(currentElement==null){
				return false;
			}else{
				currentElement.draw();
				handler.sendEmptyMessageDelayed(0, currentElement.waitTime);
				return true;
			}
		}
		
		public boolean isRunning(){
			return currentElement != null || !queue.isEmpty();
		}
		
		public void noWaitLoad(ViewHolder holder){
			setVisibility(holder,View.VISIBLE);
		}
		
//		public void redefault(){
//			while(adapter.getCount() > currentCount){
//				ViewHolder holder = adapter.getViewHolder(currentCount);
//				if(holder == null)
//					break;
//				holder.headGroup.setVisibility(View.INVISIBLE);
//				setVisibility(holder, View.INVISIBLE);
//				currentCount++;
//			}
//		}

		private void setVisibility(ViewHolder holder,int visibility) {
			holder.headTopLine.setVisibility(visibility);
			holder.headBottomLine.setVisibility(visibility);
			holder.textDate.setVisibility(visibility);
			holder.bodyTopLine.setVisibility(visibility);
			holder.bodyBottomLine.setVisibility(visibility);
			holder.textTime.setVisibility(visibility);
			holder.point.setVisibility(visibility);
			holder.groupNet.setVisibility(visibility);
			holder.groupDetail.setVisibility(visibility);
			holder.descDelayRange.setVisibility(visibility);
			holder.descAccelTime.setVisibility(visibility);
			holder.descConsumptionFlow.setVisibility(visibility);
			holder.textDelayProgress.setVisibility(visibility);
			holder.textAccelTime.setVisibility(visibility);
			holder.progressDelay.setVisibility(visibility);
			holder.textConsumptionFlow.setVisibility(visibility);
		}
		
		public void stop(){
			handler.removeMessages(0);
			queue.clear();
			if(currentElement!=null){
				currentElement.reset();
			}
		}
		
		public void start(){
			// 填充队列
			int waitTime = 80;
			int count = adapter.getCount();
			int currentCount = 0;
			while(count > currentCount){
				ViewHolder holder = adapter.getViewHolder(currentCount);
				if(holder == null)
					break;
				load(holder, waitTime);
				currentCount++;
			}
			next();
		}
		
	}
	
	private class DrawDate extends Element{

		public DrawDate(ViewHolder holder, int waitTime) {
			super(holder, waitTime);
		}

		@Override
		public void draw() {
			setVisibility(viewHolder.headTopLine);
			setVisibility(viewHolder.textDate);
		}
	}
	
	private class Second extends Element{

		public Second(ViewHolder holder, int waitTime) {
			super(holder, waitTime);
		}

		@Override
		public void draw() {
			setVisibility(viewHolder.headBottomLine);
			setVisibility(viewHolder.bodyTopLine);
			setVisibility(viewHolder.point);
		}
	}
	
	/**
	 * 设置详情组件组低透明显示
	 */
	private class DrawDetailGroupTranslation extends Element{

		public DrawDetailGroupTranslation(ViewHolder holder, int waitTime) {
			super(holder, waitTime);
		}

		@Override
		public void draw() {
			setVisibility(viewHolder.groupDetail);
			viewHolder.groupDetail.setAlpha(0.3f);
		}
		
		@Override
		public void reset() {
			viewHolder.groupDetail.setAlpha(1f);
		}
	}
	
	/**
	 * 设置详情组件组普通透明显示
	 */
	private class DrawDetailGroupCommon extends Element{

		public DrawDetailGroupCommon(ViewHolder holder, int waitTime) {
			super(holder, waitTime);
		}

		@Override
		public void draw() {
			viewHolder.groupDetail.setAlpha(0.6f);
		}
		
		@Override
		public void reset() {
			super.reset();
			viewHolder.groupDetail.setAlpha(1f);
		}
	}
	
	/**
	 * 设置详情组件组正常显示
	 */
	private class DrawDetailGroupNormal extends Element{

		public DrawDetailGroupNormal(ViewHolder holder, int waitTime) {
			super(holder, waitTime);
		}

		@Override
		public void draw() {
			viewHolder.groupDetail.setAlpha(1f);
		}
	}
	
	/**
	 * 设置详情组件内容及网络组件显示
	 */
	private class DrawDetailNetGroupFirst extends Element{

		public DrawDetailNetGroupFirst(ViewHolder holder, int waitTime) {
			super(holder, waitTime);
		}

		@Override
		public void draw() {
			setVisibility(viewHolder.groupNet);
			setVisibility(viewHolder.textTime);
			setVisibility(viewHolder.descDelayRange);
			viewHolder.descDelayRange.setAlpha(0.8f);
			setVisibility(viewHolder.descAccelTime);
			viewHolder.descAccelTime.setAlpha(0.5f);
			setVisibility(viewHolder.descConsumptionFlow);
			viewHolder.descConsumptionFlow.setAlpha(0.3f);
		}
		
		@Override
		public void reset() {
			viewHolder.descDelayRange.setAlpha(1f);
			viewHolder.descAccelTime.setAlpha(1f);
			viewHolder.descConsumptionFlow.setAlpha(1f);
		}
	}
	
	/**
	 * 设置详情组件内容及网络组件显示
	 */
	private class DrawDetailNetGroupSecond extends Element{

		public DrawDetailNetGroupSecond(ViewHolder holder, int waitTime) {
			super(holder, waitTime);
		}

		@Override
		public void draw() {
			viewHolder.descDelayRange.setAlpha(1f);
			viewHolder.descAccelTime.setAlpha(1f);
			viewHolder.descConsumptionFlow.setAlpha(1f);
			viewHolder.progressDelay.setTag(viewHolder.progressDelay.getProgress());
			viewHolder.progressDelay.setProgress(0);
			setVisibility(viewHolder.progressDelay);
		}
		
		@Override
		public void reset() {
			viewHolder.progressDelay.setProgress(Integer.valueOf(viewHolder.progressDelay.getTag().toString()));
		}
	}
	
	/**
	 * 设置详情组件内容显示
	 */
	private class DrawDetailGroupThird extends Element{

		public DrawDetailGroupThird(ViewHolder holder, int waitTime) {
			super(holder, waitTime);
		}

		@Override
		public void draw() {
			viewHolder.progressDelay.setProgress(Integer.valueOf(viewHolder.progressDelay.getTag().toString()));
		}
	}
	
	/**
	 * 设置详情组件内容显示
	 */
	private class DrawDetailGroupFour extends Element{

		public DrawDetailGroupFour(ViewHolder holder, int waitTime) {
			super(holder, waitTime);
		}

		@Override
		public void draw() {
			viewHolder.textDelayProgress.setText(viewHolder.progressDelay.getTag() + "%");
			viewHolder.textDelayProgress.setAlpha(0.5f);
			setVisibility(viewHolder.textDelayProgress);
		}
		
		@Override
		public void reset() {
			viewHolder.textDelayProgress.setAlpha(1f);
		}
	}
	
	/**
	 * 设置详情组件内容显示
	 */
	private class DrawDetailGroupFive extends Element{

		public DrawDetailGroupFive(ViewHolder holder, int waitTime) {
			super(holder, waitTime);
		}

		@Override
		public void draw() {
			viewHolder.textDelayProgress.setAlpha(1f);
		}
	}
	
	/**
	 * 设置详情组件内容显示
	 */
	private class DrawDetailGroupSex extends Element{

		public DrawDetailGroupSex(ViewHolder holder, int waitTime) {
			super(holder, waitTime);
		}

		@Override
		public void draw() {
			setVisibility(viewHolder.textAccelTime);
			viewHolder.textAccelTime.setAlpha(0.5f);
		}
		
		@Override
		public void reset() {
			viewHolder.textAccelTime.setAlpha(1f);
		}
	}
	
	/**
	 * 设置详情组件内容显示
	 */
	private class DrawDetailGroupSeven extends Element{

		public DrawDetailGroupSeven(ViewHolder holder, int waitTime) {
			super(holder, waitTime);
		}

		@Override
		public void draw() {
			viewHolder.textAccelTime.setAlpha(1f);
			setVisibility(viewHolder.textConsumptionFlow);
		}
	}
	
	private class DrawBottomLine extends Element{

		public DrawBottomLine(ViewHolder holder, int waitTime) {
			super(holder, waitTime);
		}

		@Override
		public void draw() {
//			if(viewHolder.bodyBottomLine.getVisibility() == View.INVISIBLE)
//				setVisibility(viewHolder.bodyBottomLine);
			setVisibility(viewHolder.bodyBottomLine);
		}
	}
	
}
