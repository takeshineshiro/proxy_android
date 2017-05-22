package cn.wsds.gamemaster.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

import com.subao.common.Misc;
import com.subao.common.utils.InfoUtils;
import com.subao.common.utils.StringUtils;

public class DeviceInfo {
	
	/**
	 * 取自定义的友盟的渠道信息
	 */
	public static String getUmengChannel(Context context) {
		PackageManager pm = context.getPackageManager();
		if (pm == null) {
			return StringUtils.EMPTY;
		}
		ApplicationInfo ai;
		try {
			ai = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			return StringUtils.EMPTY;
		}
		if (ai == null) {
			return StringUtils.EMPTY;
		}
		Bundle metaData = ai.metaData;
		if (metaData == null) {
			return StringUtils.EMPTY;
		}
		Object channel = metaData.get("UMENG_CHANNEL");
		if (channel == null) {
			return StringUtils.EMPTY;
		}
		return channel.toString();
	}
	
	/**
	 * 返回一个用字符串表示的设备内存信息
	 * 
	 * @return 成功时返回字符串，失败时返回null
	 */
	public static String getMemoryInfo() {
		InputStream input = null;
		try {
			input = new FileInputStream(new File("/proc/meminfo"));
			byte[] data = new byte[512];
			int bytes = input.read(data);
			if (bytes <= 0) {
				return null;
			}
			// 查找第2个换行符
			String result = null;
			int lines = 0;
			for (int i = 0; i < bytes; ++i) {
				byte ch = data[i];
				if (ch == '\n') {
					++lines;
					if (lines == 2) {
						result = new String(data, 0, i);
						break;
					}
				}
			}
			if (result == null) {
				return new String(data);
			} else {
				return result;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			Misc.close(input);
		}
		return null;
	}
	
	/**
	 * 返回设备的一些信息
	 */
	public static String get(Context context) {
		StringBuilder sb = new StringBuilder(1024);
		addKeyValueToStringBuidler(sb, "FINGERPRINT", android.os.Build.FINGERPRINT);
		addKeyValueToStringBuidler(sb, "MODEL", android.os.Build.MODEL);
		addKeyValueToStringBuidler(sb, "CPU", InfoUtils.CPU.getCpuName());
		addKeyValueToStringBuidler(sb, "DISPLAY", android.os.Build.DISPLAY);
		addKeyValueToStringBuidler(sb, "SDK_INT", Integer.toString(android.os.Build.VERSION.SDK_INT));
		addKeyValueToStringBuidler(sb, "RELEASE", android.os.Build.VERSION.RELEASE);
		addKeyValueToStringBuidler(sb, "IMSI", InfoUtils.getIMSI(context));
		addKeyValueToStringBuidler(sb, "IMEI", InfoUtils.getIMEI(context));
		sb.append(getMemoryInfo());
		return sb.toString();
	}
	
	private static StringBuilder addKeyValueToStringBuidler(StringBuilder sb, String key, String value) {
		return sb.append(key).append(':').append(' ').append(value).append('\n');
	}
}
