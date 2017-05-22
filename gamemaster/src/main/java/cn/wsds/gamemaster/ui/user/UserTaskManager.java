package cn.wsds.gamemaster.ui.user;

import hr.client.appuser.TaskCenter.AccomplishTasks;
import hr.client.appuser.TaskCenter.AccomplishTasksResponse;
import hr.client.appuser.TaskCenter.TaskBrief;
import hr.client.appuser.TaskCenter.TaskProgressElem;
import hr.client.appuser.TaskCenter.TimeInterval;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.text.TextUtils;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.DataCache;
import cn.wsds.gamemaster.data.SessionInfo;
import cn.wsds.gamemaster.data.UserInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.data.UserSession.UserSessionObserver;
import cn.wsds.gamemaster.event.NewGameInstalledEvent;
import cn.wsds.gamemaster.net.http.DefaultNoUIResponseHandler;
import cn.wsds.gamemaster.net.http.Response;
import cn.wsds.gamemaster.net.http.ResponseHandler;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.ui.UIUtils;

import com.google.protobuf.InvalidProtocolBufferException;
import com.subao.common.SuBaoObservable;

/**
 * 用户中心的任务管理
 */
public class UserTaskManager {
	
	public static final String KEY_TASK_IDENTIFIER = "key_task_identifier";
	public static final String KEY_GAME_NAME = "key_game_name";
	public static final String KEY_NAME_DISPLAY = "key_name_display";
	
	/**
	 * 用户未登录或用户登录已过期,提示重新登录文本
	 */
	public static final String RELOGIN_MESS = "获得任务积分需要您先登录哦~";
	
	private static final UserTaskManager instance = new UserTaskManager();
	/**
	 * 任务集合
	 */
	private final List<TaskRecord> records = new ArrayList<TaskRecord>();
	/**
	 * 累计任务统计上报的集合
	 */
	private final List<AccelTimeAccumulativeWorker> workers = new ArrayList<AccelTimeAccumulativeWorker>();
	
	private UserSessionObserver userSessionObserver = new UserSessionObserver() {
		public void onSessionInfoChanged(SessionInfo info) {
			asyncDownloadTaskList();
		}
	};
	
	private final TaskListObservable taskListObservable = new TaskListObservable();

	private UserTaskManager() {
		if(UserSession.isLogined()){
			asyncDownloadTaskList();
		}
	}

	public void init(){
		UserSession.getInstance().registerUserSessionObserver(userSessionObserver);
	}
	
	/**
	 * 任务列表发生变化将任务数据格式为TaskRecord 并放入 集合 {@link #records} 中
	 * @param taskBriefs
	 */
	public void setTaskBriefs(List<TaskBrief> taskBriefs) {
		records.clear();
		if(taskBriefs!=null){
			for (TaskBrief taskBrief : taskBriefs) {
				TaskRecord taskRecord = new TaskRecord(taskBrief);
				if(!taskRecord.isDirstyRecord()){
					records.add(taskRecord);
				}
			}
		}
		DataCache.getTaskRecordsDataCache().setData(records);
		taskListObservable.onTaskListChanged();
		startAccelTimeAccumulativeWork();
	}
	
	public static UserTaskManager getInstance() {
		return instance;
	}

	/**
	 * 用户信息及会话信息被观察者
	 */
	private static final class TaskListObservable extends SuBaoObservable<TaskListObserver> {
		
		public void onTaskListChanged() {
			List<TaskListObserver> list = this.cloneAllObservers();
			if (list != null) {
				for (TaskListObserver taskListObserver : list) {
					taskListObserver.onTaskListChanged();
				}
			}
		}
	}	
	
	/**
	 * 任务列表观察者
	 */
	public static abstract class TaskListObserver {
		
		/**
		 * 任务列表发生变化
		 */
		public abstract void onTaskListChanged();
		
	}
	
	/**
	 * 注册任务列表观察者
	 * @param observer 
	 */
	public void registerObserver(TaskListObserver observer){
		this.taskListObservable.registerObserver(observer);
	}
	
	public void unregisterObserver(TaskListObserver observer){
		this.taskListObservable.unregisterObserver(observer);
	}
	
