package com.subao.common.net;

import com.subao.common.msg.MessageEvent;

/**
 * Created by hujd on 16-7-1.
 */
public abstract class ResponseCallback {

	private final MessageEvent.Reporter eventReporter;
    protected final int cid;

	public ResponseCallback(MessageEvent.Reporter eventReporter, int cid) {
		this.eventReporter = eventReporter;
        this.cid = cid;
	}

	/**
	 * 返回事件名称
	 */
	protected abstract String getEventName();

	/**
	 * 请求响应回调，被HttpClient调用
	 */
	public final void onResponse(Http.Response response) {
		if (response == null) {
			doFail(-1, null);
			return;
		}
		if (isHttpResponseCodeSuccess(response.code)) {
			byte[] data = response.data;
			if (data != null) {
				onSuccess(response.code, data);
			} else {
				doFail(-1, null);
			}
		} else {
			doFail(response.code, response.data);
		}
	}

	public static boolean isHttpResponseCodeSuccess(int code) {
		return code >= 200 && code < 300;
	}

	/**
	 * 请求成功将数据进行处理
	 */
	protected abstract void onSuccess(int code, byte[] responseData);

	/**
	 * 请求失败（包括IOException和网络未连接、服务器返回失败Code、服务器返回的数据不可识别）
	 */
	protected abstract void onFail(int code, byte[] responseData);

	/**
	 * 当服务器返回失败Code、返回的数据不可识别（解析错）时被调用
	 * <p>
	 * <b>本函数保证会调用到{@link #onFail(int, byte[])}</b>
	 * </p>
	 */
	protected final void doFail(int code, byte[] responseData) {
		if (eventReporter != null) {
			eventReporter.reportEvent(getEventName(), Integer.toString(code));
		}
		this.onFail(code, responseData);
	}

	/**
	 * 请求过程中发生IO异常
	 */
	public final void doIOException() {
		if (eventReporter != null) {
			eventReporter.reportEvent(getEventName(), "io");
		}
		this.onFail(-1, null);
	}

	/**
	 * 在请求前发现网络未连接
	 */
	public final void doNetDisconnected() {
		if (eventReporter != null) {
			eventReporter.reportEvent(getEventName(), "net");
		}
		this.onFail(-1, null);
	}

	protected final void reportSuccessEvent() {
		if (eventReporter != null) {
			eventReporter.reportEvent(getEventName(), "ok");
		}
	}
}
