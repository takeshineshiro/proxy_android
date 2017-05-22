package cn.wsds.gamemaster.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import com.subao.common.data.ParallelConfigDownloader;
import com.subao.utils.FileUtils;

import android.util.JsonReader;
import android.util.JsonWriter;
import cn.wsds.gamemaster.app.GameManager;

/**
 * Created by hujd on 16-6-3.
 */
public class DoubleAccelTimeRecords implements GameManager.Observer, Iterable<DoubleAccelTimeRecords.Record> {

	private static final DoubleAccelTimeRecords INSTANCE = new DoubleAccelTimeRecords();

	private final RecordsDefault<Record> timeRecords = new RecordsDefault<Record>();
	private final RecordSerializerImplement mRecordSerializerImplement = new RecordSerializerImplement();

	private static class Persistence {

		private static final String FILE_NAME = "double_link_time";

		private static File getFile() {
			return FileUtils.getDataFile(FILE_NAME);
		}

		public static InputStream openInputStream() throws IOException {
			return new FileInputStream(getFile());
		}

		public static OutputStream openOutputStream() throws IOException {
			return new FileOutputStream(getFile(), false);
		}

	}

	public static DoubleAccelTimeRecords getInstance() {
		return INSTANCE;
	}

	private DoubleAccelTimeRecords() {
		try {
			load(Persistence.openInputStream());
		} catch (IOException e) {
		}
	}

	/**
	 * 从持久化介质里加载
	 */
	void load(InputStream input) throws IOException {
		timeRecords.load(input, mRecordSerializerImplement);
	}

	/**
	 * 供测试用例使用：清空
	 */
	void clear() {
		timeRecords.clear();
	}

	/**
	 * 记录里有多少个游戏了？
	 * @return 记录里游戏数量
	 */
	public int getGameCount() {
		return timeRecords.getGameCount();
	}
	
	public long getTotalTime() {
		long totalTime = 0;
		for (Record record : this.timeRecords) {
			totalTime += record.getTime();
		}
		return totalTime;
	}

	/**
	 * 添加一条记录
	 * 
	 * @param packageName
	 *            包名，不能为null
	 * @param timeSeconds
	 *            时长，单位：秒
	 */
	public void addRecord(String packageName, long timeSeconds) {
		OutputStream output;
		try {
			output = Persistence.openOutputStream();
		} catch (IOException e) {
			output = null;
		}
		addRecord(packageName, timeSeconds, output);
	}

	void addRecord(String packageName, long timeSeconds, OutputStream output) {
		if (packageName == null) {
			return;
		}
		Record found = null;
		for (Record exists : timeRecords) {
			if (packageName.equals(exists.getPackageName())) {
				found = exists;
				break;
			}
		}
		if (found == null) {
			found = new Record(packageName, timeSeconds);
			timeRecords.add(found);
		} else {
			found.addTime(timeSeconds);
		}
		if (output != null) {
			try {
				timeRecords.save(output, mRecordSerializerImplement);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onAccelTimeChanged(int seconds) {}

	@Override
	public void onGameListUpdate() {

	}

	@Override
	public void onDoubleAccelTimeChanged(String packageName, int seconds) {
		if (ParallelConfigDownloader.isPhoneParallelSupported()) {
			addRecord(packageName, seconds);
		}
	}
	
	@Override
	public Iterator<Record> iterator() {
		return this.timeRecords.iterator();
	}
	
	public static final class Record {
		private final String packageName; //游戏包名
		private long time; //并联加速时长

		public Record(String packageName, long time) {
			this.packageName = packageName;
			this.time = time;
		}
		
		public Record(Record other) {
			this.packageName = other.packageName;
			this.time = other.time;
		}

		public String getPackageName() {
			return packageName;
		}

		public long getTime() {
			return time;
		}

		public void addTime(long deltaTime) {
			this.time += deltaTime;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (o == this) {
				return true;
			}
			if (!(o instanceof Record)) {
				return false;
			}
			Record other = (Record)o;
			if (this.time != other.time) {
				return false;
			}
			if (this.packageName == null) {
				return other.packageName == null;
			}
			return this.packageName.equals(other.packageName);
		}
	}

	private static class RecordSerializerImplement implements RecordsDefault.RecordSerializer<Record> {
		private static final String JSON_KEY_PACKAGE_NAME = "package_name";
		private static final String JSON_KEY_TIME = "time";

		@Override
		public void writeToJson(Record record, JsonWriter writer) throws IOException {
			writer.beginObject();
			writer.name(JSON_KEY_PACKAGE_NAME).value(record.packageName);
			writer.name(JSON_KEY_TIME).value(record.time);
			writer.endObject();
		}

		@Override
		public Record createFromJson(JsonReader reader) {
			String packageName = null;
			long time = 0;
			try {
				reader.beginObject();
				while (reader.hasNext()) {
					String name = reader.nextName();
					if (JSON_KEY_PACKAGE_NAME.equals(name)) {
						packageName = reader.nextString();
					} else if (JSON_KEY_TIME.equals(name)) {
						time = reader.nextLong();
					} else {
						reader.skipValue();
					}
				}
				reader.endObject();
			} catch (RuntimeException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			if (packageName != null) {
				return new Record(packageName, time);
			} else {
				return null;
			}
		}
	}
}