	/**
	 * 通过任务标识符获取一组任务
	 * @param identifier {@link UserTaskManager.TaskIdentifier} 任务标识符
	 * @return
	 */
	public List<TaskRecord> getTaskRecord(TaskIdentifier identifier) {
		List<TaskRecord> temp = new ArrayList<TaskRecord>(records.size());
		for (TaskRecord taskRecord : records) {
			if(taskRecord.actionType == identifier){
				temp.add(taskRecord);
			}
		}
		return temp;
	}
	
	/**
	 * 取所有任务
	 * @return
	 */
	public List<TaskRecord> getAllTaskRecords(){
		List<TaskRecord> temp = new ArrayList<TaskRecord>(records.size());
		for (TaskRecord r : records) {
			temp.add(r);
		}
		return temp;
	}
	
	/**
	 * 同步任务列表
	 */
	public void asyncDownloadTaskList(){
		ActivityTaskCenter.TaskRefreshRequestor requestor = new ActivityTaskCenter.TaskRefreshRequestor();
		requestor.request();
	}
	
	/**
	 * 任务记录 
	 */
	public static final class TaskRecord implements Comparable<TaskRecord>{
		/**
		 * 服务器任务源数据
		 * 可能为空，在 adapter中显示时会生成标签记录，源数据即为空，但仅在adapter队列，不影响  <b> UserTaskManager.records </b>
		 */
		public final TaskBrief taskBrief;
		/**
		 * 当前任务标识
		 */
		public final TaskIdentifier actionType;
		/**
		 * 当前任务分组,主要用于界面显示
		 */
		public final Group group;
		
		private static final int DAY_SECOND = 60 * 60 * 24;
		private static final int WEEK_SECOND = DAY_SECOND * 7;
		/**
		 * {@link #taskBrief} 源数据中taskProgressItem 不可修改而有时就需要修改taskProgressItem所以只好声明
		 */
		private List<TaskProgressElem> taskProgressElems;
		
		private TaskRecord(TaskBrief taskBreif) {
			this(taskBreif, getTaskIdentifier(taskBreif.getAppClientParas()), getGroup(taskBreif));
		}
		
		public TaskRecord(Group group) {
			this(null, TaskIdentifier.unkown,  group);
		}

		private TaskRecord(TaskBrief taskBreif, TaskIdentifier actionType, Group group) {
			this.taskBrief = taskBreif;
			if(taskBreif!=null){
				this.taskProgressElems = taskBreif.getTaskProgressList();
			}
			this.actionType = actionType;
			this.group = group;
		}

		private static Group getGroup(TaskBrief taskBreif) {
			if(taskBreif.getIsActivityTask()){//活动任务即为特殊任务
				return Group.other;
			}
			//根据“检查点的有效间隔时间”来分组，将有效时间统一转化为 秒，然后分为 每日、每周、或特殊任务
			TimeInterval checkInterval = taskBreif.getCheckInterval();
			int interval;
			String intervalUnit = checkInterval.getIntervalUnit();
			if("second".equals(intervalUnit)){
				interval = checkInterval.getInterval();
			}else if("week".equals(intervalUnit)){
				interval = checkInterval.getInterval() * WEEK_SECOND;
			}else if("day".equals(intervalUnit)){
				interval = checkInterval.getInterval() * DAY_SECOND;
			}else{
				interval = 0;
			}
			if(interval == DAY_SECOND){
				return Group.evetyDay;
			}else if(interval == WEEK_SECOND){
				return Group.everyWeek;
			}else{
				return Group.other;
			}
		}

		private static TaskIdentifier getTaskIdentifier(Map<String, String> appClientParas){
			if(appClientParas==null){
				return TaskIdentifier.unkown;
			}
			String name = appClientParas.get(KEY_TASK_IDENTIFIER);
			return transStringToTaskIdentifier(name);
		}
		
		private static TaskIdentifier transStringToTaskIdentifier(String identifierName){
			if(TextUtils.isEmpty(identifierName)){
				return TaskIdentifier.unkown;
			}
			if ("sign in".equals(identifierName)) {
				return TaskIdentifier.signIn;
			} else if ("share".equals(identifierName)) {
				return TaskIdentifier.share;
			} else if ("accel".equals(identifierName)) {
				return TaskIdentifier.accel;
			} else if ("start game inside".equals(identifierName)) {
				return TaskIdentifier.startGameInside;
			} else if ("download and install".equals(identifierName)) {
				return TaskIdentifier.downloadAndInstallGame;
			} else if ("add appoint game".equals(identifierName)) {
				return TaskIdentifier.addAppointGame;
			} else if ("register or bind".equals(identifierName)) {
				return TaskIdentifier.registerOrBind;
			} else {
				return TaskIdentifier.unkown;
			}
		}

