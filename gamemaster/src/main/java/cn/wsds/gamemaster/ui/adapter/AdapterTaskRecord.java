package cn.wsds.gamemaster.ui.adapter;


import hr.client.appuser.TaskCenter.GetUserTasksProgressResponse;
import hr.client.appuser.TaskCenter.TaskBrief;
import hr.client.appuser.TaskCenter.TaskProgressElem;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.dialog.CommonAlertDialog;
import cn.wsds.gamemaster.dialog.MainShareDialog;
import cn.wsds.gamemaster.dialog.UserSign;
import cn.wsds.gamemaster.event.NewGameInstalledEvent;
import cn.wsds.gamemaster.net.http.Response;
import cn.wsds.gamemaster.net.http.ResponseHandler;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.ui.ActivityAddNewGame;
import cn.wsds.gamemaster.ui.ActivityMain;
import cn.wsds.gamemaster.ui.ActivityUser;
import cn.wsds.gamemaster.ui.ActivityUserAccount;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.UIUtils.IOnReloginConfirmListener;
import cn.wsds.gamemaster.ui.user.UserTaskManager;
import cn.wsds.gamemaster.ui.user.UserTaskManager.TaskIdentifier;
import cn.wsds.gamemaster.ui.user.UserTaskManager.TaskRecord;
import cn.wsds.gamemaster.ui.user.UserTaskManager.TaskRecord.Group;

import com.google.protobuf.InvalidProtocolBufferException;

public class AdapterTaskRecord extends AdapterListRefreshBase<TaskRecord> {
	
	private final Activity activity;
    public AdapterTaskRecord(Activity context) {
    	super(context);
    	activity = context;
    }

    @Override
    protected View getRealView(int position, View convertView, ViewGroup parent) {
    	TaskRecord item = getItem(position);
    	if(item==null){
    		return convertView;
    	}
    	ViewHolder viewHolder;
		if(convertView == null){
   			if(item.taskBrief == null){
   				convertView = createLabelConvertView(parent);
   				viewHolder = new LabelViewHolder(convertView);
    		}else{
    			convertView = createRecordConvertView(parent);
   				viewHolder = new RecordViewHolder(convertView);
    		}
   			convertView.setTag(viewHolder);
    	}else{
    		Object tag = convertView.getTag();
    		viewHolder = (ViewHolder) tag;
    		if(item.taskBrief == null){
    			if(!viewHolder.isLabelHolder()){
    				convertView = createLabelConvertView(parent);
       				viewHolder = new LabelViewHolder(convertView);
       				convertView.setTag(viewHolder);
    			}
    		}else{
    			if(viewHolder.isLabelHolder()){
    				convertView = createRecordConvertView(parent);
       				viewHolder = new RecordViewHolder(convertView);
       				convertView.setTag(viewHolder);
    			}
    		}
    	}
   		viewHolder.bindView(item);
    	return convertView;
    }

