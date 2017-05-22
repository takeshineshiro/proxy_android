package cn.wsds.gamemaster.messageuploader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.data.AppType;
import com.subao.common.data.Defines;
import com.subao.common.data.ServiceLocation;
import com.subao.common.data.SubaoIdManager;
import com.subao.common.msg.MessageSender;
import com.subao.common.msg.MessageSenderImpl;
import com.subao.common.msg.MessageUserId;
import com.subao.common.msg.Message_App;
import com.subao.common.msg.Message_DeviceInfo;
import com.subao.common.msg.Message_Installation;
import com.subao.common.msg.Message_Installation.UserInfo;
import com.subao.common.msg.Message_Upgrade;
import com.subao.common.msg.Message_VersionInfo;
import com.subao.common.utils.CalendarUtils;
import com.subao.common.utils.InfoUtils;
import com.subao.data.InstalledAppInfo;

import java.util.ArrayList;
import java.util.List;

import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.AccelGameList;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.DeviceInfo;

//import com.subao.common.jni.VPNJniCallback.LinkEndData;

/**
 * 事件上报管理 Created by hujd on 15-12-3.
 */
@Deprecated //不再需要
public class MessageUploaderManager {

	private static final long CHECK_PEROID = 30 * 60 * 1000; //start消息扫描周期，毫秒
	private static final AppType APP_TYPE = AppType.ANDROID_APP;

	private static MessageUploaderManager INSTANCE;

	private final MessageSenderImpl messageSender;

	private final Context mContext;
	private final Message_VersionInfo versionInfo;

	private MessageUploaderManager(Context context, ServiceLocation serviceLocation) {
		this.mContext = context.getApplicationContext();
		versionInfo = Message_VersionInfo.create(InfoUtils.getVersionName(mContext), DeviceInfo.getUmengChannel(mContext));
		messageSender = (MessageSenderImpl) MessageSenderImpl.create(serviceLocation,
				new MessageToolsImpl(mContext, versionInfo.channel));

	}

	public static void init(Context context, ServiceLocation serviceLocation) {
		INSTANCE = new MessageUploaderManager(context, serviceLocation);
	}

	public static MessageUploaderManager getInstance() {
		return INSTANCE;
	}

	public Message_VersionInfo getVersionInfo() {
		return this.versionInfo;
	}
	
	public MessageSender getMessageSender() {
		return this.messageSender;
	}

	/**
	 * 发送Install消息
	 */
	public void sendClientInstallMsg(Context context) {
		List<Message_App> apps = buildMessageAppList();
		Message_Installation msg = new Message_Installation(
			APP_TYPE, System.currentTimeMillis() / 1000,
			UserInfo.create(context),
			new Message_DeviceInfo(context),
			this.versionInfo,
			apps);
		messageSender.offerInstallation(msg);
	}

	@Nullable
	private static List<Message_App> buildMessageAppList() {
		InstalledAppInfo[] appInfos = GameManager.getInstance().getInstalledApps();
		List<Message_App> apps;
		if (appInfos != null && appInfos.length > 0) {
			apps = new ArrayList<Message_App>(appInfos.length);
			for (InstalledAppInfo appInfo : appInfos) {
				Message_App app = new Message_App(appInfo.getAppLabel(), appInfo.getPackageName());
				apps.add(app);
			}
		} else {
			apps = null;
		}
		return apps;
	}

	/**
	 * 处理start消息
	 */
	private static class RunnableStart implements Runnable {
		@Override
		public void run() {
			if (SubaoIdManager.getInstance().getSubaoId() == null) {
				MainHandler.getInstance().postDelayed(this, 1000 * 60);
				return;
			}

			MainHandler.getInstance().postDelayed(this, CHECK_PEROID);
			long lastSubmitTime = ConfigManager.getInstance().getLastTimeOfSubmitStartMessage();
			long now = System.currentTimeMillis();
			if (lastSubmitTime == 0) {
				submitMessage(now);
			} else {
				/** 跨北京时间零点上报 */
				if (!CalendarUtils.isSameDayOfCST(System.currentTimeMillis(), lastSubmitTime)) {
					submitMessage(now);
				}
			}
		}

		private static void submitMessage(long now) {


//			INSTANCE.messageSender.offerStart(
//				AccelNodeListManagerImpl.getInstance().getCount(),
//				AccelGameList.getInstance().getCount(),
//				buildMessageAppList());
//			ConfigManager.getInstance().setLastTimeOfSubmitStartMessage(now);
		}
	}

	/**
	 * 发送stat消息
	 */
	public void sendClientStartMsg() {
		MainHandler.getInstance().postDelayed(new RunnableStart(), 10 * 1000);
	}

	/**
	 * 发送upgrade消息
	 */
	public void sendClientUpgradeMsg() {
		MainHandler.getInstance().postDelayed(new Runnable() {

			@NonNull
			private Message_Upgrade createUpgradeMessage(Message_VersionInfo oldVersionInfo) {
				MessageUserId userId = MessageUserId.build();
				return new Message_Upgrade(userId, System.currentTimeMillis() / 1000,
					oldVersionInfo, versionInfo, APP_TYPE);
			}

			@Override
			public void run() {
				String subaoId = SubaoIdManager.getInstance().getSubaoId();
				if (subaoId == null) {
					MainHandler.getInstance().postDelayed(this, 1000 * 60);
				} else {
					Logger.d(LogTag.MESSAGE, "Check upgrade information");
					Message_VersionInfo lastVersionInfo = ConfigManager.getInstance().getVersionInfo();
					if (lastVersionInfo == null) {
						ConfigManager.getInstance().setVersionInfo(versionInfo);
					} else if (!versionInfo.equals(lastVersionInfo)) {
						Message_Upgrade messageUpgrade = createUpgradeMessage(lastVersionInfo);
						messageSender.offerUpgrade(messageUpgrade);
					}
				}
			}
		}, 10 * 1000);
	}

	/**
	 * 发送网络测速消息
	 */
	public void sendClientSpeedMsg(String speedJSON) {
		messageSender.onJNINetMeasureMsg(speedJSON);
	}

	/**
	 * 发送link消息
	 */
	public void sendClientLinkMsg(String messageId, String messageBody, boolean finish) {
		messageSender.onJNILinkMsg(messageId, messageBody,finish);
	}

	/*public void onJNILinkEnd(LinkEndData linkEndData) {
		messageSender.onJNILinkEnd(linkEndData);
	}*/

	public void onJNIQosMsg(String jsonFromJNI) {
		messageSender.onJNIQosMsg(jsonFromJNI);
	}

	public void onJNINetMeasureMsg(String jsonFromJNI) {
		messageSender.onJNINetMeasureMsg(jsonFromJNI);

	}

}
