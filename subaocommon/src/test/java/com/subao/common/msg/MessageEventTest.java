package com.subao.common.msg;

import com.subao.common.RoboBase;
import com.subao.common.msg.MessageEvent.ReportAllow;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MessageEventTest extends RoboBase {

	@Test
	public void test() {
		ReportAllow.set(true, true);
		assertTrue(ReportAllow.getTencent());
		assertTrue(ReportAllow.getAuth());
		//
		ReportAllow.set(true, false);
		assertTrue(ReportAllow.getTencent());
		assertFalse(ReportAllow.getAuth());
		//
		ReportAllow.set(false, true);
		assertFalse(ReportAllow.getTencent());
		assertTrue(ReportAllow.getAuth());
		//
		ReportAllow.set(false, false);
		assertFalse(ReportAllow.getTencent());
		assertFalse(ReportAllow.getAuth());
	}

}
