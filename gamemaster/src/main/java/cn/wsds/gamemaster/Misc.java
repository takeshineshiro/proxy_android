package cn.wsds.gamemaster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.SimpleTimeZone;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.JsonReader;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebView;
import cn.wsds.gamemaster.data.AppProfile;
import cn.wsds.gamemaster.data.DateParams;
import cn.wsds.gamemaster.data.ProcessCleanRecords;
import cn.wsds.gamemaster.tools.ProcessKiller;
import cn.wsds.gamemaster.ui.floatwindow.CleanMemoryAuto;

import com.subao.common.utils.CalendarUtils;

public class Misc {

	/**
	 * 获取系统属性
	 * 
	 * @param propName
	 * @return
	 */
	public static String getSystemProperty(String propName) {
		BufferedReader input = null;
		try {
			Process p = Runtime.getRuntime().exec("getprop " + propName);
			input = new BufferedReader(new InputStreamReader(p.getInputStream()), 256);
			return input.readLine().trim();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 格式化时间
	 * 
	 * @param time
	 * @param hourFormat
	 * @return
	 */
	public static String formatTime(int time, String hourFormat) {
		DateParams dateParams = DateParams.build(time, hourFormat);
		return dateParams.discardNullValue();
	}

	public static long extractMobileFlowFromGameLog(String gameLog) {
		if (TextUtils.isEmpty(gameLog)) {
			return 0;
		}
		long sendBytes = 0, recvBytes = 0;
		int value_got = 0;
		JsonReader reader = null;
		try {
			reader = new JsonReader(new StringReader(gameLog));
			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				if ("ms".equals(name)) {
					sendBytes = reader.nextLong();
					value_got |= 1;
					if (value_got == 3) {
						break;
					}
				} else if ("mr".equals(name)) {
					recvBytes = reader.nextLong();
					value_got |= 2;
					if (value_got == 3) {
						break;
					}
				} else {
					reader.skipValue();
				}
			}
			return sendBytes + recvBytes;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			com.subao.utils.Misc.safeClose(reader);
		}
		return 0;
	}

	/**
	 * 根据指定的字符串数组和颜色值，生成SpannableString
	 * <p>
	 * 文字前景颜色交替
	 * </p>
	 * 
	 * @param ssb
	 *            如果传递null，则函数创建一个新的SpannableStringBuilder。否则直接使用给定的ssb，并在后面追加
	 * @param stringList
	 *            字符串数组
	 * @param color1
	 *            颜色1
	 * @param color2
	 *            颜色2
	 * @return 新创建的{@link SpannableStringBuilder}，或入口参数ssb
	 */
	public static SpannableStringBuilder buildSpannableStringFromArrays(SpannableStringBuilder ssb,
		String[] stringList, int color1, int color2) {
		if (ssb == null) {
			ssb = new SpannableStringBuilder();
		}
		int start = ssb.length();
		int color = color1;
		for (String s : stringList) {
			ssb.append(s);
			int pos = ssb.length();
			ssb.setSpan(new ForegroundColorSpan(color), start, pos, SpannedString.SPAN_EXCLUSIVE_EXCLUSIVE);
			start = pos;
			if (color == color1) {
				color = color2;
			} else {
				color = color1;
			}
		}
		return ssb;
	}

	/**
	 * 判断给定IP是否为本地IP
	 * <p>
	 * 判断标准为：<br />
	 * 192.168.0.0 ~ 192.168.255.255<br />
	 * 10.0.0.0 ~ 10.255.255.255<br />
	 * 172.16.0.0 ~ 172.31.255.25
	 */
	public static boolean isIpLocal(String ip) {
		if (TextUtils.isEmpty(ip)) {
			return false;
		}
		if (ip.startsWith("192.168.") || ip.startsWith("10.")) {
			return true;
		}
		if (ip.startsWith("172.")) {
			try {
				int v = Integer.parseInt(ip.substring(4));
				return (v >= 16 && v <= 31);
			} catch (NumberFormatException e) {}
		}
		return false;
	}

	/**
	 * 判断给定的ResUsage是不是达到定义的“高负荷”阈值了？
	 */
	public static boolean isResUsageOverflow(ResUsageChecker.ResUsage resUsage) {
		return resUsage.memoryUsage > 55 || resUsage.cpuUsage > 40;
	}

	/**
	 * 清理内存（杀死后台进程），如果成功，且showEffect为真，就显示一个动效
	 */
	public static void cleanMemory(Context context, List<AppProfile> runningAppList, boolean showEffect, int x, int y) {
		Set<String> cleanRecord = ProcessCleanRecords.getInstance().getCleanRecord(runningAppList);
		if (ProcessKiller.execute(context, cleanRecord)) {
			if (showEffect) {
				CleanMemoryAuto.createInstance(context, x, y, cleanRecord.size());
			}
		}
	}
	
	/** 每天有多少毫秒？ */
	private static final long MILLISECONDS_PER_DAY = 3600 * 1000 * 24;
	
	/** 将给定的UTC毫秒数（自UTC 1970-1-1以来），转换成北京时间的“天”数 */
	public static int millisecondsOfUTCToDayOfSCT(long millisecondsOfUTC) {
		return (int) ((millisecondsOfUTC + CalendarUtils.MILLISECONDS_OFFSET_TIMEZONE_BEIJIN) / MILLISECONDS_PER_DAY);
	}
	
	public static String formatCalendar(Calendar c) {
		return String.format("[%4d-%02d-%02d %02d:%02d:%02d (%+d)]",
			c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
			c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND),
			c.get(Calendar.ZONE_OFFSET) / (1000 * 3600));
	}
	
	public static Calendar millisecondsOfUTCToLocalCarlendar(long millisecondsOfUTC) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millisecondsOfUTC);
		return c;
	}

	/**
	 * 创建一个北京时区（GMT+8）的Calendar
	 */
	public static Calendar createCalendarOfCST() {
		return Calendar.getInstance(new SimpleTimeZone((int) CalendarUtils.MILLISECONDS_OFFSET_TIMEZONE_BEIJIN, "CST"));
	}
	
	/**
	 * 重要，由于WebView自有的内存引用问题，所以在使用完毕后一定用本函数清除它
	 */
	public static void clearWebView(WebView webView) {
		if (webView != null) {	
			ViewParent viewParent = webView.getParent();
			if(viewParent!=null){
				((ViewGroup) viewParent).removeView(webView);
			}     
	        webView.setWebViewClient(null);
	  		webView.setWebChromeClient(null);
	  		webView.setVisibility(View.GONE);
	  		webView.removeAllViews();
	  		webView.destroy();
	    }	 
	}
	
}
