package cn.wsds.gamemaster.dialog;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.protobuf.InvalidProtocolBufferException;
import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.utils.CalendarUtils;
import com.subao.utils.FileUtils;

import android.app.Activity;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonWriter;
import cn.wsds.gamemaster.Misc;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.net.http.Response;
import cn.wsds.gamemaster.net.http.ResponseHandler;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.ui.user.UserTaskManager;
import cn.wsds.gamemaster.ui.user.UserTaskManager.TaskIdentifier;
import cn.wsds.gamemaster.ui.user.UserTaskManager.TaskListObserver;
import cn.wsds.gamemaster.ui.user.UserTaskManager.TaskRecord;
import hr.client.appuser.TaskCenter;

/**
 * 用户签到功能 Created by hujd on 15-12-22.
 */
public class UserSign {

	private static final String FILENAME = "user_sign_timestamp";
    private static final long TIME_ZONE_8_SECOND = 8 * 3600;
    private static final long HOUR_SECOND_24 = 24 * 3600;

    private final LastSign signInfoList = new LastSign();

	private AccomplishTasksResponse accTasksResponse;
	
	
	/**
	 * 最近一次从服务器拿到的用户签到历史。
	 */
	private UserTaskHistory userSignHistory;

	public enum EWeek {
		MONDAY(0),
		TUESDAY(1),
		WEDNESDAY(2),
		THURSDAY(3),
		FRIDAY(4),
		SATURDAY(5),
		SUNDAY(6);

		public static final int WEEK_COUNT = 7;

        public static final String TEXT[] = new String[]{
                "星期一",
                "星期二",
                "星期三",
                "星期四",
                "星期五",
                "星期六",
                "星期日",
        };

		public final int index;

		private EWeek(int index) {
			this.index = index;
		}
	}

	public interface IResultCallback {
		void onFinish(boolean isSuccess);
	}
	
	/** 向服务器请求签到历史的回调接口 */
	public interface RequestUserTaskHistoryCallback {
		/**
		 * 当服务器应答时被调用
		 * @param history {@link UserSign.UserTaskHistory} 服务器返回的签到历史列表。或null表示请求失败
		 */
		void onComplete(UserTaskHistory history);
	}

	private UserSign() {
		getSiginScore(true);
	}

	private static final UserSign instance = new UserSign();

	public static UserSign getInstance() {
		return instance;
	}
	
	/** 今日可签到吗？ */
    public static boolean canSignToday() {
		UserSign.UserTaskHistory tasksProgResp = UserSign.getInstance().getUserSignHistory();
		if (tasksProgResp == null || tasksProgResp.isEmpty()) {
			return true;
		}
		UserSign.UserTaskHistoryItem elem = tasksProgResp.getLastItem();
		return !elem.wasExecuted();
    }

	/**
	 * 获取最近一次从服务器拿到的“签到历史列表”，（可能为null）
	 */
	public UserTaskHistory getUserSignHistory() {
		return userSignHistory;
	}

	/**
	 * 获取签到结果
	 * 
	 * @return 签到结果
	 */
	public AccomplishTasksResponse getAccTasksResponse() {
		return accTasksResponse;
	}

	private final int[] points = {
		11,
		10,
		9,
		8,
		7,
		6,
		5
	};

	private static void initPoints(Map<String, String> paras, int[] points) {
		for (Map.Entry<String, String> entry : paras.entrySet()) {
			String strPoint = entry.getValue();
			if (TextUtils.isEmpty(strPoint)) {
				continue;
			}
			String key = entry.getKey();
			int index;
			if ("Mon".equals(key)) {
				index = EWeek.MONDAY.index;
			} else if ("Thu".equals(key)) {
				index = EWeek.THURSDAY.index;
			} else if ("Tus".equals(key)) {
				index = EWeek.TUESDAY.index;
			} else if ("Wed".equals(key)) {
				index = EWeek.WEDNESDAY.index;
			} else if ("Fri".equals(key)) {
				index = EWeek.FRIDAY.index;
			} else if ("Sat".equals(key)) {
				index = EWeek.SATURDAY.index;
			} else if ("Sun".equals(key)) {
				index = EWeek.SUNDAY.index;
			} else {
				continue;
			}
			try {
				points[index] = Integer.parseInt(strPoint);
			} catch (NumberFormatException e) {
				return;
			}
		}
	}

	private void getSiginScore(boolean isUpdateTasks) {
		List<TaskRecord> taskRecords = UserTaskManager.getInstance().getTaskRecord(TaskIdentifier.signIn);
		if(taskRecords == null || taskRecords.isEmpty()){
			if(isUpdateTasks){
				asyncDownloadTaskList(getSingScoreTaskListObserver());
			}
			return;
		}
		TaskRecord taskRecord = taskRecords.get(0);
        if (taskRecord == null) {
        	if(isUpdateTasks){
				asyncDownloadTaskList(getSingScoreTaskListObserver());
			}
        	return;
        }
		Map<String, String> paras = taskRecord.taskBrief.getCheckPoints();
		initPoints(paras, this.points);
	}

