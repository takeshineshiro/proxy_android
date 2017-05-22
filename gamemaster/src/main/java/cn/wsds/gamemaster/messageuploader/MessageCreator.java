package cn.wsds.gamemaster.messageuploader;


/**
 * Created by hujd on 16-8-2.
 */
public class MessageCreator {


//	/**
//	 *创建Start消息
//	 */
//	@NonNull
//	public static Message_Start createStartMessage(Context context) {
//		MessageUserId messageUserId = MessageUserId.build();
//		int nodeNum = AccelNodeListManagerImpl.getInstance().getCount();
//		int gameNum = AccelGameList.getInstance().getCount();
//
//		return new Message_Start(messageUserId, Message_Start.StartType.START,
//				nodeNum, gameNum, null, getMessageApps(), createMessageVersionInfo(context), AppType.ANDROID_APP);
//	}

//	/**
//	 * 创建Gaming消息
//	 */
//	@NonNull
//	public static Message_Gaming createGamingMessage(Context context, int uid) {
//		MessageUserId userId = MessageUserId.build();
//		GameInfo gameInfo = GameManager.getInstance().getGameInfoByUID(uid);
//		String appLabel, packageName;
//		if (gameInfo == null) {
//			appLabel = null;
//			packageName = null;
//		} else {
//			appLabel = gameInfo.getAppLabel();
//			packageName = gameInfo.getPackageName();
//		}
//		Message_App game = new Message_App(appLabel, packageName);
//		return new Message_Gaming(userId, System.currentTimeMillis() / 1000,
//				AppType.ANDROID_APP,
//				game,
//				getAccelMode(),
//				createMessageVersionInfo(context),
//				Message_DeviceInfo.create(context));
//	}
	
//	private static Message_Gaming.AccelMode getAccelMode() {
//		IGameVpnService intf = VPNUtils.getIGameVpnService();
//		if (intf != null) {
//			int vpnAccelState;
//			try {
//				vpnAccelState = intf.getVpnAccelState();
//			} catch (RemoteException e) {
//				return Message_Gaming.AccelMode.UNKNOWN_ACCEL_MODE;
//			}
//			if (vpnAccelState == VPNManager.ProxyMode.NONE.ordinal()) {
//				return Message_Gaming.AccelMode.NOT_ACCEL_MODE;
//			} else if (vpnAccelState == VPNManager.ProxyMode.VPN.ordinal()) {
//				return Message_Gaming.AccelMode.VPN_MODE;
//			} else if (vpnAccelState == VPNManager.ProxyMode.ROOT.ordinal()) {
//				return Message_Gaming.AccelMode.ROOT_MODE;
//			}
//		}
//		return Message_Gaming.AccelMode.UNKNOWN_ACCEL_MODE;
//	}

}
