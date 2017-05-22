package cn.wsds.gamemaster.dialog;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.InvalidProtocolBufferException;
import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.utils.CalendarUtils;
import com.subao.resutils.WeakReferenceHandler;
import com.subao.utils.FileUtils;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.Misc;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.UserInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.net.NetworkStateChecker;
import cn.wsds.gamemaster.net.http.ResponseHandler;
import cn.wsds.gamemaster.pb.Proto;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.user.UserTaskManager;
import cn.wsds.gamemaster.ui.user.UserTaskManager.TaskIdentifier;
import cn.wsds.gamemaster.ui.user.UserTaskManager.TaskRecord;
import cn.wsds.gamemaster.ui.view.LoadingRing;
import hr.client.appuser.TaskCenter;

/**
 * user sign in Created by hujd on 15-12-9.
 */
public class UserSigninDialog extends Dialog {
	
	public interface Listener extends UserSign.IResultCallback {
		public void onDismiss(UserSigninDialog dialog);
	}

	private static final int MSG_DIALOG_FINISH = 0;
	private static final int MSG_DIALOG_REFRSH = 1;

    private static UserSigninDialog instance;

	private final SignInUI[] views = new SignInUI[UserSign.EWeek.WEEK_COUNT];
	private int[] points = {0,0,0,0,0,0,0};

	private final TextView buttonSignIn;
    private final Activity activity;

    private LoadingRing loadingRing;
	private final String taskId;

	/** 今天是星期几？ （取值0~6，参同{@link UserSign.EWeek} */
	private int dayOfWeek;

	private final Listener listener;

	private View layoutSignIn;

	private final int color_8, color_11, color_31;
	private final int textSize16;

	private MyHandler mHandler = new MyHandler(this);

    private static final long TIME_ZONE_8_SECOND = 8 * 3600;
    private static final long HOUR_SECOND_24 = 24 * 3600;

	private static class MyHandler extends WeakReferenceHandler<UserSigninDialog> {

		MyHandler(UserSigninDialog dialog) {
			super(dialog);
		}

		@Override
		protected void handleMessage(UserSigninDialog dlg, Message msg) {
			switch (msg.what) {
			case MSG_DIALOG_FINISH:
				dlg.safeDismiss();
				break;
			case MSG_DIALOG_REFRSH:
                UserSign.getInstance().requestSign(dlg.taskId, dlg.listener, dlg.activity);
                dlg.refreshAnimation();
				break;
			}
		}

	}

