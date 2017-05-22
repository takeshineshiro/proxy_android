package com.subao.common.msg;

import android.annotation.SuppressLint;
import android.util.JsonWriter;

import com.subao.common.Misc;
import com.subao.common.RoboBase;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class MessageDeviceInfoTest extends RoboBase {
	
	@Test
	public void testDeviceInfoCreate(){
		Message_DeviceInfo deviceInfo = new Message_DeviceInfo(getContext());
		assertNotNull(deviceInfo.getModel());
        assertNotNull(deviceInfo.getROM());
        assertNotNull(deviceInfo.toString());
	}
	
	@SuppressLint("DefaultLocale")
    @Test
	public void serialize() throws IOException {
		String model = "abc";
		int cpuSpeed = 1000 ;
		int cpuCore = 2000;
		int memory = 0x81234567 ;
        String rom = "rom";
		Message_DeviceInfo deviceInfo = new Message_DeviceInfo(model, cpuSpeed, cpuCore, memory, rom);
		assertEquals(model, deviceInfo.getModel());
		assertEquals(cpuSpeed, deviceInfo.getCpuSpeed());
		assertEquals(cpuCore, deviceInfo.getCpuCore());
		assertEquals(memory, deviceInfo.getMemory());
        assertEquals(rom, deviceInfo.getROM());
        //
		StringWriter sw = new StringWriter(256);
		JsonWriter writer = new JsonWriter(sw);
		deviceInfo.serialize(writer);
		Misc.close(writer);
		assertEquals(
            String.format("{\"model\":\"%s\",\"cpuSpeed\":%d,\"cpuCore\":%d,\"memory\":%d,\"rom\":\"%s\"}",
                model, cpuSpeed, cpuCore, (long)memory & 0xffffffffL, rom), sw.toString());
        //
        assertEquals(deviceInfo, deviceInfo);
        assertNotEquals(deviceInfo, null);
        assertNotEquals(deviceInfo, this);
        Message_DeviceInfo deviceInfo2 = new Message_DeviceInfo(model, cpuSpeed, cpuCore, memory, rom);
        assertEquals(deviceInfo, deviceInfo2);
    }


}
