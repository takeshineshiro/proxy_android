package com.subao.common.msg;

import android.content.Context;
import android.os.Build;
import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.utils.JsonUtils;
import com.subao.common.Misc;
import com.subao.common.utils.InfoUtils;

import java.io.IOException;

public class Message_DeviceInfo implements JsonSerializable {

	//手机型号，如：小米4，华为Mate7等
	private final String model;

	public String getModel() {
		return model;
	}

	//CPU主频
	private final int cpuSpeed;

	public int getCpuSpeed() {
		return cpuSpeed;
	}

	//CPU核数
	private final int cpuCore;

	public int getCpuCore() {
		return cpuCore;
	}

	//手机内存
	private final int memory;

	public int getMemory() {
		return memory;
	}

    private final String rom;

    public String getROM() {
        return this.rom;
    }

//	private static Message_DeviceInfo instance;

//	/**
//	 * 取已创建好的实例
//	 * @return 已创建好的实例，或null（如果先前没有调用过{@link #create(Context)}）
//     */
//	public static synchronized Message_DeviceInfo getInstance() {
//		return instance;
//	}

	public Message_DeviceInfo(String model, int cpuSpeed, int cpuCore, int memory, String rom) {
		this.model = model;
		this.cpuSpeed = cpuSpeed;
		this.cpuCore = cpuCore;
		this.memory = memory;
        this.rom = rom;
	}

    public Message_DeviceInfo(Context context) {
        this(
            Build.MODEL,
            (int) InfoUtils.CPU.getMaxFreqKHz(),
            InfoUtils.CPU.getCores(),
            (int) (InfoUtils.getTotalMemory(context) / (1024 * 1024)),
            Build.DISPLAY
        );
    }

	@Override
	public void serialize(JsonWriter writer) throws IOException {
		writer.beginObject();
		JsonUtils.writeString(writer, "model", this.model);
		JsonUtils.writeUnsignedInt(writer, "cpuSpeed", this.cpuSpeed);
		JsonUtils.writeUnsignedInt(writer, "cpuCore", this.cpuCore);
		JsonUtils.writeUnsignedInt(writer, "memory", this.memory);
        JsonUtils.writeString(writer, "rom", this.rom);
		writer.endObject();		
	}

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof Message_DeviceInfo)) {
            return false;
        }
        Message_DeviceInfo other = (Message_DeviceInfo) o;
        return (this.cpuSpeed == other.cpuSpeed)
            && (this.cpuCore == other.cpuCore)
            && (this.memory == other.memory)
            && Misc.isEquals(this.model, other.model)
            && Misc.isEquals(this.rom, other.rom);
    }
}
