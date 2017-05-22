package cn.wsds.gamemaster.socket;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.subao.common.Logger;
import com.subao.common.msg.MessageUserId;

import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.service.VPNGlobalDefines;
import cn.wsds.gamemaster.ui.floatwindow.WifiAccelState;
import cn.wsds.gamemaster.vpn.JniCallbackProcesser;
import data.data_trans.AuthJWTTokenResultOuterClass;
import data.data_trans.AuthTokenResultOuterClass;
import data.data_trans.AuthUserAccelStatusResultOuterClass;
import data.data_trans.CloseQosAccelInfoOuterClass.CloseQosAccelInfo;
import data.data_trans.CreateConnectInfoOuterClass.CreateConnectInfo;
import data.data_trans.DirectTransInfoOuterClass.DirectTransInfo;
import data.data_trans.GameConnectedInfoOuterClass.GameConnectedInfo;
import data.data_trans.LinkEndDataOuterClass.LinkEndData;
import data.data_trans.MessageUserIdUpdate.MessageUserIdUpdateInfo;
import data.data_trans.ModifyQosAccelInfoOuterClass.ModifyQosAccelInfo;
import data.data_trans.NodeDetectInfoOuterClass.NodeDetectInfo;
import data.data_trans.OpenQosAccelInfoOuterClass.OpenQosAccelInfo;
import data.data_trans.QPPVicePathFlowInfoOuterClass.QPPVicePathFlowInfo;
import data.data_trans.RepairConnectionInfoOuterClass.RepairConnectionInfo;
import data.data_trans.TencentSGameDelayInfoOuterClass.TencentSGameDelayInfo;
import data.data_trans.TransDataOuterClass.TransData;

public class SocketDataParser {

	private static final String TAG = "SocketDataParser";
	private static final boolean ISDEBUG = false;

	/**
	 * 解析Service通过Socket传来的数据，并执行相应的函数操作
	 * <b>注意，本函数是在“非主线程”里被调用的</b>
	 */
	public static void parse(byte[] msg) {
		if (msg == null) {
			return;
		}
		TransData transData;
		try {
			transData = TransData.parseFrom(msg);
			processMethods(transData);
		} catch (InvalidProtocolBufferException e) {
			Logger.e(TAG, e.toString());
		}
	}

	/**
	 * 解析给定的{@link TransData}并执行相应的函数
	 * <p>注意：这是在“非主线程”里被调用</p>
	 */
	private static void processMethods(TransData transData) {
		if(transData == null){
			return ;
		}
		try {
			VPNGlobalDefines.JniMethod method = VPNGlobalDefines.
					JniMethod.fromOrdinal(transData.getMethod());
			ByteString data = transData.getData();
			if(method==null){
				return;
			}
			switch (method) {
			case METHOD_UPDATE_STATE:
				updateState(transData.getIntValue());
				break;
			case METHOD_ON_GAME_DELAY_DETECT_ERESULT:
				onGameDelayDetectResult(transData.getIntValue());
				break;
			case METHOD_ON_GAME_CONNECTED:
				onGameConnected(data);
				break;
			case METHOD_ON_GAME_LOG:
				onGameLog(transData.getStrValue());
				break;
			case METHOD_ON_LINK_MESSAGE:
				onLinkMessage(transData.getStrValue(), transData.getStrValue(),
						transData.getBoolValue());
				break;
			case METHOD_ON_REPAIR_CONNECTION:
				onRepairConnection(data);
				break;
			case METHOD_ON_CLOSE_CONNECT:
				onCloseConnect(transData.getIntValue());
				break;
			case METHOD_ON_NODE_DETECT:
				onNodeDetect(data);
				break;
			case METHOD_ON_CREATE_CONNECT:
				onCreateConnect(data);
				break;
			case METHOD_ON_NODE2_GAME_SERVER_DELAY:
				onNode2GameServerDelay(transData.getIntValue(),
						(int)transData.getLongValue());
				break;
			case METHOD_ON_DIRECT_TRANS:
				onDirectTrans(data);
				break;
			case METHOD_MODIFY_QOS_ACCEL:
				modifyQosAccel(data);
				break;
			case METHOD_QPP_VICE_PATH_FLOW:
				onQPPVicePathFlow(data);
				break;
			case METHOD_ON_TENCENT_SGAME_DELAY_INFO:
				onTencentSGameDelayInfo(data);
				break;
			case METHOD_ON_QOS_MESSAGE:
				onQosMessage(transData.getStrValue());
				break;
			case METHOD_ON_NET_MEASURE_MESSAGE:
				onNetMeasureMessage(transData.getStrValue());
				break;
			case METHOD_ON_MESSAGE_USER_ID_UPDATE:
				onMessagUserIdUpdate(data);
				break;
			case METHOD_ON_WIFI_ACCEL_STATE_CHANGE:
				onWifiAccelState(transData);
				break;
			case METHOD_ON_GET_JWT_TOKEN_RESULT:
				onGetJWTTokenResult(data);
				break;
			case METHOD_ON_GET_TOKEN_RESULT:
				onGetTokenResult(data);
				break;
			case METHOD_ON_GET_USER_ACCEL_STATUS_RESULT:
				onGetUserAccelStatusResult(data);
				break;
			case METHOD_ON_GET_USER_CONFIG_RESULT:
				onGetUserConfigResult(transData.getStrValue(),
						transData.getIntValue(), transData.getBoolValue());
				break;
			default:
				break;
			}
		} catch (Exception e) {
			Logger.e(TAG, e.toString());
		}
	}

