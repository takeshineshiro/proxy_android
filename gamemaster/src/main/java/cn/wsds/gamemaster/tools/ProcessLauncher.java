package cn.wsds.gamemaster.tools;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public class ProcessLauncher {

	public static boolean execute(Context context, String packageName) {
		try {
			Intent intent = getLaunchIntend(context, packageName);
			if (intent == null) {
				return false;
			}
			if (executeWithAM(intent)) {
				return true;
			}
			context.startActivity(intent);
			return true;
		} catch (Throwable t) {
			return false;
		}
	}
	
	private static boolean executeWithAM(Intent intent) {
		String componentName = getComponentName(intent);
		if (componentName == null) {
			return false;
		}
		boolean result = false;
		ProcessBuilder pb = new ProcessBuilder().command("am", "start", "--user", "0", componentName);
		try {
			pb.redirectErrorStream(true);
			Process process = pb.start();
			//
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.toLowerCase(Locale.US);
				if (line.contains("starting: intent")) {
					result = true;
				} else if (line.contains("error") || line.contains("exception")) {
					result = false;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return result;
	}
	
	private static Intent getLaunchIntend(Context context, String packageName) {
		PackageManager packageManager = context.getPackageManager();
		if (packageManager == null) {
			return null;
		}
		try {
			return packageManager.getLaunchIntentForPackage(packageName);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static String getComponentName(Intent intent) {
		if (intent == null) {
			return null;
		}
		ComponentName cn = intent.getComponent();
		return String.format("%s/%s", cn.getPackageName(), cn.getClassName());
	}

	public static int executeCommand(String cmd) {
		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec(cmd);
			int ret = process.waitFor();
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				if (process != null) {
					process.destroy();
				}
			} catch (Exception e) {
			}
		}
	}
	
}
