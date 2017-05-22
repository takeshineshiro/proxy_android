package cn.wsds.gamemaster.ui.doublelink;

/**
 * Created by hujd on 16-6-6.
 */
public class UtilFormatTime {

	private static final int SECONDS_PER_MINUTE = 60;
	private static final int MINUTES_PER_HOUR = 60;
	private static final int HOURS_PER_DAY = 24;

	/**
	 * 将指定的时间秒数，格式化为形如 "00:00"的时间字串
	 * 
	 * @param timeSeconds
	 *            时长，单位：秒
	 */
	public static String formatDoubleAccelTime(long timeSeconds) {
		if (timeSeconds <= 0) {
			return "00:00";
		}
		int minutes = (int) timeSeconds / SECONDS_PER_MINUTE;
		int seconds = (int) timeSeconds % SECONDS_PER_MINUTE;
		int days, hours;
		if (minutes < MINUTES_PER_HOUR) {
			days = 0;
			hours = 0;
		} else {
			hours = minutes / MINUTES_PER_HOUR;
			minutes %= MINUTES_PER_HOUR;
			if (hours < HOURS_PER_DAY) {
				days = 0;
			} else {
				days = hours / HOURS_PER_DAY;
				hours %= HOURS_PER_DAY;
			}
		}
		return format(days, hours, minutes, seconds);
	}

	private static String format(int day, int hour, int minute, int second) {
		StringBuilder sb = new StringBuilder(32);
		// 有天就显示天
		if (day > 0) {
			sb.append(day).append(' ');
		}
		// 有小时就显示小时（有天的时候也总是显示小时）
		if (hour > 0 || day > 0) {
			appendInteger(sb, hour).append(':');
		}
		// 总是显示分钟和秒
		appendInteger(sb, minute).append(':');
		appendInteger(sb, second);
		return sb.toString();
	}
	
	private static StringBuilder appendInteger(StringBuilder sb, int value) {
		if (value < 10) {
			sb.append('0');
		}
		return sb.append(value);
	}
}