		/**
		 * 任务分组
		 */
		public enum Group {
			evetyDay(1,"每日任务"),
			everyWeek(2,"每周任务"),
			other(3,"特殊任务");
			public final int orderNum;
			public final String name;
			private Group(int orderNum, String name) {
				this.orderNum = orderNum;
				this.name = name;
			}
		}
		
		@Override
		public int compareTo(TaskRecord another) {
			if(group.orderNum == another.group.orderNum){//同组
				if(taskBrief == null){
					return -1;
				}
				return taskBrief.getTaskNum() - another.taskBrief.getTaskNum();
			}
			return group.orderNum - another.group.orderNum;
		}

		/**
		 * 更新任务进度
		 * @param progressElems 任务进度
		 * @return
		 */
		public boolean updateProgressItem(List<TaskProgressElem> progressElems) {
			if(this.taskProgressElems!=null && progressElems != null && this.taskProgressElems.size() == progressElems.size()){
				return false;
			}
			this.taskProgressElems = progressElems;
			UserTaskManager.instance.taskListObservable.onTaskListChanged();
			return true;
		}
		
		public List<TaskProgressElem> getTaskProgressElems() {
			return taskProgressElems;
		}
		
		/**
		 * 任务列表是否为空
		 * @return
		 */
		public boolean isTaskProgressElemsEmpty() {
			return taskProgressElems == null || taskProgressElems.isEmpty();
		}
		
		/**
		 * 是否是一条无效的记录
		 * @return
		 */
		public boolean isDirstyRecord(){
			if(actionType == null || taskBrief == null){
				return true;
			}
			if(TaskIdentifier.registerOrBind == actionType){//兼容服务器对“注册绑定任务”无法处理通过用户信息判断
				UserInfo userInfo = UserSession.getInstance().getUserInfo();
				if(userInfo==null || TextUtils.isEmpty(userInfo.getPhoneNumber())){
					return false;
				}else{
					return true;
				}
			}
			return "1".equals(taskBrief.getTaskType()) && !isTaskProgressElemsEmpty();
		}
		
	}
	
	
	/**
	 * 应用标识符
	 */
	public enum TaskIdentifier {
		signIn,
		share,
		accel,
		startGameInside,
		downloadAndInstallGame,
		addAppointGame,
		registerOrBind,
		unkown;
	}
	
	

	/**
	 * 累计加速任务进度统计上报
	 */
	private static class AccelTimeAccumulativeWorker {

		/**
		 * 最近的累计加速上传前统计的加速值
		 */
		private int lastAccelTimeAmountWhenReportRaised;
		/**
		/**
		 * 上报累计加速最小间隔
		 */
		private static final int MIN_REPORT_ACCEL_TIME_INTERVAL_SECONDS = 60 * 10;
		
		private TaskRecord taskRecord;
		
		private cn.wsds.gamemaster.app.GameManager.Observer accelTimeChangeListener = new cn.wsds.gamemaster.app.GameManager.Observer() {

			@Override
			public void onAccelTimeChanged(int seconds) {
				int accelTime = seconds - lastAccelTimeAmountWhenReportRaised;
				if(accelTime >= MIN_REPORT_ACCEL_TIME_INTERVAL_SECONDS){
					doReportAccel(accelTime);
				}
			}

			@Override
			public void onGameListUpdate() {}

            @Override
            public void onDoubleAccelTimeChanged(String packageName, int seconds) {

            }

        };
		
		private AccelTimeAccumulativeWorker(TaskRecord taskRecord) {
			this.lastAccelTimeAmountWhenReportRaised = GameManager.getInstance().getAccelTimeSecondsAmount();
			this.taskRecord = taskRecord;
		}

		private final class ReportAccelTimeChangeResponseHandler extends ReportTaskResponseHandler {
			
			private final int accelTime;
			
			private ReportAccelTimeChangeResponseHandler(int accelTime,TaskRecord taskRecord) {
				super(taskRecord);
				this.accelTime = accelTime;
			}
			
			@Override
			protected void onNotAccepted(Response response) {
				if(HttpURLConnection.HTTP_CONFLICT == response.code){
					onReportSuccess(null);
				}else{
					onFailure();
				}
			}
			
