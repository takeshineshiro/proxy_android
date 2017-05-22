package cn.wsds.gamemaster.ui.exchange;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;

class TimeRangeFormatter {

	@SuppressLint("SimpleDateFormat") private final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日HH:mm");

	static String format(long milliseconds) {
		return simpleDateFormat.format(new Date(milliseconds));
	}
}
