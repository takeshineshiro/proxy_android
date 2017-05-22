package cn.wsds.gamemaster.data;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.os.Environment;
import android.util.JsonReader;
import android.util.JsonWriter;

import com.subao.utils.Misc;

public class PersistConfig {

	private static final PersistConfig instance = new PersistConfig();

	public static PersistConfig getInstance() {
		return instance;
	}

	private static final String DIR_NAME = "AC47158C-F559-401D-9F4C-37355FED629A";
	private static final String FILE_NAME = ".g_data";

	private static final String KEY_DEVICE_INFO_REPORTED = "DIR";
	private static final String KEY_ACTIVATION_REPORTED = "AR";

	private boolean deviceInfoReported;
	private boolean activationReported;

	private PersistConfig() {
		load();
	}

	private static File createFile() {
		File dir = new File(Environment.getExternalStorageDirectory(), DIR_NAME);
		if (!dir.exists() || !dir.isDirectory()) {
			dir.mkdirs();
		}
		return new File(dir, FILE_NAME);
	}

	private void load() {
		JsonReader reader = null;
		try {
			reader = new JsonReader(new FileReader(createFile()));
			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (KEY_DEVICE_INFO_REPORTED.equals(name)) {
					this.deviceInfoReported = reader.nextBoolean();
				} else if (KEY_ACTIVATION_REPORTED.equals(name)) {
					this.activationReported = reader.nextBoolean();
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();
		} catch (Exception ex) {
//			ex.printStackTrace();
		} finally {
			Misc.safeClose(reader);
		}
	}

	private void save() {
		JsonWriter writer = null;
		try {
			writer = new JsonWriter(new FileWriter(createFile()));
			writer.beginObject();
			writer.name(KEY_DEVICE_INFO_REPORTED).value(this.deviceInfoReported);
			writer.name(KEY_ACTIVATION_REPORTED).value(this.activationReported);
			writer.endObject();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			Misc.safeClose(writer);
		}
	}

	/** 设备详情是否已经上报过？ */
	public boolean getDeviceInfoReported() {
		return this.deviceInfoReported;
	}

	/** 设置：设备详情已经上报过 */
	public void setDeviceInfoReported() {
		if (!this.deviceInfoReported) {
			this.deviceInfoReported = true;
			save();
		}
	}
	
	/** BACKSTAGE_ACTIVATION 统计事件是否已上报过？ */
	public boolean getActivationReported() {
		return this.activationReported;
	}
	
	public void setActivationReported() {
		if (!this.activationReported) {
			this.activationReported = true;
			save();
		}
	}
}
