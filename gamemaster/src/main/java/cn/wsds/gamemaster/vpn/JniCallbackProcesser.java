package cn.wsds.gamemaster.vpn;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.subao.common.net.Protocol;
import com.subao.common.qos.QosHelper;
import com.subao.common.qos.QosManager;
import com.subao.common.qos.QosManager.Action;
import com.subao.common.qos.QosManager.CallbackParam;
import com.subao.common.utils.ThreadUtils;
import com.subao.net.NetManager;

import cn.wsds.gamemaster.FirstNetDelayManager;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.DoubleLinkUseRecords;
import cn.wsds.gamemaster.event.AuthResultObserverManager;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.ReconnectEventManager;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.messageuploader.MessageUploaderManager;
import cn.wsds.gamemaster.qos.QosTools;
import cn.wsds.gamemaster.tools.VPNUtils;

/*
 * 注意：以下所有回调都是在Jni的工作线程里，非主线程！！！
 */

public class JniCallbackProcesser {
	private static final boolean LOG = false;
	private static final String TAG = "JNICallback";

	private static JniCallbackProcesser instance;
	
	private final Context context;
	private final Handler handler;

	public static JniCallbackProcesser getInstance() {
		return instance;
	}
	
	public static void createInstance(Context context) {
		if (instance == null) {
			instance = new JniCallbackProcesser(context);
		}
	}

	private JniCallbackProcesser(Context context) {
		this.context = context.getApplicationContext();
		this.handler = new Handler(Looper.getMainLooper());
	}

	public void updateState(int state) {

//		switch (state) {
//		case VPNJni.STATE_NET_RIGHTS_DISABLED:
//			MainHandler.getInstance().onProxyRightsDisabled();
//			break;
//		}
		// FIXME: 17-3-29 hujd
	}

	/**
	 * JNI底层的游戏延时数据发生改变时调用这个函数
	 * 
	 * @param delayMilliseconds
	 *            延时的毫秒数
	 */
	public void onGameDelayDetectResult(final int delayMilliseconds) {
		MainHandler.getInstance().post(new Runnable() {
			@Override
			public void run() {
				// 重要：保证NetDelayDetectManager最先收到延迟值
				int value = FirstNetDelayManager.getInstance().
						onUDPResult(delayMilliseconds);
				// 由GameManager对延迟值进行一些必要的加工，然后用加工过的值触发TriggerManager
				TriggerManager.getInstance().raiseFirstSegmentNetDelayChange(value);				
			}
		});
	}

	public void onGameConnected(final int uid, final int connTime) {
		MainHandler.getInstance().post(new Runnable() {
			public void run() {
				GameManager.getInstance().incShortenTime(uid, connTime);
				//				TriggerManager.getInstance().raiseShortConnGameNetRequestStart(connTime);
			}
		});
	}
 
	public void onGameLog(final String log) {
		MainHandler.getInstance().post(new Runnable() {
			@Override
			public void run() {
				GameManager.getInstance().onGameLog(log);
			}
		});
	}
 
	public void onLinkMessage(final String messageId ,final String json , final  boolean finish) {
		MainHandler.getInstance().post(new Runnable() {
			@Override
			public void run() {
				MessageUploaderManager.getInstance().sendClientLinkMsg(
						messageId, json , finish);
			}
		});
	}
	
	/*public void onLinkEnd(final LinkEndData linkEndData) {
		MainHandler.getInstance().post(new Runnable() {
			@Override
			public void run() {
				MessageUploaderManager.getInstance().onJNILinkEnd(linkEndData);
			}
		});
	}*/
	
//	2.3.1 （SDK v1.6.0）取消了onLinkMessageBegin
//	public void onLinkMessageBegin(final int uid) {
//		MainHandler.getInstance().post(new Runnable() {
//			@Override
//			public void run() {
//				MessageUploaderManager.getInstance().sendClientLinkBeginMsg(uid);
//			}
//		});
//	}
 
	public void onRepairConnection(int uid, int taskId, boolean succ,
								   int reconnCount) {
//		Log.e("TTT", String.format("OnRepairConnection: uid=%d, taskId=%d, count=%d, succ=%s", uid, taskId,
//			reconnCount, Boolean.toString(succ)));
		final EventObserver.ReconnectResult rr = new EventObserver.
				ReconnectResult(uid, taskId, reconnCount, succ);
		MainHandler.getInstance().post(new Runnable() {
			@Override
			public void run() {
				//				if (rr.success || rr.count >= cn.wsds.gamemaster.GlobalDefines.MAX_COUNT_OF_CONNECTION_REPAIR) {
				//					Context context = AppMain.getContext();
				//					StatisticDefault.addEvent(context, StatisticDefault.Event.REPAIR_CONNECTION2, String.format(
				//						"%d,%d,%d", NetManager.getInstance().getCurrentNetworkType(), rr.success ? 0 : 1, rr.count));
				//				}
				ReconnectEventManager.getInstance().addEvent(rr);

			}
		});

	}
 
	public void onCloseConnect(final int errorCode) {

	}
 
