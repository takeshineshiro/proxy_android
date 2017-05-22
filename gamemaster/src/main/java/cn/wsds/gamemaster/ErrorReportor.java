package cn.wsds.gamemaster;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.text.TextUtils;
import cn.wsds.gamemaster.data.DeviceInfo;
import cn.wsds.gamemaster.data.UserSession;

import com.subao.common.data.SubaoIdManager;
import com.subao.utils.FileUtils;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.bugly.crashreport.CrashReport.CrashHandleCallback;
import com.tencent.bugly.crashreport.CrashReport.UserStrategy;

public class ErrorReportor {

	private static final boolean enabled = true;

	private static class Callback extends CrashHandleCallback {

		private static class CrashRecorder {

			private static final String CRLF = "\r\n";

			private static int getStringLength(String s) {
				return s == null ? 0 : s.length();
			}

			private static String crashTypeToString(int crashType) {
				switch (crashType) {
				case CrashHandleCallback.CRASHTYPE_JAVA_CATCH:
					return "JAVA_CATCH";
				case CrashHandleCallback.CRASHTYPE_JAVA_CRASH:
					return "JAVA_CRASH";
				case CrashHandleCallback.CRASHTYPE_NATIVE:
					return "NATIVE";
				case CrashHandleCallback.CRASHTYPE_U3D:
					return "JAVA_U3D";
				case CRASHTYPE_ANR:
					return "ANR";
				case CRASHTYPE_COCOS2DX_JS:
					return "COCOS2DX_JS";
				case CRASHTYPE_COCOS2DX_LUA:
					return "COCOS2DX_LUA";
				default:
					return Integer.toString(crashType);
				}
			}

			public static void execute(int crashType, String errorType, String errorMessage, String errorStack) {

				StringBuilder sb = new StringBuilder(getStringLength(errorType) + getStringLength(errorMessage) + getStringLength(errorStack) + 1024);
				sb.append(CRLF);
				sb.append("========< CRASH at ").append(com.subao.utils.StringUtils.formatDateTime(new Date())).append(" >========").append(CRLF);
				sb.append("** Crash Type: ").append(crashTypeToString(crashType)).append(CRLF);
				sb.append("** Error Type: ").append(errorType).append(CRLF);
				sb.append("** Error Message:\r\n").append(errorMessage).append(CRLF);
				sb.append("** Error Stack:\r\n").append(errorStack).append(CRLF);
				sb.append("========< End >========").append(CRLF);
				String content = sb.toString();
				System.err.append(content);
				System.err.flush();
				FileWriter writer = null;
				try {
					writer = new FileWriter(getErrorLogFile(), true);
					writer.write(content);
					writer.flush();
				} catch (IOException e) {

				} finally {
					com.subao.common.Misc.close(writer);
				}

			}
		}

		@Override
		public synchronized Map<String, String> onCrashHandleStart(int crashType, String errorType, String errorMessage, String errorStack) {
			try {
				CrashRecorder.execute(crashType, errorType, errorMessage, errorStack);
				String subaoId = SubaoIdManager.getInstance().getSubaoId();
				boolean isSubaoIdValid = SubaoIdManager.isSubaoIdValid(subaoId);
				String userId = UserSession.getInstance().getUserId();
				boolean isUserIdValid = !TextUtils.isEmpty(userId);
				if (isSubaoIdValid || isUserIdValid) {
					Map<String, String> map = new HashMap<String, String>(2);
					if (isSubaoIdValid) {
						map.put("subaoid", subaoId);
					}
					if (isUserIdValid) {
						map.put("userid", userId);
					}
					return map;
				}
			} catch (RuntimeException e) {
			}
			return null;
		}
	}

	public static File getErrorLogFile() {
		return FileUtils.getLogFile("errorlog.txt");
	}

	public static void deleteErrorLogFile() {
		File file = getErrorLogFile();
		file.delete();
	}

	public static void init(Context context) {
		if (enabled) {
			UserStrategy us = new UserStrategy(context);
			us.setCrashHandleCallback(new Callback());
			us.setAppChannel(DeviceInfo.getUmengChannel(context));
			CrashReport.initCrashReport(context, "900031236", false, us);
		}
	}

	public static void postCatchedException(Throwable t) {
		if (enabled) {
			CrashReport.postCatchedException(t);
		}
	}

	public static void testJavaCrash() {
		if (enabled) {
			CrashReport.testJavaCrash();
		}
	}

	public static void testNativeCrash() {
		if (enabled) {
			CrashReport.testNativeCrash();
		}
	}

	public static void testANRCrash() {
		if (enabled) {
			CrashReport.testANRCrash();
		}
	}

}
