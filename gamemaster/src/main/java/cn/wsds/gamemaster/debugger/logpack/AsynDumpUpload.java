package cn.wsds.gamemaster.debugger.logpack;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import android.os.AsyncTask;
import cn.wsds.gamemaster.ErrorReportor;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.tools.DataUploader;
import cn.wsds.gamemaster.tools.DataUploader.OnUploadCompletedCallback;

import com.subao.common.net.Http;
import com.subao.data.InstalledAppInfo;
import com.subao.utils.FileUtils;
import com.subao.utils.SubaoHttp;

/**
 * 日志管理
 */
public class AsynDumpUpload extends AsyncTask<OnUploadCompletedCallback, Boolean, Boolean> {

	private final String desc;

	public AsynDumpUpload(String desc) {
		this.desc = desc;
	}

	public void upload(OnUploadCompletedCallback callback) {
		executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor(), callback);
	}

	@Override
	protected Boolean doInBackground(OnUploadCompletedCallback... params) {
		Thread.currentThread().setName("AsynDumpUpload");
		//dump文件
		dumpInstallList();
		if (!dumpProxyContent())
			return false;
		if (!dumpConfigContent()) {
			return false;
		}
		// 文件打包上传
		packFile();
		byte[] data = FileUtils.read(LogFileUtil
			.getTempLogFile(LogFileUtil.ZIPOUT_FILE_NAME));
		OnUploadCompletedCallback callback = params[0];
		DataUploader.getInstance().addDebugDump(data, this.desc, callback);
		return true;
	}

	/**
	 * 打包代理内容
	 * 
	 * @return
	 */
	private boolean dumpProxyContent() {
		String url = "http://127.0.0.1:18900/proxy";
		String name = LogFileUtil.NAME_FILE_PROXY;
		return dumpContent(url, name);
	}

	/**
	 * 打包配置内容
	 * 
	 * @return
	 */
	private boolean dumpConfigContent() {
		String url = "http://127.0.0.1:18900/config";
		String name = LogFileUtil.NAME_FILE_CONFIG;
		return dumpContent(url, name);
	}

	/**
	 * @param url
	 * @param name
	 * @return
	 */
	private boolean dumpContent(String url, String name) {
		try {
			com.subao.common.net.Http.Response response = SubaoHttp.createHttp().doGet(new URL(url), Http.ContentType.ANY.str);
			if (response.code == 200 && response.data != null) {
				LogFileUtil.writeLogTmp(name, response.data);
				return true;
			}
		} catch (IOException e) {
		}
		return false;
	}

	/**
	 * 打包已安装app信息
	 */
	private void dumpInstallList() {
		InstalledAppInfo[] infoList = GameManager.getInstance()
			.getInstalledApps();
		if (infoList == null || infoList.length == 0) {
			return;
		}
		StringBuilder sb = new StringBuilder(infoList.length * 128);
		for (InstalledAppInfo info : infoList) {
			// 格式UID，包名，中文名
			sb.append(info.getUid());
			sb.append(',');
			sb.append(info.getPackageName());
			sb.append(',');
			sb.append(info.getAppLabel());
			sb.append('\n');
		}
		byte[] data = sb.toString().getBytes();
		String fileName = LogFileUtil.NAME_FILE_INSTALL_APP;
		LogFileUtil.writeLogTmp(fileName, data);
	}

	/**
	 * 打包各文件
	 * 
	 * @return 打包结果
	 */
	private boolean packFile() {
		// 创建文件数组
		File proxyLogFile = FileUtils.getProxyLogFile();
		File runtimeLogFile = LogFileUtil
			.getTempLogFile(LogFileUtil.NAME_FILE_RUNTIME_LOG);
		File proxyContentFile = LogFileUtil
			.getTempLogFile(LogFileUtil.NAME_FILE_PROXY);
		File configContentFile = LogFileUtil
			.getTempLogFile(LogFileUtil.NAME_FILE_CONFIG);
		File installAppFile = LogFileUtil
			.getTempLogFile(LogFileUtil.NAME_FILE_INSTALL_APP);
		//		File descFile = LogFileUtil.getTempLogFile(LogFileUtil.NAME_FILE_DESC);
		File[] fileArray = new File[] {
			ErrorReportor.getErrorLogFile(),
			proxyLogFile,
			runtimeLogFile,
			proxyContentFile,
			configContentFile,
			installAppFile
		};
		return LogFileUtil.packZip(LogFileUtil.ZIPOUT_FILE_NAME, fileArray);
	}
}