	/**
	 * 所有用户的最后一次签到时间
	 */
	private static class LastSign {

		private static final String NAME_USER_ID = "user_id";
		private static final String NAME_TIME = "time";

		private final HashMap<String, Long> map = new HashMap<String, Long>();

		public LastSign() {
			loadFromFile();
		}

		private void loadFromFile() {
			File file = FileUtils.getDataFile(FILENAME);
			byte[] data = FileUtils.read(file);
			if (data == null) {
				Logger.d(LogTag.USER, "Load Last Sign: null");
				return;
			}
			String content = new String(data);
			Logger.d(LogTag.USER, "Load Last Sign: " + content);
			JsonReader reader = null;
			try {
				reader = new JsonReader(new StringReader(content));
				reader.beginArray();
				while (reader.hasNext()) {
					String userId = null;
					long timestamp = 0;
					reader.beginObject();
					while (reader.hasNext()) {
						String name = reader.nextName();
						if (NAME_USER_ID.equals(name)) {
							userId = reader.nextString();
						} else if (NAME_TIME.equals(name)) {
							timestamp = reader.nextLong();
						} else {
							reader.skipValue();
						}
					}
					reader.endObject();
					if (timestamp > 0 && !TextUtils.isEmpty(userId)) {
						map.put(userId, timestamp);
					}
				}
				reader.endArray();
			} catch (IOException e) {
				file.delete();
			} catch (RuntimeException e) {
				file.delete();
			} finally {
				com.subao.utils.Misc.safeClose(reader);
			}
		}

		/**
		 * 返回指定用户最近一次签到的时刻（UTC毫秒数）
		 * 
		 * @return 用户最近一次签到的时刻（UTC毫秒数），非正值表示未取到
		 */
		public long getLastSignTime(String userId) {
			if (userId == null) {
				return 0;
			}
			Long result = map.get(userId);
			return result == null ? 0 : result;
		}

		/**
		 * 设置指定用户的“最近一次签到的时刻”（UTC毫秒数）
		 */
		public void putLastSignTime(String userId, long timestamp) {
			if (!TextUtils.isEmpty(userId)) {
				if (Logger.isLoggableDebug(LogTag.USER)) {
					Logger.d(LogTag.USER, String.format("Update Last sign time, UserId=[%s], timestamp=%d %s", userId, timestamp,
						Misc.formatCalendar(CalendarUtils.calendarLocal_FromMilliseconds(timestamp))));
				}
				if (timestamp > 0) {
					Long prevValue = map.put(userId, timestamp);
					if (prevValue == null || prevValue != timestamp) {
						save();
					}
				} else {
					if (map.remove(userId) != null) {
						save();
					}
				}
			}
		}

		private void save() {
			File file = FileUtils.getDataFile(FILENAME);
			if (map.isEmpty()) {
				file.delete();
				return;
			}
			//
			StringWriter sw = new StringWriter(map.size() * 128);
			JsonWriter writer = null;
			try {
				writer = new JsonWriter(sw);
				writer.beginArray();
				for (Entry<String, Long> entry : map.entrySet()) {
					writer.beginObject();
					writer.name(NAME_USER_ID).value(entry.getKey());
					writer.name(NAME_TIME).value(entry.getValue());
					writer.endObject();
				}
				writer.endArray();
			} catch (IOException e) {

			} finally {
				com.subao.utils.Misc.safeClose(writer);
			}
			//
			String output_json_content = sw.toString();
			if (Logger.isLoggableDebug(LogTag.USER)) {
				Logger.d(LogTag.USER, "Save Last Sign: " + output_json_content);
			}
			FileUtils.write(file, output_json_content.getBytes());
		}
	}

	/**
	 * 取北京时间的当前时刻，将其星期几转换为points的下标
	 * <p>规则：从周一至周日，分别为0至6</p>
	 */
	private static int getTodayWeekIndex() {
		int week_index = CalendarUtils.nowOfCST().get(Calendar.DAY_OF_WEEK) - 1;
		if (week_index == 0) {
			return EWeek.SUNDAY.index;
		}
		return week_index - 1;
	}

	/**
	 * 获取今日的分数
	 */
	public int getSignTodayScore() {
		int index = getTodayWeekIndex();
		return points[index % EWeek.WEEK_COUNT];
	}

	/**
	 * 获取明天的分数
	 */
	public int getSignTomorrowScore() {
		int index = getTodayWeekIndex();
        return points[(index + 1) % EWeek.WEEK_COUNT];
	}

