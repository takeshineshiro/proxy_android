package com.subao.common.data;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.SuBaoObservable;
import com.subao.common.utils.FileUtils;
import com.subao.common.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SubaoIdManager extends SuBaoObservable<SubaoIdManager.Observer> {

	public interface Observer {
		void onSubaoIdChange(String subaoId);
	}

	private static final String TAG = LogTag.DATA;
	private static final String FILENAME = ".sys";
	private static final int SUBAO_ID_CHARS = 32 + 4;

	public static final String EMPTY_SUBAO_ID = "00000000-0000-0000-0000-000000000000";
	private static final SubaoIdManager instance = new SubaoIdManager();

	private List<File> files;
	private String subaoId;

	public static SubaoIdManager getInstance() {
		return instance;
	}

	private SubaoIdManager() {}

	public void init(Context context) {
		try {
			if (files == null) {
				this.files = createFiles(context);
				setSubaoId(initSubaoId(files));
			}
		} catch (RuntimeException e) {
			Logger.w(TAG, "SubaoId init failed.", e);
		}
	}

	public boolean isSubaoIdValid() {
		return isSubaoIdValid(this.subaoId);
	}
	
	public static boolean isSubaoIdValid(String subaoId) {
		return subaoId != null
			&& subaoId.length() == SUBAO_ID_CHARS
			&& !EMPTY_SUBAO_ID.equals(subaoId);
	}

	private static boolean needLog() {
		return Logger.isLoggable(TAG, Log.DEBUG);
	}

	private static List<File> createFiles(Context context) {
		List<File> files = new ArrayList<File>(4);
		File dir = Environment.getExternalStorageDirectory();
		files.add(new File(dir, FILENAME));
		files.add(new File(new File(dir, "Android"), FILENAME));
		files.add(new File(new File(dir, "9C52E85A-374A-4709-866F-0E708AE2B727"), FILENAME));
		files.add(new File(context.getFilesDir(), FILENAME));
		return files;
	}

	private static String initSubaoId(List<File> files) {
		Pair<String, Integer> pair = readIdFromFiles(files);
		if (pair == null) {
			// 没有从存储介质里读到Subao ID
			Logger.d(TAG, "No SubaoId load, maybe first install");
			return null;
		}
		String result = pair.first;
		// 如果读到的ID数量不全，需要存一次盘（写所有文件）
		if (pair.second != files.size()) {
			saveIdToFiles(files, result);
		}
		return result;
	}

	/**
	 * 获取速宝 id
	 */
	public String getSubaoId() {
		return this.subaoId;
	}

	/**
	 * 设置速宝 id
	 */
	public synchronized void setSubaoId(String subaoId) {
		if (needLog()) {
			Logger.d(TAG, "set SubaoId: " + subaoId);
		}
		if (!com.subao.common.utils.StringUtils.isStringEqual(this.subaoId, subaoId)) {
			this.subaoId = subaoId;
			saveIdToFiles(files, subaoId);
			//
			List<Observer> observers = this.cloneAllObservers();
			if (observers != null) {
				for (Observer o : observers) {
					o.onSubaoIdChange(subaoId);
				}
			}
		}
	}

	/**
	 * 从各存储位置里读入SubaoId
	 */
	private static Pair<String, Integer> readIdFromFiles(List<File> files) {
		// 遍历所有文件并读取
		// 遍历完成后list里为读到的ID及其个数
		List<Pair<String, Integer>> list = new ArrayList<Pair<String, Integer>>(files.size());
		for (File file : files) {
			String id;
			try {
				byte[] data = FileUtils.read(file, 512);
				id = new String(data);
			} catch (IOException e) {
				id = StringUtils.EMPTY;
			}
			if (needLog()) {
				Logger.d(TAG, String.format("Load SubaoId [%s] from \"%s\"",
					id,
					file.getAbsolutePath()));
			}
			if (id.length() == SUBAO_ID_CHARS) {
				int i = list.size() - 1;
				while (i >= 0) {
					Pair<String, Integer> pair = list.get(i);
					if (com.subao.common.utils.StringUtils.isStringEqual(id, pair.first)) {
						// 找到了，数量加一
						list.set(i, new Pair<String, Integer>(pair.first, pair.second + 1));
						break;
					} else {
						--i;
					}
				}
				if (i < 0) {
					list.add(new Pair<String, Integer>(id, 1));
				}
			}
		}
		// 从list里选数量最多的返回
		Pair<String, Integer> result = null;
		for (Pair<String, Integer> pair : list) {
			if (result == null || result.second < pair.second) {
				result = pair;
			}
		}
		return result;
	}

	/**
	 * 把速宝Id存入到各文件里
	 */
	private static void saveIdToFiles(Iterable<File> files, String subaoId) {
		try {
			if (subaoId == null || subaoId.length() == 0) {
				clear(files);
				return;
			}
			for (File file : files) {
				boolean ok;
				try {
					FileUtils.write(file, subaoId.getBytes());
					ok = true;
				} catch (IOException e) {
					ok = false;
				}
				//
				if (needLog()) {
					Logger.d(TAG, "Save SubaoId to " + file.getAbsolutePath() + (ok ? " ok" : " failed"));
				}
			}
		} catch (RuntimeException e) {
			Logger.w(TAG, "Save subao id failed.", e);
		}
	}

	/**
	 * 调试用，清除SubaoId
	 */
	public void clear() {
		clear(files);
	}

	private static void clear(Iterable<File> files) {
		for (File file : files) {
			boolean ok = file.delete();
			if (needLog()) {
				Logger.d(TAG, String.format("Delete file \"%s\" %s", file.getAbsolutePath(), ok ? "OK" : "failed"));
			}
		}
	}
}