	private static View createRecordConvertView(ViewGroup parent) {
		return LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_record,parent,false);
	}

	private static View createLabelConvertView(ViewGroup parent) {
		return LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_label,parent,false);
	}

	private static abstract class ViewHolder {
    	public abstract void bindView(TaskRecord item);
    	public boolean isLabelHolder(){
    		return false;
    	}
    }
    
    private static final class LabelViewHolder extends ViewHolder{
    	private final TextView textLabel;
		private LabelViewHolder(View view) {
			this.textLabel = (TextView) view;
		}
    	public void bindView(TaskRecord item){
    		textLabel.setText(item.group.name);
    	}
    	@Override
    	public boolean isLabelHolder() {
    		return true;
    	}
    }
    
    private final class RecordViewHolder extends ViewHolder{
    	
    	private final TextView textName;
    	private final TextView textScore;
    	public final Button textAction;
		public RecordViewHolder(View group) {
			this.textName = (TextView) group.findViewById(R.id.text_name);
			this.textScore = (TextView) group.findViewById(R.id.text_score);
			this.textAction = (Button) group.findViewById(R.id.text_action);
		}
    	public void bindView(TaskRecord item){
    		Binder binder = createBinder(item.actionType);
			if(binder!=null){
				binder.bindView(item);
			}
    	}

    	/**
    	 * 负责将{@link TaskRecord}显示到UI上
    	 */
    	private abstract class Binder {
    		
    		public final void bindView(TaskRecord item){
        		String name = null;
        		Map<String, String> appClientParas = item.taskBrief.getAppClientParas();
        		if(appClientParas!=null){
        			name = appClientParas.get(UserTaskManager.KEY_NAME_DISPLAY);
        		}
        		if(TextUtils.isEmpty(name)){
        			name = item.taskBrief.getTaskName();
        		}
				textName.setText(name);
        		String score = getScore(item.taskBrief);
				textScore.setText("积分+" + score);
        		bindButton(item);
        	}

    		/** 由派生类实现：显示Button */
			protected abstract void bindButton(TaskRecord item);

			/** 派生类可重写：取得积分的字串 */
			protected String getScore(TaskBrief taskBrief) {
				int points = taskBrief.getPoints();
				if (points > 0){
					return String.valueOf(points);
				}
    			Collection<String> values = taskBrief.getCheckPoints().values();
    			if (values == null || values.size() == 0) {
    				return "";
    			}
				StringBuilder builder = new StringBuilder();
				for (String v : values) {
					builder.append(v);
					builder.append('/');
				}
				if (builder.length() > 1){
					builder.delete(builder.length() - 1, builder.length());
					return builder.toString();
				}
				return "";
			}
			
			protected void bindFinishedButton(){
				textAction.setBackgroundResource(R.drawable.selector_dialog_right_btn);
				textAction.setText("已完成");
				textAction.setEnabled(false);
			}
    	}
    	
    	private class AccumulativeBinder extends Binder{

    		private static final int  PROGRESS_VISIBLE_CONVERT_UNIT = 60 * 60;
    		
			@Override
			protected void bindButton(TaskRecord item) {
				textAction.setBackgroundResource(R.drawable.transparent);
				TaskBrief taskBrief = item.taskBrief;
				int accumulativeValue = getAccumulativeValue(item);
				int accumulative = getAccumulative(taskBrief);
				if(accumulative <= accumulativeValue){
					bindFinishedButton();
					return;
				}
				int unit = accumulative / PROGRESS_VISIBLE_CONVERT_UNIT;
				int pro = accumulativeValue * 10 / PROGRESS_VISIBLE_CONVERT_UNIT;
				String accuratePro = (pro % 10 >= 5) ? ".5" : "";
				textAction.setText(String.format("%d%s/%d", pro/10, accuratePro,unit));
			}

			/**
			 * @param taskBrief
			 * @return
			 */
			private int getAccumulativeValue(TaskRecord item) {
				List<TaskProgressElem> taskProgressList = item.getTaskProgressElems();
				if(taskProgressList == null || taskProgressList.isEmpty()){
					return 0;
				}
				int accumulativeValue = 0;
				for (TaskProgressElem taskProgressElem : taskProgressList) {
					accumulativeValue += taskProgressElem.getAccumulativeValue();
				}
				return accumulativeValue;
			}

			/**
			 * @param taskBrief
			 * @return
			 */
			private int getAccumulative(TaskBrief taskBrief) {
				Set<String> keySet = taskBrief.getCheckPoints().keySet();
				if(keySet == null || keySet.isEmpty()){
					return 0;
				}
				int accumulative = 0;
				try {
					accumulative = Integer.valueOf(keySet.toArray(new String[keySet.size()])[0]);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return accumulative;
			}
    		
    	}
    	
    	private class ActionBinder extends Binder{

    		private final class OnViewClickListener implements OnClickListener {

				private TaskRecord item;
				
				private ResponseHandler responseHandler = new ResponseHandler(activity,new ResponseHandler.ReLoginOnHttpUnauthorizedCallBack(activity)) {
					
					@Override
					protected void onSuccess(Response response) {
						if(HttpURLConnection.HTTP_GONE == response.code){
							onTaskGone();
						}else if(HttpURLConnection.HTTP_OK == response.code){
							parsrFrom(response.body);
						}else{
							doWork();
						}
					}

					private void parsrFrom(byte[] body) {
						try {
							GetUserTasksProgressResponse parseFrom = GetUserTasksProgressResponse.parseFrom(body);
							if(0 == parseFrom.getResultCode()){
								if(item.updateProgressItem(parseFrom.getProgressListList())){
									boolean isComplete = !item.isTaskProgressElemsEmpty();
									bindButtonState(isComplete);
									if(isComplete){
										UIUtils.showToast("您已完成过此任务");
									}
								}else{
									doWork();
								}
							}
						} catch (InvalidProtocolBufferException e) {}
					}

					private void onTaskGone() {
						if (activity == null || activity.isFinishing()) {
							return;
						}
						CommonAlertDialog dialog = new CommonAlertDialog(activity);
						dialog.setMessage("任务已过期，快去完成其他任务吧");
						dialog.setPositiveButton(R.string.confirm, null);
						dialog.setOnDismissListener(new OnDismissListener() {
							
							@Override
							public void onDismiss(DialogInterface dialog) {
								UserTaskManager.getInstance().asyncDownloadTaskList();
							}
						});
						dialog.show();
					}
					
				};

				public OnViewClickListener(TaskRecord item) {
					this.item = item;
				}

				protected void doWork() {
					if(item.actionType == TaskIdentifier.share){
						new MainShareDialog(activity).show();
						return;
					}else if(item.actionType == TaskIdentifier.registerOrBind){
						if(UserSession.isLogined()){
							ActivityUserAccount.open(activity, ActivityUserAccount.FRAGMENT_TYPE_BIND_PHONE);
						}else{
							ActivityUserAccount.open(activity, ActivityUserAccount.FRAGMENT_TYPE_REGUSTER);
						}
						return;
					}
					if(!UserSession.isLogined()){
						UIUtils.showReloginDialog(activity, UserTaskManager.RELOGIN_MESS, new IOnReloginConfirmListener() {
							
							@Override
							public void onConfirm() {
								activity.finish();
							}
						});
						return;
					}
					switch(item.actionType){
					case downloadAndInstallGame:
						UIUtils.turnActivity(activity, ActivityAddNewGame.class);
						break;
					case startGameInside:
						UIUtils.turnActivity(activity, ActivityMain.class);
						break;
					case addAppointGame:
						onClickAddPointGame();
						break;
					default:
						break;
					}
				}

				@Override
				public void onClick(View v) {
					HttpApiService.requestTasksProg(item.taskBrief.getTaskId(), responseHandler);
				}

				private void onClickAddPointGame() {
					Map<String, String> appClientParas = item.taskBrief.getAppClientParas();
					if(appClientParas!=null){
						String appLabel = appClientParas.get(UserTaskManager.KEY_GAME_NAME);
						if(!TextUtils.isEmpty(appLabel)){
							NewGameInstalledEvent.whenToAddGamesTask(appLabel);
						}
					}
					
					String uri = item.taskBrief.getActivityTaskUrl();
					UIUtils.openUrl(activity,uri);
				}
    		}
    		
			@Override
			protected void bindButton(TaskRecord item) {
				textAction.setBackgroundResource(R.drawable.selector_dialog_right_btn);
				textAction.setOnClickListener(new OnViewClickListener(item));
				
				boolean taskProgressElemsEmpty = item.isTaskProgressElemsEmpty();
				bindButtonState(!taskProgressElemsEmpty);
			}

			/**
			 * @param isComplete
			 */
			private void bindButtonState(boolean isComplete) {
				if(isComplete){
					textAction.setText("已完成");
					textAction.setEnabled(false);
				}else{
					textAction.setText("去完成");
					textAction.setEnabled(true);
				}
			}
    		
    	}
    	
    	private class SignBinder extends Binder {

			@Override
			protected void bindButton(TaskRecord item) {
				textAction.setBackgroundResource(R.drawable.selector_dialog_right_btn);
				ActivityUser.UserSignin userSignin = new ActivityUser.UserSignin(textAction,activity);
				userSignin.refreshButtonSign();
			}
    		
			@Override
			protected String getScore(TaskBrief taskBrief) {
				return String.valueOf(UserSign.getInstance().getSignTodayScore());
			}
			
    	}
    	
		
		private Binder createBinder(TaskIdentifier identifier){
			switch(identifier){
			case accel:
				return new AccumulativeBinder();
			case downloadAndInstallGame:
			case startGameInside:
			case share:
			case addAppointGame:
			case registerOrBind:
				return new ActionBinder();
			case signIn:
				return new SignBinder();
			default:
				return new Binder(){

					@Override
					protected void bindButton(TaskRecord item) {}
					
				};
			}
		}

    }

	@Override
	protected void doAddData(List<TaskRecord> data) {
		super.doAddData(arrangeData(data));
	}

	/**
	 * 整理数据 
	 * 1.去除脏数据、2.添加标签、3.重新排序
	 * @param data
	 * @return 整理后的数据
	 */
	private List<TaskRecord> arrangeData(List<TaskRecord> data) {
		List<TaskRecord> temp = new ArrayList<TaskRecord>(data);
		removeDirstyData(temp);
		Set<TaskRecord> groupLabel = getGroupLabel(temp);
		if(groupLabel!=null){
			temp.addAll(groupLabel);
		}
		Collections.sort(temp);
		return temp;
	}

	/**
	 * 移除脏数据，在源数据的基础上移除脏数据，并返回是否成功移除脏数据的结果
	 * @param data 源数据
	 * @return 
	 * 是否成功移除脏数据的结果
	 */
	public static boolean removeDirstyData(List<TaskRecord> data) {
		List<TaskRecord> temp = new ArrayList<TaskRecord>(data);
		boolean hasDirstyRecord = false;
		for (TaskRecord taskRecord : temp) {
			if(taskRecord.isDirstyRecord()){
				hasDirstyRecord = true;
				data.remove(taskRecord);
			}
		}
		return hasDirstyRecord;
	}

	/**
	 * 获得任务各组标签
	 * @param data 任务源数据
	 * @return
	 * 当源数据为空时返回  null,否则返回任务源数据中的标签集合
	 */
	private Set<TaskRecord> getGroupLabel(List<TaskRecord> data) {
		if(data.isEmpty()){
			return null;
		}
		Set<TaskRecord> temp = new HashSet<TaskRecord>();
		TaskRecord everyDayLabel = new TaskRecord(Group.evetyDay);
		TaskRecord everyWeekLabel = new TaskRecord(Group.everyWeek);
		TaskRecord otherLabel = new TaskRecord(Group.other);
		for (TaskRecord taskRecord : data) {
			switch (taskRecord.group) {
			case everyWeek:
				temp.add(everyWeekLabel);
				break;
			case evetyDay:
				temp.add(everyDayLabel);
				break;
			case other:
			default:
				temp.add(otherLabel);
				break;
			}
		}
		return temp;
	}
	
	@Override
	public boolean isLoadMore() {
		return false;
	}
	
}