	/**
	 * 给定UserID，取他的最后一次签到时刻
	 * 
	 * @return 该用户最后一次签到时刻（UTC毫秒数），非正值表示未取到
	 */
	public long getLastSignTimestamp(String userId) {
		return signInfoList.getLastSignTime(userId);
	}

    /**
     * 判断用户今天是否签到
     * @param userId
     * @return
     * false 今天没签到 true 用户已签到
     */
    public boolean isUserSignToday(String userId) {
        long lastSignTime = signInfoList.getLastSignTime(userId);
        if(lastSignTime <= 0) {
            return false;
        }

        long todaySeconds = System.currentTimeMillis() / 1000;
        return (todaySeconds + TIME_ZONE_8_SECOND)/ HOUR_SECOND_24 <= (lastSignTime / 1000 + TIME_ZONE_8_SECOND) / HOUR_SECOND_24;

    }

    /**
     * 获取积分规则
     */
    public int[] getPoints() {
        return points;
    }

	/** 在手机上保存一下：最近一次签到的时刻 */
	private boolean saveLastSignTimestamp(long timestamp) {
		if (timestamp <= 0) {
			return false;
		}
		String userId = UserSession.getInstance().getUserId();
		if (TextUtils.isEmpty(userId)) {
			return false;
		}
		signInfoList.putLastSignTime(userId, timestamp);
		return true;
	}

	private class DefaultResponseHandler extends ResponseHandler {

		private final IResultCallback resultCallback;

		DefaultResponseHandler(IResultCallback resultCallback, Activity activity) {
			super(null, new ReLoginOnHttpUnauthorizedCallBack(activity));	// 不显示进度框
			this.resultCallback = resultCallback;
		}

		protected void onActionTimestampTook(String timestamp) { }

		@Override
		protected void onSuccess(Response response) {
			if (response.body != null && (response.code == HttpURLConnection.HTTP_ACCEPTED || response.code == HttpURLConnection.HTTP_CONFLICT)) {
				try {
					TaskCenter.AccomplishTasksResponse accomtr = TaskCenter.AccomplishTasksResponse.parseFrom(response.body);
					accTasksResponse = new AccomplishTasksResponse(accomtr);
					UserSession.getInstance().updateSorce(accTasksResponse.totalPoints);
					onActionTimestampTook(accomtr.getTimeStamp());
					resultCallback.onFinish(true);
				} catch (InvalidProtocolBufferException e) {
					Logger.e(LogTag.USER, "TaskCenter.AccomplishTasksResponse.parseFrom failed");
				}
			}
		}
		
		@Override
		protected void onFailure() {
			super.onFailure();
			if (resultCallback != null) {
				resultCallback.onFinish(false);
			}
		}
	}

	private class SignResponseHandler extends DefaultResponseHandler {

		SignResponseHandler(IResultCallback resultCallback, Activity activity) {
			super(resultCallback, activity);
		}

		@Override
		protected void onActionTimestampTook(String timestamp) {
			if (!TextUtils.isEmpty(timestamp)) {
				try {
					long value = Long.parseLong(timestamp);
					saveLastSignTimestamp(value);
				} catch (NumberFormatException e) {}
			}
		}
	}

	/**
	 * 向服务器发送签到请求
	 * 
	 * @param taskId
	 *            任务id
	 */
	public boolean requestSign(String taskId, final IResultCallback resultCallback, Activity activity) {
		if (taskId == null) {
			return false;
		}
		accTasksResponse = null;
		return HttpApiService.requestTaskFinished(taskId, new SignResponseHandler(resultCallback, activity));

	}
	
	/**
	 * 异步请求签到历史
	 */
	public void asyncRequestSignHistory(final RequestUserTaskHistoryCallback callback, Activity activity) {
		asyncRequestSignHistory(callback, activity, false);
	}