			@Override
			protected void onReportSuccess(AccomplishTasksResponse response) {
				lastAccelTimeAmountWhenReportRaised = lastAccelTimeAmountWhenReportRaised + accelTime;
			}
			
		}

		/**
		 * 上报加速时间
		 * @param accelTime 上次加速时间上报后累计的加速时间
		 * @return 是否成功的去上报
		 */
		protected final boolean doReportAccel(int accelTime){
			ResponseHandler reportResponseHandler = new ReportAccelTimeChangeResponseHandler(accelTime,taskRecord);
			AccomplishTasks.Builder builder = AccomplishTasks.newBuilder();
        	builder.setAccumulativeValue(String.valueOf(accelTime));
			return HttpApiService.requestTaskFinished(getTaskId(),builder.build().toByteArray() , reportResponseHandler);
		}
		
		private String getTaskId(){
			return taskRecord.taskBrief.getTaskId();
		}
		
		public void startWork(){
			GameManager.getInstance().registerObserver(accelTimeChangeListener);
		}
		
		private void stopWork() {
			GameManager.getInstance().unregisterObserver(accelTimeChangeListener);
		}
	}
	
	/**
	 * 启动累计加速统计上报任务
	 */
	private static void startAccelTimeAccumulativeWork() {
		List<AccelTimeAccumulativeWorker> workers = getInstance().workers;
		for (AccelTimeAccumulativeWorker worker : workers) {
			worker.stopWork();
		}
		getInstance().workers.clear();
		
		List<TaskRecord> records = getInstance().getTaskRecord(TaskIdentifier.accel);
		if(records.isEmpty()){
			return;
		}
		for (TaskRecord taskRecord : records) {
			AccelTimeAccumulativeWorker worker = new AccelTimeAccumulativeWorker(taskRecord);
			getInstance().workers.add(worker);
			worker.startWork();
		}
	}
	
	private static abstract class ReportTaskResponseHandler extends DefaultNoUIResponseHandler {
		
		private final TaskRecord taskRecord;

		public ReportTaskResponseHandler(TaskRecord taskRecord) {
			this(null,taskRecord);
		}

		public ReportTaskResponseHandler(OnHttpUnauthorizedCallBack onHttpUnauthorizedCallBack, TaskRecord taskRecord){
			super(onHttpUnauthorizedCallBack);
			this.taskRecord = taskRecord;
		}
		
		@Override
		protected void onSuccess(Response response) {
			if(HttpURLConnection.HTTP_ACCEPTED == response.code){
				parseFrom(response.body);
			}else{
				onNotAccepted(response);
			}
		}

		protected void onNotAccepted(Response response) {}

		private final void parseFrom(byte[] body) {
			try {
				AccomplishTasksResponse response = AccomplishTasksResponse.parseFrom(body);
				if(0 == response.getResultCode()){
					onReportSuccess(response);
					UserSession.getInstance().updateSorce(response.getTotalPoints());
					taskRecord.updateProgressItem(response.getTaskProgressList());
				}else{
					onReportFail(response);
				}
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
			onReportFail(null);
		}

		protected void onReportFail(AccomplishTasksResponse response) {}

		protected abstract void onReportSuccess(AccomplishTasksResponse response);
		
		@Override
		public final void onNetworkUnavailable() {
			showFailToast();
		}

		@Override
		protected final void onFailure() {
			showFailToast();
		}
		
		private void showFailToast() {
			UIUtils.showToast("网络故障，获取失败");
		}
	}
	
	private static final class ReportStartGameInsideResponseHandler extends ReportTaskResponseHandler {

		public ReportStartGameInsideResponseHandler(TaskRecord taskRecord) {
			super(taskRecord);
		}

		@Override
		protected void onReportSuccess(AccomplishTasksResponse response) {
			UIUtils.showToast("成功开启加速，积分+"+response.getAcquiredPoints());
		}
	}
	
	
	public static final class ReportShareResponseHandler extends ReportTaskResponseHandler {
		
		public ReportShareResponseHandler(Activity activity,TaskRecord taskRecord) {
			super(new ReLoginOnHttpUnauthorizedCallBack(activity,RELOGIN_MESS),taskRecord);
		}

		@Override
		protected void onReportSuccess(AccomplishTasksResponse response) {
			UIUtils.showToast("分享成功，积分+"+response.getAcquiredPoints());
		}
		