	private View.OnClickListener onClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.sign_in_btn:
                if (!NetworkStateChecker.defaultInstance.isNetworkAvail()) {
                	ResponseHandler.showToastWhenNetError();
                    return;
                }
				v.setEnabled(false);
				ImageView imageView = views[dayOfWeek].imageMidd;
				imageView.setImageResource(R.drawable.date_progress_right);
				imageView.setBackgroundResource(R.drawable.date_progress_taday);
                //500毫秒为了显示签到打勾过程
				mHandler.sendEmptyMessageDelayed(MSG_DIALOG_REFRSH, 500);
				break;
			case R.id.sign_in_again_btn:
				v.setEnabled(false);
				mHandler.sendEmptyMessage(MSG_DIALOG_REFRSH);
				break;
			case R.id.sign_in_view:
				UserSigninDialog.this.safeDismiss();
				break;
			}
		}
	};

	/**
	 * 每天签到记录UI的容器封装
	 */
	private static class SignInUI {

		public final ImageView imageTop, imageMidd, imageBottom;
		public final TextView textAccumPoint, textSignInDate;

		public SignInUI(View view) {
			imageTop = (ImageView) view.findViewById(R.id.image_draw_top);
			imageMidd = (ImageView) view.findViewById(R.id.image_draw);
			imageBottom = (ImageView) view.findViewById(R.id.image_draw_bottom);
			textAccumPoint = (TextView) view.findViewById(R.id.text_accum_point);
			textSignInDate = (TextView) view.findViewById(R.id.text_signin_date);
		}
	}

	public static void createUserSignin(Activity context, Listener listener, UserSign.UserTaskHistory userTaskHistory) {
		if (instance == null) {
			if(context==null || context.isFinishing()){
				return;
			}
			instance = new UserSigninDialog(context, listener, userTaskHistory);
			instance.show();
        	ConfigManager.getInstance().setDaySignDialogPopup(CalendarUtils.todayLocal());
		}else{
			if(instance.isShowing()){
				instance.safeDismiss();
			}
			instance = null;
			createUserSignin(context, listener, userTaskHistory);
		}
	}

    /**
     * 停在刷新动画
     */
    public static void stopRefreshAnimation() {
        if(instance != null) {
            instance.loadingRing.requestStop();
        }
    }
	private UserSigninDialog(Activity activity, Listener listener, UserSign.UserTaskHistory userTaskHistory) {
		super(activity, R.style.AppDialogTheme);
        this.activity = activity;
		this.listener = listener;
		List<TaskRecord> taskRecords = UserTaskManager.getInstance().getTaskRecord(TaskIdentifier.signIn);
		if(taskRecords == null || taskRecords.isEmpty()){
			this.taskId = null;
		}else{
			TaskRecord taskRecord = taskRecords.get(0);
			TaskCenter.TaskBrief taskBrief = taskRecord.taskBrief;
			this.taskId = (taskBrief != null) ? taskBrief.getTaskId() : null;
			if (taskBrief != null) {
				points = UserSign.getInstance().getPoints();
			}
		}
		//
		Resources res = activity.getResources();
		color_8 = res.getColor(R.color.color_game_8);
		color_11 = res.getColor(R.color.color_game_11);
		color_31 = res.getColor(R.color.color_game_31);
		textSize16 = res.getDimensionPixelOffset(R.dimen.text_size_16);
		//
		setContentView(R.layout.user_signin_dialog);
		buttonSignIn = (TextView)findViewById(R.id.sign_in_btn);
		buttonSignIn.setOnClickListener(onClickListener);
		//
        initArray();
		initSignIn(userTaskHistory);
		//
		layoutSignIn = findViewById(R.id.sign_in_layout);
		loadingRing = (LoadingRing) findViewById(R.id.accel_list_refresh);
        loadingRing.setDuration(15000);
        findViewById(R.id.sign_in_view).setOnClickListener(onClickListener);
		setCurrentPointText();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mHandler.removeCallbacksAndMessages(null);
		if (this.listener != null) {
			this.listener.onDismiss(this);
		}
		if (instance == this) {
			instance = null;
		}
	}

	private void setCurrentPointText() {
        //两个布局文件，同一个id
        TextView textview = (TextView) findViewById(R.id.text_current_point);
        textview.setText(getTextBuilder("今日签到获得", Integer.toString(points[dayOfWeek]), "积分"));
	}

	private SpannableStringBuilder getTextBuilder(CharSequence partBegin, CharSequence highlight, CharSequence partEnd) {
		SpannableStringBuilder builder = new SpannableStringBuilder();
		builder.append(partBegin).append(highlight);
		if (partEnd != null && partEnd.length() > 0) {
			builder.append(partEnd);
		}
		int lenPartBegin = partBegin.length();
		builder.setSpan(new ForegroundColorSpan(color_8),
			lenPartBegin, lenPartBegin + highlight.length(),
			Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		return builder;
	}

	private void setSucessDisplay(UserSign.AccomplishTasksResponse response) {
		if (response == null) {
			return;
		}
		setContentView(R.layout.user_signin_sucess_dialog);
		TextView textView = (TextView) findViewById(R.id.text_point);
		String highlight = String.format(" +%d", response.acquiredPoints);
		textView.setText(getTextBuilder("积分", highlight, null));
		mHandler.sendEmptyMessageDelayed(MSG_DIALOG_FINISH, 3000);
	}

//    private boolean timeException() {
//        UserInfo info = UserSession.getInstance().getUserInfo();
//        if(info == null) {
//            return true;
//        }
//        UserSign.SignInfo signInfo = UserSign.getInstance().getSignInfo(info.getUserId());
//        if(signInfo == null) {
//            return true;
//        }
//
//        if(signInfo.getTimestamp() == 0) {
//            return false;
//        }
//
//        long nowTime = System.currentTimeMillis() / 1000;
//
//        return (nowTime + TIME_ZONE_CTS_SECOND) / (24 * 3600) >
//                (signInfo.getTimestamp() + TIME_ZONE_CTS_SECOND) / (24 * 3600);
//    }
	private void setFailDisplay(boolean isSign) {
		setContentView(R.layout.user_signin_fail_dialog);
		loadingRing = (LoadingRing) findViewById(R.id.accel_list_refresh);
        loadingRing.setDuration(15000);
        findViewById(R.id.sign_in_again_btn).setOnClickListener(onClickListener);
		findViewById(R.id.sign_in_view).setOnClickListener(onClickListener);
		findViewById(R.id.sign_in_layout).setVisibility(View.VISIBLE);
		setCurrentPointText();
		if (isSign) {
			setButtonAlreadySignInToday((TextView) findViewById(R.id.sign_in_again_btn));
			// 企图签到，但是服务器返回“今日已签到”，可能是别的设备签了，刷一下签到历史
			UserSign.getInstance().asyncRequestSignHistory(null, null);
		}
	}

	private void setButtonAlreadySignInToday(TextView button) {
		button.setText(R.string.already_sign_in_today);
		button.setTextColor(color_31);
		button.setEnabled(false);
	}

	private static class UserSignContinuity {

		private static final String FILENAME = "user_sign_days";

		public static final UserSignContinuity instance = new UserSignContinuity();

		private final Map<String, Proto.ContinuityInfo> map;

		private static Map<String, Proto.ContinuityInfo> loadFromFile() {
			File file = FileUtils.getDataFile(FILENAME);
			byte[] data = FileUtils.read(file);
			if (data != null) {
				try {
					Proto.UserSignContinuity continuity = Proto.UserSignContinuity.parseFrom(data);
					return new HashMap<String, Proto.ContinuityInfo>(continuity.getConSign());
				} catch (InvalidProtocolBufferException e) {
					e.printStackTrace();
				}
			}
			return new HashMap<String, Proto.ContinuityInfo>();
		}

		private UserSignContinuity() {
			this.map = loadFromFile();
		}

		private byte[] serial() {
			Proto.UserSignContinuity.Builder p = Proto.UserSignContinuity.newBuilder();
			p.putAllConSign(map);
			return p.build().toByteArray();
		}

		private void saveToFile() {
			File file = FileUtils.getDataFile(FILENAME);
			FileUtils.write(file, serial());
		}

		public Proto.ContinuityInfo getContinuedSignInfo(String userId) {
			Proto.ContinuityInfo value = map.get(userId);
			return (value == null) ? null : value;
		}

		public void setContinuedSignInfo(String userId, long timestamp, int count) {
            Proto.ContinuityInfo.Builder p = Proto.ContinuityInfo.newBuilder();
            p.setCount(count);
            p.setTimestamp(timestamp);
			map.put(userId, p.build());
			saveToFile();
		}

//        public void removeContinuedSignInfo(String userId) {
//            if (null != map.remove(userId)) {
//            	saveToFile();
//            }
//        }
	}


	/**
	 * 添加统计事件
	 */
	private void addStaticEvent() {
		Statistic.addEvent(getContext(), Statistic.Event.USER_SIGN_SAMEDAY, UserSign.EWeek.TEXT[dayOfWeek]);
		//
		UserInfo info = UserSession.getInstance().getUserInfo();
		if (info != null) {
            int cnt = 1;
            String userId = info.getUserId();
            Proto.ContinuityInfo continuityInfo = UserSignContinuity.instance.getContinuedSignInfo(userId);
            long todaySignTimestamp = UserSign.getInstance().getLastSignTimestamp(userId);
			if(continuityInfo != null) {
                boolean isContinuty = isContinuitySign(todaySignTimestamp, continuityInfo.getTimestamp());
                //如果不是连续签到，删除制定userId数据
                if(!isContinuty) {
                	// 未连续，则记录“连续签到天数为1”（因为今天签到了）
                    UserSignContinuity.instance.setContinuedSignInfo(userId, todaySignTimestamp, 1);
                    return;
                }
                cnt = continuityInfo.getCount();
                cnt++;
                if (cnt > 1 && cnt <= 90) {
                    Statistic.addEvent(getContext(), Statistic.Event.USER_SIGN_CONTINUITY, String.valueOf(cnt));
                } else if (cnt > 90) {
                    Statistic.addEvent(getContext(), Statistic.Event.USER_SIGN_CONTINUITY, "大于90");
                }
            }
            //最多统计90连续签到情况
            if (cnt <= 90) {
                UserSignContinuity.instance.setContinuedSignInfo(userId, todaySignTimestamp, cnt);
            }
        }
	}

	/**
	 * 判断两次签到时间是否是连续的自然天（北京时间）
	 * @param todaySignTimestamp 今天（最近一次）签到时刻（毫秒）
	 * @param lastContinuitySignTimestamp 记录的最后一次连续签到的时刻（毫秒）
	 * @return true表示两次签到时间，在北京时间的自然天上，是连续的
	 */
    private static boolean isContinuitySign(long todaySignTimestamp, long lastContinuitySignTimestamp) {
    	long lastContinuitySeoncds =  lastContinuitySignTimestamp / 1000;
    	long todaySeconds = todaySignTimestamp / 1000;
        return (todaySeconds + TIME_ZONE_8_SECOND)/ HOUR_SECOND_24 - (lastContinuitySeoncds + TIME_ZONE_8_SECOND) / HOUR_SECOND_24 <= 1;
    }

    /**
	 * 执行刷新动画
	 */
	private void refreshAnimation() {
		if (layoutSignIn != null) {
			layoutSignIn.setVisibility(View.INVISIBLE);
		}

		View view = findViewById(R.id.sign_in_layout);
		if (view != null) {
			view.setVisibility(View.INVISIBLE);
		}

		loadingRing.start(new LoadingRing.OnCompleteListener() {

            @Override
            public void onComplete() {
                UserSign.AccomplishTasksResponse response = UserSign.getInstance().getAccTasksResponse();
                if (response != null) {
                    if (response.resultCode != 137) {
                        addStaticEvent();
                        setSucessDisplay(response);
                    } else {
                        //显示今日已签到情况
                        setFailDisplay(true);
                    }
                } else {
                    setFailDisplay(false);
                }
                loadingRing.setVisibility(View.INVISIBLE);
            }
        });
		loadingRing.setVisibility(View.VISIBLE);
	}

	private void initSignIn(UserSign.UserTaskHistory tasksProgResp) {
		views[UserSign.EWeek.MONDAY.index].imageTop.setVisibility(View.INVISIBLE);
		views[UserSign.EWeek.SUNDAY.index].imageBottom.setVisibility(View.INVISIBLE);
		/** 处理签到显示 */
		if (tasksProgResp != null && !tasksProgResp.isEmpty()) {
			int size = Math.min(UserSign.EWeek.WEEK_COUNT, tasksProgResp.getListSize());
			dayOfWeek = size - 1;
			for (int index = 0; index < size; index++) {
				UserSign.UserTaskHistoryItem elem = tasksProgResp.getListItem(index);
				if (elem.wasExecuted()) {
					signedItemDisplay(index);	// 该日已签到
				} else if (index == (size - 1)) {
					signTodayItemDisplay(index);	// 如果该日未签到，则仅当该日是“今天”时，才特殊显示
				}
				//
				if (Logger.isLoggableDebug(LogTag.USER)) {
					Logger.d(LogTag.USER, String.format("[%d] %s, points=%d",
						index,
						Misc.formatCalendar(CalendarUtils.calendarLocal_FromMilliseconds(elem.timestamp)),
						elem.acquiredPoints));
				}
			}
			// 如果最后一天已签到，按钮灰掉
			if (GlobalDefines.CLIENT_PRE_CHECK) {
				UserSign.UserTaskHistoryItem elem = tasksProgResp.getListItem(size - 1);
				if (elem.wasExecuted()) {
					setButtonAlreadySignInToday(buttonSignIn);
				}
			}
		} else {
			if (Logger.isLoggableDebug(LogTag.USER)) {
				Logger.d(LogTag.USER, "Sign history is empty");
			}
		}
	}

	private void initArray() {
		int idList[] = new int[] {
			R.id.inc_mod_signin,
			R.id.inc_tue_signin,
			R.id.inc_wed_signin,
			R.id.inc_tur_signin,
			R.id.inc_fir_signin,
			R.id.inc_sar_signin,
			R.id.inc_sun_signin
		};
		for (int i = 0; i < views.length; ++i) {
			SignInUI signInUI = new SignInUI(findViewById(idList[i]));
			views[i] = signInUI;
			signInUI.textSignInDate.setText(UserSign.EWeek.TEXT[i]);
			signInUI.textAccumPoint.setText("+" + points[i] + "分");
		}
	}

	/**
	 * 设置UI：指定的某日已签到
	 * 
	 * @param index
	 */
	private void signedItemDisplay(int index) {
		SignInUI signInUI = views[index];
		signInUI.textAccumPoint.setTextColor(color_11);
		ImageView imageView = signInUI.imageMidd;
		imageView.setImageResource(R.drawable.date_progress_right);
		imageView.setBackgroundResource(R.drawable.date_progress_taday);
	}

	/**
	 * 设置UI：“今天”尚未签到
	 * 
	 * @param index
	 *            “今天”是七项里的哪一项？（0~6，0表示周一，6表示周日）
	 */
	private void signTodayItemDisplay(int index) {
		SignInUI signInUI = views[index];
		TextView textView = signInUI.textAccumPoint;
		textView.setTextColor(color_8);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize16);
		signInUI.imageMidd.setImageResource(R.drawable.date_progress_taday);
	}
	
	private void safeDismiss() {
		try {
			dismiss();
		} catch (RuntimeException e) { }
	}

}