	private static void onWifiAccelState(TransData transData) {
		boolean boolValue = transData.getBoolValue();
		WifiAccelState.getInstance().setAccelState(boolValue);
		TriggerManager.getInstance().raiseWifiAccelState(boolValue);
	}

	private static void updateState(int state) throws
			InvalidProtocolBufferException {
		printLogs("updateState");
		JniCallbackProcesser.getInstance().updateState(state);
	}

	private static void onGameDelayDetectResult(int delayMilliseconds) throws
			InvalidProtocolBufferException {
		printLogs("onGameDelayDetectResult");
		JniCallbackProcesser.getInstance().onGameDelayDetectResult(delayMilliseconds);
	}

	private static void onGameConnected(ByteString data) throws
			InvalidProtocolBufferException {
		printLogs("onGameConnected");
		GameConnectedInfo info = GameConnectedInfo.parseFrom(data);
		int uid = info.getUid();
		int connTime = info.getConnTime();
		JniCallbackProcesser.getInstance().onGameConnected(uid, connTime);
	}

	private static void onGameLog(String log) {
		printLogs("onGameLog");
		JniCallbackProcesser.getInstance().onGameLog(log);
	}

	private static void onLinkMessage(String messageId, String messageBody , boolean finish) {
		printLogs("onLinkMessage");
		JniCallbackProcesser.getInstance().onLinkMessage(messageId, messageBody,finish);
	}
	
	private static void onRepairConnection(ByteString data) throws
			InvalidProtocolBufferException {
		printLogs("onRepairConnection");
		RepairConnectionInfo info = RepairConnectionInfo.parseFrom(data);
		int uid = info.getUid();
		int taskId = info.getTaskId();
		boolean succ = info.getSucc();
		int reconnCount = info.getReconnCount();
		JniCallbackProcesser.getInstance().onRepairConnection(uid,
				taskId, succ, reconnCount);

	}

	private static void onCloseConnect(int errorCode) throws
			InvalidProtocolBufferException {
		printLogs("onCloseConnect");
		JniCallbackProcesser.getInstance().onCloseConnect(errorCode);
	}

	private static void onNodeDetect(ByteString data) throws
			InvalidProtocolBufferException {
		printLogs("onNodeDetect");
		NodeDetectInfo info = NodeDetectInfo.parseFrom(data);
		int code = info.getCode();
		int uid = info.getUid();
		boolean succeed = info.getSucceed();
		JniCallbackProcesser.getInstance().onNodeDetect(code, uid, succeed);
	}

	private static void onCreateConnect(ByteString data) throws
			InvalidProtocolBufferException {
		printLogs("onCreateConnect");
		CreateConnectInfo info = CreateConnectInfo.parseFrom(data);
		int errorCode = info.getErrCode();
		boolean transparent = info.getTransparent();
		JniCallbackProcesser.getInstance().onCreateConnect(errorCode,
				transparent);
	}

	private static void onNode2GameServerDelay(int uid, int delay) throws
			InvalidProtocolBufferException {
		printLogs("onGetUserAccelStatus");
		JniCallbackProcesser.getInstance().onNode2GameServerDelay(uid, delay);
	}

	private static void onDirectTrans(ByteString data) throws
			InvalidProtocolBufferException {
		printLogs("onDirectTrans");
		DirectTransInfo info = DirectTransInfo.parseFrom(data);
		int uid = info.getUid();
		int port = info.getPort();
		int delay = info.getDelay();
		JniCallbackProcesser.getInstance().onDirectTrans(uid, port, delay);
	}

	private static void modifyQosAccel(ByteString data) throws
			InvalidProtocolBufferException {
		printLogs("modifyQosAccel");
		ModifyQosAccelInfo info = ModifyQosAccelInfo.parseFrom(data);
		int id = info.getId();
		String node = info.getNode();
		String accessToken = info.getAccessToken();
		int timeSeconds = info.getTimeSeconds();
		JniCallbackProcesser.getInstance().modifyQosAccel(id, node,
				accessToken, timeSeconds);
	}

