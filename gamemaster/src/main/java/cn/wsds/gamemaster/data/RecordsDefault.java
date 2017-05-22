package cn.wsds.gamemaster.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.subao.utils.Misc;

import android.util.JsonReader;
import android.util.JsonWriter;

/**
 * 双链路数据类 Created by hujd on 16-5-6.
 */
public class RecordsDefault<T> implements Iterable<T> {

	/**
	 * 负责将序列化的类
	 */
	public interface RecordSerializer<T> {
		void writeToJson(T rec, JsonWriter writer) throws IOException;

		T createFromJson(JsonReader reader);
	}

	private final List<T> list = new ArrayList<T>(8);

	@Override
	public Iterator<T> iterator() {
		return this.list.iterator();
	}

	/**
	 * 加载
	 */
	public void load(InputStream input, RecordSerializer<T> serializer) throws IOException {
		JsonReader reader = new JsonReader(new BufferedReader(new InputStreamReader(input), 4096)); //new FileReader(FileUtils.getDataFile(fileName)));
		try {
			reader.setLenient(true);
			this.loadFromJson(reader, serializer);
		} finally {
			Misc.safeClose(reader);
		}
	}

	/**
	 * RecordList的序列化器实现
	 */
	public void save(OutputStream output, RecordSerializer<T> serializer) throws IOException {
		JsonWriter writer = null;
		try {
			writer = new JsonWriter(new BufferedWriter(new OutputStreamWriter(output), 4096));
			this.writeToJson(writer, serializer);
			writer.flush();
		} finally {
			Misc.safeClose(writer);
		}
	}

	private void loadFromJson(JsonReader reader, RecordSerializer<T> serializer) throws IOException {
		list.clear();
		try {
			reader.beginArray();
			while (reader.hasNext()) {
				T rec = serializer.createFromJson(reader);
				this.add(rec);
			}
			reader.endArray();
		} catch (RuntimeException e) {
			throw new IOException();
		}
	}

	private void writeToJson(JsonWriter writer, RecordSerializer<T> serializer) throws IOException {
		writer.beginArray();
		for (T rec : this.list) {
			serializer.writeToJson(rec, writer);
		}
		writer.endArray();
	}

	public void add(T rec) {
		if (rec != null) {
			list.add(rec);
		}
	}

	public void clear() {
		this.list.clear();
	}

	public int getGameCount() {
		return this.list.size();
	}
}
