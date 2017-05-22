package cn.wsds.gamemaster.useraction;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * UserAction的管理器，维护一个可持久化的队列，并能根据需要向服务器发送
 * 
 */
public abstract class UserActionManager {

	/**
	 * 序列化
	 */
	public interface Serializer {

		/**
		 * 将指定的{@link UserActionList}序列化为bytes
		 * 
		 * @param ual
		 *            要序列化的{@link UserActionList}，不能为null
		 * @return 序列化后的bytes数组
		 */
		public byte[] serializeSingle(UserActionList ual);

		/**
		 * 将二进制数据反序列化为{@link UserActionList}对象
		 * 
		 * @param data
		 *            二进制数据
		 * @return 成功返回{@link UserActionList}对象，失败返回null
		 */
		public UserActionList unserializeSingle(byte[] data);

		/**
		 * 将{@link UserActionList}列表序列化为二进制数据
		 * 
		 * @param list
		 *            {@link UserActionList}的列表
		 * @return 序列化后的二进制数据
		 */
		public byte[] serializeList(Iterable<UserActionList> list);

		/**
		 * 将二进制数据反序列化为{@link UserActionList}列表
		 * 
		 * @param data
		 *            二进制数据
		 * @param merge
		 *            是否合并相同VersionInfo和SubaoId的？
		 * @return 反序列化后的{@link UserActionList}列表
		 */
		public List<UserActionList> unserializeList(byte[] data, boolean merge);
	}

	private static UserActionManager instance;

	/**
	 * 取得UserActionManager的单例。<br />
	 * <b>必须保证{@code createInstance()}已被调用</b>
	 * 
	 * @return UserActionManager的实例
	 */
	public static UserActionManager getInstance() {
		if (instance == null) {
			throw new NullPointerException("Must invoke createInstance() before.");
		}
		return instance;
	}

	/**
	 * 程序初始化的时候调用（<b>且只能调用一次</b>），创建UserActionManager的实例
	 */
	public static void createInstance(VersionInfo versionInfo, Serializer serializer, String postUrl) {
		if (instance != null) {
			throw new RuntimeException("Instance of UserActionManager already created");
		}
		try {
			URL url = new URL(postUrl);
			instance = new UserActionManagerImpl(versionInfo, serializer, url);
		} catch (MalformedURLException e) {
			instance = new UserActionManagerNull();
		}
	}

	/**
	 * 外部调用：SubaoId发生变化
	 */
	public abstract void updateSubaoId(String subaoId);
	
	/**
	 * 外部调用：UserId发生变化
	 */
	public abstract void udpateUserId(String userId);

	/**
	 * 外部调用。当WiFi可用时，调用此函数，确保数据及时上报
	 */
	public abstract void onWiFiActivated();

	/**
	 * 停止线程并等待给定的毫秒数
	 * 
	 * @param milliseconds
	 *            最多等待毫秒数
	 */
	public abstract void stopAndWait(long milliseconds);

	/**
	 * 添加一条UserAction
	 */
	public abstract void addAction(long timeUTCSeconds, String actionName, String param);

}