	private static void onQPPVicePathFlow(ByteString data) throws
			InvalidProtocolBufferException {
		printLogs("onQPPVicePathFlow");
		QPPVicePathFlowInfo info = QPPVicePathFlowInfo.parseFrom(data);
		int uid = info.getUid();
		int flowBytes = info.getFlowBytes();
		int totalTime = info.getTotalTime();
		int enableTime = info.getEnableTime();
		String protocol = info.getProtocol();
		JniCallbackProcesser.getInstance().onQPPVicePathFlow(uid, protocol,
				flowBytes, totalTime, enableTime);
	}

	private static void onTencentSGameDelayInfo(ByteString data) throws
			InvalidProtocolBufferException {
		printLogs("onTencentSGameDelayInfo");
		TencentSGameDelayInfo info = TencentSGameDelayInfo.parseFrom(data);
		int arg0 = info.getArg0();
		int arg1 = info.getArg1();
		float arg2 = info.getArg2();
		JniCallbackProcesser.getInstance().onTencentSGameDelayInfo(arg0,
				arg1, arg2);
	}

	private static void onQosMessage(String jsonFromJNI) throws
			InvalidProtocolBufferException {
		printLogs("onQosMessage");
		JniCallbackProcesser.getInstance().onQosMessage(jsonFromJNI);
	}

	private static void onNetMeasureMessage(String jsonFromJNI) throws
			InvalidProtocolBufferException {
		printLogs("onNetMeasureMessage");
		JniCallbackProcesser.getInstance().onNetMeasureMessage(jsonFromJNI);
	}

	private static void onMessagUserIdUpdate(ByteString data) throws
			InvalidProtocolBufferException {
		printLogs("onMessagUserIdUpdate");
		MessageUserIdUpdateInfo info = MessageUserIdUpdateInfo.parseFrom(data);

		//TODO about para expireTime
		MessageUserId.setCurrentUserInfo(info.getUserId(), info.getServiceId(),
				info.getUserStatus(),"");
	}

	private static void onGetJWTTokenResult(ByteString data)throws
			InvalidProtocolBufferException {
		printLogs("onGetJWTTokenResult");
		AuthJWTTokenResultOuterClass.AuthJWTTokenResult info =
				AuthJWTTokenResultOuterClass.AuthJWTTokenResult.parseFrom(data);
		String jwtToken = info.getJwtToken();
		long expires = info.getExpires();
		String shortId = info.getShortId();
		int userStatus = info.getUserStatus();
		String expiredTime = info.getExpiredTime();
		boolean result = info.getResult();
		int code = info.getCode();

		JniCallbackProcesser.getInstance().onGetJWTTokenResult(
				jwtToken, expires, shortId, userStatus,
				expiredTime, result,code) ;
	}

	private static void onGetTokenResult(ByteString data)throws
			InvalidProtocolBufferException {
		printLogs("onGetTokenResult");
		AuthTokenResultOuterClass.AuthTokenResult info =
				AuthTokenResultOuterClass.AuthTokenResult.parseFrom(data);
		String ip = info.getIp();
		byte[] token = info.getToken().toByteArray();
		int length = info.getLength();
		int expires = info.getExpires();
		boolean result = info.getResult();
		int code = info.getCode();

		JniCallbackProcesser.getInstance().onGetTokenResult(ip, token,
				length, expires,result, code);
	}

	private static void onGetUserAccelStatusResult(ByteString data)throws
			InvalidProtocolBufferException {
		printLogs("onGetUserAccelStatusResult");
		AuthUserAccelStatusResultOuterClass.AuthUserAccelStatusResult info =
				AuthUserAccelStatusResultOuterClass.AuthUserAccelStatusResult.parseFrom(data);
		String shortId = info.getShortId();
		int status = info.getStatus();
		String expiredTime = info.getExpiredTime();
		boolean result = info.getResult();
		int code = info.getCode();
		JniCallbackProcesser.getInstance().onGetUserAccelStatusResult(shortId, status, expiredTime,
				result, code);
	}

    private static void  onGetUserConfigResult(String config, int code, boolean result)throws InvalidProtocolBufferException {
    	printLogs("onGetUserConfigResult");
    	JniCallbackProcesser.getInstance().onGetUserConfigResult(config, code, result);
	}


   /* private static void onAccelTokenInvalid(int err){
    	printLogs("onAccelTokenInvalid");
    	JNICallbackProcesser.getInstance().onAccelTokenInvalid(err);
    }*/

	private static void printLogs(String log) {
		if (ISDEBUG) {
			Logger.i(TAG, log);
		}
	}
}
