package com.subao.common.net;

public interface NetTypeDetector {
	
	enum NetType {
		DISCONNECT(-1),
		UNKNOWN(0),
		WIFI(1),
		MOBILE_2G(2),
		MOBILE_3G(3),
		MOBILE_4G(4);
		
		public final int value;
		
		NetType(int value) {
			this.value = value;
		}

		public static NetType fromValue(int value) {
			for (NetType nt : NetType.values()) {
				if (value == nt.value) {
					return nt;
				}
			}
			return NetType.UNKNOWN;
		}
	}

	/**
	 * 返回当前网络类型
	 * 
	 * @see {@link NetType}
	 */
	NetType getCurrentNetworkType();
	
	/**
	 * 当前是连网状态吗？
	 */
	boolean isConnected();
	
	boolean isWiFiConnected();
	
	boolean isMobileConnected();
	
}
