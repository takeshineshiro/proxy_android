package com.subao.common.msg;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MessageAppTester {

	private static final String PACKAGE_NAME = "com.wsds.games";
	private static final String APP_LABEL = "game";

	private static Message_App createTarget() {
		return new Message_App(APP_LABEL, PACKAGE_NAME);
	}

	@Test
	public void testMessageApp() {
		Message_App app = createTarget();
		assertEquals(APP_LABEL, app.appLabel);
		assertEquals(PACKAGE_NAME, app.pkgName);
	}

	@Test
	public void testEquals() {
		Message_App app = createTarget();
		assertEquals(app, app);
		assertFalse(app.equals(null));
		assertFalse(app.equals(new Object()));
		assertEquals(app, createTarget());
		assertFalse(app.equals(new Message_App(APP_LABEL, null)));
		assertFalse(app.equals(new Message_App(null, PACKAGE_NAME)));
	}

}
