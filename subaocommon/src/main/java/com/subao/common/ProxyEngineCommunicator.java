package com.subao.common;

/**
 * 负责与代理层通讯的接口
 */
public interface ProxyEngineCommunicator {

    /**
     * 设置Integer值
     *
     * @param cid   Call ID
     * @param key   Key
     * @param value Value
     */
    void setInt(int cid, String key, int value);

    /**
     * 设置String值
     *
     * @param cid   Call ID
     * @param key   Key
     * @param value Value
     */
    void setString(int cid, String key, String value);

    void defineConst(String key, String value);

    /**
     * 取单一实例。
     * <p>（暂时这么实现：VPNManager初始化后，由上层set。以后等com.subao.vpn合入com.subao.common后再优化）</p>
     */
    class Instance {

        private static ProxyEngineCommunicator instance;

        public static ProxyEngineCommunicator get() {
            return instance;
        }

        public static void set(ProxyEngineCommunicator instance) {
            Instance.instance = instance;
        }
    }

}
