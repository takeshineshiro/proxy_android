package cn.wsds.gamemaster.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.text.TextUtils;
import android.util.JsonReader;

import com.subao.common.data.AccelGame;
import com.subao.utils.FileUtils;

class AccelGameMap {

	//	private static final String TAG = LogTag.GAME;

	private static final Locale locale = Locale.US;

	private HashMap<String, AccelGame> mapByLabel;

	private final String[] blackWordList;

	private final BadGames badGames;

	private static class BadGames {
		private String[] badLabel;
		private String[] badPackageName;

		BadGames(Context context) {
			JsonReader reader = null;
			try {
				reader = new JsonReader(new InputStreamReader(FileUtils.getAssetDataStream(context, "bad_game")));
				reader.beginObject();
				while (reader.hasNext()) {
					String name = reader.nextName();
					if ("bad_label".equals(name)) {
						badLabel = loadStringList(reader);
					} else if ("bad_package".equals(name)) {
						badPackageName = loadStringList(reader);
					} else {
						reader.skipValue();
					}
				}
				reader.endObject();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (RuntimeException e) {
				e.printStackTrace();
			} finally {
				com.subao.common.Misc.close(reader);
			}
		}

		private static String[] loadStringList(JsonReader reader) throws IOException {
			List<String> list = new ArrayList<String>(32);
			reader.beginArray();
			while (reader.hasNext()) {
				String s = reader.nextString().trim();
				if (s.length() > 0) {
					list.add(s);
				}
			}
			reader.endArray();
			if (!list.isEmpty()) {
				String[] result = new String[list.size()];
				return list.toArray(result);
			}
			return null;
		}

		public boolean isLabelInclude(String appLabel) {
			for (String black : this.badLabel) {
				if (black.equals(appLabel)) {
					return true;
				}
			}
			return false;
		}

		public boolean isPackageNameInclude(String packageName) {
			if (!TextUtils.isEmpty(packageName)) {
				String lowerPackgaeName = packageName.toLowerCase(locale);
				for (String black : this.badPackageName) {
					if (black.equals(lowerPackgaeName)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	AccelGameMap(Context context) {
		this.blackWordList = loadBlackWordList(context);
		this.badGames = new BadGames(context);
	}

	private static String[] loadBlackWordList(Context context) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(FileUtils.getAssetDataStream(context, "black_words")), 2048);
			Set<String> set = new HashSet<String>(256);
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				line = line.trim();
				if (line.length() > 0) {
					set.add(line);
				}
			}
			if (!set.isEmpty()) {
				String[] result = new String[set.size()];
				int i = 0;
				for (String s : set) {
					result[i] = s;
					++i;
				}
				return result;
			}
		} catch (IOException e) {} finally {
			com.subao.common.Misc.close(reader);
		}
		return null;
	}

	public void assign(Iterable<AccelGame> accelGameList, int count) {
		if (accelGameList == null || count <= 0) {
			this.mapByLabel = null;
			return;
		}
		this.mapByLabel = new HashMap<String, AccelGame>(count);
		for (AccelGame ag : accelGameList) {
			this.mapByLabel.put(ag.appLabel.toLowerCase(locale), ag);
		}
	}

	//	public void clear() {
	//		map.clear();
	//	}

	//	public int size() {
	//		return this.map.size();
	//	}
	//
	//	public AccelGame[] valuesToArray() {
	//		int size = map.size();
	//		AccelGame[] result = new AccelGame[size];
	//		return this.map.values().toArray(result);
	//	}

	/** 根据游戏包名和名字查找相应的{@link AccelGame} */
	public AccelGame findAccelGame(String packageName, String appLabel) {
		//		if (Logger.isLoggableDebug(TAG)) {
		//			Logger.d(TAG, String.format("Try to find accel game with \"%s\" and \"%s\"", packageName, appLabel));
		//		}
		// 过滤Label在黑名单里的
		if (badGames.isLabelInclude(appLabel)) {
			//			Logger.d(TAG, "Label in bad name list");
			return null;
		}
		//
		// 过滤PackageName在黑名单里的
		if (badGames.isPackageNameInclude(packageName)) {
			//			Logger.d(TAG, "Package name in bad name list");
			return null;
		}
		//
		// Label转换为小写（便于匹配）
		String lowerAppLabel = appLabel.toLowerCase(locale);
		//
		// 先精确匹配
		if (mapByLabel != null) {
			AccelGame found = mapByLabel.get(lowerAppLabel);
			if (found != null) {
				return found;
			}
		}
		//
		// 如果关键字小于等于三个字符，不再进行模糊匹配了
		if (lowerAppLabel.length() <= 3) {
			//			Logger.d(TAG, "Label length less than 3");
			return null;
		}
		//
		// 进行模糊匹配
		if (mapByLabel != null) {
			Iterator<Map.Entry<String, AccelGame>> it = this.mapByLabel.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, AccelGame> entry = it.next();
				String key = entry.getKey();
				int keyLen = key.length();
				if (keyLen <= 2) {
					continue;
				}
				AccelGame accelGame = entry.getValue();
				// 游戏名是3个ASCII字符的不进行模糊匹配
				if (accelGame.isLabelThreeAsciiChar) {
					continue;
				}
				// 只允许精确匹配的，跳过
				if (accelGame.needExactMatch()) {
					continue;
				}
				if (lowerAppLabel.contains(key)) {
					if (doesAppLabelIncludeBlackWord(lowerAppLabel)) {
						return null;
					} else {
						return accelGame;
					}
				}
			}
		}
		//
		// 没找到，返回null
		//		Logger.d(TAG, "Not found");
		return null;
	}

	/**
	 * 判断给定的游戏名称，是否含有“黑词”（即“助手”“攻略”一类的需要过滤的词）
	 * 
	 * @param appLabel
	 *            游戏名字
	 * @return true表示游戏名字里含有“黑词”，否则返回false
	 */
	public boolean doesAppLabelIncludeBlackWord(String appLabel) {
		for (String black : blackWordList) {
			if (appLabel.contains(black)) {
				return true;
			}
		}
		return false;
	}
}
