package cn.wsds.gamemaster.service;


import com.subao.common.auth.AuthExecutor;
import com.subao.common.msg.MessageEvent;
import com.subao.common.msg.MessageEvent.Reporter;

public class AuthExecutorController implements AuthExecutor.Controller {

	public static final AuthExecutor.Controller instance = new AuthExecutorController();

	private static final MessageEvent.Reporter reporter = new MessageEvent.Reporter() {

		@Override
		public void reportEvent(String eventName, String eventParam) {
			if (MessageEvent.ReportAllow.getAuth()) {
				//TODO 发送Event
			}
		}
	};

	@Override
	public boolean isNetConnected() {
		return NetTypeDetector_ForService.getInstance().isConnected();
	}

	@Override
	public Reporter getEventReporter() {
		return reporter;
	}

	@Override
	public String getClientVersion() {
		return null;
	}

}