		@Override
		protected void onNotAccepted(Response response) {
			if(HttpURLConnection.HTTP_CONFLICT == response.code){
				 UIUtils.showToast(R.string.thank_your_share);
			}
		}
		
	}
	
	/**
	 * 上报启动游戏
	 * @param packageName 游戏包名
	 * @param gameLabel 游戏名称
	 */
	public static void reportStartGameInside(String packageName,String gameLabel){
		// 上报启动游戏
		doReuqestActionTaskFinish(TaskIdentifier.startGameInside,new ResponseHandlerCreater() {
			
			@Override
			public ReportTaskResponseHandler create(TaskRecord taskRecord) {
				return new ReportStartGameInsideResponseHandler(taskRecord);
			}
		});
		
		// 上报添加指定游戏成功
		if(!TextUtils.isEmpty(gameLabel) && NewGameInstalledEvent.isValidNewGame(gameLabel)){
			List<TaskRecord> records = getInstance().getTaskRecord(TaskIdentifier.addAppointGame);
			if(records.isEmpty()){
				return;
			}
			for (TaskRecord taskRecord : records) {
				ResponseHandler responseHandler = new ReportTaskResponseHandler(taskRecord){
					
					@Override
					protected void onReportSuccess(AccomplishTasksResponse response) {}
					
				};
				TaskBrief taskBrief = taskRecord.taskBrief;
				Map<String, String> appClientParas = taskBrief.getAppClientParas();
				if(appClientParas == null){
					continue;
				}
				String gameName = appClientParas.get(KEY_GAME_NAME);
				if(TextUtils.isEmpty(gameName)){
					continue;
				}
				if(gameLabel.contains(gameName)){
					HttpApiService.requestTaskFinished(taskBrief.getTaskId(),null,responseHandler);
				}
			}	
		}
	}
	
	/**
	 * @param identifier 任务标识
	 */
	public static void doReuqestActionTaskFinish(TaskIdentifier identifier,ResponseHandlerCreater handlerCreater) {
		doReuqestActionTaskFinish(identifier, null,handlerCreater);
	}
	
	
	public static void doReuqestActionTaskFinish(TaskIdentifier identifier,String checkPointKey, ResponseHandlerCreater handlerCreater) {
		List<TaskRecord> records = getInstance().getTaskRecord(identifier);
		if(records.isEmpty()){
			return;
		}
		for (TaskRecord taskRecord : records) {
			TaskBrief taskBrief = taskRecord.taskBrief;
			byte[] postData = null;
			if(!TextUtils.isEmpty(checkPointKey)){
				AccomplishTasks.Builder builder = AccomplishTasks.newBuilder();
				builder.setTaskCheckValue(checkPointKey);
				postData = builder.build().toByteArray();
			}
			
			ResponseHandler responseHandler = null;
			if(handlerCreater!=null){
				responseHandler = handlerCreater.create(taskRecord);
			}
			HttpApiService.requestTaskFinished(taskBrief.getTaskId(),postData,responseHandler);
		}
	}
	
