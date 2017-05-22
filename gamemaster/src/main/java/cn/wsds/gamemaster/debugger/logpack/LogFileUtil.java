package cn.wsds.gamemaster.debugger.logpack;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import cn.wsds.gamemaster.ErrorReportor;
import cn.wsds.gamemaster.debugger.logpack.RuntimeLogCatcher.OnLogUpdateListener;

import com.subao.utils.FileUtils;


public class LogFileUtil {
	
	private static final String LOG_TEMP_DIRECTORY_NAME = "logtmp";
	public static final String ZIPOUT_FILE_NAME = "zippack.zip";
	public static final String NAME_FILE_INSTALL_APP = "install.txt";
	/** 日志文件名 */
	public static final String NAME_FILE_RUNTIME_LOG = "runtime.log";
	public static final String NAME_FILE_CONFIG = "config.html";
	public static final String NAME_FILE_PROXY = "proxy.html";
	public static final String NAME_FILE_DESC = "desc.txt";
	
	/**
	 * 保存日志
	 * 当日志有更新时追加保存
	 * 
	 */
	public static final class WriteRuntimeLog implements Closeable, OnLogUpdateListener {
		/** 日志数据流 */
		private FileOutputStream out;

		public WriteRuntimeLog() throws IOException {
			File file = new File(getLogTmpDir(true), NAME_FILE_RUNTIME_LOG);
			File parentFile = file.getParentFile();
			if(!parentFile.exists()){
				parentFile.mkdirs();
			}
			out = new FileOutputStream(file, true);
		}
		
		@Override
		public void onLogUpdate(String log) {
			try {
				out.write(log.getBytes());
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void close() throws IOException {
			if (out != null) {
				out.close();
				out = null;
			}
		}
	}
	
	/**
	 * 文件压缩为 ZIP包
	 * @param outzipName 
	 * @param path 
	 * @return
	 */
	public static boolean packZip(String outzipName,File... file) {
		File outzipFile = getTempLogFile(outzipName);//
		ZipOutputStream out = null;
		try {
			out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outzipFile)));
			int buffer = 1024;
			byte data[] = new byte[buffer];
			for (File cf : file) {
				if(!cf.exists()){
					continue;
				}
				BufferedInputStream origin = new BufferedInputStream(new FileInputStream(cf), buffer);
				ZipEntry entry = new ZipEntry(cf.getName());
				out.putNextEntry(entry);
				int count;
				while ((count = origin.read(data, 0, buffer)) != -1) {
					out.write(data, 0, count);
				}
				origin.close();
			}
		} catch (Exception e) { 
			e.printStackTrace();
			return false;
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	public static void writeLogTmp(String fileName, byte[] data) {
		FileUtils.write(new File(getLogTmpDir(true),fileName), data);
	}
	private static File getLogTmpDir(boolean createWhenNotExists){
		File dir = new File(FileUtils.getDataDirectory(),LOG_TEMP_DIRECTORY_NAME);
		if (createWhenNotExists && !(dir.exists() && dir.isDirectory())){
			dir.mkdirs();
		}
		return dir;
	}

	public static File getTempLogFile(String name) {
		return new File(getLogTmpDir(true), name);
	}

	public static void clearFile() {
		FileUtils.deleteFileOrDirectory(getLogTmpDir(false));
		ErrorReportor.deleteErrorLogFile();
	}

	public static void saveErrorDesc(String desc) {
		writeLogTmp(NAME_FILE_DESC, desc.getBytes());
	}

	public static void clearErrorDesc() {
		getTempLogFile(NAME_FILE_DESC).delete();
	}
}
