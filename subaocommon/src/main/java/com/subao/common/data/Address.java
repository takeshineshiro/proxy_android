package com.subao.common.data;

import android.annotation.SuppressLint;

import com.subao.common.Misc;

@SuppressLint("DefaultLocale")
public class Address {

	//	public static class Url {
	//		public static final String DATA_CDN = "http://manager.wsds.cn";
	//	}

	public static class EndPoint {
		public final String host;
		public final int port;

		public EndPoint(String host, int port) {
			this.host = host;
			this.port = port;
		}

		@Override
		public boolean equals(Object o) {
			if (super.equals(o)) {
				return true;
			}
			if (o == null) {
				return false;
			}
			if (!(o instanceof EndPoint)) {
				return false;
			}
			EndPoint other = (EndPoint) o;
			return this.port == other.port
				&& Misc.isEquals(this.host, other.host);
		}
		
		@Override
		public String toString() {
			return String.format("[%s:%d]", this.host, this.port);
		}

        public static final EndPoint PORTAL = new EndPoint("portal.wsds.cn", -1);

        /** 消息上报服务器 */
        public static final EndPoint MESSAGE = new EndPoint(HostName.ACCEL_NODES, 503);

        /** HR **/
        public static final EndPoint HR = new EndPoint("api.xunyou.mobi", -1);

    }

    public static class HostName {

        /**
         * 加速节点的域名
         */
        public final static String ACCEL_NODES = "node-ddns.wsds.cn";

        /**
         * 测试服务器的域名
         * FIXME 可以去掉了
         */
        @Deprecated
        public static final String TEST_SERVER = "uat.xunyou.mobi";

        /**
         * 分析客户端地理位置和ISP的域名
         */
        public final static String ISP_MAP = "isp_map.wsds.cn";

    }

}
