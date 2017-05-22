package cn.wsds.gamemaster.ui.user;

import hr.client.appuser.TaskCenter.GetTaskListResponse;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.DataCache;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.tools.onlineconfig.OnlineConfigAgent;
import cn.wsds.gamemaster.ui.ActivityListRefreshBase;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.adapter.AdapterListRefreshBase;
import cn.wsds.gamemaster.ui.adapter.AdapterTaskRecord;
import cn.wsds.gamemaster.ui.user.UserTaskManager.TaskRecord;

import com.google.protobuf.InvalidProtocolBufferException;
import com.tencent.tauth.Tencent;

public class ActivityTaskCenter extends ActivityListRefreshBase<TaskRecord>{
	
	private UserTaskManager.TaskListObserver taskListObserver = new UserTaskManager.TaskListObserver(){

		@Override
		public void onTaskListChanged() {
			setData(DataCache.getTaskRecordsDataCache().getCacheData());
		}
		
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		findViewById(R.id.top_line).setVisibility(View.GONE);
		ListView listView = getListView();
		listView.setDividerHeight(getResources().getDimensionPixelSize(R.dimen.about_list_line_width));
		
		AdapterTaskRecord adapter = new AdapterTaskRecord(ActivityTaskCenter.this);
		setListAdapter(adapter);
		List<TaskRecord> cacheData = DataCache.getTaskRecordsDataCache().getCacheData();
		if(cacheData.isEmpty()){
			doRefresh();
		}else{
			setData(cacheData);
		}
		
		UserTaskManager.getInstance().registerObserver(taskListObserver);
	}
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		UserTaskManager.getInstance().unregisterObserver(taskListObserver);
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		AdapterListRefreshBase<TaskRecord> listAdapter = getListAdapter();
		if(listAdapter!=null){
			List<TaskRecord> datas = new ArrayList<TaskRecord>();
			for (int i=0; i < listAdapter.getDataSize();i++) {
				TaskRecord item = listAdapter.getItem(i);
				datas.add(item);
			}
			boolean removeDirstyDate = AdapterTaskRecord.removeDirstyData(datas);
			if(removeDirstyDate){
				setData(datas);
			}
		}
	}

	@Override
	protected int getLayoutResID() {
		return R.layout.activity_points_history;
	}

	@Override
	protected LoadMoreRequestor<TaskRecord> createLoadMoreRequestor() {
		return null;
	}

	@Override
	protected RefreshRequestor<TaskRecord> createRefreshRequestor() {
		return new TaskRefreshRequestor(this);
	}

	public static final class TaskRefreshRequestor extends RefreshRequestor<TaskRecord> {
		
		public TaskRefreshRequestor() {
			this(null);
		}

		public TaskRefreshRequestor(ActivityListRefreshBase<TaskRecord> activity) {
			super(activity);
		}

		@Override
		protected boolean doRequest() {
			OnlineConfigAgent instance = OnlineConfigAgent.getInstance();
			String baseApiUrl = instance.getBaseApiUrl();
			return HttpApiService.requestTaskList(baseApiUrl, this);
		}

		@Override
		protected void setData(byte[] body) throws InvalidProtocolBufferException {
			GetTaskListResponse response = GetTaskListResponse.parseFrom(body);
			UserTaskManager.getInstance().setTaskBriefs(response.getTaskListList());
			ActivityListRefreshBase<TaskRecord> ref = activityRef.get();
			if(ref!=null){
				UIUtils.showToast("加载完成");
			}
		}

		@Override
		protected void clearCache() {
			UserTaskManager.getInstance().setTaskBriefs(null);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Tencent.onActivityResultData(requestCode, resultCode, data, null);
	}
	
	@Override
	protected int getEmptyRefreshTextRes() {
		return R.string.list_refresh_empty_task;
	}
}
