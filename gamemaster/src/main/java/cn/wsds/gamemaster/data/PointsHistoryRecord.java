package cn.wsds.gamemaster.data;

import hr.client.appuser.PointsChangeRecordOuterClass.PointsChangeRecord;

import java.util.Calendar;

import android.text.TextUtils;

public class PointsHistoryRecord {
	
	public final String changeInfoDesc;
	public final int recordYear;
	public final int recordMonth;
	public final int recordDay;
	public final long socre;
	public final long milliseconds;
	
	public PointsHistoryRecord(PointsChangeRecord record, Calendar calendar) {
		String changeInfoDesc = getChangeInfoDesc(record.getChangeInfo());
		this.changeInfoDesc = changeInfoDesc;
		this.socre = record.getPoints();
		String timestamp = record.getTimestamp();
		if(TextUtils.isEmpty(timestamp)){
			this.milliseconds = 0;
		}else{
			this.milliseconds = Long.valueOf(timestamp);
		}
		if(milliseconds == 0){
			this.recordMonth = 0;
			this.recordDay = 0;
			this.recordYear = 0;
		}else{
			calendar.setTimeInMillis(milliseconds);
			this.recordMonth = calendar.get(Calendar.MONTH) + 1;
			this.recordDay = calendar.get(Calendar.DAY_OF_MONTH);
			this.recordYear = calendar.get(Calendar.YEAR);
		}
	}
	
	private static String getChangeInfoDesc(String changeInfo){
		if (null == changeInfo || changeInfo.length() == 0) {
			return changeInfo;
		} else if ("Coupon exchange fail".equals(changeInfo)) {
			return "流量兑换失败";
		} else if ("Coupon exchange".equals(changeInfo)) {
			return "流量兑换";
		} else if ("Sign in".equals(changeInfo)) {
			return "每日签到";
		} else if ("Bind phone number".equals(changeInfo)) {
			return "绑定手机号";
		} else if ("User register".equals(changeInfo)) {
			return "用户注册";
		} else if ("Sharing".equals(changeInfo)) {
			return "分享";
		} else if ("Card exchange".equals(changeInfo)) {
			return "礼包兑换";
		} else if (changeInfo.startsWith("Task")) {
			return "任务完成";
		} else {
			return changeInfo;
		}
	}
	
}
