package cn.wsds.gamemaster.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.subao.utils.FileUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RootUtil {
	private final static boolean LOG = false;
	private final static String TAG = "RootUtil";

	private final static String SU_DIRECTORY = "/system/bin";
	private final static String SU_NAME = "com.subao.su";
	public final static String INJECT_NAME = "com.subao.inj";
	
	@SuppressLint("SdCardPath")
	private static final String[] SHARED_DIR_LIST = {
		"/data/subao_gamemaster", "/data/data/cn.wsds.gamemaster/subao_gamemaster", "/sdcard/subao_gamemaster"
	};
	private static final String[] PATH_LIST = {
		"/system/bin/", "/system/xbin/", "/sbin/", "/data/local/xbin/", "/data/local/bin/", "/system/sd/xbin/",
		"/system/bin/failsafe/", "/data/local/"
	};
	private static String injectFile = null; // inject文件拷贝到/data/data/里，全路径

	private final static int EXEC_FAILURE = -1;
	private final static int DAEMON_NOT_RUNNING = 254; // 守护进程没运行，必须和自定义su约定好，不要乱改！！！

	private static Boolean alreadyRoot;
	private static Context context;
	private static boolean isWaitingAuthorize = false;
	private static boolean daemonRunning = false;

	public static int SDK_VERSION_FOR_DAEMON = 14;

	public static enum RequestRootResult {
		Reject, 	// 用户点拒绝了
		Succeed, 	// 用户点授权了，成功获取Root权限
		Failed		// 用户点授权了，但获取Root权限失败
	}

	public static interface OnRequestRootListener {

		public void onRequestRoot(RequestRootResult result);
	}

	public static interface OnClearRootListener {

		public void onClearRoot(boolean result);
	}

	public static interface OnExecCommandListener {
		public void onExecCommand(int result);
	}

	/**
	 * 应用启动时调用
	 * 
	 * @param c
	 */
	public static void init(Context c) {
		context = c.getApplicationContext();
	}

	/** 判断手机是否root，不弹出root请求框 */
	public static boolean isRoot() {
		return false ;
		/*if (alreadyRoot == null) {
			for (String path : PATH_LIST) {
				String filename = path + "su";
				if (isFileExists(filename) && isExecutable(filename)) {
					alreadyRoot = true;
					return true;
				}
			}
			alreadyRoot = false;
		}

		return alreadyRoot.booleanValue();*/
	}

	/**
	 * 是否已获得Root权限了
	 * 
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static boolean isGotRootPermission() {
		String fullPath = String.format("%s/%s", SU_DIRECTORY, SU_NAME);
		final File file = new File(fullPath);
		if (!file.exists() || !isExecutable(fullPath)) {
			return false;
		}

		int sdkVer = android.os.Build.VERSION.SDK_INT;
		if (sdkVer < SDK_VERSION_FOR_DAEMON) {
			return true;
		}

		// 需要开守护进程
		Thread t = new Thread() {
			public void run() {
//				daemonRunning = VPNJni.isDaemonRunning(); // 检测守护进程是否在运行
				daemonRunning = false; // 检测守护进程是否在运行

			};
		};
		t.start();

		try {
			t.join(2000); // 超时
			if (t.isAlive()) {
				Log.e(TAG, "isGotRootPermission:检测守护进程的线程超时没返回，关闭之");
				t.stop();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return daemonRunning;

		/////////////////////可能卡一会/////////////////////
		//		List<String> commands = new ArrayList<String>();
		//		commands.add("ls");
		//		int ret = innerExecCommands(commands);
		//		if (ret == DAEMON_NOT_RUNNING) { // 守护进程还没启动
		//			return false;
		//		} else if (ret == EXEC_SUCCESS) { // 执行成功
		//			return true;
		//		} else { // 执行失败
		//			return false;
		//		}
	}

	private static int getFileVersion(String name) {
		int ver = 0;
		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec(name + " -v");
			//os = new DataOutputStream(process.getOutputStream());
			//os.writeBytes("exit\n");
			//os.flush();

			BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String str = in.readLine();
			if (str != null) {
				ver = Integer.parseInt(str);
			}

			return ver;
			//			ret = process.waitFor();
			//			return ret;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				if (process != null) {
					process.destroy();
				}
			} catch (Exception e) {}
		}

		return ver;
	}

	/**
	 * su文件或inj文件是否有更新版本
	 * 
	 * @return
	 */
	public static boolean isFileUpdated() {
		String newSuFile = extractFile(SU_NAME);
		String newInjFile = extractFile(INJECT_NAME);
		if (newSuFile == null || newInjFile == null) {
			Log.e(TAG, "提取文件失败！！！");
			return false;
		}

		if (getFileVersion(SU_NAME) < getFileVersion(newSuFile)) {
			return true;
		}

		if (getFileVersion(INJECT_NAME) < getFileVersion(newInjFile)) {
			return true;
		}

		return false;
	}

	/**
	 * 以root权限，在另一个线程中执行命令
	 * 
	 * @param commands
	 *            每个元素都是一条命令："cp source.txt target.txt"
	 */
	public static void postExecuteInThread(Collection<String> commands, OnExecCommandListener listener) {
		if (isRoot()) {
			ExecCommandTask.postExecuteInThread(commands, listener);
		} else {
			Log.e(TAG, "postExecuteInThread(), not root!");
			if (listener != null) {
				listener.onExecCommand(-12345678);
			}
		}
	}
	
	/**
	 * 以root权限，在另一个线程中执行命令
	 */
	public static void postExecuteInThread(String cmd, OnExecCommandListener listener) {
		ArrayList<String> commands = new ArrayList<String>(1);
		commands.add(cmd);
		postExecuteInThread(commands, listener);
	}

	

	/**
	 * 请求Root权限，把我们的su拷贝到系统bin目录
	 * 
	 * @param listener
	 */
	public static void requestRoot(OnRequestRootListener listener) {
		if (LOG) {
			Log.i(TAG, "requestRoot：请求root权限...");
		}

		if (listener == null) {
			Log.e(TAG, "requestRoot:listener == null！！！");
			return;
		}

		if (!isRoot()) {
			//Log.e(TAG, "requestRoot:手机还没Root过！！！");
			listener.onRequestRoot(RequestRootResult.Failed);
			return;
		}

		RequestRootTask.execute(listener);
	}

	/**
	 * 清除Root权限
	 * 
	 * @param listener
	 */
	public static void clearRoot(OnClearRootListener listener) {
		if (listener == null) {
			Log.e(TAG, "clearRoot:listener == null！！！");
			return;
		}

		if (!isRoot()) {
			//Log.e(TAG, "clearRoot:手机还没Root过！！！");
			listener.onClearRoot(false);
			return;
		}

		ClearRootTask.execute(listener);
	}

	/**
	 * 是否正在弹窗，等待用户授权
	 * 
	 * @return
	 */
	public static boolean isWaitingAuthorize() {
		return isWaitingAuthorize;
	}

	/**
	 * 增加观察者，授权完毕后接收通知
	 * 
	 * @param listener
	 */
	public static void addRequestRootListener(OnRequestRootListener listener) {
		if (listener != null) {
			RequestRootTask.addRequestRootListener(listener);
		}
	}

//	/**
//	 * 注入进程，测试用的
//	 * 
//	 * @param pids
//	 * @param listener
//	 */
//	public static void injectPIDs(List<String> pids, OnExecCommandListener listener) {
//		String soName = "libhook.so"; // assets资源文件名
//		String soFile = extractFile(soName);
//
//		List<String> commands = new ArrayList<String>();
//		for (String pid : pids) {
//			String cmd = String.format("%s %s %s", injectName, pid, soFile); //test
//			commands.add(cmd);
//		}
//
//		postExecuteInThread(commands, listener);
//	}

	private static boolean isFileExists(String filename) {
		try {
			File file = new File(filename);
			return file.exists();
		} catch (Exception ex) {
			return false;
		}
	}

	// 是否可以执行
	private static boolean isExecutable(String filePath) {
		try {
			File file = new File(filePath);
			boolean canExecute = file.canExecute();
			long length = file.length();
			return canExecute && length > 0;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	// 调用自定义su来执行命令，绕过系统su
	private static int innerExecCommands(Iterable<String> commands) {
		int ret = EXEC_FAILURE;
		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec(SU_NAME); // 自定义su
			os = new DataOutputStream(process.getOutputStream());
			for (String cmd : commands) {
				if (cmd != null) {
					os.writeBytes(cmd + "\n");
					if (LOG) {
						Log.i(TAG, String.format("cmd:%s\n", cmd));
					}
				}
			}
			os.writeBytes("exit\n");
			os.flush();

			ret = process.waitFor();
			if (ret == DAEMON_NOT_RUNNING) {
				Log.e(TAG, "com.subao.su守护进程没运行");
			} else if (ret == 0) {
				Log.i(TAG, String.format("EXEC_SUCCESS"));
			} else {
				Log.e(TAG, String.format("EXEC_FAILURE:ret:%d", ret));
			}

			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return DAEMON_NOT_RUNNING;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				if (process != null) {
					process.destroy();
				}
			} catch (Exception e) {}
		}
	}

	/**
	 * 以root权限来执行命令
	 * 
	 * @param commands
	 *            每个元素都是一条命令："cp source.txt target.txt"
	 * @return 是否执行成功
	 */
	public static int execCommands(Iterable<String> commands) {
		if (!isRoot()) {
			//Log.e(TAG, "execCommands:手机还没Root过！！！");
			return -1;
		}

		int sdkVer = android.os.Build.VERSION.SDK_INT;
		if (sdkVer < SDK_VERSION_FOR_DAEMON) {
			//Log.i(TAG, "execCommands:不需要开守护进程");
			String fullPath = String.format("%s/%s", SU_DIRECTORY, SU_NAME);
			if (!isExecutable(fullPath) && copyRunMySU(false) != RequestRootResult.Succeed) {
				Log.e(TAG, "execCommands:拷贝自定义su失败！！！");
				return -1;
			}
			return innerExecCommands(commands);
		}

		// 需要开守护进程
		int count = 0;
		while (count++ < 2) {
			int ret = innerExecCommands(commands);
			if (ret == DAEMON_NOT_RUNNING) { // 守护进程还没启动
				if (copyRunMySU(true) == RequestRootResult.Succeed) {
					Log.i(TAG, "execCommands:启动自定义su守护进程成功！！！");
					continue; // 启动成功，重新执行命令
				} else {
					if (LOG) {
						Log.e(TAG, "execCommands:启动自定义su守护进程失败！！！");
					}
					return -1; // 启动失败
				}
			} else {
				return ret; // 执行结果
			}
		}
		return -1;
	}

	// 把自定义su拷贝到临时文件，返回文件全路径
	private static String extractFile(String filename) {
		File file = new File(context.getFilesDir(), filename);
		if (FileUtils.extractFileFromAssets(context, filename, file, "777")) {
			return file.getAbsolutePath();
		} else {
			return null;
		}
	}

	// 启动su守护进程
	static RequestRootResult copyRunMySU(boolean run) {
		RequestRootResult ret = RequestRootResult.Failed;

		String tempSUFile = extractFile(SU_NAME); // 提取自定义su到临时文件
		injectFile = extractFile(INJECT_NAME);
		if (tempSUFile == null || injectFile == null) {
			Log.e(TAG, "提取文件失败！！！");
			return RequestRootResult.Failed;
		}

		int pid = -1;
		int sdkVer = android.os.Build.VERSION.SDK_INT;
		if (sdkVer >= SDK_VERSION_FOR_DAEMON) { // 有守护进程
			pid = getPidFromName(SU_NAME);
		}

		Process process = null;
		DataOutputStream os = null;

		try {
			process = Runtime.getRuntime().exec("su"); // 申请root权限
			os = new DataOutputStream(process.getOutputStream());
			if (pid > 0) {
				os.writeBytes(String.format("kill -9 %d\n", pid)); // 杀守护进程
			}
			os.writeBytes("mount -o remount,rw /system\n"); // 可读写
			os.writeBytes(String.format("cat %s > %s/%s\n", tempSUFile, SU_DIRECTORY, SU_NAME)); // 拷贝到/system/bin/com.subao.su
			os.writeBytes(String.format("cat %s > %s/%s\n", injectFile, SU_DIRECTORY, INJECT_NAME)); // 拷贝到/system/bin/com.subao.inj
			os.writeBytes(String.format("chmod 6777 %s/%s\n", SU_DIRECTORY, SU_NAME)); // 修改com.subao.su文件权限
			os.writeBytes(String.format("chmod 777 %s/%s \n", SU_DIRECTORY, INJECT_NAME)); // 修改com.subao.inj文件权限
			if (run) {
				os.writeBytes(String.format("%s --daemon\n", SU_NAME)); // 启动守护进程
			}
			os.writeBytes("exit\n");
			os.flush();

			isWaitingAuthorize = true;
			boolean ok = process.waitFor() == 0 ? true : false;
			isWaitingAuthorize = false;

			if (ok) { // 用户授权了；但有些手机，用户拒绝了也返回true
				ret = isGotRootPermission() ? RequestRootResult.Succeed : RequestRootResult.Failed;
			} else {
				ret = RequestRootResult.Reject; // 用户拒绝了
			}
			if (LOG) {
				Log.i(TAG, String.format("copyRunMySU:ok:%d", ok ? 1 : 0));
			}
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return RequestRootResult.Failed;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				if (process != null) {
					process.destroy();
				}
			} catch (Exception e) {}
		}
	}

	public static int createSharedDir() {
		List<String> commands = new ArrayList<String>();
		for (String path : SHARED_DIR_LIST) {
			commands.add(String.format("mkdir -p %s", path));
			commands.add(String.format("chmod 777 %s", path));
		}

		return innerExecCommands(commands);
	}

	/////////////////////////////////////////////////// 以下代码用来清除Root权限 /////////////////////////////////////////////////////////
	static boolean clearRoot() {
		int pid = -1;
		int sdkVer = android.os.Build.VERSION.SDK_INT;
		if (sdkVer >= SDK_VERSION_FOR_DAEMON) { // 有守护进程
			pid = getPidFromName(SU_NAME);
		}

		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec("su"); // 申请root权限
			os = new DataOutputStream(process.getOutputStream());
			if (pid > 0) {
				os.writeBytes(String.format("kill -9 %d\n", pid)); // 杀守护进程
			}
			os.writeBytes("mount -o remount,rw /system\n"); // 可读写
			os.writeBytes(String.format("rm %s/%s\n", SU_DIRECTORY, SU_NAME)); // 删除/system/bin/com.subao.su
			os.writeBytes(String.format("rm %s/%s\n", SU_DIRECTORY, INJECT_NAME)); // 删除/system/bin/com.subao.inj
			os.writeBytes("exit\n");
			os.flush();

			boolean ok = process.waitFor() == 0 ? true : false;
			if (ok) { // 有些手机，用户拒绝了也返回true
				ok = !isGotRootPermission();
			}
			if (LOG) {
				Log.i(TAG, String.format("clearRoot:ok:%d", ok ? 1 : 0));
			}
			return ok;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				if (process != null) {
					process.destroy();
				}
			} catch (Exception e) {}
		}
	}

	// 根据进程名获取进程pid
	private static int getPidFromName(String name) {
		int pid = -1;
		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec("ps");
			os = new DataOutputStream(process.getOutputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String str = null;
			while ((str = in.readLine()) != null) { // 遍历进程信息
				//Log.i("RootUtil", str);
				String[] cols = str.split("\\s+");
				if (cols.length < 2) {
					continue;
				}
				String pname = cols[cols.length - 1];
				if (pname.equals(name)) {
					pid = Integer.parseInt(cols[1]);
					break;
				}
			}

			return pid;
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
			} catch (Exception e) {}
		}
	}
	
	/**
	 * 是否可以使用Root模式？
	 * @return true表示可以使用Root，false表示不可以
	 */
	public static boolean canUseRootMode(){
		return android.os.Build.VERSION.SDK_INT < 21 && RootUtil.isRoot();
	}

}
