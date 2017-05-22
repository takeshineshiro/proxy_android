package com.subao.common;

import android.os.Handler;

import com.subao.common.utils.ThreadUtils;

public class MainThreadExecutor {

	public static <R> R execute(Handler handler, RunnableHasResult<R> r) {
		if (ThreadUtils.isInAndroidUIThread()) {
			r.run();
		} else {
			handler.post(r);
		}
		return r.waitResult();
	}

}
