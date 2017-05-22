package cn.wsds.gamemaster.service;

import com.google.protobuf.ByteString;
import com.subao.common.accel.EngineWrapper;
import com.subao.common.auth.AuthExecutor;
import com.subao.common.auth.AuthResultReceiverImpl;
import com.subao.common.data.HRDataTrans;
import com.subao.common.data.ServiceLocation;
import com.subao.common.jni.JniWrapper;
import com.subao.common.thread.MainHandler;

import cn.wsds.gamemaster.socket.SocketServer;
import data.data_trans.AuthJWTTokenResultOuterClass;
import data.data_trans.TransDataOuterClass;

/*
 * 注意：以下所有回调都是在Jni的工作线程里，非主线程！！！
 */

public class JniCallbackPoster extends EngineWrapper.JniCallbackImpl {

	private static class  JniAuthResultReceiver extends AuthResultReceiverImpl {

		public JniAuthResultReceiver(JniWrapper jniWrapper,
									 HRDataTrans.Arguments arguments,
									 ServiceLocation serviceLocationOfIPInfoQuery) {
			super(jniWrapper, arguments, serviceLocationOfIPInfoQuery);
		}

		@Override
		public void onGetJWTTokenResult(int cid, String jwtToken, long expires,
										String shortId, int userStatus,
										String expiredTime, boolean result, int code) {
			super.onGetJWTTokenResult(cid, jwtToken, expires, shortId,
					userStatus, expiredTime, result, code);

			AuthJWTTokenResultOuterClass.AuthJWTTokenResult.Builder builder =
					AuthJWTTokenResultOuterClass.AuthJWTTokenResult.newBuilder();

			if(jwtToken!=null){
				builder.setJwtToken(jwtToken);
			}

			builder.setExpires(expires);

			if(shortId!=null){
				builder.setShortId(shortId);
			}

			builder.setUserStatus(userStatus);

			if(expiredTime!=null){
				builder.setExpiredTime(expiredTime);
			}

			builder.setCode(code);
			sendData(VPNGlobalDefines.JniMethod.METHOD_ON_GET_JWT_TOKEN_RESULT,
					builder.build().toByteString());
		}

		@Override
		public void onGetUserAccelStatusResult(int cid, String shortId, int status,
											   String expiredTime, boolean result, int code) {
			super.onGetUserAccelStatusResult(cid, shortId, status,
					expiredTime, result, code);
		}

		@Override
		public void onGetTokenResult(int cid, String ip, byte[] token,
									 int length, int expires, boolean result, int code) {
			super.onGetTokenResult(cid, ip, token, length,
					expires, result, code);
		}

		@Override
		public void onGetUserConfigResult(int cid, String jwtToken, String userId,
										  AuthExecutor.Configs configs,
										  int code, boolean result) {
			super.onGetUserConfigResult(cid, jwtToken, userId, configs,
					code, result);
		}
	}

	private static class Sender {

		private final TransDataOuterClass.TransData.Builder builder;

		public Sender(VPNGlobalDefines.JniMethod method) {
			builder = TransDataOuterClass.TransData.newBuilder();
			builder.setMethod(method.ordinal());
		}

		public Sender setIntValue(int value) {
			builder.setIntValue(value);
			return this;
		}

		public Sender setData(ByteString value) {
			builder.setData(value);
			return this;
		}

		public Sender setLongValue(long value) {
			builder.setLongValue(value);
			return this;
		}

		public Sender setStrValue(String value) {
			builder.setStrValue(value);
			return this;
		}

		public Sender setBooleanValue(boolean value) {
			builder.setBoolValue(value);
			return this;
		}

		public void send() {
			SocketServer.getInstance().postData(builder.build().toByteArray());
		}
	}

	private final JniAuthResultReceiver authResultReceiver ;
    private final EngineWrapper.AuthExecutorController authExecutorController;
	public JniCallbackPoster(EngineWrapper engineWrapper, JniWrapper jniWrapper,
							 EngineWrapper.AuthExecutorController authExecutorController,
							 ServiceLocation messageServiceLocation,
							 HRDataTrans.Arguments hrTransArguments) {
		super(engineWrapper, jniWrapper, authExecutorController,
				messageServiceLocation, hrTransArguments);

		this.authExecutorController = authExecutorController ;
		authResultReceiver = new JniAuthResultReceiver(jniWrapper,hrTransArguments,
				messageServiceLocation);
	}

	@Override
	public void onProxyActive(boolean open) {
		super.onProxyActive(open);
	}

	@Override
	public void requestUserAuth(final int cid, final String userId, final String token,
								final String appId) {
		MainHandler.getInstance().post(new Runnable() {
			@Override
			public void run() {
				AuthExecutor.getJWTToken(authExecutorController, cid, userId,
						token, appId, authResultReceiver);
			}
		});
	}

	private static void sendData(VPNGlobalDefines.JniMethod method,
								 ByteString data) {
		new Sender(method).setData(data).send();
	}

	private static void sendData(VPNGlobalDefines.JniMethod method,
								 int value) {
		new Sender(method).setIntValue(value).send();
	}

	private static void sendData(VPNGlobalDefines.JniMethod method,
								 String value) {
		new Sender(method).setStrValue(value).send();
	}

}