	public void onNodeDetect(int code, int uid, boolean succeed) {
		TriggerManager.raiseOnNodeDetectEvent(code, uid, succeed);
	}
 
	public void onCreateConnect(final int errCode, final boolean transparent) {

	}
 
	public void onNode2GameServerDelay(final int uid, final int delay) {
		MainHandler.getInstance().post(new Runnable() {

			@Override
			public void run() {
				GameManager.getInstance().onSecondSegmentNetDelayChange(uid,
						delay, false);
			}
		});
	}
 
	public void onDirectTrans(final int uid, final int port, final int delay) {
		MainHandler.getInstance().post(new Runnable() {
			@Override
			public void run() {
				//Log.d("PROXY", String.format("$$ - direct trans, uid=%d, port=%d, delay=%d", uid, port, delay));
				GameManager.getInstance().onDirectTrans(uid, port, delay);
			}
		});
	}

	private static void runInMainThread(Runnable r) {
		if (ThreadUtils.isInAndroidUIThread()) {
			r.run();
		} else {
			instance.handler.post(r);
		}
	}
 
	public void modifyQosAccel(int id, String node, String accessToken,
							   int timeSeconds) {
		doModifyQosAccel(id, node, accessToken,timeSeconds);
	}

	public static void doModifyQosAccel(final int id, String node,
										String accessToken, int timeSeconds) {
		//FIXME sessionId
		QosHelper.Modifier modifier = new QosHelper.Modifier(
			new QosManager.Key(id, node, accessToken),
				"",
			new QosHelper.Callback() {
				@Override
				public void onQosResult(QosManager.Action action,
					CallbackParam param) {
					// VPNManager.getInstance().modifyQosAccelResult(param.id,
					// param.timeMinutes * 60, param.error.intValue);
					VPNUtils.modifyQosAccelResult(id,
						param.timeLength, param.error, TAG);
				}
			}, timeSeconds);
		runInMainThread(modifier);
	}

	/**
	 * 被C层调用：通知Java层，客户端节点测速报告
	 * 
	 * @param json
	 *            测速报告的Json串
	 */
	public void onNetworkMeasurementMsg(final String json) {
		// 注意，这里是VPN线程，1、不能占用该线程过多时间，2、不能在这里使用到UI相关内容
		MainHandler.getInstance().post(new Runnable() {
			@Override
			public void run() {
				MessageUploaderManager.getInstance().sendClientSpeedMsg(json);
			}
		});
	}

	/**
	 * 被C层调用：通知Java层，双链接辅路流量
	 */
	public void onQPPVicePathFlow(final int uid, final String protocol,
								  final int flowBytes, final int totalTime,
								  final int enableTime) {
		MainHandler.getInstance().post(new Runnable() {
			@Override
			public void run() {
				if (LOG) {
					Log.d(TAG, "totalTime: " + totalTime + "enableTime: " + enableTime);
				}
				float percent = totalTime <= 0 ? 0 : enableTime * 100f / totalTime;
				DoubleLinkUseRecords.getInstance().createRecords(uid, protocol,
						flowBytes, Math.round(percent));
			}
		});
	}
 
	public void onTencentSGameDelayInfo(int arg0, int arg1, float arg2) {
		// SDK专用的。APP不做任何事		
	}
	
	public void switchAccel(boolean on) {
		//SDK的逻辑，由代理层来开关加速。APP忽略此功能，不做任何事 	
	}

	public void onQosMessage(String jsonFromJNI) {
		 MessageUploaderManager.getInstance().onJNIQosMsg(jsonFromJNI);
	}
	
	public void onNetMeasureMessage(String jsonFromJNI) {
		 MessageUploaderManager.getInstance().onJNINetMeasureMsg(jsonFromJNI);
	}

	public void onGetJWTTokenResult(final String jwtToken, final long expires,
									final String shortId, final int userStatus,
									final String expiredTime,final boolean result,
									final int code){

		runInMainThread(new Runnable() {
			@Override
			public void run() {
				AuthResultObserverManager.getInstance().onGetJWTTokenResult(
						jwtToken, expires, shortId, userStatus, expiredTime,
						result, code);
			}
		});

	}

	public void onGetTokenResult(final String ip, final byte[] token,
								 final int length,final int expires,
								 final boolean result, final int code){
		runInMainThread(new Runnable() {
			@Override
			public void run() {
				AuthResultObserverManager.getInstance().onGetTokenResult(ip,
						token, length, expires, result, code);
			}
		});
	}

	public void onGetUserAccelStatusResult(final String shortId, final int status,
										   final String expiredTime,
										   final boolean result, final int code){
		runInMainThread(new Runnable() {
			@Override
			public void run() {
				AuthResultObserverManager.getInstance().
						onGetUserAccelStatusResult(shortId, status,
						expiredTime, result, code);
			}
		});
	}

	public void onGetUserConfigResult(final String config,
									  final int code, final boolean result){
		runInMainThread(new Runnable() {
			@Override
			public void run() {
				AuthResultObserverManager.getInstance().
						onGetUserConfigResult(config, code, result);
			}
		});
	}
}
