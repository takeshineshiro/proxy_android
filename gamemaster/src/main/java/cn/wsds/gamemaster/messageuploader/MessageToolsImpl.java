package cn.wsds.gamemaster.messageuploader;

import android.content.Context;

import com.subao.common.data.AppType;
import com.subao.common.msg.MessageBuilder;
import com.subao.common.msg.MessagePersistent;
import com.subao.common.msg.MessageTools;
import com.subao.common.msg.Message_Gaming.AccelMode;
import com.subao.common.msg.Message_VersionInfo;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.utils.InfoUtils;
import com.subao.common.utils.ThreadUtils;
import com.subao.net.NetManager;

import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.data.DeviceInfo;
import cn.wsds.gamemaster.tools.VPNUtils;

/**
 * APP所用的{@link MessageTools}，实现消息上报系统所必须的一些接口
 */
public class MessageToolsImpl implements MessageTools {
	private final Context context;
	private final String channel;

	private final MessageBuilder messageBuilder;

	public MessageToolsImpl(Context context, String channel) {
		this.context = context.getApplicationContext();
		this.channel = channel;
		this.messageBuilder = new MessageBuilder(
			this.context,
			AppType.ANDROID_APP,
			Message_VersionInfo.create(
				InfoUtils.getVersionName(this.context),
				DeviceInfo.getUmengChannel(this.context)));
	}

	@Override
	public Context getContext() {
		return context;
	}

	/*@Override
	public String getChannel() {
		return channel;
	}*/

	@Override
	public NetTypeDetector getNetTypeDetector() {
		return NetManager.getInstance();
	}

	@Override
	public AppType getAppType() {
		return AppType.ANDROID_APP;
	}

	@Override
	public void runInMainThread(Runnable runnable) {
		if (ThreadUtils.isInAndroidUIThread()) {
			runnable.run();
		} else {
			MainHandler.getInstance().post(runnable);
		}
	}

	@Override
	public void onMessageStartSent() {}

	@Override
	public String getIMSI() {
		return InfoUtils.getIMSI(context);
	}

	/*@Override
	public SmartIPSelector getSmartIPSelector() {
		return AccelNodeListManagerImpl.getInstance();
	}*/

	@Override
	public AccelMode getCurrentAccelMode() {
		int state = VPNUtils.getAccelStatus("MessageTools");
		// TODO: 17-3-31 vpn
		if (state == 1) {
			return AccelMode.VPN_MODE;
		}
		return AccelMode.UNKNOWN_ACCEL_MODE;
	}

	@Override
	public MessagePersistent getMessagePersistent() {
		return null;
	}

	/*@Override
	public GameInfo getGameInfoWithUid(int uid) {
		cn.wsds.gamemaster.app.GameInfo gameInfo = GameManager.getInstance().getGameInfoByUID(uid);
		if (gameInfo != null) {
			AppType appType = gameInfo.isSDKEmbed() ? AppType.ANDROID_SDK_EMBEDED : AppType.ANDROID_APP;
			return new GameInfo(gameInfo.getAppLabel(), gameInfo.getPackageName(), appType);
		} else {
			return new GameInfo(null, null, AppType.UNKNOWN_APPTYPE);
		}
	}*/

	@Override
	public MessageBuilder getMessageBuilder() {
		return this.messageBuilder;
	}

}
