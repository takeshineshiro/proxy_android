package cn.wsds.gamemaster.net;

import com.subao.net.NetManager;

/**
 * 网络状态检查
 */
public abstract class NetworkStateChecker {
	/**
	 * 判断当前网络是否可用
	 */
	public abstract boolean isNetworkAvail();

	public static final NetworkStateChecker defaultInstance = new DefaultImpl();

	private static class DefaultImpl extends NetworkStateChecker {

		@Override
		public boolean isNetworkAvail() {
			return NetManager.getInstance().isConnected();

		}

	}
}