	/**
	 * 异步请求签到历史
	 */
	public void asyncRequestSignHistory(final RequestUserTaskHistoryCallback callback, Activity activity,boolean isUpdateTasksIfNeed) {
		List<TaskRecord> taskRecords = UserTaskManager.getInstance().getTaskRecord(TaskIdentifier.signIn);
		if(taskRecords == null || taskRecords.isEmpty()){
			onAsyncRequestSignHistoryTaskEmpty(callback, activity, isUpdateTasksIfNeed);
			return;
		}
		TaskRecord taskRecord = taskRecords.get(0);
        if (taskRecord == null) {
        	onAsyncRequestSignHistoryTaskEmpty(callback, activity, isUpdateTasksIfNeed);
			return;
        }
        String taskId = taskRecord.taskBrief.getTaskId();
		if (taskId == null) {
			return;
		}

		userSignHistory = null;
		ResponseHandler responseHandler = new ResponseHandler(activity, new ResponseHandler.ReLoginOnHttpUnauthorizedCallBack(activity)) {

			@Override
			protected void onSuccess(Response response) {
				if (response.code == HttpURLConnection.HTTP_OK) {
					if (response.body != null) {
						try {
							TaskCenter.GetUserTasksProgressResponse tasksProg = TaskCenter.GetUserTasksProgressResponse.parseFrom(response.body);
							userSignHistory = new UserTaskHistory(tasksProg);	// 取得历史列表
							// 根据历史列表，更新“本帐号的最后一次签到时刻”，并存盘
							for (int i = userSignHistory.getListSize() - 1; i >= 0; --i) {
								UserTaskHistoryItem elem = userSignHistory.getListItem(i);
								if (elem.wasExecuted()) {
									saveLastSignTimestamp(elem.timestamp);
									break;
								}
							}
							// 通知回调接口
							if (callback != null) {
								callback.onComplete(userSignHistory);
							}
						} catch (InvalidProtocolBufferException e) {
							Logger.e(LogTag.USER, "TaskCenter.GetUserTasksProgress.parseFrom failed");
						}
					}
				} else if (response.code == HttpURLConnection.HTTP_NOT_FOUND ||
					response.code == HttpURLConnection.HTTP_INTERNAL_ERROR) {
					if (callback != null) {
						callback.onComplete(null);
					}
				}
			}
		};

		HttpApiService.requestTasksProg(taskId, responseHandler);
	}

	/**
	 * @param callback
	 * @param activity
	 * @param isUpdateTasksIfNeed
	 */
	private void onAsyncRequestSignHistoryTaskEmpty(final RequestUserTaskHistoryCallback callback,Activity activity, boolean isUpdateTasksIfNeed) {
		if(isUpdateTasksIfNeed){
			asyncDownloadTaskList(getAsyncRequestSignHistoryTaskListObserver(callback, activity));
		}else{
			callback.onComplete(null);
		}
	}

	private void asyncDownloadTaskList(TaskListObserver observer) {
		UserTaskManager.getInstance().asyncDownloadTaskList();
		UserTaskManager.getInstance().registerObserver(observer);
	}
	
	private TaskListObserver getAsyncRequestSignHistoryTaskListObserver(final RequestUserTaskHistoryCallback callback, final Activity activity){
		return new TaskListObserver(){
			@Override
			public void onTaskListChanged() {
				asyncRequestSignHistory(callback, activity,false);
				UserTaskManager.getInstance().unregisterObserver(this);
			}
		};
	}
	
	private TaskListObserver getSingScoreTaskListObserver(){
		return new TaskListObserver(){
			@Override
			public void onTaskListChanged() {
				getSiginScore(false);
			}
			
		};
	}
	
	public static class AccomplishTasksResponse {
		final int resultCode;
		final int acquiredPoints;
		final int totalPoints;

		AccomplishTasksResponse(TaskCenter.AccomplishTasksResponse protoResponse) {
			this.resultCode = protoResponse.getResultCode();
			this.acquiredPoints = protoResponse.getAcquiredPoints();
			this.totalPoints = protoResponse.getTotalPoints();
		}

	}

	/**
	 * 用户任务（签到、分享等）历史
	 */
	public static class UserTaskHistory {
		private final ArrayList<UserTaskHistoryItem> list;

		/** 历史是否为空？ */
		public boolean isEmpty() {
			return list.isEmpty();
		}

		/** 历史记录有多少项？ */
		public int getListSize() {
			return list.size();
		}

		/** 根据下标取历史记录 */
		public UserTaskHistoryItem getListItem(int index) {
			return list.get(index);
		}

		/** 取最后一项（如果列表为空则返回null） */
		public UserTaskHistoryItem getLastItem() {
			return list.isEmpty() ? null : list.get(list.size() - 1);
		}

		private UserTaskHistory(TaskCenter.GetUserTasksProgressResponse protoResponse) {
			int count = protoResponse.getProgressListCount();
			this.list = new ArrayList<UserTaskHistoryItem>(count);
			for (int i = 0; i < count; i++) {
				UserTaskHistoryItem item = new UserTaskHistoryItem(protoResponse.getProgressList(i));
				this.list.add(item);
			}
		}
	}

	/**
	 * {@link UserTaskHistory} 里的记录项
	 */
	public static class UserTaskHistoryItem {
		public final long timestamp;
		public final int acquiredPoints;

		public UserTaskHistoryItem(TaskCenter.TaskProgressElem protoElem) {
			long timestamp = 0;
			try {
				timestamp = Long.parseLong(protoElem.getTimestamp());
			} catch (NumberFormatException e) {}
			this.timestamp = timestamp;
			this.acquiredPoints = protoElem.getAcquiredPoints();
		}

		/**
		 * 本日的任务是否已经完成（已经拿过积分了）？
		 */
		public boolean wasExecuted() {
			return this.acquiredPoints != 0;
		}
	}
	
}