	public interface ResponseHandlerCreater {
		public ReportTaskResponseHandler create(TaskRecord taskRecord);
	}
	
//	测试假数据
//
//	private static final class LoadTestData {
//		
//		private static TaskBrief createAccelEveryDay(){
//			String taskId = "bca8ba5d-6862-4df3-ada4-55d60a058315";
//			String taskName = "累计加速2小时";
//		    String taskDescription = "every day";
//			
//		    Map<String, String> appParas = new HashMap<String,String>();
//		    appParas.put(KEY_TASK_IDENTIFIER,"accel every Day");
//		    
//		    TaskBrief.Builder builder = TaskBrief.newBuilder();
//		    builder.setTaskId(taskId);
//		    builder.setTaskNum(3);
//		    builder.setTaskName(taskName);
//		    builder.setTaskDescription(taskDescription);
//		    builder.setIsActivityTask(false);
//		    builder.setCheckType(1);
//		    builder.setAccumulativeUnit(60 * 60 * 2);
//		    builder.putAllAppClientParas(appParas);
//		    TimeInterval.Builder timeBuilder = TimeInterval.newBuilder();
//		    timeBuilder.setInterval(60 * 60 * 24);
//		    timeBuilder.setIntervalUnit("second");
//		    builder.setPeriod(timeBuilder.build());
//		    
//		    Map<String,String> checkPoints = new HashMap<String,String>();
//		    checkPoints.put(KEY_POINTS, "4");
//		    builder.putAllCheckPoints(checkPoints);
//		    
//		    TaskProgressElem.Builder taskProgressBuilder = TaskProgressElem.newBuilder();
//		    taskProgressBuilder.setAccumulativeValue(60 * 60 * 2);
//		    builder.addTaskProgress(taskProgressBuilder.build());
//		    return builder.build();
//		}
//		
//		private static TaskBrief createAccelEveryWeek(){
//			String taskId = "bca8ba5d-6862-4df3-ada4-55d60a058315";
//			String taskName = "本周累计加速14小时";
//		    
//		    TaskBrief.Builder builder = TaskBrief.newBuilder();
//		    builder.setTaskId(taskId);
//		    builder.setTaskNum(5);
//		    builder.setTaskName(taskName);
//		    builder.setIsActivityTask(false);
//		    builder.setCheckType(1);
//		    builder.setAccumulativeUnit(60 * 60 * 14);
//		    
//		    Map<String, String> appParas = new HashMap<String,String>();
//		    appParas.put(KEY_TASK_IDENTIFIER,"accel every week");
//		    builder.putAllAppClientParas(appParas);
//		    
//		    TimeInterval.Builder timeBuilder = TimeInterval.newBuilder();
//		    timeBuilder.setInterval(60 * 60 * 24 * 7);
//		    timeBuilder.setIntervalUnit("second");
//		    builder.setPeriod(timeBuilder.build());
//		    
//		    Map<String,String> checkPoints = new HashMap<String,String>();
//		    checkPoints.put(KEY_POINTS, "10");
//		    TaskProgressElem.Builder taskProgressBuilder = TaskProgressElem.newBuilder();
//		    taskProgressBuilder.setAccumulativeValue(60 * 30 * 21);
//		    builder.addTaskProgress(taskProgressBuilder.build());
//		    builder.putAllCheckPoints(checkPoints);
//		    return builder.build();
//		}
//		
//		private static TaskBrief createStartGameInsideTask(){
//			String taskId = "bca8ba5d-6862-4df3-ada4-55d60a058315";
//			String taskName = "从客户端开启加速游戏1次";
//		    
//		    TaskBrief.Builder builder = TaskBrief.newBuilder();
//		    builder.setTaskId(taskId);
//		    builder.setTaskNum(2);
//		    builder.setTaskName(taskName);
//		    builder.setIsActivityTask(false);
//		    builder.setCheckType(0);
//		    
//		    Map<String, String> appParas = new HashMap<String,String>();
//		    appParas.put(KEY_TASK_IDENTIFIER,"start game inside");
//		    builder.putAllAppClientParas(appParas);
//		    
//		    TimeInterval.Builder timeBuilder = TimeInterval.newBuilder();
//		    timeBuilder.setInterval(60 * 60 * 24);
//		    timeBuilder.setIntervalUnit("second");
//		    builder.setPeriod(timeBuilder.build());
//		    
//		    Map<String,String> checkPoints = new HashMap<String,String>();
//		    checkPoints.put(KEY_POINTS, "5");
//		    builder.putAllCheckPoints(checkPoints);
//		    
//		    return builder.build();
//		}
//
//		private static TaskBrief createSignTask(){
//			String taskId = "bca8ba5d-6862-4df3-ada4-55d60a058315";
//			String taskName = "每日签到";
//		    
//		    TaskBrief.Builder builder = TaskBrief.newBuilder();
//		    
//		    builder.setTaskId(taskId);
//		    builder.setTaskNum(1);
//		    builder.setTaskName(taskName);
//		    builder.setIsActivityTask(false);
//		    builder.setCheckType(2);
//		    
//		    Map<String, String> appParas = new HashMap<String,String>();
//		    appParas.put(KEY_TASK_IDENTIFIER,"sign in");
//		    builder.putAllAppClientParas(appParas);
//		    
//		    TimeInterval.Builder timeBuilder = TimeInterval.newBuilder();
//		    timeBuilder.setInterval(60 * 60 * 24);
//		    timeBuilder.setIntervalUnit("second");
//		    builder.setPeriod(timeBuilder.build());
//		    
//		    Map<String,String> checkPoints = new HashMap<String,String>();
//		    checkPoints.put("Mon","11");
//		    checkPoints.put("Tus","10");
//		    checkPoints.put("Wed","9");
//		    checkPoints.put("Thu","8");
//		    checkPoints.put("Fri","7");
//		    checkPoints.put("Sat","6");
//		    checkPoints.put("Sun","5");
//		    builder.putAllCheckPoints(checkPoints);
//		    return builder.build();
//		}
//		
//		private static TaskBrief createShareTask(){
//			String taskId = "50a87279-79eb-4f31-ade8-9d8d8cf81f3a";
//			String taskName = "本月首次分享迅游手游加速器";
//		    
//		    
//		    TaskBrief.Builder builder = TaskBrief.newBuilder();
//		    Map<String, String> appParas = new HashMap<String,String>();
//			appParas.put(KEY_TASK_IDENTIFIER,"share");
//			builder.putAllAppClientParas(appParas);
//		    
//			builder.setTaskId(taskId);
//			builder.setTaskNum(6);
//			builder.setTaskName(taskName);
//			builder.setIsActivityTask(false);
//			builder.setCheckType(3);
//			
//			TimeInterval.Builder timeBuilder = TimeInterval.newBuilder();
//		    timeBuilder.setInterval(60 * 60 * 24 * 30);
//		    timeBuilder.setIntervalUnit("second");
//		    builder.setPeriod(timeBuilder.build());
//		    
//		    Map<String,String> checkPoints = new HashMap<String,String>();
//		    checkPoints.put("weixinSharing", "30");
//		    checkPoints.put("normalSharing", "10");
//		    builder.putAllCheckPoints(checkPoints);
//		    
//		    
//		    return builder.build();
//		}
//		
//		private static TaskBrief createAddAppointGame(){
//			String taskId = "50a87279-79eb-4f31-ade8-9d8d8cf81f3a";
//			String taskName = "添加指定游戏";
//		    
//		    TaskBrief.Builder builder = TaskBrief.newBuilder();
//		    Map<String, String> appParas = new HashMap<String,String>();
//		    appParas.put(KEY_TASK_IDENTIFIER,"add appoint game");
//			builder.putAllAppClientParas(appParas);
//			builder.setActivityTaskUrl("http://game.wsds.cn/");
//		    
//			builder.setTaskId(taskId);
//			builder.setTaskNum(8);
//			builder.setTaskType("1");
//			builder.setTaskName(taskName);
//			builder.setIsActivityTask(true);
//			builder.setCheckType(0);
//			    
//			TimeInterval.Builder timeBuilder = TimeInterval.newBuilder();
//		    timeBuilder.setInterval(60 * 60 * 24 * 30);
//		    timeBuilder.setIntervalUnit("second");
//		    builder.setPeriod(timeBuilder.build());
//		    
//		    builder.setPoints(20);
//		    
//		    return builder.build();
//		}
//		
//		private static TaskBrief createRegisterOrBind(){
//			String taskId = "50a87279-79eb-4f31-ade8-9d8d8cf81f3a";
//			String taskName = "注册绑定";
//		    
//		    TaskBrief.Builder builder = TaskBrief.newBuilder();
//		    
//		    Map<String, String> appParas = new HashMap<String,String>();
//			appParas.put(KEY_TASK_IDENTIFIER,"register or bind");
//			builder.putAllAppClientParas(appParas);
//		    
//			builder.setTaskId(taskId);
//			builder.setTaskNum(9);
//			builder.setTaskType("1");
//			builder.setTaskName(taskName);
//			builder.setIsActivityTask(true);
//			builder.setCheckType(0);
//		    builder.setPoints(5);
//		    
//		    TaskProgressElem.Builder taskProgressBuilder = TaskProgressElem.newBuilder();
//		    builder.addTaskProgress(taskProgressBuilder);
//		    
//		    return builder.build();
//		}
//		
//		
//		public static List<TaskBrief> createTestData(){
//			List<TaskBrief> datas = new ArrayList<TaskBrief>();
//			datas.add(createShareTask());
//			datas.add(createSignTask());
//			datas.add(createAccelEveryDay());
//			datas.add(createAccelEveryWeek());
//			datas.add(createStartGameInsideTask());
//			datas.add(createAddAppointGame());
//			datas.add(createRegisterOrBind());
//			return datas;
//		}
//	}

	
	
}
 